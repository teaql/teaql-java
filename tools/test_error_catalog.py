import tempfile
import unittest
from pathlib import Path

if __package__:
    from .error_catalog import discover, validate_catalog
else:
    from error_catalog import discover, validate_catalog


class ErrorCatalogTest(unittest.TestCase):
    def test_discovers_multiline_throw_and_legacy_factory(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "sample/src/main/java/io/teaql/Sample.java"
            source.parent.mkdir(parents=True)
            source.write_text(
                '''
class Sample {
  void run() {
    throw new TeaQLRuntimeException(
        "Missing purpose: " + operation);
  }
  RuntimeException legacy() {
    return new DynamicFieldException("DYNAMIC_FIELD_NOT_FOUND", "missing");
  }
}
''',
                encoding="utf-8",
            )

            results = discover(root)

            self.assertEqual(2, len(results))
            self.assertEqual("TeaQLRuntimeException", results[0].exception)
            self.assertEqual(4, results[0].line)
            self.assertEqual("DYNAMIC_FIELD_NOT_FOUND", results[1].legacy_code)

    def test_catalog_validation_detects_duplicates_and_bad_range(self):
        with tempfile.TemporaryDirectory() as directory:
            catalog = Path(directory) / "catalog.yml"
            catalog.write_text(
                '''
numbering:
  first: 1001
  last: 9999
  reuse_retired_ids: false
spaces:
  QRY:
    title: Query
errors:
  - id: TQL-QRY-1001
    name: DUPLICATE
  - id: TQL-QRY-1001
    name: DUPLICATE
  - id: TQL-QRY-1000
    name: TOO_LOW
''',
                encoding="utf-8",
            )

            errors = validate_catalog(catalog)

            self.assertIn("duplicate error ID: TQL-QRY-1001", errors)
            self.assertIn("duplicate symbolic name: DUPLICATE", errors)
            self.assertIn("number outside 1001-9999: TQL-QRY-1000", errors)

    def test_catalog_validation_enforces_allocation_policy(self):
        with tempfile.TemporaryDirectory() as directory:
            catalog = Path(directory) / "catalog.yml"
            catalog.write_text(
                '''
numbering:
  first: 1001
  last: 9999
  reuse_retired_ids: true
spaces:
  QRY:
    title: Query
errors:
  - id: TQL-QRY-1002
    name: SECOND
  - id: TQL-QRY-1001
    name: FIRST
''',
                encoding="utf-8",
            )

            errors = validate_catalog(catalog)

            self.assertIn("numbering.reuse_retired_ids must be false", errors)
            self.assertIn(
                "non-monotonic error ID in QRY: TQL-QRY-1001 follows TQL-QRY-1002",
                errors,
            )

    def test_catalog_validation_rejects_malformed_yaml(self):
        with tempfile.TemporaryDirectory() as directory:
            catalog = Path(directory) / "catalog.yml"
            catalog.write_text(
                '''
numbering:
  first: 1001
  last: 9999
  reuse_retired_ids: false
spaces:
  QRY:
    title: Query
totally: [not valid yaml
errors:
  - id: TQL-QRY-1001
    name: VALID_NAME
''',
                encoding="utf-8",
            )

            errors = validate_catalog(catalog)

            self.assertIn(
                "invalid YAML syntax at line 9: totally: [not valid yaml",
                errors,
            )

    def test_discovery_ignores_comments_and_string_literals(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "sample/src/main/java/io/teaql/Sample.java"
            source.parent.mkdir(parents=True)
            source.write_text(
                '''
class Sample {
  // throw new CommentedException("COMMENTED");
  String example = "throw new StringException(\"STRING\")";
  /* return new LegacyException("LEGACY_COMMENT", "ignored"); */
}
''',
                encoding="utf-8",
            )

            self.assertEqual([], discover(root))


if __name__ == "__main__":
    unittest.main()
