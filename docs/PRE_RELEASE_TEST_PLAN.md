# Pre-release test plan

The 1.9 development build passed local Kotlin tests, 42 connected tests on an Android 16 emulator, lint, and visual checks of the updated Home and Reading screens. Those results are recorded in [VERIFICATION.md](VERIFICATION.md). The checks below address the remaining release risks; they are not marked as passed until performed on a release candidate.

| Priority | Check | Pass condition | Status |
| --- | --- | --- | --- |
| P0 | Existing family phone upgrade | History, profiles, settings, active card and duration survive the chosen signing/data migration path; counts match before and after. | Pending; release signing path undecided |
| P0 | Play-distributed build | Signed AAB installs through an internal test track, opens offline, and updates from an earlier Play test build without data loss. | Pending |
| P0 | Sentence catalogue checks | The 344 active sentences cover all 475 real words, exclude silly words, and pass the compiler's balance and tag checks on the release candidate. | Passed on the local debug build; rerun for the signed candidate |
| P1 | Supported Android versions | Run core Home → word/sentence → Pause → score/undo → History flows on API 26, 33, and 36, including a fresh install and an app upgrade. | API 36 emulator covered; API 26 and 33 pending |
| P1 | Phone, tablet and resizable window | Portrait navigation and landscape reading on phones; usable controls and preserved card on tablet/foldable rotation and split screen. | Phone and one tablet-size recreation check passed; broader visual check pending |
| P1 | Accessibility | TalkBack announces each action and result; scoring is possible without a swipe; controls remain visible at 200% text and display enlargement. | Pending |
| P1 | Empty and changed practice pools | Zero eligible sentences give a clear route to fix choices; stage/letter changes during an active session are explained and do not alter its saved card. | Zero state and selector count pass UI tests; full adjustment path and active-session changes still need device review |
| P1 | Long-term history | Seed a profile with thousands of attempts, then time startup, scoring, History scrolling, profile switching and milestone calculation on a midrange device. | Pending |
| P2 | Appearance and lifecycle | Exercise System/Light/Dark, background/foreground, process death, and rotation from reading and Pause, including a larger text setting. | Device tests cover card recovery; full matrix pending |

For manual checks, capture the device model, OS version, app version, test data setup, result, and any screenshot or log. Run the final smoke test on the exact signed bundle distributed through Play, since the debug APK is not the release artifact.
