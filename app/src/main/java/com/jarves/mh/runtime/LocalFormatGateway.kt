package com.jarves.mh.runtime

import android.util.Log
import com.jarves.mh.model.ProviderProtocol
import com.jarves.mh.model.ProviderProfile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONArray
import org.json.JSONObject

/**
 * Loopback-only Anthropic `/v1/messages` <-> OpenAI (`/v1/chat/completions` & `/v1/responses`)
 * translation gateway with real-time SSE token/reasoning/tool streaming and custom header support.
 */
internal class LocalFormatGateway(
    private val profile: ProviderProfile,
    private val apiKey: String,
) : AutoCloseable {
    private val running = AtomicBoolean(true)
    private val server = ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"))
    val url: String = "http://127.0.0.1:${server.localPort}"

    fun start(): LocalFormatGateway = apply {
        Thread({ acceptLoop() }, "cs-format-gateway").apply { isDaemon = true; start() }
    }

    private fun acceptLoop() {
        while (running.get()) {
            runCatching { server.accept() }.getOrNull()?.let { socket ->
                runCatching { socket.soTimeout = SOCKET_TIMEOUT_MS }
                Thread({ socket.use(::handle) }, "cs-format-request").apply { isDaemon = true; start() }
            }
        }
    }

    private fun handle(socket: Socket) {
        val input = BufferedInputStream(socket.getInputStream())
        val requestLine = readLine(input) ?: return
        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = readLine(input) ?: return
            if (line.isEmpty()) break
            val split = line.indexOf(':')
            if (split > 0) headers[line.substring(0, split).lowercase()] = line.substring(split + 1).trim()
        }
        val output = BufferedOutputStream(socket.getOutputStream())
        if (!isAuthorized(headers)) {
            writeJson(output, 401, errorJson("authentication_error", "Missing or invalid credentials"))
            return
        }
        val length = headers["content-length"]?.toIntOrNull() ?: 0
        if (length !in 0..MAX_REQUEST_BYTES) {
            writeJson(output, 413, errorJson("invalid_request_error", "Request body is too large"))
            return
        }
        val bodyBytes = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val count = input.read(bodyBytes, offset, length - offset)
            if (count < 0) break
            offset += count
        }
        val path = requestLine.split(' ').getOrNull(1).orEmpty().substringBefore('?')
        if (path.endsWith("/count_tokens")) {
            val approximate = bodyBytes.decodeToString().length / 4 + 1
            writeJson(output, 200, JSONObject().put("input_tokens", approximate).toString())
            return
        }
        if (!path.endsWith("/messages")) {
            writeJson(output, 404, errorJson("not_found", "Unsupported gateway endpoint"))
            return
        }
        runCatching {
            val anthropic = JSONObject(bodyBytes.decodeToString())
            val wantsStream = anthropic.optBoolean("stream", false)
            val modelName = anthropic.optString("model", profile.model)
            val useResponsesApi = profile.effectiveClaudeProtocol == ProviderProtocol.OPENAI_RESPONSES
            if (useResponsesApi) {
                val upstream = callProviderJson(resolveUpstreamEndpoint(responses = true), toOpenAiResponses(anthropic))
                if (upstream.first !in 200..299) {
                    writeJson(output, upstream.first, errorJson("api_error", providerError(upstream.second)))
                } else {
                    val translated = fromOpenAiResponses(JSONObject(upstream.second), modelName)
                    if (wantsStream) writeSyntheticStream(output, translated) else writeJson(output, 200, translated.toString())
                }
            } else if (wantsStream) {
                streamChatCompletions(anthropic, modelName, output)
            } else {
                val upstream = callProviderJson(resolveUpstreamEndpoint(responses = false), toOpenAiChat(anthropic, stream = false))
                if (upstream.first !in 200..299) {
                    writeJson(output, upstream.first, errorJson("api_error", providerError(upstream.second)))
                } else {
                    val translated = fromOpenAiChat(JSONObject(upstream.second), modelName)
                    writeJson(output, 200, translated.toString())
                }
            }
        }.onFailure { error ->
            Log.w("FormatGateway", "Gateway request failed", error)
            runCatching {
                writeJson(output, 502, errorJson("api_error", error.message ?: "Provider request failed"))
            }
        }
    }

    internal fun resolveUpstreamEndpoint(responses: Boolean): String {
        val raw = profile.resolvedBaseUrl.trim().trimEnd('/')
        if (responses) {
            if (raw.endsWith("/responses")) return raw
            val clean = raw.removeSuffix("/chat/completions").trimEnd('/')
            val uriPath = runCatching { URI(clean).path.orEmpty().trimEnd('/') }.getOrDefault("")
            return if (uriPath.isEmpty()) "$clean/v1/responses" else "$clean/responses"
        }
        if (raw.endsWith("/chat/completions")) return raw
        val clean = raw.removeSuffix("/responses").trimEnd('/')
        val uriPath = runCatching { URI(clean).path.orEmpty().trimEnd('/') }.getOrDefault("")
        return if (uriPath.isEmpty()) "$clean/v1/chat/completions" else "$clean/chat/completions"
    }

    private fun openConfiguredConnection(endpoint: String): HttpURLConnection {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 20_000
        connection.readTimeout = 240_000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        if (apiKey.isNotBlank() && apiKey != "no-key-required") {
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
        }
        if (endpoint.startsWith("https://opencode.ai/zen/")) {
            connection.setRequestProperty("User-Agent", "opencode/1.18.20")
            connection.setRequestProperty("x-session-id", "session-${UUID.randomUUID()}")
        }
        profile.parsedCustomHeaders.forEach { (header, value) ->
            connection.setRequestProperty(header, value)
        }
        return connection
    }

    /**
     * Real-time SSE streaming from OpenAI `/chat/completions` (`stream: true`) directly into
     * Anthropic's streaming protocol (`thinking_delta`, `text_delta`, `input_json_delta`), with
     * automatic fallback if a self-hosted server returns non-streamed JSON instead of `text/event-stream`.
     */
    private fun streamChatCompletions(anthropic: JSONObject, modelName: String, output: BufferedOutputStream) {
        val endpoint = resolveUpstreamEndpoint(responses = false)
        if (!com.jarves.mh.network.LocalNetwork.isPlainHttpAllowed(endpoint)) {
            writeJson(output, 400, errorJson("invalid_request_error", "Plain http:// is only allowed for local network addresses"))
            return
        }
        val payload = toOpenAiChat(anthropic, stream = true)
        val connection = openConfiguredConnection(endpoint)
        connection.setRequestProperty("Accept", "text/event-stream, application/json")
        try {
            connection.outputStream.use { it.write(payload.toString().toByteArray()) }
            val code = connection.responseCode
            if (code !in 200..299) {
                val errBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                writeJson(output, code, errorJson("api_error", providerError(errBody)))
                return
            }
            val contentType = connection.contentType.orEmpty().lowercase()
            if (!contentType.contains("event-stream")) {
                val fullBody = connection.inputStream.bufferedReader().use { it.readText() }
                val translated = fromOpenAiChat(JSONObject(fullBody), modelName)
                writeSyntheticStream(output, translated)
                return
            }

            val headers = "HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\nCache-Control: no-cache\r\nConnection: close\r\n\r\n"
            output.write(headers.toByteArray())
            fun emitEvent(name: String, data: JSONObject) {
                output.write("event: $name\ndata: $data\n\n".toByteArray())
                output.flush()
            }

            val msgId = "msg_${UUID.randomUUID()}"
            emitEvent(
                "message_start",
                JSONObject()
                    .put("type", "message_start")
                    .put(
                        "message",
                        JSONObject()
                            .put("id", msgId)
                            .put("type", "message")
                            .put("role", "assistant")
                            .put("model", modelName)
                            .put("content", JSONArray())
                            .put("stop_reason", JSONObject.NULL)
                            .put("stop_sequence", JSONObject.NULL)
                            .put("usage", JSONObject().put("input_tokens", 0).put("output_tokens", 0)),
                    ),
            )

            var nextBlockIndex = 0
            var activeBlockType: String? = null
            var activeBlockIndex = -1
            val toolSlotToBlock = mutableMapOf<Int, Int>()
            var sawToolCalls = false
            var inputTokens = 0
            var outputTokens = 0

            fun closeActiveNonToolBlock() {
                if (activeBlockType != null && activeBlockIndex >= 0) {
                    emitEvent("content_block_stop", JSONObject().put("type", "content_block_stop").put("index", activeBlockIndex))
                    activeBlockType = null
                    activeBlockIndex = -1
                }
            }

            BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                while (true) {
                    val rawLine = reader.readLine() ?: break
                    val line = rawLine.trim()
                    if (!line.startsWith("data:")) continue
                    val dataStr = line.removePrefix("data:").trim()
                    if (dataStr == "[DONE]" || dataStr.isEmpty()) continue
                    val chunk = runCatching { JSONObject(dataStr) }.getOrNull() ?: continue

                    chunk.optJSONObject("usage")?.let { usage ->
                        inputTokens = usage.optInt("prompt_tokens", inputTokens)
                        outputTokens = usage.optInt("completion_tokens", outputTokens)
                    }

                    val choice = chunk.optJSONArray("choices")?.optJSONObject(0) ?: continue
                    val delta = choice.optJSONObject("delta") ?: choice.optJSONObject("message") ?: continue

                    val reasoningDelta = delta.optString("reasoning_content").ifEmpty { delta.optString("reasoning") }
                    if (reasoningDelta.isNotEmpty() && reasoningDelta != "null") {
                        if (activeBlockType != "thinking") {
                            closeActiveNonToolBlock()
                            activeBlockIndex = nextBlockIndex++
                            activeBlockType = "thinking"
                            emitEvent(
                                "content_block_start",
                                JSONObject()
                                    .put("type", "content_block_start")
                                    .put("index", activeBlockIndex)
                                    .put("content_block", JSONObject().put("type", "thinking").put("thinking", "")),
                            )
                        }
                        emitEvent(
                            "content_block_delta",
                            JSONObject()
                                .put("type", "content_block_delta")
                                .put("index", activeBlockIndex)
                                .put("delta", JSONObject().put("type", "thinking_delta").put("thinking", reasoningDelta)),
                        )
                    }

                    val textDelta = if (delta.isNull("content")) "" else delta.optString("content")
                    if (textDelta.isNotEmpty()) {
                        if (activeBlockType != "text") {
                            closeActiveNonToolBlock()
                            activeBlockIndex = nextBlockIndex++
                            activeBlockType = "text"
                            emitEvent(
                                "content_block_start",
                                JSONObject()
                                    .put("type", "content_block_start")
                                    .put("index", activeBlockIndex)
                                    .put("content_block", JSONObject().put("type", "text").put("text", "")),
                            )
                        }
                        emitEvent(
                            "content_block_delta",
                            JSONObject()
                                .put("type", "content_block_delta")
                                .put("index", activeBlockIndex)
                                .put("delta", JSONObject().put("type", "text_delta").put("text", textDelta)),
                        )
                    }

                    val toolCalls = delta.optJSONArray("tool_calls")
                    if (toolCalls != null && toolCalls.length() > 0) {
                        closeActiveNonToolBlock()
                        sawToolCalls = true
                        for (i in 0 until toolCalls.length()) {
                            val tc = toolCalls.optJSONObject(i) ?: continue
                            val slot = tc.optInt("index", i)
                            val fn = tc.optJSONObject("function")
                            val blockIdx = toolSlotToBlock.getOrPut(slot) {
                                val allocated = nextBlockIndex++
                                val toolId = tc.optString("id").ifBlank { "tool_${UUID.randomUUID()}" }
                                val toolName = fn?.optString("name").orEmpty().ifBlank { "tool" }
                                emitEvent(
                                    "content_block_start",
                                    JSONObject()
                                        .put("type", "content_block_start")
                                        .put("index", allocated)
                                        .put(
                                            "content_block",
                                            JSONObject()
                                                .put("type", "tool_use")
                                                .put("id", toolId)
                                                .put("name", toolName)
                                                .put("input", JSONObject()),
                                        ),
                                )
                                allocated
                            }
                            val argFragment = fn?.optString("arguments").orEmpty()
                            if (argFragment.isNotEmpty()) {
                                emitEvent(
                                    "content_block_delta",
                                    JSONObject()
                                        .put("type", "content_block_delta")
                                        .put("index", blockIdx)
                                        .put("delta", JSONObject().put("type", "input_json_delta").put("partial_json", argFragment)),
                                )
                            }
                        }
                    }
                }
            }

            closeActiveNonToolBlock()
            toolSlotToBlock.values.sorted().forEach { idx ->
                emitEvent("content_block_stop", JSONObject().put("type", "content_block_stop").put("index", idx))
            }
            emitEvent(
                "message_delta",
                JSONObject()
                    .put("type", "message_delta")
                    .put(
                        "delta",
                        JSONObject()
                            .put("stop_reason", if (sawToolCalls) "tool_use" else "end_turn")
                            .put("stop_sequence", JSONObject.NULL),
                    )
                    .put("usage", JSONObject().put("input_tokens", inputTokens).put("output_tokens", outputTokens)),
            )
            emitEvent("message_stop", JSONObject().put("type", "message_stop"))
        } finally {
            connection.disconnect()
        }
    }

    internal fun toOpenAiChat(source: JSONObject, stream: Boolean = false): JSONObject {
        val target = JSONObject()
            .put("model", normalizeModel(profile.model))
            .put("stream", stream)
            .put("max_tokens", source.optInt("max_tokens", 4096))
        if (source.has("temperature")) target.put("temperature", source.get("temperature"))
        val messages = JSONArray()
        source.opt("system")?.let { system ->
            val text = when (system) {
                is JSONArray -> contentText(system)
                else -> system.toString()
            }
            if (text.isNotBlank()) messages.put(JSONObject().put("role", "system").put("content", text))
        }
        val sourceMessages = source.optJSONArray("messages") ?: JSONArray()
        for (index in 0 until sourceMessages.length()) {
            val message = sourceMessages.getJSONObject(index)
            val role = message.optString("role")
            val content = message.opt("content")
            if (content !is JSONArray) {
                messages.put(JSONObject().put("role", role).put("content", content ?: ""))
                continue
            }
            val text = contentText(content)
            val toolCalls = JSONArray()
            val toolResults = mutableListOf<JSONObject>()
            for (partIndex in 0 until content.length()) {
                val part = content.optJSONObject(partIndex) ?: continue
                when (part.optString("type")) {
                    "tool_use" -> toolCalls.put(
                        JSONObject().put("id", part.optString("id"))
                            .put("type", "function")
                            .put(
                                "function",
                                JSONObject()
                                    .put("name", part.optString("name"))
                                    .put("arguments", part.optJSONObject("input")?.toString() ?: "{}"),
                            ),
                    )
                    "tool_result" -> toolResults += JSONObject()
                        .put("role", "tool")
                        .put("tool_call_id", part.optString("tool_use_id"))
                        .put("content", valueText(part.opt("content")))
                }
            }
            if (text.isNotBlank() || toolCalls.length() > 0) {
                val converted = JSONObject().put("role", role).put("content", text.ifBlank { JSONObject.NULL })
                if (toolCalls.length() > 0) converted.put("tool_calls", toolCalls)
                messages.put(converted)
            }
            toolResults.forEach(messages::put)
        }
        target.put("messages", messages)
        source.optJSONArray("tools")?.let { tools ->
            val converted = JSONArray()
            for (index in 0 until tools.length()) {
                val tool = tools.getJSONObject(index)
                converted.put(
                    JSONObject().put("type", "function").put(
                        "function",
                        JSONObject()
                            .put("name", tool.optString("name"))
                            .put("description", tool.optString("description"))
                            .put("parameters", tool.optJSONObject("input_schema") ?: JSONObject().put("type", "object")),
                    ),
                )
            }
            target.put("tools", converted).put("tool_choice", "auto")
        }
        return target
    }

    internal fun toOpenAiResponses(source: JSONObject): JSONObject {
        val target = JSONObject()
            .put("model", normalizeModel(profile.model))
            .put("max_output_tokens", source.optInt("max_tokens", 4096))
        source.opt("system")?.let { system ->
            val text = if (system is JSONArray) contentText(system) else system.toString()
            if (text.isNotBlank()) target.put("instructions", text)
        }
        val inputItems = JSONArray()
        val sourceMessages = source.optJSONArray("messages") ?: JSONArray()
        for (index in 0 until sourceMessages.length()) {
            val message = sourceMessages.getJSONObject(index)
            val role = message.optString("role", "user")
            val content = message.opt("content")
            if (content !is JSONArray) {
                val textType = if (role == "assistant") "output_text" else "input_text"
                inputItems.put(
                    JSONObject()
                        .put("role", role)
                        .put("content", JSONArray().put(JSONObject().put("type", textType).put("text", content?.toString().orEmpty()))),
                )
                continue
            }
            val textParts = JSONArray()
            for (partIndex in 0 until content.length()) {
                val part = content.optJSONObject(partIndex) ?: continue
                when (part.optString("type")) {
                    "text" -> {
                        val textType = if (role == "assistant") "output_text" else "input_text"
                        textParts.put(JSONObject().put("type", textType).put("text", part.optString("text")))
                    }
                    "tool_use" -> inputItems.put(
                        JSONObject()
                            .put("type", "function_call")
                            .put("call_id", part.optString("id"))
                            .put("name", part.optString("name"))
                            .put("arguments", part.optJSONObject("input")?.toString() ?: "{}"),
                    )
                    "tool_result" -> inputItems.put(
                        JSONObject()
                            .put("type", "function_call_output")
                            .put("call_id", part.optString("tool_use_id"))
                            .put("output", valueText(part.opt("content"))),
                    )
                }
            }
            if (textParts.length() > 0) {
                inputItems.put(JSONObject().put("role", role).put("content", textParts))
            }
        }
        target.put("input", inputItems)
        source.optJSONArray("tools")?.let { tools ->
            val converted = JSONArray()
            for (index in 0 until tools.length()) {
                val tool = tools.getJSONObject(index)
                converted.put(
                    JSONObject()
                        .put("type", "function")
                        .put("name", tool.optString("name"))
                        .put("description", tool.optString("description"))
                        .put("parameters", tool.optJSONObject("input_schema") ?: JSONObject().put("type", "object")),
                )
            }
            target.put("tools", converted)
        }
        return target
    }

    internal fun fromOpenAiChat(source: JSONObject, model: String): JSONObject {
        val message = source.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message") ?: JSONObject()
        val content = JSONArray()
        val reasoning = message.optString("reasoning_content").ifBlank { message.optString("reasoning") }
        if (reasoning.isNotBlank() && reasoning != "null") {
            content.put(JSONObject().put("type", "thinking").put("thinking", reasoning))
        }
        val text = if (message.isNull("content")) "" else message.optString("content")
        if (text.isNotBlank()) content.put(JSONObject().put("type", "text").put("text", text))
        val calls = message.optJSONArray("tool_calls") ?: JSONArray()
        for (index in 0 until calls.length()) {
            val call = calls.getJSONObject(index)
            val function = call.optJSONObject("function") ?: JSONObject()
            val arguments = runCatching { JSONObject(function.optString("arguments", "{}")) }.getOrDefault(JSONObject())
            content.put(
                JSONObject()
                    .put("type", "tool_use")
                    .put("id", call.optString("id").ifBlank { "tool_${UUID.randomUUID()}" })
                    .put("name", function.optString("name"))
                    .put("input", arguments),
            )
        }
        val usage = source.optJSONObject("usage") ?: JSONObject()
        return JSONObject()
            .put("id", source.optString("id").ifBlank { "msg_${UUID.randomUUID()}" })
            .put("type", "message")
            .put("role", "assistant")
            .put("model", model)
            .put("content", content)
            .put("stop_reason", if (calls.length() > 0) "tool_use" else "end_turn")
            .put("stop_sequence", JSONObject.NULL)
            .put(
                "usage",
                JSONObject()
                    .put("input_tokens", usage.optInt("prompt_tokens"))
                    .put("output_tokens", usage.optInt("completion_tokens")),
            )
    }

    internal fun fromOpenAiResponses(source: JSONObject, model: String): JSONObject {
        val content = JSONArray()
        var sawToolCall = false
        val outputs = source.optJSONArray("output") ?: JSONArray()
        for (i in 0 until outputs.length()) {
            val item = outputs.optJSONObject(i) ?: continue
            when (item.optString("type")) {
                "reasoning" -> {
                    val summaryArr = item.optJSONArray("summary")
                    val summaryText = buildString {
                        if (summaryArr != null) {
                            for (j in 0 until summaryArr.length()) {
                                append(summaryArr.optJSONObject(j)?.optString("text").orEmpty())
                            }
                        }
                    }
                    if (summaryText.isNotBlank()) {
                        content.put(JSONObject().put("type", "thinking").put("thinking", summaryText))
                    }
                }
                "message" -> {
                    val parts = item.optJSONArray("content") ?: JSONArray()
                    for (j in 0 until parts.length()) {
                        val p = parts.optJSONObject(j) ?: continue
                        val txt = p.optString("text")
                        if (txt.isNotBlank()) content.put(JSONObject().put("type", "text").put("text", txt))
                    }
                }
                "function_call" -> {
                    sawToolCall = true
                    val arguments = runCatching { JSONObject(item.optString("arguments", "{}")) }.getOrDefault(JSONObject())
                    content.put(
                        JSONObject()
                            .put("type", "tool_use")
                            .put("id", item.optString("call_id").ifBlank { item.optString("id").ifBlank { "tool_${UUID.randomUUID()}" } })
                            .put("name", item.optString("name"))
                            .put("input", arguments),
                    )
                }
            }
        }
        if (content.length() == 0) {
            source.optString("output_text").takeIf(String::isNotBlank)?.let {
                content.put(JSONObject().put("type", "text").put("text", it))
            }
        }
        val usage = source.optJSONObject("usage") ?: JSONObject()
        return JSONObject()
            .put("id", source.optString("id").ifBlank { "msg_${UUID.randomUUID()}" })
            .put("type", "message")
            .put("role", "assistant")
            .put("model", model)
            .put("content", content)
            .put("stop_reason", if (sawToolCall) "tool_use" else "end_turn")
            .put("stop_sequence", JSONObject.NULL)
            .put(
                "usage",
                JSONObject()
                    .put("input_tokens", usage.optInt("input_tokens", usage.optInt("prompt_tokens")))
                    .put("output_tokens", usage.optInt("output_tokens", usage.optInt("completion_tokens"))),
            )
    }

    private fun callProviderJson(endpoint: String, body: JSONObject): Pair<Int, String> {
        if (!com.jarves.mh.network.LocalNetwork.isPlainHttpAllowed(endpoint)) {
            return 400 to errorJson("invalid_request_error", "Plain http:// is only allowed for local network addresses")
        }
        val connection = openConfiguredConnection(endpoint)
        return try {
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            code to stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        } finally {
            connection.disconnect()
        }
    }

    private fun writeSyntheticStream(output: BufferedOutputStream, message: JSONObject) {
        val headers = "HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\nCache-Control: no-cache\r\nConnection: close\r\n\r\n"
        output.write(headers.toByteArray())
        fun event(name: String, data: JSONObject) { output.write("event: $name\ndata: $data\n\n".toByteArray()) }
        val content = message.getJSONArray("content")
        event(
            "message_start",
            JSONObject()
                .put("type", "message_start")
                .put("message", JSONObject(message.toString()).put("content", JSONArray()).put("stop_reason", JSONObject.NULL)),
        )
        for (index in 0 until content.length()) {
            val block = content.getJSONObject(index)
            val type = block.getString("type")
            val start = when (type) {
                "thinking" -> JSONObject().put("type", "thinking").put("thinking", "")
                "text" -> JSONObject().put("type", "text").put("text", "")
                else -> JSONObject().put("type", "tool_use").put("id", block.getString("id")).put("name", block.getString("name")).put("input", JSONObject())
            }
            event("content_block_start", JSONObject().put("type", "content_block_start").put("index", index).put("content_block", start))
            val delta = when (type) {
                "thinking" -> JSONObject().put("type", "thinking_delta").put("thinking", block.getString("thinking"))
                "text" -> JSONObject().put("type", "text_delta").put("text", block.getString("text"))
                else -> JSONObject().put("type", "input_json_delta").put("partial_json", block.getJSONObject("input").toString())
            }
            event("content_block_delta", JSONObject().put("type", "content_block_delta").put("index", index).put("delta", delta))
            event("content_block_stop", JSONObject().put("type", "content_block_stop").put("index", index))
        }
        event(
            "message_delta",
            JSONObject()
                .put("type", "message_delta")
                .put("delta", JSONObject().put("stop_reason", message.getString("stop_reason")).put("stop_sequence", JSONObject.NULL))
                .put("usage", message.getJSONObject("usage")),
        )
        event("message_stop", JSONObject().put("type", "message_stop"))
        output.flush()
    }

    private fun writeJson(output: BufferedOutputStream, code: Int, body: String) {
        val bytes = body.toByteArray()
        val reason = if (code in 200..299) "OK" else "Error"
        output.write("HTTP/1.1 $code $reason\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
        output.write(bytes)
        output.flush()
    }

    private fun isAuthorized(headers: Map<String, String>): Boolean {
        val presented = headers["x-api-key"]
            ?: headers["authorization"]?.removePrefix("Bearer ")?.removePrefix("bearer ")?.trim()
            ?: return false
        return MessageDigest.isEqual(presented.toByteArray(), apiKey.toByteArray())
    }

    private fun readLine(input: BufferedInputStream): String? {
        val bytes = ArrayList<Byte>()
        while (bytes.size <= MAX_HEADER_LINE_BYTES) {
            val value = input.read()
            if (value < 0) return if (bytes.isEmpty()) null else bytes.toByteArray().decodeToString()
            if (value == '\n'.code) return bytes.toByteArray().decodeToString().trimEnd('\r')
            bytes += value.toByte()
        }
        return null
    }

    private fun contentText(array: JSONArray): String = buildString {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            if (item.optString("type") == "text") append(item.optString("text"))
        }
    }

    private fun valueText(value: Any?): String = when (value) {
        is String -> value
        is JSONArray -> contentText(value).ifBlank { value.toString() }
        null, JSONObject.NULL -> ""
        else -> value.toString()
    }

    private fun providerError(body: String): String = runCatching {
        JSONObject(body).optJSONObject("error")?.optString("message").orEmpty().ifBlank { body.take(500) }
    }.getOrDefault(body.take(500))

    private fun normalizeModel(model: String): String = model
        .removePrefix("models/")
        .substringBefore('[')
        .trim()

    private fun errorJson(type: String, message: String) =
        JSONObject().put("type", "error").put("error", JSONObject().put("type", type).put("message", message)).toString()

    override fun close() {
        running.set(false)
        runCatching { server.close() }
    }

    private companion object {
        const val MAX_REQUEST_BYTES = 12 * 1024 * 1024
        const val MAX_HEADER_LINE_BYTES = 16 * 1024
        const val SOCKET_TIMEOUT_MS = 30_000
    }
}
