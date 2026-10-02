# Verification

## Active balanced sentence catalogue — 2 October 2026

The checked-in active list has 344 sentences covering all 475 real library words, with no silly words. The ten most frequent real words contribute 22.9% of real-word appearances; the [frequency table](../content/sentence-frequency.csv) lists every word and helper. All 344 sentences are in the Android catalogue, with no parent-approval status gating inclusion.

The catalogue compiler check and 24 Python tests passed. All 43 local Android unit tests passed. The version 1.10 debug APK (code 12) was built and copied to `deliverables/LittleWords-debug.apk`; it matches `app/build/outputs/apk/debug/app-debug.apk` at SHA-256 `99d3e5087317d70d436a916164c63e365561af73162a8dbe1e346fc45591ef26`. The signer matches the prior debug APK. These checks cover helper eligibility, the exclusion of silly words and lowercase `i` from helpers, real-word progress from a sentence containing helpers, and progress from a sentence retired from the active list. A device and visual release check remain outstanding. The APK was not installed on a device.

Historical sentence attempts remain in History, and successful historical reads still credit real library words. The sentence progress total now counts the 344 active sentences.

## Store-readiness polish 1.9 — 30 September 2026

Home now shows the count from the actual sentence selector and offers Adjust choices when no short sentence is eligible. Settings and Learning stages explain that stages guide sentence selection by sounds and avoid calling unapproved content “reviewed.” Reading has a first-score swipe hint plus an immediate Show/Hide buttons control; that choice persists per profile. Settings opens About & privacy with accurate local-storage and deletion information. Release signing can be supplied through private environment variables; without them the release AAB is unsigned.

- 40 local Kotlin tests, 42 connected tests on a separate Android 16 (API 36.1) emulator, debug lint, debug APK build, and release AAB build passed. Connected tests include the Home zero state and selector count, scoring guidance/buttons, persistence across profiles and restart, and Settings → privacy → Settings with unsaved choices intact.
- A repeat run exposed a test-only race when `ReadingUiTest` closed its temporary database before stopping its ViewModel. The cleanup now cancels and joins that ViewModel first; the final 42-test run passed.
- Home and Reading were visually checked at 1080×2400 portrait and 2400×1080 landscape: [Home](screenshots/1.9/home-light.png), [Reading](screenshots/1.9/reading-light.png). A narrow phone, tablet, 200% text, TalkBack, API 26/33, and a Play-installed bundle still need release-candidate checks.
- Version 1.9, code 11, retains application ID `com.littlewords.app`. The development APK uses the prior debug signing certificate and SHA-256 `e5e223830762d157acc3233841b4eb3b2f228a337d0d4f42b9b1ab4cdcda92f1`; build, deliverable, and project-output copies match. The release AAB built successfully but is unsigned, so it cannot be submitted to Play yet.
- At the time of this release check, the full privacy policy still needed a publisher contact and hosted URL, and the then-current 130 sentences required recorded human review before public release. Existing debug-installed phone data also needed an export/import path before moving to a differently signed Play build.

## Slate blue and portrait navigation 1.8 — 28 September 2026

Implemented the user-approved Slate blue palette in both appearances, including selected controls, links, progress bars and the launcher. Phone navigation is portrait; word/sentence reading and Pause remain landscape. Tablet and multi-window orientation is unrestricted. Headers and history adapt to portrait; large-text Home labels were corrected after visual inspection.

- 40 local Kotlin tests and all 37 connected Android tests passed on the isolated Android 16 emulator. The added orientation test checks landscape reading/Pause, activity recreation, portrait Home, and resuming the same card.
- Both activity-recreation tests passed again at an 800 dp tablet width after the final Home label adjustment.
- The final debug build and lint passed: no errors; one Android Gradle plugin update advisory.
- Home and Settings were visually inspected in Light and Dark at 1080×2400. Home was inspected at 320 dp width and 1.5× text; all navigation and reading actions remain readable. Evidence: [`home-light.png`](screenshots/slate-blue/home-light.png), [`home-dark.png`](screenshots/slate-blue/home-dark.png), [`settings-dark.png`](screenshots/slate-blue/settings-dark.png), [`home-dark-large-text.png`](screenshots/slate-blue/home-dark-large-text.png).
- Version 1.8, code 10, retains application ID `com.littlewords.app`. The signing certificate matches the previous development APK. Final APK SHA-256: `28c88698f774a9b3d3cbbfaac5119610d6bad390f65eec51d5c5831d49e6f7b0`. Build, deliverables and both project-output copies are identical.
- This is a development-signed test APK. A physical family-device upgrade and split-screen visual sweep were not performed. Startup background follows the system theme until saved per-profile appearance loads. This does not constitute complete Play Store readiness testing.


## Database upgrade fix 1.7.1 — 27 September 2026

The Redmi app crashed on launch after upgrading directly from 1.5 to 1.7. Room rejected the version-3 `sessions` table during the version-4 migration because the earlier app had created a partial `one_active_session_per_profile` index that Room's exported schema does not declare. The migration now drops both manually managed partial indices before Room validates the schema; the existing `onOpen` callback recreates them afterward.

- A new version-3 migration test reproduced the phone's exception before the fix and passed afterward. Both migration tests passed on the Redmi.
- Version 1.7.1 (code 9) was installed over 1.7 without clearing app data. The app launched and remained running.
- The phone's database advanced from version 3 to 4. Before and after counts were identical: 2 profiles, 1 settings row, 7 sessions, 104 cards, and 97 attempts. SQLite's quick check returned `ok` for both copies.
- Local unit tests, Android lint, and the debug APK build passed. The deliverable, build output, and project-level output APK share SHA-256 `cc185e482b795e32e99e4a2ff90207940885be1c074890048e93b2540507dcb0`.

## Progress bars release 1.7 — 27 September 2026

Every learning stage and expanded skill now displays a rounded progress bar, whole-number percentage, and confident-word count out of that section's full real-word collection. The denominator remains stable when practice filters change. The bar exposes its progress and count to accessibility services. The updated skill layout was inspected on the Android 16 emulator; see [`screenshots/learning-stages-detail-light.png`](screenshots/learning-stages-detail-light.png).

- 16 Python catalogue/compiler tests and 33 local Kotlin unit tests passed; generated catalogue files are current.
- 35 connected Android emulator tests passed, including new checks for 70% progress and the empty-collection case.
- Debug APK build and Android lint passed. Application ID remains `com.littlewords.app`; version code is 8 and version name is 1.7. The signing certificate matches release 1.6.
- The debug APK in `deliverables/LittleWords-debug.apk`, the build output, and the project-level `outputs` copy have the same SHA-256: `e92cfde5ab9162813f7a3525cf5643d973b1310c0dc539d245b1c75f20da4359`.

## Learning stages release 1.6 — 27 September 2026

The app now has profile-specific learning stages. The 707-entry catalogue contains 475 real words assigned exactly once across short-vowel CVC (158), digraphs and doubled consonants (82), blends (172), and additional practice (63). The 232 silly words remain separate. The generated [`catalog-report.md`](../content/catalog-report.md) lists every primary subskill count.

The implementation derives confidence from valid word attempts in two distinct sessions, marks a confident word for review after a later Needs practice score, and recalculates after undo. Stage selections are profile-scoped JSON settings; older settings and active-session snapshots keep their original length and sound rules. Room schema 4 adds only the versioned stage-achievement table. The 1-to-4 migration test preserved its recorded word, session, profile, and duration state.

Verification run on the available Android 16 API 36.1 emulator:

- 16 Python catalogue/compiler tests passed; generated files passed check-only validation.
- 33 local Kotlin unit tests and 33 connected Android tests passed, including stage eligibility, coverage, profile isolation, stage controls, configuration compatibility, and migration.
- Debug APK build and Android lint passed. Application ID remains `com.littlewords.app`; version code is 7 and version name is 1.6. The new APK's signing certificate matches the previous development APK.
- The Learning stages screen was inspected in light and dark themes at 2400×1080 landscape. The home screen was inspected at 1280×720 and at 1.5× system text size; both reading actions remained fully visible after a compact-layout adjustment. The app still requests landscape orientation on phones. A portrait tablet window and a family-device upgrade were not checked in this run.

Commands run:

```sh
python3 -m unittest discover -s tools -p 'test_*.py'
python3 tools/compile_catalog.py --check
./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:assembleDebug :app:lintDebug
```

The 1.6 debug APK is `deliverables/LittleWords-debug.apk`. Its SHA-256 is `243636471306309e73fd7f4fedd4e8b8fd2bd9d55a08c97a1c5666f033f118b4`.

## Previous release 1.5

Verified on 22 September 2026 with a dedicated Android 16 API 36.1 arm64 emulator. Version 1.4 was also installed over version 1.3 on the connected family phone; the app opened, its database reached schema version 3, and the user confirmed it worked. Version 1.5 was checked on the emulator only.

## 1.5 catalogue

The bundled catalogue contains 707 unique entries:

| Group | Count |
| --- | ---: |
| Two-letter real | 13 |
| Three-letter real | 180 |
| Three-letter silly | 232 |
| Four-letter real | 160 |
| Five-letter real | 122 |

The generated report is at [`../content/catalog-report.md`](../content/catalog-report.md). The CSV compiler and its check-only mode passed against the checked-in generated Kotlin file.

The separate sentence catalogue at version 1.5 contained 130 entries: the previous 60 plus 70 new original decodable drafts. Every sentence had two to six words, used only real words from the bundled word catalogue, and carried the exact union of those words' sound-pattern tags. With that version's default sound settings, 100 were eligible; with the family phone's settings, 101 were eligible. The current catalogue and report are described at the top of this document.

## Automated checks

- 14 Python compiler tests cover both CSV formats, duplicates, word and sentence shape, category lengths, supported tags, exact sentence-pattern derivation, reviewed real-word membership, silly-word exclusions, minimum sizes, deterministic generation, reports, and stale-output detection.
- 25 local Kotlin tests cover word and sentence catalogue integrity, phonics eligibility and selection, full-pass sentence variety, enabled-category validation, recent-item spacing, missed-item priority, empty/tiny pools, multisyllable gating, category toggling, and visible-reading timing.
- 29 Android emulator tests cover legacy configuration decoding, Room 1-to-3 migration, word/sentence mode configuration round trips, real persistence, exact sentence storage, durable scoring, full-pass sentence variety, timing storage, rapid duplicate-score rejection, undo audit history, repetition spacing, restart recovery, failed-write rollback, word/sentence mode UI, expandable session history, swipe behavior, appearance, activity recreation, profile switching, duplicate names, isolation of settings, progress, and active sessions, and the home page's visible choices without a scroll action.
- Android debug lint completed without errors.
- The debug APK built successfully from the checked-in generated catalogue.

Commands used:

```sh
python3 -m unittest tools/test_compile_catalog.py -v
python3 tools/compile_catalog.py --check
./gradlew :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:assembleDebug :app:lintDebug
ANDROID_SERIAL=emulator-5580 ./gradlew :app:connectedDebugAndroidTest --offline
```

## APK result

- Application ID: `com.littlewords.app`
- Version code: `6`
- Version name: `1.5`
- Minimum Android API: `26`
- SHA-256: `df1f6922c1cb2e3e677fcece96721b6b9e4233caa267d5a3802e36d4b30c2dd2`
- The build requests no internet permission.

The generated APK, `deliverables/LittleWords-debug.apk`, is byte-for-byte identical to the build output and the copy in the project-level `outputs` directory.

## Upgrade behavior

The Room schema migrates from version 1 through version 3. Version 2 adds nullable reading time to attempts. Version 3 creates profiles, assigns all existing settings and sessions to **Gagan**, and preserves cards and attempts through their sessions. Older attempts correctly display “time not recorded”; no historical duration is inferred. Migration was tested against a version-1 database and an existing version-2 emulator installation. Stored configuration JSON accepts both the old five-weight format and the newer enabled-category format; configurations without a practice mode default to word practice. Existing cards keep exact text, category, and pattern snapshots. Normal APK upgrades retain the database when the application ID and signing key remain unchanged and the version code increases.

Uninstalling or clearing app storage still deletes settings and reading history. The supplied APK is development-signed; future upgrade APKs must use the same development key, or a stable private release key must be adopted before installing the first long-term release.

## Manual display checks

The initial release was manually checked on landscape phone and portrait tablet-sized emulator windows for home, settings, reading, pause, and progress screens; swipe outcomes; restart persistence; light/dark appearance; and word resizing. The sentence-mode update was manually checked on a landscape phone emulator for the home choice and sentence reading screen. Sentence text remained centered, large, unclipped, and clearly labelled. UI instrumentation exercised sentence-mode startup, expandable session results, stored timing, and migration.

For version 1.5, the home page was checked visually at the emulator's 2400×1080 landscape display and a smaller 1280×720 landscape display. The profile, navigation, and both reading actions remained visible together. The page has no scroll container; its emulator UI test confirms that no scroll action is exposed.

## Content-review boundary

Structural validation cannot prove pronunciation, regional meaning, sentence naturalness, or child suitability. The word and sentence pools are conservative, but a parent or phonics educator should review the exact entries against the child's dialect and teaching sequence before broader distribution.
