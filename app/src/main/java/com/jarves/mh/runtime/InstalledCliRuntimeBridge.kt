package com.jarves.mh.runtime

import android.content.Context
import android.util.Log
import com.jarves.mh.model.BUILTIN_CLI_PACKAGES
import com.jarves.mh.model.ChangeItem
import com.jarves.mh.model.ChatMessage
import com.jarves.mh.model.CliToolPackage
import com.jarves.mh.model.ProjectKind
import com.jarves.mh.model.ProviderKind
import com.jarves.mh.model.ProviderProfile
import com.jarves.mh.model.RuntimeEvent
import com.jarves.mh.model.ToolRequest
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Universal [RuntimeBridge] that turns ANY installed Linux CLI coding agent
 * (`opencode`, `codex`, `gemini`, `qwen`, `aider`, or custom user-installed CLIs)
 * into a first-class Chat Engine inside the main Claude-Code style Chat UI —
 * complete with live streaming responses, workspace file diff tracking (`+added / -removed`),
 * Keep/Undo support, and Stop control.
 */
class InstalledCliRuntimeBridge(
    private val context: Context,
    private val activeCliLookup: () -> CliToolPackage?,
    private val secretFor: (ProviderProfile) -> String?,
) : RuntimeBridge {

    private val installer = RuntimeInstaller(context)
    private val checkpoints = WorkspaceCheckpoints(context.filesDir, context)
    private val eventBus = MutableSharedFlow<RuntimeEvent>(extraBufferCapacity = 64)
    override val events: Flow<RuntimeEvent> = eventBus

    private val finishedSessions = ConcurrentHashMap.newKeySet<String>()
    @Volatile private var activeProcess: Process? = null
    @Volatile private var activeSessionId: String? = null
    @Volatile private var userStopRequested: Boolean = false

    fun configureProjectRoot(projectId: String, rootPath: String) {
        checkpoints.configureProjectRoot(projectId, rootPath)
    }

    override suspend fun startSession(
        projectId: String,
        projectSlug: String,
        projectKind: ProjectKind,
        prompt: String,
        conversationHistory: List<ChatMessage>,
        provider: ProviderProfile,
    ): String = withContext(Dispatchers.IO + NonCancellable) {
        val sessionId = UUID.randomUUID().toString()
        finishedSessions.remove(sessionId)
        activeSessionId = sessionId
        userStopRequested = false

        val cli = activeCliLookup()
        if (cli == null) {
            eventBus.emit(RuntimeEvent.SessionFailed(sessionId, "Selected CLI engine was not found. Please pick an installed CLI or built-in model."))
            return@withContext sessionId
        }

        eventBus.emit(RuntimeEvent.SessionStarted(sessionId))
        eventBus.emit(RuntimeEvent.ToolStarted(sessionId, cli.name, "Running `${cli.checkBinary.ifBlank { cli.launchCommand }}` in /workspace/$projectSlug"))

        runCatching {
            RuntimeTaskController.stopAction = {
                userStopRequested = true
                val running = activeProcess
                if (running != null) {
                    Thread {
                        runCatching {
                            running.destroy()
                            Thread.sleep(400)
                            if (running.isAlive) running.destroyForcibly()
                        }
                    }.start()
                }
            }

            val installed = installer.installedRuntime()
            val workspace = checkpoints.ensureWorkspace(projectId)
            checkpoints.createCheckpoint(projectId, workspace)
            val before = checkpoints.snapshot(workspace)

            val guestWorkspacePath = "/workspace/$projectSlug"
            val contextualPrompt = buildCliContextPrompt(prompt, conversationHistory, guestWorkspacePath)

            // Write prompt into rootfs /tmp so shell escaping & ARG_MAX never fail on large prompts
            val promptFileName = ".cs_cli_prompt_${sessionId.take(8)}.txt"
            val hostTmpDir = File(installed.rootfs, "tmp").apply { mkdirs() }
            val hostPromptFile = File(hostTmpDir, promptFileName)
            hostPromptFile.writeText(contextualPrompt, Charsets.UTF_8)
            val guestPromptPath = "/tmp/$promptFileName"

            val apiSecret = secretFor(provider).orEmpty()
            val env = buildMap {
                put("TERM", "dumb")
                put("NO_COLOR", "1")
                put("CI", "1")
                put("LANG", "C.UTF-8")
                put("LC_ALL", "C.UTF-8")
                put(
                    "PATH",
                    "/root/.local/bin:/root/.npm-global/bin:/root/.bun/bin:/root/.cargo/bin:/root/.deno/bin:/root/go/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                )
                if (apiSecret.isNotBlank()) {
                    when (provider.kind) {
                        ProviderKind.ANTHROPIC -> put("ANTHROPIC_API_KEY", apiSecret)
                        ProviderKind.OPENAI, ProviderKind.OPENAI_CUSTOM -> {
                            put("OPENAI_API_KEY", apiSecret)
                            if (provider.baseUrl.isNotBlank()) put("OPENAI_BASE_URL", provider.baseUrl)
                        }
                        ProviderKind.LLM_ROUTER -> {
                            put("OPENROUTER_API_KEY", apiSecret)
                            put("OPENAI_API_KEY", apiSecret)
                        }
                        else -> {
                            put("OPENAI_API_KEY", apiSecret)
                            put("ANTHROPIC_API_KEY", apiSecret)
                            put("GEMINI_API_KEY", apiSecret)
                            put("GOOGLE_API_KEY", apiSecret)
                        }
                    }
                }
            }

            val shellScript = buildCliExecutionScript(cli, guestPromptPath, guestWorkspacePath)
            val process = installer.process(
                proot = installed.proot,
                rootfs = installed.rootfs,
                workspace = workspace,
                environment = env,
                guestCommand = listOf("/usr/bin/bash", "-lc", shellScript),
                guestWorkspacePath = guestWorkspacePath,
            )
            activeProcess = process
            runCatching { process.outputStream.close() }
            if (userStopRequested) process.destroy()

            try {
                coroutineScope {
                    val liveDiffWatcher = launch {
                        var lastEmitted = emptyList<String>()
                        while (isActive && process.isAlive) {
                            delay(1_800)
                            runCatching {
                                val liveChanged = checkpoints.changedFiles(workspace, before)
                                if (liveChanged.isNotEmpty() && liveChanged != lastEmitted) {
                                    lastEmitted = liveChanged
                                    checkpoints.saveChangedPaths(projectId, liveChanged)
                                    val details = loadPendingChanges(projectId)
                                    eventBus.emit(RuntimeEvent.FilesChanged(sessionId, details))
                                }
                            }
                        }
                    }

                    val nativeProcess = process as? NativeSpawnProcess
                        ?: error("Unsupported Android runtime process")
                    var outputOffset = 0L
                    var emittedChars = 0
                    val fullRaw = StringBuilder()

                    while (process.isAlive || nativeProcess.outputFile.length() > outputOffset) {
                        val available = nativeProcess.outputFile.length() - outputOffset
                        if (available <= 0L) {
                            delay(24)
                            continue
                        }
                        val bytes = ByteArray(minOf(available, 16L * 1024L).toInt())
                        val count = RandomAccessFile(nativeProcess.outputFile, "r").use { raf ->
                            raf.seek(outputOffset)
                            raf.read(bytes)
                        }
                        if (count > 0) {
                            outputOffset += count
                            fullRaw.append(bytes.decodeToString(0, count))
                            val cleaned = InteractivePtySession.stripPtyAnsi(fullRaw.toString())
                            if (cleaned.length > emittedChars) {
                                val delta = cleaned.substring(emittedChars)
                                emittedChars = cleaned.length
                                eventBus.emit(RuntimeEvent.AssistantDelta(sessionId, delta))
                            }
                        }
                    }

                    val exitCode = process.waitFor()
                    liveDiffWatcher.cancelAndJoin()

                    val changed = checkpoints.changedFiles(workspace, before)
                    if (changed.isNotEmpty()) {
                        checkpoints.saveChangedPaths(projectId, changed)
                        val details = loadPendingChanges(projectId)
                        eventBus.emit(RuntimeEvent.FilesChanged(sessionId, details))
                    } else if (!File(checkpoints.checkpointDir(projectId), "changes.json").isFile) {
                        acceptLastChanges(projectId)
                    }

                    eventBus.emit(RuntimeEvent.ToolCompleted(sessionId, cli.name, "Exit code $exitCode"))

                    if (userStopRequested) {
                        emitFailureOnce(sessionId, "Stopped by user")
                    } else if (exitCode == 0 || emittedChars > 0) {
                        if (emittedChars == 0) {
                            eventBus.emit(RuntimeEvent.AssistantDelta(sessionId, "Completed `${cli.name}` task cleanly."))
                        }
                        emitCompletedOnce(sessionId)
                    } else {
                        emitFailureOnce(sessionId, "`${cli.name}` exited with code $exitCode. Check CLI login/config in `>_` CLI if needed.")
                    }
                }
            } finally {
                runCatching { hostPromptFile.delete() }
            }
        }.onFailure { err ->
            Log.e("InstalledCliBridge", "CLI Chat session failed", err)
            emitFailureOnce(sessionId, err.message ?: "CLI execution failed")
        }

        activeProcess = null
        activeSessionId = null
        RuntimeTaskController.stopAction = null
        sessionId
    }

    private suspend fun emitCompletedOnce(sessionId: String) {
        if (finishedSessions.add(sessionId)) {
            eventBus.emit(RuntimeEvent.SessionCompleted(sessionId))
        }
    }

    private suspend fun emitFailureOnce(sessionId: String, message: String) {
        if (finishedSessions.add(sessionId)) {
            eventBus.emit(RuntimeEvent.SessionFailed(sessionId, message))
        }
    }

    override suspend fun respondToApproval(request: ToolRequest, approved: Boolean) = Unit

    override suspend fun stopSession(sessionId: String) = withContext(Dispatchers.IO) {
        if (activeSessionId == sessionId) {
            stopActiveSession()
        }
    }

    override suspend fun stopActiveSession() = withContext(Dispatchers.IO) {
        userStopRequested = true
        val proc = activeProcess ?: return@withContext
        runCatching {
            proc.destroy()
            delay(350)
            if (proc.isAlive) proc.destroyForcibly()
        }
    }

    override suspend fun undoLastChanges(projectId: String): Boolean = withContext(Dispatchers.IO) {
        val checkpoint = checkpoints.checkpointDir(projectId)
        val backup = File(checkpoint, "project")
        if (!backup.isDirectory) return@withContext false
        val workspace = checkpoints.ensureWorkspace(projectId)
        checkpoints.readChangedPaths(projectId).forEach { path ->
            if (!checkpoints.isInternalRuntimePath(path)) {
                val target = checkpoints.safeWorkspaceFile(workspace, path)
                val orig = checkpoints.safeWorkspaceFile(backup, path)
                if (orig.isFile) {
                    target.parentFile?.mkdirs()
                    orig.copyTo(target, overwrite = true)
                } else if (target.isFile) {
                    target.delete()
                }
            }
        }
        checkpoint.deleteRecursively()
        true
    }

    override suspend fun acceptLastChanges(projectId: String) {
        withContext(Dispatchers.IO) {
            checkpoints.checkpointDir(projectId).deleteRecursively()
        }
    }

    override suspend fun loadPendingChanges(projectId: String): List<ChangeItem> = withContext(Dispatchers.IO) {
        val workspace = checkpoints.ensureWorkspace(projectId)
        val paths = checkpoints.readChangedPaths(projectId).filterNot(checkpoints::isInternalRuntimePath)
        if (paths.isEmpty()) emptyList() else checkpoints.buildChangeDetails(projectId, workspace, paths)
    }

    override suspend fun undoFileChange(projectId: String, path: String): Boolean = withContext(Dispatchers.IO) {
        if (checkpoints.isInternalRuntimePath(path) || path !in checkpoints.readChangedPaths(projectId)) return@withContext false
        val workspace = checkpoints.ensureWorkspace(projectId)
        val backup = File(checkpoints.checkpointDir(projectId), "project")
        val target = checkpoints.safeWorkspaceFile(workspace, path)
        val orig = checkpoints.safeWorkspaceFile(backup, path)
        if (orig.isFile) {
            target.parentFile?.mkdirs()
            orig.copyTo(target, overwrite = true)
        } else if (target.isFile) {
            target.delete()
        }
        checkpoints.removeChangedPath(projectId, path)
        true
    }

    override suspend fun acceptFileChange(projectId: String, path: String): Boolean = withContext(Dispatchers.IO) {
        if (checkpoints.isInternalRuntimePath(path) || path !in checkpoints.readChangedPaths(projectId)) return@withContext false
        val workspace = checkpoints.ensureWorkspace(projectId)
        val backup = File(checkpoints.checkpointDir(projectId), "project")
        val current = checkpoints.safeWorkspaceFile(workspace, path)
        val baseline = checkpoints.safeWorkspaceFile(backup, path)
        if (current.isFile) {
            baseline.parentFile?.mkdirs()
            current.copyTo(baseline, overwrite = true)
        } else if (baseline.isFile) {
            baseline.delete()
        }
        checkpoints.removeChangedPath(projectId, path)
        true
    }

    companion object {
        private val GUEST_BIN_DIRS = listOf(
            "root/.npm-global/bin",
            "root/.local/bin",
            "root/.bun/bin",
            "root/.cargo/bin",
            "root/.deno/bin",
            "root/go/bin",
            "usr/local/bin",
            "usr/bin",
            "bin",
        )

        /**
         * Fast zero-spawn filesystem check returning the IDs of all [CliToolPackage]s
         * whose executable binary is currently installed inside the Ubuntu arm64 rootfs.
         */
        fun discoverInstalledCliIds(
            rootfs: File?,
            customClis: List<CliToolPackage>,
        ): Set<String> {
            if (rootfs == null || !rootfs.isDirectory) return emptySet()
            val all = customClis + BUILTIN_CLI_PACKAGES
            val found = LinkedHashSet<String>()
            for (pkg in all) {
                val bin = pkg.checkBinary.trim()
                    .ifBlank { pkg.launchCommand.trim().substringBefore(' ') }
                    .removePrefix("./")
                if (bin.isBlank()) continue
                val exists = if (bin.startsWith('/')) {
                    File(rootfs, bin.removePrefix("/")).isFile
                } else {
                    GUEST_BIN_DIRS.any { dir -> File(rootfs, "$dir/$bin").isFile }
                }
                if (exists) found += pkg.id
            }
            return found
        }

        internal fun buildCliExecutionScript(
            cli: CliToolPackage,
            guestPromptPath: String,
            guestWorkspacePath: String,
        ): String {
            val bin = cli.checkBinary.trim().ifBlank { cli.launchCommand.trim().substringBefore(' ') }
            val invokeLine = when (cli.id) {
                "opencode-cli" -> """opencode run "$(cat "$guestPromptPath")""""
                "openai-codex" -> """codex exec --skip-git-repo-check --dangerously-bypass-approvals-and-sandbox "$(cat "$guestPromptPath")""""
                "google-gemini-cli" -> """gemini -y -p "$(cat "$guestPromptPath")""""
                "qwen-code-cli" -> """qwen -y -p "$(cat "$guestPromptPath")""""
                "aider-cli" -> """aider --yes-always --no-pretty --no-auto-commits --message-file "$guestPromptPath""""
                else -> {
                    val launch = cli.launchCommand.trim()
                    when {
                        "{prompt}" in launch -> launch.replace("{prompt}", "\"$(cat \"$guestPromptPath\")\"")
                        bin == "opencode" -> """opencode run "$(cat "$guestPromptPath")""""
                        bin == "codex" -> """codex exec --skip-git-repo-check --dangerously-bypass-approvals-and-sandbox "$(cat "$guestPromptPath")""""
                        bin == "gemini" -> """gemini -y -p "$(cat "$guestPromptPath")""""
                        bin == "qwen" -> """qwen -y -p "$(cat "$guestPromptPath")""""
                        bin == "aider" -> """aider --yes-always --no-pretty --no-auto-commits --message-file "$guestPromptPath""""
                        else -> """$launch "$(cat "$guestPromptPath")""""
                    }
                }
            }
            return """
                export PATH="/root/.local/bin:/root/.npm-global/bin:/root/.bun/bin:/root/.cargo/bin:/root/.deno/bin:/root/go/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:${'$'}PATH"
                cd '$guestWorkspacePath' 2>/dev/null || true
                if ! command -v '$bin' >/dev/null 2>&1; then
                  echo "CLI '$bin' is not installed yet. Installing '${cli.name}' now..."
                  DEBIAN_FRONTEND=noninteractive ${cli.installCommand}
                fi
                $invokeLine
            """.trimIndent()
        }

        private fun buildCliContextPrompt(
            prompt: String,
            history: List<ChatMessage>,
            guestWorkspacePath: String,
        ): String {
            val recent = history
                .filter { it.fromUser || !it.text.startsWith("Hi! Tell me") }
                .dropLast(1)
                .recentWithinCharacterBudget(48_000)
            if (recent.isEmpty()) return prompt
            return buildString {
                appendLine("Working directory: $guestWorkspacePath")
                appendLine("Recent conversation context:")
                for (m in recent.takeLast(8)) {
                    val role = if (m.fromUser) "User" else "Assistant"
                    appendLine("$role: ${m.text.take(4_000)}")
                }
                appendLine()
                appendLine("Current request:")
                append(prompt)
            }
        }
    }
}
