# Human review of reading content

The 344 bundled sentence drafts pass structural checks. Together they include all 475 real library words and no silly words. Their sound tags match the words and the four explicitly allowed helpers: `I`, `a`, `my`, and `to`. The ten most frequent real words account for 22.9% of real-word appearances. See the [complete frequency table](../content/sentence-frequency.csv) and [summary](../content/sentence-catalog-report.md). These checks cannot establish that a sentence sounds natural, fits a child's dialect and learning sequence, or is suitable for children.

Use [the sentence review sheet](../content/sentence-human-review.csv) to review all 344 sentences before publishing. The 342 new rows are marked `needs_parent_review`. Two retained rows are marked `verify_prior_review` because their source notes say “reviewed,” but the repository does not record who reviewed them or when. None is recorded as approved for public release. The helper words also need a phonics and naturalness check using their notes in [the helper list](../content/sentence-helpers.csv).

For each sentence, a parent or phonics educator should record:

1. Whether every word and sound fits the intended teaching sequence.
2. Whether it reads naturally in the target dialect; pay particular attention to unusual phrases and abstract library words such as `gloss`, `span`, and `tenet`.
3. Whether its meaning and imagery are appropriate for the intended child age group and regions, including words such as `gash`, `pill`, and `sin`.
4. A final decision in `review_status`: `approved`, `revise`, or `reject`, plus reviewer and date. Use `comments` to explain any change.

The review sheet is a tracking artifact. Editing it does not change the app. Apply approved revisions to `content/sentences.csv`, regenerate the catalogues with `python3 tools/compile_catalog.py`, and rerun the catalogue and Android tests before a release. Then run `python3 -m tools.sync_sentence_review` to align the sheet with source rows; unchanged sentence decisions are retained. Do not describe the full set as “reviewed” in the app or Play listing while entries remain pending.

The app retains older sentence attempts in History when a sentence leaves the active catalogue. Word progress still credits real words in successful historical sentence attempts. The current sentence progress total uses only the 344 active sentences, so that total can change after this catalogue update. No database migration is needed.
