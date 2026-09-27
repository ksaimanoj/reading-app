# Verification

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

## Current catalogue

The bundled catalogue contains 707 unique entries:

| Group | Count |
| --- | ---: |
| Two-letter real | 13 |
| Three-letter real | 180 |
| Three-letter silly | 232 |
| Four-letter real | 160 |
| Five-letter real | 122 |

The generated report is at [`../content/catalog-report.md`](../content/catalog-report.md). The CSV compiler and its check-only mode passed against the checked-in generated Kotlin file.

The separate sentence catalogue contains 130 entries: the previous 60 plus 70 new original decodable drafts. Every sentence has two to six words, uses only real words from the bundled word catalogue, and carries the exact union of those words' sound-pattern tags. With the app's default sound settings, 100 are eligible; with the family phone's settings, 101 are eligible. Its generated report is at [`../content/sentence-catalog-report.md`](../content/sentence-catalog-report.md). New drafts still need parent review for naturalness and suitability.

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
