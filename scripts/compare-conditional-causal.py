#!/usr/bin/env python3
"""Compare revision-bound CLI exports and trace conditional branch links."""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path


def digest(path):
    value = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def key(row):
    return (row["actionId"], row["stateDependency"]["conditionPath"])


def totals(rows):
    return {
        status: {
            "cards": len({row["card"] for row in rows if row["verdict"] == status}),
            "pathways": sum(row["verdict"] == status for row in rows),
        }
        for status in ("MATCH", "NO_MATCH", "UNKNOWN")
    }


def compare_history(before, after):
    old, new = read(before), read(after)
    stable = set(old) | set(new)
    stable -= {"counts", "limitations", "results", "stateDependencies"}
    assert all(old.get(field) == new.get(field) for field in stable), (before, stable)
    old_rows = {key(row): row for row in old["results"]}
    new_rows = {key(row): row for row in new["results"]}
    assert len(old_rows) == len(old["results"]) and len(new_rows) == len(new["results"])
    assert old_rows.keys() == new_rows.keys()
    transitions = [
        {"card": row["card"], "action": identifier[0], "conditionPath": identifier[1],
         "before": old_rows[identifier]["verdict"], "after": row["verdict"]}
        for identifier, row in new_rows.items()
        if row["verdict"] != old_rows[identifier]["verdict"]
    ]
    assert all(t["before"] == "UNKNOWN" and t["after"] == "MATCH" for t in transitions), transitions
    return {
        "eligible": len(new["mechanicalInputCards"]),
        "beforeSha256": digest(before),
        "afterSha256": digest(after),
        "before": totals(old["results"]),
        "after": totals(new["results"]),
        "branchChange": dict(Counter(d.get("causalChange", "NOT_EXPORTED") for d in new["stateDependencies"])),
        "unresolvedCards": sorted({r["card"] for r in new["results"] if r["verdict"] == "UNKNOWN"}),
        "transitions": transitions,
    }


def gravelighter(history, deaths):
    history_report, death_report = read(history), read(deaths)
    row, = [r for r in history_report["results"] if r["card"] == "Gravelighter"]
    dependency = row["stateDependency"]
    draw, = [e for e in history_report["eventOutcomes"] if e["id"] in dependency["trueLinks"]["eventOutcomeIds"]]
    sacrifice, = [f for f in history_report["facts"] if f["id"] in dependency["falseLinks"]["factIds"]]
    event_row, = [r for r in death_report["results"] if r["actionId"] == dependency["actionId"] and r["abilityPath"] == sacrifice["path"]]
    assert row["verdict"] == "MATCH"
    assert draw["kind"] == "DRAW_CARDS" and sacrifice["kind"] == "SACRIFICE"
    assert event_row["mechanicalCompatibility"] == "MATCH" and event_row["policyAcceptance"] == "NO_MATCH"
    return {
        "condition": dependency["condition"],
        "checkpoint": dependency["checkpoint"],
        "incomingTrigger": dependency["incomingEvent"]["trigger"],
        "stateVerdict": row["verdict"],
        "trueOutcome": {key: draw[key] for key in ("id", "path", "native", "recipient", "confidence")},
        "falseOperation": {key: sacrifice[key] for key in ("id", "path", "native", "objectFilter", "sourceZone", "sourcePlayer", "prerequisites")},
        "deathObjective": {key: event_row[key] for key in ("verdict", "mechanicalCompatibility", "policyAcceptance", "decisions")},
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("before", type=Path)
    parser.add_argument("after", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--history-after", type=Path, help="Optional separate directory for current history reports")
    args = parser.parse_args()
    history_after = args.history_after or args.after
    history_files = {
        "judith-creature-died-this-turn.json", "krenko-creature-died-this-turn.json", "kenrith-history.json",
    }
    legacy_objectives = (
        "own-nontoken-creature-deaths", "reanimate-own-creatures",
        "create-creature-tokens", "stock-own-graveyard",
    )
    legacy_files = {f"{commander}-{objective}.json" for commander in ("judith", "krenko")
                    for objective in legacy_objectives} | {"kenrith-death.json"}
    legacy = {}
    history = {}
    for filename in sorted(legacy_files | history_files):
        old, new = args.before / filename, args.after / filename
        if filename in history_files:
            history[filename] = compare_history(old, history_after / filename)
        else:
            checksum = digest(old)
            assert checksum == digest(new), filename
            report = read(new)
            legacy[filename] = {
                "bothSha256": checksum,
                "eligible": len(report["mechanicalInputCards"]),
                "counts": totals(report["results"]),
            }
    trace = gravelighter(history_after / "judith-creature-died-this-turn.json", args.after / "judith-own-nontoken-creature-deaths.json")
    args.output.write_text(json.dumps({"legacy": legacy, "history": history, "gravelighter": trace}, indent=2) + "\n", encoding="utf-8")
    for name, report in history.items():
        print(name, report["eligible"], report["before"], report["after"], [r["card"] for r in report["transitions"]])


if __name__ == "__main__":
    main()
