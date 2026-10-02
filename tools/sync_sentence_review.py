#!/usr/bin/env python3
"""Keep sentence-review decisions aligned with the curated sentence CSV."""

from __future__ import annotations

import csv
from pathlib import Path
from typing import Sequence

from tools.compile_catalog import SentenceRow, parse_sentence_csv


REVIEW_HEADERS = (
    "source_line", "sentence", "patterns", "source_note", "review_status",
    "phonics_sequence", "naturalness", "regional_fit", "child_suitability",
    "reviewer", "review_date", "comments",
)


def sync_review_rows(
    sentences: Sequence[SentenceRow], old_rows: Sequence[dict[str, str]],
) -> list[dict[str, str]]:
    old_by_text = {row.get("sentence", ""): row for row in old_rows}
    if len(old_by_text) != len(old_rows):
        raise ValueError("duplicate sentence in old review sheet")
    if len({item.sentence for item in sentences}) != len(sentences):
        raise ValueError("duplicate sentence in sentence catalogue")

    result: list[dict[str, str]] = []
    for item in sentences:
        tags = "|".join(item.patterns)
        old = old_by_text.get(item.sentence)
        same_tags = old is not None and set((old.get("patterns") or "").split("|")) == set(item.patterns)
        row = {header: (old.get(header, "") if same_tags and old else "") for header in REVIEW_HEADERS}
        row.update({
            "source_line": str(item.line),
            "sentence": item.sentence,
            "patterns": tags,
            "source_note": item.note,
            "review_status": (old.get("review_status", "") if same_tags and old else "needs_parent_review"),
        })
        result.append(row)
    return result


def main() -> None:
    root = Path(__file__).resolve().parents[1]
    sentences = parse_sentence_csv(root / "content" / "sentences.csv")
    review_path = root / "content" / "sentence-human-review.csv"
    with review_path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        if tuple(reader.fieldnames or ()) != REVIEW_HEADERS:
            raise ValueError("sentence review sheet headers do not match the expected format")
        old_rows = list(reader)
    rows = sync_review_rows(sentences, old_rows)
    temporary = review_path.with_name(f".{review_path.name}.tmp")
    with temporary.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=REVIEW_HEADERS, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)
    temporary.replace(review_path)
    print(f"synced {len(rows)} sentence-review rows")


if __name__ == "__main__":
    main()
