#!/usr/bin/env python3
"""Discover TeaQL error candidates and validate the YAML error catalog.

The implementation uses only the Python standard library. It intentionally
recognizes a narrow YAML/catalog shape so contributors do not need PyYAML just
to run discovery or CI validation.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path


ID_RE = re.compile(r"TQL-([A-Z][A-Z0-9]{1,7})-([0-9]{4})\Z")
THROW_RE = re.compile(r"\bthrow\s+new\s+([A-Za-z_$][\w.$]*)\s*\(")
NEW_EXCEPTION_RE = re.compile(r"\bnew\s+([A-Za-z_$][\w.$]*Exception)\s*\(")
SPACE_LINE_RE = re.compile(r"^  ([A-Z][A-Z0-9]{1,7}):\s*$")
ID_LINE_RE = re.compile(r"^\s*-?\s*id:\s*([^#\s]+)")
NAME_LINE_RE = re.compile(r"^\s+name:\s*([^#\s]+)")
FIRST_RE = re.compile(r"^\s+first:\s*([0-9]+)\s*$")
LAST_RE = re.compile(r"^\s+last:\s*([0-9]+)\s*$")
REUSE_RETIRED_RE = re.compile(r"^\s+reuse_retired_ids:\s*(true|false)\s*$")
TOP_LEVEL_RE = re.compile(r"^(schema|id_format):\s*(\S.*)$")
NUMBERING_LINE_RE = re.compile(
    r"^  (first|last|reuse_retired_ids):\s*(\S.*)$"
)
SPACE_TITLE_RE = re.compile(r"^    title:\s*(\S.*)$")
ERROR_ID_SYNTAX_RE = re.compile(r"^  - id:\s*(\S.*)$")
ERROR_FIELD_RE = re.compile(r"^    ([a-z][a-z0-9_]*):(?:\s*(\S.*))?$")
ERROR_LIST_ITEM_RE = re.compile(r"^      -\s+(\S.*)$")
ERROR_LIST_FIELDS = {"parameters", "legacy_names"}


@dataclass(frozen=True)
class Occurrence:
    file: str
    line: int
    exception: str
    expression: str
    legacy_code: str | None = None


def _balanced_call(source: str, open_paren: int) -> str | None:
    """Return constructor arguments, respecting strings and nested parens."""
    depth = 0
    quote: str | None = None
    escaped = False
    for pos in range(open_paren, len(source)):
        char = source[pos]
        if quote is not None:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == quote:
                quote = None
            continue
        if char in ('"', "'"):
            quote = char
        elif char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return source[open_paren + 1 : pos]
    return None


def _code_mask(source: str) -> str:
    """Mask comments and literals while preserving offsets and line breaks."""
    result = list(source)
    pos = 0
    state = "code"
    quote = ""
    while pos < len(source):
        char = source[pos]
        next_char = source[pos + 1] if pos + 1 < len(source) else ""
        if state == "code":
            if char == "/" and next_char == "/":
                result[pos] = result[pos + 1] = " "
                pos += 2
                state = "line_comment"
                continue
            if char == "/" and next_char == "*":
                result[pos] = result[pos + 1] = " "
                pos += 2
                state = "block_comment"
                continue
            if char in ('"', "'"):
                quote = char
                result[pos] = " "
                pos += 1
                state = "literal"
                continue
        elif state == "line_comment":
            if char == "\n":
                state = "code"
            else:
                result[pos] = " "
            pos += 1
            continue
        elif state == "block_comment":
            if char == "*" and next_char == "/":
                result[pos] = result[pos + 1] = " "
                pos += 2
                state = "code"
                continue
            if char != "\n":
                result[pos] = " "
            pos += 1
            continue
        else:
            if char == "\\":
                result[pos] = " "
                if pos + 1 < len(source):
                    if source[pos + 1] != "\n":
                        result[pos + 1] = " "
                    pos += 2
                    continue
            if char == quote:
                state = "code"
            if char != "\n":
                result[pos] = " "
            pos += 1
            continue
        pos += 1
    return "".join(result)


def _normal(expression: str) -> str:
    return re.sub(r"\s+", " ", expression).strip()


def _fingerprint(exception: str, expression: str, legacy_code: str | None) -> str:
    identity = legacy_code or f"{exception}\0{_normal(expression)}"
    return hashlib.sha256(identity.encode("utf-8")).hexdigest()[:12]


def _space_for(path: str, expression: str, legacy_code: str | None) -> str:
    value = f"{path} {expression} {legacy_code or ''}".lower()
    if "dynamic-field" in value or "dynamic_field" in value:
        return "DYN"
    if "business-id" in value or "business_id" in value or "sequence" in value:
        return "BID"
    if "jackson" in value or "serializ" in value or "query-json" in value:
        return "SER"
    if "tool-http" in value or "external call" in value:
        return "TOL"
    if "context" in value or "capability" in value or "policy" in value:
        return "CTX"
    if any(word in value for word in ("audit", "mutation", "save", "delete", "concurrent")):
        return "MUT"
    if any(word in value for word in ("query", "purpose", "criteria", "operator", "expression")):
        return "QRY"
    if any(word in value for word in ("metadata", "descriptor", "property", "entitymeta")):
        return "MET"
    if any(word in value for word in ("sql", "jdbc", "postgres", "oracle", "sqlite", "mssql", "duckdb", "hana", "snowflake", "db2", "dm8")):
        return "SQL"
    if "teaql-utils" in value:
        return "UTL"
    return "COR"


def discover(root: Path) -> list[Occurrence]:
    occurrences: list[Occurrence] = []
    java_files = sorted(root.glob("*/src/main/java/**/*.java"))
    for file in java_files:
        source = file.read_text(encoding="utf-8")
        code = _code_mask(source)
        relative = file.relative_to(root).as_posix()
        seen_open_parens: set[int] = set()
        for match in THROW_RE.finditer(code):
            args = _balanced_call(source, match.end() - 1)
            if args is None:
                continue
            legacy_match = re.match(r'\s*"([A-Z][A-Z0-9_]{2,})"\s*,', args)
            occurrences.append(
                Occurrence(
                    relative,
                    source.count("\n", 0, match.start()) + 1,
                    match.group(1).split(".")[-1],
                    _normal(args),
                    legacy_match.group(1) if legacy_match else None,
                )
            )
            seen_open_parens.add(match.end() - 1)

        # Factory methods may return an exception instead of throwing it here.
        for match in NEW_EXCEPTION_RE.finditer(code):
            open_paren = match.end() - 1
            if open_paren in seen_open_parens:
                continue
            args = _balanced_call(source, open_paren)
            if args is None:
                continue
            legacy_match = re.match(r'\s*"([A-Z][A-Z0-9_]{2,})"\s*,', args)
            if legacy_match is None:
                continue
            occurrences.append(
                Occurrence(
                    relative,
                    source.count("\n", 0, match.start()) + 1,
                    match.group(1).split(".")[-1],
                    _normal(args),
                    legacy_match.group(1),
                )
            )
    return occurrences


def _yaml_string(value: str) -> str:
    # JSON strings are valid YAML scalars and avoid a hand-written escaper.
    return json.dumps(value, ensure_ascii=False)


def _valid_catalog_scalar(value: str) -> bool:
    """Validate the deliberately small scalar subset used by the catalog."""
    value = value.strip()
    if not value:
        return False
    if value.startswith('"'):
        try:
            return isinstance(json.loads(value), str)
        except (json.JSONDecodeError, TypeError):
            return False
    if value.startswith("'"):
        return len(value) >= 2 and value.endswith("'")
    if value == "[]":
        return True
    if value[0] in "[{&*!|>@`" or ": " in value:
        return False
    return True


def _catalog_syntax_errors(lines: list[str]) -> list[str]:
    """Check the indentation-based YAML subset accepted for catalog.yml."""
    errors: list[str] = []
    section: str | None = None
    current_space = False
    current_error = False
    current_list: str | None = None

    for number, line in enumerate(lines, 1):
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        if "\t" in line[: len(line) - len(line.lstrip())]:
            errors.append(f"invalid YAML syntax at line {number}: tabs are not allowed")
            continue

        indent = len(line) - len(line.lstrip(" "))
        if indent == 0:
            current_space = False
            current_error = False
            current_list = None
            scalar_match = TOP_LEVEL_RE.fullmatch(line)
            if scalar_match:
                if not _valid_catalog_scalar(scalar_match.group(2)):
                    errors.append(f"invalid YAML scalar at line {number}")
                section = None
                continue
            if line in {"numbering:", "spaces:", "errors:", "errors: []"}:
                section = line.split(":", 1)[0]
                continue
            errors.append(f"invalid YAML syntax at line {number}: {line.strip()}")
            section = None
            continue

        if section == "numbering" and indent == 2:
            match = NUMBERING_LINE_RE.fullmatch(line)
            if match and _valid_catalog_scalar(match.group(2)):
                continue
        elif section == "spaces":
            if indent == 2 and SPACE_LINE_RE.fullmatch(line):
                current_space = True
                continue
            if indent == 4 and current_space:
                match = SPACE_TITLE_RE.fullmatch(line)
                if match and _valid_catalog_scalar(match.group(1)):
                    continue
        elif section == "errors":
            if indent == 2:
                match = ERROR_ID_SYNTAX_RE.fullmatch(line)
                if match and _valid_catalog_scalar(match.group(1)):
                    current_error = True
                    current_list = None
                    continue
            elif indent == 4 and current_error:
                match = ERROR_FIELD_RE.fullmatch(line)
                if match:
                    field, value = match.groups()
                    if field in ERROR_LIST_FIELDS and value is None:
                        current_list = field
                        continue
                    if value is not None and _valid_catalog_scalar(value):
                        current_list = None
                        continue
            elif indent == 6 and current_error and current_list:
                match = ERROR_LIST_ITEM_RE.fullmatch(line)
                if match and _valid_catalog_scalar(match.group(1)):
                    continue

        errors.append(f"invalid YAML syntax at line {number}: {line.strip()}")
    return errors


def write_candidates(root: Path, occurrences: list[Occurrence], output) -> None:
    grouped: dict[str, list[Occurrence]] = defaultdict(list)
    for item in occurrences:
        grouped[_fingerprint(item.exception, item.expression, item.legacy_code)].append(item)

    output.write("schema: teaql-error-candidates/v1\n")
    output.write(f"source_root: {_yaml_string(root.resolve().as_posix())}\n")
    output.write("allocation: human-review-required\n")
    space_counts = Counter(
        _space_for(items[0].file, items[0].expression, items[0].legacy_code)
        for items in grouped.values()
    )
    output.write("summary:\n")
    output.write(f"  occurrences: {len(occurrences)}\n")
    output.write(f"  candidate_groups: {len(grouped)}\n")
    output.write("  by_suggested_space:\n")
    for space, count in sorted(space_counts.items()):
        output.write(f"    {space}: {count}\n")
    output.write("candidates:\n")
    ordered_groups = sorted(
        grouped.items(),
        key=lambda pair: (
            _space_for(
                pair[1][0].file,
                pair[1][0].expression,
                pair[1][0].legacy_code,
            ),
            pair[1][0].legacy_code or "",
            pair[1][0].file,
            pair[1][0].line,
        ),
    )
    for fingerprint, items in ordered_groups:
        first = items[0]
        output.write(f"  - fingerprint: {fingerprint}\n")
        output.write("    review_status: unreviewed\n")
        output.write(f"    suggested_space: {_space_for(first.file, first.expression, first.legacy_code)}\n")
        output.write(f"    exception: {_yaml_string(first.exception)}\n")
        if first.legacy_code:
            output.write(f"    legacy_code: {_yaml_string(first.legacy_code)}\n")
        output.write(f"    expression: {_yaml_string(first.expression)}\n")
        output.write("    occurrences:\n")
        for item in items:
            output.write(f"      - file: {_yaml_string(item.file)}\n")
            output.write(f"        line: {item.line}\n")


def validate_catalog(path: Path) -> list[str]:
    lines = path.read_text(encoding="utf-8").splitlines()
    spaces = {match.group(1) for line in lines if (match := SPACE_LINE_RE.match(line))}
    ids = [match.group(1) for line in lines if (match := ID_LINE_RE.match(line))]
    names = [match.group(1) for line in lines if (match := NAME_LINE_RE.match(line))]
    first = next((int(m.group(1)) for line in lines if (m := FIRST_RE.match(line))), None)
    last = next((int(m.group(1)) for line in lines if (m := LAST_RE.match(line))), None)
    reuse_retired = next(
        (m.group(1) for line in lines if (m := REUSE_RETIRED_RE.match(line))), None
    )
    errors = _catalog_syntax_errors(lines)

    if first is None or last is None or first > last:
        errors.append("numbering.first and numbering.last must define a valid range")
        first, last = 1001, 9999
    if not spaces:
        errors.append("at least one error space must be declared")
    if reuse_retired != "false":
        errors.append("numbering.reuse_retired_ids must be false")

    for duplicate, count in Counter(ids).items():
        if count > 1:
            errors.append(f"duplicate error ID: {duplicate}")
    for duplicate, count in Counter(names).items():
        if count > 1:
            errors.append(f"duplicate symbolic name: {duplicate}")

    last_id_by_space: dict[str, tuple[int, str]] = {}
    for value in ids:
        match = ID_RE.fullmatch(value)
        if not match:
            errors.append(f"invalid error ID: {value}")
            continue
        space, number_text = match.groups()
        number = int(number_text)
        if space not in spaces:
            errors.append(f"undeclared error space in {value}: {space}")
        if not first <= number <= last:
            errors.append(f"number outside {first}-{last}: {value}")
        previous = last_id_by_space.get(space)
        if previous is not None and number < previous[0]:
            errors.append(
                f"non-monotonic error ID in {space}: {value} follows {previous[1]}"
            )
        if previous is None or number > previous[0]:
            last_id_by_space[space] = (number, value)
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    scan = commands.add_parser("scan", help="write a YAML review inbox")
    scan.add_argument("--root", type=Path, default=Path.cwd())
    scan.add_argument("--output", type=Path)
    validate = commands.add_parser("validate", help="validate catalog IDs")
    validate.add_argument("catalog", type=Path)
    args = parser.parse_args(argv)

    if args.command == "scan":
        occurrences = discover(args.root.resolve())
        if args.output:
            args.output.parent.mkdir(parents=True, exist_ok=True)
            with args.output.open("w", encoding="utf-8") as stream:
                write_candidates(args.root, occurrences, stream)
            destination = args.output.as_posix()
        else:
            write_candidates(args.root, occurrences, sys.stdout)
            destination = "stdout"
        unique = {
            _fingerprint(item.exception, item.expression, item.legacy_code)
            for item in occurrences
        }
        print(
            f"discovered {len(occurrences)} occurrences in {len(unique)} candidate groups; wrote {destination}",
            file=sys.stderr,
        )
        return 0

    errors = validate_catalog(args.catalog)
    if errors:
        for error in errors:
            print(f"error: {error}", file=sys.stderr)
        return 1
    print(f"valid catalog: {args.catalog} ({len(args.catalog.read_text(encoding='utf-8').splitlines())} lines)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
