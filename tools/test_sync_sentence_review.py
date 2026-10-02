import unittest

from tools import sync_sentence_review as review
from tools.compile_catalog import SentenceRow


class SentenceReviewSyncTest(unittest.TestCase):
    def test_keeps_retained_decisions_and_marks_new_sentences_pending(self):
        old = [{
            "source_line": "9", "sentence": "cat sat.", "patterns": "short_a",
            "source_note": "subject and action", "review_status": "approved",
            "phonics_sequence": "yes", "naturalness": "yes", "regional_fit": "yes",
            "child_suitability": "yes", "reviewer": "Parent", "review_date": "2026-09-30",
            "comments": "natural",
        }]
        sentences = [
            SentenceRow(2, "I am glad.", ("short_a", "blends", "tricky"), "first-person statement"),
            SentenceRow(3, "cat sat.", ("short_a",), "subject and action"),
        ]

        rows = review.sync_review_rows(sentences, old)

        self.assertEqual(["I am glad.", "cat sat."], [row["sentence"] for row in rows])
        self.assertEqual("needs_parent_review", rows[0]["review_status"])
        self.assertEqual("", rows[0]["reviewer"])
        self.assertEqual("short_a|blends|tricky", rows[0]["patterns"])
        self.assertEqual("2", rows[0]["source_line"])
        self.assertEqual("approved", rows[1]["review_status"])
        self.assertEqual("Parent", rows[1]["reviewer"])
        self.assertEqual("3", rows[1]["source_line"])

    def test_changed_tags_require_fresh_review(self):
        old = [{"sentence": "cat sat.", "patterns": "short_a", "review_status": "approved",
                "reviewer": "Parent", "review_date": "2026-09-30"}]
        rows = review.sync_review_rows(
            [SentenceRow(2, "cat sat.", ("short_a", "tricky"), "revised tags")], old,
        )
        self.assertEqual("needs_parent_review", rows[0]["review_status"])
        self.assertEqual("", rows[0]["reviewer"])

    def test_duplicate_sentence_text_is_rejected(self):
        sentences = [SentenceRow(2, "cat sat.", ("short_a",), "one"),
                     SentenceRow(3, "cat sat.", ("short_a",), "two")]
        with self.assertRaisesRegex(ValueError, "duplicate sentence"):
            review.sync_review_rows(sentences, [])


if __name__ == "__main__":
    unittest.main()
