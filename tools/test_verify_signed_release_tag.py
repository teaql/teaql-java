#!/usr/bin/env python3
"""Exercise the actual signed-tag gate with valid and invalid Git objects."""

import importlib.util
from pathlib import Path
import unittest


path = Path(__file__).with_name("verify_signed_release_tag.py")
spec = importlib.util.spec_from_file_location("verify_signed_release_tag", path)
subject = importlib.util.module_from_spec(spec)
spec.loader.exec_module(subject)

COMMIT = "a" * 40
TAG_SHA = "b" * 40
TAG = "v1.556-RELEASE"
BASE = "https://api.github.com/repos/teaql/teaql-java"


def valid_responses():
    return {
        BASE + "/git/ref/tags/" + TAG: {
            "ref": "refs/tags/" + TAG, "object": {"type": "tag", "sha": TAG_SHA}},
        BASE + "/git/tags/" + TAG_SHA: {
            "sha": TAG_SHA, "tag": TAG,
            "object": {"type": "commit", "sha": COMMIT},
            "verification": {"verified": True, "reason": "valid", "signature": "test-signature"}},
    }


class SignedReleaseTagTests(unittest.TestCase):
    def run_case(self, responses):
        requested = []
        def get(url):
            requested.append(url)
            return responses[url]
        return subject.verify("teaql/teaql-java", TAG, COMMIT, get), requested

    def test_verified_annotated_tag_points_to_exact_release_commit(self):
        tag_sha, requested = self.run_case(valid_responses())
        self.assertEqual(tag_sha, TAG_SHA)
        self.assertEqual(requested, list(valid_responses()))

    def test_lightweight_tag_cannot_publish(self):
        responses = valid_responses()
        responses[BASE + "/git/ref/tags/" + TAG]["object"] = {"type": "commit", "sha": COMMIT}
        with self.assertRaisesRegex(ValueError, "annotated tag"):
            self.run_case(responses)

    def test_unsigned_annotated_tag_cannot_publish(self):
        responses = valid_responses()
        responses[BASE + "/git/tags/" + TAG_SHA]["verification"] = {
            "verified": False, "reason": "unsigned", "signature": None}
        with self.assertRaisesRegex(ValueError, "verified a signature"):
            self.run_case(responses)

    def test_tag_for_another_commit_cannot_publish(self):
        responses = valid_responses()
        responses[BASE + "/git/tags/" + TAG_SHA]["object"]["sha"] = "c" * 40
        with self.assertRaisesRegex(ValueError, "release commit"):
            self.run_case(responses)

    def test_changed_tag_reference_cannot_publish(self):
        responses = valid_responses()
        responses[BASE + "/git/ref/tags/" + TAG]["ref"] = "refs/tags/vother"
        with self.assertRaisesRegex(ValueError, "different tag reference"):
            self.run_case(responses)

    def test_unverified_signature_cannot_publish(self):
        responses = valid_responses()
        responses[BASE + "/git/tags/" + TAG_SHA]["verification"]["verified"] = False
        with self.assertRaisesRegex(ValueError, "verified a signature"):
            self.run_case(responses)

    def test_verified_flag_without_signature_cannot_publish(self):
        responses = valid_responses()
        responses[BASE + "/git/tags/" + TAG_SHA]["verification"]["signature"] = None
        with self.assertRaisesRegex(ValueError, "verified a signature"):
            self.run_case(responses)


if __name__ == "__main__":
    unittest.main(verbosity=2)
