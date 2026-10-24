package com.jarves.mh.runtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Desktop-grade interactive Linux PTY session backed by [Vt100TerminalEmulator],
 * zero-polling kernel PTY streaming, live terminal query responses, and `SIGWINCH` resizing.
 */
class InteractivePtySession(
    val sessionId: String,
    val label: String,
    private val installer: RuntimeInstaller,
    private val scope: CoroutineScope,
    private val workspaceDir: File,
    private val guestWorkspacePath: String = "/workspace",
    private val onPreviewUrlDetected: ((String) -> Unit)? = null,
) {
    private val emulator = Vt100TerminalEmulator(initialRows = 32, initialCols = 112).also { emu ->
        emu.onReplyToPty = { reply ->
            val target = process
            if (target != null && target.isAlive) {
                val replyBytes = reply.toByteArray(Charsets.UTF_8)
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        target.outputStream.write(replyBytes)
                        target.outputStream.flush()
                    }
                }
            }
        }
    }

    private val _snapshot = MutableStateFlow(emulator.snapshot())
    val snapshot: StateFlow<TerminalScreenSnapshot> = _snapshot.asStateFlow()

    private val _buffer = MutableStateFlow("")
    val buffer: StateFlow<String> = _buffer.asStateFlow()

    private val _alive = MutableStateFlow(false)
    val alive: StateFlow<Boolean> = _alive.asStateFlow()

    @Volatile private var process: Process? = null
    private var snapshotJob: Job? = null
    private var livenessJob: Job? = null
    private val dirtySignal = Channel<Unit>(Channel.CONFLATED)

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
            val rows = emulator.rows
            val cols = emulator.cols
            // Important: unset COLUMNS/LINES env vars and enable checkwinsize so Go/BubbleTea/Node TUIs
            // (`opencode`, `claude`, `codex`, `nvim`) ALWAYS read live kernel TIOCGWINSZ dimensions!
            val initRc = """
                unset COLUMNS LINES
                stty sane echo icanon icrnl rows $rows cols $cols 2>/dev/null || true
                shopt -s checkwinsize 2>/dev/null || true
                export DEBIAN_FRONTEND=noninteractive
                export APT_LISTCHANGES_FRONTEND=none
                export PIP_BREAK_SYSTEM_PACKAGES=1
                export NPM_CONFIG_PREFIX=/root/.npm-global
                export LANG=C.UTF-8
                export LC_ALL=C.UTF-8
                export TERM=xterm-256color
                export COLORTERM=truecolor
                export PATH="/root/.local/bin:/root/.npm-global/bin:/root/.bun/bin:/root/.cargo/bin:/root/.deno/bin:/root/go/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:${'$'}PATH"
                alias ls='ls --color=auto'
                alias ll='ls -lah --color=auto'
                alias grep='grep --color=auto'
                export PS1='\[\033[38;5;246m\]\w\[\033[0m\] \[\033[38;2;217;119;87m\]❯\[\033[0m\] '
                cd ${shellSingleQuote(guestWorkspacePath)} 2>/dev/null || true
                exec /usr/bin/bash --norc --noprofile -i
            """.trimIndent()
            startSnapshotCoalescer()
            val spawned = installer.process(
                proot = runtime.proot,
                rootfs = runtime.rootfs,
                workspace = workspaceDir,
                environment = mapOf(
                    "TERM" to "xterm-256color",
                    "COLORTERM" to "truecolor",
                    "LANG" to "C.UTF-8",
                    "LC_ALL" to "C.UTF-8",
                ),
                guestCommand = listOf("/usr/bin/bash", "-lc", initRc),
                guestWorkspacePath = guestWorkspacePath,
                pseudoTerminal = true,
                ptyRows = rows,
                ptyColumns = cols,
                onLivePtyChunk = { bytes, len ->
                    emulator.feedBytes(bytes, len)
                    dirtySignal.trySend(Unit)
                },
            )
            process = spawned
            _alive.value = true
            startLivenessWatcher(spawned)
            true
        }.getOrDefault(false)
    }

    private fun startSnapshotCoalescer() {
        if (snapshotJob?.isActive == true) return
        snapshotJob = scope.launch(Dispatchers.Default) {
            for (signal in dirtySignal) {
                delay(8)
                // Respect DEC ?2026 synchronized output frame batching (up to 16ms max wait) so TUI redraws never tear!
                if (emulator.isSynchronizedOutputActive) {
                    delay(12)
                }
                while (dirtySignal.tryReceive().isSuccess) {
                    // Drain coalesced burst signals
                }
                val snap = emulator.snapshot(showBlockCursor = true)
                _snapshot.value = snap
                _buffer.value = snap.plainText
                detectHttpUrl(snap.plainText.takeLast(800))?.let { url ->
                    onPreviewUrlDetected?.invoke(url)
                }
            }
        }
    }

    private fun startLivenessWatcher(proc: Process) {
        livenessJob?.cancel()
        livenessJob = scope.launch(Dispatchers.IO) {
            while (isActive && proc.isAlive) {
                delay(400)
            }
            _alive.value = false
        }
    }

    fun resize(rows: Int, cols: Int) {
        val r = rows.coerceIn(10, 200)
        val c = cols.coerceIn(24, 300)
        if (r == emulator.rows && c == emulator.cols) {
            // Still ensure kernel PTY matches in case process started right before layout
            (process as? NativeSpawnProcess)?.resizePty(r, c)
            return
        }
        emulator.resize(r, c)
        (process as? NativeSpawnProcess)?.resizePty(r, c)
        dirtySignal.trySend(Unit)
    }

    /** Sends a full command line (used by CLI Store / Run-in-Terminal buttons). */
    fun sendLine(command: String) {
        if (!ensureStarted()) return
        val normalized = normalizeAptForRootPty(command)
        sendRaw(normalized + "\r")
    }

    /** Sends raw keystrokes/bytes directly to the PTY master fd with zero buffering delay. */
    fun sendRaw(raw: String) {
        if (raw.isEmpty()) return
        val active = process
        if (active == null || !active.isAlive) {
            if (!ensureStarted()) return
        }
        val target = process ?: return
        val bytes = raw.toByteArray(Charsets.UTF_8)
        scope.launch(Dispatchers.IO) {
            runCatching {
                target.outputStream.write(bytes)
                target.outputStream.flush()
            }
        }
    }

    fun pasteText(text: String) {
        if (text.isEmpty()) return
        val snap = _snapshot.value
        val payload = if (snap.bracketedPaste) {
            "\u001B[200~$text\u001B[201~"
        } else {
            text
        }
        sendRaw(payload)
    }

    fun sendInterrupt() {
        val active = process ?: return
        scope.launch(Dispatchers.IO) {
            runCatching {
                active.outputStream.write(byteArrayOf(3))
                active.outputStream.flush()
                (active as? NativeSpawnProcess)?.interrupt()
            }
        }
    }

    fun clearBuffer() {
        emulator.clearAll()
        dirtySignal.trySend(Unit)
        if (process?.isAlive == true) {
            sendRaw("\u000C")
        }
    }

    @Synchronized
    fun restart() {
        runCatching { process?.destroy() }
        process = null
        emulator.clearAll()
        dirtySignal.trySend(Unit)
        ensureStarted()
    }

    @Synchronized
    fun close() {
        snapshotJob?.cancel()
        snapshotJob = null
        livenessJob?.cancel()
        livenessJob = null
        runCatching { process?.destroy() }
        process = null
        _alive.value = false
    }

    companion object {
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
            val afterClear = input
                .substringAfterLast("\u001B[2J")
                .substringAfterLast("\u001Bc")
            val withoutOsc = afterClear.replace(Regex("\u001B\\][^\\u0007\\u001B]*(?:\u0007|\u001B\\\\)"), "")
            val withoutCsi = withoutOsc.replace(Regex("\u001B\\[[0-9;:?]*[ -/]*[@-~]"), "")
            val withoutSingleEsc = withoutCsi.replace(Regex("\u001B[@-Z\\\\-_]"), "")
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
