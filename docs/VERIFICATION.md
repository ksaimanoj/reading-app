# Verification

Verified on 22 September 2026 with a dedicated Android 16 API 36.1 arm64 emulator. Version 1.4 was also installed over version 1.3 on the connected family phone; the app opened, its database reached schema version 3, and the user confirmed it worked.

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
- 28 Android emulator tests cover legacy configuration decoding, Room 1-to-3 migration, word/sentence mode configuration round trips, real persistence, exact sentence storage, durable scoring, full-pass sentence variety, timing storage, rapid duplicate-score rejection, undo audit history, repetition spacing, restart recovery, failed-write rollback, word/sentence mode UI, expandable session history, swipe behavior, appearance, activity recreation, profile switching, duplicate names, and isolation of settings, progress, and active sessions.
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
- Version code: `5`
- Version name: `1.4`
- Minimum Android API: `26`
- SHA-256: `457de7bad82b977c472930f957ed066e24d2c0573d2aa719a79ee6d760e9a21e`
- The build requests no internet permission.

The generated APK, `deliverables/LittleWords-debug.apk`, is byte-for-byte identical to the build output and the copy in the project-level `outputs` directory.

## Upgrade behavior

The Room schema migrates from version 1 through version 3. Version 2 adds nullable reading time to attempts. Version 3 creates profiles, assigns all existing settings and sessions to **Gagan**, and preserves cards and attempts through their sessions. Older attempts correctly display “time not recorded”; no historical duration is inferred. Migration was tested against a version-1 database and an existing version-2 emulator installation. Stored configuration JSON accepts both the old five-weight format and the newer enabled-category format; configurations without a practice mode default to word practice. Existing cards keep exact text, category, and pattern snapshots. Normal APK upgrades retain the database when the application ID and signing key remain unchanged and the version code increases.

Uninstalling or clearing app storage still deletes settings and reading history. The supplied APK is development-signed; future upgrade APKs must use the same development key, or a stable private release key must be adopted before installing the first long-term release.

## Manual display checks

The initial release was manually checked on landscape phone and portrait tablet-sized emulator windows for home, settings, reading, pause, and progress screens; swipe outcomes; restart persistence; light/dark appearance; and word resizing. The sentence-mode update was manually checked on a landscape phone emulator for the home choice and sentence reading screen. Sentence text remained centered, large, unclipped, and clearly labelled. UI instrumentation exercised sentence-mode startup, expandable session results, stored timing, and migration.

## Content-review boundary

Structural validation cannot prove pronunciation, regional meaning, sentence naturalness, or child suitability. The word and sentence pools are conservative, but a parent or phonics educator should review the exact entries against the child's dialect and teaching sequence before broader distribution.
