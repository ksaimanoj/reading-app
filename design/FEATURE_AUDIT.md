# Little Words: feature inventory and design review

Reviewed 28 September 2026 against the Android source and bundled content. This is a design artifact; it does not change the app. The HTML prototype uses illustrative child progress and history.

## Available features

| Area | Current behavior |
| --- | --- |
| Profiles | Switch children, create a profile (including Testing), and keep each profile's settings, active session, history, and milestones separate. Existing upgraded data appears under Gagan. |
| Word practice | Start or resume a session showing one large word at a time. The selected stages or legacy word groups, letters, and sound patterns determine eligible words. Words missed recently get review priority; stage-based practice also favors less-seen eligible words. |
| Sentence practice | Start or resume short-sentence practice from 130 bundled sentences. Eligible sentences depend on letters, sound patterns, and patterns supported by selected stage skills. All eligible sentences appear before a repeat. |
| Scoring | Swipe right for an independent read, left for needs practice. Optional on-screen scoring buttons. Reading time is captured while a card is visible; pause and background time are excluded. |
| Session control | Pause, keep reading, undo the last swipe, save the current place and go home, or end practice. A session retains the choices it started with. |
| Learning stages | Four real-word stages with 16 skills, plus a separate silly-word group. Include whole stages or individual skills in future word practice. View examples, skill explanations, filtered word lists, and available counts. |
| Milestones | Confident means independent reads in two distinct sessions. A later needs-practice outcome flags review without removing confidence. Stage and skill bars show confident counts against the full bundled collection. First independent word, ten confident words, and collection completion are shown. |
| History | Filter attempts by real words, silly words, and sentences. See totals, independent-read rate, expandable sessions, per-card outcome and time, recent attempts, and reading to revisit. Undone swipes are excluded. |
| Settings | System/light/dark appearance; show scoring buttons; mark silly words; choose legacy length groups; restrict sound patterns and individual letters; see eligible sentence count. Stage-based profiles use Advanced options for the sound and letter restrictions. |
| Content and privacy | 707 bundled word entries (475 real, 232 silly), 130 sentences, local storage, no account or internet permission, and backup disabled. Export/restore is not available. |

The phone app requests landscape orientation. The HTML design prototype is responsive so the same information can be reviewed at desktop, tablet, and narrow browser sizes; this is a design exploration, not a change to Android orientation.

## Conflicts and compatibility findings

| Priority | Finding | Evidence and effect | Design decision to explore |
| --- | --- | --- | --- |
| High | Stage selection is not an exact sentence-word boundary. | `SentenceSelector.eligible` compares sentence pattern tags with patterns supported by selected stage skills; it does not check each sentence word's primary stage/skill. Selecting the CVC stage permits `cat sat on mat.` because its sound tags are short vowels, although `on` is catalogued in Additional practice / Two-letter words. | Explain that sentence readiness uses sounds, or change the eventual sentence rule to require selected word membership. The prototype labels sentence availability as sound-based. |
| High | Sentence action can be offered with no eligible sentences. | Home always offers “Try short sentences” when there is no active session. Settings warns at zero, while `startSession` rejects the action only after tap. | Show eligibility and an inline route to adjust choices before starting. The prototype shows this state in Settings and its start control logic. |
| Medium | Stage progress and the current practice pool use different totals. | Progress bars use all real words in each stage, while filters can leave fewer available. This is intentional and supports stable milestones, but “0 of 40” beside “3 available” can look contradictory. | Pair collection progress with a clearly separate “available for practice” count and explanation. |
| Medium | Stage choices replace legacy word-group selection. | `selectedSubskills == null` uses the old length categories. Saving stage choices sets `selectedSubskills` and widens letters/patterns on first migration, so the old length choices stop controlling selection. | Show a one-time change explanation before saving stage choices on legacy profiles. |
| Medium | Changes to stages or filters do not change a session already in progress. | Sessions save a configuration snapshot. The current card and next cards continue under that snapshot until the session ends. | Label choices “for the next session” whenever a session is active, with an easy way to finish it. |
| Medium | History and stage milestones can appear to disagree. | Sentence outcomes and silly words appear in History but do not count toward real-word stage confidence. Also, a confident word can remain confident while flagged “needs review.” | Keep the two measures visually distinct and explain the rule near the relevant result. |
| Low | Turning off silly-word labels can hide the distinction during practice. | Silly words may remain included while `showSillyMarker` is false. | Put a contextual warning beside that setting when silly practice is included. |
| Low | One active session blocks choosing another mode. | Home shows Resume reading in place of the two start actions until the session is ended. | Make the active mode explicit and expose “End session” from the pause flow. |

## Suggested design order

1. Align the Home, Practice, Pause, Learning stages, History, Settings, and Profiles flows in the prototype.
2. Decide whether sentence practice should follow exact stage membership or sound readiness. This changes both wording and future selection behavior.
3. Confirm the empty-sentence, legacy-transition, and active-session states before making Android feature changes.

Source anchors: `ui/LittleWordsApp.kt`, `ui/HomeScreen.kt`, `ui/PracticeScreen.kt`, `ui/LearningStagesScreen.kt`, `ui/ProgressScreen.kt`, `ui/SettingsScreen.kt`, `ui/ProfilesScreen.kt`, `domain/Selector.kt`, `domain/SentenceSelector.kt`, `domain/LearningStages.kt`, `data/ReadingRepository.kt`, and `content/{words,sentences}.csv`.

## Sentence progress and session history — implemented in source

The prototype now shows two independent reads of “cat can nap.” in separate sessions. It credits cat, can, and nap as confident words and shows the sentence itself as confident. A sentence marked Needs practice stays at the sentence level because that outcome does not identify which word was difficult. Only words in the real-word catalogue receive word credit; the current 130 sentences contain no uncatalogued words.

Learning stages gains a Sentence progress panel with Not tried, Practising, Confident, and Needs review filters. History filters also apply to the visible session list and expanded rows, as well as totals and recent reading. The prototype values are illustrative and do not represent a child's data.

The Android milestone calculator now credits each catalogued real word in a successful sentence. Sentence outcomes have their own progress and status filters. History filters its session list and expanded rows to the selected content type, and ended sessions show their saved end time plus elapsed duration from start to end. Elapsed duration includes pauses and time away from the app; per-card reading time still excludes them. The database already saved session end times, so no schema change was needed. Whether selected stages should limit sentence practice by exact word membership is a separate decision; the existing sound-based rule remains.
