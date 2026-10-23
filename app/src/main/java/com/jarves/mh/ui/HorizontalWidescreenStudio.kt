package com.jarves.mh.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.ui.theme.EngineCyan
import com.jarves.mh.ui.theme.EngineEmerald

data class WidescreenRailItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val badge: String? = null,
    val accent: Boolean = false,
)

/**
 * Brand-new v5.0 Animated 3D Isometric Voxel-Prism Logo (`CS Personal Engine`).
 * Replaces the old rounded icon everywhere with a breathing emerald/cyan isometric prism cube + orbital telemetry arc.
 */
@Composable
fun VoxelPrismBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    isRunning: Boolean = false,
) {
    val infinite = rememberInfiniteTransition(label = "voxel_prism")
    val orbitAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRunning) 2600 else 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbit_angle",
    )
    val coreGlow by infinite.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRunning) 650 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "core_glow",
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val r = (this.size.minDimension * 0.36f) * coreGlow

        // Subtle emerald radial aura
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(EngineEmerald.copy(alpha = 0.24f), Color.Transparent),
                center = Offset(cx, cy),
                radius = this.size.minDimension * 0.54f,
            ),
            radius = this.size.minDimension * 0.52f,
            center = Offset(cx, cy),
        )

        // Rotating orbital telemetry arc
        rotate(degrees = orbitAngle, pivot = Offset(cx, cy)) {
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(Color.Transparent, EngineEmerald.copy(alpha = 0.75f), EngineCyan, Color.Transparent),
                ),
                startAngle = 0f,
                sweepAngle = 210f,
                useCenter = false,
                style = Stroke(width = (w * 0.055f).coerceAtLeast(1.4f), cap = StrokeCap.Round),
            )
        }

        // Isometric 3D Voxel-Prism Cube
        val dx = r * 0.86f
        val dy = r * 0.50f

        // Top Diamond Facet (Emerald Mint)
        val topFace = Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + dx, cy - dy)
            lineTo(cx, cy)
            lineTo(cx - dx, cy - dy)
            close()
        }
        drawPath(
            path = topFace,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF6EE7B7), EngineEmerald),
                start = Offset(cx, cy - r),
                end = Offset(cx, cy),
            ),
        )

        // Left Facet (Deep Obsidian Slate)
        val leftFace = Path().apply {
            moveTo(cx - dx, cy - dy)
            lineTo(cx, cy)
            lineTo(cx, cy + r)
            lineTo(cx - dx, cy + dy)
            close()
        }
        drawPath(
            path = leftFace,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
                start = Offset(cx - dx, cy - dy),
                end = Offset(cx, cy + r),
            ),
        )

        // Right Facet (Electric Cyan-Emerald)
        val rightFace = Path().apply {
            moveTo(cx, cy)
            lineTo(cx + dx, cy - dy)
            lineTo(cx + dx, cy + dy)
            lineTo(cx, cy + r)
            close()
        }
        drawPath(
            path = rightFace,
            brush = Brush.linearGradient(
                colors = listOf(EngineCyan, Color(0xFF0284C7)),
                start = Offset(cx, cy),
                end = Offset(cx + dx, cy + r),
            ),
        )

        // Crisp center vertex highlight
        drawLine(
            color = Color(0xFFF8FAFC).copy(alpha = 0.85f),
            start = Offset(cx - dx * 0.5f, cy - dy * 0.5f),
            end = Offset(cx, cy),
            strokeWidth = (w * 0.045f).coerceAtLeast(1.1f),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color(0xFFF8FAFC).copy(alpha = 0.85f),
            start = Offset(cx + dx * 0.5f, cy - dy * 0.5f),
            end = Offset(cx, cy),
            strokeWidth = (w * 0.045f).coerceAtLeast(1.1f),
            cap = StrokeCap.Round,
        )
    }
}

/**
 * 1-tap Horizontal <-> Portrait orientation switcher so the user gets full Horizontal Widescreen
 * coding by default and can flip anytime with zero friction.
 */
@Composable
fun StudioOrientationButton(
    compact: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val isLandscape = config.screenWidthDp >= config.screenHeightDp

    Surface(
        onClick = {
            val activity = context as? Activity ?: return@Surface
            activity.requestedOrientation = if (isLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        },
        modifier = modifier.size(if (compact) 30.dp else 36.dp),
        shape = RoundedCornerShape(9.dp),
        color = Color(0xFF121722),
        border = BorderStroke(0.9.dp, Color(0xFF232C3D)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.ScreenRotation,
                contentDescription = "Switch Horizontal / Vertical Orientation",
                tint = if (isLandscape) EngineEmerald else Color(0xFF94A3B8),
                modifier = Modifier.size(if (compact) 15.dp else 17.dp),
            )
        }
    }
}

/**
 * Sleek Desktop-IDE Left Vertical Navigation Rail (`58dp` wide) built specifically for
 * Horizontal Widescreen coding — saves 100% of vertical screen height!
 */
@Composable
fun StudioWidescreenRail(
    items: List<WidescreenRailItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    isRunning: Boolean = false,
    topSlot: (@Composable () -> Unit)? = null,
    bottomSlot: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color(0xFF07090E),
        border = BorderStroke(0.8.dp, Color(0xFF171E2C)),
        modifier = modifier
            .fillMaxHeight()
            .width(60.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (topSlot != null) {
                    topSlot()
                    Spacer(Modifier.height(2.dp))
                } else {
                    VoxelPrismBrandMark(size = 30.dp, isRunning = isRunning)
                    Spacer(Modifier.height(4.dp))
                }

                items.forEach { item ->
                    val selected = item.id == selectedId
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.0f else 0.94f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        label = "rail_scale_${item.id}",
                    )
                    val indicatorHeight by animateDpAsState(
                        targetValue = if (selected) 20.dp else 0.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "rail_ind_${item.id}",
                    )
                    val activeTint = if (item.accent) EngineCyan else EngineEmerald

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .clickable { onSelect(item.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                selected -> activeTint.copy(alpha = 0.16f)
                                item.badge != null -> Color(0xFF101916)
                                else -> Color.Transparent
                            },
                            border = if (selected) {
                                BorderStroke(1.dp, activeTint.copy(alpha = 0.55f))
                            } else if (item.badge != null) {
                                BorderStroke(0.8.dp, EngineEmerald.copy(alpha = 0.35f))
                            } else {
                                null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = when {
                                        selected -> activeTint
                                        item.accent -> EngineCyan.copy(alpha = 0.85f)
                                        else -> Color(0xFF94A3B8)
                                    },
                                    modifier = Modifier.size(17.dp),
                                )
                                Text(
                                    text = item.badge ?: item.label,
                                    fontSize = 8.5.sp,
                                    fontFamily = if (item.badge != null) FontFamily.Monospace else FontFamily.Default,
                                    fontWeight = if (selected || item.badge != null) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = when {
                                        selected -> Color(0xFFF8FAFC)
                                        item.badge != null -> EngineEmerald
                                        else -> Color(0xFF8B98AD)
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        if (indicatorHeight > 0.dp) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .width(3.dp)
                                    .height(indicatorHeight)
                                    .background(activeTint, CircleShape),
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                bottomSlot?.invoke()
                StudioOrientationButton(compact = true)
            }
        }
    }
}

/**
 * Smooth Spring-Physics Horizontal Widescreen Pane Transition Host.
 */
@Composable
fun <T> StudioAnimatedPaneHost(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(animationSpec = tween(190, easing = FastOutSlowInEasing)) +
                slideInHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    initialOffsetX = { fullWidth -> fullWidth / 18 },
                ) +
                scaleIn(
                    initialScale = 0.985f,
                    animationSpec = tween(190, easing = FastOutSlowInEasing),
                )) togetherWith
                (fadeOut(animationSpec = tween(130)) +
                    slideOutHorizontally(
                        animationSpec = tween(140),
                        targetOffsetX = { fullWidth -> -fullWidth / 24 },
                    )) using SizeTransform(clip = true)
        },
        label = "studio_widescreen_pane",
    ) { state ->
        content(state)
    }
}

/**
 * Dedicated Horizontal Widescreen **Minecraft Launcher + Java / Android XML / Kotlin Studio** (`MC Kit` tab).
 * Gives 1-tap engineering blueprints + live toggle for the specialist AI system brain!
 */
@Composable
fun MinecraftLauncherStudioDeck(
    specialistBrainEnabled: Boolean,
    onToggleSpecialistBrain: (Boolean) -> Unit,
    onRunBlueprintPrompt: (String) -> Unit,
    onOpenTerminalCommand: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stackChips = listOf(
        "Java 8 / 17 / 21",
        "Android XML Layouts",
        "Kotlin + ViewBinding",
        "Mojang Manifest v2",
        "ARM64 libjli.so + LWJGL3",
        "GL4ES / Zink Vulkan",
        "Fabric + Modrinth API",
        "Xbox Live / XSTS / Offline Auth",
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07090E)),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0E131D),
                border = BorderStroke(1.dp, EngineEmerald.copy(alpha = 0.45f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        VoxelPrismBrandMark(size = 38.dp, isRunning = specialistBrainEnabled)
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "MINECRAFT LAUNCHER · JAVA + XML + KOTLIN BRAIN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.6.sp,
                                    color = EngineEmerald,
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = EngineEmerald.copy(alpha = 0.16f),
                                ) {
                                    Text(
                                        text = if (specialistBrainEnabled) "ACTIVE ON PROMPTS" else "PAUSED",
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EngineEmerald,
                                    )
                                }
                            }
                            Text(
                                text = "Auto-injects Mojang Manifest v2, SHA-1 Asset Downloader, Xbox/XSTS Auth, ARM64 OpenJDK libjli.so + LWJGL3/GL4ES/Zink specs, and strict Java + Android XML + Kotlin architecture into every AI prompt.",
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                color = Color(0xFFCBD5E1),
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(
                        checked = specialistBrainEnabled,
                        onCheckedChange = onToggleSpecialistBrain,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                stackChips.forEach { tag ->
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF111723),
                        border = BorderStroke(0.8.dp, Color(0xFF222D40)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = EngineCyan, modifier = Modifier.size(12.dp))
                            Text(tag, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE2E8F0))
                        }
                    }
                }
                Surface(
                    onClick = {
                        onOpenTerminalCommand("apt-get update && apt-get install -y openjdk-17-jdk gradle maven kotlin")
                    },
                    shape = CircleShape,
                    color = EngineCyan.copy(alpha = 0.16f),
                    border = BorderStroke(0.9.dp, EngineCyan.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = EngineCyan, modifier = Modifier.size(13.dp))
                        Text("Install JDK 17 + Gradle + Kotlin in Console", fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = EngineCyan)
                    }
                }
            }
        }

        // 2-Column Horizontal Widescreen Blueprint Cards
        items(MinecraftLauncherKnowledge.blueprints.chunked(2)) { rowPair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowPair.forEach { bp ->
                    Surface(
                        onClick = { onRunBlueprintPrompt(bp.prompt) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0E131D),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    ) {
                        Column(
                            modifier = Modifier.padding(13.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EngineEmerald.copy(alpha = 0.15f),
                                    border = BorderStroke(0.8.dp, EngineEmerald.copy(alpha = 0.45f)),
                                ) {
                                    Text(
                                        text = bp.badge,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EngineEmerald,
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EngineCyan, modifier = Modifier.size(13.dp))
                                    Text("Generate", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = EngineCyan)
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = EngineCyan, modifier = Modifier.size(13.dp))
                                }
                            }
                            Text(
                                text = bp.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFF8FAFC),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = bp.stackLabel,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EngineCyan,
                                maxLines = 1,
                            )
                            Text(
                                text = bp.summary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (rowPair.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
