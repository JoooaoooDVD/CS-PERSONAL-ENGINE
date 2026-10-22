package com.jarves.mh.runtime

import android.content.Context
import com.jarves.mh.data.ApiKeyVault
import com.jarves.mh.data.AppPreferences
import com.jarves.mh.model.GitFileChange
import com.jarves.mh.model.GitStatusSnapshot
import com.jarves.mh.network.GitHubClient
import java.io.File

/**
 * Executes non-interactive Git & GitHub operations inside a project's PRoot workspace
 * with automatic `GIT_ASKPASS`, PAT / `gh` CLI token fallback, and commit identity.
 */
class GitWorkspaceManager(
    private val context: Context,
    private val installer: RuntimeInstaller,
    private val vault: ApiKeyVault,
    private val preferences: AppPreferences,
) {
    private fun runGitShell(
        workspace: File,
        projectSlug: String,
        script: String,
        extraEnv: Map<String, String> = emptyMap(),
    ): Pair<Int, String> {
        if (!installer.isInstalled()) return 1 to "Linux runtime is not installed yet."
        val runtime = installer.installedRuntime()
        val guestWorkspace = "/workspace/$projectSlug"
        val outFile = File(context.cacheDir, "cs-git-${System.nanoTime()}.log")
        return try {
            val process = installer.process(
                proot = runtime.proot,
                rootfs = runtime.rootfs,
                workspace = workspace,
                environment = extraEnv,
                guestCommand = listOf("/usr/bin/env", "bash", "-lc", script),
                guestWorkspacePath = guestWorkspace,
                outputFile = outFile,
            )
            val exit = process.waitFor()
            val raw = outFile.readTailText(128 * 1024)
            exit to sanitizeGitOutput(raw)
        } finally {
            outFile.delete()
        }
    }

    fun inspectStatus(workspace: File, projectSlug: String): GitStatusSnapshot {
        if (!File(workspace, ".git").exists()) {
            return GitStatusSnapshot(initialized = false)
        }
        val script = """
            set +e
            echo "===BRANCH==="
            git symbolic-ref --short HEAD 2>/dev/null || git rev-parse --short HEAD 2>/dev/null || echo "main"
            echo "===BRANCHES==="
            git for-each-ref --format='%(refname:short)' refs/heads/ 2>/dev/null
            echo "===REMOTE==="
            git remote get-url origin 2>/dev/null || true
            echo "===AHEAD_BEHIND==="
            git rev-list --left-right --count @{upstream}...HEAD 2>/dev/null || echo "0 0"
            echo "===LAST_COMMIT==="
            git log -1 --pretty=format:'%h · %s (%cr)' 2>/dev/null || true
            echo "===STATUS==="
            git status --porcelain=v1 -uall 2>/dev/null || true
        """.trimIndent()
        val (_, output) = runGitShell(workspace, projectSlug, script)
        return parseGitStatusOutput(output)
    }

    fun initRepository(workspace: File, projectSlug: String): Result<String> = runCatching {
        val (exit, out) = runGitShell(
            workspace,
            projectSlug,
            "git init -b main 2>/dev/null || (git init && git symbolic-ref HEAD refs/heads/main)",
        )
        check(exit == 0) { out.ifBlank { "git init failed" } }
        if (!File(workspace, ".gitignore").exists()) {
            File(workspace, ".gitignore").writeText(
                "node_modules/\n.venv/\nvenv/\n__pycache__/\nbuild/\ndist/\n.gradle/\nlocal.properties\n.DS_Store\n",
            )
        }
        "Initialized Git repository on branch main"
    }

    fun commitAndPush(
        workspace: File,
        projectSlug: String,
        commitMessage: String,
        pushAfterCommit: Boolean = true,
    ): Result<String> = runCatching {
        if (!File(workspace, ".git").exists()) {
            initRepository(workspace, projectSlug).getOrThrow()
        }
        val safeMessage = commitMessage.trim().ifBlank { generateSmartCommitMessage(workspace, projectSlug) }
        val script = buildString {
            appendLine("set -e")
            appendLine("git add -A")
            appendLine("if ! git diff --cached --quiet; then")
            appendLine("  git commit -m \"${'$'}CS_COMMIT_MSG\"")
            appendLine("fi")
            if (pushAfterCommit) {
                appendLine("branch=${'$'}(git symbolic-ref --short HEAD 2>/dev/null || echo main)")
                appendLine("remote_url=${'$'}(git remote get-url origin 2>/dev/null || true)")
                appendLine("if [ -z \"${'$'}remote_url\" ]; then")
                appendLine("  echo 'NO_REMOTE_CONFIGURED'")
                appendLine("  exit 42")
                appendLine("fi")
                appendLine("git push -u origin \"${'$'}branch\"")
            }
        }
        val (exit, out) = runGitShell(
            workspace = workspace,
            projectSlug = projectSlug,
            script = script,
            extraEnv = mapOf("CS_COMMIT_MSG" to safeMessage),
        )
        if (exit == 42 || out.contains("NO_REMOTE_CONFIGURED")) {
            error("Committed locally, but no GitHub remote is configured yet. Create or link a GitHub repository below to push.")
        }
        check(exit == 0) { friendlyGitPushError(out) }
        if (pushAfterCommit) "Committed & pushed to GitHub successfully" else "Committed changes locally"
    }

    fun pullLatest(workspace: File, projectSlug: String): Result<String> = runCatching {
        val (exit, out) = runGitShell(
            workspace,
            projectSlug,
            "set -e; branch=$(git symbolic-ref --short HEAD 2>/dev/null || echo main); git pull --rebase origin \"${'$'}branch\"",
        )
        check(exit == 0) { friendlyGitPushError(out) }
        out.lineSequence().lastOrNull { it.isNotBlank() } ?: "Pulled latest changes from origin"
    }

    fun checkoutOrCreateBranch(workspace: File, projectSlug: String, branchName: String, create: Boolean): Result<String> = runCatching {
        val clean = branchName.trim().replace(Regex("[^A-Za-z0-9._/-]"), "-").trim('-', '/')
        require(clean.isNotBlank()) { "Enter a valid branch name" }
        val flag = if (create) "-B" else ""
        val cmd = if (create) "git checkout -B \"$clean\"" else "git checkout \"$clean\""
        val (exit, out) = runGitShell(workspace, projectSlug, cmd)
        check(exit == 0) { out.ifBlank { "Could not switch to branch $clean" } }
        "Switched to branch $clean"
    }

    fun configureRemoteUrl(workspace: File, projectSlug: String, remoteUrl: String): Result<String> = runCatching {
        val clean = remoteUrl.trim()
        require(clean.startsWith("https://") || clean.startsWith("git@")) { "Enter an https:// GitHub repository URL" }
        if (!File(workspace, ".git").exists()) {
            initRepository(workspace, projectSlug).getOrThrow()
        }
        val (exit, out) = runGitShell(
            workspace,
            projectSlug,
            "if git remote get-url origin >/dev/null 2>&1; then git remote set-url origin \"${'$'}CS_REMOTE\"; else git remote add origin \"${'$'}CS_REMOTE\"; fi",
            extraEnv = mapOf("CS_REMOTE" to clean),
        )
        check(exit == 0) { out.ifBlank { "Could not set origin remote" } }
        "Connected remote origin → $clean"
    }

    fun createGitHubRepoAndPush(
        workspace: File,
        projectSlug: String,
        repoName: String,
        description: String,
        isPrivate: Boolean,
        commitMessage: String,
    ): Result<String> = runCatching {
        val cleanRepoName = repoName.trim().replace(Regex("[^A-Za-z0-9._-]"), "-").trim('-').ifBlank { projectSlug }
        val pat = vault.get(RuntimeInstaller.GITHUB_PAT_VAULT_KEY).orEmpty()

        val cloneUrl = if (pat.isNotBlank()) {
            val created = GitHubClient(context).createRepository(
                token = pat,
                name = cleanRepoName,
                description = description,
                isPrivate = isPrivate,
            )
            created.cloneUrl
        } else if (installer.isGitHubCliInstalled()) {
            val visibilityFlag = if (isPrivate) "--private" else "--public"
            val (exit, out) = runGitShell(
                workspace = workspace,
                projectSlug = projectSlug,
                script = "/root/.local/bin/gh repo create \"${'$'}CS_REPO_NAME\" $visibilityFlag --description \"${'$'}CS_REPO_DESC\" --confirm 2>/dev/null || " +
                    "/root/.local/bin/gh repo create \"${'$'}CS_REPO_NAME\" $visibilityFlag --description \"${'$'}CS_REPO_DESC\"",
                extraEnv = mapOf(
                    "CS_REPO_NAME" to cleanRepoName,
                    "CS_REPO_DESC" to description.take(200),
                ),
            )
            check(exit == 0) { out.ifBlank { "Connect GitHub (PAT or CLI) first before creating a repository." } }
            val urlMatch = Regex("https://github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+").find(out)?.value
            if (urlMatch != null) "$urlMatch.git" else {
                val login = preferences.githubLogin.takeIf(String::isNotBlank)
                    ?: error("GitHub login unknown; reconnect GitHub.")
                "https://github.com/$login/$cleanRepoName.git"
            }
        } else {
            error("Connect your GitHub Personal Access Token or GitHub account first.")
        }

        configureRemoteUrl(workspace, projectSlug, cloneUrl).getOrThrow()
        commitAndPush(
            workspace = workspace,
            projectSlug = projectSlug,
            commitMessage = commitMessage.ifBlank { "Initial commit from CS Personal Engine" },
            pushAfterCommit = true,
        ).getOrThrow()
        "Created ${cloneUrl.removeSuffix(".git")} & pushed branch!"
    }

    fun generateSmartCommitMessage(workspace: File, projectSlug: String): String {
        val snap = inspectStatus(workspace, projectSlug)
        if (snap.modifiedFiles.isEmpty()) return "chore: sync workspace via CS Personal Engine"
        val paths = snap.modifiedFiles.map { it.path }
        val added = snap.modifiedFiles.count { it.status.contains('?') || it.status.contains('A') }
        val deleted = snap.modifiedFiles.count { it.status.contains('D') }
        val verb = when {
            added == snap.modifiedFiles.size -> "feat: add"
            deleted == snap.modifiedFiles.size -> "chore: remove"
            else -> "feat: update"
        }
        val names = paths.take(3).map { it.substringAfterLast('/') }.distinct().joinToString(", ")
        val extra = if (paths.size > 3) " (+${paths.size - 3} more)" else ""
        return "$verb $names$extra"
    }

    private fun friendlyGitPushError(raw: String): String {
        val lower = raw.lowercase()
        return when {
            "authentication failed" in lower || "could not read username" in lower || "403" in lower || "permission to" in lower ->
                "GitHub authentication or repo scope missing. Add a GitHub Personal Access Token (with `repo` + `workflow` scopes) or reconnect GitHub."
            "fetch first" in lower || "non-fast-forward" in lower ->
                "Remote has newer commits. Tap 'Pull' first to rebase onto remote changes, then Push again."
            "repository not found" in lower ->
                "Remote repository not found on GitHub. Check the remote URL or create the repo first."
            else -> raw.lineSequence().filter(String::isNotBlank).toList().takeLast(4).joinToString(" · ").ifBlank { "Git command failed" }
        }
    }

    private fun sanitizeGitOutput(raw: String): String = raw
        .replace(Regex("\\u001B\\[[0-?]*[ -/]*[@-~]"), "")
        .replace(Regex("(?i)(ghp_[A-Za-z0-9_]+|github_pat_[A-Za-z0-9_]+)"), "ghp_••••")
        .trim()

    companion object {
        internal fun parseGitStatusOutput(output: String): GitStatusSnapshot {
            val sections = mutableMapOf<String, MutableList<String>>()
            var current = ""
            output.lineSequence().forEach { line ->
                if (line.startsWith("===") && line.endsWith("===")) {
                    current = line.removeSurrounding("===")
                    sections.putIfAbsent(current, mutableListOf())
                } else if (current.isNotEmpty()) {
                    sections[current]?.add(line)
                }
            }
            val branch = sections["BRANCH"]?.firstOrNull { it.isNotBlank() }?.trim().orEmpty().ifBlank { "main" }
            val branches = sections["BRANCHES"].orEmpty().map(String::trim).filter(String::isNotBlank).distinct()
            val remote = sections["REMOTE"]?.firstOrNull { it.isNotBlank() }?.trim().orEmpty()
            val counts = sections["AHEAD_BEHIND"]?.firstOrNull()?.trim()?.split(Regex("\\s+")).orEmpty()
            val behind = counts.getOrNull(0)?.toIntOrNull() ?: 0
            val ahead = counts.getOrNull(1)?.toIntOrNull() ?: 0
            val lastCommit = sections["LAST_COMMIT"]?.firstOrNull { it.isNotBlank() }?.trim().orEmpty()
            val files = sections["STATUS"].orEmpty()
                .filter { it.length >= 3 }
                .map { raw ->
                    val status = raw.substring(0, 2).trim().ifBlank { "M" }
                    val staged = raw[0] != ' ' && raw[0] != '?'
                    val path = raw.substring(3).trim().removeSurrounding("\"")
                    GitFileChange(status = status, path = path, staged = staged)
                }
            return GitStatusSnapshot(
                initialized = true,
                branch = branch,
                branches = if (branch in branches) branches else listOf(branch) + branches,
                remoteUrl = remote,
                aheadCount = ahead,
                behindCount = behind,
                modifiedFiles = files,
                lastCommitSummary = lastCommit,
            )
        }
    }
}
