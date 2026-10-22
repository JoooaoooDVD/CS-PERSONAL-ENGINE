package com.jarves.mh.runtime

import com.jarves.mh.model.AgentKind
import com.jarves.mh.model.ENGINE_SLASH_COMMANDS
import com.jarves.mh.model.OPENAI_COMPATIBLE_PRESETS
import com.jarves.mh.model.ProviderKind
import com.jarves.mh.model.ProviderProfile
import com.jarves.mh.model.ProviderProtocol
import com.jarves.mh.model.providerProtocolForAgent
import com.jarves.mh.model.providersForAgent
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsPersonalEngineUpgradeTest {

    @Test
    fun openAiCustomAvailableForClaudeCodeAndDeepSeekHarness() {
        assertTrue(ProviderKind.OPENAI_CUSTOM in providersForAgent(AgentKind.CLAUDE_CODE))
        assertTrue(ProviderKind.OPENAI_CUSTOM in providersForAgent(AgentKind.DEEPSEEK_HARNESS))
        assertTrue(OPENAI_COMPATIBLE_PRESETS.size >= 8)
    }

    @Test
    fun openAiCustomNormalizesLocalBaseUrlForDshAndResolvesProtocols() {
        val ollamaProfile = ProviderProfile(
            kind = ProviderKind.OPENAI_CUSTOM,
            baseUrl = "http://127.0.0.1:11434",
            model = "qwen2.5-coder:7b",
            dshApi = "openai-completions",
        )
        assertEquals(ProviderProtocol.OPENAI_CHAT, ollamaProfile.effectiveClaudeProtocol)
        assertEquals(ProviderProtocol.OPENAI_CHAT, providerProtocolForAgent(ollamaProfile, AgentKind.CLAUDE_CODE))

        val route = DshRouteMapper.forProfile(ollamaProfile)
        assertEquals("openai-completions", route.custom?.api)
        assertEquals("http://127.0.0.1:11434/v1", route.custom?.baseUrl)

        val responsesProfile = ollamaProfile.copy(dshApi = "openai-responses")
        assertEquals(ProviderProtocol.OPENAI_RESPONSES, responsesProfile.effectiveClaudeProtocol)
    }

    @Test
    fun customHeadersParseCleanlyAndStripCrlf() {
        val profile = ProviderProfile(
            kind = ProviderKind.OPENAI_CUSTOM,
            customHeaders = """
                # gateway headers
                CF-Access-Client-Id: id-123
                X-Custom-Token : secret-xyz
                MalformedLineWithoutColon
            """.trimIndent(),
        )
        assertEquals(
            mapOf(
                "CF-Access-Client-Id" to "id-123",
                "X-Custom-Token" to "secret-xyz",
            ),
            profile.parsedCustomHeaders,
        )
    }

    @Test
    fun localFormatGatewayStreamsOpenAiSseToAnthropicEventsWithCustomHeaders() {
        var capturedHeader: String? = null
        var capturedPath: String? = null
        ServerSocket(0).use { upstream ->
            val upstreamThread = thread(isDaemon = true) {
                upstream.accept().use { client ->
                    val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
                    capturedPath = reader.readLine()?.split(" ")?.getOrNull(1)
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                        if (line.startsWith("X-Engine-Test:", ignoreCase = true)) {
                            capturedHeader = line.substringAfter(':').trim()
                        }
                    }
                    val ssePayload = buildString {
                        append("data: {\"choices\":[{\"delta\":{\"reasoning_content\":\"Analyzing \"}}]}\n\n")
                        append("data: {\"choices\":[{\"delta\":{\"content\":\"Hello CS Engine!\"}}]}\n\n")
                        append("data: {\"choices\":[{\"delta\":{},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":12,\"completion_tokens\":4}}\n\n")
                        append("data: [DONE]\n\n")
                    }.toByteArray(StandardCharsets.UTF_8)

                    val headers = "HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\nContent-Length: ${ssePayload.size}\r\nConnection: close\r\n\r\n"
                    client.getOutputStream().apply {
                        write(headers.toByteArray(StandardCharsets.UTF_8))
                        write(ssePayload)
                        flush()
                    }
                }
            }

            val profile = ProviderProfile(
                kind = ProviderKind.OPENAI_CUSTOM,
                baseUrl = "http://127.0.0.1:${upstream.localPort}/v1",
                model = "gpt-4o",
                dshApi = "openai-completions",
                customHeaders = "X-Engine-Test: verified-header",
            )
            val gateway = LocalFormatGateway(profile, apiKey = "no-key-required").start()
            try {
                val gwPort = gateway.url.substringAfterLast(':').toInt()
                val reqBody = JSONObject()
                    .put("model", "claude-sonnet-4-6")
                    .put("stream", true)
                    .put(
                        "messages",
                        JSONArray().put(JSONObject().put("role", "user").put("content", "Hi")),
                    )
                    .toString()
                    .toByteArray(StandardCharsets.UTF_8)

                val responseText = Socket("127.0.0.1", gwPort).use { sock ->
                    sock.soTimeout = 5_000
                    val req = "POST /v1/messages HTTP/1.1\r\n" +
                        "Host: 127.0.0.1\r\n" +
                        "x-api-key: no-key-required\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: ${reqBody.size}\r\n\r\n"
                    sock.getOutputStream().apply {
                        write(req.toByteArray(StandardCharsets.UTF_8))
                        write(reqBody)
                        flush()
                    }
                    sock.getInputStream().bufferedReader(StandardCharsets.UTF_8).readText()
                }
                upstreamThread.join(3_000)

                assertEquals("/v1/chat/completions", capturedPath)
                assertEquals("verified-header", capturedHeader)
                assertTrue(responseText.contains("thinking_delta"))
                assertTrue(responseText.contains("Analyzing "))
                assertTrue(responseText.contains("Hello CS Engine!"))
                assertTrue(responseText.contains("message_stop"))
            } finally {
                gateway.close()
            }
        }
    }

    @Test
    fun openRouterRoutingGatewayRejectsUnauthenticatedLoopbackRequests() {
        val gateway = OpenRouterRoutingGateway(
            ProviderProfile(ProviderKind.LLM_ROUTER, openRouterProviderOrder = "anthropic"),
            apiKey = "expected-router-key",
        ).start()
        try {
            val port = gateway.url.substringAfterLast(':').toInt()
            val statusLine = Socket("127.0.0.1", port).use { sock ->
                sock.soTimeout = 5_000
                sock.getOutputStream().apply {
                    write("POST /api/v1/messages HTTP/1.1\r\nHost: 127.0.0.1\r\nContent-Length: 2\r\n\r\n{}".toByteArray())
                    flush()
                }
                sock.getInputStream().bufferedReader().readLine().orEmpty()
            }
            assertTrue("Expected 401 status line, got: $statusLine", statusLine.contains("401"))
        } finally {
            gateway.close()
        }
    }

    @Test
    fun gitWorkspaceParserExtractsBranchRemoteAheadBehindAndModifiedFiles() {
        val raw = """
            ===BRANCH===
            feat/cs-engine
            ===BRANCHES===
            main
            feat/cs-engine
            ===REMOTE===
            https://github.com/user/cs-personal-engine.git
            ===AHEAD_BEHIND===
            1 3
            ===LAST_COMMIT===
            a1b2c3d · feat: add live engine hud (2 minutes ago)
            ===STATUS===
             M app/src/main/MainActivity.kt
            A  app/src/main/NewFeature.kt
            ?? README_NOTES.md
        """.trimIndent()

        val snap = GitWorkspaceManager.parseGitStatusOutput(raw)
        assertTrue(snap.initialized)
        assertEquals("feat/cs-engine", snap.branch)
        assertEquals(listOf("main", "feat/cs-engine"), snap.branches)
        assertEquals("https://github.com/user/cs-personal-engine.git", snap.remoteUrl)
        assertEquals(3, snap.aheadCount)
        assertEquals(1, snap.behindCount)
        assertEquals(3, snap.modifiedFiles.size)
        assertFalse(snap.modifiedFiles[0].staged)
        assertTrue(snap.modifiedFiles[1].staged)
        assertEquals("??", snap.modifiedFiles[2].status)
    }

    @Test
    fun slashCommandsCoverCoreWorkflows() {
        val slashes = ENGINE_SLASH_COMMANDS.map { it.command }.toSet()
        assertTrue("/push" in slashes)
        assertTrue("/fix" in slashes)
        assertTrue("/run" in slashes)
        assertTrue("/ci" in slashes)
    }
}
