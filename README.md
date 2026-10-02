# Little Words

Little Words is a private, offline Android app for parent-guided reading practice. It shows one large word or short sentence at a time and records whether the child read it independently or needs more practice.

## Design prototype

The [navigable HTML prototype](design/prototype.html) is the current Android UI baseline for design review. It uses illustrative data. Review proposed user-facing changes there before implementing them in the app. The [screen comparison](design/SCREEN_COMPARISON.md) links saved emulator captures, and the [feature audit](design/FEATURE_AUDIT.md) records current features and interactions to resolve. Project guidance is in [AGENTS.md](AGENTS.md) and [project memory](memory/projects/little-words.md).

## Using the app

1. Tap the profile name on the home screen to switch children or add a **Testing** profile. Existing data from an earlier version appears under **Gagan**.
2. Open **Progress → Learning stages** to see this child's unique-word milestones and filtered sentence progress. Expand a stage to see its skills and word lists. Reading a sentence independently also credits each real word in it. A word or sentence becomes confident after independent reads in two different sessions; a later Needs practice score marks that item for review without removing confidence. A missed sentence does not mark all of its words as missed.
3. Include the stages and skills you want, then tap **Save choices**. You can mix earlier review with new content. Changes affect the next session; an active session keeps its saved choices. Existing profiles continue using their custom length and sound filters until you save stage choices.
4. Choose **System**, **Light**, or **Dark** appearance. Both themes use slate-blue accents.
5. Tap **Start reading** for individual words, or **Try short sentences** for sentence practice. Home shows how many sentences match the current choices. If none match, tap **Adjust choices**.
6. Swipe right for **Read it** or left for **Needs practice**. The first reading card explains the gestures; **Show buttons** offers the same scoring actions without swiping and saves that preference for the current child.
7. Tap **Pause** to undo the latest swipe, save your place, or end the session.
8. Open **Progress → History** for session history. Filter by words or sentences and tap a session to see matching attempts. Ended sessions show their end time and elapsed duration.

On phones, navigation screens use portrait; word and sentence reading, including Pause, use landscape. Tablets and multi-window layouts follow the available space. Returning home and resuming keeps the current card.

Sounding out and then blending the whole word or sentence counts as an independent read. If the parent supplies a sound or the answer, use Needs practice. Silly words are deliberately unfamiliar and are marked on screen by default. Sentence practice uses only sentences whose letters and sound-pattern tags are currently enabled. Every eligible sentence appears before the app repeats one, including across sessions. Within later passes, less-seen sentences come first.

Learning-stage totals count the bundled real-word collection, not every English word or the currently enabled practice pool. Each stage and expanded skill shows a progress bar, percentage, and confident-word count out of its full collection. Each real word belongs to exactly one stage and skill. The initial stages are short-vowel CVC words, digraphs and doubled consonants, and blends. Two-letter, tricky, and two-syllable words are in Additional practice. Silly words are separate and do not count toward real-word milestones. Detailed stage progress is a record of parent observations, not a reading-level assessment. The app does not automatically unlock or disable stages. More advanced vowel stages await reviewed content.

**Settings** keeps appearance, scoring-button, and silly-word display preferences, and opens **About & privacy** for local-data details. After saving stage choices, open **Advanced options** there to restrict letters or sound patterns. Stage choices guide sentence practice by supported sounds; a sentence may contain a word assigned to another stage. The stage counts stay fixed; each expanded skill shows how many of its words remain available under those restrictions. If a later release adds real words, increase `CATALOGUE_VERSION` in `LearningStages.kt` so completion records for the earlier collection remain visible.

Reading time starts when a card is visible and ends when it is scored. Pauses and time in the background are excluded. The time is stored with each new attempt; older attempts show **time not recorded**. If Android terminates the app mid-card, timing restarts when the card is shown again.

Session duration is elapsed time from the saved start time to the end time. It includes pauses and time away from the app. Active sessions show **In progress** until ended; no separate duration is stored because it can be calculated from the two timestamps.

## Privacy and data

The app has no internet permission, account, advertising, analytics, or remote content. Each profile has separate settings, reading history, and saved sessions. They stay in app-private storage on the device. Android backup and device-transfer backup are disabled for this data.

Uninstalling the app or clearing its storage deletes the reading history. Export and restore are future features. The About & privacy screen summarizes these facts. A complete public policy with publisher contact details is still required before Play submission; see [the release checklist](docs/PLAY_PRIVACY_CHECKLIST.md).

## Installing or upgrading the supplied APK

The ready-to-install development build is in `deliverables/LittleWords-debug.apk`. Copy it to an Android device running Android 8.0 or later and open it. Android may ask you to allow installation from the app used to open the file.

This APK is signed with a development key. A Play Store or long-term family release should use a private release signing key.

For Play bundle signing and the family phone's data-transition steps, see [Play release and existing-device data](docs/PLAY_RELEASE.md).

Settings and reading history survive a normal APK upgrade when the new APK keeps the `com.littlewords.app` application ID, is signed with the same key, and has a higher version code. Keep the signing keystore safe: Android will reject an upgrade signed with a different key. Uninstalling or clearing app storage still deletes local data.

## Updating the bundled reading content

The editable sources of truth are `content/words.csv`, `content/sentences.csv`, and `content/sentence-helpers.csv`. Word rows contain the word, its length group, sound-pattern tags, an optional review note, and explicit primary stage and subskill. The compiler validates membership and produces stage counts in `content/catalog-report.md`. Each sentence has two to eight words drawn from the real-word catalogue or the explicit helper allowlist (`I`, `a`, `my`, `to`), exact combined sound-pattern tags, and a content-review note. The compiler rejects unknown or silly words, checks that all 475 real words appear, and limits the ten most frequent real words to 25% of real-word appearances. The full distribution is in `content/sentence-frequency.csv`.

Android builds use the checked-in generated `CatalogData.kt` and `SentenceCatalogData.kt`, so building the APK does not require Python or internet access.

From this directory, after reviewing edits to the CSV:

```sh
python3 tools/compile_catalog.py
python3 tools/compile_catalog.py --check
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

The first command validates both catalogues, regenerates the Kotlin data, and updates the reports in `content/`. The check command verifies that generated content is current without changing files. Invalid rows report their CSV row numbers and do not replace the last valid generated files. After changing sentence text or tags, run `python3 -m tools.sync_sentence_review` and use [the review sheet](content/sentence-human-review.csv) to record the parent or educator decision for each row.

## Building from source

Open this `android-app` folder in Android Studio, let the project sync, then run the `app` configuration. The project uses the included Gradle wrapper and Android API 36. From a terminal with the Android SDK configured:

```sh
./gradlew :app:assembleDebug
```

The generated APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Content note

The bundled word catalogue contains 707 entries: 475 real words and 232 three-letter silly words. The 344 sentence drafts cover every real word, include no silly word, and have a 22.9% top-ten real-word frequency share. They use four explicit helpers. All 344 sentences still need a recorded parent or educator decision before broader distribution. Invented words are drawn from a fixed pool rather than generated on the device. Structural checks cannot fully verify pronunciation, regional meaning, grammar, or teaching suitability.

See [verification results](docs/VERIFICATION.md), the [original app design](../docs/superpowers/specs/2026-09-17-reading-app-design.md), and the [catalogue expansion design](../docs/superpowers/specs/2026-09-19-word-catalogue-expansion-design.md).
