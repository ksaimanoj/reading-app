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


HEADERS = ("word", "category", "patterns", "note", "stage", "subskill")
SENTENCE_HEADERS = ("sentence", "patterns", "note")
HELPER_HEADERS = ("token", "patterns", "note")
SENTENCE_PATTERN = re.compile(r"(?:I|[a-z]+)(?:,? (?:I|[a-z]+)){1,7}[.?]")
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
STAGE_SUBSKILLS = {
    "cvc": frozenset(SHORT_VOWELS),
    "digraphs": frozenset({"sh", "ch", "th", "ck", "double", "combined"}),
    "blends": frozenset({"plain", "combined"}),
    "additional": frozenset({"two_letter", "tricky", "two_syllables"}),
    "silly": frozenset({"silly"}),
}
COMPLEX_PATTERNS = frozenset({"sh", "ch", "th", "ck", "double"})
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
    stage: str = ""
    subskill: str = ""


@dataclass(frozen=True)
class SentenceRow:
    line: int
    sentence: str
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
                stage=(raw["stage"] or "").strip(),
                subskill=(raw["subskill"] or "").strip(),
            ))
        return rows


def parse_sentence_csv(path: Path) -> list[SentenceRow]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        if tuple(reader.fieldnames or ()) != SENTENCE_HEADERS:
            raise ValueError(f"Expected CSV headers {','.join(SENTENCE_HEADERS)} in that order.")
        rows = []
        for line, raw in enumerate(reader, start=2):
            rows.append(SentenceRow(
                line=line,
                sentence=(raw["sentence"] or "").strip(),
                patterns=tuple(part.strip() for part in (raw["patterns"] or "").split("|") if part.strip()),
                note=(raw["note"] or "").strip(),
            ))
        return rows


def parse_helper_csv(path: Path) -> dict[str, tuple[str, ...]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        if tuple(reader.fieldnames or ()) != HELPER_HEADERS:
            raise ValueError(f"Expected CSV headers {','.join(HELPER_HEADERS)} in that order.")
        helpers: dict[str, tuple[str, ...]] = {}
        for line, raw in enumerate(reader, start=2):
            token = (raw["token"] or "").strip()
            patterns = tuple(part.strip() for part in (raw["patterns"] or "").split("|") if part.strip())
            note = (raw["note"] or "").strip()
            if not re.fullmatch(r"I|[a-z]+", token):
                raise ValueError(f"helper row {line}: invalid token '{token}'")
            if token in helpers:
                raise ValueError(f"helper row {line}: duplicate helper '{token}'")
            if not patterns or len(set(patterns)) != len(patterns) or any(p not in PATTERN_ORDER for p in patterns):
                raise ValueError(f"helper row {line}: invalid sound patterns")
            if not note:
                raise ValueError(f"helper row {line}: a human review note is required")
            helpers[token] = patterns
        return helpers


def sentence_tokens(sentence: str) -> list[str]:
    if not SENTENCE_PATTERN.fullmatch(sentence) or sentence.count(",") > 1:
        raise ValueError("use two to eight words, an optional internal comma, and a period or question mark")
    return sentence[:-1].replace(",", "").split()


def validate(rows: Sequence[Row], minimum_size: int = 707, require_coverage: bool = True,
             require_stage_membership: bool = False) -> list[str]:
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
        if require_stage_membership:
            expected = reviewed_membership(item)
            if expected is None or item.stage != expected[0]:
                errors.append(f"{prefix} invalid primary stage '{item.stage}' for {item.word}; expected {expected}")
            elif item.subskill != expected[1]:
                errors.append(f"{prefix} invalid primary subskill '{item.subskill}' for {item.word}; expected {expected[1]}")
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


def reviewed_membership(item: Row) -> tuple[str, str] | None:
    """Check explicit assignments against the reviewed structural taxonomy."""
    patterns = set(item.patterns)
    if item.category == "THREE_SILLY":
        return "silly", "silly"
    if "two_syllables" in patterns:
        return "additional", "two_syllables"
    if "tricky" in patterns:
        return "additional", "tricky"
    if item.category == "TWO_REAL":
        return "additional", "two_letter"
    if "blends" in patterns:
        return "blends", "combined" if patterns & COMPLEX_PATTERNS else "plain"
    complex_patterns = patterns & COMPLEX_PATTERNS
    if complex_patterns:
        return "digraphs", next(iter(complex_patterns)) if len(complex_patterns) == 1 else "combined"
    vowel = next(iter(patterns & SHORT_VOWELS), None)
    if (item.category == "THREE_REAL" and len(patterns) == 1 and vowel
            and re.fullmatch(r"[^aeiou][aeiou][^aeiou]", item.word)):
        return "cvc", vowel
    return None


def validate_sentences(
    sentences: Sequence[SentenceRow],
    words: Sequence[Row],
    minimum_size: int = 120,
    helpers: dict[str, tuple[str, ...]] | None = None,
    require_real_word_coverage: bool = False,
) -> list[str]:
    errors: list[str] = []
    occurrences: dict[str, list[int]] = {}
    for item in sentences:
        occurrences.setdefault(item.sentence, []).append(item.line)
    duplicates = {sentence for sentence, lines in occurrences.items() if len(lines) > 1}
    real_words = {item.word: set(item.patterns) for item in words if item.category != "THREE_SILLY"}
    helper_tags = {token: set(patterns) for token, patterns in (helpers or {}).items()}
    for token in helper_tags:
        if token in real_words:
            errors.append(f"helper '{token}' duplicates a real library word")
    known_tags = {**real_words, **helper_tags}
    seen_real_words: set[str] = set()

    for item in sentences:
        prefix = f"sentence row {item.line}:"
        if item.sentence in duplicates:
            errors.append(f"{prefix} duplicate sentence '{item.sentence}'")
        try:
            tokens = sentence_tokens(item.sentence)
        except ValueError as error:
            errors.append(f"{prefix} {error}")
            continue
        if not item.note:
            errors.append(f"{prefix} a human review note is required")

        seen_patterns: set[str] = set()
        for pattern in item.patterns:
            if pattern in seen_patterns:
                errors.append(f"{prefix} duplicate pattern '{pattern}'")
            seen_patterns.add(pattern)
            if pattern not in PATTERN_ORDER:
                errors.append(f"{prefix} unknown pattern '{pattern}'")

        unknown_words = sorted({token for token in tokens if token not in known_tags})
        for word in unknown_words:
            errors.append(f"{prefix} word '{word}' is not in the reviewed real-word catalogue or helper allowlist")
        expected_patterns = set().union(*(known_tags.get(token, set()) for token in tokens))
        if set(item.patterns) != expected_patterns:
            expected = "|".join(sorted(expected_patterns, key=lambda value: PATTERN_ORDER.get(value, 999)))
            errors.append(f"{prefix} patterns must exactly match the sentence words: {expected}")
        seen_real_words.update(token for token in tokens if token in real_words)

    if len(sentences) < minimum_size:
        errors.append(f"sentence catalogue has {len(sentences)} entries; expected at least {minimum_size}")
    if require_real_word_coverage:
        missing = sorted(real_words.keys() - seen_real_words)
        if missing:
            errors.append(f"sentence catalogue is missing {len(missing)} real words: {', '.join(missing)}")
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
            f'        Word("{item.word}", Category.{item.category}, setOf({rendered_patterns}), "{item.stage}", "{item.subskill}"),',
        )
    lines.extend(("    )", "}", ""))
    return "\n".join(lines)


def render_sentence_kotlin(rows: Sequence[SentenceRow]) -> str:
    lines = [
        "// Generated by tools/compile_catalog.py. Do not edit by hand.",
        "package com.littlewords.app.domain",
        "",
        "internal object SentenceCatalogData {",
        "    val sentences: List<Sentence> = listOf(",
    ]
    for item in sorted(rows, key=lambda value: value.sentence):
        patterns = sorted(item.patterns, key=lambda value: PATTERN_ORDER.get(value, 999))
        rendered_patterns = ", ".join(f'\"{value}\"' for value in patterns)
        lines.append(f'        Sentence("{item.sentence}", setOf({rendered_patterns})),')
    lines.extend(("    )", "}", ""))
    return "\n".join(lines)


def render_report(rows: Sequence[Row]) -> str:
    category_counts = Counter(item.category for item in rows)
    pattern_counts = Counter(pattern for item in rows for pattern in item.patterns)
    stage_counts = Counter((item.stage, item.subskill) for item in rows)
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
    lines.extend(("", "## Primary stage and subskill membership", "", "Each entry belongs to one counting group. Silly words are excluded from real-word milestones.", "", "| Stage | Subskill | Count |", "| --- | --- | ---: |"))
    lines.extend(f"| `{stage}` | `{subskill}` | {stage_counts[stage, subskill]} |" for stage, subskills in STAGE_SUBSKILLS.items() for subskill in sorted(subskills) if stage_counts[stage, subskill])
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


def render_sentence_report(rows: Sequence[SentenceRow]) -> str:
    pattern_counts = Counter(pattern for item in rows for pattern in item.patterns)
    lines = [
        "# Generated sentence catalogue report",
        "",
        f"Total curated sentence drafts: **{len(rows)}**",
        "",
        "Every sentence contains two to eight words from the real-word catalogue or explicit helper allowlist.",
        "Its required sound patterns are the exact union of its words' and helpers' tags.",
        "",
        "## Patterns",
        "",
        "| Pattern | Count |",
        "| --- | ---: |",
    ]
    lines.extend(f"| `{name}` | {pattern_counts[name]} |" for name in PATTERNS)
    lines.extend((
        "",
        "## Review boundary",
        "",
        "The compiler verifies structure and word/tag consistency. New sentence drafts still require parent or educator review for meaning, grammar, dialect, and teaching suitability.",
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
    errors = validate(rows, minimum_size=minimum_size, require_coverage=require_coverage,
                      require_stage_membership=minimum_size >= 707)
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


def compile_sentence_files(
    source: Path,
    word_source: Path,
    kotlin_output: Path,
    report_output: Path,
    *,
    helper_source: Path,
    frequency_output: Path,
    check: bool,
    minimum_size: int = 120,
    enforce_full_catalogue: bool = True,
) -> bool:
    rows = parse_sentence_csv(source)
    words = parse_csv(word_source)
    helpers = parse_helper_csv(helper_source)
    errors = validate_sentences(rows, words, minimum_size=minimum_size, helpers=helpers,
                                require_real_word_coverage=enforce_full_catalogue)
    if errors:
        raise ValueError("\n".join(errors))
    expected = {
        kotlin_output: render_sentence_kotlin(rows),
        report_output: render_sentence_report(rows),
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
        word_rows = parse_csv(root / "content" / "words.csv")
        sentence_rows = parse_sentence_csv(root / "content" / "sentences.csv")
        helpers = parse_helper_csv(root / "content" / "sentence-helpers.csv")
        preflight_errors = validate(word_rows, minimum_size=args.minimum_size,
                                    require_stage_membership=True)
        preflight_errors.extend(validate_sentences(sentence_rows, word_rows, helpers=helpers,
                                                  require_real_word_coverage=True))
        if preflight_errors:
            raise ValueError("\n".join(preflight_errors))
        words_current = compile_files(
            root / "content" / "words.csv",
            root / "app" / "src" / "main" / "java" / "com" / "littlewords" / "app" / "domain" / "CatalogData.kt",
            root / "content" / "catalog-report.md",
            check=args.check,
            minimum_size=args.minimum_size,
        )
        sentences_current = compile_sentence_files(
            root / "content" / "sentences.csv",
            root / "content" / "words.csv",
            root / "app" / "src" / "main" / "java" / "com" / "littlewords" / "app" / "domain" / "SentenceCatalogData.kt",
            root / "content" / "sentence-catalog-report.md",
            helper_source=root / "content" / "sentence-helpers.csv",
            frequency_output=root / "content" / "sentence-frequency.csv",
            check=args.check,
        )
    except (OSError, ValueError) as error:
        print(error, file=sys.stderr)
        return 1
    if args.check and not (words_current and sentences_current):
        return 1
    print("catalogues are current" if args.check else "catalogues generated")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
