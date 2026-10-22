package com.jarves.mh.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.model.ActivityItem
import com.jarves.mh.model.AgentKind
import com.jarves.mh.model.BUILTIN_CLI_PACKAGES
import com.jarves.mh.model.ChangeItem
import com.jarves.mh.model.ChatAttachment
import com.jarves.mh.model.ChatMessage
import com.jarves.mh.model.CliToolPackage
import com.jarves.mh.model.DiffLine
import com.jarves.mh.model.DiffLineType
import com.jarves.mh.model.OPENAI_COMPATIBLE_PRESETS
import com.jarves.mh.model.ProviderProfile
import com.jarves.mh.model.SavedCustomProvider
import com.jarves.mh.model.inferredDshApiForUrl
import com.jarves.mh.network.DiscoveredModel
import com.jarves.mh.network.ModelDiscoveryResult
import com.jarves.mh.ui.theme.AntigravityAqua
import com.jarves.mh.ui.theme.AntigravityBlue
import com.jarves.mh.ui.theme.AntigravityViolet
import com.jarves.mh.ui.theme.EngineCyan
import com.jarves.mh.ui.theme.EngineEmerald
import com.jarves.mh.ui.theme.EngineViolet
import kotlinx.coroutines.launch

/**
 * Iconic Google Antigravity 4-point Spark + Orbital Halo vector mark — styled in crisp
 * Platinum / Emerald / Cyan so the app's Dark-Grey / Pitch-Black theme stays 100% pure!
 */
@Composable
fun AntigravitySparkMark(
    isRunning: Boolean = false,
    size: Dp = 26.dp,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "agy_spark")
    val pulse by infinite.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRunning) 800 else 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "agy_pulse",
    )
    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = (this.size.minDimension / 2f) * 0.82f * (if (isRunning) pulse else 1f)
        val sparkBrush = Brush.linearGradient(
            colors = if (isRunning) {
                listOf(Color(0xFFF8FAFC), EngineEmerald, EngineCyan)
            } else {
                listOf(Color(0xFFF8FAFC), Color(0xFFCBD5E1), EngineEmerald)
            },
            start = Offset(0f, 0f),
            end = Offset(this.size.width, this.size.height),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFFFFF).copy(alpha = 0.14f), Color.Transparent),
                center = Offset(cx, cy),
                radius = this.size.minDimension * 0.62f,
            ),
            radius = this.size.minDimension * 0.5f,
            center = Offset(cx, cy),
        )
        // Outer orbital arc
        drawArc(
            brush = sparkBrush,
            startAngle = -35f,
            sweepAngle = 250f,
            useCenter = false,
            style = Stroke(width = this.size.minDimension * 0.065f, cap = StrokeCap.Round),
        )
        // 4-point Antigravity star path
        val waist = r * 0.24f
        val star = Path().apply {
            moveTo(cx, cy - r)
            quadraticBezierTo(cx + waist, cy - waist, cx + r, cy)
            quadraticBezierTo(cx + waist, cy + waist, cx, cy + r)
            quadraticBezierTo(cx - waist, cy + waist, cx - r, cy)
            quadraticBezierTo(cx - waist, cy - waist, cx, cy - r)
            close()
        }
        drawPath(path = star, brush = sparkBrush)
    }
}

/**
 * Full-screen welcome hero inside ChatTab — renders a mobile-friendly Antigravity IDE Mission Control
 * layout when Antigravity is selected, strictly preserving the Dark-Grey / Pitch-Black color scheme!
 */
@Composable
fun FullScreenChatWelcomeHero(
    isAntigravity: Boolean,
    projectName: String,
    activeAgentLabel: String,
    activeModelLabel: String,
    antigravityEffort: String,
    fullAccessEnabled: Boolean,
    onOpenEngineSwitcher: () -> Unit,
    onOpenCustomProviderStudio: () -> Unit,
    onOpenCliStore: () -> Unit,
    onSelectEffort: (String) -> Unit,
    onQuickPrompt: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentPrimary = MaterialTheme.colorScheme.primary
    val accentSecondary = EngineEmerald

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 14.dp),
        horizontalAlignment = if (isAntigravity) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Brand Badge (Pure Dark-Grey / Pitch-Black surface!)
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                if (isAntigravity) {
                    AntigravitySparkMark(size = 16.dp)
                } else {
                    AnimatedEngineOrb(isRunning = false, size = 16.dp)
                }
                Text(
                    text = if (isAntigravity) "ANTIGRAVITY IDE · MOBILE MISSION CONTROL" else "CS PERSONAL ENGINE · FULL ROOT IDE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.7.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Text(
            text = if (isAntigravity) "Build or refactor in $projectName" else "Ready to build in $projectName",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (isAntigravity) {
            // Antigravity IDE Mobile Workspace Readiness Strip (Dark-Grey / Black card)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(7.dp).background(EngineEmerald, CircleShape))
                        Text("Agentic Planner + Diff Viewer + Termux PTY", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("READY", fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = EngineEmerald)
                }
            }
        }

        // Interactive Model & Effort & Custom Provider Controls
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.enginePressScale(onClick = onOpenEngineSwitcher),
                shape = RoundedCornerShape(11.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (isAntigravity) AntigravitySparkMark(size = 13.dp)
                    else Icon(Icons.Default.AutoAwesome, null, tint = accentPrimary, modifier = Modifier.size(14.dp))
                    Text(
                        text = "$activeAgentLabel · $activeModelLabel",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Icon(Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                }
            }

            if (isAntigravity) {
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf("low" to "Fast", "medium" to "Med", "high" to "High").forEach { (id, label) ->
                            val selected = antigravityEffort == id
                            Surface(
                                modifier = Modifier.clickable { onSelectEffort(id) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                border = if (selected) BorderStroke(1.dp, accentSecondary.copy(alpha = 0.55f)) else null,
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.5.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) accentSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.enginePressScale(onClick = onOpenCustomProviderStudio),
                shape = RoundedCornerShape(11.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Default.Add, null, tint = EngineCyan, modifier = Modifier.size(14.dp))
                    Text(
                        text = "Custom Provider",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Surface(
                modifier = Modifier.enginePressScale(onClick = onOpenCliStore),
                shape = RoundedCornerShape(11.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(Icons.Default.Terminal, null, tint = EngineEmerald, modifier = Modifier.size(14.dp))
                    Text(
                        text = if (fullAccessEnabled) "Install Any CLI" else "CLI Hub",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // 2x2 Quick Starter Action Cards
        val starters = listOf(
            Triple(
                "Build Full Feature",
                "Design, implement, and test a complete production feature in this workspace",
                "Inspect this workspace, design a clean architecture, implement the requested feature end-to-end, and verify everything builds and runs.",
            ),
            Triple(
                "Run Commands & CLIs",
                "Use full Ubuntu root terminal (`apt`, `npm -g`, `pip`, `git`, custom CLIs)",
                "Check the environment in `/workspace`, install any needed Linux or npm/pip tools, and run full verification commands.",
            ),
            Triple(
                "Debug & Fix Issues",
                "Scan logs, terminal output & files to fix root causes automatically",
                "Inspect recent errors, terminal logs, and source files in this workspace. Fix every root cause cleanly and verify with tests.",
            ),
            Triple(
                "Commit & Push GitHub",
                "Review `git diff`, craft a clean commit, and push to `origin`",
                "Run `git status` and `git diff`, stage all changes, create a clean commit message, and push to the configured GitHub remote.",
            ),
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            starters.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { (title, desc, promptText) ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .enginePressScale { onQuickPrompt(promptText) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Column(
                                modifier = Modifier.padding(13.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = desc,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-screen Chat Message renderer — replaces cramped bubbles with a spacious Antigravity / Cursor-grade
 * full-screen conversation layout where user prompts are sleek pills and AI responses stream directly
 * across the full chat canvas with copy & runnable bash support.
 */
@Composable
fun FullScreenChatMessageItem(
    message: ChatMessage,
    isAntigravity: Boolean,
    agentDisplayName: String,
    isStreamingNow: Boolean,
    onRunInTerminal: (String) -> Unit,
    onOpenAttachment: (ChatAttachment) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    if (message.fromUser) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 5.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.widthIn(max = 338.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SelectionContainer {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.5.sp,
                                lineHeight = 21.sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    if (message.attachments.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            message.attachments.forEach { attachment ->
                                FullScreenAttachmentPill(attachment = attachment, onClick = { onOpenAttachment(attachment) })
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Assistant Full-Width Response Canvas — pure Dark-Grey / Black surface with mobile IDE polish
        val accentColor = if (isAntigravity) EngineEmerald else MaterialTheme.colorScheme.primary
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isAntigravity) {
                    AntigravitySparkMark(isRunning = isStreamingNow, size = 17.dp)
                } else {
                    AnimatedEngineOrb(isRunning = isStreamingNow, size = 17.dp)
                }
                Text(
                    text = agentDisplayName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (isStreamingNow) {
                    Surface(
                        shape = CircleShape,
                        color = EngineEmerald.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, EngineEmerald.copy(alpha = 0.45f)),
                    ) {
                        Text(
                            text = "STREAMING",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = EngineEmerald,
                        )
                    }
                } else if (message.workedMillis > 0L) {
                    Text(
                        text = "${(message.workedMillis / 1000L).coerceAtLeast(1L)}s",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (message.text.isNotBlank()) {
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(message.text)) },
                        modifier = Modifier.size(26.dp),
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy response",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (isStreamingNow) accentColor.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    SelectionContainer {
                        MarkdownText(
                            markdown = message.text,
                            color = MaterialTheme.colorScheme.onSurface,
                            onRunCode = onRunInTerminal,
                        )
                    }
                    if (message.attachments.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            message.attachments.forEach { attachment ->
                                FullScreenAttachmentPill(attachment = attachment, onClick = { onOpenAttachment(attachment) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenAttachmentPill(
    attachment: ChatAttachment,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                if (attachment.mimeType.startsWith("image/")) Icons.Default.Image else Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = attachment.displayName,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Mobile-Friendly Antigravity / CS Engine Task Mission Control Card — renders directly inside Chat
 * in pure Dark-Grey / Pitch-Black theme with step checkmarks, shell commands, and exact `+N / -N` file diff pills!
 */
@Composable
fun InlineAgentStepsCard(
    items: List<ActivityItem>,
    isAntigravity: Boolean,
    isRunning: Boolean,
    thinkingActive: Boolean,
    elapsedSeconds: Long,
    onRunInTerminal: (String) -> Unit,
    onOpenFile: ((String) -> Unit)? = null,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val visibleItems = items.takeLast(24)
    val completedCount = visibleItems.count { it.isComplete }
    val accent = if (isAntigravity) EngineEmerald else EngineCyan
    val headerLabel = when {
        isRunning && thinkingActive -> if (isAntigravity) "Antigravity Task Planner · ${elapsedSeconds}s" else "Reasoning & Planning · ${elapsedSeconds}s"
        isRunning -> {
            val latest = visibleItems.lastOrNull()
            val cleanTitle = latest?.title?.removePrefix("Running ") ?: "Executing"
            if (isAntigravity) "Task Progress ($completedCount/${visibleItems.size.coerceAtLeast(1)}) · $cleanTitle · ${elapsedSeconds}s"
            else "$cleanTitle · ${elapsedSeconds}s"
        }
        else -> {
            if (isAntigravity) "Task Completed · ${visibleItems.size} ${if (visibleItems.size == 1) "step" else "steps"} · ${elapsedSeconds.coerceAtLeast(1L)}s"
            else "Completed ${visibleItems.size} agent ${if (visibleItems.size == 1) "step" else "steps"} · ${elapsedSeconds.coerceAtLeast(1L)}s"
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isRunning) accent.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isRunning) {
                    if (isAntigravity) AntigravitySparkMark(isRunning = true, size = 15.dp)
                    else CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.8.dp, color = accent)
                } else {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = EngineEmerald,
                    )
                }
                Text(
                    text = headerLabel,
                    modifier = Modifier.weight(1f),
                    fontSize = 11.8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRunning) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 11.dp, end = 11.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (visibleItems.isEmpty() && thinkingActive) {
                        Text(
                            text = "Analyzing workspace architecture & planning execution steps…",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    visibleItems.forEach { step ->
                        val isBash = step.isCommand || step.title.contains("Bash", ignoreCase = true) || step.title.contains("command", ignoreCase = true)
                        val isThink = step.title == "Think"
                        val isCodeDiffStep = step.title.startsWith("Code Updated ·")
                        when {
                            isCodeDiffStep -> {
                                // Compact per-file +Added / -Removed summary preserved right inside the step timeline!
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0A0C10),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(modifier = Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = step.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        step.detail.split('\n').filter { it.isNotBlank() }.forEach { line ->
                                            val match = Regex("^(.+?)\\s+\\+(\\d+)\\s+-(\\d+)$").find(line.trim())
                                            if (match != null) {
                                                val path = match.groupValues[1].trim()
                                                val add = match.groupValues[2]
                                                val del = match.groupValues[3]
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .then(if (onOpenFile != null) Modifier.clickable { onOpenFile(path) } else Modifier)
                                                        .padding(vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                ) {
                                                    FileExtIdeBadge(path = path)
                                                    Text(
                                                        text = path,
                                                        modifier = Modifier.weight(1f),
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    DiffCountPills(additions = add.toIntOrNull() ?: 0, deletions = del.toIntOrNull() ?: 0)
                                                }
                                            } else {
                                                Text(
                                                    text = line,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            isBash -> {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF07090D),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(modifier = Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Terminal, null, tint = EngineEmerald, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = if (step.isComplete) "Bash · Exit OK" else "Running in Linux…",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EngineEmerald,
                                                modifier = Modifier.weight(1f),
                                            )
                                            val firstLineCmd = step.detail.lineSequence().firstOrNull()?.trim().orEmpty()
                                            if (firstLineCmd.isNotBlank() && firstLineCmd.length < 140) {
                                                Text(
                                                    text = "Run in Termux ›",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EngineCyan,
                                                    modifier = Modifier.clickable { onRunInTerminal(firstLineCmd) },
                                                )
                                            }
                                        }
                                        Text(
                                            text = step.detail.trim().takeLast(600),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            color = Color(0xFFD6E2F0),
                                            maxLines = 8,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                            isThink -> {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(Icons.Default.Psychology, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = step.detail.trim().ifBlank { "Thinking…" },
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 6,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                            else -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                ) {
                                    Icon(
                                        if (step.isComplete) Icons.Default.Check else Icons.Default.Code,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = if (step.isComplete) EngineEmerald else MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = "${step.title.removePrefix("Running ").removeSuffix(" completed")}: ${step.detail.replace('\n', ' ').trim()}",
                                        fontSize = 11.2.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Instant Engine, Model, Effort & Custom Provider Switcher accessible directly from the Workspace Top Bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceQuickEngineSheet(
    state: AppUiState,
    onDismiss: () -> Unit,
    onSelectAgent: (AgentKind) -> Unit,
    onSetAntigravityModel: (String) -> Unit,
    onSetAntigravityEffort: (String) -> Unit,
    onActivateCustomProvider: (String) -> Unit,
    onDeleteCustomProvider: (String) -> Unit,
    onOpenAddCustomProvider: (SavedCustomProvider?) -> Unit,
    onToggleFullAccess: (Boolean) -> Unit,
    onOpenCliStore: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val antigravityModels = remember(state.antigravityModels) {
        state.antigravityModels.ifEmpty {
            listOf(
                "gemini-3.8-flash-high",
                "gemini-3.8-flash-medium",
                "gemini-3.1-pro-high",
                "claude-sonnet-4-6",
                "claude-opus-4-6-thinking",
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Engine, Model & Custom Providers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Switch coding engine, reasoning model, or activate any custom API provider instantly.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = { onOpenAddCustomProvider(null) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Provider", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 1. Coding Agent Engine Picker
            Text("CODING ENGINE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AgentKind.entries.forEach { agent ->
                    val selected = state.agentKind == agent
                    val installed = state.installedAgentVersions.containsKey(agent)
                    val title = when (agent) {
                        AgentKind.ANTIGRAVITY -> "Antigravity"
                        AgentKind.CLAUDE_CODE -> "Claude Code"
                        AgentKind.DEEPSEEK_HARNESS -> "DeepSeek"
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = installed && !state.isRunning) { onSelectAgent(agent) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 11.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (installed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                maxLines = 1,
                            )
                            Text(
                                text = if (installed) (if (selected) "Active" else "Ready") else "Install in Agent tab",
                                fontSize = 10.sp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            // 2. Antigravity Model & Effort Controls when Antigravity is active
            if (state.agentKind == AgentKind.ANTIGRAVITY) {
                Text("ANTIGRAVITY IDE MODELS & REASONING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EngineEmerald)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low" to "Fast", "medium" to "Balanced", "high" to "High Reasoning").forEach { (id, label) ->
                        FilterChip(
                            selected = state.antigravityEffort == id,
                            onClick = { onSetAntigravityEffort(id) },
                            label = { Text(label, fontSize = 11.5.sp) },
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    antigravityModels.take(8).forEach { modelId ->
                        val active = state.antigravityModel == modelId || (state.antigravityModel.isBlank() && modelId == "gemini-3.8-flash-high")
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSetAntigravityModel(modelId) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) EngineEmerald.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, if (active) EngineEmerald else MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AntigravitySparkMark(size = 16.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(formatAntigravityModelName(modelId), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(modelId, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (active) Icon(Icons.Default.CheckCircle, null, tint = EngineEmerald, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // 3. Saved Custom Providers List (Always visible! 1-tap switches engine & activates!)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "SAVED CUSTOM PROVIDERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EngineCyan,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onOpenAddCustomProvider(null) }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+ Add Custom Provider", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (state.savedCustomProviders.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAddCustomProvider(null) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Default.Hub, null, tint = EngineCyan)
                        Column(Modifier.weight(1f)) {
                            Text("Add OpenAI / Groq / Ollama / Custom API Provider", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Connect any custom Base URL, API key, model ID, and wire protocol.", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.savedCustomProviders.forEach { item ->
                        val isCurrent = state.agentKind != AgentKind.ANTIGRAVITY && state.activeCustomProviderId == item.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onActivateCustomProvider(item.id) },
                            shape = RoundedCornerShape(13.dp),
                            color = if (isCurrent) EngineCyan.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, if (isCurrent) EngineCyan else MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                        if (isCurrent) {
                                            Text("ACTIVE", fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, color = EngineCyan)
                                        }
                                    }
                                    Text(
                                        "${item.model} · ${item.baseUrl}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                IconButton(onClick = { onOpenAddCustomProvider(item) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { onDeleteCustomProvider(item.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.DeleteOutline, "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Full Root Terminal & CLI Hub Controls
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, null, tint = EngineEmerald, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Unrestricted Full Root Terminal & AI Access", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Zero command blocks · AI & Terminal can run any binary, apt, npm, pip, or CLI", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = state.fullAccessTerminalEnabled, onCheckedChange = onToggleFullAccess)
                    }
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenCliStore()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(11.dp),
                    ) {
                        Icon(Icons.Default.Terminal, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Open CLI Store & External Agent Installer (Codex, Gemini, Aider…)", fontSize = 11.5.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

/**
 * Dedicated `+ Add Custom Provider` Multi-Profile Creator & Manager Bottom Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomProviderStudioSheet(
    editingProvider: SavedCustomProvider? = null,
    initialApiKey: String = "",
    savedProviders: List<SavedCustomProvider>,
    activeCustomProviderId: String,
    onDiscoverModels: suspend (ProviderProfile, String) -> ModelDiscoveryResult,
    onSaveAndActivate: (SavedCustomProvider, String) -> Unit,
    onActivateExisting: (String) -> Unit,
    onDeleteExisting: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var name by rememberSaveable(editingProvider?.id) { mutableStateOf(editingProvider?.name ?: "") }
    var baseUrl by rememberSaveable(editingProvider?.id) { mutableStateOf(editingProvider?.baseUrl ?: "https://api.openai.com/v1") }
    var model by rememberSaveable(editingProvider?.id) { mutableStateOf(editingProvider?.model ?: "gpt-4.1") }
    var wireProtocol by rememberSaveable(editingProvider?.id) { mutableStateOf(editingProvider?.wireProtocol ?: "openai-completions") }
    var apiKey by rememberSaveable(editingProvider?.id) { mutableStateOf(initialApiKey) }
    var customHeaders by rememberSaveable(editingProvider?.id) { mutableStateOf(editingProvider?.customHeaders ?: "") }
    var showKey by rememberSaveable { mutableStateOf(false) }

    var discovering by remember { mutableStateOf(false) }
    var discoveredModels by remember { mutableStateOf<List<DiscoveredModel>>(emptyList()) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Hub, null, tint = EngineCyan, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (editingProvider == null) "Add Custom AI Provider" else "Edit Custom Provider",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        "Connect OpenAI, Groq, Together, OpenRouter, Ollama, vLLM, or any Anthropic/OpenAI endpoint.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Quick-Fill Preset Chips
            Text("QUICK-FILL TEMPLATES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EngineCyan)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                OPENAI_COMPATIBLE_PRESETS.forEach { preset ->
                    FilterChip(
                        selected = baseUrl.trim() == preset.baseUrl,
                        onClick = {
                            if (name.isBlank() || OPENAI_COMPATIBLE_PRESETS.any { it.label == name }) {
                                name = preset.label
                            }
                            baseUrl = preset.baseUrl
                            model = preset.defaultModel
                            wireProtocol = preset.wireApi
                        },
                        label = { Text(preset.label, fontSize = 11.5.sp) },
                    )
                }
                FilterChip(
                    selected = wireProtocol == "anthropic-messages",
                    onClick = {
                        if (name.isBlank()) name = "Custom Anthropic Gateway"
                        baseUrl = "https://api.anthropic.com"
                        model = "claude-sonnet-4-6"
                        wireProtocol = "anthropic-messages"
                    },
                    label = { Text("Anthropic Proxy", fontSize = 11.5.sp) },
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Provider Name (e.g. My Groq / Local Ollama / Proxy)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            // Wire Protocol Selector
            Text("WIRE PROTOCOL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    "openai-completions" to "OpenAI /chat/completions",
                    "openai-responses" to "OpenAI /responses",
                    "anthropic-messages" to "Anthropic /v1/messages",
                ).forEach { (proto, title) ->
                    FilterChip(
                        selected = wireProtocol == proto,
                        onClick = { wireProtocol = proto },
                        label = { Text(title, fontSize = 11.5.sp) },
                    )
                }
            }

            OutlinedTextField(
                value = baseUrl,
                onValueChange = {
                    baseUrl = it
                    wireProtocol = inferredDshApiForUrl(it)
                },
                label = { Text("Base URL (e.g. https://api.openai.com/v1)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key (optional for local Ollama / LM Studio)") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model ID (e.g. gpt-4.1, llama-3.3-70b)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            discovering = true
                            feedback = null
                            val tempEntry = SavedCustomProvider(
                                name = name.ifBlank { "Custom" },
                                baseUrl = baseUrl,
                                model = model,
                                wireProtocol = wireProtocol,
                                customHeaders = customHeaders,
                            )
                            when (val res = onDiscoverModels(tempEntry.toProviderProfile(), apiKey.trim())) {
                                is ModelDiscoveryResult.Success -> {
                                    discoveredModels = res.models
                                    if (model.isBlank() && res.models.isNotEmpty()) {
                                        model = res.models.first().id
                                    }
                                    feedback = "Discovered ${res.models.size} models from endpoint"
                                    feedbackError = false
                                }
                                is ModelDiscoveryResult.Failure -> {
                                    feedback = res.message
                                    feedbackError = true
                                }
                            }
                            discovering = false
                        }
                    },
                    enabled = baseUrl.isNotBlank() && !discovering,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    if (discovering) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Fetch", fontSize = 12.sp)
                    }
                }
            }

            if (discoveredModels.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    discoveredModels.take(25).forEach { m ->
                        FilterChip(
                            selected = model == m.id,
                            onClick = { model = m.id },
                            label = { Text(m.id, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = customHeaders,
                onValueChange = { customHeaders = it },
                label = { Text("Custom HTTP Headers (Optional · Key: Value per line)") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            feedback?.let { msg ->
                Text(
                    text = msg,
                    fontSize = 12.sp,
                    color = if (feedbackError) MaterialTheme.colorScheme.error else EngineEmerald,
                )
            }

            Button(
                onClick = {
                    val entry = SavedCustomProvider(
                        id = editingProvider?.id ?: java.util.UUID.randomUUID().toString(),
                        name = name.trim().ifBlank {
                            baseUrl.substringAfter("://").substringBefore('/').ifBlank { "Custom Provider" }
                        },
                        baseUrl = baseUrl.trim(),
                        model = model.trim(),
                        wireProtocol = wireProtocol,
                        customHeaders = customHeaders.trim(),
                    )
                    onSaveAndActivate(entry, apiKey.trim())
                    onDismiss()
                },
                enabled = baseUrl.isNotBlank() && model.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EngineCyan, contentColor = Color(0xFF04131D)),
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save & Activate Custom Provider", fontWeight = FontWeight.ExtraBold)
            }

            if (savedProviders.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("YOUR SAVED CUSTOM PROVIDERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                savedProviders.forEach { saved ->
                    val isCurrent = saved.id == activeCustomProviderId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onActivateExisting(saved.id)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrent) EngineCyan.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, if (isCurrent) EngineCyan else MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(saved.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    "${saved.model} · ${saved.baseUrl}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (isCurrent) {
                                Text("ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = EngineCyan)
                                Spacer(Modifier.width(6.dp))
                            }
                            IconButton(onClick = { onDeleteExisting(saved.id) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Universal Linux CLI & External Coding Agent Store (install & run Codex CLI, Gemini CLI, OpenCode,
 * Aider, Qwen Code, gh, ripgrep/nvim/tmux, uv, Bun, or ANY user-defined custom CLI command!).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CliMarketplaceSheet(
    savedCustomClis: List<CliToolPackage>,
    onRunCommandInTerminal: (String) -> Unit,
    onAddCustomCli: (name: String, installCmd: String, launchCmd: String) -> Unit,
    onRemoveCustomCli: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddForm by rememberSaveable { mutableStateOf(false) }
    var customName by rememberSaveable { mutableStateOf("") }
    var customInstall by rememberSaveable { mutableStateOf("") }
    var customLaunch by rememberSaveable { mutableStateOf("") }

    val allPackages = remember(savedCustomClis) { savedCustomClis + BUILTIN_CLI_PACKAGES }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, null, tint = EngineEmerald, modifier = Modifier.size(23.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Linux CLI & Coding Agent Hub", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Full root Ubuntu arm64 access — install & run any CLI, package manager, or AI agent.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(
                        onClick = { showAddForm = !showAddForm },
                        shape = RoundedCornerShape(11.dp),
                        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Custom CLI", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (showAddForm) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, EngineEmerald.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Register Any Custom CLI / Shell Tool", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("CLI Name (e.g. Deno / Rust Cargo / Custom Agent)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = customInstall,
                                onValueChange = { customInstall = it },
                                label = { Text("Install Command (apt / npm -g / pip / curl | sh)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = customLaunch,
                                onValueChange = { customLaunch = it },
                                label = { Text("Launch Command (e.g. deno --version)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onAddCustomCli(customName, customInstall, customLaunch)
                                        customName = ""
                                        customInstall = ""
                                        customLaunch = ""
                                        showAddForm = false
                                    },
                                    enabled = customInstall.isNotBlank(),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Save CLI Preset")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val cmd = customInstall.trim()
                                        if (cmd.isNotBlank()) {
                                            onDismiss()
                                            onRunCommandInTerminal(cmd)
                                        }
                                    },
                                    enabled = customInstall.isNotBlank(),
                                ) {
                                    Text("Run Now")
                                }
                            }
                        }
                    }
                }
            }

            items(allPackages, key = { it.id }) { pkg ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Text(pkg.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Surface(
                                        shape = CircleShape,
                                        color = EngineEmerald.copy(alpha = 0.14f),
                                    ) {
                                        Text(
                                            pkg.category,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EngineEmerald,
                                        )
                                    }
                                }
                                Text(pkg.description, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (pkg.isCustom) {
                                IconButton(onClick = { onRemoveCustomCli(pkg.id) }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Text(
                            text = "$ ${pkg.installCommand}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onDismiss()
                                    onRunCommandInTerminal(pkg.installCommand)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                            ) {
                                Icon(Icons.Default.Download, null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Install in Linux", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onRunCommandInTerminal(pkg.launchCommand)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Launch `${pkg.launchCommand.take(14)}`", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════════════════
// v4.0.0 MOBILE IDE CODE DIFF ENGINE (+ADDED / -REMOVED LINES) & ANTIGRAVITY COMPOSER BAR
// ═════════════════════════════════════════════════════════════════════════════════════════

private val IdeDiffGreen = Color(0xFF34D399)
private val IdeDiffRed = Color(0xFFF87171)

/**
 * Fast line-level diff counter (`+addedLines` to `-removedLines`) used by the live mobile code editor
 * and test suite to show instant `+N / -N` line changes as you edit or update files.
 */
internal fun computeLineDiffStats(original: String, modified: String): Pair<Int, Int> {
    if (original == modified) return 0 to 0
    val oldLines = if (original.isEmpty()) emptyList() else original.replace("\r\n", "\n").split('\n')
    val newLines = if (modified.isEmpty()) emptyList() else modified.replace("\r\n", "\n").split('\n')
    // Prefix/suffix trim + multiset/windowed LCS for ultra-fast mobile responsiveness
    var prefix = 0
    val minLen = minOf(oldLines.size, newLines.size)
    while (prefix < minLen && oldLines[prefix] == newLines[prefix]) {
        prefix++
    }
    var oldEnd = oldLines.size - 1
    var newEnd = newLines.size - 1
    while (oldEnd >= prefix && newEnd >= prefix && oldLines[oldEnd] == newLines[newEnd]) {
        oldEnd--
        newEnd--
    }
    val remOld = oldLines.subList(prefix, oldEnd + 1)
    val remNew = newLines.subList(prefix, newEnd + 1)
    if (remOld.isEmpty()) return remNew.size to 0
    if (remNew.isEmpty()) return 0 to remOld.size
    if (remOld.size.toLong() * remNew.size.toLong() <= 160_000L) {
        val m = remOld.size
        val n = remNew.size
        var prev = IntArray(n + 1)
        var curr = IntArray(n + 1)
        for (i in 1..m) {
            val oldLine = remOld[i - 1]
            for (j in 1..n) {
                curr[j] = if (oldLine == remNew[j - 1]) {
                    prev[j - 1] + 1
                } else {
                    maxOf(prev[j], curr[j - 1])
                }
            }
            val tmp = prev
            prev = curr
            curr = tmp
            curr.fill(0)
        }
        val common = prev[n]
        return (n - common) to (m - common)
    }
    // Fallback multiset frequency match for giant (>400x400 changed block) files in <1ms
    val counts = HashMap<String, Int>(remOld.size * 2)
    for (line in remOld) counts[line] = (counts[line] ?: 0) + 1
    var matched = 0
    for (line in remNew) {
        val c = counts[line] ?: 0
        if (c > 0) {
            matched++
            counts[line] = c - 1
        }
    }
    return (remNew.size - matched) to (remOld.size - matched)
}

@Composable
fun DiffCountPills(
    additions: Int,
    deletions: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(5.dp),
            color = IdeDiffGreen.copy(alpha = 0.14f),
            border = BorderStroke(0.7.dp, IdeDiffGreen.copy(alpha = 0.42f)),
        ) {
            Text(
                text = "+$additions",
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = IdeDiffGreen,
            )
        }
        Surface(
            shape = RoundedCornerShape(5.dp),
            color = IdeDiffRed.copy(alpha = 0.14f),
            border = BorderStroke(0.7.dp, IdeDiffRed.copy(alpha = 0.42f)),
        ) {
            Text(
                text = "-$deletions",
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = IdeDiffRed,
            )
        }
    }
}

@Composable
fun DiffProportionalStatBar(
    additions: Int,
    deletions: Int,
    blocks: Int = 5,
) {
    val total = (additions + deletions).coerceAtLeast(1)
    val greenBlocks = if (additions == 0) 0 else ((additions.toFloat() / total) * blocks).toInt().coerceAtLeast(1).coerceAtMost(blocks)
    val redBlocks = if (deletions == 0) 0 else (blocks - greenBlocks).coerceAtLeast(if (greenBlocks == blocks) 0 else 1)
    val actualGreen = if (deletions > 0 && greenBlocks == blocks) blocks - 1 else greenBlocks
    val actualRed = (blocks - actualGreen).coerceAtMost(if (deletions > 0) blocks else 0)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(blocks) { idx ->
            val blockColor = when {
                idx < actualGreen -> IdeDiffGreen
                idx < actualGreen + actualRed -> IdeDiffRed
                else -> Color(0xFF2A303C)
            }
            Box(
                modifier = Modifier
                    .size(width = 6.dp, height = 6.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(blockColor),
            )
        }
    }
}

@Composable
fun FileExtIdeBadge(path: String) {
    val ext = path.substringAfterLast('.', "").uppercase().take(4).ifBlank { "FILE" }
    val tint = when (ext) {
        "KT", "KTS" -> Color(0xFFA78BFA)
        "TS", "TSX", "JS", "JSX" -> Color(0xFF38BDF8)
        "PY" -> Color(0xFFFACC15)
        "RS", "GO", "C", "CPP", "H" -> Color(0xFFFB923C)
        "SH", "BASH", "YML", "YAML" -> IdeDiffGreen
        "JSON", "TOML", "XML" -> Color(0xFFF472B6)
        else -> Color(0xFF94A3B8)
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = tint.copy(alpha = 0.14f),
        border = BorderStroke(0.7.dp, tint.copy(alpha = 0.4f)),
    ) {
        Text(
            text = ext,
            modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = tint,
        )
    }
}

/**
 * Expandable Mobile IDE Code Diff Deck rendered directly inside Chat AND inside the Diffs tab!
 * Displays exact `+lines added` and `-lines removed`, proportional stat bars, syntax-highlighted
 * unified diff lines with line numbers, and 1-tap `Open`, `Keep ✓`, and `Undo ↺` controls.
 */
@Composable
fun CodeChangesDiffDeck(
    changes: List<ChangeItem>,
    onOpenFile: ((String) -> Unit)? = null,
    onKeepAll: () -> Unit,
    onUndoAll: () -> Unit,
    onKeepFile: (String) -> Unit,
    onUndoFile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (changes.isEmpty()) return
    val totalAdditions = remember(changes) { changes.sumOf { it.additions } }
    val totalDeletions = remember(changes) { changes.sumOf { it.deletions } }
    var expandedFilePath by rememberSaveable { mutableStateOf<String?>(changes.firstOrNull()?.path) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Deck Summary Header: "3 files updated  +142  -28  ■■■▪▪   [Undo All] [Keep All]"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.Code, contentDescription = null, tint = IdeDiffGreen, modifier = Modifier.size(16.dp))
                Text(
                    text = "${changes.size} ${if (changes.size == 1) "file" else "files"} updated",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                DiffCountPills(additions = totalAdditions, deletions = totalDeletions)
                DiffProportionalStatBar(additions = totalAdditions, deletions = totalDeletions)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Revert",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = IdeDiffRed,
                    modifier = Modifier
                        .clickable(onClick = onUndoAll)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
                Surface(
                    onClick = onKeepAll,
                    shape = RoundedCornerShape(7.dp),
                    color = IdeDiffGreen.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, IdeDiffGreen.copy(alpha = 0.5f)),
                ) {
                    Text(
                        text = "Keep All ✓",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = IdeDiffGreen,
                    )
                }
            }

            // Per-file Expandable Unified Diff Cards
            changes.forEach { change ->
                val isExpanded = expandedFilePath == change.path
                val fileName = change.path.substringAfterLast('/')
                val parentDir = change.path.substringBeforeLast('/', "")
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = Color(0xFF0A0C10),
                    border = BorderStroke(
                        1.dp,
                        if (isExpanded) IdeDiffGreen.copy(alpha = 0.42f) else MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedFilePath = if (isExpanded) null else change.path }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            FileExtIdeBadge(path = change.path)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fileName,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF1F5F9),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (parentDir.isNotBlank()) {
                                    Text(
                                        text = parentDir,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.8.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            DiffCountPills(additions = change.additions, deletions = change.deletions)
                            Icon(
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                HorizontalDivider(color = Color(0xFF1A1F29))
                                if (change.binary) {
                                    Text(
                                        text = "Binary file modified",
                                        modifier = Modifier.padding(10.dp),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8),
                                    )
                                } else if (change.diffLines.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 260.dp)
                                            .verticalScroll(rememberScrollState())
                                            .horizontalScroll(rememberScrollState())
                                            .background(Color(0xFF06080B))
                                            .padding(vertical = 4.dp),
                                    ) {
                                        change.diffLines.take(180).forEach { line ->
                                            IdeUnifiedDiffRow(line = line)
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color(0xFF1A1F29))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0D1016))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (onOpenFile != null && !change.binary) {
                                        Surface(
                                            onClick = { onOpenFile(change.path) },
                                            shape = RoundedCornerShape(7.dp),
                                            color = Color(0xFF171C26),
                                            border = BorderStroke(1.dp, Color(0xFF283040)),
                                        ) {
                                            Text(
                                                text = "Open in Editor",
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EngineCyan,
                                            )
                                        }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Surface(
                                        onClick = {
                                            expandedFilePath = null
                                            onUndoFile(change.path)
                                        },
                                        shape = RoundedCornerShape(7.dp),
                                        color = Color(0xFF221115),
                                        border = BorderStroke(1.dp, IdeDiffRed.copy(alpha = 0.4f)),
                                    ) {
                                        Text(
                                            text = "Undo ↺",
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = IdeDiffRed,
                                        )
                                    }
                                    Surface(
                                        onClick = {
                                            expandedFilePath = null
                                            onKeepFile(change.path)
                                        },
                                        shape = RoundedCornerShape(7.dp),
                                        color = IdeDiffGreen.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, IdeDiffGreen.copy(alpha = 0.48f)),
                                    ) {
                                        Text(
                                            text = "Keep ✓",
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = IdeDiffGreen,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdeUnifiedDiffRow(line: DiffLine) {
    val marker = when (line.type) {
        DiffLineType.ADDITION -> "+"
        DiffLineType.DELETION -> "-"
        DiffLineType.CONTEXT -> " "
        DiffLineType.INFO -> "@@"
    }
    val rowBg = when (line.type) {
        DiffLineType.ADDITION -> Color(0xFF0D281B)
        DiffLineType.DELETION -> Color(0xFF2D1217)
        DiffLineType.INFO -> Color(0xFF111620)
        DiffLineType.CONTEXT -> Color.Transparent
    }
    val textFg = when (line.type) {
        DiffLineType.ADDITION -> Color(0xFF6EE7B7)
        DiffLineType.DELETION -> Color(0xFFFCA5A5)
        DiffLineType.INFO -> Color(0xFF94A3B8)
        DiffLineType.CONTEXT -> Color(0xFFCBD5E1)
    }
    val oldNum = line.oldLine?.toString().orEmpty().padStart(4)
    val newNum = line.newLine?.toString().orEmpty().padStart(4)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .padding(horizontal = 6.dp, vertical = 1.5.dp),
    ) {
        Text(
            text = "$oldNum $newNum ",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            lineHeight = 14.5.sp,
            color = Color(0xFF475569),
            softWrap = false,
        )
        Text(
            text = "$marker ${line.text}",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.8.sp,
            lineHeight = 14.5.sp,
            color = textFg,
            softWrap = false,
        )
    }
}

/**
 * Sleek Mobile-Friendly Antigravity / CS Engine Micro-Control Strip docked right above the prompt capsule:
 * Gives 1-tap access to Model/Engine switcher, Antigravity effort (`Fast`/`Med`/`High`), `+API`, `>_ Termux`,
 * and live `+A / -D` modified lines counter!
 */
@Composable
fun AntigravityMobileComposerStrip(
    isAntigravity: Boolean,
    activeAgentLabel: String,
    activeModelLabel: String,
    antigravityEffort: String,
    gitBranch: String,
    totalAdditions: Int,
    totalDeletions: Int,
    changedFilesCount: Int,
    onOpenEngineSwitcher: () -> Unit,
    onSelectEffort: (String) -> Unit,
    onOpenCustomProvider: () -> Unit,
    onOpenTermux: () -> Unit,
    onClickDiffBadge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Active Engine + Model selector pill (Antigravity signature inline selector)
        Surface(
            onClick = onOpenEngineSwitcher,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (isAntigravity) AntigravitySparkMark(size = 11.dp)
                else Box(Modifier.size(6.dp).background(EngineCyan, CircleShape))
                Text(
                    text = activeModelLabel.ifBlank { activeAgentLabel },
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Icon(Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
            }
        }

        // Antigravity Effort pill (`Fast / Med / High`)
        if (isAntigravity) {
            val nextEffort = when (antigravityEffort) {
                "low" -> "medium"
                "medium" -> "high"
                else -> "low"
            }
            val effortDisplay = when (antigravityEffort) {
                "low" -> "⚡ Fast"
                "medium" -> "⚡ Balanced"
                else -> "✦ High"
            }
            Surface(
                onClick = { onSelectEffort(nextEffort) },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    text = effortDisplay,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = EngineEmerald,
                )
            }
        } else {
            Surface(
                onClick = onOpenCustomProvider,
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    text = "+ API",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = EngineCyan,
                )
            }
        }

        // Live Diff Badge (`+A -D`) right on the IDE bar whenever files are modified!
        if (changedFilesCount > 0) {
            Surface(
                onClick = onClickDiffBadge,
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0E1713),
                border = BorderStroke(1.dp, IdeDiffGreen.copy(alpha = 0.45f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = "${changedFilesCount}f",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE2E8F0),
                    )
                    Text(
                        text = "+$totalAdditions",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = IdeDiffGreen,
                    )
                    Text(
                        text = "-$totalDeletions",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = IdeDiffRed,
                    )
                }
            }
        }

        // Active Git Branch micro-pill + 1-tap Termux launcher
        if (gitBranch.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    text = "⎇ $gitBranch",
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Surface(
            onClick = onOpenTermux,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Text(
                text = ">_ Termux",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = EngineEmerald,
            )
        }
    }
}

