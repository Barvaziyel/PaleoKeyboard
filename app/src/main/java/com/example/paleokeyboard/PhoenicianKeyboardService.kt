package com.example.paleokeyboard

import android.annotation.SuppressLint
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.Button

class PhoenicianKeyboardService : InputMethodService() {

    private var isDeleting = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var deleteDelay = 200L
    private var isSymbols = false
    private var currentMap: Map<Int, String> = emptyMap()

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

    override fun onEvaluateFullscreenMode(): Boolean {
        return false
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        updateKeyboard(view)

        val symbols = view.findViewById<Button>(R.id.key_symbols)

        symbols.setOnClickListener {
            isSymbols = !isSymbols
            updateKeyboard(view)
        }

        val enter = view.findViewById<Button>(R.id.key_enter)

        enter.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            currentInputConnection.commitText("\n", 1)
        }

        val backspace = view.findViewById<Button>(R.id.key_backspace)

        backspace.setOnClickListener {
            deleteOnce()
        }

        @SuppressLint("ClickableViewAccessibility")
        backspace.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    isDeleting = true
                    deleteDelay = 200L
                    handler.postDelayed(deleteRunnable, 300)
                    false
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    isDeleting = false
                    false
                }
                else -> false
            }
        }

        val globe = view.findViewById<Button>(R.id.key_globe)
        val shouldShowGlobe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            shouldOfferSwitchingToNextInputMethod()
        } else {
            true
        }

        if (shouldShowGlobe) {
            globe.visibility = View.VISIBLE
            globe.setOnClickListener {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                @Suppress("DEPRECATION")
                imm.switchToNextInputMethod(window.window?.attributes?.token, false)
            }
            globe.setOnLongClickListener {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
                true
            }
        } else {
            globe.visibility = View.GONE
        }

        view.setOnApplyWindowInsetsListener { v, insets ->
            val bottom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.systemBars()).bottom
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetBottom
            }
            // Add extra padding if bottom inset is 0 to ensure it's not too close to the edge
            val extraPadding = if (bottom == 0) (10 * resources.displayMetrics.density).toInt() else 0
            v.setPadding(0, 0, 0, bottom + extraPadding)
            insets
        }

        return view
    }

    private fun deleteOnce() {
        val ic = currentInputConnection
        val before = ic.getTextBeforeCursor(2, 0)

        if (!before.isNullOrEmpty()) {
            val deleteLength = if (Character.isSurrogate(before.last())) 2 else 1
            ic.deleteSurroundingText(deleteLength, 0)
        }
    }

    private val deleteRunnable = object : Runnable {
        override fun run() {
            if (isDeleting) {
                deleteOnce()
                deleteDelay = maxOf(50L, deleteDelay - 10)
                handler.postDelayed(this, deleteDelay)
            }
        }
    }

    private fun updateKeyboard(view: View) {
        currentMap = if (isSymbols) symbolsMap else phoenicianMap

        currentMap.forEach { (id, text) ->
            val button = view.findViewById<Button?>(id) ?: return@forEach
            button.text = text

            button.setOnClickListener {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)

                val output = if (isSymbols) text else "\u200F$text"
                currentInputConnection.commitText(output, 1)
            }

            if (id == R.id.key_space) {
                button.setOnLongClickListener {
                    it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showInputMethodPicker()
                    true
                }
            }
        }

        val symbolsButton = view.findViewById<Button>(R.id.key_symbols)
        symbolsButton.text = if (isSymbols) "\uD802\uDD00\uD802\uDD01\uD802\uDD02" else "\u003F\u0021\u0024"
    }
}
