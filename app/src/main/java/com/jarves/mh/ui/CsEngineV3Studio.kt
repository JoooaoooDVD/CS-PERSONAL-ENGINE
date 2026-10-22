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
import com.jarves.mh.model.ChatAttachment
import com.jarves.mh.model.ChatMessage
import com.jarves.mh.model.CliToolPackage
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
 * Iconic Google Antigravity 4-point Cosmic Spark + Orbital Halo vector mark.
 */
@Composable
fun AntigravitySparkMark(
    isRunning: Boolean = false,
    size: Dp = 26.dp,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "agy_spark")
    val pulse by infinite.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRunning) 900 else 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "agy_pulse",
    )
    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = (this.size.minDimension / 2f) * 0.82f * (if (isRunning) pulse else 1f)
        val cosmicBrush = Brush.linearGradient(
            colors = listOf(AntigravityBlue, AntigravityViolet, AntigravityAqua),
            start = Offset(0f, 0f),
            end = Offset(this.size.width, this.size.height),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AntigravityViolet.copy(alpha = 0.30f), Color.Transparent),
                center = Offset(cx, cy),
                radius = this.size.minDimension * 0.65f,
            ),
            radius = this.size.minDimension * 0.5f,
            center = Offset(cx, cy),
        )
        // Outer orbital arc
        drawArc(
            brush = cosmicBrush,
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
        drawPath(path = star, brush = cosmicBrush)
    }
}

/**
 * Full-screen welcome hero inside ChatTab — transforms into authentic Google Antigravity UI when
 * Antigravity is selected, or CS Personal Engine Carbon Studio when Claude/DeepSeek/Custom Provider is active.
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
    val accentPrimary = if (isAntigravity) AntigravityBlue else EngineCyan
    val accentSecondary = if (isAntigravity) AntigravityViolet else EngineViolet

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Brand Badge
        Surface(
            shape = CircleShape,
            color = accentPrimary.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, accentPrimary.copy(alpha = 0.32f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isAntigravity) {
                    AntigravitySparkMark(size = 18.dp)
                } else {
                    AnimatedEngineOrb(isRunning = false, size = 18.dp)
                }
                Text(
                    text = if (isAntigravity) "GOOGLE ANTIGRAVITY · TURBO IDE" else "CS PERSONAL ENGINE · FULL ROOT",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    color = accentPrimary,
                )
            }
        }

        Text(
            text = if (isAntigravity) "What should we build in $projectName?" else "Ready to build in $projectName",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // Interactive Model & Effort & Custom Provider Controls (Antigravity signature inline bar)
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.enginePressScale(onClick = onOpenEngineSwitcher),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, accentPrimary.copy(alpha = 0.42f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = accentPrimary, modifier = Modifier.size(14.dp))
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
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf("low" to "Low", "medium" to "Med", "high" to "High").forEach { (id, label) ->
                            val selected = antigravityEffort == id
                            Surface(
                                modifier = Modifier.clickable { onSelectEffort(id) },
                                shape = RoundedCornerShape(9.dp),
                                color = if (selected) accentSecondary.copy(alpha = 0.22f) else Color.Transparent,
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
                shape = RoundedCornerShape(12.dp),
                color = EngineCyan.copy(alpha = 0.11f),
                border = BorderStroke(1.dp, EngineCyan.copy(alpha = 0.38f)),
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
                        color = EngineCyan,
                    )
                }
            }

            Surface(
                modifier = Modifier.enginePressScale(onClick = onOpenCliStore),
                shape = RoundedCornerShape(12.dp),
                color = EngineEmerald.copy(alpha = 0.11f),
                border = BorderStroke(1.dp, EngineEmerald.copy(alpha = 0.38f)),
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
                        color = EngineEmerald,
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
        val bubbleBrush = if (isAntigravity) {
            Brush.linearGradient(
                listOf(
                    AntigravityBlue.copy(alpha = 0.24f),
                    AntigravityViolet.copy(alpha = 0.20f),
                ),
            )
        } else {
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        }
        val borderTint = if (isAntigravity) AntigravityBlue.copy(alpha = 0.45f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, borderTint),
                modifier = Modifier.widthIn(max = 335.dp),
            ) {
                Column(
                    modifier = Modifier
                        .background(bubbleBrush)
                        .padding(horizontal = 15.dp, vertical = 11.dp),
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
        // Assistant Full-Width Response Canvas ("Chat mein pura chat dikhega")
        val accentColor = if (isAntigravity) AntigravityBlue else EngineCyan
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isAntigravity) {
                    AntigravitySparkMark(isRunning = isStreamingNow, size = 18.dp)
                } else {
                    AnimatedEngineOrb(isRunning = isStreamingNow, size = 18.dp)
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
                        color = accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                    ) {
                        Text(
                            text = "LIVE",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor,
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
                shape = RoundedCornerShape(18.dp),
                color = if (isAntigravity) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    1.dp,
                    if (isStreamingNow) accentColor.copy(alpha = 0.48f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f),
                ),
            ) {
                Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
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
 * Structured inline Thought / Terminal / File Artifact Step Card rendered directly inside the Chat timeline.
 * In Antigravity mode, renders with Antigravity's signature Cosmic Violet Thought Header & Dark Console Step Box.
 */
@Composable
fun InlineAgentStepsCard(
    items: List<ActivityItem>,
    isAntigravity: Boolean,
    isRunning: Boolean,
    thinkingActive: Boolean,
    elapsedSeconds: Long,
    onRunInTerminal: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val visibleItems = items.takeLast(24)
    val accent = if (isAntigravity) AntigravityViolet else EngineCyan
    val headerLabel = when {
        isRunning && thinkingActive -> if (isAntigravity) "Antigravity Thinking · ${elapsedSeconds}s" else "Reasoning & Planning · ${elapsedSeconds}s"
        isRunning -> {
            val latest = visibleItems.lastOrNull()
            val cleanTitle = latest?.title?.removePrefix("Running ") ?: "Executing"
            "$cleanTitle · ${elapsedSeconds}s"
        }
        else -> "Completed ${visibleItems.size} agent ${if (visibleItems.size == 1) "step" else "steps"} · ${elapsedSeconds.coerceAtLeast(1L)}s"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isAntigravity) Color(0xFF101624) else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            if (isRunning) accent.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 13.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isRunning) {
                    if (isAntigravity) AntigravitySparkMark(isRunning = true, size = 16.dp)
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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRunning) accent else MaterialTheme.colorScheme.onSurfaceVariant,
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
                        .padding(start = 12.dp, end = 12.dp, bottom = 11.dp),
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
                        if (isBash) {
                            Surface(
                                shape = RoundedCornerShape(11.dp),
                                color = Color(0xFF07090F),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Terminal, null, tint = EngineEmerald, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (step.isComplete) "Terminal Step" else "Running Command…",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EngineEmerald,
                                            modifier = Modifier.weight(1f),
                                        )
                                        val firstLineCmd = step.detail.lineSequence().firstOrNull()?.trim().orEmpty()
                                        if (firstLineCmd.isNotBlank() && firstLineCmd.length < 140) {
                                            Text(
                                                text = "Open in Shell",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.clickable { onRunInTerminal(firstLineCmd) },
                                            )
                                        }
                                    }
                                    Text(
                                        text = step.detail.trim().takeLast(600),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 15.5.sp,
                                        color = Color(0xFFD6E2F0),
                                        maxLines = 8,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        } else if (isThink) {
                            Surface(
                                shape = RoundedCornerShape(11.dp),
                                color = accent.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(Icons.Default.Psychology, null, tint = accent, modifier = Modifier.size(15.dp))
                                    Text(
                                        text = step.detail.trim().ifBlank { "Thinking…" },
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 6,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                Icon(
                                    Icons.Default.Code,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "${step.title.removePrefix("Running ").removeSuffix(" completed")}: ${step.detail.replace('\n', ' ').trim()}",
                                    fontSize = 11.5.sp,
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
                Text("ANTIGRAVITY COSMIC MODELS & EFFORT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AntigravityViolet)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low" to "Low Effort", "medium" to "Medium", "high" to "High Reasoning").forEach { (id, label) ->
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
                            color = if (active) AntigravityBlue.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, if (active) AntigravityBlue else MaterialTheme.colorScheme.outlineVariant),
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
                                if (active) Icon(Icons.Default.CheckCircle, null, tint = AntigravityBlue, modifier = Modifier.size(18.dp))
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
