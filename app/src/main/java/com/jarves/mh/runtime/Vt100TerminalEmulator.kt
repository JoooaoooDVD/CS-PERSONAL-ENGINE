package com.jarves.mh.runtime

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction

@Immutable
class TerminalRenderRow(
    val chars: CharArray,
    val fg: IntArray,
    val bg: IntArray,
    val flags: ByteArray,
    val length: Int = chars.size,
)

@Immutable
data class TerminalScreenSnapshot(
    val renderRows: List<TerminalRenderRow> = emptyList(),
    val styledLines: List<AnnotatedString> = emptyList(),
    val plainText: String = "",
    val scrollbackCount: Int = 0,
    val cursorScreenRow: Int = 0,
    val cursorCol: Int = 0,
    val cursorVisible: Boolean = true,
    val cursorStyle: Int = 1, // 0/1/2 block, 3/4 underline, 5/6 bar
    val bracketedPaste: Boolean = false,
    val alternateScreen: Boolean = false,
    val synchronizedUpdate: Boolean = false,
    val windowTitle: String = "",
    val rows: Int = 32,
    val cols: Int = 112,
    val revision: Long = 0L,
)

/**
 * Desktop-grade VT100 / VT520 / Xterm-256Color / ITU-T.416 TrueColor terminal emulator.
 *
 * Designed specifically so modern Go / BubbleTea / LipGloss / Rust / Node TUIs
 * (`opencode`, `claude`, `codex`, `gemini`, `nvim`, `lazygit`, `htop`, `tmux`)
 * render with 100% pixel-exact desktop Alacritty/Kitty fidelity on Android.
 */
class Vt100TerminalEmulator(
    initialRows: Int = 32,
    initialCols: Int = 112,
    private val maxScrollback: Int = 1500,
    var onReplyToPty: ((String) -> Unit)? = null,
) {
    private class CellLine(capacity: Int) {
        var chars = CharArray(capacity) { ' ' }
        var fg = IntArray(capacity) { DEFAULT_FG }
        var bg = IntArray(capacity) { DEFAULT_BG }
        var flags = ByteArray(capacity)
        var dirty = true
        var cachedStyled: AnnotatedString? = null
        var cachedRenderRow: TerminalRenderRow? = null

        fun ensureWidth(cols: Int) {
            if (cols <= chars.size) return
            val oldSize = chars.size
            chars = chars.copyOf(cols).also { arr ->
                for (i in oldSize until cols) arr[i] = ' '
            }
            fg = fg.copyOf(cols).also { arr ->
                for (i in oldSize until cols) arr[i] = DEFAULT_FG
            }
            bg = bg.copyOf(cols).also { arr ->
                for (i in oldSize until cols) arr[i] = DEFAULT_BG
            }
            flags = flags.copyOf(cols)
            invalidate()
        }

        fun clearRange(from: Int, toExclusive: Int, fillBg: Int, fillFg: Int = DEFAULT_FG) {
            val start = from.coerceAtLeast(0)
            val end = toExclusive.coerceAtMost(chars.size)
            for (i in start until end) {
                chars[i] = ' '
                fg[i] = fillFg
                bg[i] = fillBg
                flags[i] = 0
            }
            invalidate()
        }

        fun invalidate() {
            dirty = true
            cachedStyled = null
            cachedRenderRow = null
        }

        fun toRenderRow(cols: Int): TerminalRenderRow {
            val cached = cachedRenderRow
            if (!dirty && cached != null && cached.length == cols) return cached
            val len = cols.coerceAtLeast(1)
            ensureWidth(len)
            val snap = TerminalRenderRow(
                chars = chars.copyOf(len),
                fg = fg.copyOf(len),
                bg = bg.copyOf(len),
                flags = flags.copyOf(len),
                length = len,
            )
            cachedRenderRow = snap
            return snap
        }
    }

    var rows: Int = initialRows.coerceIn(10, 240)
        private set
    var cols: Int = initialCols.coerceIn(20, 320)
        private set

    private var primaryGrid = Array(rows) { CellLine(cols) }
    private var altGrid = Array(rows) { CellLine(cols) }
    private val scrollback = ArrayDeque<CellLine>(maxScrollback)

    private var inAltScreen = false
    private var cursorRow = 0
    private var cursorCol = 0
    private var savedRow = 0
    private var savedCol = 0
    private var altSavedRow = 0
    private var altSavedCol = 0

    private var scrollTop = 0
    private var scrollBottom = rows - 1

    private var curFg = DEFAULT_FG
    private var curBg = DEFAULT_BG
    private var curFlags: Int = 0

    private var cursorVisible = true
    private var cursorStyle = 1
    private var bracketedPaste = false
    private var synchronizedOutput = false
    private var wrapPending = false
    private var lastGraphicChar: Char = ' '
    private var windowTitle = ""
    private var revision = 0L

    val isSynchronizedOutputActive: Boolean
        @Synchronized get() = synchronizedOutput

    private val utf8Decoder = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE)
    private var pendingBytes = ByteArray(8)
    private var pendingByteCount = 0

    private var parserState = STATE_NORMAL
    private val csiBuffer = StringBuilder(96)
    private val oscBuffer = StringBuilder(256)
    private var oscEscSeen = false

    private fun activeGrid(): Array<CellLine> = if (inAltScreen) altGrid else primaryGrid

    @Synchronized
    fun resize(newRows: Int, newCols: Int) {
        val targetRows = newRows.coerceIn(10, 240)
        val targetCols = newCols.coerceIn(20, 320)
        if (targetRows == rows && targetCols == cols) return

        fun resizeGrid(old: Array<CellLine>): Array<CellLine> {
            return Array(targetRows) { r ->
                if (r < old.size) {
                    old[r].apply {
                        ensureWidth(targetCols)
                        invalidate()
                    }
                } else {
                    CellLine(targetCols)
                }
            }
        }
        primaryGrid = resizeGrid(primaryGrid)
        altGrid = resizeGrid(altGrid)
        rows = targetRows
        cols = targetCols
        scrollTop = 0
        scrollBottom = rows - 1
        cursorRow = cursorRow.coerceIn(0, rows - 1)
        cursorCol = cursorCol.coerceIn(0, cols - 1)
        wrapPending = false
        revision++
    }

    @Synchronized
    fun clearAll() {
        scrollback.clear()
        val grid = activeGrid()
        for (r in 0 until rows) grid[r].clearRange(0, cols, DEFAULT_BG)
        cursorRow = 0
        cursorCol = 0
        wrapPending = false
        revision++
    }

    @Synchronized
    fun feedBytes(bytes: ByteArray, length: Int) {
        if (length <= 0) return
        val combined: ByteBuffer = if (pendingByteCount > 0) {
            val merged = ByteArray(pendingByteCount + length)
            System.arraycopy(pendingBytes, 0, merged, 0, pendingByteCount)
            System.arraycopy(bytes, 0, merged, pendingByteCount, length)
            pendingByteCount = 0
            ByteBuffer.wrap(merged)
        } else {
            ByteBuffer.wrap(bytes, 0, length)
        }
        val chars = CharBuffer.allocate(combined.remaining() + 4)
        utf8Decoder.decode(combined, chars, false)
        if (combined.hasRemaining()) {
            val rem = combined.remaining()
            if (rem <= pendingBytes.size) {
                combined.get(pendingBytes, 0, rem)
                pendingByteCount = rem
            }
        }
        chars.flip()
        for (i in 0 until chars.limit()) {
            processChar(chars.get(i))
        }
        revision++
    }

    @Synchronized
    fun feedText(text: String) {
        if (text.isEmpty()) return
        for (i in 0 until text.length) processChar(text[i])
        revision++
    }

    private fun processChar(ch: Char) {
        when (parserState) {
            STATE_NORMAL -> when {
                ch == '\u001B' -> parserState = STATE_ESC
                ch == '\r' -> {
                    cursorCol = 0
                    wrapPending = false
                }
                ch == '\n' || ch == '\u000B' || ch == '\u000C' -> {
                    wrapPending = false
                    lineFeed()
                }
                ch == '\b' -> {
                    wrapPending = false
                    if (cursorCol > 0) cursorCol--
                }
                ch == '\t' -> {
                    wrapPending = false
                    val nextTab = ((cursorCol / 8) + 1) * 8
                    cursorCol = nextTab.coerceAtMost(cols - 1)
                }
                ch == '\u0007' || ch == '\u000E' || ch == '\u000F' -> Unit
                Character.isLowSurrogate(ch) -> Unit // Ignore trailing surrogate half; BMP/plane handled safely
                Character.getType(ch) == Character.NON_SPACING_MARK.toInt() -> Unit
                ch >= ' ' -> {
                    lastGraphicChar = ch
                    putChar(ch)
                }
            }
            STATE_ESC -> when (ch) {
                '[' -> {
                    csiBuffer.setLength(0)
                    parserState = STATE_CSI
                }
                ']' -> {
                    oscBuffer.setLength(0)
                    oscEscSeen = false
                    parserState = STATE_OSC
                }
                'P', 'X', '^', '_' -> {
                    // DCS / SOS / PM / APC string: consume until ST (ESC \ or BEL)
                    oscBuffer.setLength(0)
                    oscEscSeen = false
                    parserState = STATE_DCS
                }
                '(', ')', '*', '+' -> parserState = STATE_CHARSET
                '7' -> {
                    savedRow = cursorRow
                    savedCol = cursorCol
                    parserState = STATE_NORMAL
                }
                '8' -> {
                    cursorRow = savedRow.coerceIn(0, rows - 1)
                    cursorCol = savedCol.coerceIn(0, cols - 1)
                    wrapPending = false
                    parserState = STATE_NORMAL
                }
                'M' -> {
                    if (cursorRow == scrollTop) scrollRegionDown(1)
                    else if (cursorRow > 0) cursorRow--
                    wrapPending = false
                    parserState = STATE_NORMAL
                }
                'D' -> {
                    lineFeed()
                    wrapPending = false
                    parserState = STATE_NORMAL
                }
                'E' -> {
                    cursorCol = 0
                    lineFeed()
                    wrapPending = false
                    parserState = STATE_NORMAL
                }
                'c' -> {
                    resetEmulator()
                    parserState = STATE_NORMAL
                }
                else -> parserState = STATE_NORMAL
            }
            STATE_CHARSET -> parserState = STATE_NORMAL
            STATE_DCS -> {
                if (ch == '\u0007') {
                    parserState = STATE_NORMAL
                } else if (oscEscSeen) {
                    oscEscSeen = false
                    if (ch == '\\') parserState = STATE_NORMAL
                } else if (ch == '\u001B') {
                    oscEscSeen = true
                }
            }
            STATE_OSC -> {
                if (ch == '\u0007') {
                    finishOsc()
                    parserState = STATE_NORMAL
                } else if (oscEscSeen) {
                    oscEscSeen = false
                    if (ch == '\\') {
                        finishOsc()
                        parserState = STATE_NORMAL
                    }
                } else if (ch == '\u001B') {
                    oscEscSeen = true
                } else if (oscBuffer.length < 512) {
                    oscBuffer.append(ch)
                }
            }
            STATE_CSI -> {
                if (ch in '\u0040'..'\u007E') {
                    handleCsi(csiBuffer.toString(), ch)
                    parserState = STATE_NORMAL
                } else if (csiBuffer.length < 128) {
                    csiBuffer.append(ch)
                } else {
                    parserState = STATE_NORMAL
                }
            }
        }
    }

    private fun resetEmulator() {
        curFg = DEFAULT_FG
        curBg = DEFAULT_BG
        curFlags = 0
        cursorVisible = true
        cursorStyle = 1
        bracketedPaste = false
        synchronizedOutput = false
        inAltScreen = false
        scrollTop = 0
        scrollBottom = rows - 1
        for (r in 0 until rows) primaryGrid[r].clearRange(0, cols, DEFAULT_BG)
        for (r in 0 until rows) altGrid[r].clearRange(0, cols, DEFAULT_BG)
        cursorRow = 0
        cursorCol = 0
        wrapPending = false
    }

    private fun finishOsc() {
        val raw = oscBuffer.toString()
        when {
            raw.startsWith("0;") || raw.startsWith("2;") -> {
                windowTitle = raw.substring(2).take(80)
            }
            raw == "10;?" -> {
                // Terminal foreground color query (Xterm / LipGloss / Ultraviolet)
                onReplyToPty?.invoke("\u001B]10;rgb:e6e6/eded/f3f3\u001B\\")
            }
            raw == "11;?" -> {
                // Terminal background color query -> instant dark theme detection for OpenCode / Neovim!
                onReplyToPty?.invoke("\u001B]11;rgb:0606/0808/0c0c\u001B\\")
            }
        }
    }

    private fun putChar(ch: Char) {
        if (wrapPending) {
            cursorCol = 0
            lineFeed()
            wrapPending = false
        }
        val grid = activeGrid()
        val line = grid[cursorRow]
        line.ensureWidth(cols)
        val inverse = (curFlags and FLAG_INVERSE) != 0
        val effectiveBg = if (inverse) {
            if (curFg == DEFAULT_FG) 0xFFE6EDF3.toInt() else curFg
        } else {
            curBg
        }
        val effectiveFg = if (inverse) {
            if (curBg == DEFAULT_BG) 0xFF06080C.toInt() else curBg
        } else {
            curFg
        }
        line.chars[cursorCol] = ch
        line.fg[cursorCol] = effectiveFg
        line.bg[cursorCol] = effectiveBg
        line.flags[cursorCol] = (curFlags and FLAG_INVERSE.inv()).toByte()
        line.invalidate()
        if (cursorCol >= cols - 1) {
            wrapPending = true
        } else {
            cursorCol++
        }
    }

    private fun lineFeed() {
        if (cursorRow == scrollBottom) {
            scrollRegionUp(1)
        } else if (cursorRow < rows - 1) {
            cursorRow++
        }
    }

    private fun scrollRegionUp(count: Int) {
        val grid = activeGrid()
        repeat(count.coerceIn(1, rows)) {
            val topLine = grid[scrollTop]
            if (!inAltScreen && scrollTop == 0) {
                if (scrollback.size >= maxScrollback) scrollback.removeFirst()
                scrollback.addLast(topLine)
            }
            for (r in scrollTop until scrollBottom) {
                grid[r] = grid[r + 1]
            }
            grid[scrollBottom] = CellLine(cols).apply {
                if (curBg != DEFAULT_BG) clearRange(0, cols, curBg)
            }
        }
    }

    private fun scrollRegionDown(count: Int) {
        val grid = activeGrid()
        repeat(count.coerceIn(1, rows)) {
            for (r in scrollBottom downTo scrollTop + 1) {
                grid[r] = grid[r - 1]
            }
            grid[scrollTop] = CellLine(cols).apply {
                if (curBg != DEFAULT_BG) clearRange(0, cols, curBg)
            }
        }
    }

    private fun handleCsi(paramsRaw: String, finalChar: Char) {
        // Handle intermediate space sequences like DECSCUSR (`CSI Ps SP q`)
        if (paramsRaw.endsWith(" ") && finalChar == 'q') {
            val styleCode = paramsRaw.dropLast(1).trim().toIntOrNull() ?: 1
            cursorStyle = styleCode.coerceIn(0, 6)
            return
        }
        // Handle DECRQM (`CSI ? Pd $ p`) mode queries used by BubbleTea / OpenCode / Kitty
        if (paramsRaw.endsWith("$") && finalChar == 'p') {
            val body = paramsRaw.dropLast(1)
            if (body.startsWith("?")) {
                val mode = body.substring(1).toIntOrNull() ?: 0
                val status = when (mode) {
                    2026 -> if (synchronizedOutput) 1 else 2
                    2004 -> if (bracketedPaste) 1 else 2
                    1049 -> if (inAltScreen) 1 else 2
                    25 -> if (cursorVisible) 1 else 2
                    else -> 0
                }
                onReplyToPty?.invoke("\u001B[?$mode;$status\$y")
            }
            return
        }

        val privateMode = paramsRaw.startsWith("?")
        val secondaryDa = paramsRaw.startsWith(">")
        if (secondaryDa && finalChar == 'c') {
            onReplyToPty?.invoke("\u001B[>1;10;0c")
            return
        }
        // Ignore Kitty keyboard protocol progressive push/pop (`>u`, `<u`, `=u`) cleanly
        if ((paramsRaw.startsWith(">") || paramsRaw.startsWith("<") || paramsRaw.startsWith("=")) && finalChar == 'u') {
            return
        }

        if (finalChar == 'm' && !privateMode) {
            applyModernSgr(paramsRaw)
            return
        }

        val clean = if (privateMode) paramsRaw.substring(1) else paramsRaw.trimStart('>', '!', '=')
        val params = if (clean.isEmpty()) emptyList() else clean.split(';').map { it.substringBefore(':').toIntOrNull() ?: 0 }
        fun p(idx: Int, default: Int = 1): Int = params.getOrNull(idx)?.takeIf { it > 0 } ?: default
        fun p0(idx: Int, default: Int = 0): Int = params.getOrNull(idx) ?: default

        if (privateMode) {
            val set = finalChar == 'h'
            if (finalChar == 'h' || finalChar == 'l') {
                for (code in params) {
                    when (code) {
                        25 -> cursorVisible = set
                        2004 -> bracketedPaste = set
                        2026 -> synchronizedOutput = set
                        47, 1047, 1049 -> {
                            if (set && !inAltScreen) {
                                altSavedRow = cursorRow
                                altSavedCol = cursorCol
                                inAltScreen = true
                                for (r in 0 until rows) altGrid[r].clearRange(0, cols, DEFAULT_BG)
                                cursorRow = 0
                                cursorCol = 0
                                wrapPending = false
                            } else if (!set && inAltScreen) {
                                inAltScreen = false
                                cursorRow = altSavedRow.coerceIn(0, rows - 1)
                                cursorCol = altSavedCol.coerceIn(0, cols - 1)
                                wrapPending = false
                            }
                        }
                    }
                }
            }
            return
        }

        val grid = activeGrid()
        when (finalChar) {
            'A' -> { cursorRow = (cursorRow - p(0)).coerceAtLeast(0); wrapPending = false }
            'B' -> { cursorRow = (cursorRow + p(0)).coerceAtMost(rows - 1); wrapPending = false }
            'C' -> { cursorCol = (cursorCol + p(0)).coerceAtMost(cols - 1); wrapPending = false }
            'D' -> { cursorCol = (cursorCol - p(0)).coerceAtLeast(0); wrapPending = false }
            'E' -> { cursorRow = (cursorRow + p(0)).coerceAtMost(rows - 1); cursorCol = 0; wrapPending = false }
            'F' -> { cursorRow = (cursorRow - p(0)).coerceAtLeast(0); cursorCol = 0; wrapPending = false }
            'G', '`' -> { cursorCol = (p(0) - 1).coerceIn(0, cols - 1); wrapPending = false }
            'd' -> { cursorRow = (p(0) - 1).coerceIn(0, rows - 1); wrapPending = false }
            'H', 'f' -> {
                cursorRow = (p(0, 1) - 1).coerceIn(0, rows - 1)
                cursorCol = (p(1, 1) - 1).coerceIn(0, cols - 1)
                wrapPending = false
            }
            'b' -> {
                // REP: Repeat preceding graphic character p(0) times! Crucial for BubbleTea / OpenCode box fills!
                val count = p(0).coerceIn(1, cols * 2)
                repeat(count) { putChar(lastGraphicChar) }
            }
            'n' -> {
                when (p0(0)) {
                    5 -> onReplyToPty?.invoke("\u001B[0n")
                    6 -> onReplyToPty?.invoke("\u001B[${cursorRow + 1};${cursorCol + 1}R")
                }
            }
            'c' -> {
                onReplyToPty?.invoke("\u001B[?62;22c")
            }
            'J' -> {
                when (p0(0, 0)) {
                    0 -> {
                        grid[cursorRow].clearRange(cursorCol, cols, curBg)
                        for (r in cursorRow + 1 until rows) grid[r].clearRange(0, cols, curBg)
                    }
                    1 -> {
                        for (r in 0 until cursorRow) grid[r].clearRange(0, cols, curBg)
                        grid[cursorRow].clearRange(0, cursorCol + 1, curBg)
                    }
                    2, 3 -> {
                        if (p0(0) == 3 && !inAltScreen) scrollback.clear()
                        for (r in 0 until rows) grid[r].clearRange(0, cols, curBg)
                    }
                }
            }
            'K' -> {
                when (p0(0, 0)) {
                    0 -> grid[cursorRow].clearRange(cursorCol, cols, curBg)
                    1 -> grid[cursorRow].clearRange(0, cursorCol + 1, curBg)
                    2 -> grid[cursorRow].clearRange(0, cols, curBg)
                }
            }
            'L' -> {
                val count = p(0).coerceAtMost(scrollBottom - cursorRow + 1)
                for (r in scrollBottom downTo cursorRow + count) {
                    grid[r] = grid[r - count]
                }
                for (r in cursorRow until (cursorRow + count).coerceAtMost(rows)) {
                    grid[r] = CellLine(cols).apply { if (curBg != DEFAULT_BG) clearRange(0, cols, curBg) }
                }
            }
            'M' -> {
                val count = p(0).coerceAtMost(scrollBottom - cursorRow + 1)
                for (r in cursorRow..scrollBottom - count) {
                    grid[r] = grid[r + count]
                }
                for (r in (scrollBottom - count + 1)..scrollBottom) {
                    if (r in 0 until rows) {
                        grid[r] = CellLine(cols).apply { if (curBg != DEFAULT_BG) clearRange(0, cols, curBg) }
                    }
                }
            }
            'P' -> {
                val line = grid[cursorRow]
                line.ensureWidth(cols)
                val count = p(0).coerceAtMost(cols - cursorCol)
                for (c in cursorCol until cols - count) {
                    line.chars[c] = line.chars[c + count]
                    line.fg[c] = line.fg[c + count]
                    line.bg[c] = line.bg[c + count]
                    line.flags[c] = line.flags[c + count]
                }
                line.clearRange(cols - count, cols, curBg)
            }
            '@' -> {
                val line = grid[cursorRow]
                line.ensureWidth(cols)
                val count = p(0).coerceAtMost(cols - cursorCol)
                for (c in cols - 1 downTo cursorCol + count) {
                    line.chars[c] = line.chars[c - count]
                    line.fg[c] = line.fg[c - count]
                    line.bg[c] = line.bg[c - count]
                    line.flags[c] = line.flags[c - count]
                }
                line.clearRange(cursorCol, cursorCol + count, curBg)
            }
            'X' -> {
                grid[cursorRow].ensureWidth(cols)
                grid[cursorRow].clearRange(cursorCol, (cursorCol + p(0)).coerceAtMost(cols), curBg)
            }
            'S' -> scrollRegionUp(p(0))
            'T' -> scrollRegionDown(p(0))
            'r' -> {
                val top = (p(0, 1) - 1).coerceIn(0, rows - 1)
                val bottom = (p(1, rows) - 1).coerceIn(top, rows - 1)
                scrollTop = top
                scrollBottom = bottom
                cursorRow = 0
                cursorCol = 0
                wrapPending = false
            }
            's' -> { savedRow = cursorRow; savedCol = cursorCol }
            'u' -> {
                cursorRow = savedRow.coerceIn(0, rows - 1)
                cursorCol = savedCol.coerceIn(0, cols - 1)
                wrapPending = false
            }
        }
    }

    /**
     * Full ITU-T T.416 & Xterm SGR parser supporting BOTH semicolon (`38;2;R;G;B`) AND
     * modern colon subparameters (`38:2::R:G:B`, `38:2:R:G:B`, `38:5:N`, `4:3`, `58:2:...`)!
     */
    private fun applyModernSgr(paramsRaw: String) {
        if (paramsRaw.isEmpty()) {
            curFg = DEFAULT_FG
            curBg = DEFAULT_BG
            curFlags = 0
            return
        }
        val groups = paramsRaw.split(';')
        var i = 0
        while (i < groups.size) {
            val token = groups[i]
            if (':' in token) {
                val sub = token.split(':')
                val head = sub.getOrNull(0)?.toIntOrNull() ?: 0
                when (head) {
                    0 -> { curFg = DEFAULT_FG; curBg = DEFAULT_BG; curFlags = 0 }
                    4 -> {
                        val uStyle = sub.getOrNull(1)?.toIntOrNull() ?: 1
                        curFlags = if (uStyle == 0) curFlags and FLAG_UNDERLINE.inv() else curFlags or FLAG_UNDERLINE
                    }
                    38, 48 -> {
                        val isFg = head == 38
                        val mode = sub.getOrNull(1)?.toIntOrNull() ?: 0
                        if (mode == 5 && sub.size >= 3) {
                            val idx = sub[2].toIntOrNull()?.coerceIn(0, 255) ?: 0
                            val color = xterm256ToArgb(idx)
                            if (isFg) curFg = color else curBg = color
                        } else if (mode == 2 && sub.size >= 5) {
                            // 5 parts = 38:2:R:G:B; 6+ parts = 38:2:CS:R:G:B
                            val offset = if (sub.size >= 6) 3 else 2
                            val r = sub.getOrNull(offset)?.toIntOrNull()?.coerceIn(0, 255) ?: 0
                            val g = sub.getOrNull(offset + 1)?.toIntOrNull()?.coerceIn(0, 255) ?: 0
                            val b = sub.getOrNull(offset + 2)?.toIntOrNull()?.coerceIn(0, 255) ?: 0
                            val color = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                            if (isFg) curFg = color else curBg = color
                        }
                    }
                    58, 59 -> Unit // Underline color / reset — safely ignored without corrupting fg/bg
                }
                i++
                continue
            }

            when (val code = token.toIntOrNull() ?: 0) {
                0 -> { curFg = DEFAULT_FG; curBg = DEFAULT_BG; curFlags = 0 }
                1 -> curFlags = curFlags or FLAG_BOLD
                2 -> curFlags = curFlags or FLAG_DIM
                3 -> curFlags = curFlags or FLAG_ITALIC
                4 -> curFlags = curFlags or FLAG_UNDERLINE
                7 -> curFlags = curFlags or FLAG_INVERSE
                22 -> curFlags = curFlags and (FLAG_BOLD or FLAG_DIM).inv()
                23 -> curFlags = curFlags and FLAG_ITALIC.inv()
                24 -> curFlags = curFlags and FLAG_UNDERLINE.inv()
                27 -> curFlags = curFlags and FLAG_INVERSE.inv()
                in 30..37 -> curFg = ANSI_16_COLORS[code - 30]
                39 -> curFg = DEFAULT_FG
                in 40..47 -> curBg = ANSI_16_COLORS[code - 40]
                49 -> curBg = DEFAULT_BG
                59 -> Unit
                in 90..97 -> curFg = ANSI_16_COLORS[code - 90 + 8]
                in 100..107 -> curBg = ANSI_16_COLORS[code - 100 + 8]
                38, 48, 58 -> {
                    val isFg = code == 38
                    val isBg = code == 48
                    val mode = groups.getOrNull(i + 1)?.toIntOrNull() ?: 0
                    if (mode == 5 && i + 2 < groups.size) {
                        val color = xterm256ToArgb((groups[i + 2].toIntOrNull() ?: 0).coerceIn(0, 255))
                        if (isFg) curFg = color else if (isBg) curBg = color
                        i += 2
                    } else if (mode == 2 && i + 4 < groups.size) {
                        val r = (groups[i + 2].toIntOrNull() ?: 0).coerceIn(0, 255)
                        val g = (groups[i + 3].toIntOrNull() ?: 0).coerceIn(0, 255)
                        val b = (groups[i + 4].toIntOrNull() ?: 0).coerceIn(0, 255)
                        val color = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                        if (isFg) curFg = color else if (isBg) curBg = color
                        i += 4
                    }
                }
            }
            i++
        }
    }

    @Synchronized
    fun snapshot(showBlockCursor: Boolean = true): TerminalScreenSnapshot {
        val grid = activeGrid()
        val lastActiveScreenRow = if (inAltScreen) {
            rows - 1
        } else {
            var last = cursorRow
            for (r in rows - 1 downTo 0) {
                val line = grid[r]
                if (line.chars.any { it != ' ' } || line.bg.any { it != DEFAULT_BG }) {
                    last = maxOf(last, r)
                    break
                }
            }
            last
        }

        val totalRowsCount = (if (inAltScreen) 0 else scrollback.size) + lastActiveScreenRow + 1
        val renderRows = ArrayList<TerminalRenderRow>(totalRowsCount)
        val styled = ArrayList<AnnotatedString>(totalRowsCount)
        val plainBuilder = StringBuilder()

        if (!inAltScreen) {
            for (sbLine in scrollback) {
                renderRows.add(sbLine.toRenderRow(cols))
                val rendered = renderLine(sbLine, cursorColOnLine = -1)
                styled.add(rendered)
                plainBuilder.append(rendered.text).append('\n')
            }
        }
        val sbOffset = if (inAltScreen) 0 else scrollback.size
        for (r in 0..lastActiveScreenRow) {
            renderRows.add(grid[r].toRenderRow(cols))
            val activeCursorCol = if (showBlockCursor && cursorVisible && r == cursorRow) {
                cursorCol.coerceIn(0, cols - 1)
            } else {
                -1
            }
            val rendered = renderLine(grid[r], cursorColOnLine = activeCursorCol)
            styled.add(rendered)
            plainBuilder.append(rendered.text)
            if (r < lastActiveScreenRow) plainBuilder.append('\n')
        }

        return TerminalScreenSnapshot(
            renderRows = renderRows,
            styledLines = styled,
            plainText = plainBuilder.toString(),
            scrollbackCount = sbOffset,
            cursorScreenRow = cursorRow,
            cursorCol = cursorCol,
            cursorVisible = cursorVisible,
            cursorStyle = cursorStyle,
            bracketedPaste = bracketedPaste,
            alternateScreen = inAltScreen,
            synchronizedUpdate = synchronizedOutput,
            windowTitle = windowTitle,
            rows = rows,
            cols = cols,
            revision = revision,
        )
    }

    private fun renderLine(line: CellLine, cursorColOnLine: Int): AnnotatedString {
        if (cursorColOnLine < 0 && !line.dirty && line.cachedStyled != null) {
            return line.cachedStyled!!
        }
        val limit = line.chars.size.coerceAtMost(cols)
        var effectiveEnd = 0
        for (idx in limit - 1 downTo 0) {
            if (line.chars[idx] != ' ' || line.bg[idx] != DEFAULT_BG || idx == cursorColOnLine) {
                effectiveEnd = idx + 1
                break
            }
        }
        if (cursorColOnLine >= effectiveEnd) effectiveEnd = cursorColOnLine + 1
        if (effectiveEnd == 0) {
            val empty = AnnotatedString("")
            if (cursorColOnLine < 0) {
                line.cachedStyled = empty
                line.dirty = false
            }
            return empty
        }

        val built = buildAnnotatedString {
            var runStart = 0
            while (runStart < effectiveEnd) {
                val isCursor = runStart == cursorColOnLine
                val fg = if (isCursor) CURSOR_FG else line.fg[runStart]
                val bg = if (isCursor) CURSOR_BG else line.bg[runStart]
                val fl = line.flags[runStart].toInt()
                var runEnd = runStart + 1
                if (!isCursor) {
                    while (runEnd < effectiveEnd &&
                        runEnd != cursorColOnLine &&
                        line.fg[runEnd] == fg &&
                        line.bg[runEnd] == bg &&
                        line.flags[runEnd].toInt() == fl
                    ) {
                        runEnd++
                    }
                }
                val bold = (fl and FLAG_BOLD) != 0
                val dim = (fl and FLAG_DIM) != 0
                val italic = (fl and FLAG_ITALIC) != 0
                val underline = (fl and FLAG_UNDERLINE) != 0
                val fgColor = Color(fg).let { if (dim) it.copy(alpha = 0.65f) else it }
                val bgColor = if (bg == DEFAULT_BG && !isCursor) Color.Unspecified else Color(bg)
                pushStyle(
                    SpanStyle(
                        color = fgColor,
                        background = bgColor,
                        fontWeight = if (bold || isCursor) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = if (underline) TextDecoration.Underline else TextDecoration.None,
                    ),
                )
                for (k in runStart until runEnd) {
                    val ch = if (k < line.chars.size) line.chars[k] else ' '
                    append(if (ch < ' ') ' ' else ch)
                }
                pop()
                runStart = runEnd
            }
        }
        if (cursorColOnLine < 0) {
            line.cachedStyled = built
            line.dirty = false
        }
        return built
    }

    companion object {
        private const val STATE_NORMAL = 0
        private const val STATE_ESC = 1
        private const val STATE_CSI = 2
        private const val STATE_OSC = 3
        private const val STATE_CHARSET = 4
        private const val STATE_DCS = 5

        const val FLAG_BOLD = 1
        const val FLAG_DIM = 2
        const val FLAG_ITALIC = 4
        const val FLAG_UNDERLINE = 8
        const val FLAG_INVERSE = 16

        const val DEFAULT_FG = 0xFFE6EDF3.toInt()
        const val DEFAULT_BG = 0x00000000
        const val CURSOR_FG = 0xFF06080C.toInt()
        const val CURSOR_BG = 0xFF34D399.toInt()

        private val ANSI_16_COLORS = intArrayOf(
            0xFF16181D.toInt(), // 0 Black
            0xFFFF6B6B.toInt(), // 1 Red
            0xFF34D399.toInt(), // 2 Green
            0xFFFBBF24.toInt(), // 3 Yellow
            0xFF60A5FA.toInt(), // 4 Blue
            0xFFC084FC.toInt(), // 5 Magenta
            0xFF38BDF8.toInt(), // 6 Cyan
            0xFFD1D5DB.toInt(), // 7 White
            0xFF6B7280.toInt(), // 8 Bright Black (Gray)
            0xFFFF8787.toInt(), // 9 Bright Red
            0xFF6EE7B7.toInt(), // 10 Bright Green
            0xFFFDE047.toInt(), // 11 Bright Yellow
            0xFF93C5FD.toInt(), // 12 Bright Blue
            0xFFD8B4FE.toInt(), // 13 Bright Magenta
            0xFF7DD3FC.toInt(), // 14 Bright Cyan
            0xFFF9FAFB.toInt(), // 15 Bright White
        )

        internal fun xterm256ToArgb(index: Int): Int {
            if (index < 16) return ANSI_16_COLORS[index]
            if (index in 16..231) {
                val c = index - 16
                val levels = intArrayOf(0, 95, 135, 175, 215, 255)
                val r = levels[(c / 36) % 6]
                val g = levels[(c / 6) % 6]
                val b = levels[c % 6]
                return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
            val gray = 8 + (index - 232) * 10
            return (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
        }
    }
}
