"""Compare linked-movement additions while preserving all production conclusions."""
import argparse
from collections import Counter
import json
import hashlib
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
    facts = {f["id"]: f for f in after["facts"]}
    additions = []
    for old, new in zip(before["actions"], after["actions"], strict=True):
        assert old == {k:v for k,v in new.items() if k != "linkedMovements"}, (label,new["id"])
        for relation in new.get("linkedMovements", []):
            for step in (relation["departure"],relation.get("continuation")):
                if step is None: continue
                assert step["actionId"] in action_ids, (label,step)
                for fact_id in step["factIds"]:
                    assert fact_id in facts and facts[fact_id]["actionId"] == step["actionId"]
                    assert facts[fact_id]["path"] == step["path"]
            additions.append({"card":new["card"],"face":new["face"],"id":relation["id"],"kind":relation["kind"],"support":relation["support"]})
    return {"objective":after["objective"],"population":after["counts"]["commander_eligible_cards"],
        "populationSha256":population_hash(after),"unchangedResults":counts(after["results"]),
        "relationships":len(additions),"relationshipCards":len({a["card"] for a in additions}),
        "kinds":dict(Counter(a["kind"] for a in additions))}, additions


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
            summary["baselineSha256"]=hashlib.sha256((args.baseline/name).read_bytes()).hexdigest()
            summary["candidateSha256"]=hashlib.sha256((args.candidate/name).read_bytes()).hexdigest()
            reports.append(summary)
        output[commander] = {"reports": reports}
    args.output.write_text(json.dumps(output, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
