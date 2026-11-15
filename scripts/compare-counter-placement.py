"""Summarize counter discovery and verify exact preservation of existing reports."""

import argparse
import hashlib
import json
from pathlib import Path

OBJECTIVES = (
    "own-nontoken-creature-deaths",
    "creature-died-this-turn",
    "reanimate-own-creatures",
    "create-creature-tokens",
    "stock-own-graveyard",
)
VERDICTS = ("MATCH", "UNKNOWN", "NO_MATCH")
CONTROLS = (
    "Hardened Scales", "Travel Preparations", "Walking Ballista", "Abzan Falconer",
    "Winding Constrictor", "Wall of Roots", "Goblin Chieftain",
)


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def read(path):
    with path.open(encoding="utf-8") as stream:
        return json.load(stream)


def population_hash(report):
    return hashlib.sha256(
        json.dumps(report["mechanicalInputCards"], ensure_ascii=False).encode("utf-8")
    ).hexdigest()


def counts(rows):
    return {
        verdict: {"cards": len({row["card"] for row in rows if row["verdict"] == verdict}),
                  "pathways": sum(row["verdict"] == verdict for row in rows)}
        for verdict in VERDICTS
    }


def summarize(path):
    report = read(path)
    rows = report["results"]
    controls = {}
    for name in CONTROLS:
        eligibility = next((e for e in report["eligibility"] if e["card"] == name), None)
        controls[name] = {
            "eligibility": {key: eligibility[key] for key in ("format", "deck", "identity")}
            if eligibility else None,
            "pathways": [
                {key: row[key] for key in ("branch", "actionId", "abilityPath", "verdict", "decisions")}
                for row in rows if row["card"] == name
            ],
        }
    return {
        "commander": report["commander"],
        "engine": report["argentumCommit"],
        "population": report["counts"]["commander_eligible_cards"],
        "populationSha256": population_hash(report),
        "unsupportedStructures": len(report["unsupported"]),
        "structuralSupportRecords": len(report["structuralSupport"]),
        "total": counts(rows),
        "byRelationship": {
            branch: counts([row for row in rows if row["branch"] == branch])
            for branch in ("COUNTER_PLACEMENT", "COUNTER_MODIFICATION")
        },
        "controls": controls,
        "representativeUnknowns": list(dict.fromkeys(
            row["card"] for row in rows if row["verdict"] == "UNKNOWN"
        ))[:12],
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("baseline", type=Path)
    parser.add_argument("candidate", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    previous = []
    new = []
    for commander in ("judith", "krenko", "kenrith"):
        for objective in OBJECTIVES:
            name = f"{commander}-{objective}.json"
            before = args.baseline / name
            if commander == "kenrith" and not before.exists():
                aliases = {
                    "own-nontoken-creature-deaths": "kenrith-death.json",
                    "creature-died-this-turn": "kenrith-history.json",
                }
                if objective in aliases:
                    before = args.baseline / aliases[objective]
            after = args.candidate / name
            before_hash, after_hash = sha256(before), sha256(after)
            previous.append({"report": name, "equal": before_hash == after_hash,
                             "beforeSha256": before_hash, "afterSha256": after_hash})
        summary = summarize(args.candidate / f"{commander}-place-plus-one-counters.json")
        baseline_population = population_hash(read(args.baseline / f"{commander}-create-creature-tokens.json"))
        summary["samePopulationAsExistingObjectives"] = baseline_population == summary["populationSha256"]
        new.append(summary)

    args.output.write_text(json.dumps({"existingObjectives": previous, "counterObjective": new},
                                     ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    if not all(item["equal"] for item in previous):
        parser.exit(1, "At least one existing report changed. See the written comparison.\n")
    if not all(item["samePopulationAsExistingObjectives"] for item in new):
        parser.exit(1, "At least one counter report used a different population. See the written comparison.\n")


if __name__ == "__main__":
    main()
