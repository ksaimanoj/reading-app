# Human review of reading content

The bundled sentence catalogue passes structural checks: sentences use catalogue words, and the sound tags match their words. Those checks cannot establish that a sentence sounds natural, fits a child's dialect and learning sequence, or is suitable for children.

Use [the sentence review sheet](../content/sentence-human-review.csv) to review all 130 sentences before publishing. The 70 rows marked `needs_parent_review` are explicitly called out for parent review in the source catalogue. The other 60 rows are marked `verify_prior_review` because their source notes say “reviewed,” but the repository does not record who reviewed them or when. Neither status means the item is approved for public release.

For each sentence, a parent or phonics educator should record:

1. Whether every word and sound fits the intended teaching sequence.
2. Whether it reads naturally in the target dialect; discuss telegraphic examples such as “man fed hen.” and “dad can jog in sun.” rather than assuming that their short form is suitable.
3. Whether its meaning and imagery are appropriate for the intended child age group and regions.
4. A final decision in `review_status`: `approved`, `revise`, or `reject`, plus reviewer and date. Use `comments` to explain any change.

The review sheet is a tracking artifact. Editing it does not change the app. Apply approved revisions to `content/sentences.csv`, regenerate the catalogues, and rerun the catalogue tests before a release. Keep the sheet aligned with source rows after any CSV edit. Do not describe the full set as “reviewed” in the app or Play listing while entries remain pending.
