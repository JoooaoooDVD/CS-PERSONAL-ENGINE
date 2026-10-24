package com.jarves.mh.runtime

import com.jarves.mh.model.AgentKind
import com.jarves.mh.model.BUILTIN_CLI_PACKAGES
import com.jarves.mh.model.ChatMessage
import com.jarves.mh.model.CliToolPackage
import com.jarves.mh.model.ENGINE_SLASH_COMMANDS
import com.jarves.mh.model.OPENAI_COMPATIBLE_PRESETS
import com.jarves.mh.model.ProviderKind
import com.jarves.mh.model.ProviderProfile
import com.jarves.mh.model.ProviderProtocol
import com.jarves.mh.model.SavedCustomProvider
import com.jarves.mh.model.providerProtocolForAgent
import com.jarves.mh.model.providersForAgent
import androidx.compose.ui.graphics.toArgb
import com.jarves.mh.ui.MinecraftLauncherKnowledge
import com.jarves.mh.ui.computeClaudeSessionBenchmark
import com.jarves.mh.ui.computeLineDiffStats
import com.jarves.mh.ui.theme.ClaudeDarkBg
import com.jarves.mh.ui.theme.ClaudeDarkSurface
import com.jarves.mh.ui.theme.ClaudeTerracotta
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.nio.file.Files
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
        emu.resize(40, 100)
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

    @Test
    fun vt100EmulatorSupportsOpenCodeColonSgrRepAndTerminalQueries() {
        val replies = mutableListOf<String>()
        val emu = Vt100TerminalEmulator(initialRows = 24, initialCols = 80).apply {
            onReplyToPty = { replies.add(it) }
        }

        // OpenCode / Ultraviolet startup queries: OSC 11;?, CSI 6n, DECRQM ?2026$p
        emu.feedText("\u001B]11;?\u0007\u001B[6n\u001B[?2026\$p")
        assertTrue(replies.any { it.startsWith("\u001B]11;rgb:") })
        assertTrue(replies.any { it == "\u001B[1;1R" })
        assertTrue(replies.any { it == "\u001B[?2026;2\$y" })

        // ITU-T T.416 colon TrueColor background + foreground + CSI b (REP repeat graphic char)
        emu.feedText("\u001B[?1049h\u001B[1;1H\u001B[48:2::14:18:24m\u001B[38:2:52:211:153m▀\u001B[5b \u001B[10b")
        val snap = emu.snapshot()
        assertEquals(24, snap.renderRows.size)
        val row0 = snap.renderRows[0]
        // '▀' printed once + repeated 5 times = 6 block glyphs at cols 0..5, then 11 spaces at cols 6..16 with dark bg!
        for (c in 0 until 6) {
            assertEquals('▀', row0.chars[c])
            assertEquals((0xFF shl 24) or (52 shl 16) or (211 shl 8) or 153, row0.fg[c])
            assertEquals((0xFF shl 24) or (14 shl 16) or (18 shl 8) or 24, row0.bg[c])
        }
        for (c in 6 until 17) {
            assertEquals(' ', row0.chars[c])
            assertEquals((0xFF shl 24) or (14 shl 16) or (18 shl 8) or 24, row0.bg[c])
        }
    }

    @Test
    fun minecraftLauncherSpecialistBrainProvidesJavaXmlKotlinArchitecture() {
        val brief = MinecraftLauncherKnowledge.buildSpecialistSystemBrief("Build a Minecraft launcher version selector in Java and XML")
        assertTrue("Java" in brief)
        assertTrue("Android XML" in brief)
        assertTrue("Kotlin" in brief)
        assertTrue("version_manifest_v2.json" in brief)
        assertTrue("libjli.so" in brief)
        assertTrue("LWJGL3" in brief)
        assertTrue("Minecraft Launcher Task" in brief)
        assertTrue(MinecraftLauncherKnowledge.blueprints.size >= 6)
    }

    @Test
    fun pureDarkClaudeCodePaletteAndOsc11BackgroundMatchV6Spec() {
        assertEquals(0xFF0C0C0E.toInt(), ClaudeDarkBg.toArgb())
        assertEquals(0xFF151518.toInt(), ClaudeDarkSurface.toArgb())
        assertEquals(0xFFD97757.toInt(), ClaudeTerracotta.toArgb())

        val replies = mutableListOf<String>()
        val emu = Vt100TerminalEmulator(initialRows = 24, initialCols = 80).apply {
            onReplyToPty = { replies.add(it) }
        }
        emu.feedText("\u001B]11;?\u0007")
        assertTrue(replies.any { it.contains("0c0c/0c0c/0e0e") })
    }

    @Test
    fun v7WorkspaceCheckpointsExcludesPocketDevAndHandlesMultiFileDiffsSafely() {
        val tempRoot = Files.createTempDirectory("cs-engine-v7-ckpt").toFile()
        try {
            val workspace = File(tempRoot, "project").apply { mkdirs() }
            val storeDir = File(tempRoot, "store").apply { mkdirs() }
            File(workspace, ".pocketdev/imports").apply { mkdirs() }
            File(workspace, ".pocketdev/imports/huge.zip").writeBytes(ByteArray(4096) { 0x50 })
            File(workspace, "node_modules/pkg").apply { mkdirs() }
            File(workspace, "node_modules/pkg/index.js").writeText("module.exports = 1;")
            File(workspace, "src/Main.kt").apply {
                parentFile?.mkdirs()
                writeText("fun main() {\n    println(\"v6\")\n}\n")
            }

            val checkpoints = WorkspaceCheckpoints(storeDir)
            checkpoints.createCheckpoint("proj-1", workspace)
            val snap = checkpoints.snapshot(workspace)
            assertFalse(snap.keys.any { path -> path.startsWith(".pocketdev") || path.startsWith("node_modules") })
            assertTrue("src/Main.kt" in snap.keys)

            File(workspace, "src/Main.kt").writeText("fun main() {\n    println(\"v7\")\n    println(\"fast\")\n}\n")
            File(workspace, "src/NewFeature.kt").writeText("class NewFeature\n")
            val details = checkpoints.buildChangeDetails("proj-1", workspace, listOf("src/Main.kt", "src/NewFeature.kt"))
            assertEquals(2, details.size)
            val mainDiff = details.first { it.path == "src/Main.kt" }
            assertEquals(2, mainDiff.additions)
            assertEquals(1, mainDiff.deletions)
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun v7InstalledCliDiscoveryAndCommandBuilderSupportAllInstalledClis() {
        val fakeRootfs = Files.createTempDirectory("cs-engine-v7-rootfs").toFile()
        try {
            val binDir = File(fakeRootfs, "usr/local/bin").apply { mkdirs() }
            File(binDir, "opencode").writeText("#!/bin/sh\necho ok\n")
            File(binDir, "codex").writeText("#!/bin/sh\necho ok\n")
            val customCli = CliToolPackage(
                id = "custom-my-agent",
                name = "My Custom Agent",
                category = "Custom CLI",
                description = "Custom agent",
                installCommand = "npm i -g my-agent",
                launchCommand = "my-agent",
                checkBinary = "my-agent",
                isCustom = true,
            )
            File(binDir, "my-agent").writeText("#!/bin/sh\necho ok\n")

            val discovered = InstalledCliRuntimeBridge.discoverInstalledCliIds(fakeRootfs, listOf(customCli))
            assertTrue("opencode-cli" in discovered)
            assertTrue("openai-codex" in discovered)
            assertTrue("custom-my-agent" in discovered)
            assertFalse("aider-cli" in discovered)

            val openCodeScript = InstalledCliRuntimeBridge.buildCliExecutionScript(
                BUILTIN_CLI_PACKAGES.first { it.id == "opencode-cli" },
                "/tmp/p.txt",
                "/workspace",
            )
            assertTrue("opencode run" in openCodeScript)

            val codexScript = InstalledCliRuntimeBridge.buildCliExecutionScript(
                BUILTIN_CLI_PACKAGES.first { it.id == "openai-codex" },
                "/tmp/p.txt",
                "/workspace",
            )
            assertTrue("codex exec" in codexScript)
        } finally {
            fakeRootfs.deleteRecursively()
        }
    }

    @Test
    fun v7ClaudeContextBenchmarkAndCompactSlashCommandWorkAccurately() {
        assertTrue(ENGINE_SLASH_COMMANDS.any { it.command == "/compact" })

        val msgs = listOf(
            ChatMessage(fromUser = true, text = "a".repeat(8_000)),
            ChatMessage(fromUser = false, text = "b".repeat(12_000), workedMillis = 2_500L),
        )
        val stats = computeClaudeSessionBenchmark(
            messages = msgs,
            streamingPreview = null,
            pendingAttachments = emptyList(),
            activeModelLabel = "claude-sonnet-4-6",
            taskStartedAtMillis = 1_000L,
            taskFinishedAtMillis = 3_500L,
            isRunning = false,
        )
        assertEquals(200_000, stats.maxContextTokens)
        assertEquals(2_000, stats.promptTokens)
        assertEquals(3_000, stats.responseTokens)
        assertTrue(stats.usedPercent > 2.5f && stats.usedPercent < 5.0f)
        assertTrue(stats.tokensPerSecond > 500f)
    }
}
