# Verification

Verified on 19 September 2026 with a dedicated Android 16 API 36.1 arm64 emulator.

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

## Automated checks

- 12 Python compiler tests cover CSV parsing, duplicates, word shape, category lengths, supported tags, structural tags, tricky-word review notes, silly-word exclusions, minimum size, deterministic generation, reports, and stale-output detection.
- 16 local Kotlin tests cover catalogue integrity, combined-pool eligibility and selection, enabled-category validation, recent-word spacing, missed-word priority, empty/tiny pools, multisyllable gating, and category toggling.
- 19 Android emulator tests cover legacy percentage migration, new configuration round trips, real Room persistence, durable scoring, rapid duplicate-score rejection, undo audit history, repetition spacing, restart recovery, failed-write rollback, group-switch UI, swipe behavior, appearance, and activity recreation.
- Android debug lint completed without errors.
- The debug APK built successfully from the checked-in generated catalogue.

Commands used:

```sh
python3 -m unittest tools/test_compile_catalog.py -v
python3 tools/compile_catalog.py --check
./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

## APK result

- Application ID: `com.littlewords.app`
- Version code: `2`
- Version name: `1.1`
- Minimum Android API: `26`
- SHA-256: `8ab55c55356d7cf3a1b40f3da91ab33070af2183844c88a8ae85c114e8f29401`
- The build requests no internet permission.

The generated APK, `deliverables/LittleWords-debug.apk`, is byte-for-byte identical to the build output and the copy in the project-level `outputs` directory.

## Upgrade behavior

The Room schema remains at version 1. Stored configuration JSON accepts both the old five-weight format and the new enabled-category format. Existing cards keep exact word, category, and pattern snapshots. Normal APK upgrades retain the database when the application ID and signing key remain unchanged and the version code increases.

Uninstalling or clearing app storage still deletes settings and reading history. The supplied APK is development-signed; future upgrade APKs must use the same development key, or a stable private release key must be adopted before installing the first long-term release.

## Prior manual display checks

The initial release was manually checked on landscape phone and portrait tablet-sized emulator windows for home, settings, reading, pause, and progress screens; swipe outcomes; restart persistence; light/dark appearance; and word resizing. This catalogue update additionally compiles and exercises the new settings screen through UI instrumentation, but it did not repeat the full screenshot set.

## Content-review boundary

Structural validation cannot prove pronunciation, regional meaning, or child suitability. The real and silly pools are conservative, but a parent or phonics educator should review the exact entries against the child's dialect and teaching sequence before broader distribution.
