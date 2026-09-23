"""Guard the narrow, expiring false-positive exception in the OWASP scan."""

from __future__ import annotations

import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path
from xml.etree import ElementTree


SUPPRESSION_FILE = (
    Path(__file__).resolve().parents[1]
    / ".github/security/owasp-suppressions.xml"
)
NS = {"odc": "https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.4.xsd"}


class OwaspSuppressionsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.rules = ElementTree.parse(SUPPRESSION_FILE).getroot().findall("odc:suppress", NS)

    def test_sqlite_wrapper_exception_is_exact_and_short_lived(self):
        wrapper = [
            rule for rule in self.rules
            if rule.findtext("odc:packageUrl", namespaces=NS)
            == "pkg:maven/io.teaql/teaql-sqlite@1.548-RELEASE"
        ]
        self.assertEqual(1, len(wrapper))
        rule = wrapper[0]
        self.assertEqual(
            "cpe:2.3:a:sqlite:sqlite:1.548:release:*:*:*:*:*:*",
            rule.findtext("odc:cpe", namespaces=NS),
        )
        expiry = datetime.strptime(rule.attrib["until"], "%Y-%m-%dZ").replace(
            tzinfo=timezone.utc
        )
        now = datetime.now(timezone.utc)
        self.assertGreater(expiry, now)
        self.assertLessEqual(expiry, now + timedelta(days=90))
        self.assertIn("org.xerial:sqlite-jdbc", rule.findtext("odc:notes", namespaces=NS))

    def test_no_driver_or_global_severity_waiver(self):
        for rule in self.rules:
            self.assertIsNone(rule.find("odc:cvssScore", NS))
            self.assertNotIn(
                "org.xerial/sqlite-jdbc",
                rule.findtext("odc:packageUrl", default="", namespaces=NS),
            )
            self.assertIsNotNone(rule.get("until"))


if __name__ == "__main__":
    unittest.main()
