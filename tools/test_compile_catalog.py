import csv
import tempfile
import unittest
from pathlib import Path

from tools import compile_catalog as compiler


def row(word, category, patterns, note="", line=2):
    return compiler.Row(
        line=line,
        word=word,
        category=category,
        patterns=tuple(patterns.split("|")) if patterns else (),
        note=note,
    )


def sentence_row(sentence, patterns, note="reviewed", line=2):
    return compiler.SentenceRow(
        line=line,
        sentence=sentence,
        patterns=tuple(patterns.split("|")) if patterns else (),
        note=note,
    )


class CatalogueCompilerTest(unittest.TestCase):
    def test_stage_membership_requires_explicit_reviewed_group(self):
        item = row("cat", "THREE_REAL", "short_a")
        self.assertTrue(any("primary stage" in error for error in compiler.validate([item], minimum_size=0, require_coverage=False, require_stage_membership=True)))

    def test_stage_membership_rejects_misclassified_examples(self):
        items = [
            compiler.Row(2, "ship", "FOUR_REAL", ("short_i", "sh"), "", "cvc", "short_i"),
            compiler.Row(3, "brush", "FIVE_REAL", ("short_u", "blends", "sh"), "", "blends", "plain"),
        ]
        errors = compiler.validate(items, minimum_size=0, require_coverage=False, require_stage_membership=True)
        self.assertTrue(any("row 2" in error and "primary stage" in error for error in errors))
        self.assertTrue(any("row 3" in error and "primary subskill" in error for error in errors))

    def test_duplicate_word_names_both_rows(self):
        rows = [
            row("cat", "THREE_REAL", "short_a", line=2),
            row("cat", "THREE_SILLY", "short_a", line=8),
        ]

        errors = compiler.validate(rows, minimum_size=0)

        self.assertTrue(any("row 2" in error and "duplicate word 'cat'" in error for error in errors))
        self.assertTrue(any("row 8" in error and "duplicate word 'cat'" in error for error in errors))

    def test_rejects_invalid_text_and_category_length(self):
        errors = compiler.validate(
            [row("Cat!", "THREE_REAL", "short_a"), row("cats", "THREE_REAL", "short_a", line=3)],
            minimum_size=0,
        )

        self.assertTrue(any("lowercase a-z" in error for error in errors))
        self.assertTrue(any("row 3" in error and "requires 3 letters" in error for error in errors))

    def test_rejects_unknown_and_duplicate_patterns(self):
        errors = compiler.validate(
            [row("cat", "THREE_REAL", "short_a|short_a|magic")],
            minimum_size=0,
        )

        self.assertTrue(any("duplicate pattern 'short_a'" in error for error in errors))
        self.assertTrue(any("unknown pattern 'magic'" in error for error in errors))

    def test_non_tricky_words_require_a_short_vowel_pattern(self):
        errors = compiler.validate([row("cat", "THREE_REAL", "blends")], minimum_size=0)

        self.assertTrue(any("short-vowel pattern" in error for error in errors))

    def test_tricky_only_words_require_a_review_note(self):
        errors = compiler.validate([row("one", "THREE_REAL", "tricky")], minimum_size=0)

        self.assertTrue(any("review note" in error for error in errors))
        self.assertEqual([], compiler.validate(
            [row("one", "THREE_REAL", "tricky", "common irregular")],
            minimum_size=0,
            require_coverage=False,
        ))

    def test_structural_patterns_must_appear_in_spelling(self):
        rows = [
            row("cat", "THREE_REAL", "short_a|sh", line=2),
            row("chat", "FOUR_REAL", "short_a|ck", line=3),
            row("fish", "FOUR_REAL", "short_i|double", line=4),
        ]

        errors = compiler.validate(rows, minimum_size=0)

        self.assertTrue(any("row 2" in error and "does not contain 'sh'" in error for error in errors))
        self.assertTrue(any("row 3" in error and "does not end in 'ck'" in error for error in errors))
        self.assertTrue(any("row 4" in error and "does not contain a double consonant" in error for error in errors))

    def test_silly_words_are_checked_against_the_explicit_exclusion_list(self):
        errors = compiler.validate([row("fuk", "THREE_SILLY", "short_u")], minimum_size=0)

        self.assertTrue(any("silly-word exclusion list" in error for error in errors))

    def test_minimum_size_and_required_coverage_are_reported(self):
        errors = compiler.validate([row("cat", "THREE_REAL", "short_a")], minimum_size=707)

        self.assertTrue(any("at least 707" in error for error in errors))
        self.assertTrue(any("missing category TWO_REAL" in error for error in errors))
        self.assertTrue(any("missing pattern short_e" in error for error in errors))

    def test_rendering_is_deterministic(self):
        first = compiler.render_kotlin([
            row("cat", "THREE_REAL", "short_a"),
            row("am", "TWO_REAL", "short_a"),
        ])
        second = compiler.render_kotlin([
            row("am", "TWO_REAL", "short_a"),
            row("cat", "THREE_REAL", "short_a"),
        ])

        self.assertEqual(first, second)
        self.assertLess(first.index('Word("am"'), first.index('Word("cat"'))

    def test_report_records_the_silly_word_safety_boundary(self):
        report = compiler.render_report([row("cat", "THREE_REAL", "short_a")])

        self.assertIn("Silly-word safety exclusions", report)
        self.assertIn("`fuk`", report)

    def test_parse_requires_exact_headers(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "words.csv"
            path.write_text("word,category,patterns\ncat,THREE_REAL,short_a\n", encoding="utf-8")

            with self.assertRaisesRegex(ValueError, "headers"):
                compiler.parse_csv(path)

    def test_compile_check_detects_stale_output_without_rewriting(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "words.csv"
            kotlin = root / "CatalogData.kt"
            report = root / "catalog-report.md"
            with source.open("w", encoding="utf-8", newline="") as handle:
                writer = csv.DictWriter(handle, fieldnames=compiler.HEADERS)
                writer.writeheader()
                writer.writerow({"word": "cat", "category": "THREE_REAL", "patterns": "short_a", "note": ""})

            compiler.compile_files(source, kotlin, report, check=False, minimum_size=0, require_coverage=False)
            kotlin.write_text("stale", encoding="utf-8")

            self.assertFalse(compiler.compile_files(source, kotlin, report, check=True, minimum_size=0, require_coverage=False))
            self.assertEqual("stale", kotlin.read_text(encoding="utf-8"))

    def test_sentence_validation_requires_reviewed_real_words_and_exact_patterns(self):
        words = [
            row("cat", "THREE_REAL", "short_a"),
            row("sat", "THREE_REAL", "short_a", line=3),
        ]

        self.assertEqual([], compiler.validate_sentences(
            [sentence_row("cat sat.", "short_a")], words, minimum_size=0,
        ))
        unknown = compiler.validate_sentences(
            [sentence_row("cat zed.", "short_a")], words, minimum_size=0,
        )
        wrong_patterns = compiler.validate_sentences(
            [sentence_row("cat sat.", "short_i")], words, minimum_size=0,
        )

        self.assertTrue(any("not in the reviewed real-word catalogue" in error for error in unknown))
        self.assertTrue(any("patterns must exactly match" in error for error in wrong_patterns))

    def test_sentence_rendering_is_deterministic(self):
        first = compiler.render_sentence_kotlin([
            sentence_row("dad sat.", "short_a"),
            sentence_row("cat ran.", "short_a"),
        ])
        second = compiler.render_sentence_kotlin([
            sentence_row("cat ran.", "short_a"),
            sentence_row("dad sat.", "short_a"),
        ])

        self.assertEqual(first, second)
        self.assertLess(first.index('Sentence("cat ran."'), first.index('Sentence("dad sat."'))

    def test_sentence_validation_accepts_helpers_questions_and_commas(self):
        words = [
            row("am", "TWO_REAL", "short_a"), row("glad", "FOUR_REAL", "short_a|blends"),
            row("what", "FOUR_REAL", "tricky"), row("is", "TWO_REAL", "tricky"),
            row("in", "TWO_REAL", "short_i"), row("the", "THREE_REAL", "th|tricky"),
            row("box", "THREE_REAL", "short_o"), row("if", "TWO_REAL", "short_i"),
            row("you", "THREE_REAL", "tricky"), row("can", "THREE_REAL", "short_a"),
            row("run", "THREE_REAL", "short_u"),
        ]
        sentences = [
            sentence_row("I am glad.", "short_a|blends|tricky"),
            sentence_row("what is in the box?", "short_i|short_o|th|tricky", line=3),
            sentence_row("if you can, run.", "short_a|short_i|short_u|tricky", line=4),
        ]
        self.assertEqual([], compiler.validate_sentences(
            sentences, words, minimum_size=0, helpers={"I": ("tricky",), "a": ("tricky",)},
        ))
        self.assertEqual(["if", "you", "can", "run"], compiler.sentence_tokens("if you can, run."))

    def test_sentence_validation_rejects_unreviewed_tokens_and_bad_format(self):
        words = [row("am", "TWO_REAL", "short_a"), row("glad", "FOUR_REAL", "short_a|blends"),
                 row("bem", "THREE_SILLY", "short_e")]
        helpers = {"I": ("tricky",)}
        for text in ("i am glad.", "zed am glad.", "bem am glad."):
            with self.subTest(text=text):
                errors = compiler.validate_sentences(
                    [sentence_row(text, "short_a|blends|tricky")], words,
                    minimum_size=0, helpers=helpers,
                )
                self.assertTrue(any("not in the reviewed real-word catalogue" in error for error in errors))
        errors = compiler.validate_sentences(
            [sentence_row("I am glad!", "short_a|blends|tricky")], words,
            minimum_size=0, helpers=helpers,
        )
        self.assertTrue(any("two to eight" in error for error in errors))
        errors = compiler.validate_sentences(
            [sentence_row("I am glad.", "short_a|blends")], words,
            minimum_size=0, helpers=helpers,
        )
        self.assertTrue(any("patterns must exactly match" in error for error in errors))

    def test_helper_csv_requires_a_review_note(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "helpers.csv"
            source.write_text("token,patterns,note\nI,tricky,\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "review note"):
                compiler.parse_helper_csv(source)

    def test_helper_allowlist_cannot_legalize_silly_word_or_lowercase_i(self):
        words = [row("am", "TWO_REAL", "short_a"), row("bem", "THREE_SILLY", "short_e")]
        for token in ("bem", "i"):
            with self.subTest(token=token):
                errors = compiler.validate_sentences(
                    [sentence_row(f"{token} am.", "short_a|tricky")], words,
                    minimum_size=0, helpers={token: ("tricky",)},
                )
                self.assertTrue(any("helper" in error and token in error for error in errors))
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "helpers.csv"
            source.write_text("token,patterns,note\ni,tricky,Invalid lowercase pronoun\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "lowercase 'i'"):
                compiler.parse_helper_csv(source)

    def test_sentence_validation_can_require_every_real_word(self):
        errors = compiler.validate_sentences(
            [sentence_row("cat cat.", "short_a")],
            [row("cat", "THREE_REAL", "short_a"), row("sat", "THREE_REAL", "short_a")],
            minimum_size=0, require_real_word_coverage=True,
        )
        self.assertTrue(any("sat" in error for error in errors))

    def test_sentence_frequency_counts_occurrences_and_excludes_silly_words(self):
        words = [row("cat", "THREE_REAL", "short_a"), row("am", "TWO_REAL", "short_a"),
                 row("glad", "FOUR_REAL", "short_a|blends"), row("sat", "THREE_REAL", "short_a"),
                 row("bem", "THREE_SILLY", "short_e")]
        sentences = [sentence_row("cat cat.", "short_a"),
                     sentence_row("I am glad.", "short_a|blends|tricky")]
        real_counts, helper_counts = compiler.sentence_frequency(sentences, words, {"I": ("tricky",)})
        self.assertEqual(2, real_counts["cat"])
        self.assertEqual(0, real_counts["sat"])
        self.assertNotIn("bem", real_counts)
        self.assertEqual(1, helper_counts["I"])
        self.assertEqual(4, sum(real_counts.values()))
        self.assertEqual(1, sum(helper_counts.values()))

    def test_balance_uses_real_word_denominator_even_with_many_helpers(self):
        tokens = "bat bed big bug bun bus cab cat cot cup cut dad den dig dog dot dug fan fed fin".split()
        words = [row(token, "THREE_REAL", "short_a") for token in tokens]
        sentences = [sentence_row(f"I {tokens[index]} I {tokens[index + 1]}.", "short_a|tricky")
                     for index in range(0, 20, 2)]
        errors = compiler.validate_sentence_balance(sentences, words, {"I": ("tricky",)})
        self.assertTrue(any("50.0%" in error and "25%" in error for error in errors))

    def test_sentence_frequency_outputs_are_deterministic(self):
        words = [row("cat", "THREE_REAL", "short_a"), row("sat", "THREE_REAL", "short_a")]
        first = [sentence_row("cat sat.", "short_a"), sentence_row("I sat.", "short_a|tricky")]
        helpers = {"I": ("tricky",)}
        self.assertEqual(
            compiler.render_sentence_frequency_csv(first, words, helpers),
            compiler.render_sentence_frequency_csv(list(reversed(first)), words, helpers),
        )
        self.assertIn("real,cat", compiler.render_sentence_frequency_csv(first, words, helpers))
        self.assertIn("helper,I", compiler.render_sentence_frequency_csv(first, words, helpers))


if __name__ == "__main__":
    unittest.main()
