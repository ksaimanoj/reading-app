# Sentence content

All 344 sentences in [the sentence catalogue](../content/sentences.csv) are included in the app's main sentence list. Together they cover all 475 real library words and no silly words. The four explicit helpers are `I`, `a`, `my`, and `to`. The ten most frequent real words account for 22.9% of real-word appearances; [the frequency table](../content/sentence-frequency.csv) lists every word and helper.

There is no parent-approval status or review sheet for sentence inclusion. The compiler checks sentence length, punctuation, allowed words, exact sound tags, complete real-word coverage, and the frequency limit. Each sentence retains a short content note to help with future editing. These automated checks cannot establish every aspect of meaning, dialect, or child suitability.

To change the list, edit `content/sentences.csv`, run `python3 tools/compile_catalog.py`, then run `python3 tools/compile_catalog.py --check` and the tests. The generated `SentenceCatalogData.kt` is the active Android list. The letter, sound-pattern, and stage choices still determine which of those sentences are eligible for a particular session.

Older sentence attempts remain in History after a sentence leaves the active catalogue. Successful historical reads still credit real library words. Sentence progress totals use only the current active list. No database migration is needed.
