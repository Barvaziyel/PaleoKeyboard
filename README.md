# PaleoKeyboard

A Paleo-Hebrew keyboard for Android. It types the 22 Paleo-Hebrew letters on the Hebrew keyboard layout: 𐤀 is where א is, 𐤒 where ק is, and so on. If you can type Hebrew, you already know where everything is.

## What it does

- **Space bar types the word divider 𐤟**, the small dot Paleo-Hebrew puts between words. To get a normal space instead, open the app and turn off "Word divider on the space bar".
- **Symbols page** (the `?!$` key) with digits and common punctuation.
- **Enter does what the text field asks for**: search, send, go, or done. In a message or note it starts a new line.
- **Light and dark**, following the phone's theme.
- **Switching keyboards**: tap 🌐, or long-press the space bar to pick from a list.

Lines start with an invisible right-to-left mark, so apps show Paleo-Hebrew text right to left even when they would guess the direction wrong.

## Install

1. On your phone, open the [latest release](https://github.com/Barvaziyel/PaleoKeyboard/releases/latest) and download the `.apk` file.
2. Open it. Android asks whether your browser may install apps: allow it, then tap **Install**.
3. Open the **PaleoKeyboard** app, tap **Open settings** and turn the keyboard on.
4. Tap **Choose keyboard** and pick **Paleo-Hebrew**. The box underneath is there to try it.

Needs Android 8.0 or newer. Android already has a font for these letters, so they show without installing anything. Someone you send Paleo-Hebrew text to needs a font for it too.

To build it yourself instead, open the project in Android Studio and press Run, or connect a phone and run `./gradlew installDebug`.

## Releasing

1. Raise `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Run `./gradlew assembleRelease`. It signs with the key named in your own `~/.gradle/gradle.properties` (`paleoStoreFile`, `paleoStorePassword`, `paleoKeyAlias`, `paleoKeyPassword`). Without those, the APK comes out unsigned.
3. Copy `app/build/outputs/apk/release/app-release.apk` to `PaleoKeyboard-X.Y.apk` and run `gh release create vX.Y PaleoKeyboard-X.Y.apk`.

Every update has to be signed with the same key, or phones refuse to install it over the old version. Keep a backup of the key file and its password somewhere other than this computer.

## License

MIT. See [LICENSE](LICENSE).
