package com.jarves.mh.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.model.ENGINE_SLASH_COMMANDS
import com.jarves.mh.model.EngineSlashCommand
import com.jarves.mh.model.GitStatusSnapshot
import com.jarves.mh.ui.theme.EngineAmber
import com.jarves.mh.ui.theme.EngineCyan
import com.jarves.mh.ui.theme.EngineEmerald
import com.jarves.mh.ui.theme.EngineViolet

/**
 * Tactile spring-physics press feedback modifier for CS Personal Engine cards & actions.
 */
fun Modifier.enginePressScale(
    pressedScale: Float = 0.965f,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "enginePressScale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    pressed = true
                    try {
                        val released = tryAwaitRelease()
                        if (released) onClick()
                    } finally {
                        pressed = false
                    }
                },
            )
        }
}

/**
 * Signature animated orbital core emblem for CS Personal Engine.
 * Pulses gently at idle and spins dual telemetry arcs when the engine is actively streaming or executing.
 */
@Composable
fun AnimatedEngineOrb(
    isRunning: Boolean,
    isConnected: Boolean = true,
    size: Dp = 28.dp,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "engineOrb")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRunning) 1800 else 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbRotation",
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRunning) 650 else 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orbPulse",
    )

    val coreColor = when {
        isRunning -> EngineCyan
        isConnected -> EngineEmerald
        else -> EngineAmber
    }

    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        drawCircle(
            color = coreColor.copy(alpha = if (isRunning) 0.22f * pulse else 0.12f * pulse),
            radius = radius,
        )
        drawArc(
            brush = Brush.sweepGradient(
                listOf(
                    Color.Transparent,
                    coreColor.copy(alpha = 0.85f),
                    EngineViolet.copy(alpha = 0.75f),
                    Color.Transparent,
                ),
            ),
            startAngle = rotation,
            sweepAngle = if (isRunning) 250f else 170f,
            useCenter = false,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round),
        )
        drawCircle(
            color = coreColor,
            radius = radius * (if (isRunning) 0.34f * pulse else 0.28f),
        )
    }
}

/**
 * Real-time Live Writing & Execution Telemetry HUD positioned directly above the chat composer.
 * Gives full live visibility into what the AI is writing, thinking, editing, and running,
 * plus 1-tap toggles for Live Workspace Vision and Auto-Execute Commands.
 */
@Composable
fun LiveEngineTelemetryHud(
    isRunning: Boolean,
    statusMessage: String,
    activeToolSummary: String?,
    streamingPreview: String?,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    autoContextEnabled: Boolean,
    onToggleAutoContext: (Boolean) -> Unit,
    autoRunCommands: Boolean,
    onToggleAutoRunCommands: (Boolean) -> Unit,
    onOpenTerminal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "hudShimmer")
    val shimmerX by infinite.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )
    val cursorBlink by infinite.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursorBlink",
    )

    val borderBrush = if (isRunning) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.outlineVariant,
                EngineCyan.copy(alpha = 0.85f),
                EngineViolet.copy(alpha = 0.7f),
                MaterialTheme.colorScheme.outlineVariant,
            ),
            start = Offset(shimmerX * 600f, 0f),
            end = Offset((shimmerX + 0.5f) * 600f, 160f),
        )
    } else {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
            ),
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderBrush, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleExpanded() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AnimatedEngineOrb(isRunning = isRunning, size = 20.dp)
                    AnimatedContent(
                        targetState = when {
                            !activeToolSummary.isNullOrBlank() && isRunning -> activeToolSummary
                            isRunning -> statusMessage.ifBlank { "Engine streaming live response…" }
                            else -> "CS Engine Live Feed · Ready"
                        },
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                        label = "liveHudHeadline",
                    ) { headline ->
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isRunning) EngineCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MiniTogglePill(
                        label = "Vision",
                        active = autoContextEnabled,
                        activeColor = EngineEmerald,
                        icon = Icons.Default.Visibility,
                        onClick = { onToggleAutoContext(!autoContextEnabled) },
                    )
                    MiniTogglePill(
                        label = "Auto-Exec",
                        active = autoRunCommands,
                        activeColor = EngineAmber,
                        icon = Icons.Default.Bolt,
                        onClick = { onToggleAutoRunCommands(!autoRunCommands) },
                    )
                    IconButton(onClick = onToggleExpanded, modifier = Modifier.size(26.dp)) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = "Toggle Live Stream",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded && (isRunning || !streamingPreview.isNullOrBlank() || !activeToolSummary.isNullOrBlank()),
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    if (!activeToolSummary.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                .clickable { onOpenTerminal() }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = EngineEmerald,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = activeToolSummary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                text = "Console →",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EngineCyan,
                            )
                        }
                    }
                    if (!streamingPreview.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF06080B))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                        ) {
                            val tail = streamingPreview.takeLast(320).trimStart()
                            Text(
                                text = if (isRunning && cursorBlink > 0.55f) "$tail ▊" else tail,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                color = Color(0xFFD8E4F2),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniTogglePill(
    label: String,
    active: Boolean,
    activeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.enginePressScale(onClick = onClick),
        shape = CircleShape,
        color = if (active) activeColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
        border = BorderStroke(
            1.dp,
            if (active) activeColor.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(12.dp),
                tint = if (active) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (active) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Animated Slash-Command Palette (`/` trigger) for instant AI engineering workflows.
 */
@Composable
fun SlashCommandPalette(
    query: String,
    onSelect: (EngineSlashCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trimmed = query.trimStart()
    val visible = trimmed.startsWith("/") && !trimmed.contains(' ')
    val matches = remember(trimmed) {
        if (!visible) emptyList()
        else ENGINE_SLASH_COMMANDS.filter { it.command.startsWith(trimmed.lowercase()) }
    }

    AnimatedVisibility(
        visible = matches.isNotEmpty(),
        enter = expandVertically(spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, EngineCyan.copy(alpha = 0.35f)),
            shadowElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Text(
                    text = "CS ENGINE SLASH WORKFLOWS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = EngineCyan,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                )
                matches.forEach { cmd ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(cmd) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EngineCyan.copy(alpha = 0.14f),
                            ) {
                                Text(
                                    text = cmd.command,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = EngineCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = cmd.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = cmd.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
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
 * Live Dev-Server Port Detector Banner — appears automatically when a local server is listening.
 */
@Composable
fun DetectedDevServerBanner(
    ports: List<Int>,
    onOpenPort: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = ports.isNotEmpty(),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = EngineEmerald.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, EngineEmerald.copy(alpha = 0.45f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    AnimatedEngineOrb(isRunning = false, isConnected = true, size = 18.dp)
                    Text(
                        text = "Live Server Active",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = EngineEmerald,
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ports.forEach { port ->
                        Button(
                            onClick = { onOpenPort(port) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(9.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EngineEmerald,
                                contentColor = Color(0xFF032217),
                            ),
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(":$port", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full Visual Git & GitHub Sync Studio Bottom Sheet.
 * Supports 1-tap GitHub PAT connection, OAuth Web Login, Commit & Push, Pull, Branch switching,
 * Remote configuration, and 1-tap New GitHub Repository Creation & Push.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitControlStudioSheet(
    projectName: String,
    projectSlug: String,
    gitStatus: GitStatusSnapshot,
    isBusy: Boolean,
    feedbackMessage: String?,
    gitHubLogin: String,
    hasGitHubPat: Boolean,
    gitAuthorName: String,
    gitAuthorEmail: String,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onSaveGitHubPat: (pat: String, authorName: String, authorEmail: String) -> Unit,
    onClearGitHubPat: () -> Unit,
    onStartOAuthLogin: () -> Unit,
    onInitRepo: () -> Unit,
    onCommitAndPush: (message: String, pushAfterCommit: Boolean) -> Unit,
    onPull: () -> Unit,
    onCheckoutBranch: (branch: String, create: Boolean) -> Unit,
    onSetRemoteUrl: (url: String) -> Unit,
    onCreateRepoAndPush: (name: String, description: String, isPrivate: Boolean, commitMessage: String) -> Unit,
    onAutoGenerateCommitMsg: () -> String,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var commitMessage by rememberSaveable { mutableStateOf("") }
    var patInput by rememberSaveable { mutableStateOf("") }
    var showPatText by rememberSaveable { mutableStateOf(false) }
    var authorNameInput by rememberSaveable(gitAuthorName) { mutableStateOf(gitAuthorName) }
    var authorEmailInput by rememberSaveable(gitAuthorEmail) { mutableStateOf(gitAuthorEmail) }
    var showTokenEditor by rememberSaveable { mutableStateOf(gitHubLogin.isBlank() && !hasGitHubPat) }

    var newBranchInput by rememberSaveable { mutableStateOf("") }
    var remoteUrlInput by rememberSaveable(gitStatus.remoteUrl) { mutableStateOf(gitStatus.remoteUrl) }
    var createRepoName by rememberSaveable(projectSlug) { mutableStateOf(projectSlug) }
    var createRepoDesc by rememberSaveable(projectName) { mutableStateOf("Built with CS Personal Engine — $projectName") }
    var createRepoPrivate by rememberSaveable { mutableStateOf(true) }
    var showCreateRepoPanel by rememberSaveable(gitStatus.remoteUrl) { mutableStateOf(gitStatus.remoteUrl.isBlank()) }

    val connected = gitHubLogin.isNotBlank() || hasGitHubPat

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
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AnimatedEngineOrb(isRunning = isBusy, isConnected = connected, size = 28.dp)
                    Column {
                        Text(
                            text = "Git & GitHub Push Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (connected) {
                                "Authenticated${if (gitHubLogin.isNotBlank()) " as @$gitHubLogin" else " via GitHub Token"} · Ready to Push"
                            } else {
                                "Connect GitHub Token or Account to enable 1-tap Push"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (connected) EngineEmerald else EngineAmber,
                        )
                    }
                }
                IconButton(onClick = onRefresh, enabled = !isBusy) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Git Status")
                    }
                }
            }

            if (!feedbackMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EngineCyan.copy(alpha = 0.13f),
                    border = BorderStroke(1.dp, EngineCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = feedbackMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            // 1. GitHub Authentication & Identity Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = if (connected) Icons.Default.CheckCircle else Icons.Default.Key,
                                contentDescription = null,
                                tint = if (connected) EngineEmerald else EngineCyan,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = if (connected) "GitHub Push Credentials Active" else "Connect GitHub Credentials",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        TextButton(onClick = { showTokenEditor = !showTokenEditor }) {
                            Text(if (showTokenEditor) "Hide" else if (connected) "Configure" else "Setup")
                        }
                    }

                    AnimatedVisibility(visible = showTokenEditor) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Paste a GitHub Personal Access Token (`ghp_…` or `github_pat_…` with `repo` & `workflow` scopes) for instant push in Chat, Terminal, and Offline builds — or sign in via Browser.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedTextField(
                                value = patInput,
                                onValueChange = { patInput = it },
                                label = { Text(if (hasGitHubPat) "Replace Personal Access Token (saved)" else "GitHub Personal Access Token") },
                                placeholder = { Text("ghp_xxxxxxxxxxxxxxxxxxxx") },
                                singleLine = true,
                                visualTransformation = if (showPatText) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    TextButton(onClick = { showPatText = !showPatText }) {
                                        Text(if (showPatText) "Hide" else "Show", fontSize = 11.sp)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = authorNameInput,
                                    onValueChange = { authorNameInput = it },
                                    label = { Text("Git Author Name") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = authorEmailInput,
                                    onValueChange = { authorEmailInput = it },
                                    label = { Text("Git Commit Email") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = {
                                        onSaveGitHubPat(patInput.trim(), authorNameInput.trim(), authorEmailInput.trim())
                                        patInput = ""
                                    },
                                    enabled = !isBusy && (patInput.isNotBlank() || authorNameInput.isNotBlank() || authorEmailInput.isNotBlank()),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Save Git Credentials")
                                }
                                OutlinedButton(
                                    onClick = onStartOAuthLogin,
                                    enabled = !isBusy,
                                ) {
                                    Text("Web OAuth")
                                }
                                if (hasGitHubPat) {
                                    TextButton(onClick = onClearGitHubPat, enabled = !isBusy) {
                                        Text("Clear PAT", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Repository State & Branch Controls
            if (!gitStatus.initialized) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "This workspace is not a Git repository yet.",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Button(
                            onClick = onInitRepo,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Initialize Git Repository (`main`)")
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Default.CallSplit, contentDescription = null, tint = EngineCyan, modifier = Modifier.size(18.dp))
                                Text(
                                    text = gitStatus.branch,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                )
                                if (gitStatus.aheadCount > 0 || gitStatus.behindCount > 0) {
                                    Text(
                                        text = "↑${gitStatus.aheadCount} ↓${gitStatus.behindCount}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = EngineEmerald,
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = onPull,
                                enabled = !isBusy && gitStatus.hasRemote,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp),
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Pull", fontSize = 12.sp)
                            }
                        }

                        if (gitStatus.lastCommitSummary.isNotBlank()) {
                            Text(
                                text = "Last commit: ${gitStatus.lastCommitSummary}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        // Branch chips + quick create branch
                        if (gitStatus.branches.size > 1) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                gitStatus.branches.forEach { branch ->
                                    FilterChip(
                                        selected = branch == gitStatus.branch,
                                        onClick = { if (branch != gitStatus.branch) onCheckoutBranch(branch, false) },
                                        label = { Text(branch, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EngineCyan.copy(alpha = 0.2f),
                                            selectedLabelColor = EngineCyan,
                                        ),
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = newBranchInput,
                                onValueChange = { newBranchInput = it },
                                placeholder = { Text("feature/new-branch") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedButton(
                                onClick = {
                                    onCheckoutBranch(newBranchInput, true)
                                    newBranchInput = ""
                                },
                                enabled = !isBusy && newBranchInput.isNotBlank(),
                            ) {
                                Text("Checkout -B")
                            }
                        }
                    }
                }

                // 3. Commit & Push Section
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Working Tree (${gitStatus.modifiedFiles.size} changed)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            TextButton(
                                onClick = { commitMessage = onAutoGenerateCommitMsg() },
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Auto-Message", fontSize = 12.sp)
                            }
                        }

                        if (gitStatus.modifiedFiles.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 140.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                    .verticalScroll(rememberScrollState())
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                gitStatus.modifiedFiles.take(40).forEach { file ->
                                    val badgeColor = when {
                                        file.status.contains('?') || file.status.contains('A') -> EngineEmerald
                                        file.status.contains('D') -> MaterialTheme.colorScheme.error
                                        else -> EngineAmber
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = file.status.padEnd(2),
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = badgeColor,
                                        )
                                        Text(
                                            text = file.path,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = commitMessage,
                            onValueChange = { commitMessage = it },
                            label = { Text("Commit message (blank = smart auto-message)") },
                            placeholder = { Text("feat: implement live engine streaming & GitHub sync") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = {
                                    onCommitAndPush(commitMessage, true)
                                    commitMessage = ""
                                },
                                enabled = !isBusy && (gitStatus.hasChanges || gitStatus.aheadCount > 0),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Commit & Push")
                            }
                            OutlinedButton(
                                onClick = {
                                    onCommitAndPush(commitMessage, false)
                                    commitMessage = ""
                                },
                                enabled = !isBusy && gitStatus.hasChanges,
                            ) {
                                Text("Commit Only")
                            }
                        }
                    }
                }
            }

            // 4. Remote Origin & 1-Tap Create GitHub Repository Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = if (gitStatus.hasRemote) "GitHub Remote Linked" else "Publish to New or Existing GitHub Repo",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(onClick = { showCreateRepoPanel = !showCreateRepoPanel }) {
                            Text(if (showCreateRepoPanel) "Collapse" else "New Repo")
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = remoteUrlInput,
                            onValueChange = { remoteUrlInput = it },
                            label = { Text("Remote `origin` URL") },
                            placeholder = { Text("https://github.com/username/repo.git") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(
                            onClick = { onSetRemoteUrl(remoteUrlInput) },
                            enabled = !isBusy && remoteUrlInput.isNotBlank(),
                        ) {
                            Text("Set URL")
                        }
                    }

                    AnimatedVisibility(visible = showCreateRepoPanel) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Text(
                                text = "Create a brand-new GitHub repository & push this workspace in 1 tap:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedTextField(
                                value = createRepoName,
                                onValueChange = { createRepoName = it },
                                label = { Text("New GitHub Repository Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = createRepoDesc,
                                onValueChange = { createRepoDesc = it },
                                label = { Text("Description (optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = if (createRepoPrivate) Icons.Default.Lock else Icons.Default.Public,
                                        contentDescription = null,
                                        tint = if (createRepoPrivate) EngineAmber else EngineEmerald,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = if (createRepoPrivate) "Private Repository" else "Public Repository",
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                                Switch(
                                    checked = createRepoPrivate,
                                    onCheckedChange = { createRepoPrivate = it },
                                )
                            }
                            Button(
                                onClick = {
                                    onCreateRepoAndPush(
                                        createRepoName,
                                        createRepoDesc,
                                        createRepoPrivate,
                                        commitMessage,
                                    )
                                },
                                enabled = !isBusy && createRepoName.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Create GitHub Repo & Push Workspace")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
