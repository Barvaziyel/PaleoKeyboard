package com.example.paleokeyboard

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** The Phoenician word divider, a small mark between words. */
const val WORD_DIVIDER = "𐤟"

/** Settings shared by the setup screen and the keyboard. */
object KeyboardPrefs {
    private const val FILE = "settings"
    private const val SPACE_IS_DIVIDER = "space_is_divider"

    fun of(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Whether the space bar types [WORD_DIVIDER] (the default) instead of a space. */
    fun spaceIsDivider(context: Context): Boolean = of(context).getBoolean(SPACE_IS_DIVIDER, true)

    fun setSpaceIsDivider(context: Context, value: Boolean) =
        of(context).edit { putBoolean(SPACE_IS_DIVIDER, value) }
}
