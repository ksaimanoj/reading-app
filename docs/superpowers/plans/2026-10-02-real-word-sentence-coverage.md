# Real-Word Sentence Coverage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make curated sentence practice cover every real library word while reducing concentration in the ten most repeated real words.

**Architecture:** A checked-in sentence CSV remains the single runtime source. A checked-in helper allowlist, catalogue compiler, and frequency report validate content before generating Kotlin; Android keeps the existing sentence selector and historical progress model, with a case-normalized letter check for `I`.

**Tech Stack:** Python 3 standard library and `unittest`; Kotlin/JUnit; Android Gradle; standalone HTML/JavaScript prototype.

**Spec:** `docs/superpowers/specs/2026-10-02-real-word-sentence-coverage-design.md`

## Global Constraints

- `sources/` in the containing ChatGPT project is read-only.
- Update `design/prototype.html` and obtain user review before changing Android behavior or bundled content (`AGENTS.md`). Preserve its existing uncommitted edits while updating it.
- Cover every real word in `content/words.csv`; exclude every silly word from sentence text.
- Sentence text has 2–8 alphabetic tokens, lowercase library words or uppercase `I`, optional internal comma, and final `.` or `?`.
- Helpers start with `I` and `a`, each tagged `tricky`; any added helper needs a phonics/naturalness note and inclusion in frequency and human review.
- Retain database history and the timestamped Redmi backup; no database migration.
- Final top-ten share of real-word token occurrences is at most 25%; report all real-word counts and helper counts.
- New content is a draft until the parent or educator review sheet records decisions. Public release waits for that review.

## File map

| File | Responsibility |
| --- | --- |
| `design/prototype.html` | Navigable, illustrative design preview and review gate. |
| `content/sentence-helpers.csv` | Explicit helper spellings, tags, and rationale. |
| `content/sentences.csv` | Curated sentence text, exact tags, and content note. |
| `tools/compile_catalog.py` | Parse and validate helpers/sentences; enforce coverage and balance; generate Kotlin and reports. |
| `tools/test_compile_catalog.py` | Unit tests for parser, validation, coverage, counting, and output determinism. |
| `content/sentence-frequency.csv` | Generated complete real-word and helper frequency table. |
| `content/sentence-catalog-report.md` | Generated coverage and concentration summary. |
| `content/sentence-human-review.csv` | Per-sentence parent or educator review decisions. |
| `tools/sync_sentence_review.py` | Carry existing review decisions into the refreshed review sheet by exact sentence text. |
| `tools/test_sync_sentence_review.py` | Ensure review decisions survive reordering and new rows remain pending. |
| `app/src/main/java/com/littlewords/app/domain/SentenceSelector.kt` | Normalize letter case during eligibility checks. |
| `app/src/main/java/com/littlewords/app/domain/SentenceCatalogData.kt` | Generated runtime sentence catalogue. |
| `app/src/test/java/com/littlewords/app/domain/SentenceSelectorTest.kt` | Helper eligibility and no-repeat behavior. |
| `app/src/test/java/com/littlewords/app/domain/LearningStagesTest.kt` | Real-word credit from a successful sentence with helpers. |
| `docs/CONTENT_REVIEW.md` and `design/FEATURE_AUDIT.md` | Describe the updated draft and outstanding human review. |

## Review Focus

1. `I` in a sentence with a selected lowercase `i` must be eligible when `tricky` is enabled; Task 5 tests this.
2. A silly word, unknown token, or lowercase `i` must not bypass the helper allowlist; Task 2 tests these inputs.
3. An internal comma and a question mark must not produce bogus tokens or incorrect tags; Task 2 tests both.
4. Repeating one real word twice within a sentence must add two frequency occurrences, while a helper is counted separately; Task 3 tests this.
5. A successful historical sentence containing a helper must credit only real words, including after that sentence leaves the active catalogue; Task 5 tests this.

---

### Task 1: Update and review the prototype

**Files:** Modify `design/prototype.html`; inspect `design/SCREEN_COMPARISON.md`, `design/FEATURE_AUDIT.md`, and current Compose screens.

**Interfaces:** Produces the reviewed visible examples and explanatory copy for Tasks 4–5; uses illustrative progress only.

- [ ] **Step 1: Inspect the current prototype diff and screen references.** Run `git diff -- design/prototype.html`, then inspect the current practice and progress Compose screens. Keep the earlier prototype work, but remove the stale `341 sentences` and `211 new drafts` claims.
- [ ] **Step 2: Show the proposed content clearly.** In the existing sentence preview data, add examples such as `I am glad.`, `if you can, run.`, and `what is in the box?`; show `475 real words represented` as a proposed library coverage claim, not as child progress. Keep a note that examples await human review.

```javascript
const proposedSentenceExamples = ['I am glad.', 'if you can, run.', 'what is in the box?'];
const proposedCoverageLabel = 'Proposed catalogue: 475 of 475 real words represented · examples await parent review';
```
- [ ] **Step 3: Verify navigation and syntax.** Open the standalone file, navigate Home → sentence practice → Progress → Settings, and run the existing JavaScript syntax check used for this prototype. Confirm no illustrative count is presented as the child's actual history.
- [ ] **Step 4: Show the prototype to the user and wait for design review.** Do not modify `content/` or Android source until the user approves this preview, per `AGENTS.md`.
- [ ] **Step 5: Commit only the approved prototype.** Run `git add design/prototype.html` and `git commit -m 'Preview balanced full-word sentence practice'`.

### Task 2: Add helper and sentence-format validation

**Files:** Create `content/sentence-helpers.csv`; modify `tools/compile_catalog.py`; test `tools/test_compile_catalog.py`.

**Interfaces:** Add `parse_helper_csv(path: Path) -> dict[str, tuple[str, ...]]` and `sentence_tokens(sentence: str) -> list[str]`. Extend `validate_sentences(..., helpers: dict[str, tuple[str, ...]] | None = None, require_real_word_coverage: bool = False)`. Extend `compile_sentence_files(source, word_source, kotlin_output, report_output, *, helper_source: Path, frequency_output: Path, check: bool, minimum_size: int = 120, enforce_full_catalogue: bool = True) -> bool`. Existing small fixtures keep `require_real_word_coverage=False`.

- [ ] **Step 1: Write failing parser/validation tests.** Cover `I am glad.`, `what is in the box?`, and `if you can, run.` with fixture words; assert rejection of lowercase `i`, unknown `zed`, a silly word, unsupported punctuation, a helper without a note, and incorrect union tags. The relevant fixture shape is:

```python
helpers = {"I": ("tricky",), "a": ("tricky",)}
errors = compiler.validate_sentences(
    [sentence_row("I am glad.", "short_a|blends|tricky")],
    [row("am", "TWO_REAL", "short_a"), row("glad", "FOUR_REAL", "short_a|blends")],
    minimum_size=0, helpers=helpers,
)
self.assertEqual([], errors)
```

- [ ] **Step 2: Run `python3 -m unittest tools.test_compile_catalog` and confirm the new tests fail.**
- [ ] **Step 3: Implement the explicit helper file and tokenizer.** Create `content/sentence-helpers.csv` with `token,patterns,note` and rows for `I` and `a`, each with `tricky` and a review rationale. Tokenize after validating exact sentence grammar; allow one comma only between tokens, a final period or question mark, and 2–8 words. For lookup, preserve the surface spelling so lowercase `i` is rejected while library words remain lowercase.

```csv
token,patterns,note
I,tricky,First-person pronoun needed for natural sentences using am
a,tricky,Article needed for natural singular noun phrases
```

```python
SENTENCE_PATTERN = re.compile(r"(?:I|[a-z]+)(?:,? (?:I|[a-z]+)){1,7}[.?]")

def sentence_tokens(sentence: str) -> list[str]:
    if not SENTENCE_PATTERN.fullmatch(sentence) or sentence.count(",") > 1:
        raise ValueError("use 2–8 words, optional internal comma, and final period or question mark")
    return sentence[:-1].replace(",", "").split()
```
- [ ] **Step 4: Update `validate_sentences` to union library and helper tags, reject silly/unknown tokens, require notes, and optionally enforce real-word coverage.** Wire helper loading into `main` and `compile_sentence_files`; retain the small-fixture path for focused tests. Exact error messages should identify the sentence row and offending token or missing words.

```python
real_words = {item.word: set(item.patterns) for item in words if item.category != "THREE_SILLY"}
known_tags = {**real_words, **{token: set(tags) for token, tags in helpers.items()}}
tokens = sentence_tokens(item.sentence)
unknown = [token for token in tokens if token not in known_tags]
expected_patterns = set().union(*(known_tags[token] for token in tokens if token in known_tags))
missing = set(real_words) - {token for sentence in sentences for token in sentence_tokens(sentence.sentence)}
```
- [ ] **Step 5: Run `python3 -m unittest tools.test_compile_catalog`; commit parser, helper CSV, and tests.** The normal catalogue build may remain red until Task 4 supplies full coverage.

### Task 3: Generate and enforce a frequency report

**Files:** Modify `tools/compile_catalog.py` and `tools/test_compile_catalog.py`; generate `content/sentence-frequency.csv` and `content/sentence-catalog-report.md`.

**Interfaces:** Add `sentence_frequency(sentences: Sequence[SentenceRow], words: Sequence[Row], helpers: dict[str, tuple[str, ...]]) -> tuple[Counter[str], Counter[str]]` and `validate_sentence_balance(..., max_top_ten_share: float = 0.25) -> list[str]`. `compile_sentence_files` writes the report and frequency CSV and checks them under `--check`.

- [ ] **Step 1: Write failing tests for all 475-style coverage and token counting.** Use a tiny fixture to assert missing real words are reported, silly words are absent, `cat cat.` counts `cat` twice, `I` is in helper counts, and top-ten share uses real-word tokens as its denominator. Test deterministic output after reversing sentence input order.

```python
real_counts, helper_counts = compiler.sentence_frequency(
    [sentence_row("cat cat.", "short_a"), sentence_row("I am glad.", "short_a|blends|tricky")],
    fixture_words, {"I": ("tricky",)},
)
self.assertEqual(2, real_counts["cat"])
self.assertEqual(1, helper_counts["I"])
```

- [ ] **Step 2: Run `python3 -m unittest tools.test_compile_catalog` and confirm the new assertions fail.**
- [ ] **Step 3: Implement frequency counting and a full distribution CSV.** Include every real library word, even when its count is zero during drafting, with word, stage, count, and rank; add a clearly labeled helper section or a separate `kind` column. Add totals, covered/missing counts, ten highest real-word counts and share, all-token share, and stage distribution to the Markdown report.

```python
real_counts = Counter({item.word: 0 for item in words if item.category != "THREE_SILLY"})
helper_counts = Counter({token: 0 for token in helpers})
for item in sentences:
    for token in sentence_tokens(item.sentence):
        if token in real_counts:
            real_counts[token] += 1
        elif token in helper_counts:
            helper_counts[token] += 1
total_real = sum(real_counts.values())
top_ten_share = (sum(count for _, count in real_counts.most_common(10)) / total_real
                 if total_real else 0.0)
```
- [ ] **Step 4: Make the normal compiler fail on missing coverage or top-ten share above 25%.** Keep unit-fixture compilation able to opt out of these full-catalogue gates. A report-generation command may run without enforcement during curation so the gap is visible; `--check` and normal generation enforce gates.

```python
if require_real_word_coverage and any(count == 0 for count in real_counts.values()):
    errors.append("sentence catalogue omits real words")
if require_balance and top_ten_share > 0.25:
    errors.append(f"top-ten real-word share {top_ten_share:.1%} exceeds 25%")
```
- [ ] **Step 5: Run focused tests and commit compiler/report changes.** Generated final reports are committed with Task 4 after content passes.

### Task 4: Curate full coverage and synchronize human review

**Files:** Modify `content/sentences.csv`, `content/sentence-human-review.csv`; create `tools/sync_sentence_review.py` and `tools/test_sync_sentence_review.py`; generate `app/src/main/java/com/littlewords/app/domain/SentenceCatalogData.kt`, `content/sentence-frequency.csv`, and `content/sentence-catalog-report.md`.

**Interfaces:** Consume `sentence_tokens`, real-word coverage, and frequency checks from Tasks 2–3. The review sync keys on exact sentence text and preserves prior `review_status`, reviewer, date, and comments for retained rows; new or revised rows get `needs_parent_review` with empty decision fields.

- [ ] **Step 1: Write a failing review-sync test.** Given a reordered old sheet containing `cat sat.` and a new catalogue containing `cat sat.` plus `I am glad.`, assert the former retains its review metadata and the latter is pending with the correct `source_line` and tags. Run `python3 -m unittest tools.test_sync_sentence_review` to confirm failure.
- [ ] **Step 2: Implement review sync with the existing CSV headers.** Reject duplicate sentence text and preserve exact prior decisions only for unchanged text; write rows in sentence CSV order. Run the new test and confirm pass.
- [ ] **Step 3: Curate the actual sentences.** Use `../outputs/sentence-rebalance-2026-10-02/proposed-without-top-ten-only.csv` as candidate material, not as accepted content. Add natural 2–8 word sentences for uncovered CVC, digraph, blend, and additional words. For each sentence, write the exact union tags and a substantive note. Use `I`, `a`, or a separately justified helper when library words cannot express a natural sentence. Avoid repeatedly using `can`, `got`, `ran`, `sat`, `in`, `the`, or one template merely to increase count.

```csv
sentence,patterns,note
I am glad.,short_a|blends|tricky,First-person statement using am and a blend word; parent review needed
if you can, run.,short_a|short_i|short_u|tricky,Conditional instruction using if; parent review needed
what is in the box?,short_i|short_o|th|tricky,Question using what; parent review needed
```
- [ ] **Step 4: Run the non-enforcing draft frequency report, inspect every zero-count real word and the highest-frequency words, and revise until normal `python3 tools/compile_catalog.py` passes all coverage and 25% balance checks.** Manually read every sentence for grammar, meaning, sound sequence, and child suitability; mark new content pending rather than approved. Run review sync and check every catalogue row has one review-sheet row.
- [ ] **Step 5: Run `python3 tools/compile_catalog.py --check`, `python3 -m unittest tools.test_compile_catalog tools.test_sync_sentence_review`, and inspect the final frequency table.** Commit the source CSV, review sheet, generated Kotlin, generated reports, sync utility, and tests together.

### Task 5: Keep selection and progress correct with helpers

**Files:** Modify `app/src/main/java/com/littlewords/app/domain/SentenceSelector.kt`, `app/src/test/java/com/littlewords/app/domain/SentenceSelectorTest.kt`, and `app/src/test/java/com/littlewords/app/domain/LearningStagesTest.kt`.

**Interfaces:** `SentenceSelector.eligible(config)` still returns `List<Sentence>`; sentence word credit continues through `WordProgressCalculator.calculate` without adding helpers to `Catalog.words`.

- [ ] **Step 1: Write failing Kotlin tests.** Assert `I am glad.` is eligible only when its tags and letters are selected, `I` matches lowercase `i` in `config.letters`, question/comma sentences remain eligible, all eligible sentences appear before repeats, and an empty pool returns the existing validation message. Add a `WordProgressCalculator` test with successful historical `I am glad.` attempts across sessions: `am` and `glad` progress; `I` never becomes a library milestone.
- [ ] **Step 2: Run `./gradlew :app:testDebugUnitTest` and confirm the new eligibility test fails.**
- [ ] **Step 3: Make the minimal selector change.** Replace the letter predicate with case-normalized comparison, preserving tag and selected-subskill checks:

```kotlin
sentence.text.asSequence().filter { it.isLetter() }
    .all { it.lowercaseChar() in config.letters }
```

- [ ] **Step 4: Update catalogue-format assertions to accept the approved grammar and helper allowlist.** Assert the generated catalogue covers all real words and no silly words, rather than treating every token as a library word.
- [ ] **Step 5: Rerun `./gradlew :app:testDebugUnitTest` and commit the selector and tests.** Do not change database schema or remove historical attempts.

### Task 6: Final documentation and verification

**Files:** Modify `docs/CONTENT_REVIEW.md`, `design/FEATURE_AUDIT.md`, and, if needed, `README.md`.

**Interfaces:** Consume the final generated report and review sheet; document draft/release status and the progress effect of retiring sentences.

- [ ] **Step 1: Update docs with exact final counts and rules.** State 475/475 real-word coverage, the actual final sentence count, observed top-ten share, helper set, pending review count, and that historical attempts remain while active sentence totals use the current catalogue.
- [ ] **Step 2: Run the catalogue compiler check and focused tests once more.** Commands: `python3 tools/compile_catalog.py --check`, `python3 -m unittest tools.test_compile_catalog tools.test_sync_sentence_review`, and `./gradlew :app:testDebugUnitTest`.
- [ ] **Step 3: Inspect `git diff --check`, generated-file status, and review-sheet row parity; commit the documentation.** Report any remaining human-review gate plainly. Do not call the sentence catalogue publicly approved while any included row is pending.
