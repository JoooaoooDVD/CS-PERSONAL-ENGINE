package com.jarves.mh.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.runtime.TerminalScreenSnapshot
import com.jarves.mh.ui.theme.AppThemeMode
import com.jarves.mh.ui.theme.EngineCyan
import com.jarves.mh.ui.theme.EngineEmerald
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class TerminalSessionTabUi(
    val id: String,
    val title: String,
    val alive: Boolean = true,
)

/**
 * Sentinel character kept at index 0 of the invisible Android IME input sink so soft-keyboard
 * Backspace (`DEL`) ALWAYS fires `onValueChange` even at the start of an empty prompt!
 */
private const val IME_SENTINEL = "\u200B"

@OptIn(ExperimentalFoundationApi::class)
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
    title: String = "Linux Shell",
    subtitle: String = "/workspace",
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
    interactivePtyMode: Boolean = true,
    onTogglePtyMode: ((Boolean) -> Unit)? = null,
    ptyBuffer: String = "",
    ptySnapshot: TerminalScreenSnapshot? = null,
    ptyAlive: Boolean = false,
    sessionTabs: List<TerminalSessionTabUi> = emptyList(),
    activeSessionTabId: String = "",
    onSelectSessionTab: ((String) -> Unit)? = null,
    onNewSessionTab: (() -> Unit)? = null,
    onCloseSessionTab: ((String) -> Unit)? = null,
    onEnsurePtyStarted: (() -> Unit)? = null,
    onSendPtyCommand: ((String) -> Unit)? = null,
    onSendPtyRaw: ((String) -> Unit)? = null,
    onPastePty: ((String) -> Unit)? = null,
    onResizePty: ((Int, Int) -> Unit)? = null,
    onInterruptPty: (() -> Unit)? = null,
    onClearPty: (() -> Unit)? = null,
    onRestartPty: (() -> Unit)? = null,
    onOpenCliHub: (() -> Unit)? = null,
    onCloseFullScreen: (() -> Unit)? = null,
    isImmersiveFullScreen: Boolean = false,
    onToggleImmersiveFullScreen: (() -> Unit)? = null,
) {
    val clipboard = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    var fontSizeSp by rememberSaveable { mutableFloatStateOf(12.5f) }
    var ctrlSticky by rememberSaveable { mutableStateOf(false) }
    var altSticky by rememberSaveable { mutableStateOf(false) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }

    // Hidden IME buffer initialized with sentinel so Backspace & Enter work 100% natively like Termux
    var imeValue by remember {
        mutableStateOf(TextFieldValue(IME_SENTINEL, TextRange(IME_SENTINEL.length)))
    }

    val focusTerminal = {
        selectionMode = false
        focusRequester.requestFocus()
        scope.launch {
            delay(30)
            keyboardController?.show()
        }
    }

    LaunchedEffect(Unit) {
        onEnsurePtyStarted?.invoke()
        delay(80)
        focusRequester.requestFocus()
    }

    LaunchedEffect(commandDraft) {
        if (!commandDraft.isNullOrBlank()) {
            if (onSendPtyCommand != null) {
                onSendPtyCommand(commandDraft)
            } else {
                onRun(commandDraft)
            }
            onCommandDraftConsumed()
        }
    }

    val sendKeyBytes: (String) -> Unit = { raw ->
        if (onSendPtyRaw != null) {
            onSendPtyRaw(raw)
        } else if (raw == "\r" || raw == "\n") {
            onInput("")
        } else {
            onInput(raw)
        }
    }

    val dispatchTypedString: (String) -> Unit = { typed ->
        for (ch in typed) {
            when {
                ch == '\n' || ch == '\r' -> sendKeyBytes("\r")
                ctrlSticky -> {
                    val lower = ch.lowercaseChar()
                    val ctrlByte = when {
                        lower in 'a'..'z' -> (lower.code - 'a'.code + 1).toChar().toString()
                        ch == '[' -> "\u001B"
                        ch == '\\' -> "\u001C"
                        ch == ']' -> "\u001D"
                        ch == '^' -> "\u001E"
                        ch == '_' || ch == '/' -> "\u001F"
                        else -> ch.toString()
                    }
                    ctrlSticky = false
                    sendKeyBytes(ctrlByte)
                }
                altSticky -> {
                    altSticky = false
                    sendKeyBytes("\u001B$ch")
                }
                else -> sendKeyBytes(ch.toString())
            }
        }
    }

    val styledLines = ptySnapshot?.styledLines.orEmpty()
    val revision = ptySnapshot?.revision ?: ptyBuffer.length.toLong()

    // Instant auto-follow bottom whenever new terminal output or keystrokes arrive
    LaunchedEffect(revision, styledLines.size) {
        if (!selectionMode && styledLines.isNotEmpty()) {
            listState.scrollToItem((styledLines.size - 1).coerceAtLeast(0))
        }
    }

    val applyStatusBarPadding = onCloseFullScreen != null || (!compactHeader && isImmersiveFullScreen)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .then(if (applyStatusBarPadding) Modifier.statusBarsPadding() else Modifier)
            .imePadding()
            .navigationBarsPadding(),
    ) {
        // ── Razor-Thin 36dp Termux+ Session & Tool Strip ──
        Surface(
            color = Color(0xFF0B0D11),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (onCloseFullScreen != null) {
                    IconButton(onClick = onCloseFullScreen, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFFE2E8F0),
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }

                // Multi-Session Shell Tabs (`1: bash`, `2: bash`, `+`)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (sessionTabs.isNotEmpty() && onSelectSessionTab != null) {
                        sessionTabs.forEachIndexed { idx, tab ->
                            val active = tab.id == activeSessionTabId
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = if (active) Color(0xFF1A1F29) else Color(0xFF101319),
                                border = BorderStroke(
                                    1.dp,
                                    if (active) EngineEmerald.copy(alpha = 0.65f) else Color(0xFF1F2430),
                                ),
                                modifier = Modifier.combinedClickable(
                                    onClick = { onSelectSessionTab(tab.id) },
                                    onLongClick = { if (sessionTabs.size > 1) onCloseSessionTab?.invoke(tab.id) },
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(if (tab.alive) EngineEmerald else Color(0xFF64748B), CircleShape),
                                    )
                                    Text(
                                        text = "${idx + 1}: ${tab.title}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        color = if (active) Color(0xFFF8FAFC) else Color(0xFF94A3B8),
                                    )
                                    if (sessionTabs.size > 1 && onCloseSessionTab != null) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Close session",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable { onCloseSessionTab(tab.id) },
                                        )
                                    }
                                }
                            }
                        }
                        if (onNewSessionTab != null) {
                            Surface(
                                onClick = onNewSessionTab,
                                shape = RoundedCornerShape(7.dp),
                                color = Color(0xFF12161E),
                                border = BorderStroke(1.dp, Color(0xFF232936)),
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Add, contentDescription = "New shell tab", tint = EngineEmerald, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(if (ptyAlive) EngineEmerald else Color(0xFF64748B), CircleShape),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = ptySnapshot?.windowTitle?.takeIf { it.isNotBlank() } ?: "$title · $promptPath",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0),
                            maxLines = 1,
                        )
                    }
                }

                // Font Zoom A- / A+
                Text(
                    text = "A-",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier
                        .clickable { fontSizeSp = (fontSizeSp - 1f).coerceIn(9f, 20f) }
                        .padding(horizontal = 5.dp, vertical = 4.dp),
                )
                Text(
                    text = "A+",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier
                        .clickable { fontSizeSp = (fontSizeSp + 1f).coerceIn(9f, 20f) }
                        .padding(horizontal = 5.dp, vertical = 4.dp),
                )

                if (onOpenCliHub != null) {
                    Surface(
                        onClick = onOpenCliHub,
                        shape = RoundedCornerShape(6.dp),
                        color = EngineCyan.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, EngineCyan.copy(alpha = 0.4f)),
                    ) {
                        Text(
                            text = "+CLI",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EngineCyan,
                        )
                    }
                }

                // Copy / Select toggle
                IconButton(
                    onClick = {
                        val copyText = ptySnapshot?.plainText?.ifBlank { ptyBuffer } ?: ptyBuffer
                        if (copyText.isNotBlank()) {
                            clipboard.setText(AnnotatedString(copyText))
                        }
                        selectionMode = !selectionMode
                    },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy terminal output",
                        tint = if (selectionMode) EngineCyan else Color(0xFF94A3B8),
                        modifier = Modifier.size(15.dp),
                    )
                }

                if (onRestartPty != null) {
                    IconButton(onClick = onRestartPty, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset PTY", tint = Color(0xFF94A3B8), modifier = Modifier.size(15.dp))
                    }
                }

                if (onToggleImmersiveFullScreen != null) {
                    IconButton(onClick = onToggleImmersiveFullScreen, modifier = Modifier.size(28.dp)) {
                        Icon(
                            if (isImmersiveFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Toggle full screen terminal",
                            tint = if (isImmersiveFullScreen) EngineEmerald else Color(0xFF94A3B8),
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = Color(0xFF1A1E26), thickness = 0.5.dp)

        // ── 100% Edge-to-Edge Pitch-Black VT100 Terminal Screen (NO Chat Input Box!) ──
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF000000))
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom != 1f) {
                            fontSizeSp = (fontSizeSp * zoom).coerceIn(9f, 20f)
                        }
                    }
                }
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { focusTerminal() },
                ),
        ) {
            // Dynamically compute exact VT100 rows x cols from viewport pixel size & monospace glyph metrics!
            val charWidthPx = with(density) { (fontSizeSp * 0.61f).sp.toPx() }.coerceAtLeast(4f)
            val lineHeightSp = fontSizeSp * 1.32f
            val lineHeightPx = with(density) { lineHeightSp.sp.toPx() }.coerceAtLeast(8f)
            val computedCols = ((constraints.maxWidth - with(density) { 12.dp.toPx() }) / charWidthPx).toInt().coerceIn(28, 220)
            val computedRows = ((constraints.maxHeight - with(density) { 8.dp.toPx() }) / lineHeightPx).toInt().coerceIn(12, 140)

            LaunchedEffect(computedRows, computedCols, activeSessionTabId) {
                onResizePty?.invoke(computedRows, computedCols)
            }

            // Invisible 1dp Direct Keystroke Sink — sends every character, Backspace & Enter IMMEDIATELY to PTY!
            BasicTextField(
                value = imeValue,
                onValueChange = { next ->
                    val currentText = next.text
                    when {
                        currentText.isEmpty() -> {
                            // Sentinel was deleted -> user pressed Backspace!
                            sendKeyBytes("\u007F")
                        }
                        currentText.startsWith(IME_SENTINEL) -> {
                            val added = currentText.substring(IME_SENTINEL.length)
                            if (added.isNotEmpty()) dispatchTypedString(added)
                        }
                        else -> {
                            dispatchTypedString(currentText.replace(IME_SENTINEL, ""))
                        }
                    }
                    imeValue = TextFieldValue(IME_SENTINEL, TextRange(IME_SENTINEL.length))
                },
                modifier = Modifier
                    .size(1.dp)
                    .alpha(0f)
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.Enter, Key.NumPadEnter -> { sendKeyBytes("\r"); true }
                            Key.Backspace -> { sendKeyBytes("\u007F"); true }
                            Key.Delete -> { sendKeyBytes("\u001B[3~"); true }
                            Key.Tab -> { sendKeyBytes("\t"); true }
                            Key.Escape -> { sendKeyBytes("\u001B"); true }
                            Key.DirectionUp -> { sendKeyBytes("\u001B[A"); true }
                            Key.DirectionDown -> { sendKeyBytes("\u001B[B"); true }
                            Key.DirectionRight -> { sendKeyBytes("\u001B[C"); true }
                            Key.DirectionLeft -> { sendKeyBytes("\u001B[D"); true }
                            Key.MoveHome -> { sendKeyBytes("\u001B[H"); true }
                            Key.MoveEnd -> { sendKeyBytes("\u001B[F"); true }
                            Key.PageUp -> { sendKeyBytes("\u001B[5~"); true }
                            Key.PageDown -> { sendKeyBytes("\u001B[6~"); true }
                            else -> false
                        }
                    },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.None,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { sendKeyBytes("\r") },
                    onGo = { sendKeyBytes("\r") },
                    onSend = { sendKeyBytes("\r") },
                    onNext = { sendKeyBytes("\t") },
                ),
                cursorBrush = SolidColor(Color.Transparent),
            )

            if (selectionMode) {
                // Full-text copy/select overlay when user taps Copy icon
                SelectionContainer(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(styledLines) { _, line ->
                            Text(
                                text = line,
                                fontFamily = FontFamily.Monospace,
                                fontSize = fontSizeSp.sp,
                                lineHeight = lineHeightSp.sp,
                                softWrap = true,
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                ) {
                    if (styledLines.isEmpty()) {
                        item {
                            Text(
                                text = ptyBuffer.ifBlank { "root@cs-engine:~# █" },
                                fontFamily = FontFamily.Monospace,
                                fontSize = fontSizeSp.sp,
                                lineHeight = lineHeightSp.sp,
                                color = Color(0xFF34D399),
                            )
                        }
                    } else {
                        itemsIndexed(styledLines) { _, line ->
                            Text(
                                text = line,
                                fontFamily = FontFamily.Monospace,
                                fontSize = fontSizeSp.sp,
                                lineHeight = lineHeightSp.sp,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        }

        // ── Authentic Termux 2-Row Extra-Keys Matrix Bar (Direct Keystrokes!) ──
        HorizontalDivider(color = Color(0xFF1A1E26), thickness = 0.5.dp)
        Surface(
            color = Color(0xFF0B0D11),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                // Row 1: ESC  /  -  |  HOME  UP  END  PGUP  ^C
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    TermuxMatrixKey("ESC", Modifier.weight(1f)) { sendKeyBytes("\u001B"); focusTerminal() }
                    TermuxMatrixKey("/", Modifier.weight(1f)) { sendKeyBytes("/"); focusTerminal() }
                    TermuxMatrixKey("-", Modifier.weight(1f)) { sendKeyBytes("-"); focusTerminal() }
                    TermuxMatrixKey("|", Modifier.weight(1f)) { sendKeyBytes("|"); focusTerminal() }
                    TermuxMatrixKey("HOME", Modifier.weight(1.1f)) { sendKeyBytes("\u001B[H"); focusTerminal() }
                    TermuxMatrixKey("▲", Modifier.weight(1f)) { sendKeyBytes("\u001B[A"); focusTerminal() }
                    TermuxMatrixKey("END", Modifier.weight(1.1f)) { sendKeyBytes("\u001B[F"); focusTerminal() }
                    TermuxMatrixKey("PGUP", Modifier.weight(1.1f)) { sendKeyBytes("\u001B[5~"); focusTerminal() }
                    TermuxMatrixKey("^C", Modifier.weight(1f), danger = true) {
                        onInterruptPty?.invoke() ?: onInterrupt?.invoke()
                        focusTerminal()
                    }
                }
                // Row 2: TAB  CTRL  ALT  ~  LEFT  DOWN  RIGHT  PGDN  PASTE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    TermuxMatrixKey("TAB", Modifier.weight(1f)) { sendKeyBytes("\t"); focusTerminal() }
                    TermuxMatrixKey("CTRL", Modifier.weight(1.1f), active = ctrlSticky) {
                        ctrlSticky = !ctrlSticky
                        focusTerminal()
                    }
                    TermuxMatrixKey("ALT", Modifier.weight(1.1f), active = altSticky) {
                        altSticky = !altSticky
                        focusTerminal()
                    }
                    TermuxMatrixKey("~", Modifier.weight(0.9f)) { sendKeyBytes("~"); focusTerminal() }
                    TermuxMatrixKey("◀", Modifier.weight(1f)) { sendKeyBytes("\u001B[D"); focusTerminal() }
                    TermuxMatrixKey("▼", Modifier.weight(1f)) { sendKeyBytes("\u001B[B"); focusTerminal() }
                    TermuxMatrixKey("▶", Modifier.weight(1f)) { sendKeyBytes("\u001B[C"); focusTerminal() }
                    TermuxMatrixKey("PGDN", Modifier.weight(1.1f)) { sendKeyBytes("\u001B[6~"); focusTerminal() }
                    TermuxMatrixKey("PASTE", Modifier.weight(1.2f)) {
                        val clip = clipboard.getText()?.text.orEmpty()
                        if (clip.isNotEmpty()) {
                            if (onPastePty != null) onPastePty(clip) else sendKeyBytes(clip)
                        }
                        focusTerminal()
                    }
                }
            }
        }
    }
}

@Composable
private fun TermuxMatrixKey(
    label: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val bgColor = when {
        active -> EngineEmerald.copy(alpha = 0.25f)
        danger -> Color(0xFF281216)
        else -> Color(0xFF151921)
    }
    val borderColor = when {
        active -> EngineEmerald
        danger -> Color(0xFF5C232C)
        else -> Color(0xFF222834)
    }
    val textColor = when {
        active -> EngineEmerald
        danger -> Color(0xFFFF8787)
        else -> Color(0xFFE2E8F0)
    }
    Surface(
        onClick = onClick,
        modifier = modifier.height(30.dp),
        shape = RoundedCornerShape(5.dp),
        color = bgColor,
        border = BorderStroke(0.8.dp, borderColor),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
            )
        }
    }
}
