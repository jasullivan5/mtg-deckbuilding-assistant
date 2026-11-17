"""Verify unchanged discovery and measure fixed chosen-target cost interpretation."""
import argparse
from hashlib import sha256
from collections import Counter
import json
from pathlib import Path
from runpy import run_path

shared = run_path(str(Path(__file__).with_name("compare-counter-placement.py")))
read, counts, population_hash = (shared[key] for key in ("read", "counts", "population_hash"))
OBJECTIVES = (*shared["OBJECTIVES"], "place-plus-one-counters")


def chosen_cost(cost):
    return (cost.get("reductionSource") or {}).get("type") == "FixedIfAnyTargetMatches" or (
        cost["native"].get("modification", {}).get("type") == "ReduceColoredIfAnyTargetMatches"
    )


def verdict(cost):
    if cost is None:
        return "ABSENT"
    reduced = cost["isolatedManaWhenTrue"]
    return "UNKNOWN" if reduced is None else "NO_MATCH" if reduced == cost["baseMana"] else "MATCH"


def compare(before, after):
    label = after["commander"], after["objective"]
    assert before.keys() == after.keys(), label
    assert all(before[key] == after[key] for key in before if key != "actions"), label
    inventory = []
    selection_count = 0
    for old, new in zip(before["actions"], after["actions"], strict=True):
        fields = {"conditionalCosts", "targetSelection"}
        assert {k: v for k, v in old.items() if k not in fields} == {k: v for k, v in new.items() if k not in fields}, label
        selection = new.get("targetSelection")
        if selection:
            assert new["path"] == "script.spell" and selection["actionId"] == new["id"]
            assert len(new["native"]["targetRequirements"]) == len(selection["requirements"])
            selection_count += 1
        previous = old.get("conditionalCosts", [])
        current = new.get("conditionalCosts", [])
        assert [c for c in previous if not chosen_cost(c)] == [c for c in current if not chosen_cost(c)], label
        prior = {c["declarationPath"]: c for c in previous if chosen_cost(c)}
        for cost in filter(chosen_cost, current):
            old_cost = prior.pop(cost["declarationPath"], None)
            if old_cost:
                derived = {"reduction", "isolatedManaWhenTrue", "chosenTargetCondition"}
                assert {k: v for k, v in old_cost.items() if k not in derived} == {
                    k: v for k, v in cost.items() if k not in derived
                }, label
            condition = cost["chosenTargetCondition"]
            assert condition["quantifier"] == "ANY_SELECTED"
            filter_hash = sha256(json.dumps(condition["filter"], sort_keys=True).encode()).hexdigest()
            inventory.append({
                "card": new["card"], "action": new["id"], "declaration": cost["declarationPath"],
                "kind": cost["kind"], "filterSha256": filter_hash, "requirements": len(selection["requirements"]) if selection else 0,
                "before": verdict(old_cost), "after": verdict(cost),
            })
        assert not prior, label
    return {
        "objective": after["objective"], "population": after["counts"]["commander_eligible_cards"],
        "populationSha256": population_hash(after), "unchangedResults": counts(after["results"]),
        "actionsWithTargetSelection": selection_count,
    }, inventory


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for arg in ("baseline", "candidate", "output"):
        parser.add_argument(arg, type=Path)
    args = parser.parse_args()
    commanders = {}
    for commander in ("kenrith", "judith", "krenko"):
        reports = []
        inventory = None
        for objective in OBJECTIVES:
            name = f"{commander}-{objective}.json"
            summary, records = compare(read(args.baseline / name), read(args.candidate / name))
            if inventory is None:
                inventory = records
            assert inventory == records
            reports.append(summary)
        commanders[commander] = {
            "declarations": len(inventory),
            "cards": len({row["card"] for row in inventory}),
            "actions": len({row["action"] for row in inventory}),
            "newlySupportedConditions": len({
                (row["action"], row["filterSha256"]) for row in inventory if row["after"] == "MATCH"
            }),
            "transitions": dict(Counter(f'{row["kind"]}: {row["before"]} -> {row["after"]}' for row in inventory)),
            "reports": reports,
            "inventory": inventory,
        }
    args.output.write_text(json.dumps(commanders, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
