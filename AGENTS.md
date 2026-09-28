# Little Words project instructions

## Design-first workflow

- Treat `design/prototype.html` as the navigable **current-UI design baseline** for the Android app. It is a review artifact with illustrative profile data, not production code or a record of actual child progress.
- For user-facing feature enhancements and screen improvements, update the HTML prototype first and let the user review the design before changing Android app behavior or layout. Keep the prototype navigable and usable as a standalone local HTML file.
- Compare proposed screens with `design/SCREEN_COMPARISON.md`, the saved emulator screenshots in `docs/screenshots/`, and the current Compose screen code. When screenshots are from an older release, use current Compose code for behavior and label any unverified visual details.
- Keep `design/concept-prototype.html` separate. It is an earlier exploratory concept and does not represent the current app.
- Use `design/FEATURE_AUDIT.md` when evaluating interactions between features. Resolve design questions in the prototype before implementing related Android changes.
- The Kotlin app and bundled catalogues remain the source of truth for actual behavior, data, and eligibility rules. Do not copy illustrative prototype numbers into production.

The fuller project memory is in `memory/projects/little-words.md`.
