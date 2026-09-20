# Little Words

Little Words is a private, offline Android app for parent-guided reading practice. It shows one large word at a time and records whether the child read it independently or needs more practice.

## Using the app

1. Open **Settings** and choose the sounds and letters your child knows.
2. Turn the five word groups on or off. The app mixes all eligible words from the enabled groups automatically.
3. Choose **System**, **Light**, or **Dark** appearance.
4. Tap **Start reading**.
5. Swipe right for **Read independently**. Swipe left for **Needs practice**.
6. Tap **Pause** to undo the latest swipe, save your place, or end the session.
7. Open **Progress** for session history and words to revisit.

Sounding out and then blending the whole word counts as an independent read. If the parent supplies a sound or the answer, use Needs practice. Silly words are deliberately unfamiliar and are marked on screen by default.

## Privacy and data

The app has no internet permission, account, advertising, analytics, or remote content. Word lists, settings, and reading history stay in app-private storage on the device. Android backup and device-transfer backup are disabled for this data.

Uninstalling the app or clearing its storage deletes the reading history. Export and restore are future features.

## Installing or upgrading the supplied APK

The ready-to-install development build is in `deliverables/LittleWords-debug.apk`. Copy it to an Android device running Android 8.0 or later and open it. Android may ask you to allow installation from the app used to open the file.

This APK is signed with a development key. A Play Store or long-term family release should use a private release signing key.

Settings and reading history survive a normal APK upgrade when the new APK keeps the `com.littlewords.app` application ID, is signed with the same key, and has a higher version code. Keep the signing keystore safe: Android will reject an upgrade signed with a different key. Uninstalling or clearing app storage still deletes local data.

## Updating the bundled words

The editable source of truth is `content/words.csv`. Each row contains the word, its word group, its required pattern tags, and an optional review note. Android builds use the checked-in generated `CatalogData.kt`, so building the APK does not require Python or internet access.

From this directory, after reviewing edits to the CSV:

```sh
python3 tools/compile_catalog.py
python3 tools/compile_catalog.py --check
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

The first command validates the complete catalogue, regenerates the Kotlin data, and updates `content/catalog-report.md`. The check command verifies that generated content is current without changing files. Invalid rows report their CSV row numbers and do not replace the last valid generated files.

## Building from source

Open this `android-app` folder in Android Studio, let the project sync, then run the `app` configuration. The project uses the included Gradle wrapper and Android API 36. From a terminal with the Android SDK configured:

```sh
./gradlew :app:assembleDebug
```

The generated APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Content note

The bundled catalogue contains 707 entries across two- through five-letter real words and three-letter silly words. Invented words are drawn from a fixed reviewed pool rather than generated on the device. Structural checks cannot fully verify pronunciation, regional meaning, or teaching suitability, so the exact pool should receive a parent or educator review before broader distribution.

See [verification results](docs/VERIFICATION.md), the [original app design](../docs/superpowers/specs/2026-09-17-reading-app-design.md), and the [catalogue expansion design](../docs/superpowers/specs/2026-09-19-word-catalogue-expansion-design.md).
