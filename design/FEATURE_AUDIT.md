# Little Words: feature inventory and design review

Updated 2 October 2026 against the Android source and bundled content. The sentence, scoring, and privacy navigation changes are implemented in Android. The HTML prototype uses illustrative child progress, history, and sentence availability.

## Available features

| Area | Current behavior |
| --- | --- |
| Profiles | Switch children, create a profile (including Testing), and keep each profile's settings, active session, history, and milestones separate. Existing upgraded data appears under Gagan. |
| Word practice | Start or resume a session showing one large word at a time. The selected stages or legacy word groups, letters, and sound patterns determine eligible words. Words missed recently get review priority; stage-based practice also favors less-seen eligible words. |
| Sentence practice | Start or resume practice from 344 bundled sentences covering all 475 real words. All are in the active catalogue. Eligible sentences depend on letters, sound patterns, and patterns supported by selected stage skills. All eligible sentences appear before a repeat. |
| Scoring | Swipe right for an independent read, left for needs practice. A first-card hint explains swipes, and Show/Hide buttons on the reading screen gives immediate access to scoring buttons. Reading time is captured while a card is visible; pause and background time are excluded. |
| Session control | Pause, keep reading, undo the last swipe, save the current place and go home, or end practice. A session retains the choices it started with. |
| Learning stages | Four real-word stages with 16 skills, plus a separate silly-word group. Include whole stages or individual skills in future word practice. View examples, skill explanations, filtered word lists, and available counts. |
| Milestones | Confident means independent reads in two distinct sessions. A later needs-practice outcome flags review without removing confidence. Stage and skill bars show confident counts against the full bundled collection. First independent word, ten confident words, and collection completion are shown. |
| History | Filter attempts by real words, silly words, and sentences. See totals, independent-read rate, expandable sessions, per-card outcome and time, recent attempts, and reading to revisit. Undone swipes are excluded. |
| Settings | System/light/dark appearance; show scoring buttons; mark silly words; choose legacy length groups; restrict sound patterns and individual letters; see eligible sentence count. Stage-based profiles use Advanced options for the sound and letter restrictions. About & privacy explains local storage and deletion. |
| Content and privacy | 707 bundled word entries (475 real, 232 silly), 344 active sentences, local storage, no account or internet permission, and backup disabled. Export/restore is not available. |

The phone app uses portrait navigation and landscape Reading/Pause; tablets and multiwindow follow the available space. The HTML prototype simulates those screen shapes for design review and does not control Android orientation.

## Conflicts and compatibility findings

| Priority | Finding | Evidence and effect | Design decision to explore |
| --- | --- | --- | --- |
| High | Stage selection is not an exact sentence-word boundary. | `SentenceSelector.eligible` compares sentence pattern tags with patterns supported by selected stage skills; it does not check each sentence word's primary stage/skill. A sentence can contain a word assigned to another stage when its sound tags are supported. | The app explains that stages guide sentence practice by supported sounds. Exact word membership remains a future product decision. |
| Resolved | Sentence action with no eligible sentences. | Home now uses the actual sentence selector count, hides the start action at zero, and offers Adjust choices; Settings also explains a zero pool. | Verify the full route on a device. |
| Resolved | All curated sentences belong to the active list. | The generated Android catalogue has 344 sentences. Structural checks confirm 475/475 real-word coverage and 22.9% top-ten real-word share. | Keep the compiler and device checks in the release process; no parent-approval sheet gates inclusion. |
| Partial | Privacy policy is not ready for publication. | Settings now opens an About & privacy summary, but the full policy still needs a publisher contact and a verified public URL. | Finalize and host the policy, then add its link or complete text in the app before Play submission. |
| Medium | Stage progress and the current practice pool use different totals. | Progress bars use all real words in each stage, while filters can leave fewer available. This is intentional and supports stable milestones, but “0 of 40” beside “3 available” can look contradictory. | Pair collection progress with a clearly separate “available for practice” count and explanation. |
| Medium | Stage choices replace legacy word-group selection. | `selectedSubskills == null` uses the old length categories. Saving stage choices sets `selectedSubskills` and widens letters/patterns on first migration, so the old length choices stop controlling selection. | Show a one-time change explanation before saving stage choices on legacy profiles. |
| Medium | Changes to stages or filters do not change a session already in progress. | Sessions save a configuration snapshot. The current card and next cards continue under that snapshot until the session ends. | Label choices “for the next session” whenever a session is active, with an easy way to finish it. |
| Medium | History and stage milestones can appear to disagree. | Successful sentence reads credit their real words, including from retired sentences; missed sentences do not mark each word missed. Silly words never count toward real-word confidence. A confident word can remain confident while flagged “needs review.” | Keep the measures visually distinct and explain the rule near the relevant result. |
| Resolved | Swipe scoring needed discovery and a gesture alternative. | Reading now shows a first-score hint and a persistent Show/Hide buttons control; the choice is saved for the current child. | Verify with TalkBack and enlarged text before release. |
| Low | Turning off silly-word labels can hide the distinction during practice. | Silly words may remain included while `showSillyMarker` is false. | Put a contextual warning beside that setting when silly practice is included. |
| Low | One active session blocks choosing another mode. | Home shows Resume reading in place of the two start actions until the session is ended. | Make the active mode explicit and expose “End session” from the pause flow. |

## Suggested design order

1. Verify the active sentence list and its filtering on the release candidate, then settle the release wording and policy text.
2. Finalize the hosted privacy policy and its in-app destination.
3. Verify narrow phones, tablets, accessibility, and dark/light appearance on the final release candidate.

Source anchors: `ui/LittleWordsApp.kt`, `ui/HomeScreen.kt`, `ui/PracticeScreen.kt`, `ui/LearningStagesScreen.kt`, `ui/ProgressScreen.kt`, `ui/SettingsScreen.kt`, `ui/ProfilesScreen.kt`, `domain/Selector.kt`, `domain/SentenceSelector.kt`, `domain/LearningStages.kt`, `data/ReadingRepository.kt`, and `content/{words,sentences}.csv`.

## Sentence progress and session history — implemented in source

The prototype shows two independent reads of “cat can nap.” in separate sessions. It credits cat, can, and nap as confident words and shows the sentence itself as confident. A sentence marked Needs practice stays at the sentence level because that outcome does not identify which word was difficult. Only real library words receive word credit; `I`, `a`, `my`, and `to` are sentence helpers and do not become word milestones.

Learning stages gains a Sentence progress panel with Not tried, Practising, Confident, and Needs review filters. History filters also apply to the visible session list and expanded rows, as well as totals and recent reading. The prototype values are illustrative and do not represent a child's data.

The Android milestone calculator credits each catalogued real word in a successful sentence, even if that sentence later leaves the active catalogue. Sentence outcomes have their own progress and status filters; the current sentence total uses the 344 active sentences, while History retains older attempts. History filters its session list and expanded rows to the selected content type, and ended sessions show their saved end time plus elapsed duration from start to end. Elapsed duration includes pauses and time away from the app; per-card reading time still excludes them. No schema change was needed. Whether selected stages should limit sentence practice by exact word membership is a separate decision; the existing sound-based rule remains.
