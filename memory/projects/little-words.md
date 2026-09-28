# Little Words project memory

Updated 28 September 2026.

## What this project is

Little Words is an offline, parent-guided Android reading practice app. It has profiles, word and sentence practice, stage choices and milestones, history, and per-profile settings. The Android implementation lives under `app/`; the bundled content lives under `content/`.

## Design prototype decision

The user wants design settled before further feature enhancements or improvements. `design/prototype.html` is the standing, navigable HTML **visual baseline** for the app. Future user-facing proposals should be shown there first, reviewed with the user, and then carried into the Android app after the design direction is agreed. This prototype does not store or show real child data; its sample state exists only to make screen flows reviewable.

The earlier `design/concept-prototype.html` explored a different layout and was rejected as a depiction of the current Android UI. Keep it clearly separate from the baseline.

## How to use the prototype

1. Open `design/prototype.html` locally and navigate through Home, word/sentence practice, Pause, Learning stages, History, Settings, and Profiles.
2. Before designing a change, check `design/FEATURE_AUDIT.md` for existing behavior and possible conflicts.
3. Compare the relevant HTML screens with `design/SCREEN_COMPARISON.md`, saved emulator images in `docs/screenshots/`, and the current Compose screen files in `app/src/main/java/com/littlewords/app/ui/`.
4. Edit the HTML mock to show proposed wording, hierarchy, states, and navigation. Keep current behavior and proposed changes distinguishable. Avoid presenting sample counts as real child progress.
5. Review the updated mock with the user before implementing the corresponding Android enhancement. After implementation, compare emulator screens with the approved design and record any deliberate differences.

## Verification limits recorded so far

Saved emulator screenshots cover Home, reading, sentence reading, Learning stages, History, and Settings, though some images predate the latest screen code. Profiles and Pause were matched to Compose code and still need fresh visual capture. On 28 September 2026, ADB listed `emulator-5580` as `unauthorized`, so a fresh live screenshot was not captured in that review. The in-app browser control blocked direct inspection of a local `file://` tab; the user could see the HTML after a static fallback was added.

## Open design question

Sentence selection currently follows allowed letters and sound-pattern tags. A selected stage does not require every word in an eligible sentence to belong to that stage. Decide whether future design should explain this sound-based rule or change the rule to exact stage membership before implementing a related enhancement.


## Approved theme and orientation — 28 September 2026

The user rejected green/mint accents, reviewed Iris, Terracotta and Slate blue through GPT-6 Sol High sub-agent work, then approved Slate blue. The prototype defaults to Slate blue; alternate accents remain review-only. Android 1.8 implements coordinated light/dark slate-blue themes, launcher colors, portrait phone navigation and landscape reading/Pause. Tablets and multi-window layouts remain adaptive.

Home navigation, page headers and History statistics now fit portrait. A 320 dp phone with 1.5× text uses a separate profile row and shorter sentence label. Preserve the concurrent sentence-progress/history changes. The Android checks passed 40 local unit tests and 37 device tests, including preserving the card and Pause across recreation/rotation. Build and lint pass. Final visual evidence and limitations are recorded in `docs/VERIFICATION.md`.

The prototype remains a standalone file with illustrative data. Future UI proposals still require design review before Android implementation. Accent report and pre-change backup are under `outputs/polish-design-work/` at the project root.
