"""Check payment evidence against the unchanged Class-availability CLI exports."""
import argparse
from collections import Counter
import json
from pathlib import Path
from runpy import run_path

shared = run_path(str(Path(__file__).with_name("compare-counter-placement.py")))
read, counts, population_hash = (shared[k] for k in ("read", "counts", "population_hash"))
objectives = (*shared["OBJECTIVES"], "place-plus-one-counters")


def strip_costs(row):
    return {key: value for key, value in row.items() if key != "conditionalCosts"}


def compare(before, after):
    ignored = {"actions", "results", "counts", "limitations"}
    assert {k: v for k, v in before.items() if k not in ignored} == {
        k: v for k, v in after.items() if k not in ignored
    }, (after["commander"], after["objective"], "unexpected report evidence change")
    old_ids = {row["id"] for row in before["actions"]}
    stable = []
    for row in after["actions"]:
        if row["id"] in old_ids:
            stable.append(strip_costs(row))
        else:
            assert row["path"] == "script.spell" and row["conditionalCosts"]
    assert stable == before["actions"]

    rows = after["results"]
    costs = [r for r in rows if r.get("conditionalCost")]
    assert [r for r in rows if not r.get("conditionalCost")] == before["results"]
    assert not costs or after["objective"] == "creature-died-this-turn"

    relationships = []
    for action in after["actions"]:
        for cost in action.get("conditionalCosts", []):
            base, reduced = cost["baseMana"], cost["isolatedManaWhenTrue"]
            verdict = "UNKNOWN" if reduced is None else "NO_MATCH" if reduced == base else "MATCH"
            relationships.append({"card": action["card"], "action": action["id"],
                                  "declaration": cost["declarationPath"], "checkpoint": cost["checkpoint"],
                                  "verdict": verdict})
    return {
        "commander": after["commander"], "objective": after["objective"],
        "population": after["counts"]["commander_eligible_cards"],
        "populationSha256": population_hash(after),
        "before": counts(before["results"]), "after": counts(rows),
        "costRecordVerdicts": dict(sorted(Counter(r["verdict"] for r in relationships).items())),
        "newObjectiveRows": [{"card": r["card"], "path": r["abilityPath"], "verdict": r["verdict"]} for r in costs],
    }, relationships


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for arg in ("baseline", "candidate", "output"):
        parser.add_argument(arg, type=Path)
    args = parser.parse_args()
    rows, inventories = [], {}
    for commander in ("kenrith", "judith", "krenko"):
        for objective in objectives:
            name = f"{commander}-{objective}.json"
            row, inventory = compare(read(args.baseline / name), read(args.candidate / name))
            assert inventory == inventories.setdefault(commander, inventory)
            rows.append(row)
    args.output.write_text(json.dumps({"comparisons": rows, "inventories": inventories},
                                     indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
