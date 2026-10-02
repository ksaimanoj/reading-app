# Real-word sentence coverage and balance

Date: 2026-10-02

## Purpose and agreed scope

Sentence practice should give the learner a chance to read every real word in the bundled word library, while relying less on a few repeatedly used words. The parent wants natural, varied sentences and places no ceiling on the number of sentences. Silly words remain in word-only practice. Prior reading frequency does not constrain which catalogue sentences may be retired.

The current app has 130 sentences covering 65 of 475 real words. The earlier, unshipped 333-sentence pruned draft covers 97 real words and leaves 378 absent. Its ten most frequent real words account for 39.8% of its real-word tokens; the current app's share is 47.5%. The pruned draft is a source of candidates, not the replacement catalogue.

## Chosen approach

Build one reviewed, curated sentence catalogue. Retain useful current and draft sentences, remove repeated constructions that add little coverage or context, and write sentences for the remaining real words. Prefer two distinct contexts per word when they read naturally, but require at least one sentence per real word. Add as many sentences as improve coverage or variety without repeating the same small vocabulary unnecessarily.

Runtime sentence generation is out of scope: it would make grammar, review, and the frequency distribution harder to control. Selector weighting alone cannot expose words absent from the catalogue. Existing selection behavior, which presents every eligible sentence before repeating one, remains the starting behavior.

## Sentence format and source data

`content/words.csv` remains the source of the 475 real words and 232 silly words. `content/sentences.csv` remains the source of sentence text, sound patterns, and review notes. Each sentence has 2–8 alphabetic word tokens. Library words use their existing lowercase spelling; uppercase `I` is allowed as a pronoun. A sentence ends in a period or question mark and may use an internal comma followed by a space where grammar needs one. Other punctuation is excluded. Sentence text must be unique.

An explicit, checked-in helper-word allowlist supports natural sentences without adding those helpers to the practice word library. It starts with `I` and `a`, both tagged `tricky`. Additional helpers may be added only with a phonics and naturalness note explaining the need; each is counted in the frequency report and included in human review. A helper's sound tags participate in sentence eligibility just like the tags of library words. Prefer library words when they make an equally natural sentence.

The compiler tokenizes sentence text case-insensitively for lookup, accepts only real library words or allowlisted helpers, and checks that a sentence's tags exactly equal the union of its words' and helpers' tags. It rejects silly words, unknown tokens, unsupported punctuation, duplicate sentence text, and missing review notes. The normal catalogue build requires every one of the 475 real words to appear in at least one sentence. If the library changes, the check uses the new set of real words rather than a hard-coded list.

## App behavior and historical data

The compiled Kotlin catalogue continues to supply sentence text and sound tags. `SentenceSelector` keeps its existing sound and selected-letter filtering, normalizing letter case so `I` is tested as `i`. A helper tagged `tricky` therefore appears only when that pattern is enabled. The existing empty-pool message remains available when the selected sounds or letters exclude every sentence.

Successful sentence attempts continue to credit only real words in `Catalog.words`; helpers receive no word milestone. The app database needs no schema change. Retired sentences remain in historical reading attempts, while current sentence progress totals use the active catalogue. Historical successful reads can continue to contribute to real-word milestones under the existing progress rule.

## Balance report and acceptance criteria

Generate a reproducible report from the final sentence CSV, with per-word sentence frequency, distribution by learning stage, the ten most frequent words and their share, and a separate helper-word count. Count each token occurrence, including a repeated word within one sentence. Report both real-word-only and all-token concentration so helper use is visible.

The required checks are:

1. All real library words have sentence frequency at least one; silly words have frequency zero.
2. Sentence text is unique and passes format, token, tag, and note validation.
3. The final ten most frequent real words account for no more than 25% of all real-word token occurrences. If naturalness review makes that target impossible, revise the pool and report the tradeoff before considering a different threshold.
4. The report lists the full frequency distribution, not only the top ten, so words with very little exposure remain visible.
5. Eligible sentences still complete a pass before any repeat for a fixed practice configuration.

## Review and delivery sequence

First update `design/prototype.html` to reflect the proposed catalogue and helper/punctuation examples, compare it with the current Compose behavior and saved design references, and obtain user review before changing Android behavior or bundled content. The prototype uses illustrative progress and must not be treated as a record of the child's reads.

Maintain `content/sentence-human-review.csv` for every retained and new sentence. A parent or phonics educator checks sound sequence, naturalness, dialect, meaning, and age suitability, then records `approved`, `revise`, or `reject` with reviewer and date. Structural validation does not constitute that human approval. The catalogue can be prepared and tested as a draft; public release waits until the included content has completed review.

Implementation checks cover helper and punctuation validation, full coverage and frequency reporting, selector eligibility with uppercase `I`, unchanged repeat behavior, and word credit from successful sentences. Regenerate the Kotlin catalogue and reports, run the catalogue checks and focused Android tests, and compare the final frequency report with the baseline above. Preserve the timestamped Redmi database backup as the source record; this work does not modify that backup.
