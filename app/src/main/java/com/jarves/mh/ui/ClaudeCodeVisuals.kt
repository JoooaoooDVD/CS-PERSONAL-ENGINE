package com.jarves.mh.ui

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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.model.ChangeItem
import com.jarves.mh.model.ChatAttachment
import com.jarves.mh.model.ChatMessage
import com.jarves.mh.ui.theme.ClaudeDarkBg
import com.jarves.mh.ui.theme.ClaudeDarkBorder
import com.jarves.mh.ui.theme.ClaudeDarkCard
import com.jarves.mh.ui.theme.ClaudeDarkSurface
import com.jarves.mh.ui.theme.ClaudeTerracotta
import com.jarves.mh.ui.theme.ClaudeTextPrimary
import com.jarves.mh.ui.theme.ClaudeTextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

// ═════════════════════════════════════════════════════════════════════════════
// 1. ZERO-EMOJI CRISP VECTOR GEOMETRY ICONS FOR CLAUDE CODE UI
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Authentic Claude Code animated 6-spoke starburst vector spinner (100% pure Canvas geometry —
 * zero Unicode emoji font fallback on any Android OEM!).
 */
@Composable
fun ClaudeStarburstSpinner(
    size: Dp = 14.dp,
    color: Color = ClaudeTerracotta,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "claude_starburst")
    val angleDeg by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
        ),
        label = "starburst_rot",
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 680, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "starburst_pulse",
    )

    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val maxR = (this.size.minDimension / 2f) * 0.90f
        val strokeW = (this.size.minDimension * 0.13f).coerceAtLeast(1.6f)

        // Subtle warm halo
        drawCircle(
            color = color.copy(alpha = 0.16f * pulse),
            radius = maxR * 1.05f,
            center = Offset(cx, cy),
        )

        val baseRad = angleDeg * (PI.toFloat() / 180f)
        for (i in 0 until 6) {
            val theta = baseRad + i * (PI.toFloat() / 3f)
            val spokeScale = if (i % 2 == 0) pulse else (1.72f - pulse).coerceIn(0.65f, 1.0f)
            val r = maxR * spokeScale
            val innerR = maxR * 0.18f
            drawLine(
                color = color,
                start = Offset(cx + cos(theta) * innerR, cy + sin(theta) * innerR),
                end = Offset(cx + cos(theta) * r, cy + sin(theta) * r),
                strokeWidth = strokeW,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(
            color = color,
            radius = strokeW * 0.65f,
            center = Offset(cx, cy),
        )
    }
}

/**
 * Crisp vector `>` prompt chevron (replaces Unicode `❯` text so spacing & stroke are razor-sharp).
 */
@Composable
fun ClaudePromptChevron(
    size: Dp = 13.dp,
    color: Color = ClaudeTerracotta,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.26f, h * 0.18f)
            lineTo(w * 0.74f, h * 0.50f)
            lineTo(w * 0.26f, h * 0.82f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = (this.size.minDimension * 0.16f).coerceAtLeast(1.8f),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

/**
 * Crisp vector `⎿` L-branch tree connector for tool/bash/diff sub-steps.
 */
@Composable
fun ClaudeTreeBranchIcon(
    size: Dp = 13.dp,
    color: Color = Color(0xFF5A5A64),
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.28f, h * 0.10f)
            lineTo(w * 0.28f, h * 0.68f)
            quadraticBezierTo(w * 0.28f, h * 0.80f, w * 0.42f, h * 0.80f)
            lineTo(w * 0.86f, h * 0.80f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = (this.size.minDimension * 0.12f).coerceAtLeast(1.4f),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

/**
 * Crisp vector assistant/step status dot (replaces `⏺` which turns into a red emoji on Android!).
 */
@Composable
fun ClaudeStatusDot(
    isActive: Boolean,
    activeColor: Color = ClaudeTerracotta,
    idleColor: Color = Color(0xFFE4E4E7),
    size: Dp = 8.dp,
    modifier: Modifier = Modifier,
) {
    if (isActive) {
        val infinite = rememberInfiniteTransition(label = "claude_dot_pulse")
        val ringScale by infinite.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.65f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "dot_ring_scale",
        )
        val ringAlpha by infinite.animateFloat(
            initialValue = 0.55f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "dot_ring_alpha",
        )
        Canvas(modifier = modifier.size(size * 1.7f)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseR = (size.toPx() / 2f)
            drawCircle(
                color = activeColor.copy(alpha = ringAlpha),
                radius = baseR * ringScale,
                center = center,
            )
            drawCircle(
                color = activeColor,
                radius = baseR,
                center = center,
            )
        }
    } else {
        Canvas(modifier = modifier.size(size)) {
            drawCircle(
                color = idleColor,
                radius = this.size.minDimension / 2f,
                center = Offset(this.size.width / 2f, this.size.height / 2f),
            )
        }
    }
}

/**
 * Shimmering Claude Code Thinking / Action Status Label with cycling realistic verbs when thinking.
 */
@Composable
fun ClaudeShimmerStatusText(
    baseLabel: String,
    isRunning: Boolean,
    thinkingActive: Boolean,
    streamedTokensEstimate: Int = 0,
    modifier: Modifier = Modifier,
) {
    val thinkingVerbs = remember {
        listOf("Thinking", "Analyzing codebase", "Synthesizing plan", "Reasoning")
    }
    var verbIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(isRunning, thinkingActive) {
        if (isRunning && thinkingActive) {
            while (true) {
                delay(2400)
                verbIndex = (verbIndex + 1) % thinkingVerbs.size
            }
        } else {
            verbIndex = 0
        }
    }

    val displayLabel = remember(baseLabel, isRunning, thinkingActive, verbIndex, streamedTokensEstimate) {
        val core = if (isRunning && thinkingActive && baseLabel.startsWith("Thinking")) {
            baseLabel.replaceFirst("Thinking", thinkingVerbs[verbIndex])
        } else {
            baseLabel
        }
        if (isRunning && streamedTokensEstimate > 0) {
            val tokStr = if (streamedTokensEstimate >= 1000) {
                "%.1fk tok".format(streamedTokensEstimate / 1000f)
            } else {
                "$streamedTokensEstimate tok"
            }
            "$core · ↓ $tokStr"
        } else {
            core
        }
    }

    if (!isRunning) {
        Text(
            text = displayLabel,
            modifier = modifier,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = ClaudeTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        return
    }

    val infinite = rememberInfiniteTransition(label = "claude_text_shimmer")
    val progress by infinite.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1650, easing = LinearEasing),
        ),
        label = "shimmer_x",
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            ClaudeTerracotta.copy(alpha = 0.78f),
            Color(0xFFFFD4BE),
            ClaudeTerracotta.copy(alpha = 0.78f),
        ),
        start = Offset(progress * 520f - 140f, 0f),
        end = Offset(progress * 520f + 140f, 0f),
    )

    Text(
        text = displayLabel,
        modifier = modifier,
        style = TextStyle(
            brush = shimmerBrush,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. CLAUDE CODE LIVE CONTEXT WINDOW `%` & SPEED BENCHMARK METER
// ═════════════════════════════════════════════════════════════════════════════

@Immutable
data class ClaudeSessionBenchmarkStats(
    val usedTokens: Int,
    val maxContextTokens: Int,
    val promptTokens: Int,
    val responseTokens: Int,
    val systemTokens: Int,
    val usedPercent: Float,
    val tokensPerSecond: Float,
    val lastTurnSeconds: Float,
)

fun computeClaudeSessionBenchmark(
    messages: List<ChatMessage>,
    streamingPreview: String?,
    pendingAttachments: List<ChatAttachment>,
    activeModelLabel: String,
    taskStartedAtMillis: Long?,
    taskFinishedAtMillis: Long?,
    isRunning: Boolean,
): ClaudeSessionBenchmarkStats {
    val lowerModel = activeModelLabel.lowercase()
    val maxWindow = when {
        "gemini" in lowerModel -> 1_000_000
        "claude" in lowerModel || "sonnet" in lowerModel || "opus" in lowerModel || "haiku" in lowerModel -> 200_000
        "gpt-4.1" in lowerModel -> 1_000_000
        "codex" in lowerModel || "o3" in lowerModel || "o4" in lowerModel || "gpt" in lowerModel -> 200_000
        else -> 128_000
    }

    var userChars = 0L
    var assistantChars = 0L
    var latestAssistantChars = 0
    var latestWorkedMs = 0L

    for (m in messages) {
        if (m.text.startsWith("Hi! Tell me")) continue
        if (m.fromUser) {
            userChars += m.text.length
        } else {
            assistantChars += m.text.length
            if (m.text.isNotBlank()) {
                latestAssistantChars = m.text.length
                if (m.workedMillis > 0L) latestWorkedMs = m.workedMillis
            }
        }
    }
    val streamLen = streamingPreview?.length ?: 0
    assistantChars += streamLen

    val promptTok = (userChars / 4L).toInt()
    val responseTok = (assistantChars / 4L).toInt()
    val systemTok = if (messages.size <= 1 && userChars == 0L && streamLen == 0) {
        0
    } else {
        1_600 + (pendingAttachments.size * 450)
    }

    val totalUsed = (promptTok + responseTok + systemTok).coerceAtLeast(0)
    val pct = ((totalUsed.toFloat() / maxWindow.toFloat()) * 100f).coerceIn(0f, 100f)

    val now = System.currentTimeMillis()
    val activeDurationMs = when {
        isRunning && taskStartedAtMillis != null -> (now - taskStartedAtMillis).coerceAtLeast(300L)
        taskStartedAtMillis != null && taskFinishedAtMillis != null && taskFinishedAtMillis > taskStartedAtMillis ->
            taskFinishedAtMillis - taskStartedAtMillis
        latestWorkedMs > 0L -> latestWorkedMs
        else -> 0L
    }
    val turnTokens = if (isRunning && streamLen > 0) (streamLen / 4).coerceAtLeast(1) else (latestAssistantChars / 4)
    val tokPerSec = if (activeDurationMs >= 400L && turnTokens > 0) {
        (turnTokens * 1000f) / activeDurationMs.toFloat()
    } else {
        0f
    }

    return ClaudeSessionBenchmarkStats(
        usedTokens = totalUsed,
        maxContextTokens = maxWindow,
        promptTokens = promptTok,
        responseTokens = responseTok,
        systemTokens = systemTok,
        usedPercent = pct,
        tokensPerSecond = tokPerSec,
        lastTurnSeconds = activeDurationMs / 1000f,
    )
}

/**
 * Compact animated Claude Code Context `%` Benchmark Pill (`[◐ 14%]`) for the Session Top Bar & Composer.
 */
@Composable
fun ClaudeContextBenchmarkPill(
    stats: ClaudeSessionBenchmarkStats,
    isRunning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedPct by animateFloatAsState(
        targetValue = stats.usedPercent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "ctx_pct_spring",
    )

    val gaugeColor = when {
        animatedPct >= 82f -> Color(0xFFF87171)
        animatedPct >= 58f -> Color(0xFFFBBF24)
        else -> ClaudeTerracotta
    }

    val displayPctText = when {
        stats.usedTokens == 0 -> "0%"
        animatedPct < 1f -> "<1%"
        else -> "${animatedPct.toInt()}%"
    }

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color(0xFF17171C),
        border = BorderStroke(
            0.8.dp,
            if (isRunning) gaugeColor.copy(alpha = 0.55f) else Color(0xFF26262E),
        ),
        modifier = modifier.height(28.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Canvas(modifier = Modifier.size(13.dp)) {
                val stroke = 2.1.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2f, stroke / 2f)
                drawArc(
                    color = Color(0xFF2B2B34),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                val sweep = (animatedPct / 100f).coerceIn(0.03f, 1f) * 360f
                if (stats.usedTokens > 0 || isRunning) {
                    drawArc(
                        color = gaugeColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Text(
                text = displayPctText,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (stats.usedTokens > 0) ClaudeTextPrimary else ClaudeTextSecondary,
            )
        }
    }
}

/**
 * Full Pure-Dark Claude Code Context & Engine Benchmark Bottom Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaudeBenchmarkSheet(
    stats: ClaudeSessionBenchmarkStats,
    activeEngineName: String,
    activeModelName: String,
    changes: List<ChangeItem>,
    canCompact: Boolean,
    onCompactContext: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val animatedPct by animateFloatAsState(
        targetValue = stats.usedPercent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "sheet_arc_pct",
    )

    val gaugeColor = when {
        animatedPct >= 82f -> Color(0xFFF87171)
        animatedPct >= 58f -> Color(0xFFFBBF24)
        else -> ClaudeTerracotta
    }

    val totalAdditions = remember(changes) { changes.sumOf { it.additions } }
    val totalDeletions = remember(changes) { changes.sumOf { it.deletions } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ClaudeDarkSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = ClaudeTerracotta, modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Session Context & Benchmark",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ClaudeTextPrimary,
                    )
                    Text(
                        text = "$activeEngineName · $activeModelName",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        color = ClaudeTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Large Animated Context Window Gauge Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ClaudeDarkBg,
                border = BorderStroke(0.9.dp, ClaudeDarkBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier.size(132.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.size(132.dp)) {
                            val strokeW = 10.dp.toPx()
                            val arcSize = Size(size.width - strokeW, size.height - strokeW)
                            val topLeft = Offset(strokeW / 2f, strokeW / 2f)
                            drawArc(
                                color = Color(0xFF22222A),
                                startAngle = 140f,
                                sweepAngle = 260f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round),
                            )
                            val activeSweep = (animatedPct / 100f).coerceIn(0f, 1f) * 260f
                            if (activeSweep > 0.5f) {
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        listOf(ClaudeTerracotta, gaugeColor, Color(0xFFFFB08E), ClaudeTerracotta),
                                    ),
                                    startAngle = 140f,
                                    sweepAngle = activeSweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Round),
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (stats.usedTokens > 0 && animatedPct < 1f) "<1%" else "${animatedPct.toInt()}%",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = ClaudeTextPrimary,
                            )
                            Text(
                                text = "context used",
                                fontSize = 11.sp,
                                color = ClaudeTextSecondary,
                            )
                        }
                    }

                    Text(
                        text = "${formatTokenCount(stats.usedTokens)} / ${formatTokenCount(stats.maxContextTokens)} tokens · ${(100f - animatedPct).coerceIn(0f, 100f).toInt()}% free",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = ClaudeTextSecondary,
                    )

                    // Segmented Breakdown Bar
                    val denom = stats.usedTokens.coerceAtLeast(1).toFloat()
                    val pFrac by animateFloatAsState(stats.promptTokens / denom, label = "p_frac")
                    val rFrac by animateFloatAsState(stats.responseTokens / denom, label = "r_frac")
                    val sFrac by animateFloatAsState(stats.systemTokens / denom, label = "s_frac")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1F1F26)),
                    ) {
                        if (stats.usedTokens > 0) {
                            if (pFrac > 0.01f) {
                                Box(Modifier.weight(pFrac.coerceAtLeast(0.02f)).fillMaxHeight().background(ClaudeTerracotta))
                            }
                            if (rFrac > 0.01f) {
                                Box(Modifier.weight(rFrac.coerceAtLeast(0.02f)).fillMaxHeight().background(Color(0xFF4ADE80)))
                            }
                            if (sFrac > 0.01f) {
                                Box(Modifier.weight(sFrac.coerceAtLeast(0.02f)).fillMaxHeight().background(Color(0xFF60A5FA)))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        BenchmarkLegendDot("Prompts ${formatTokenCount(stats.promptTokens)}", ClaudeTerracotta)
                        BenchmarkLegendDot("Output ${formatTokenCount(stats.responseTokens)}", Color(0xFF4ADE80))
                        BenchmarkLegendDot("Workspace ${formatTokenCount(stats.systemTokens)}", Color(0xFF60A5FA))
                    }
                }
            }

            // Speed & Diff Benchmark Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ClaudeDarkCard,
                    border = BorderStroke(0.8.dp, ClaudeDarkBorder),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.Default.AutoAwesome, null, tint = ClaudeTerracotta, modifier = Modifier.size(14.dp))
                            Text("Throughput", fontSize = 11.sp, color = ClaudeTextSecondary)
                        }
                        Text(
                            text = if (stats.tokensPerSecond > 0.1f) "%.1f tok/s".format(stats.tokensPerSecond) else "Ready",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ClaudeTextPrimary,
                        )
                        Text(
                            text = if (stats.lastTurnSeconds > 0f) "Last turn %.1fs".format(stats.lastTurnSeconds) else "Zero prompt bloat",
                            fontSize = 10.5.sp,
                            color = ClaudeTextSecondary,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ClaudeDarkCard,
                    border = BorderStroke(0.8.dp, ClaudeDarkBorder),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.Default.Code, null, tint = Color(0xFF4ADE80), modifier = Modifier.size(14.dp))
                            Text("Session Diffs", fontSize = 11.sp, color = ClaudeTextSecondary)
                        }
                        Text(
                            text = "+$totalAdditions / -$totalDeletions",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ClaudeTextPrimary,
                        )
                        Text(
                            text = "${changes.size} ${if (changes.size == 1) "file" else "files"} tracked",
                            fontSize = 10.5.sp,
                            color = ClaudeTextSecondary,
                        )
                    }
                }
            }

            if (canCompact) {
                Button(
                    onClick = {
                        onCompactContext()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClaudeTerracotta,
                        contentColor = Color(0xFF0C0C0E),
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Compact Session Context (/compact)", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                }
            }

            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun BenchmarkLegendDot(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = ClaudeTextSecondary,
        )
    }
}

private fun formatTokenCount(tokens: Int): String = when {
    tokens >= 1_000_000 -> "%.1fM".format(tokens / 1_000_000f)
    tokens >= 1_000 -> "%.1fk".format(tokens / 1_000f)
    else -> tokens.toString()
}

// ═════════════════════════════════════════════════════════════════════════════
// 3. LIVE ANIMATED ARCHIVE IMPORT / GIT CLONE PROGRESS BANNER
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun ClaudeLiveImportProgressCard(
    visible: Boolean,
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + expandVertically(tween(240)),
        exit = fadeOut(tween(180)) + shrinkVertically(tween(200)),
        modifier = modifier,
    ) {
        val infinite = rememberInfiniteTransition(label = "import_bar_sweep")
        val sweep by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1350, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "import_sweep",
        )

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = ClaudeDarkCard,
            border = BorderStroke(0.9.dp, ClaudeTerracotta.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ClaudeStarburstSpinner(size = 16.dp, color = ClaudeTerracotta)
                    Icon(
                        imageVector = Icons.Default.FolderZip,
                        contentDescription = null,
                        tint = ClaudeTerracotta,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ClaudeTextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = detail,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    color = ClaudeTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF25252D)),
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(3.dp)) {
                        val segW = size.width * 0.36f
                        val startX = (size.width + segW) * sweep - segW
                        drawRect(
                            color = ClaudeTerracotta,
                            topLeft = Offset(startX.coerceAtLeast(0f), 0f),
                            size = Size((startX + segW).coerceAtMost(size.width) - startX.coerceAtLeast(0f), size.height),
                        )
                    }
                }
            }
        }
    }
}
