#!/usr/bin/env python3
"""Reproduce a bounded research sample from confirmed missing coverage; never a runtime interpreter."""
import argparse
import gzip
import hashlib
import json
import os
import pathlib
import subprocess
import time
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
HEADERS = {"User-Agent": "MTGDeckbuildingAssistant/0.27 (github.com/jasullivan5/mtg-deckbuilding-assistant)", "Accept": "application/json"}


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def select(args):
    manifest = read(pathlib.Path(str(args.snapshot) + ".manifest.json"))
    candidates = {}
    reports = []
    for path in sorted(args.reports.glob("*.json")):
        report = read(path)
        reports.append({"commander": report["commander"], "sha256": hashlib.sha256(path.read_bytes()).hexdigest()})
        if report["snapshot"]["sha256"] != manifest["sha256"]:
            raise ValueError("Coverage report snapshot mismatch")
        for record in report["records"]:
            if record["status"] == "NATIVE_DEFINITION_MISSING":
                candidates[record["external"]["oracleId"]] = record["external"]
    if not candidates:
        raise ValueError("No confirmed missing records")
    if {r["commander"] for r in reports} != {"Judith, the Scourge Diva", "Krenko, Mob Boss", "Oviya Pashiri, Sage Lifecrafter"}:
        raise ValueError("Supply all three Phase 2.7 pre-translation commander coverage reports")
    with gzip.open(args.snapshot, "rt", encoding="utf-8") as source:
        raw = {card["oracle_id"]: card for line in source if (card := json.loads(line)).get("oracle_id") in candidates}
    # These are research sampling strata, not classifications consumed by objective discovery.
    def text(c):
        return "\n".join([c.get("oracle_text", "")] + [f.get("oracle_text", "") for f in c.get("card_faces", [])]).lower()
    def token(c):
        return "create" in text(c) and "token" in text(c)
    strata = [
        ("fixed-red-anchor", lambda c: c["name"] == "Krenko's Command"),
        ("upkeep-life-loss-anchor", lambda c: c["name"] == "Bitterblossom"),
        ("green-token", lambda c: token(c) and c["color_identity"] == ["G"]),
        ("enters-token", lambda c: token(c) and "enters" in text(c)),
        ("activated-token", lambda c: token(c) and ":" in text(c)),
        ("conditional-token", lambda c: token(c) and "if " in text(c)),
        ("token-copy", lambda c: token(c) and "copy" in text(c)),
        ("opponent-token", lambda c: token(c) and "opponent" in text(c)),
        ("treasure-negative-review", lambda c: "treasure token" in text(c) and "creature token" not in text(c)),
        ("graveyard-movement", lambda c: "graveyard" in text(c) and "battlefield" in text(c)),
        ("combat-timing", lambda c: "combat" in text(c) and "beginning" in text(c)),
        ("multi-face", lambda c: len(c.get("card_faces", [])) > 1),
        ("dynamic-token-quantity", lambda c: token(c) and ("create x " in text(c) or "for each" in text(c))),
        ("replacement-layer", lambda c: "instead" in text(c)),
        ("token-reference-negative-review", lambda c: "token" in text(c) and "create" not in text(c)),
        ("plain-non-token-control", lambda c: "token" not in text(c) and "draw" in text(c)),
    ]
    chosen = []
    used = set()
    for stratum, predicate in strata:
        pool = [c for oid, c in raw.items() if oid not in used and predicate(c)]
        if not pool:
            raise ValueError("Empty sample stratum: " + stratum)
        rank = lambda c: hashlib.sha256((manifest["sha256"] + ":" + c["oracle_id"]).encode()).hexdigest()
        card = min(pool, key=rank)
        used.add(card["oracle_id"])
        chosen.append({"oracleId": card["oracle_id"], "name": card["name"], "stratum": stratum, "selectionHash": rank(card)})
        write(args.output / "cards" / (card["oracle_id"] + ".json"), card)
    write(args.output / "selection.json", {"snapshotSha256": manifest["sha256"], "snapshotUpdatedAt": manifest["updatedAt"],
        "method": "Two explicit predeclared gap anchors, then one unused confirmed-missing Oracle ID per ordered textual research stratum; minimum SHA256(snapshotSha256 + ':' + oracleId). Strata only select research inputs; they do not determine mechanical outcomes.", "inputReports": reports, "cards": chosen})
    for card in chosen:
        print(card["stratum"] + "\t" + card["name"])


def assay(args):
    selection = read(args.sample / "selection.json")
    exe = ROOT / "argentum/oracle-assay/build/install/oracle-assay/bin" / ("oracle-assay.bat" if os.name == "nt" else "oracle-assay")
    environment = dict(os.environ)
    environment["JAVA_TOOL_OPTIONS"] = environment.get("JAVA_TOOL_OPTIONS", "") + " -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
    results = []
    for card in selection["cards"]:
        path = (args.sample / "cards" / (card["oracleId"] + ".json")).resolve()
        start = time.monotonic()
        result = subprocess.run([str(exe), "compile", "--file", str(path)], cwd=ROOT,
                                capture_output=True, text=True, encoding="utf-8", errors="strict", timeout=180, env=environment)
        output = args.output / card["oracleId"]
        output.parent.mkdir(parents=True, exist_ok=True)
        output.with_suffix(".stdout.txt").write_text(result.stdout, encoding="utf-8")
        output.with_suffix(".stderr.txt").write_text(result.stderr, encoding="utf-8")
        if result.returncode not in (0, 1):
            raise RuntimeError("Assay execution failed for " + card["name"] + ": " + result.stderr)
        results.append({**card, "exitCode": result.returncode, "elapsedSeconds": round(time.monotonic()-start, 3),
                        "stdoutSha256": hashlib.sha256(result.stdout.encode()).hexdigest(),
                        "stderr": result.stderr.strip()})
        print(card["name"], result.returncode, flush=True)
    write(args.output / "results.json", {"snapshotSha256": selection["snapshotSha256"], "cards": results})


def rulings(args):
    for card in read(args.sample / "selection.json")["cards"]:
        target = args.sample / "rulings" / (card["oracleId"] + ".json")
        if target.exists():
            continue
        raw = read(args.sample / "cards" / (card["oracleId"] + ".json"))
        url = raw["rulings_uri"]
        if not url.startswith("https://api.scryfall.com/cards/") or not url.endswith("/rulings"):
            raise ValueError("Unexpected rulings URL")
        time.sleep(0.15)
        with urllib.request.urlopen(urllib.request.Request(url, headers=HEADERS), timeout=30) as response:
            data = json.load(response)
        if data.get("has_more"):
            raise ValueError("Unexpected paginated rulings; inspect manually")
        write(target, data)
        print(card["name"], len(data["data"]), flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    pick = sub.add_parser("select")
    pick.add_argument("--snapshot", type=pathlib.Path, required=True)
    pick.add_argument("--reports", type=pathlib.Path, required=True)
    pick.add_argument("--output", type=pathlib.Path, required=True)
    for name in ("assay", "rulings"):
        child = sub.add_parser(name)
        child.add_argument("--sample", type=pathlib.Path, required=True)
        if name == "assay":
            child.add_argument("--output", type=pathlib.Path, required=True)
    arguments = parser.parse_args()
    {"select": select, "assay": assay, "rulings": rulings}[arguments.command](arguments)
