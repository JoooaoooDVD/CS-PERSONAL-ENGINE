package com.jarves.mh.runtime

import com.jarves.mh.model.AgentKind
import com.jarves.mh.model.BUILTIN_CLI_PACKAGES
import com.jarves.mh.model.ENGINE_SLASH_COMMANDS
import com.jarves.mh.model.OPENAI_COMPATIBLE_PRESETS
import com.jarves.mh.model.ProviderKind
import com.jarves.mh.model.ProviderProfile
import com.jarves.mh.model.ProviderProtocol
import com.jarves.mh.model.SavedCustomProvider
import com.jarves.mh.model.providerProtocolForAgent
import com.jarves.mh.model.providersForAgent
import com.jarves.mh.ui.computeLineDiffStats
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
        assertTrue("/cli" in slashes)
    }

    @Test
    fun savedCustomProviderMapsToCorrectOpenAiAndAnthropicProfiles() {
        val groqCustom = SavedCustomProvider(
            id = "groq-1",
            name = "My Groq Fast",
            baseUrl = "https://api.groq.com/openai/v1",
            model = "llama-3.3-70b-versatile",
            wireProtocol = "openai-completions",
            customHeaders = "X-Team: dev",
        )
        val groqProfile = groqCustom.toProviderProfile()
        assertEquals(ProviderKind.OPENAI_CUSTOM, groqProfile.kind)
        assertEquals(ProviderProtocol.OPENAI_CHAT, groqProfile.effectiveClaudeProtocol)
        assertEquals("CUSTOM_PROVIDER_groq-1", groqCustom.vaultKeyId)
        assertEquals(mapOf("X-Team" to "dev"), groqProfile.parsedCustomHeaders)

        val anthropicCustom = SavedCustomProvider(
            id = "anth-1",
            name = "Custom Anthropic Proxy",
            baseUrl = "https://proxy.example.com/anthropic",
            model = "claude-sonnet-4-6",
            wireProtocol = "anthropic-messages",
        )
        val anthProfile = anthropicCustom.toProviderProfile()
        assertEquals(ProviderKind.CUSTOM, anthProfile.kind)
        assertEquals(ProviderProtocol.ANTHROPIC_GATEWAY, anthProfile.effectiveClaudeProtocol)
    }

    @Test
    fun interactivePtySessionStripsAnsiAndNormalizesRootAptCommands() {
        val rawPty = "\u001B[32mroot@cs-engine:~#\u001B[0m npm -v\r\nDownloading 10%\rDownloading 100%\r\n10.8.2\r\n"
        val clean = InteractivePtySession.stripPtyAnsi(rawPty)
        assertTrue("root@cs-engine:~# npm -v" in clean)
        assertTrue("Downloading 100%" in clean)
        assertFalse("Downloading 10%" in clean)
        assertTrue("10.8.2" in clean)

        val normalizedApt = InteractivePtySession.normalizeAptForRootPty("sudo apt install ripgrep jq")
        assertTrue("DEBIAN_FRONTEND=noninteractive apt-get -y" in normalizedApt)
        assertTrue("ripgrep jq" in normalizedApt)
    }

    @Test
    fun builtinCliMarketplaceCoversMajorCodingAgentsAndLinuxToolchains() {
        val ids = BUILTIN_CLI_PACKAGES.map { it.id }.toSet()
        assertTrue("openai-codex" in ids)
        assertTrue("google-gemini-cli" in ids)
        assertTrue("opencode-cli" in ids)
        assertTrue("aider-cli" in ids)
        assertTrue("qwen-code-cli" in ids)
        assertTrue("github-cli" in ids)
        assertTrue("linux-power-pack" in ids)
    }

    @Test
    fun vt100TerminalEmulatorHandlesTypingBackspaceColorsAltScreenAndResize() {
        val emu = Vt100TerminalEmulator(initialRows = 24, initialCols = 80)

        // 1. Colored prompt + direct typing + kernel backspace (`\b \b` / `\b\e[K`) + Enter (`\r\n`)
        emu.feedText("\u001B]0;root@cs-engine:/workspace\u0007")
        emu.feedText("\u001B[?2004h")
        emu.feedText("\u001B[1;32mroot@cs-engine\u001B[0m:\u001B[1;36m/workspace\u001B[0m# git sttaus\b\b\b\batus\r\nOn branch main\r\n")
        val snap1 = emu.snapshot(showBlockCursor = true)
        assertEquals("root@cs-engine:/workspace", snap1.windowTitle)
        assertTrue(snap1.bracketedPaste)
        assertFalse(snap1.alternateScreen)
        assertTrue("root@cs-engine:/workspace# git status" in snap1.plainText)
        assertTrue("On branch main" in snap1.plainText)

        // 2. Alternate Screen Buffer (`vim` / `htop` / `codex` TUI) preserves primary shell buffer!
        emu.feedText("\u001B[?1049h\u001B[2J\u001B[1;1H\u001B[38;2;52;211;153mNVIM v0.10.0\u001B[0m")
        val altSnap = emu.snapshot(showBlockCursor = true)
        assertTrue(altSnap.alternateScreen)
        assertTrue("NVIM v0.10.0" in altSnap.plainText)
        assertFalse("On branch main" in altSnap.plainText)

        // Exit alternate screen -> restores primary prompt & output intact!
        emu.feedText("\u001B[?1049l")
        val restoredSnap = emu.snapshot(showBlockCursor = true)
        assertFalse(restoredSnap.alternateScreen)
        assertTrue("On branch main" in restoredSnap.plainText)

        // 3. Dynamic viewport resize (`TIOCSWINSZ`)
        emu.resize(rows = 40, cols = 100)
        val resized = emu.snapshot()
        assertEquals(40, resized.rows)
        assertEquals(100, resized.cols)
    }

    @Test
    fun computeLineDiffStatsCalculatesExactAdditionsAndDeletions() {
        assertEquals(0 to 0, computeLineDiffStats("val a = 1\nval b = 2", "val a = 1\nval b = 2"))
        assertEquals(2 to 0, computeLineDiffStats("line1\nline2", "line1\nline2\nline3\nline4"))
        assertEquals(0 to 1, computeLineDiffStats("line1\nline2\nline3", "line1\nline3"))
        assertEquals(
            2 to 1,
            computeLineDiffStats(
                "fun greet() {\n    println(\"old\")\n}",
                "fun greet() {\n    println(\"new 1\")\n    println(\"new 2\")\n}",
            ),
        )
    }
}
