"""Keep the dialect guide aligned with context-owned schema metadata."""

from pathlib import Path
import unittest


class DialectIntegrationGuideTest(unittest.TestCase):
    def test_schema_example_uses_invoking_context(self):
        guide = (
            Path(__file__).resolve().parents[1] / "DIALECT_INTEGRATION_GUIDE.md"
        ).read_text(encoding="utf-8")
        self.assertIn("SchemaExecutor.Invocation.requireContextOwned(invocation)", guide)
        self.assertIn("EntityMetaFactory.requireFrom(context)", guide)
        self.assertIn("repository.ensurePhysicalSchema(context)", guide)
        self.assertNotIn(
            "EntityMetaFactory.get().allEntityDescriptors()", guide
        )


if __name__ == "__main__":
    unittest.main()
