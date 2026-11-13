package com.example.paleokeyboard

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.core.view.WindowCompat
import java.text.BreakIterator

/** Right-to-left mark: an invisible character that marks a line as right to left. */
internal const val RLM = '\u200F'

// Holding backspace: wait this long, then delete faster and faster down to the floor.
private const val REPEAT_START_DELAY = 400L
private const val REPEAT_FIRST_INTERVAL = 100L
private const val REPEAT_FASTEST_INTERVAL = 40L

class PhoenicianKeyboardService : InputMethodService() {

    private val handler = Handler(Looper.getMainLooper())
    private var keyboardView: View? = null
    private var isSymbols = false
    private var repeatInterval = REPEAT_FIRST_INTERVAL

    private val phoenicianMap = mapOf(
        R.id.key_apostrophe to "'",
        R.id.key_dash to "-",
        R.id.key_qof to "𐤒",
        R.id.key_resh to "𐤓",
        R.id.key_alef to "𐤀",
        R.id.key_tet to "𐤈",
        R.id.key_waw to "𐤅",
        R.id.key_peh to "𐤐",

        R.id.key_shin to "𐤔",
        R.id.key_dalet to "𐤃",
        R.id.key_gimel to "𐤂",
        R.id.key_kaf to "𐤊",
        R.id.key_ayin to "𐤏",
        R.id.key_yod to "𐤉",
        R.id.key_het to "𐤇",
        R.id.key_lamed to "𐤋",

        R.id.key_zayin to "𐤆",
        R.id.key_sameh to "𐤎",
        R.id.key_bet to "𐤁",
        R.id.key_he to "𐤄",
        R.id.key_nun to "𐤍",
        R.id.key_mem to "𐤌",
        R.id.key_sadiq to "𐤑",
        R.id.key_taf to "𐤕",

        R.id.key_comma to ",",
        R.id.key_space to " ",
        R.id.key_dot to "."
    )

    private val symbolsMap = mapOf(
        R.id.key_apostrophe to "!",
        R.id.key_dash to "?",
        R.id.key_qof to "@",
        R.id.key_resh to "#",
        R.id.key_alef to "$",
        R.id.key_tet to "%",
        R.id.key_waw to "^",
        R.id.key_peh to "&",

        R.id.key_shin to "*",
        R.id.key_dalet to "(",
        R.id.key_gimel to ")",
        R.id.key_kaf to "_",
        R.id.key_ayin to "+",
        R.id.key_yod to "=",
        R.id.key_het to "/",
        R.id.key_lamed to "\\",

        R.id.key_zayin to "[",
        R.id.key_sameh to "]",
        R.id.key_bet to ":",
        R.id.key_he to ";",
        R.id.key_nun to "<",
        R.id.key_mem to ">",
        R.id.key_sadiq to "\"",
        R.id.key_taf to "'",

        R.id.key_comma to ",",
        R.id.key_space to " ",
        R.id.key_dot to "."
    )

    private val repeatDelete = object : Runnable {
        override fun run() {
            deleteBackward()
            handler.postDelayed(this, repeatInterval)
            repeatInterval = maxOf(REPEAT_FASTEST_INTERVAL, repeatInterval - 10)
        }
    }

    /**
     * Buzz the moment a key is touched rather than when the finger lifts. Returns false,
     * so the key still handles the touch and clicks as usual.
     */
    @SuppressLint("ClickableViewAccessibility")
    private val buzzOnPress = View.OnTouchListener { v, event ->
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        false
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    @SuppressLint("ClickableViewAccessibility", "InflateParams") // see buzzOnPress; IME views have no parent
    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        keyboardView = view

        for (id in phoenicianMap.keys) {
            view.key(id).setOnClickListener { type(id) }
        }
        // A long press already buzzes on its own, so these listeners don't.
        view.key(R.id.key_space).setOnLongClickListener { showKeyboardPicker(); true }
        view.key(R.id.key_symbols).setOnClickListener {
            isSymbols = !isSymbols
            showLabels()
        }
        view.key(R.id.key_enter).setOnClickListener { pressEnter() }
        view.key(R.id.key_globe).apply {
            setOnClickListener { switchKeyboard() }
            setOnLongClickListener { showKeyboardPicker(); true }
        }
        for (id in phoenicianMap.keys + listOf(R.id.key_symbols, R.id.key_enter, R.id.key_globe)) {
            view.key(id).setOnTouchListener(buzzOnPress)
        }
        setUpBackspace(view.key(R.id.key_backspace))

        // Keep the keys clear of the navigation bar (the keyboard draws behind it on Android 15+).
        val minBottomGap = (10 * resources.displayMetrics.density).toInt()
        view.setOnApplyWindowInsetsListener { v, insets ->
            val navBar = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.systemBars()).bottom
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetBottom
            }
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, if (navBar > 0) navBar else minBottomGap)
            insets
        }

        styleNavigationBar()
        refresh()
        return view
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // A new text field starts on letters; the same field being refreshed keeps its page.
        if (!restarting) isSymbols = false
        stopRepeat()
        refresh()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        stopRepeat()
        super.onFinishInputView(finishingInput)
    }

    override fun onWindowHidden() {
        stopRepeat()
        super.onWindowHidden()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    /** Brings every label up to date with the current page and text field. */
    private fun refresh() {
        val view = keyboardView ?: return
        showLabels()
        view.key(R.id.key_enter).text = enterLabel(currentInputEditorInfo)
        val offerSwitching = Build.VERSION.SDK_INT < Build.VERSION_CODES.P ||
            shouldOfferSwitchingToNextInputMethod()
        view.key(R.id.key_globe).visibility = if (offerSwitching) View.VISIBLE else View.GONE
    }

    private fun showLabels() {
        val view = keyboardView ?: return
        for ((id, text) in if (isSymbols) symbolsMap else phoenicianMap) {
            view.key(id).text = text
        }
        view.key(R.id.key_symbols).apply {
            text = if (isSymbols) "𐤀𐤁𐤂" else getString(R.string.symbols_key)
            contentDescription = getString(if (isSymbols) R.string.key_letters else R.string.key_symbols)
        }
    }

    private fun type(id: Int) {
        val ic = currentInputConnection ?: return
        val text = (if (isSymbols) symbolsMap else phoenicianMap).getValue(id)
        val markLine = !isSymbols && startsLine(ic.getTextBeforeCursor(1, 0))
        ic.commitText(if (markLine) "$RLM$text" else text, 1)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpBackspace(key: View) {
        // Delete on touch (not release) and repeat while held.
        key.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    v.playSoundEffect(SoundEffectConstants.CLICK)
                    deleteBackward()
                    handler.removeCallbacks(repeatDelete)
                    repeatInterval = REPEAT_FIRST_INTERVAL
                    handler.postDelayed(repeatDelete, REPEAT_START_DELAY)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> stopRepeat()
            }
            true
        }
        // TalkBack and switch access click the key instead of touching it.
        key.setOnClickListener { deleteBackward() }
    }

    private fun stopRepeat() {
        handler.removeCallbacks(repeatDelete)
        keyboardView?.findViewById<View>(R.id.key_backspace)?.isPressed = false
    }

    private fun deleteBackward() {
        val ic = currentInputConnection ?: return
        if (!ic.getSelectedText(0).isNullOrEmpty()) {
            ic.commitText("", 1)
            return
        }
        val before = ic.getTextBeforeCursor(32, 0)
        if (before.isNullOrEmpty()) {
            // Nothing visible to delete (or the app won't share its text): send a real
            // Delete key so the app can act on it, e.g. remove a chip or step back a box.
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        } else {
            ic.deleteSurroundingText(charsToDelete(before), 0)
        }
    }

    private fun pressEnter() {
        val ic = currentInputConnection ?: return
        val action = enterAction(currentInputEditorInfo)
        if (action == null) ic.commitText("\n", 1) else ic.performEditorAction(action)
    }

    private fun enterLabel(info: EditorInfo?): String = when (enterAction(info)) {
        EditorInfo.IME_ACTION_SEARCH -> "🔍"
        EditorInfo.IME_ACTION_SEND -> "➤"
        EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_NEXT -> "→"
        EditorInfo.IME_ACTION_DONE -> "✓"
        else -> "⏎"
    }

    private fun switchKeyboard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switchToNextInputMethod(false)
        } else {
            @Suppress("DEPRECATION")
            inputMethodManager.switchToNextInputMethod(window.window?.attributes?.token, false)
        }
    }

    private fun showKeyboardPicker() = inputMethodManager.showInputMethodPicker()

    private val inputMethodManager: InputMethodManager
        get() = getSystemService(InputMethodManager::class.java)

    /** Color the strip under the keyboard to match it, with buttons that stay readable. */
    private fun styleNavigationBar() {
        val w = window.window ?: return
        @Suppress("DEPRECATION") // Ignored on Android 15+, where the keyboard draws behind the bar.
        w.navigationBarColor = getColor(R.color.keyboard_background)
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(w, w.decorView).isAppearanceLightNavigationBars = !night
    }

    private fun View.key(id: Int): Button = findViewById(id)
}

/**
 * True when the cursor is at the start of a line. Phoenician lines begin with an invisible
 * [RLM] so that apps which judge direction by the first character show them right to left.
 * Null means the app won't say what's before the cursor, so no mark is added.
 */
internal fun startsLine(before: CharSequence?): Boolean =
    before != null && (before.isEmpty() || before.last() == '\n')

/**
 * How many chars of [before] (the text before the cursor) one backspace removes: the last
 * character you can see, whole (a Phoenician letter is two chars, an emoji can be many),
 * plus any invisible [RLM]s touching it, so every press visibly deletes something.
 */
internal fun charsToDelete(before: CharSequence): Int {
    val text = before.toString()
    var end = text.length
    while (end > 0 && text[end - 1] == RLM) end--
    var start = end
    if (end > 0) {
        val characters = BreakIterator.getCharacterInstance()
        characters.setText(text)
        start = characters.preceding(end).coerceAtLeast(0)
    }
    while (start > 0 && text[start - 1] == RLM) start--
    return text.length - start
}

/**
 * What the app wants Enter to do (Search, Send, Go…), or null for a plain new line.
 * Same rules Android's own keyboard follows.
 */
internal fun enterAction(info: EditorInfo?): Int? {
    if (info == null || info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) return null
    if (info.actionLabel != null) return info.actionId
    val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
    return if (action == EditorInfo.IME_ACTION_NONE) null else action
}
