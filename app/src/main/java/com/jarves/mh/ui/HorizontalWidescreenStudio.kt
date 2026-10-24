package com.jarves.mh.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.model.AgentKind
import com.jarves.mh.model.Project
import com.jarves.mh.ui.theme.ClaudeDarkBg
import com.jarves.mh.ui.theme.ClaudeDarkBorder
import com.jarves.mh.ui.theme.ClaudeDarkCard
import com.jarves.mh.ui.theme.ClaudeDarkSurface
import com.jarves.mh.ui.theme.ClaudeTerracotta
import com.jarves.mh.ui.theme.ClaudeTextPrimary
import com.jarves.mh.ui.theme.ClaudeTextSecondary

/**
 * Iconic 8-bit Pixel "Claw'd" Mascot from Claude Code (Mobile & PC CLI).
 * Drawn with crisp mathematical pixel blocks in warm terracotta `#D97757` on dark `#0C0C0E`.
 */
@Composable
fun ClawdPixelMascot(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    isRunning: Boolean = false,
    showSparkles: Boolean = false,
) {
    val infinite = rememberInfiniteTransition(label = "clawd_anim")
    val bobOffset by infinite.animateFloat(
        initialValue = 0f,
        targetValue = if (isRunning) -4f else -1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRunning) 420 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "clawd_bob",
    )
    val starAlpha by infinite.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.68f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "clawd_star",
    )

    val boxDp = if (showSparkles) size * 2.2f else size
    Canvas(modifier = modifier.size(boxDp)) {
        val totalW = this.size.width
        val totalH = this.size.height

        if (showSparkles) {
            // Subtle pixel stars around Claw'd just like the official Claude Code mobile empty screen
            val starColor = ClaudeTerracotta.copy(alpha = starAlpha)
            val s = (totalW * 0.018f).coerceAtLeast(2f)
            fun drawPixelStar(cx: Float, cy: Float) {
                drawRect(starColor, Offset(cx - s * 1.5f, cy - s * 0.5f), Size(s * 3f, s))
                drawRect(starColor, Offset(cx - s * 0.5f, cy - s * 1.5f), Size(s, s * 3f))
            }
            drawPixelStar(totalW * 0.72f, totalH * 0.14f)
            drawPixelStar(totalW * 0.14f, totalH * 0.42f)
            drawPixelStar(totalW * 0.78f, totalH * 0.84f)
        }

        val spriteW = if (showSparkles) totalW * 0.44f else totalW * 0.88f
        // 13x10 pixel grid for exact Claw'd proportions
        val px = spriteW / 13f
        val spriteH = px * 10f
        val ox = (totalW - spriteW) * 0.5f
        val oy = (totalH - spriteH) * 0.5f + bobOffset

        val bodyColor = ClaudeTerracotta
        val eyeColor = Color(0xFF0C0C0E)

        fun block(gx: Int, gy: Int, gw: Int, gh: Int, color: Color) {
            drawRect(
                color = color,
                topLeft = Offset(ox + gx * px, oy + gy * px),
                size = Size(gw * px + 0.4f, gh * px + 0.4f),
            )
        }

        // Main rectangular head/body: cols 2..10 (9 wide), rows 1..6 (6 tall)
        block(2, 1, 9, 6, bodyColor)
        // Left & right side pixel arms: col 1 & col 11, rows 3..4 (2 tall)
        block(1, 3, 1, 2, bodyColor)
        block(11, 3, 1, 2, bodyColor)
        // 4 pixel legs: cols 3, 5, 7, 9, rows 7..8 (2 tall)
        block(3, 7, 1, 2, bodyColor)
        block(5, 7, 1, 2, bodyColor)
        block(7, 7, 1, 2, bodyColor)
        block(9, 7, 1, 2, bodyColor)
        // Two dark square pixel eyes: col 3 & col 9, row 3
        block(3, 3, 1, 2, eyeColor)
        block(9, 3, 1, 2, eyeColor)
    }
}

/** Compatibility alias used by BrandMark & onboarding screens. */
@Composable
fun VoxelPrismBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    isRunning: Boolean = false,
) {
    ClawdPixelMascot(modifier = modifier, size = size, isRunning = isRunning, showSparkles = false)
}

/**
 * 1:1 Pure-Dark Claude Code Mobile Home Screen ("Code" — matches the RIGHT screen in the user's screenshot).
 * Ultra-simple, mobile-friendly, zero clutter!
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ClaudeCodeMobileHomeScreen(
    state: AppUiState,
    listState: LazyListState,
    activeModelDisplay: String,
    onOpenProject: (Project) -> Unit,
    onQuickNewSession: () -> Unit,
    onCreateNamedProject: (String) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onOpenFullPageCli: () -> Unit,
    onOpenEngines: () -> Unit,
    onOpenCustomProvider: () -> Unit,
    onOpenCliHub: () -> Unit,
    onOpenSettings: () -> Unit,
    onImportZipClick: () -> Unit,
    onCloneGitUrl: (String) -> Unit,
    onToggleMcSpecialist: (Boolean) -> Unit,
) {
    var showMenuSheet by rememberSaveable { mutableStateOf(false) }
    var showNewDialog by rememberSaveable { mutableStateOf(false) }
    var showCloneDialog by rememberSaveable { mutableStateOf(false) }
    var newProjectName by rememberSaveable { mutableStateOf("") }
    var cloneUrl by rememberSaveable { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<Project?>(null) }
    var renameDraft by rememberSaveable { mutableStateOf("") }

    val projects = state.projects
    val readyProjects = remember(projects, state.activeProject?.id) {
        if (projects.isEmpty()) emptyList() else listOf(projects.first())
    }
    val idleProjects = remember(projects) {
        if (projects.size <= 1) emptyList() else projects.drop(1)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ClaudeDarkBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
        ) {
            // Minimal Top Bar: `≡` on left, `>_` & `+` on right
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(
                        onClick = { showMenuSheet = true },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = ClaudeTextPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        IconButton(
                            onClick = onOpenFullPageCli,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Full-Page CLI",
                                tint = ClaudeTextSecondary,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        IconButton(
                            onClick = { showNewDialog = true },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New folder project",
                                tint = ClaudeTextPrimary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Editorial Serif "Code" Heading — 1:1 with user's screenshot!
                Text(
                    text = "Code",
                    fontFamily = FontFamily.Serif,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Normal,
                    color = ClaudeTextPrimary,
                )

                Spacer(Modifier.height(22.dp))
            }

            if (projects.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 56.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        ClawdPixelMascot(size = 64.dp, showSparkles = true)
                        Text(
                            text = "No sessions yet",
                            fontSize = 15.sp,
                            color = ClaudeTextSecondary,
                        )
                        Surface(
                            onClick = onQuickNewSession,
                            shape = CircleShape,
                            color = ClaudeDarkCard,
                            border = BorderStroke(1.dp, ClaudeDarkBorder),
                        ) {
                            Text(
                                text = "New session",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = ClaudeTextPrimary,
                            )
                        }
                    }
                }
            } else {
                if (readyProjects.isNotEmpty()) {
                    item {
                        Text(
                            text = "Ready",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = ClaudeTextSecondary,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                    items(readyProjects, key = { it.id }) { project ->
                        ClaudeSessionListRow(
                            project = project,
                            showOpenBadge = true,
                            onOpen = { onOpenProject(project) },
                            onRename = {
                                renameTarget = project
                                renameDraft = project.name
                            },
                            onDelete = { onDeleteProject(project.id) },
                        )
                    }
                }

                if (idleProjects.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(22.dp))
                        Text(
                            text = "Idle",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = ClaudeTextSecondary,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                    items(idleProjects, key = { it.id }) { project ->
                        ClaudeSessionListRow(
                            project = project,
                            showOpenBadge = false,
                            onOpen = { onOpenProject(project) },
                            onRename = {
                                renameTarget = project
                                renameDraft = project.name
                            },
                            onDelete = { onDeleteProject(project.id) },
                        )
                    }
                }
            }
        }

        // Clean Floating Bottom "New session" Pill
        Surface(
            onClick = onQuickNewSession,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
            shape = CircleShape,
            color = ClaudeTerracotta,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text(
                    text = "New session",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }

    if (showMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMenuSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ClaudeDarkSurface,
            contentColor = ClaudeTextPrimary,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 10.dp),
                ) {
                    ClawdPixelMascot(size = 28.dp)
                    Column {
                        Text("CS Personal Engine", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ClaudeTextPrimary)
                        Text(activeModelDisplay, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = ClaudeTextSecondary)
                    }
                }
                HorizontalDivider(color = ClaudeDarkBorder)

                MinimalDrawerActionRow(Icons.Default.Terminal, "Open Full-Page CLI", "claude · opencode · codex · bash") {
                    showMenuSheet = false
                    onOpenFullPageCli()
                }
                MinimalDrawerActionRow(Icons.Default.AutoAwesome, "Model & Engine", "Antigravity · Claude · DeepSeek") {
                    showMenuSheet = false
                    onOpenEngines()
                }
                MinimalDrawerActionRow(Icons.Default.Add, "Custom Provider", "OpenAI / Anthropic / Groq / OpenRouter API") {
                    showMenuSheet = false
                    onOpenCustomProvider()
                }
                MinimalDrawerActionRow(Icons.Default.Code, "Install CLI Tools", "1-tap claude, opencode, codex, gemini, gh") {
                    showMenuSheet = false
                    onOpenCliHub()
                }
                MinimalDrawerActionRow(Icons.Default.FolderOpen, "Import ZIP or Clone Git", "Open existing Android / Java / Kotlin repo") {
                    showMenuSheet = false
                    showCloneDialog = true
                }

                // Quiet toggle for Minecraft Launcher Java/XML/Kotlin brain
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Java · XML · Kotlin + MC Brain", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ClaudeTextPrimary)
                        Text("Auto-send Minecraft Launcher & Android XML context", fontSize = 11.5.sp, color = ClaudeTextSecondary)
                    }
                    Switch(
                        checked = state.mcJavaXmlSpecialistEnabled,
                        onCheckedChange = onToggleMcSpecialist,
                    )
                }

                HorizontalDivider(color = ClaudeDarkBorder)
                MinimalDrawerActionRow(Icons.Default.Settings, "Settings", "Keys, Linux toolchains & diagnostics") {
                    showMenuSheet = false
                    onOpenSettings()
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    if (showNewDialog) {
        AlertDialog(
            onDismissRequest = { showNewDialog = false },
            containerColor = ClaudeDarkSurface,
            title = { Text("New project", color = ClaudeTextPrimary) },
            text = {
                OutlinedTextField(
                    value = newProjectName,
                    onValueChange = { newProjectName = it },
                    singleLine = true,
                    placeholder = { Text("mc-launcher") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newProjectName.trim().ifBlank { "mc-launcher" }
                        newProjectName = ""
                        showNewDialog = false
                        onCreateNamedProject(trimmed)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClaudeTerracotta),
                ) {
                    Text("Create", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showCloneDialog) {
        AlertDialog(
            onDismissRequest = { showCloneDialog = false },
            containerColor = ClaudeDarkSurface,
            title = { Text("Import or Clone", color = ClaudeTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = cloneUrl,
                        onValueChange = { cloneUrl = it },
                        singleLine = true,
                        placeholder = { Text("https://github.com/user/repo.git") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        onClick = {
                            showCloneDialog = false
                            onImportZipClick()
                        },
                    ) {
                        Text("Or import .zip archive from phone")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val url = cloneUrl.trim()
                        if (url.isNotEmpty()) {
                            cloneUrl = ""
                            showCloneDialog = false
                            onCloneGitUrl(url)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClaudeTerracotta),
                ) {
                    Text("Clone", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloneDialog = false }) { Text("Cancel") }
            },
        )
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = ClaudeDarkSurface,
            title = { Text("Rename session", color = ClaudeTextPrimary) },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val next = renameDraft.trim()
                        if (next.isNotEmpty()) onRenameProject(target.id, next)
                        renameTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClaudeTerracotta),
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClaudeSessionListRow(
    project: Project,
    showOpenBadge: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpen,
                onLongClick = { menuOpen = true },
            )
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = project.name,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Medium,
                color = ClaudeTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "workspace/${project.slug}",
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = ClaudeTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (showOpenBadge) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF13261B),
                    border = BorderStroke(0.7.dp, Color(0xFF224630)),
                ) {
                    Text(
                        text = "Open",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4ADE80),
                    )
                }
            }
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Options",
                        tint = ClaudeTextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null, modifier = Modifier.size(17.dp)) },
                        onClick = { menuOpen = false; onRename() },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(17.dp)) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun MinimalDrawerActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = ClaudeTextSecondary, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = ClaudeTextPrimary)
            Text(subtitle, fontSize = 11.5.sp, color = ClaudeTextSecondary)
        }
    }
}

/**
 * Mobile-Friendly Pure-Dark Minecraft Launcher (Java + XML + Kotlin) Specialist Blueprint Deck.
 */
@Composable
fun MinecraftLauncherStudioDeck(
    specialistBrainEnabled: Boolean,
    onToggleSpecialistBrain: (Boolean) -> Unit,
    onRunBlueprintPrompt: (String) -> Unit,
    onOpenTerminalCommand: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ClaudeDarkBg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ClaudeDarkSurface,
                border = BorderStroke(0.9.dp, ClaudeDarkBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Java · Android XML · Kotlin Brain",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ClaudeTextPrimary,
                        )
                        Text(
                            text = "Mojang manifests, Fabric/Modrinth APIs, ARM64 JRE 8/17/21 JNI & XML UI blueprints",
                            fontSize = 12.sp,
                            color = ClaudeTextSecondary,
                        )
                    }
                    Switch(
                        checked = specialistBrainEnabled,
                        onCheckedChange = onToggleSpecialistBrain,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onOpenTerminalCommand("java -version && javac -version") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Check JDK", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { onOpenTerminalCommand("./gradlew assembleDebug") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Gradle Build", fontSize = 12.sp)
                }
            }
        }

        items(MinecraftLauncherKnowledge.blueprints, key = { it.id }) { bp ->
            Surface(
                onClick = { onRunBlueprintPrompt(bp.prompt) },
                shape = RoundedCornerShape(14.dp),
                color = ClaudeDarkSurface,
                border = BorderStroke(0.8.dp, ClaudeDarkBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ClaudeTerracotta.copy(alpha = 0.14f),
                        ) {
                            Text(
                                text = bp.badge,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = ClaudeTerracotta,
                            )
                        }
                        Text(
                            text = bp.stackLabel,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ClaudeTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = bp.title,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ClaudeTextPrimary,
                    )
                    Text(
                        text = bp.summary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = ClaudeTextSecondary,
                    )
                }
            }
        }
    }
}

