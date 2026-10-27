#!/usr/bin/env python3
"""Offline downloader integrity/replay checks; no Scryfall requests."""
import hashlib
import importlib.util
import io
import json
import pathlib
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("snapshot", pathlib.Path(__file__).with_name("oracle-snapshot.py"))
snapshot = importlib.util.module_from_spec(spec)
spec.loader.exec_module(snapshot)


class SnapshotDownloadTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = pathlib.Path(self.temporary.name)
        self.data = b'{"name":"Synthetic fixture"}\n'
        self.meta = {"type":"oracle_cards", "id":"fixture", "updated_at":"2026-10-26T00:00:00Z",
                     "jsonl_download_uri":"https://data.scryfall.io/oracle-cards/fixture.jsonl"}

    def tearDown(self):
        self.temporary.cleanup()

    def test_download_records_exact_bytes_and_replay_reuses_without_network(self):
        def response(url):
            return io.BytesIO(json.dumps(self.meta).encode() if url == snapshot.ENDPOINT else self.data)
        with patch.object(snapshot, "fetch", side_effect=response) as network:
            snapshot.download(self.root)
            self.assertEqual(network.call_count, 2)
        target = self.root / "fixture.jsonl"
        manifest = pathlib.Path(str(target) + ".manifest.json")
        self.assertEqual(json.loads(manifest.read_text())["sha256"], hashlib.sha256(self.data).hexdigest())
        with patch.object(snapshot, "fetch", side_effect=AssertionError("Replay of cached file must stay offline")):
            snapshot.download(self.root, manifest)

    def test_bad_replay_hash_never_publishes_download(self):
        manifest = self.root / "replay.json"
        manifest.write_text(json.dumps({"schemaVersion":1,"bulkType":"oracle_cards","bulkId":"fixture",
            "updatedAt":self.meta["updated_at"],"format":"jsonl","downloadUri":self.meta["jsonl_download_uri"],
            "sha256":"0"*64,"bytes":len(self.data)}))
        with patch.object(snapshot,"fetch",return_value=io.BytesIO(self.data)):
            with self.assertRaises(ValueError):
                snapshot.download(self.root,manifest)
        self.assertFalse((self.root/"fixture.jsonl").exists())
        self.assertFalse((self.root/"fixture.jsonl.part").exists())

    def test_non_scryfall_downloads_are_refused(self):
        for url in ["http://data.scryfall.io/cards.json", "https://example.com/cards.json"]:
            with self.assertRaises(ValueError):
                snapshot.fetch(url)


if __name__ == "__main__":
    unittest.main()
