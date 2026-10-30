#!/usr/bin/env python3
"""Offline guards: cleanup aliases cannot hide mechanical or historical evidence changes."""
import copy
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location("comparison", Path(__file__).with_name("compare-baseline.py"))
comparison = importlib.util.module_from_spec(spec)
spec.loader.exec_module(comparison)


class CatalogComparisonTest(unittest.TestCase):
    def setUp(self):
        self.old = {"argentumCommit": "old", "source": "old/card.kt", "results": [
            {"verdict": "MATCH", "prerequisites": ["Pay the retained cost"], "quantity": 1}],
            "evidence": {"argentumCommit": "recorded", "sha256": "immutable"},
            "url": "https://github.com/jasullivan5/argentum-engine/blob/old/native.kt"}
        self.current = copy.deepcopy(self.old)
        self.current.update(argentumCommit="new", source="src/main/kotlin/card.kt",
                            url="https://github.com/jasullivan5/argentum-engine/blob/new/native.kt")
        self.paths = {"src/main/kotlin/card.kt": "old/card.kt"}

    def normalized(self, value=None):
        return comparison.normalize_report(value or self.current, "new", "old", self.paths)

    def test_only_declared_paths_and_runtime_urls_are_equivalent(self):
        self.assertEqual(self.normalized(), self.old)
        self.assertEqual(self.current["argentumCommit"], "new")  # Input is not mutated.

    def test_mechanical_or_prerequisite_changes_are_not_normalized(self):
        for field, value in (("verdict", "UNKNOWN"), ("quantity", 2), ("prerequisites", [])):
            changed = copy.deepcopy(self.current)
            changed["results"][0][field] = value
            self.assertNotEqual(comparison.canonical_digest(self.normalized(changed)), comparison.canonical_digest(self.old))

    def test_duplicates_and_unlisted_locations_are_not_discarded(self):
        changed = copy.deepcopy(self.current)
        changed["results"] *= 2
        self.assertNotEqual(self.normalized(changed), self.old)
        self.assertNotEqual(comparison.canonical_digest(self.normalized(changed)), comparison.canonical_digest(self.old))
        changed = copy.deepcopy(self.current)
        changed["source"] = "unreviewed/card.kt"
        self.assertNotEqual(self.normalized(changed), self.old)
        self.assertNotEqual(comparison.canonical_digest(self.normalized(changed)), comparison.canonical_digest(self.old))

    def test_historical_revision_and_evidence_hash_remain_bound(self):
        for field in ("argentumCommit", "sha256"):
            changed = copy.deepcopy(self.current)
            changed["evidence"][field] = "changed"
            self.assertNotEqual(self.normalized(changed), self.old)

    def test_mismatches_and_mixed_runtime_revisions_fail_the_gate(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            def write(name, value):
                (root / name).write_text(json.dumps(value), encoding="utf-8")
            baseline = {"argentumCommit": "old", "reports": {name: {
                "sha256CanonicalJsonUtf8": comparison.canonical_digest(self.old)} for name in ("a", "b")}}
            for name in ("a", "b"):
                write(name + ".json", self.current)
                write(name + "-benchmark.json", {"numerators": {"confirmed_mismatch_cases": 0}})
            write("diff-identical.json", {"changes": []})
            self.assertTrue(comparison.compare_reports(root, baseline, self.paths)["passed"])
            write("b-benchmark.json", {"numerators": {"confirmed_mismatch_cases": 1}})
            self.assertFalse(comparison.compare_reports(root, baseline, self.paths)["passed"])
            write("b-benchmark.json", {"numerators": {"confirmed_mismatch_cases": 0}})
            mixed = copy.deepcopy(self.current)
            mixed["argentumCommit"] = "another"
            write("b.json", mixed)
            self.assertFalse(comparison.compare_reports(root, baseline, self.paths)["passed"])


if __name__ == "__main__":
    unittest.main()
