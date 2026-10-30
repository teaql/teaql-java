#!/usr/bin/env python3
"""Exercise the all-module release-absence check against an owned HTTP registry."""

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import os
from pathlib import Path
import shutil
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
        self.server.observed.append(self.path)
        sequence = self.server.responses.get(self.path, [404])
        index = min(self.server.observed.count(self.path) - 1, len(sequence) - 1)
        self.send_response(sequence[index])
        self.end_headers()

    def log_message(self, *args):
        pass


class MavenReleaseAbsenceTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="teaql-java-absence-http-")
        self.work = Path(self.temporary.name)
        self.candidate = self.work / "candidate"
        (self.candidate / "tools").mkdir(parents=True)
        shutil.copyfile(ROOT / "tools/verify_maven_release_absence.sh",
                        self.candidate / "tools/verify_maven_release_absence.sh")
        (self.candidate / "pom.xml").write_text(
            "<modules>\n" + "\n".join(f"<module>{name}</module>" for name in MODULES)
            + "\n</modules>\n")
        self.bin = self.work / "bin"
        self.bin.mkdir()
        sleeper = self.bin / "sleep"
        sleeper.write_text("#!/bin/sh\nexit 0\n")
        sleeper.chmod(0o700)
        self.server = ThreadingHTTPServer(("127.0.0.1", 0), RegistryHandler)
        self.server.daemon_threads = True
        self.server.observed = []
        self.server.responses = {}
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()

    def tearDown(self):
        self.server.shutdown()
        self.thread.join(timeout=5)
        self.server.server_close()
        self.assertFalse(self.thread.is_alive())
        self.temporary.cleanup()

    def verify(self):
        env = dict(os.environ,
                   TEAQL_MAVEN_RELEASE_REPOSITORY=(
                       f"http://127.0.0.1:{self.server.server_port}/repository"),
                   PATH=str(self.bin) + os.pathsep + os.environ["PATH"])
        return subprocess.run(["bash", str(self.candidate / "tools/verify_maven_release_absence.sh"),
                               VERSION], cwd=self.work, env=env, capture_output=True,
                              text=True, timeout=30)

    def artifact(self, name):
        return f"/repository/io/teaql/{name}/{VERSION}/{name}-{VERSION}.pom"

    def test_all_parent_and_child_poms_are_absent(self):
        result = self.verify()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(set(self.server.observed),
                         {self.artifact("teaql-java-parent")}
                         | {self.artifact(Path(name).name) for name in MODULES})
        self.assertEqual(len(self.server.observed), 1 + len(MODULES))

    def test_existing_non_core_module_blocks_deployment(self):
        path = self.artifact("teaql-blob-reference")
        self.server.responses[path] = [200]
        result = self.verify()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("already exists", result.stderr)
        self.assertIn(path, result.stderr)
        self.assertEqual(self.server.observed.count(path), 1)

    def test_existing_parent_blocks_deployment(self):
        path = self.artifact("teaql-java-parent")
        self.server.responses[path] = [200]
        result = self.verify()
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(self.server.observed, [path])

    def test_transient_registry_failure_fails_closed(self):
        path = self.artifact("teaql-blob-reference")
        self.server.responses[path] = [503]
        result = self.verify()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Could not prove", result.stderr)
        self.assertEqual(self.server.observed.count(path), 5)

    def test_retry_can_observe_later_absence(self):
        path = self.artifact("teaql-blob-reference")
        self.server.responses[path] = [503, 503, 404]
        result = self.verify()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.server.observed.count(path), 3)


if __name__ == "__main__":
    unittest.main(verbosity=2)
