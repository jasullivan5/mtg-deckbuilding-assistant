"""Compare controlled-permanent cost interpretation on identical CLI populations."""
import argparse
from collections import Counter
import json
from pathlib import Path
from runpy import run_path

shared = run_path(str(Path(__file__).with_name("compare-counter-placement.py")))
read, counts, population_hash = (shared[key] for key in ("read", "counts", "population_hash"))
OBJECTIVES = (*shared["OBJECTIVES"], "place-plus-one-counters")


def verdict(cost):
    reduced = cost["isolatedManaWhenTrue"]
    if reduced is None:
        return "UNKNOWN"
    return "NO_MATCH" if reduced == cost["baseMana"] else "MATCH"


def compare(before, after):
    label = after["commander"], after["objective"]
    assert before.keys() == after.keys(), label
    assert all(before[key] == after[key] for key in before if key != "actions"), label
    assert len(before["actions"]) == len(after["actions"]), label
    inventory = []
    for old, new in zip(before["actions"], after["actions"], strict=True):
        costs = "conditionalCosts"
        assert {k: v for k, v in old.items() if k != costs} == {k: v for k, v in new.items() if k != costs}
        for prior, current in zip(old.get(costs, []), new.get(costs, []), strict=True):
            source = current["reductionSource"]
            if not source or source["type"] != "FixedIfControlFilter":
                assert prior == current, label
                continue
            derived = {"condition", "reduction", "isolatedManaWhenTrue"}
            assert {k: v for k, v in prior.items() if k not in derived} == {
                k: v for k, v in current.items() if k not in derived
            }, label
            inventory.append({
                "card": new["card"], "action": new["id"], "declaration": current["declarationPath"],
                "filter": source["filter"], "before": verdict(prior), "after": verdict(current),
            })
    return {
        "objective": after["objective"],
        "population": after["counts"]["commander_eligible_cards"],
        "populationSha256": population_hash(after),
        "unchangedResults": counts(after["results"]),
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
            "transitions": dict(Counter(f'{row["before"]} -> {row["after"]}' for row in inventory)),
            "reports": reports,
            "inventory": inventory,
        }
    args.output.write_text(json.dumps(commanders, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
