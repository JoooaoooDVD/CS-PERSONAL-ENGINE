package com.jarves.mh.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jarves.mh.runtime.TerminalScreenSnapshot
import com.jarves.mh.runtime.Vt100TerminalEmulator
import com.jarves.mh.ui.theme.AppThemeMode
import com.jarves.mh.ui.theme.ClaudeTerracotta
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

data class TerminalSessionTabUi(
    val id: String,
    val title: String,
    val alive: Boolean = true,
)

private const val STUDIO_IME_ANCHOR = "  "
private val ConsoleCanvasBg = Color(0xFF0C0C0E)
private val ConsoleChromeBg = Color(0xFF111114)
private val ConsoleBorder = Color(0xFF222227)

/**
 * Ultra-Minimal Claude-Code Style Voice Mic Icon Button (`🎤`).
 * Matches the clean microphone icon inside the `Code anything...` bar and Full-Page CLI.
 */
@Composable
fun StudioVoiceMicButton(
    onTranscribed: (String) -> Unit,
    compact: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var listening by remember { mutableStateOf(false) }
    var recognizerRef by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val fallbackVoiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { res ->
        listening = false
        if (res.resultCode == Activity.RESULT_OK) {
            val matches = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()?.trim().orEmpty()
            if (spoken.isNotEmpty()) onTranscribed(spoken)
        }
    }

    val launchSystemDialogFallback = {
        runCatching {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak…")
            }
            fallbackVoiceLauncher.launch(intent)
        }.onFailure {
            listening = false
        }
    }

    val startInlineRecognition = {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            runCatching {
                recognizerRef?.destroy()
                val sr = SpeechRecognizer.createSpeechRecognizer(context)
                recognizerRef = sr
                sr.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { listening = true }
                    override fun onBeginningOfSpeech() { listening = true }
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() { listening = false }
                    override fun onError(error: Int) {
                        listening = false
                        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                            error == SpeechRecognizer.ERROR_CLIENT
                        ) {
                            launchSystemDialogFallback()
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        listening = false
                        val text = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            .orEmpty()
                        if (text.isNotEmpty()) onTranscribed(text)
                    }
                    override fun onPartialResults(partialResults: Bundle?) = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                listening = true
                sr.startListening(intent)
            }.onFailure {
                launchSystemDialogFallback()
            }
        } else {
            launchSystemDialogFallback()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) startInlineRecognition() else launchSystemDialogFallback()
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { recognizerRef?.destroy() }
            recognizerRef = null
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mic_alpha",
    )

    val boxSize = if (compact) 30.dp else 34.dp
    Box(
        modifier = modifier
            .size(boxSize)
            .background(
                color = if (listening) ClaudeTerracotta.copy(alpha = pulseAlpha * 0.35f) else Color.Transparent,
                shape = CircleShape,
            )
            .clickable {
                if (listening) {
                    listening = false
                    runCatching { recognizerRef?.stopListening() }
                } else {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) startInlineRecognition()
                    else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Voice Input",
            tint = if (listening) ClaudeTerracotta else Color(0xFFA1A1AA),
            modifier = Modifier.size(if (compact) 18.dp else 20.dp),
        )
    }
}

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
    title: String = "CLI",
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
    isImmersiveFullScreen: Boolean = true,
    onToggleImmersiveFullScreen: (() -> Unit)? = null,
) {
    val clipboard = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var fontSizeSp by rememberSaveable { mutableFloatStateOf(12f) }
    var ctrlSticky by rememberSaveable { mutableStateOf(false) }
    var altSticky by rememberSaveable { mutableStateOf(false) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var showMacroDock by rememberSaveable { mutableStateOf(true) }
    var scrollRowsFromBottom by remember { mutableFloatStateOf(0f) }

    var imeValue by remember {
        mutableStateOf(TextFieldValue(STUDIO_IME_ANCHOR, TextRange(STUDIO_IME_ANCHOR.length)))
    }

    val focusTerminal = {
        selectionMode = false
        focusRequester.requestFocus()
        scope.launch {
            delay(25)
            keyboardController?.show()
        }
    }

    LaunchedEffect(Unit) {
        onEnsurePtyStarted?.invoke()
        delay(60)
        focusRequester.requestFocus()
    }

    LaunchedEffect(commandDraft) {
        if (!commandDraft.isNullOrBlank()) {
            if (onSendPtyCommand != null) onSendPtyCommand(commandDraft) else onRun(commandDraft)
            onCommandDraftConsumed()
        }
    }

    val sendKeyBytes: (String) -> Unit = { raw ->
        scrollRowsFromBottom = 0f
        if (onSendPtyRaw != null) {
            onSendPtyRaw(raw)
        } else if (raw == "\r" || raw == "\n") {
            onInput("")
        } else {
            onInput(raw)
        }
    }

    val dispatchTypedString: (String) -> Unit = { typed ->
        if (typed.length > 1 && !ctrlSticky && !altSticky && '\n' !in typed && '\r' !in typed) {
            scrollRowsFromBottom = 0f
            if (onPastePty != null) onPastePty(typed) else sendKeyBytes(typed)
        } else {
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
    }

    // 100% FULL-PAGE PURE DARK CLI TERMINAL
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ConsoleCanvasBg)
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding(),
    ) {
        // Minimal Pure-Dark Single-Line Top Bar (`<` · Sessions · `🎤` · `+CLI` · `⌨` · `↻`)
        Surface(
            color = ConsoleChromeBg,
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
                    IconButton(onClick = onCloseFullScreen, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = Color(0xFFF4F4F6),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (sessionTabs.isNotEmpty() && onSelectSessionTab != null) {
                        sessionTabs.forEachIndexed { idx, tab ->
                            val active = tab.id == activeSessionTabId
                            Surface(
                                shape = CircleShape,
                                color = if (active) Color(0xFF202026) else Color.Transparent,
                                border = if (active) BorderStroke(0.8.dp, Color(0xFF32323A)) else null,
                                modifier = Modifier.combinedClickable(
                                    onClick = { onSelectSessionTab(tab.id) },
                                    onLongClick = { if (sessionTabs.size > 1) onCloseSessionTab?.invoke(tab.id) },
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(if (tab.alive) ClaudeTerracotta else Color(0xFF52525B), CircleShape),
                                    )
                                    Text(
                                        text = "CLI ${idx + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (active) Color(0xFFF4F4F6) else Color(0xFF8E8E95),
                                    )
                                    if (sessionTabs.size > 1 && onCloseSessionTab != null) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = Color(0xFF8E8E95),
                                            modifier = Modifier
                                                .size(11.dp)
                                                .clickable { onCloseSessionTab(tab.id) },
                                        )
                                    }
                                }
                            }
                        }
                        if (onNewSessionTab != null) {
                            IconButton(onClick = onNewSessionTab, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "New CLI", tint = Color(0xFFA1A1AA), modifier = Modifier.size(15.dp))
                            }
                        }
                    } else {
                        Text(
                            text = ptySnapshot?.windowTitle?.takeIf { it.isNotBlank() } ?: title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFF4F4F6),
                            maxLines = 1,
                        )
                    }
                }

                StudioVoiceMicButton(
                    compact = true,
                    onTranscribed = { spokenText ->
                        scrollRowsFromBottom = 0f
                        if (onPastePty != null) onPastePty(spokenText) else sendKeyBytes(spokenText)
                        focusTerminal()
                    },
                )

                if (onOpenCliHub != null) {
                    Surface(
                        onClick = onOpenCliHub,
                        shape = CircleShape,
                        color = Color(0xFF1C1C22),
                        border = BorderStroke(0.8.dp, Color(0xFF2D2D35)),
                    ) {
                        Text(
                            text = "+CLI",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE4E4E7),
                        )
                    }
                }

                IconButton(
                    onClick = { showMacroDock = !showMacroDock },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        if (showMacroDock) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                        contentDescription = "Keys",
                        tint = Color(0xFFA1A1AA),
                        modifier = Modifier.size(16.dp),
                    )
                }

                IconButton(
                    onClick = {
                        val copyText = ptySnapshot?.plainText?.ifBlank { ptyBuffer } ?: ptyBuffer
                        if (copyText.isNotBlank()) clipboard.setText(AnnotatedString(copyText))
                        selectionMode = !selectionMode
                    },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = if (selectionMode) ClaudeTerracotta else Color(0xFFA1A1AA),
                        modifier = Modifier.size(14.dp),
                    )
                }

                if (onRestartPty != null) {
                    IconButton(onClick = onRestartPty, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color(0xFFA1A1AA), modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
        HorizontalDivider(color = ConsoleBorder, thickness = 0.6.dp)

        // Full-Page Pixel-Exact Cell-Grid Canvas (Pinch to zoom font size, swipe to scroll!)
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(ConsoleCanvasBg),
        ) {
            val fontPx = with(density) { fontSizeSp.sp.toPx() }.coerceAtLeast(13f)
            val horizontalPadPx = with(density) { 6.dp.toPx() }
            val verticalPadPx = with(density) { 4.dp.toPx() }

            val textPaint = remember(fontPx) {
                AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.SUBPIXEL_TEXT_FLAG).apply {
                    typeface = Typeface.MONOSPACE
                    textSize = fontPx
                }
            }
            val boldTextPaint = remember(fontPx) {
                AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.SUBPIXEL_TEXT_FLAG).apply {
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    textSize = fontPx
                }
            }

            val rawGlyphWidth = remember(textPaint) { textPaint.measureText("M").coerceAtLeast(6f) }
            val fontAscent = remember(textPaint) { -textPaint.fontMetrics.ascent }
            val fontDescent = remember(textPaint) { textPaint.fontMetrics.descent }
            val cellHeightPx = remember(fontAscent, fontDescent) {
                ceil(fontAscent + fontDescent + 2f).coerceAtLeast(12f)
            }

            val usableWidthPx = (constraints.maxWidth - horizontalPadPx * 2f).coerceAtLeast(120f)
            val usableHeightPx = (constraints.maxHeight - verticalPadPx * 2f).coerceAtLeast(80f)
            val computedCols = floor(usableWidthPx / rawGlyphWidth).toInt().coerceIn(28, 280)
            val computedRows = floor(usableHeightPx / cellHeightPx).toInt().coerceIn(10, 160)
            val cellWidthPx = usableWidthPx / computedCols.toFloat()

            LaunchedEffect(computedRows, computedCols, activeSessionTabId) {
                onResizePty?.invoke(computedRows, computedCols)
            }

            BasicTextField(
                value = imeValue,
                onValueChange = { next ->
                    val cur = next.text
                    when {
                        cur.length < STUDIO_IME_ANCHOR.length -> {
                            val deletedCount = (STUDIO_IME_ANCHOR.length - cur.length).coerceAtLeast(1)
                            repeat(deletedCount) { sendKeyBytes("\u007F") }
                        }
                        cur.startsWith(STUDIO_IME_ANCHOR) -> {
                            val added = cur.substring(STUDIO_IME_ANCHOR.length)
                            if (added.isNotEmpty()) dispatchTypedString(added)
                        }
                        else -> {
                            val cleaned = cur.trimStart(' ')
                            if (cleaned.isNotEmpty()) dispatchTypedString(cleaned) else sendKeyBytes("\u007F")
                        }
                    }
                    imeValue = TextFieldValue(STUDIO_IME_ANCHOR, TextRange(STUDIO_IME_ANCHOR.length))
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
                    keyboardType = KeyboardType.Text,
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

            val renderRows = ptySnapshot?.renderRows.orEmpty()
            val maxScrollRows = (renderRows.size - computedRows).coerceAtLeast(0).toFloat()

            val scrollState = rememberScrollableState { deltaPx ->
                if (ptySnapshot?.alternateScreen == true) {
                    val rowsMoved = (deltaPx / cellHeightPx).toInt()
                    if (rowsMoved > 0) sendKeyBytes("\u001B[A")
                    else if (rowsMoved < 0) sendKeyBytes("\u001B[B")
                    deltaPx
                } else {
                    val rowDelta = deltaPx / cellHeightPx
                    val nextOffset = (scrollRowsFromBottom + rowDelta).coerceIn(0f, maxScrollRows)
                    val consumed = (nextOffset - scrollRowsFromBottom) * cellHeightPx
                    scrollRowsFromBottom = nextOffset
                    consumed
                }
            }

            if (selectionMode) {
                val styledLines = ptySnapshot?.styledLines.orEmpty()
                SelectionContainer(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp)) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(styledLines) { _, line ->
                            Text(
                                text = line,
                                fontFamily = FontFamily.Monospace,
                                fontSize = fontSizeSp.sp,
                                lineHeight = (fontSizeSp * 1.25f).sp,
                                softWrap = true,
                            )
                        }
                    }
                }
            } else {
                val cursorBlink = rememberInfiniteTransition(label = "cursor_glow")
                val cursorAlpha by cursorBlink.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 0.35f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(650, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "cursor_alpha",
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .scrollable(state = scrollState, orientation = Orientation.Vertical)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                if (zoom != 1f) {
                                    fontSizeSp = (fontSizeSp * zoom).coerceIn(8.5f, 22f)
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { focusTerminal() })
                        },
                ) {
                    if (renderRows.isEmpty()) {
                        drawIntoCanvas { canvas ->
                            textPaint.color = 0xFFD97757.toInt()
                            canvas.nativeCanvas.drawText(
                                "~/workspace ❯ ",
                                horizontalPadPx,
                                verticalPadPx + fontAscent,
                                textPaint,
                            )
                        }
                        return@Canvas
                    }

                    val totalRows = renderRows.size
                    val clampedBack = scrollRowsFromBottom.toInt().coerceIn(0, (totalRows - 1).coerceAtLeast(0))
                    val endRowExclusive = (totalRows - clampedBack).coerceAtLeast(1)
                    val startRow = (endRowExclusive - computedRows).coerceAtLeast(0)

                    val snap = ptySnapshot
                    val absCursorRow = if (snap != null) snap.scrollbackCount + snap.cursorScreenRow else -1
                    val absCursorCol = snap?.cursorCol ?: -1
                    val showCursor = snap?.cursorVisible == true && clampedBack == 0

                    // PASS 1: Contiguous Background Rectangles
                    for (visIdx in 0 until (endRowExclusive - startRow)) {
                        val rowObj = renderRows[startRow + visIdx]
                        val yTop = verticalPadPx + visIdx * cellHeightPx
                        val rowLen = minOf(rowObj.length, computedCols)
                        var c = 0
                        while (c < rowLen) {
                            val bgArgb = rowObj.bg[c]
                            if (bgArgb == Vt100TerminalEmulator.DEFAULT_BG) {
                                c++
                                continue
                            }
                            var runEnd = c + 1
                            while (runEnd < rowLen && rowObj.bg[runEnd] == bgArgb) {
                                runEnd++
                            }
                            drawRect(
                                color = Color(bgArgb),
                                topLeft = Offset(horizontalPadPx + c * cellWidthPx, yTop),
                                size = Size((runEnd - c) * cellWidthPx + 0.5f, cellHeightPx + 0.6f),
                            )
                            c = runEnd
                        }
                    }

                    // PASS 2: Cursor + Custom Vector Box/Block/Braille + Monospace Text
                    val singleCharBuf = CharArray(1)
                    for (visIdx in 0 until (endRowExclusive - startRow)) {
                        val absRowIdx = startRow + visIdx
                        val rowObj = renderRows[absRowIdx]
                        val yTop = verticalPadPx + visIdx * cellHeightPx
                        val baselineY = yTop + fontAscent
                        val rowLen = minOf(rowObj.length, computedCols)

                        if (showCursor && absRowIdx == absCursorRow && absCursorCol in 0 until computedCols) {
                            val cx = horizontalPadPx + absCursorCol * cellWidthPx
                            val cursorColor = ClaudeTerracotta.copy(alpha = cursorAlpha)
                            when (snap?.cursorStyle ?: 1) {
                                3, 4 -> drawRect(
                                    color = cursorColor,
                                    topLeft = Offset(cx, yTop + cellHeightPx - 3f),
                                    size = Size(cellWidthPx, 3f),
                                )
                                5, 6 -> drawRect(
                                    color = cursorColor,
                                    topLeft = Offset(cx, yTop),
                                    size = Size(2.5f.coerceAtLeast(cellWidthPx * 0.18f), cellHeightPx),
                                )
                                else -> drawRect(
                                    color = cursorColor,
                                    topLeft = Offset(cx, yTop),
                                    size = Size(cellWidthPx, cellHeightPx),
                                )
                            }
                        }

                        for (c in 0 until rowLen) {
                            val ch = rowObj.chars[c]
                            if (ch <= ' ') continue
                            val isBlockCursorHere = showCursor &&
                                absRowIdx == absCursorRow &&
                                c == absCursorCol &&
                                (snap?.cursorStyle ?: 1) in 0..2
                            val rawFg = if (isBlockCursorHere) 0xFF0C0C0E.toInt() else rowObj.fg[c]
                            val fl = rowObj.flags[c].toInt()
                            val fgColor = if ((fl and Vt100TerminalEmulator.FLAG_DIM) != 0 && !isBlockCursorHere) {
                                Color(rawFg).copy(alpha = 0.62f)
                            } else {
                                Color(rawFg)
                            }
                            val xLeft = horizontalPadPx + c * cellWidthPx

                            if (ch.code in 0x2500..0x259F || ch.code in 0x2800..0x28FF) {
                                if (drawCustomTerminalGlyph(ch, xLeft, yTop, cellWidthPx, cellHeightPx, fgColor)) {
                                    continue
                                }
                            }

                            val bold = (fl and Vt100TerminalEmulator.FLAG_BOLD) != 0 || isBlockCursorHere
                            val paint = if (bold) boldTextPaint else textPaint
                            paint.isUnderlineText = (fl and Vt100TerminalEmulator.FLAG_UNDERLINE) != 0
                            paint.textSkewX = if ((fl and Vt100TerminalEmulator.FLAG_ITALIC) != 0) -0.20f else 0f
                            paint.color = if ((fl and Vt100TerminalEmulator.FLAG_DIM) != 0 && !isBlockCursorHere) {
                                ((0x9E shl 24) or (rawFg and 0x00FFFFFF))
                            } else {
                                rawFg
                            }
                            singleCharBuf[0] = ch
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawText(singleCharBuf, 0, 1, xLeft, baselineY, paint)
                            }
                        }
                    }
                }
            }
        }

        // Minimal Pure-Dark Key Row (Toggleable via `⌨` in top bar)
        AnimatedVisibility(visible = showMacroDock) {
            Column {
                HorizontalDivider(color = ConsoleBorder, thickness = 0.6.dp)
                Surface(
                    color = ConsoleChromeBg,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        StudioMacroPill("ESC") { sendKeyBytes("\u001B"); focusTerminal() }
                        StudioMacroPill("TAB") { sendKeyBytes("\t"); focusTerminal() }
                        StudioMacroPill("CTRL", active = ctrlSticky) { ctrlSticky = !ctrlSticky; focusTerminal() }
                        StudioMacroPill("ALT", active = altSticky) { altSticky = !altSticky; focusTerminal() }
                        StudioMacroPill("◀") { sendKeyBytes("\u001B[D"); focusTerminal() }
                        StudioMacroPill("▼") { sendKeyBytes("\u001B[B"); focusTerminal() }
                        StudioMacroPill("▲") { sendKeyBytes("\u001B[A"); focusTerminal() }
                        StudioMacroPill("▶") { sendKeyBytes("\u001B[C"); focusTerminal() }
                        StudioMacroPill("/") { sendKeyBytes("/"); focusTerminal() }
                        StudioMacroPill("-") { sendKeyBytes("-"); focusTerminal() }
                        StudioMacroPill("~") { sendKeyBytes("~"); focusTerminal() }
                        StudioMacroPill("A-") { fontSizeSp = (fontSizeSp - 1f).coerceIn(8.5f, 22f) }
                        StudioMacroPill("A+") { fontSizeSp = (fontSizeSp + 1f).coerceIn(8.5f, 22f) }
                        StudioMacroPill("Paste") {
                            val clip = clipboard.getText()?.text.orEmpty()
                            if (clip.isNotEmpty()) {
                                if (onPastePty != null) onPastePty(clip) else sendKeyBytes(clip)
                            }
                            focusTerminal()
                        }
                        StudioMacroPill("^C", danger = true) {
                            onInterruptPty?.invoke() ?: onInterrupt?.invoke()
                            focusTerminal()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioMacroPill(
    label: String,
    active: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val bg = when {
        active -> ClaudeTerracotta.copy(alpha = 0.25f)
        danger -> Color(0xFF241315)
        else -> Color(0xFF19191E)
    }
    val border = when {
        active -> ClaudeTerracotta
        danger -> Color(0xFF4C2226)
        else -> ConsoleBorder
    }
    val fg = when {
        active -> ClaudeTerracotta
        danger -> Color(0xFFF87171)
        else -> Color(0xFFE4E4E7)
    }
    Surface(
        onClick = onClick,
        modifier = Modifier.height(27.dp),
        shape = RoundedCornerShape(7.dp),
        color = bg,
        border = BorderStroke(0.7.dp, border),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = fg,
                maxLines = 1,
            )
        }
    }
}

private fun DrawScope.drawCustomTerminalGlyph(
    ch: Char,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    color: Color,
): Boolean {
    val code = ch.code
    if (code in 0x2580..0x259F) {
        val hw = w * 0.5f
        val hh = h * 0.5f
        val ov = 0.5f
        when (code) {
            0x2580 -> drawRect(color, Offset(x, y), Size(w + ov, hh + ov))
            in 0x2581..0x2588 -> {
                val frac = (code - 0x2580) / 8f
                val bh = h * frac
                drawRect(color, Offset(x, y + h - bh), Size(w + ov, bh + ov))
            }
            in 0x2589..0x258F -> {
                val eighths = 8 - (code - 0x2588)
                val bw = w * (eighths / 8f)
                drawRect(color, Offset(x, y), Size(bw + ov, h + ov))
            }
            0x2590 -> drawRect(color, Offset(x + hw, y), Size(hw + ov, h + ov))
            0x2591 -> drawRect(color.copy(alpha = color.alpha * 0.25f), Offset(x, y), Size(w + ov, h + ov))
            0x2592 -> drawRect(color.copy(alpha = color.alpha * 0.50f), Offset(x, y), Size(w + ov, h + ov))
            0x2593 -> drawRect(color.copy(alpha = color.alpha * 0.75f), Offset(x, y), Size(w + ov, h + ov))
            0x2594 -> drawRect(color, Offset(x, y), Size(w + ov, h * 0.125f + ov))
            0x2595 -> drawRect(color, Offset(x + w * 0.875f, y), Size(w * 0.125f + ov, h + ov))
            in 0x2596..0x259F -> {
                val mask = when (code) {
                    0x2596 -> 4
                    0x2597 -> 8
                    0x2598 -> 1
                    0x2599 -> 1 or 4 or 8
                    0x259A -> 1 or 8
                    0x259B -> 1 or 2 or 4
                    0x259C -> 1 or 2 or 8
                    0x259D -> 2
                    0x259E -> 2 or 4
                    0x259F -> 2 or 4 or 8
                    else -> 0
                }
                if ((mask and 1) != 0) drawRect(color, Offset(x, y), Size(hw + ov, hh + ov))
                if ((mask and 2) != 0) drawRect(color, Offset(x + hw, y), Size(hw + ov, hh + ov))
                if ((mask and 4) != 0) drawRect(color, Offset(x, y + hh), Size(hw + ov, hh + ov))
                if ((mask and 8) != 0) drawRect(color, Offset(x + hw, y + hh), Size(hw + ov, hh + ov))
            }
        }
        return true
    }

    if (code in 0x2500..0x257F) {
        val cx = x + w * 0.5f
        val cy = y + h * 0.5f
        val thin = (w * 0.12f).coerceIn(1.2f, 2.4f)
        val heavy = (w * 0.26f).coerceIn(2.4f, 4.6f)
        fun seg(left: Int = 0, right: Int = 0, up: Int = 0, down: Int = 0) {
            if (left > 0) drawLine(color, Offset(x, cy), Offset(cx + 0.5f, cy), strokeWidth = if (left == 2) heavy else thin)
            if (right > 0) drawLine(color, Offset(cx - 0.5f, cy), Offset(x + w + 0.5f, cy), strokeWidth = if (right == 2) heavy else thin)
            if (up > 0) drawLine(color, Offset(cx, y), Offset(cx, cy + 0.5f), strokeWidth = if (up == 2) heavy else thin)
            if (down > 0) drawLine(color, Offset(cx, cy - 0.5f), Offset(cx, y + h + 0.5f), strokeWidth = if (down == 2) heavy else thin)
        }
        when (code) {
            0x2500, 0x2504, 0x2508, 0x254C -> seg(left = 1, right = 1)
            0x2501, 0x2505, 0x2509, 0x254D -> seg(left = 2, right = 2)
            0x2502, 0x2506, 0x250A, 0x254E -> seg(up = 1, down = 1)
            0x2503, 0x2507, 0x250B, 0x254F -> seg(up = 2, down = 2)
            0x250C, 0x256D -> seg(right = 1, down = 1)
            0x250F -> seg(right = 2, down = 2)
            0x2510, 0x256E -> seg(left = 1, down = 1)
            0x2513 -> seg(left = 2, down = 2)
            0x2514, 0x2570 -> seg(right = 1, up = 1)
            0x2517 -> seg(right = 2, up = 2)
            0x2518, 0x256F -> seg(left = 1, up = 1)
            0x251B -> seg(left = 2, up = 2)
            0x251C -> seg(up = 1, down = 1, right = 1)
            0x2523 -> seg(up = 2, down = 2, right = 2)
            0x2524 -> seg(up = 1, down = 1, left = 1)
            0x252B -> seg(up = 2, down = 2, left = 2)
            0x252C -> seg(left = 1, right = 1, down = 1)
            0x2533 -> seg(left = 2, right = 2, down = 2)
            0x2534 -> seg(left = 1, right = 1, up = 1)
            0x253B -> seg(left = 2, right = 2, up = 2)
            0x253C -> seg(left = 1, right = 1, up = 1, down = 1)
            0x254B -> seg(left = 2, right = 2, up = 2, down = 2)
            0x2550 -> {
                val off = h * 0.14f
                drawLine(color, Offset(x, cy - off), Offset(x + w + 0.5f, cy - off), strokeWidth = thin)
                drawLine(color, Offset(x, cy + off), Offset(x + w + 0.5f, cy + off), strokeWidth = thin)
            }
            0x2551 -> {
                val off = w * 0.16f
                drawLine(color, Offset(cx - off, y), Offset(cx - off, y + h + 0.5f), strokeWidth = thin)
                drawLine(color, Offset(cx + off, y), Offset(cx + off, y + h + 0.5f), strokeWidth = thin)
            }
            0x2554 -> seg(right = 2, down = 2)
            0x2557 -> seg(left = 2, down = 2)
            0x255A -> seg(right = 2, up = 2)
            0x255D -> seg(left = 2, up = 2)
            0x2574 -> seg(left = 1)
            0x2575 -> seg(up = 1)
            0x2576 -> seg(right = 1)
            0x2577 -> seg(down = 1)
            0x2578 -> seg(left = 2)
            0x2579 -> seg(up = 2)
            0x257A -> seg(right = 2)
            0x257B -> seg(down = 2)
            else -> seg(left = 1, right = 1)
        }
        return true
    }

    if (code in 0x2800..0x28FF) {
        val bits = code - 0x2800
        if (bits == 0) return true
        val r = (w * 0.11f).coerceIn(1.0f, 2.4f)
        val colX0 = x + w * 0.30f
        val colX1 = x + w * 0.70f
        val rowY = floatArrayOf(y + h * 0.20f, y + h * 0.40f, y + h * 0.60f, y + h * 0.80f)
        val dotBits = intArrayOf(0x01, 0x02, 0x04, 0x40, 0x08, 0x10, 0x20, 0x80)
        for (i in 0 until 4) {
            if ((bits and dotBits[i]) != 0) drawCircle(color, r, Offset(colX0, rowY[i]))
            if ((bits and dotBits[i + 4]) != 0) drawCircle(color, r, Offset(colX1, rowY[i]))
        }
        return true
    }
    return false
}
