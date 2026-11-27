"""Verify that all Discover differences are nested temporary grant evidence."""
import argparse
from collections import Counter
import json
from pathlib import Path
from runpy import run_path

helpers = run_path(str(Path(__file__).with_name("compare-granted-triggers.py")))
shared = helpers["shared"]


def compare(before, after, seen):
    assert before.keys() == after.keys()
    assert all(before[field] == after[field] for field in before if field != "actions")
    totals = Counter()
    for old, new in zip(before["actions"], after["actions"], strict=True):
        assert old == {key: value for key, value in new.items() if key != "temporaryGrants"}, new["id"]
        for grant in new.get("temporaryGrants", []):
            assert seen.setdefault(grant["id"], grant) == grant, grant["id"]
            action = grant["body"]["action"]
            assert new["id"] == grant["actionId"] == action["associatedActionId"]
            assert action["path"] == grant["path"] + ".ability"
            totals[grant["effect"]["type"]] += 1
    return dict(objective=after["objective"], eligiblePhysicalCards=after["counts"]["commander_eligible_cards"],
        populationSha256=shared["population_hash"](after), unchangedResults=shared["counts"](after["results"]),
        addedTemporaryGrants=dict(totals))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    for argument in ("baseline", "candidate", "output"):
        parser.add_argument(argument, type=Path)
    args = parser.parse_args()
    result, seen = {}, {}
    for commander in ("judith", "krenko", "kenrith"):
        result[commander] = []
        for objective in helpers["OBJECTIVES"]:
            filename = f"{commander}-{objective}.json"
            result[commander].append(compare(shared["read"](args.baseline / filename),
                shared["read"](args.candidate / filename), seen))
            print(commander, objective, "existing report unchanged", flush=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
