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


if __name__ == "__main__":
    unittest.main()
