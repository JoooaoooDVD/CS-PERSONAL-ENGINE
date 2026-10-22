package com.jarves.mh.runtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile

/**
 * Persistent, unrestricted interactive Linux PTY shell session (`bash --login -i`) backed by
 * [NativeSpawnProcess] with a real Unix pseudo-terminal (`posix_openpt` / `TIOCSWINSZ`).
 *
 * Unlike one-shot `bash -c` invocations, this session keeps shell state (`cd`, `export`,
 * virtualenvs, SSH sessions, interactive REPLs like `python3`/`node`, and interactive AI CLIs
 * like `codex`, `gemini`, `opencode`, `aider`, `qwen`, `gh`, `tmux`) alive continuously.
 */
class InteractivePtySession(
    private val installer: RuntimeInstaller,
    private val scope: CoroutineScope,
    private val workspaceDir: File,
    private val guestWorkspacePath: String = "/workspace",
    private val onPreviewUrlDetected: ((String) -> Unit)? = null,
) {
    private val _buffer = MutableStateFlow("")
    val buffer: StateFlow<String> = _buffer.asStateFlow()

    private val _alive = MutableStateFlow(false)
    val alive: StateFlow<Boolean> = _alive.asStateFlow()

    private var process: Process? = null
    private var readerJob: Job? = null
    private val rawTranscript = StringBuilder()

    @Synchronized
    fun ensureStarted(): Boolean {
        val existing = process
        if (existing != null && existing.isAlive) {
            _alive.value = true
            return true
        }
        if (!installer.isInstalled()) return false
        return runCatching {
            val runtime = installer.installedRuntime()
            workspaceDir.mkdirs()
            val initRc = """
                export DEBIAN_FRONTEND=noninteractive
                export APT_LISTCHANGES_FRONTEND=none
                export PIP_BREAK_SYSTEM_PACKAGES=1
                export NPM_CONFIG_PREFIX=/root/.npm-global
                export PATH="/root/.local/bin:/root/.npm-global/bin:/root/.bun/bin:/root/.cargo/bin:/root/.deno/bin:/root/go/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:${'$'}PATH"
                export PS1='root@cs-engine:\w# '
                cd ${shellSingleQuote(guestWorkspacePath)} 2>/dev/null || true
                exec /usr/bin/bash --norc --noprofile -i
            """.trimIndent()
            val spawned = installer.process(
                proot = runtime.proot,
                rootfs = runtime.rootfs,
                workspace = workspaceDir,
                environment = mapOf(
                    "PS1" to "root@cs-engine:\\w# ",
                    "TERM" to "xterm-256color",
                    "COLORTERM" to "truecolor",
                ),
                guestCommand = listOf("/usr/bin/bash", "-lc", initRc),
                guestWorkspacePath = guestWorkspacePath,
                pseudoTerminal = true,
                ptyRows = 42,
                ptyColumns = 110,
            )
            process = spawned
            _alive.value = true
            startReaderLoop(spawned)
            true
        }.getOrDefault(false)
    }

    private fun startReaderLoop(proc: Process) {
        readerJob?.cancel()
        val native = proc as? NativeSpawnProcess ?: return
        readerJob = scope.launch(Dispatchers.IO) {
            var offset = 0L
            while (isActive && (proc.isAlive || native.outputFile.length() > offset)) {
                val available = native.outputFile.length() - offset
                if (available <= 0L) {
                    delay(35)
                    continue
                }
                val chunkSize = minOf(available, 16_384L).toInt()
                val bytes = ByteArray(chunkSize)
                val count = runCatching {
                    RandomAccessFile(native.outputFile, "r").use { raf ->
                        raf.seek(offset)
                        raf.read(bytes)
                    }
                }.getOrDefault(0)
                if (count > 0) {
                    offset += count
                    val decoded = bytes.decodeToString(0, count)
                    synchronized(rawTranscript) {
                        rawTranscript.append(decoded)
                        if (rawTranscript.length > MAX_TRANSCRIPT_CHARS * 2) {
                            rawTranscript.delete(0, rawTranscript.length - MAX_TRANSCRIPT_CHARS)
                        }
                        val cleaned = stripPtyAnsi(rawTranscript.toString()).takeLast(MAX_TRANSCRIPT_CHARS)
                        _buffer.value = cleaned
                        detectHttpUrl(cleaned)?.let { url -> onPreviewUrlDetected?.invoke(url) }
                    }
                }
            }
            _alive.value = false
        }
    }

    fun sendLine(command: String) {
        if (!ensureStarted()) return
        val normalized = normalizeAptForRootPty(command)
        sendRaw(normalized + "\n")
    }

    fun sendRaw(raw: String) {
        val active = process
        if (active == null || !active.isAlive) {
            if (!ensureStarted()) return
        }
        val target = process ?: return
        scope.launch(Dispatchers.IO) {
            runCatching {
                target.outputStream.write(raw.toByteArray(Charsets.UTF_8))
                target.outputStream.flush()
            }
        }
    }

    fun sendInterrupt() {
        val active = process ?: return
        scope.launch(Dispatchers.IO) {
            runCatching {
                // Send ETX (^C) over PTY master AND SIGINT to foreground process group
                active.outputStream.write(byteArrayOf(3))
                active.outputStream.flush()
                (active as? NativeSpawnProcess)?.interrupt()
            }
        }
    }

    fun clearBuffer() {
        synchronized(rawTranscript) {
            rawTranscript.clear()
            _buffer.value = ""
        }
        if (process?.isAlive == true) {
            sendRaw("\n")
        }
    }

    @Synchronized
    fun restart() {
        close()
        ensureStarted()
    }

    @Synchronized
    fun close() {
        readerJob?.cancel()
        readerJob = null
        runCatching { process?.destroy() }
        process = null
        _alive.value = false
    }

    companion object {
        private const val MAX_TRANSCRIPT_CHARS = 48_000

        internal fun normalizeAptForRootPty(command: String): String {
            val trimmed = command.trim()
            if (trimmed.isEmpty()) return command
            return command
                .replace(Regex("(?<![\\w-])sudo\\s+apt(?:-get)?\\s+"), "apt-get ")
                .replace(
                    Regex("(?<![\\w-])apt(?:-get)?\\s+(install|upgrade|full-upgrade|dist-upgrade|remove|autoremove|fix-broken)\\b"),
                    "DEBIAN_FRONTEND=noninteractive apt-get -y -o Dpkg::Options::=--force-confold $1",
                )
        }

        internal fun stripPtyAnsi(input: String): String {
            if (input.isEmpty()) return ""
            // Handle clear-screen sequences (\u001B[2J or \u001Bc) by keeping text after the last clear
            val afterClear = input
                .substringAfterLast("\u001B[2J")
                .substringAfterLast("\u001Bc")
            val withoutOsc = afterClear.replace(Regex("\u001B\\][^\\u0007\\u001B]*(?:\u0007|\u001B\\\\)"), "")
            val withoutCsi = withoutOsc.replace(Regex("\u001B\\[[0-9;?]*[ -/]*[@-~]"), "")
            val withoutSingleEsc = withoutCsi.replace(Regex("\u001B[@-Z\\\\-_]"), "")
            // Handle carriage-return progress bars (`\r` not followed by `\n`) cleanly per line.
            // Note: use `.split('\n')` rather than `lineSequence()` because `lineSequence()` splits on `\r` too!
            return withoutSingleEsc
                .replace("\r\n", "\n")
                .split('\n')
                .joinToString("\n") { rawLine ->
                    val line = rawLine.trimEnd('\r')
                    if ('\r' in line) line.substringAfterLast('\r') else line
                }
                .replace("\u0007", "")
        }

        private fun detectHttpUrl(output: String): String? {
            val match = Regex("https?://(?:localhost|127\\.0\\.0\\.1|0\\.0\\.0\\.0):(\\d{2,5})(?:/[^\\s]*)?")
                .findAll(output)
                .lastOrNull()
                ?: return null
            val port = match.groupValues[1].toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
            return "http://127.0.0.1:$port/"
        }

        private fun shellSingleQuote(value: String): String = "'${value.replace("'", "'\\''")}'"
    }
}
