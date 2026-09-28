# Android screen comparison

Reviewed 28 September 2026. The comparison below records the Android 1.7.1 baseline. The user approved Slate blue, and the theme refresh and portrait navigation are now implemented in Android 1.8. The profile history shown in HTML is illustrative.

| Screen | Emulator evidence | Baseline correction |
| --- | --- | --- |
| Home | [`home-light.png`](../docs/screenshots/home-light.png) | Restored the small brand and top-right profile, Progress, and Settings links; the large “Let's read.” title; and the two-action panel on the right. Removed the extra dashboard cards and four-item navigation. The current code also adds a Learning stages link and stage message when stage choices have been saved. |
| Word reading | [`reading-dark.png`](../docs/screenshots/reading-dark.png) | Restored the full-screen reading surface, very large sans-serif word, small optional silly-word label, and Pause at top right. Scoring buttons are hidden by default. |
| Sentence reading | [`sentence-light.png`](../docs/screenshots/sentence-light.png) | Restored the simple short-sentence label, large centered sans-serif text, and no extra card number or prompts. |
| Learning stages | [`learning-stages-light.png`](../docs/screenshots/learning-stages-light.png), [`learning-stages-dark.png`](../docs/screenshots/learning-stages-dark.png), [`learning-stages-detail-light.png`](../docs/screenshots/learning-stages-detail-light.png) | Restored the Back / Every little step header, child name, Learning stages and History tabs, full-width vertical stage panels, inline expansion of skills and word lists, and fixed Save choices footer. |
| History | [`progress-dark.png`](../docs/screenshots/progress-dark.png) | Restored the shared Progress header and tabs, full-width scrolling layout, filter chips, and a single four-stat panel. |
| Settings | [`settings-light.png`](../docs/screenshots/settings-light.png), [`settings-dark.png`](../docs/screenshots/settings-dark.png) | Restored the Back / Make it their own / Save & back header and vertically scrolling settings sections. The saved settings captures predate Learning stages; current `SettingsScreen.kt` supplies the Practice content and Advanced options sections in the HTML baseline. |
| Profiles and Pause | No saved emulator capture | Matched `ProfilesScreen.kt` and the pause dialog in `LittleWordsApp.kt`. These two screens still need visual verification from a newly authorized emulator. |

The saved emulator captures span earlier releases. `HomeScreen.kt`, `PracticeScreen.kt`, `LearningStagesScreen.kt`, `ProgressScreen.kt`, `SettingsScreen.kt`, and `ProfilesScreen.kt` are the source of truth for the current 1.7.1 structure where a capture is older. A live emulator is visible to ADB as `emulator-5580`, but its status is `unauthorized`, so fresh screenshots were unavailable during this review. The HTML is a visual baseline, not a pixel-perfect Android renderer.

The previous, exploratory concept is preserved in [`concept-prototype.html`](concept-prototype.html). It is deliberately separate from the current-UI baseline.


## Approved theme and orientation update

- Dark appearance uses a charcoal/navy background, slate panels, warm white text, and pale slate-blue actions. Light appearance uses warm neutral surfaces and deeper slate-blue actions. Selected controls, links, progress bars, and the launcher use the approved accent.
- Phone Home, Profiles, Settings, Learning stages, and History use portrait layouts. History statistics use two columns and rows can wrap. Reading words/sentences and the Pause overlay stay landscape.
- Controls outside the app canvas let the reviewer compare Phone and Adaptive layouts and appearance. They belong to the review tool, not the proposed app. The browser simulates screen shape; it does not implement Android orientation changes.
- Existing practice rules, scoring defaults, sample content, and navigation destinations are retained. The proposal does not introduce a session-summary feature.
- The pre-change HTML is saved in [prototype-before.html](../../outputs/polish-design-work/prototype-before.html). The earlier concept prototype remains separate.

Android phone orientation and card preservation passed device tests. Home and Settings were inspected in both themes; a narrow 320 dp phone at 1.5× text exposed clipped Home labels, now corrected with a separate profile row and shorter sentence label. See `../docs/VERIFICATION.md` for results and remaining limits.


## Accent alternatives — Slate blue selected

The green/mint accents were rejected for both Light and Dark. The preview now offers three non-green candidates: Iris, Terracotta, and Slate blue (approved default). A review-only Accent row changes actions, selected chips, tab indicators, switches, progress fills/tracks and focus styling across the existing screens. Each option has coordinated light/dark variants and neutral surfaces. Slate blue is now applied to Android. Iris and Terracotta remain review-only alternatives.
