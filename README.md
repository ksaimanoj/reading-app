# Little Words

Little Words is a private, offline Android app for parent-guided reading practice. It shows one large word or short sentence at a time and records whether the child read it independently or needs more practice.

## Using the app

1. Tap the profile name on the home screen to switch children or add a **Testing** profile. Existing data from an earlier version appears under **Gagan**.
2. Open **Progress → Learning stages** to see this child's unique-word milestones. Expand a stage to see its skills and word lists. A word becomes confident after independent reads in two different sessions; a later Needs practice score marks it for review without removing confidence.
3. Include the stages and skills you want, then tap **Save choices**. You can mix earlier review with new content. Changes affect the next session; an active session keeps its saved choices. Existing profiles continue using their custom length and sound filters until you save stage choices.
4. Choose **System**, **Light**, or **Dark** appearance.
5. Tap **Start reading** for individual words, or **Try short sentences** for sentence practice.
6. Swipe right for **Read independently**. Swipe left for **Needs practice**.
7. Tap **Pause** to undo the latest swipe, save your place, or end the session.
8. Open **Progress → History** for session history. Tap a session to see every completed word or sentence, its outcome, and its reading time.

Sounding out and then blending the whole word or sentence counts as an independent read. If the parent supplies a sound or the answer, use Needs practice. Silly words are deliberately unfamiliar and are marked on screen by default. Sentence practice uses only sentences whose letters and sound-pattern tags are currently enabled. Every eligible sentence appears before the app repeats one, including across sessions. Within later passes, less-seen sentences come first.

Learning-stage totals count the bundled real-word collection, not every English word or the currently enabled practice pool. Each stage and expanded skill shows a progress bar, percentage, and confident-word count out of its full collection. Each real word belongs to exactly one stage and skill. The initial stages are short-vowel CVC words, digraphs and doubled consonants, and blends. Two-letter, tricky, and two-syllable words are in Additional practice. Silly words are separate and do not count toward real-word milestones. Detailed stage progress is a record of parent observations, not a reading-level assessment. The app does not automatically unlock or disable stages. More advanced vowel stages await reviewed content.

**Settings** keeps appearance, scoring-button, and silly-word display preferences. After saving stage choices, open **Advanced options** there to restrict letters or sound patterns. The stage counts stay fixed; each expanded skill shows how many of its words remain available under those restrictions. If a later release adds real words, increase `CATALOGUE_VERSION` in `LearningStages.kt` so completion records for the earlier collection remain visible.

Reading time starts when a card is visible and ends when it is scored. Pauses and time in the background are excluded. The time is stored with each new attempt; older attempts show **time not recorded**. If Android terminates the app mid-card, timing restarts when the card is shown again.

## Privacy and data

The app has no internet permission, account, advertising, analytics, or remote content. Each profile has separate settings, reading history, and saved sessions. They stay in app-private storage on the device. Android backup and device-transfer backup are disabled for this data.

Uninstalling the app or clearing its storage deletes the reading history. Export and restore are future features.

## Installing or upgrading the supplied APK

The ready-to-install development build is in `deliverables/LittleWords-debug.apk`. Copy it to an Android device running Android 8.0 or later and open it. Android may ask you to allow installation from the app used to open the file.

This APK is signed with a development key. A Play Store or long-term family release should use a private release signing key.

Settings and reading history survive a normal APK upgrade when the new APK keeps the `com.littlewords.app` application ID, is signed with the same key, and has a higher version code. Keep the signing keystore safe: Android will reject an upgrade signed with a different key. Uninstalling or clearing app storage still deletes local data.

## Updating the bundled reading content

The editable sources of truth are `content/words.csv` and `content/sentences.csv`. Word rows contain the word, its length group, sound-pattern tags, an optional review note, and explicit primary stage and subskill. The compiler validates membership and produces stage counts in `content/catalog-report.md`. Sentence rows contain two to six real words from the reviewed word catalogue, their exact combined sound-pattern tags, and a required content-review note. The compiler rejects unknown or silly words inside sentences and rejects incomplete pattern tagging.

Android builds use the checked-in generated `CatalogData.kt` and `SentenceCatalogData.kt`, so building the APK does not require Python or internet access.

From this directory, after reviewing edits to the CSV:

```sh
python3 tools/compile_catalog.py
python3 tools/compile_catalog.py --check
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

The first command validates both catalogues, regenerates the Kotlin data, and updates the reports in `content/`. The check command verifies that generated content is current without changing files. Invalid rows report their CSV row numbers and do not replace the last valid generated files.

## Building from source

Open this `android-app` folder in Android Studio, let the project sync, then run the `app` configuration. The project uses the included Gradle wrapper and Android API 36. From a terminal with the Android SDK configured:

```sh
./gradlew :app:assembleDebug
```

The generated APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Content note

The bundled word catalogue contains 707 entries across two- through five-letter real words and three-letter silly words. A separate catalogue contains 130 short sentences assembled only from bundled real words. Newly added sentences are original decodable drafts and should be checked by a parent before wider use. Invented words are drawn from a fixed pool rather than generated on the device. Structural checks cannot fully verify pronunciation, regional meaning, grammar, or teaching suitability, so the exact content should receive a parent or educator review before broader distribution.

See [verification results](docs/VERIFICATION.md), the [original app design](../docs/superpowers/specs/2026-09-17-reading-app-design.md), and the [catalogue expansion design](../docs/superpowers/specs/2026-09-19-word-catalogue-expansion-design.md).
