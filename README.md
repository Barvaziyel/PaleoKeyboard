# PaleoKeyboard

A Phoenician keyboard for Android. It types the 22 Phoenician letters on the Hebrew keyboard layout: 𐤀 is where א is, 𐤒 where ק is, and so on. If you can type Hebrew, you already know where everything is.

## What it does

- **Space bar types the word divider 𐤟.** To get a normal space instead, open the app and turn off "Space bar types 𐤟".
- **Symbols page** (the `?!$` key) with digits and common punctuation.
- **Enter does what the text field asks for**: search, send, go, or done. In a message or note it starts a new line.
- **Light and dark**, following the phone's theme.
- **Switching keyboards**: tap 🌐, or long-press the space bar to pick from a list.

Lines start with an invisible right-to-left mark, so apps show Phoenician text right to left even when they would guess the direction wrong.

## Install

There's no release yet, so build it yourself:

1. Open the project in Android Studio and press Run, or connect a phone and run `./gradlew installDebug`.
2. Open the **PaleoKeyboard** app, tap **Open settings** and turn the keyboard on.
3. Tap **Choose keyboard** and pick Phoenician Keyboard. The box underneath is there to try it.

Needs Android 8.0 or newer. Android comes with a Phoenician font (Noto Sans Phoenician), so the letters show without installing anything. Someone you send Phoenician text to needs a Phoenician font to see it.
