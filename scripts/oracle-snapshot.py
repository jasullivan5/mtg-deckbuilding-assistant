#!/usr/bin/env python3
"""Deliberate Scryfall bulk update. Normal application queries never use the network."""
import argparse
import datetime as dt
import hashlib
import json
import pathlib
import urllib.parse
import urllib.request

HEADERS = {
    "User-Agent": "MTGDeckbuildingAssistant/0.27 (github.com/jasullivan5/mtg-deckbuilding-assistant)",
    "Accept": "application/json",
}
ENDPOINT = "https://api.scryfall.com/bulk-data/oracle_cards"


def fetch(url):
    parsed = urllib.parse.urlparse(url)
    if parsed.scheme != "https" or parsed.hostname not in {"api.scryfall.com", "data.scryfall.io"}:
        raise ValueError("Expected an HTTPS Scryfall bulk URL")
    return urllib.request.urlopen(urllib.request.Request(url, headers=HEADERS), timeout=120)


def download(directory, replay=None):
    expected = json.loads(replay.read_text(encoding="utf-8")) if replay else None
    if expected:
        if expected.get("schemaVersion") != 1 or expected.get("bulkType") != "oracle_cards":
            raise ValueError("Unsupported replay manifest")
        metadata = {"type": "oracle_cards", "id": expected["bulkId"], "updated_at": expected["updatedAt"]}
        metadata["jsonl_download_uri" if expected["format"] == "jsonl" else "download_uri"] = expected["downloadUri"]
    else:
        with fetch(ENDPOINT) as response:
            metadata = json.load(response)
    if metadata.get("type") != "oracle_cards":
        raise ValueError("Endpoint did not return Oracle Cards metadata")
    url = metadata.get("jsonl_download_uri") or metadata["download_uri"]
    directory.mkdir(parents=True, exist_ok=True)
    destination = directory / pathlib.PurePosixPath(urllib.parse.urlparse(url).path).name
    manifest_path = pathlib.Path(str(destination) + ".manifest.json")
    if destination.exists() or manifest_path.exists():
        if not (destination.exists() and manifest_path.exists()):
            raise ValueError("Incomplete existing snapshot; choose a new directory")
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        with destination.open("rb") as source:
            digest = hashlib.file_digest(source, "sha256").hexdigest()
        if manifest["sha256"] != digest or manifest["updatedAt"] != metadata["updated_at"]:
            raise ValueError("Existing snapshot differs; it was not overwritten")
        if expected and (digest != expected["sha256"] or destination.stat().st_size != expected["bytes"]):
            raise ValueError("Existing snapshot does not match replay manifest")
        print(destination)
        return
    temporary = pathlib.Path(str(destination) + ".part")
    digest = hashlib.sha256()
    size = 0
    try:
        with fetch(url) as response, temporary.open("wb") as output:
            while chunk := response.read(1024 * 1024):
                output.write(chunk)
                digest.update(chunk)
                size += len(chunk)
        if not size:
            raise ValueError("Empty bulk response")
        if expected and (digest.hexdigest() != expected["sha256"] or size != expected["bytes"]):
            raise ValueError("Downloaded snapshot does not match replay manifest")
        manifest = {
            "schemaVersion": 1, "bulkType": "oracle_cards", "bulkId": metadata["id"],
            "updatedAt": metadata["updated_at"],
            "downloadedAt": dt.datetime.now(dt.timezone.utc).isoformat(),
            "sourceUri": ENDPOINT, "downloadUri": url,
            "format": "jsonl" if ".jsonl" in destination.name else "json-array",
            "compression": "gzip" if destination.suffix == ".gz" else "none",
            "sha256": digest.hexdigest(), "bytes": size,
        }
        temporary.rename(destination)
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
        print(destination)
        print(json.dumps(manifest, indent=2))
    finally:
        temporary.unlink(missing_ok=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=["download"])
    parser.add_argument("--directory", type=pathlib.Path, required=True,
                        help="Ignored local directory, normally generated/oracle")
    parser.add_argument("--manifest", type=pathlib.Path,
                        help="Replay an archived version and enforce its size/SHA256 instead of fetching latest metadata")
    args = parser.parse_args()
    download(args.directory, args.manifest)
