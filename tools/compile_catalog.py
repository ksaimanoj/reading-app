#!/usr/bin/env python3
"""Validate the Little Words CSV catalogue and compile deterministic app data."""

from __future__ import annotations

import argparse
import csv
import re
import sys
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Sequence


HEADERS = ("word", "category", "patterns", "note")
CATEGORY_LENGTHS = {
    "TWO_REAL": 2,
    "THREE_REAL": 3,
    "THREE_SILLY": 3,
    "FOUR_REAL": 4,
    "FIVE_REAL": 5,
}
CATEGORY_ORDER = {name: index for index, name in enumerate(CATEGORY_LENGTHS)}
PATTERNS = (
    "short_a",
    "short_e",
    "short_i",
    "short_o",
    "short_u",
    "blends",
    "sh",
    "ch",
    "th",
    "ck",
    "double",
    "tricky",
    "two_syllables",
)
PATTERN_ORDER = {name: index for index, name in enumerate(PATTERNS)}
SHORT_VOWELS = frozenset(PATTERNS[:5])
SILLY_EXCLUSIONS = frozenset({
    "ass", "bra", "bum", "cum", "fag", "fuk", "gay", "god", "hoe",
    "jap", "jew", "jiz", "jus", "kik", "nig", "sex", "tit", "vag", "veg",
    "viz", "vom", "wop", "yuk",
})


@dataclass(frozen=True)
class Row:
    line: int
    word: str
    category: str
    patterns: tuple[str, ...]
    note: str = ""


def parse_csv(path: Path) -> list[Row]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        if tuple(reader.fieldnames or ()) != HEADERS:
            raise ValueError(f"Expected CSV headers {','.join(HEADERS)} in that order.")
        rows = []
        for line, raw in enumerate(reader, start=2):
            rows.append(Row(
                line=line,
                word=(raw["word"] or "").strip(),
                category=(raw["category"] or "").strip(),
                patterns=tuple(part.strip() for part in (raw["patterns"] or "").split("|") if part.strip()),
                note=(raw["note"] or "").strip(),
            ))
        return rows


def validate(rows: Sequence[Row], minimum_size: int = 707, require_coverage: bool = True) -> list[str]:
    errors: list[str] = []
    occurrences: dict[str, list[int]] = {}
    for item in rows:
        occurrences.setdefault(item.word, []).append(item.line)
    duplicates = {word for word, lines in occurrences.items() if len(lines) > 1}

    for item in rows:
        prefix = f"row {item.line}:"
        if not re.fullmatch(r"[a-z]+", item.word):
            errors.append(f"{prefix} word must contain lowercase a-z letters only")
        if item.word in duplicates:
            errors.append(f"{prefix} duplicate word '{item.word}'")
        expected_length = CATEGORY_LENGTHS.get(item.category)
        if expected_length is None:
            errors.append(f"{prefix} unknown category '{item.category}'")
        elif len(item.word) != expected_length:
            errors.append(f"{prefix} category {item.category} requires {expected_length} letters")

        seen_patterns: set[str] = set()
        for pattern in item.patterns:
            if pattern in seen_patterns:
                errors.append(f"{prefix} duplicate pattern '{pattern}'")
            seen_patterns.add(pattern)
            if pattern not in PATTERN_ORDER:
                errors.append(f"{prefix} unknown pattern '{pattern}'")
        if not item.patterns:
            errors.append(f"{prefix} at least one pattern is required")
        if not (set(item.patterns) & SHORT_VOWELS) and "tricky" not in item.patterns:
            errors.append(f"{prefix} non-tricky words require a short-vowel pattern")
        if "tricky" in item.patterns and not item.note:
            errors.append(f"{prefix} tricky words require a review note")
        if item.category == "THREE_SILLY" and item.word in SILLY_EXCLUSIONS:
            errors.append(f"{prefix} word is on the silly-word exclusion list")
        for digraph in ("sh", "ch", "th"):
            if digraph in item.patterns and digraph not in item.word:
                errors.append(f"{prefix} pattern '{digraph}' does not contain '{digraph}' in the word")
        if "ck" in item.patterns and not item.word.endswith("ck"):
            errors.append(f"{prefix} pattern 'ck' does not end in 'ck'")
        if "double" in item.patterns:
            has_double_consonant = any(
                left == right and left not in "aeiou"
                for left, right in zip(item.word, item.word[1:])
            )
            if not has_double_consonant:
                errors.append(f"{prefix} pattern 'double' does not contain a double consonant")

    if len(rows) < minimum_size:
        errors.append(f"catalogue has {len(rows)} entries; expected at least {minimum_size}")
    if require_coverage:
        categories = {item.category for item in rows}
        patterns = {pattern for item in rows for pattern in item.patterns}
        errors.extend(f"catalogue is missing category {name}" for name in CATEGORY_LENGTHS if name not in categories)
        errors.extend(f"catalogue is missing pattern {name}" for name in PATTERNS if name not in patterns)
    return errors


def _sorted(rows: Iterable[Row]) -> list[Row]:
    return sorted(rows, key=lambda item: (CATEGORY_ORDER.get(item.category, 999), item.word))


def render_kotlin(rows: Sequence[Row]) -> str:
    lines = [
        "// Generated by tools/compile_catalog.py. Do not edit by hand.",
        "package com.littlewords.app.domain",
        "",
        "internal object CatalogData {",
        "    val words: List<Word> = listOf(",
    ]
    for item in _sorted(rows):
        patterns = sorted(item.patterns, key=lambda value: PATTERN_ORDER.get(value, 999))
        rendered_patterns = ", ".join(f'\"{value}\"' for value in patterns)
        lines.append(
            f'        Word("{item.word}", Category.{item.category}, setOf({rendered_patterns})),',
        )
    lines.extend(("    )", "}", ""))
    return "\n".join(lines)


def render_report(rows: Sequence[Row]) -> str:
    category_counts = Counter(item.category for item in rows)
    pattern_counts = Counter(pattern for item in rows for pattern in item.patterns)
    lines = [
        "# Generated catalogue report",
        "",
        f"Total entries: **{len(rows)}**",
        "",
        "## Categories",
        "",
        "| Category | Count |",
        "| --- | ---: |",
    ]
    lines.extend(f"| `{name}` | {category_counts[name]} |" for name in CATEGORY_LENGTHS)
    lines.extend(("", "## Patterns", "", "| Pattern | Count |", "| --- | ---: |"))
    lines.extend(f"| `{name}` | {pattern_counts[name]} |" for name in PATTERNS)
    lines.extend((
        "",
        "## Silly-word safety exclusions",
        "",
        ", ".join(f"`{word}`" for word in sorted(SILLY_EXCLUSIONS)),
        "",
        "## Review boundary",
        "",
        "This report covers structural validation. Pronunciation, regional meaning, and child suitability require human review.",
        "",
    ))
    return "\n".join(lines)


def _atomic_write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(f".{path.name}.tmp")
    temporary.write_text(content, encoding="utf-8")
    temporary.replace(path)


def compile_files(
    source: Path,
    kotlin_output: Path,
    report_output: Path,
    *,
    check: bool,
    minimum_size: int = 707,
    require_coverage: bool = True,
) -> bool:
    rows = parse_csv(source)
    errors = validate(rows, minimum_size=minimum_size, require_coverage=require_coverage)
    if errors:
        raise ValueError("\n".join(errors))
    expected = {
        kotlin_output: render_kotlin(rows),
        report_output: render_report(rows),
    }
    if check:
        stale = [path for path, content in expected.items() if not path.exists() or path.read_text(encoding="utf-8") != content]
        for path in stale:
            print(f"stale generated file: {path}", file=sys.stderr)
        return not stale
    for path, content in expected.items():
        _atomic_write(path, content)
    return True


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="verify generated files without changing them")
    parser.add_argument("--minimum-size", type=int, default=707)
    args = parser.parse_args(argv)
    root = Path(__file__).resolve().parents[1]
    try:
        current = compile_files(
            root / "content" / "words.csv",
            root / "app" / "src" / "main" / "java" / "com" / "littlewords" / "app" / "domain" / "CatalogData.kt",
            root / "content" / "catalog-report.md",
            check=args.check,
            minimum_size=args.minimum_size,
        )
    except (OSError, ValueError) as error:
        print(error, file=sys.stderr)
        return 1
    if args.check and not current:
        return 1
    print("catalogue is current" if args.check else "catalogue generated")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
