"""Compare Aura evidence additions while preserving all production conclusions."""
import argparse
from collections import Counter
import json
from pathlib import Path
from runpy import run_path

shared = run_path(str(Path(__file__).with_name("compare-counter-placement.py")))
read, counts, population_hash = (shared[key] for key in ("read", "counts", "population_hash"))
OBJECTIVES = (*shared["OBJECTIVES"], "place-plus-one-counters")


def compare(before, after):
    label = after["commander"], after["objective"]
    assert before.keys() == after.keys(), label
    assert all(before[k] == after[k] for k in before if k != "actions"), label
    action_ids = {a["id"] for a in after["actions"]}
    outcome_ids = {o["id"] for o in after.get("eventOutcomes", [])}
    additions = []
    for old, new in zip(before["actions"], after["actions"], strict=True):
        relation = new.get("attachment")
        if relation is None:
            assert old == new, (label, new["id"])
            continue
        assert old == {k: v for k, v in new.items() if k not in ("attachment", "targetSelection")}, (label, new["id"])
        assert new["path"] == "script" and relation["actionId"] == new["id"]
        if relation["restriction"] is not None:
            assert new["targetSelection"]["origin"] == "AURA_ENCHANT"
            assert new["targetSelection"]["requirements"][0] == relation["restriction"]
        for dependency in relation["dependencies"]:
            assert dependency["actionId"] in action_ids, (label, dependency["path"])
            assert set(dependency["eventOutcomeIds"]) <= outcome_ids
        additions.append({
            "card": new["card"], "face": new["face"], "support": relation["support"],
            "dependencies": dict(Counter(f'{d["kind"]}:{d["verdict"]}' for d in relation["dependencies"])),
        })
    return {
        "objective": after["objective"], "population": after["counts"]["commander_eligible_cards"],
        "populationSha256": population_hash(after), "unchangedResults": counts(after["results"]),
        "attachmentRecords": len(additions), "attachmentCards": len({a["card"] for a in additions}),
        "dependencyVerdicts": dict(Counter(key for a in additions for key, n in a["dependencies"].items() for _ in range(n))),
    }, additions


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("baseline", "candidate", "output"):
        parser.add_argument(name, type=Path)
    args = parser.parse_args()
    output = {}
    for commander in ("kenrith", "judith", "krenko"):
        reports, inventory = [], None
        for objective in OBJECTIVES:
            name = f"{commander}-{objective}.json"
            summary, records = compare(read(args.baseline/name), read(args.candidate/name))
            if inventory is None:
                inventory = records
            assert inventory == records, (commander, objective)
            reports.append(summary)
        output[commander] = {"reports": reports}
    args.output.write_text(json.dumps(output, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
