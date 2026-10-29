#!/usr/bin/env python3
"""Exercise the real release verifier against an owned loopback HTTP registry."""

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import os
from pathlib import Path
import subprocess
import tempfile
import threading
import unittest
from xml.etree import ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]
NS = {"m": "http://maven.apache.org/POM/4.0.0"}
POM = ET.parse(ROOT / "pom.xml").getroot()
VERSION = POM.findtext("m:version", namespaces=NS)
MODULES = [node.text for node in POM.findall("m:modules/m:module", NS)
           if not node.text.startswith("examples/")]


class RegistryHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.server.observed.append(("GET", self.path))
        if self.path != self.server.metadata_path:
            self.send_error(404)
            return
        body = ("<metadata><versioning><release>" + self.server.release
                + "</release></versioning></metadata>").encode()
        self.send_response(200)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_HEAD(self):
        self.server.observed.append(("HEAD", self.path))
        self.send_response(200 if self.path in self.server.artifacts else 404)
        self.end_headers()

    def log_message(self, *args):
        pass


class MavenReleaseVerifierTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="teaql-java-release-http-")
        self.work = Path(self.temporary.name)
        self.bin = self.work / "bin"
        self.bin.mkdir()
        # Exercise all native retry requests without a twelve-second fixture wait.
        sleeper = self.bin / "sleep"
        sleeper.write_text("#!/bin/sh\nexit 0\n")
        sleeper.chmod(0o700)
        self.server = ThreadingHTTPServer(("127.0.0.1", 0), RegistryHandler)
        self.server.daemon_threads = True
        self.server.release = VERSION
        self.server.observed = []
        self.server.metadata_path = "/repository/io/teaql/teaql-core/maven-metadata.xml"
        self.server.artifacts = {
            f"/repository/io/teaql/{name}/{VERSION}/{name}-{VERSION}{suffix}"
            for name in MODULES for suffix in (".pom", ".jar", "-sources.jar")
        }
        self.server.artifacts.add(
            f"/repository/io/teaql/teaql-java-parent/{VERSION}/teaql-java-parent-{VERSION}.pom")
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()

    def tearDown(self):
        self.server.shutdown()
        self.thread.join(timeout=5)
        self.server.server_close()
        self.assertFalse(self.thread.is_alive())
        self.temporary.cleanup()

    def verify(self, cwd=None):
        environment = dict(os.environ,
            TEAQL_MAVEN_RELEASE_REPOSITORY=f"http://127.0.0.1:{self.server.server_port}/repository",
            PATH=str(self.bin) + os.pathsep + os.environ["PATH"])
        return subprocess.run(["bash", str(ROOT / "tools/verify_maven_release.sh"), VERSION],
                              cwd=self.work if cwd is None else cwd, env=environment, capture_output=True,
                              text=True, timeout=30)

    def test_all_current_modules_from_other_working_directory(self):
        result = self.verify()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn(f"parent plus {len(MODULES)} child modules", result.stdout)
        heads = [path for method, path in self.server.observed if method == "HEAD"]
        self.assertEqual(set(heads), self.server.artifacts)
        self.assertEqual(len(heads), 1 + len(MODULES) * 3)

    def assert_missing_rejected(self, name, suffix):
        missing = f"/repository/io/teaql/{name}/{VERSION}/{name}-{VERSION}{suffix}"
        self.server.artifacts.remove(missing)
        result = self.verify(ROOT)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Missing release artifact:", result.stderr)
        self.assertIn(missing, result.stderr)
        self.assertEqual(self.server.observed.count(("HEAD", missing)), 5)

    def test_missing_secure_jar_fails_after_bounded_retries(self):
        self.assert_missing_rejected("teaql-secure-document-reference", ".jar")

    def test_missing_blob_sources_fails_after_bounded_retries(self):
        self.assert_missing_rejected("teaql-blob-reference", "-sources.jar")

    def test_metadata_must_name_requested_release(self):
        self.server.release = "unrelated-version"
        result = self.verify()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("metadata does not publish", result.stderr)
        self.assertEqual(self.server.observed, [("GET", self.server.metadata_path)] * 5)


if __name__ == "__main__":
    unittest.main(verbosity=2)
