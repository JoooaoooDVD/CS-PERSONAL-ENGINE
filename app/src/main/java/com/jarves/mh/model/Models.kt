package com.jarves.mh.model

import java.time.Instant
import java.text.Normalizer
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

enum class ProviderProtocol { CLAUDE_LOGIN, ANTHROPIC, ANTHROPIC_GATEWAY, OPENROUTER, OPENAI_RESPONSES, OPENAI_CHAT }

enum class ProviderKind(
    val title: String,
    val subtitle: String,
    val protocol: ProviderProtocol,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val experimental: Boolean = false,
    val fixedBaseUrl: Boolean = false,
    val fixedProtocol: Boolean = false,
) {
    CLAUDE("Claude subscription", "Pro, Max, Team or Enterprise", ProviderProtocol.CLAUDE_LOGIN, "", "default"),
    ANTHROPIC("Anthropic API", "Usage billed through Console", ProviderProtocol.ANTHROPIC, "https://api.anthropic.com", "claude-sonnet-4-6"),
    LLM_ROUTER("OpenRouter", "Use your OpenRouter API key", ProviderProtocol.OPENROUTER, "https://openrouter.ai/api", "~anthropic/claude-sonnet-latest"),
    DEEPSEEK("DeepSeek", "Use your DeepSeek API key", ProviderProtocol.ANTHROPIC_GATEWAY, "https://api.deepseek.com/anthropic", "deepseek-v4-flash"),
    KIMI("Kimi", "Anthropic-compatible endpoint", ProviderProtocol.ANTHROPIC_GATEWAY, "https://api.moonshot.ai/anthropic", "kimi-k2.6", true),
    OPENCODE_ZEN(
        "OpenCode Zen",
        "Models through the OpenCode Zen gateway",
        ProviderProtocol.OPENAI_RESPONSES,
        "https://opencode.ai/zen/v1",
        "deepseek-v4-flash",
        fixedBaseUrl = true,
        fixedProtocol = true,
    ),
    NVIDIA_NIM(
        "NVIDIA NIM",
        "OpenAI-compatible models hosted by NVIDIA",
        ProviderProtocol.OPENAI_CHAT,
        "https://integrate.api.nvidia.com/v1",
        "qwen/qwen2.5-coder-32b-instruct",
        fixedBaseUrl = true,
        fixedProtocol = true,
    ),
    OPENAI_CUSTOM(
        "OpenAI-Compatible API",
        "OpenAI, Groq, Together, Ollama, vLLM & custom /v1 servers",
        ProviderProtocol.OPENAI_CHAT,
        "https://api.openai.com/v1",
        "gpt-4.1",
    ),
    CUSTOM(
        "Custom API",
        "Universal Anthropic / OpenAI-compatible gateway",
        ProviderProtocol.ANTHROPIC_GATEWAY,
        "",
        "",
        true,
    ),
}

/**
 * Coding agent engine installed in the private Linux runtime.
 * Each coding agent is installed independently on demand over the shared Core runtime.
 */
enum class AgentKind(
    val stableId: String,
    val title: String,
    val subtitle: String,
    val downloadNote: String,
) {
    CLAUDE_CODE(
        "claude-code",
        "Claude Code",
        "Anthropic's coding agent · broad provider support",
        "71.8 MB",
    ),
    DEEPSEEK_HARNESS(
        "deepseek-harness",
        "DeepSeek Harness",
        "Official DeepSeek coding agent · API-key providers",
        "26.5 MB",
    ),
    ANTIGRAVITY(
        "antigravity",
        "Antigravity CLI",
        "Google's official coding agent · Google account",
        "39.9 MB",
    ),
    ;

    companion object {
        fun fromStored(value: String?): AgentKind = entries.firstOrNull {
            it.stableId == value || it.name == value
        } ?: CLAUDE_CODE
    }
}

/** Provider kinds usable with [AgentKind.DEEPSEEK_HARNESS]. Claude OAuth login has no dsh equivalent. */
val DEEPSEEK_HARNESS_PROVIDERS: Set<ProviderKind> = setOf(
    ProviderKind.DEEPSEEK,
    ProviderKind.ANTHROPIC,
    ProviderKind.LLM_ROUTER,
    ProviderKind.KIMI,
    ProviderKind.OPENCODE_ZEN,
    ProviderKind.NVIDIA_NIM,
    ProviderKind.OPENAI_CUSTOM,
    ProviderKind.CUSTOM,
)

val DSH_PROTOCOL_PROVIDERS: Set<ProviderKind> = setOf(
    ProviderKind.KIMI,
    ProviderKind.OPENCODE_ZEN,
    ProviderKind.NVIDIA_NIM,
    ProviderKind.OPENAI_CUSTOM,
    ProviderKind.CUSTOM,
)

/** Providers whose wire protocol can be chosen for BOTH Claude Code (via LocalFormatGateway) and DSH. */
val UNIVERSAL_PROTOCOL_PROVIDERS: Set<ProviderKind> = setOf(
    ProviderKind.OPENAI_CUSTOM,
    ProviderKind.CUSTOM,
)

data class OpenAiCompatiblePreset(
    val id: String,
    val label: String,
    val baseUrl: String,
    val defaultModel: String,
    val wireApi: String = "openai-completions",
    val keyOptional: Boolean = false,
)

val OPENAI_COMPATIBLE_PRESETS: List<OpenAiCompatiblePreset> = listOf(
    OpenAiCompatiblePreset("openai", "OpenAI", "https://api.openai.com/v1", "gpt-4.1"),
    OpenAiCompatiblePreset("groq", "Groq", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile"),
    OpenAiCompatiblePreset("together", "Together AI", "https://api.together.xyz/v1", "Qwen/Qwen2.5-Coder-32B-Instruct"),
    OpenAiCompatiblePreset("mistral", "Mistral / Codestral", "https://api.mistral.ai/v1", "codestral-latest"),
    OpenAiCompatiblePreset("fireworks", "Fireworks AI", "https://api.fireworks.ai/inference/v1", "accounts/fireworks/models/deepseek-v3"),
    OpenAiCompatiblePreset("cerebras", "Cerebras", "https://api.cerebras.ai/v1", "qwen-3-32b"),
    OpenAiCompatiblePreset("xai", "xAI Grok", "https://api.x.ai/v1", "grok-3-mini"),
    OpenAiCompatiblePreset("ollama", "Ollama (Local)", "http://127.0.0.1:11434/v1", "qwen2.5-coder:7b", keyOptional = true),
    OpenAiCompatiblePreset("lmstudio", "LM Studio / vLLM", "http://127.0.0.1:1234/v1", "local-model", keyOptional = true),
)

fun defaultDshApiForProvider(kind: ProviderKind): String = when (kind) {
    ProviderKind.OPENCODE_ZEN -> "openai-responses"
    ProviderKind.NVIDIA_NIM, ProviderKind.OPENAI_CUSTOM -> "openai-completions"
    else -> "anthropic-messages"
}

/**
 * Best-effort protocol choice for a user-entered custom gateway URL.
 * The picker remains editable because a URL alone cannot prove a gateway's wire format.
 */
fun inferredDshApiForUrl(baseUrl: String): String {
    val normalized = baseUrl.trim().trimEnd('/').lowercase(Locale.ROOT)
    return when {
        normalized.endsWith("/responses") -> "openai-responses"
        "/anthropic" in normalized || "api.anthropic.com" in normalized -> "anthropic-messages"
        normalized.endsWith("/v1") || "api.openai.com" in normalized || "groq.com" in normalized ||
            "together.xyz" in normalized || "mistral.ai" in normalized || ":11434" in normalized ||
            ":1234" in normalized -> "openai-completions"
        else -> "anthropic-messages"
    }
}

/** Resolves the protocol the selected coding agent will actually use for this saved profile. */
fun providerProtocolForAgent(profile: ProviderProfile, agent: AgentKind): ProviderProtocol {
    val supportsCustomWireChoice = when (agent) {
        AgentKind.DEEPSEEK_HARNESS -> profile.kind in DSH_PROTOCOL_PROVIDERS
        AgentKind.CLAUDE_CODE -> profile.kind in UNIVERSAL_PROTOCOL_PROVIDERS
        AgentKind.ANTIGRAVITY -> false
    }
    if (!supportsCustomWireChoice) {
        return profile.kind.protocol
    }
    val api = if (profile.kind.fixedProtocol) defaultDshApiForProvider(profile.kind) else profile.dshApi
    return when (api) {
        "openai-completions" -> ProviderProtocol.OPENAI_CHAT
        "openai-responses" -> ProviderProtocol.OPENAI_RESPONSES
        else -> ProviderProtocol.ANTHROPIC_GATEWAY
    }
}

/** Provider choices shown for the selected coding agent. */
fun providersForAgent(agent: AgentKind): List<ProviderKind> = when (agent) {
    AgentKind.DEEPSEEK_HARNESS -> ProviderKind.entries.filter { it in DEEPSEEK_HARNESS_PROVIDERS }
    AgentKind.CLAUDE_CODE -> ProviderKind.entries.filterNot { it == ProviderKind.OPENCODE_ZEN }
    AgentKind.ANTIGRAVITY -> emptyList()
}

data class ProviderProfile(
    val kind: ProviderKind,
    val baseUrl: String = kind.defaultBaseUrl,
    val model: String = kind.defaultModel,
    val hasSecret: Boolean = false,
    /** Custom-route wire protocol: anthropic-messages | openai-completions | openai-responses. */
    val dshApi: String = defaultDshApiForProvider(kind),
    /** Comma-separated OpenRouter provider slugs, tried in order. Blank keeps automatic routing. */
    val openRouterProviderOrder: String = "",
    /** Whether OpenRouter may use providers outside [openRouterProviderOrder]. */
    val openRouterAllowFallbacks: Boolean = true,
    /** Newline-separated `Header-Name: value` pairs sent to Custom / OpenAI-Compatible endpoints. */
    val customHeaders: String = "",
) {
    /** Effective base URL: fixed kinds always resolve to their constant, ignoring stored drift. */
    val resolvedBaseUrl: String get() = if (kind.fixedBaseUrl) kind.defaultBaseUrl else baseUrl

    /** Effective protocol when driving Claude Code (resolves custom OpenAI/Anthropic wire selection). */
    val effectiveClaudeProtocol: ProviderProtocol
        get() = providerProtocolForAgent(this, AgentKind.CLAUDE_CODE)

    val openRouterProviders: List<String>
        get() = openRouterProviderOrder.split(',')
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()

    val parsedCustomHeaders: Map<String, String>
        get() = customHeaders.lineSequence()
            .map(String::trim)
            .filter { it.isNotBlank() && ':' in it }
            .mapNotNull { line ->
                val split = line.indexOf(':')
                val name = line.substring(0, split).trim()
                val value = line.substring(split + 1).trim()
                if (name.isBlank() || value.isBlank()) null else name to value
            }
            .toMap()
}

enum class ProjectKind { PROJECT, QUICK_PROJECT }

enum class ProjectType { GENERAL, ANDROID }

enum class AndroidTemplate(val label: String) {
    COMPOSE("Jetpack Compose"),
    XML("Kotlin + XML"),
}

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val language: String,
    val slug: String = projectSlug(name),
    val rootPath: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val kind: ProjectKind = ProjectKind.PROJECT,
    val type: ProjectType = ProjectType.GENERAL,
    val androidTemplate: AndroidTemplate? = null,
) {
    val formattedUpdatedAt: String
        get() {
            val diff = System.currentTimeMillis() - updatedAtMillis
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            return when {
                diff < 0 || seconds < 60 -> "Just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                days == 1L -> "Yesterday"
                days < 7 -> "${days}d ago"
                else -> {
                    val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(updatedAtMillis))
                }
            }
        }
}

fun projectSlug(name: String): String {
    val ascii = Normalizer.normalize(name, Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .take(48)
        .trimEnd('-')
    return ascii.ifBlank { "project" }
}

data class QuickChatIdentity(val displayName: String, val slug: String)

fun generateQuickChatIdentity(usedSlugs: Set<String>, random: Random = Random.Default): QuickChatIdentity {
    val adjectives = listOf("bright", "calm", "clever", "curious", "gentle", "nimble", "quiet", "swift", "wise", "bold")
    val pioneers = listOf("turing", "lovelace", "hopper", "tesla", "curie", "ramanujan", "bose", "kalam", "faraday", "darwin")
    repeat(20) {
        val base = "${adjectives.random(random)}-${pioneers.random(random)}"
        if (base !in usedSlugs) return QuickChatIdentity(base.toDisplayName(), base)
    }
    val base = "${adjectives.random(random)}-${pioneers.random(random)}"
    val slug = generateSequence(2) { it + 1 }.map { "$base-$it" }.first { it !in usedSlugs }
    return QuickChatIdentity(slug.toDisplayName(), slug)
}

private fun String.toDisplayName(): String = split('-').joinToString(" ") { word ->
    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
}

data class WorkspaceEntry(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val depth: Int,
    val sizeBytes: Long = 0,
    /** Null until the directory's direct children have been counted off the UI thread. */
    val childCount: Int? = null,
)

enum class RiskLevel { SAFE, REVIEW, HIGH }

/**
 * Optional development toolchains the user can pick during onboarding.
 * Node.js, npm, Git, and Claude Code itself are always installed because the
 * agent runtime depends on them; these stacks add heavier extras on demand.
 */
enum class DevStack(
    val label: String,
    val description: String,
    val installsSummary: String,
) {
    WEB(
        "Web (JavaScript / TypeScript)",
        "Websites and web apps with HTML, CSS, and JS frameworks.",
        "Node.js and npm (already included)",
    ),
    PYTHON(
        "Python",
        "Scripts, automation, data work, and Python backends.",
        "python3, pip, venv, and build tools",
    ),
    ANDROID(
        "Android (Java / Kotlin)",
        "Build Android app projects and install them directly on this phone.",
        "JDK 17, ARM64 Android SDK 36, Build Tools 35, Gradle 8.14.3, and an offline Maven cache",
    ),
    CPP(
        "C / C++",
        "Fast compiled programs, algorithms, and systems code.",
        "gcc, g++, make, cmake, gdb",
    ),
    PHP(
        "PHP",
        "Websites and apps with PHP — classic sites and Laravel projects.",
        "php-cli, common extensions, and Composer",
    ),
}

data class ToolRequest(
    val approvalId: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val toolName: String,
    val explanation: String,
    val affectedPaths: List<String> = emptyList(),
    val commandPreview: String? = null,
    val risk: RiskLevel,
)

sealed interface RuntimeEvent {
    val sessionId: String

    data class SessionStarted(override val sessionId: String) : RuntimeEvent
    data class AssistantDelta(override val sessionId: String, val text: String) : RuntimeEvent
    data class ReasoningProgress(override val sessionId: String, val estimatedTokens: Int) : RuntimeEvent
    data class ReasoningSummary(
        override val sessionId: String,
        val summary: String,
        val blockId: Long,
        val startsNewBlock: Boolean = false,
        val isFinal: Boolean = false,
    ) : RuntimeEvent
    data class ToolStarted(
        override val sessionId: String,
        val toolName: String,
        val detail: String,
    ) : RuntimeEvent
    data class ToolProgress(
        override val sessionId: String,
        val toolName: String,
        val detail: String,
    ) : RuntimeEvent
    data class RuntimeLog(
        override val sessionId: String,
        val title: String,
        val detail: String,
    ) : RuntimeEvent
    data class ToolRequested(override val sessionId: String, val request: ToolRequest) : RuntimeEvent
    data class ToolApproved(override val sessionId: String, val approvalId: String) : RuntimeEvent
    data class ToolRejected(override val sessionId: String, val approvalId: String) : RuntimeEvent
    data class ToolCompleted(override val sessionId: String, val toolName: String, val summary: String) : RuntimeEvent
    data class FilesChanged(override val sessionId: String, val changes: List<ChangeItem>) : RuntimeEvent {
        val paths: List<String> get() = changes.map { it.path }
    }
    data class PreviewStarted(override val sessionId: String, val url: String) : RuntimeEvent
    data class SessionCompleted(override val sessionId: String) : RuntimeEvent
    data class SessionFailed(override val sessionId: String, val reason: String) : RuntimeEvent
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val fromUser: Boolean,
    val text: String,
    val createdAt: Instant = Instant.now(),
    val attachments: List<ChatAttachment> = emptyList(),
    val workItems: List<ActivityItem> = emptyList(),
    val workedMillis: Long = 0L,
)

data class ChatAttachment(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val relativePath: String,
    val mimeType: String,
    val sizeBytes: Long,
)

data class ProjectChat(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New chat",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

enum class DiffLineType { CONTEXT, ADDITION, DELETION, INFO }

data class DiffLine(
    val type: DiffLineType,
    val text: String,
    val oldLine: Int? = null,
    val newLine: Int? = null,
)

data class ChangeItem(
    val path: String,
    val additions: Int,
    val deletions: Int,
    val diffLines: List<DiffLine> = emptyList(),
    val binary: Boolean = false,
    val accepted: Boolean? = null,
)

data class ActivityItem(
    val title: String,
    val detail: String,
    val isComplete: Boolean = true,
    val isCommand: Boolean = false,
)

data class GitFileChange(
    val status: String,
    val path: String,
    val staged: Boolean = false,
)

data class GitStatusSnapshot(
    val initialized: Boolean = false,
    val branch: String = "main",
    val branches: List<String> = emptyList(),
    val remoteUrl: String = "",
    val aheadCount: Int = 0,
    val behindCount: Int = 0,
    val modifiedFiles: List<GitFileChange> = emptyList(),
    val lastCommitSummary: String = "",
    val loading: Boolean = false,
    val busyAction: String? = null,
    val statusMessage: String? = null,
    val statusError: Boolean = false,
) {
    val hasChanges: Boolean get() = modifiedFiles.isNotEmpty()
    val hasRemote: Boolean get() = remoteUrl.isNotBlank()
}

data class EngineSlashCommand(
    val command: String,
    val title: String,
    val description: String,
    val expandedPrompt: String,
    val immediateAction: String? = null,
)

val ENGINE_SLASH_COMMANDS: List<EngineSlashCommand> = listOf(
    EngineSlashCommand(
        command = "/push",
        title = "Commit & Push to GitHub",
        description = "Inspect git status, craft a clean commit message, commit all changes, and push to origin",
        expandedPrompt = "Inspect `git status` and `git diff`, stage all project changes, write a concise conventional commit message, commit, and push the current branch to GitHub (`origin`). Verify the push succeeded.",
    ),
    EngineSlashCommand(
        command = "/fix",
        title = "Auto-Diagnose & Fix Errors",
        description = "Analyze latest terminal/build errors and fix root causes automatically",
        expandedPrompt = "Inspect the recent terminal output, build logs, and modified files in this workspace. Identify the root cause of any errors or failing commands, apply the fix cleanly, and run verification commands to prove it works.",
    ),
    EngineSlashCommand(
        command = "/run",
        title = "Detect & Start Dev Server",
        description = "Install missing dependencies if needed and launch the project's dev/start command",
        expandedPrompt = "Inspect this project's configuration (`package.json`, `pyproject.toml`, `Cargo.toml`, `Makefile`, etc.), install dependencies if missing, and run or verify the main build/test/dev command.",
    ),
    EngineSlashCommand(
        command = "/review",
        title = "Deep Security & Code Audit",
        description = "Audit architecture, edge cases, security bugs, and performance bottlenecks",
        expandedPrompt = "Perform a thorough staff-level code & security review of the current project. Identify concrete bugs, security vulnerabilities, race conditions, and architectural improvements with exact file & line references.",
    ),
    EngineSlashCommand(
        command = "/test",
        title = "Write & Run Unit Tests",
        description = "Generate comprehensive tests for recent changes and execute the test suite",
        expandedPrompt = "Write thorough unit/integration tests covering the core logic and edge cases of this project, then run the test suite and fix any failures until everything passes.",
    ),
    EngineSlashCommand(
        command = "/ci",
        title = "Generate GitHub Actions CI",
        description = "Create a production-ready `.github/workflows` build & release pipeline",
        expandedPrompt = "Create or upgrade `.github/workflows/ci.yml` for this project with caching, automated tests, linting, and release artifact publishing tailored to this codebase.",
    ),
)
