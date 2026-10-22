package com.jarves.mh.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.R
import com.jarves.mh.ui.theme.AppThemeMode
import com.jarves.mh.ui.theme.EngineCyan
import com.jarves.mh.ui.theme.EngineEmerald
import com.jarves.mh.ui.theme.PocketGreen
import com.jarves.mh.ui.theme.PocketOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    lines: List<TerminalOutputLine>,
    isRunning: Boolean,
    onRun: (String) -> Unit,
    onInput: (String) -> Unit = {},
    onInterrupt: (() -> Unit)? = null,
    onClear: () -> Unit,
    onToggleTheme: () -> Unit,
    themeMode: AppThemeMode,
    title: String = stringResource(R.string.ws_terminal_linux_title),
    subtitle: String = stringResource(R.string.ws_terminal_linux_subtitle),
    liveOutput: String = "",
    currentCommand: String? = null,
    commandDraft: String? = null,
    onCommandDraftConsumed: () -> Unit = {},
    promptPath: String = "/workspace",
    onStop: (() -> Unit)? = null,
    onFixWithAi: ((command: String, output: String, exitCode: Int) -> Unit)? = null,
    showThemeAction: Boolean = false,
    showQuickCommands: Boolean = true,
    compactHeader: Boolean = false,
    interactivePtyMode: Boolean = false,
    onTogglePtyMode: ((Boolean) -> Unit)? = null,
    ptyBuffer: String = "",
    ptyAlive: Boolean = false,
    onEnsurePtyStarted: (() -> Unit)? = null,
    onSendPtyCommand: ((String) -> Unit)? = null,
    onSendPtyRaw: ((String) -> Unit)? = null,
    onInterruptPty: (() -> Unit)? = null,
    onClearPty: (() -> Unit)? = null,
    onRestartPty: (() -> Unit)? = null,
    onOpenCliHub: (() -> Unit)? = null,
) {
    var commandInput by remember { mutableStateOf(TextFieldValue()) }
    var commandHistory by remember { mutableStateOf(emptyList<String>()) }
    var historyIndex by remember { mutableStateOf(-1) }
    var ctrlActive by rememberSaveable { mutableStateOf(false) }
    val terminalScrollState = rememberScrollState()
    val inputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val scope = rememberCoroutineScope()
    val terminalPromptPath = if (promptPath == "/workspace") "~" else "~${promptPath.removePrefix("/workspace")}"

    val usePty = interactivePtyMode && onSendPtyCommand != null

    LaunchedEffect(usePty) {
        if (usePty) onEnsurePtyStarted?.invoke()
    }

    val openTerminalKeyboard = {
        inputFocusRequester.requestFocus()
        scope.launch {
            delay(60)
            keyboardController?.show()
        }
    }

    val executeOrSend = { rawCmd: String ->
        if (usePty) {
            onSendPtyCommand?.invoke(rawCmd)
        } else if (isRunning) {
            onInput(rawCmd)
        } else {
            onRun(rawCmd)
        }
    }

    val submitCommand = {
        val submitted = commandInput.text
        if (submitted.isNotEmpty() || usePty) {
            executeOrSend(submitted)
            if (submitted.isNotBlank()) {
                commandHistory = (commandHistory + submitted).takeLast(80)
            }
            historyIndex = -1
            commandInput = TextFieldValue()
        }
    }

    LaunchedEffect(commandDraft) {
        if (!commandDraft.isNullOrBlank()) {
            if (usePty) {
                onSendPtyCommand?.invoke(commandDraft)
            } else {
                commandInput = TextFieldValue(commandDraft, TextRange(commandDraft.length))
            }
            historyIndex = -1
            onCommandDraftConsumed()
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { terminalScrollState.maxValue }
            .collect { maxValue -> terminalScrollState.scrollTo(maxValue) }
    }
    LaunchedEffect(lines.size, isRunning, ptyBuffer.length, commandInput.text.length) {
        delay(50)
        terminalScrollState.scrollTo(terminalScrollState.maxValue)
    }

    val quickCommands = listOf(
        "ls -la",
        "git status",
        "apt-get update",
        "node -v && npm -v",
        "python3 --version",
        "claude --version",
        "agy --version",
        "gh --version",
        "df -h",
    )

    Scaffold(
        modifier = if (compactHeader) Modifier else Modifier.statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(EngineEmerald.copy(alpha = 0.14f), RoundedCornerShape(9.dp))
                            .border(1.dp, EngineEmerald.copy(alpha = 0.38f), RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = EngineEmerald, modifier = Modifier.size(17.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp)
                            Surface(
                                shape = CircleShape,
                                color = if (usePty && ptyAlive) EngineEmerald.copy(alpha = 0.16f) else EngineCyan.copy(alpha = 0.14f),
                            ) {
                                Text(
                                    text = if (usePty) (if (ptyAlive) "PTY LIVE" else "PTY IDLE") else "ROOT BASH",
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (usePty && ptyAlive) EngineEmerald else EngineCyan,
                                )
                            }
                        }
                        Text(
                            if (usePty) "Full Interactive Ubuntu Root PTY · Any CLI / REPL / Package" else subtitle,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }

                    if (onTogglePtyMode != null) {
                        Surface(
                            modifier = Modifier.clickable { onTogglePtyMode(!interactivePtyMode) },
                            shape = RoundedCornerShape(9.dp),
                            color = if (interactivePtyMode) EngineEmerald.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (interactivePtyMode) EngineEmerald else MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Text(
                                text = if (interactivePtyMode) "PTY ON" else "Blocks",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (interactivePtyMode) EngineEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (onOpenCliHub != null) {
                        Surface(
                            modifier = Modifier.clickable(onClick = onOpenCliHub),
                            shape = RoundedCornerShape(9.dp),
                            color = EngineCyan.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, EngineCyan.copy(alpha = 0.45f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(Icons.Default.Add, null, tint = EngineCyan, modifier = Modifier.size(13.dp))
                                Text("CLI Hub", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = EngineCyan)
                            }
                        }
                    }

                    if (usePty && onRestartPty != null) {
                        IconButton(onClick = onRestartPty, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Restart PTY shell", modifier = Modifier.size(17.dp))
                        }
                    }

                    IconButton(
                        onClick = { if (usePty) onClearPty?.invoke() else onClear() },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.ws_terminal_clear_output), modifier = Modifier.size(18.dp))
                    }

                    if (showThemeAction) {
                        IconButton(onClick = onToggleTheme, modifier = Modifier.size(32.dp)) {
                            Icon(
                                if (themeMode == AppThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = stringResource(R.string.theme_toggle),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .then(if (compactHeader) Modifier else Modifier.imePadding()),
        ) {
            if (showQuickCommands && !keyboardVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    quickCommands.forEach { cmd ->
                        AssistChip(
                            onClick = { executeOrSend(cmd) },
                            label = {
                                Text(cmd, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            },
                        )
                    }
                }
            }

            val isDark = when (themeMode) {
                AppThemeMode.DARK, AppThemeMode.AMOLED -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            val terminalBg = when (themeMode) {
                AppThemeMode.AMOLED -> Color(0xFF000000)
                else -> if (isDark) Color(0xFF05070C) else Color(0xFFF6F8FA)
            }
            val promptGreen = if (isDark) PocketGreen else Color(0xFF0D7A3E)
            val commandTextColor = if (isDark) Color(0xFFF0F6FC) else MaterialTheme.colorScheme.onSurface
            val outputTextColor = if (isDark) Color(0xFFD2DCE8) else MaterialTheme.colorScheme.onSurface
            val emptyStateColor = if (isDark) Color(0xFF6E7681) else MaterialTheme.colorScheme.onSurfaceVariant
            val lastFailedLine = lines.lastOrNull()?.takeIf { !usePty && !isRunning && it.exitCode != 0 }

            if (lastFailedLine != null && onFixWithAi != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exit code ${lastFailedLine.exitCode}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text = lastFailedLine.command,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { onFixWithAi(lastFailedLine.command, lastFailedLine.output, lastFailedLine.exitCode) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("⚡ Fix with AI", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Full-Screen Linux Console Viewport
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .border(
                        1.dp,
                        if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(14.dp),
                    )
                    .clickable { openTerminalKeyboard() },
                color = terminalBg,
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(terminalScrollState)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SelectionContainer {
                        if (usePty) {
                            Text(
                                text = ptyBuffer.ifBlank {
                                    "CS Personal Engine · Ubuntu arm64 Full Root Interactive PTY\n" +
                                        "Run any command or CLI: apt, npm -g, pip, uv, cargo, git, gh, codex, gemini, opencode, aider, python3, node...\n" +
                                        "root@cs-engine:$terminalPromptPath# "
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 17.5.sp,
                                color = outputTextColor,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (lines.isEmpty()) {
                                    Text(
                                        stringResource(R.string.workspace_terminal_ready),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = emptyStateColor,
                                    )
                                }
                                lines.forEach { item ->
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TerminalCommandPrompt(promptPath = terminalPromptPath, command = item.command, isDark = isDark)
                                        if (item.output.isNotEmpty()) {
                                            Text(
                                                text = sanitizeTerminalOutput(item.output),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                lineHeight = 18.sp,
                                                color = if (item.exitCode != 0) MaterialTheme.colorScheme.error else outputTextColor,
                                                modifier = Modifier.padding(start = 6.dp),
                                            )
                                        }
                                    }
                                }
                                if (isRunning && currentCommand != null) {
                                    TerminalCommandPrompt(promptPath = terminalPromptPath, command = currentCommand, isDark = isDark)
                                }
                                if (liveOutput.isNotBlank()) {
                                    Text(
                                        sanitizeTerminalOutput(liveOutput),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = outputTextColor,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // Hardware Control Strip — ALWAYS visible (even with keyboard up) for interactive CLIs!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                TerminalKeyButton("TAB", "Tab complete") {
                    if (usePty) {
                        onSendPtyRaw?.invoke(commandInput.text + "\t")
                        commandInput = TextFieldValue()
                    } else {
                        commandInput = commandInput.copy(text = commandInput.text + "  ", selection = TextRange(commandInput.text.length + 2))
                    }
                }
                TerminalKeyButton("CTRL+C", "Interrupt / SIGINT") {
                    if (usePty) onInterruptPty?.invoke()
                    else if (isRunning) onInterrupt?.invoke()
                    else commandInput = TextFieldValue()
                }
                TerminalKeyButton("CTRL+D", "EOF") {
                    if (usePty) onSendPtyRaw?.invoke("\u0004")
                }
                TerminalKeyButton("ESC", "Escape") {
                    if (usePty) onSendPtyRaw?.invoke("\u001B") else commandInput = TextFieldValue()
                }
                TerminalKeyButton("↑", "Previous command") {
                    if (usePty && commandInput.text.isEmpty()) {
                        onSendPtyRaw?.invoke("\u001B[A")
                    } else {
                        commandHistory.getOrNull(if (historyIndex < 0) commandHistory.lastIndex else (historyIndex - 1).coerceAtLeast(0))?.let {
                            historyIndex = if (historyIndex < 0) commandHistory.lastIndex else (historyIndex - 1).coerceAtLeast(0)
                            commandInput = TextFieldValue(it, TextRange(it.length))
                        }
                    }
                }
                TerminalKeyButton("↓", "Next command") {
                    if (usePty && commandInput.text.isEmpty()) {
                        onSendPtyRaw?.invoke("\u001B[B")
                    } else if (historyIndex >= 0) {
                        historyIndex = (historyIndex + 1).takeIf { it < commandHistory.size } ?: -1
                        val next = commandHistory.getOrNull(historyIndex) ?: ""
                        commandInput = TextFieldValue(next, TextRange(next.length))
                    }
                }
                TerminalIconKeyButton(Icons.Default.ArrowBack, "Left") {
                    if (usePty && commandInput.text.isEmpty()) onSendPtyRaw?.invoke("\u001B[D")
                    else commandInput = commandInput.copy(selection = TextRange((commandInput.selection.start - 1).coerceAtLeast(0)))
                }
                TerminalIconKeyButton(Icons.Default.ArrowForward, "Right") {
                    if (usePty && commandInput.text.isEmpty()) onSendPtyRaw?.invoke("\u001B[C")
                    else commandInput = commandInput.copy(selection = TextRange((commandInput.selection.end + 1).coerceAtMost(commandInput.text.length)))
                }
                TerminalKeyButton("y↵", "Confirm yes") {
                    if (usePty) onSendPtyRaw?.invoke("y\n") else onInput("y")
                }
                TerminalKeyButton("CTRL", "Ctrl modifier", active = ctrlActive) {
                    ctrlActive = !ctrlActive
                    if (ctrlActive) openTerminalKeyboard()
                }
            }

            // Dedicated Sleek Command Input Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (usePty) "#" else "$",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = promptGreen,
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = commandInput,
                        onValueChange = { next ->
                            if (ctrlActive && next.text.length > commandInput.text.length) {
                                val ch = next.text.last().lowercaseChar()
                                when (ch) {
                                    'c' -> if (usePty) onInterruptPty?.invoke() else onInterrupt?.invoke()
                                    'd' -> if (usePty) onSendPtyRaw?.invoke("\u0004")
                                    'z' -> if (usePty) onSendPtyRaw?.invoke("\u001A")
                                    'l' -> if (usePty) onClearPty?.invoke() else onClear()
                                }
                                ctrlActive = false
                            } else {
                                commandInput = next
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(inputFocusRequester),
                        singleLine = true,
                        cursorBrush = SolidColor(promptGreen),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = commandTextColor,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = { submitCommand() },
                            onDone = { submitCommand() },
                            onGo = { submitCommand() },
                        ),
                        decorationBox = { inner ->
                            if (commandInput.text.isEmpty()) {
                                Text(
                                    text = "Run any Linux command or CLI (apt, npm -g, pip, codex, gemini, gh…)",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    color = emptyStateColor,
                                    maxLines = 1,
                                )
                            }
                            inner()
                        },
                    )
                    IconButton(
                        onClick = { submitCommand() },
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Execute command",
                            tint = promptGreen,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalKeyButton(
    label: String,
    description: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
        modifier = Modifier.height(30.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (active) PocketOrange.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
            contentColor = if (active) PocketOrange else MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(
            1.dp,
            if (active) PocketOrange else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Text(
            if (active) "$label ✓" else label,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun TerminalIconKeyButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(30.dp)) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun TerminalCommandPrompt(promptPath: String, command: String, isDark: Boolean = true) {
    val promptGreen = if (isDark) PocketGreen else Color(0xFF0D7A3E)
    val commandColor = if (isDark) Color(0xFFF0F6FC) else MaterialTheme.colorScheme.onSurface
    val promptText = remember(promptPath, command, isDark) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = promptGreen, fontWeight = FontWeight.Bold)) {
                append("root@cs-engine:$promptPath# ")
            }
            withStyle(SpanStyle(color = commandColor, fontWeight = FontWeight.SemiBold)) {
                append(command)
            }
        }
    }
    Text(
        text = promptText,
        modifier = Modifier.fillMaxWidth(),
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        softWrap = true,
    )
}
