"""Check that Discover changes are additive conditional grant evidence."""
import argparse
from collections import Counter
import json
from pathlib import Path
from runpy import run_path

helper = run_path(str(Path(__file__).with_name("compare-granted-triggers.py")))
shared = helper["shared"]
FIELDS = ("grantedAbilities", "grantedTriggers")


def compare(before, after, grants_seen):
    assert before.keys() == after.keys()
    assert all(before[key] == after[key] for key in before if key != "actions")
    counts = Counter()
    for old, new in zip(before["actions"], after["actions"], strict=True):
        assert {k: v for k, v in old.items() if k not in FIELDS} == {
            k: v for k, v in new.items() if k not in FIELDS
        }, old["id"]
        for field in FIELDS:
            grants = new.get(field, [])
            assert old.get(field, []) == [g for g in grants if not g.get("conditional")], old["id"]
            for grant in (g for g in grants if g.get("conditional")):
                assert grants_seen.setdefault(grant["id"], grant) == grant
                conditional = grant["conditional"]
                assert new["native"]["type"] == "ConditionalStaticAbility"
                assert grant["actionId"] == new["id"]
                assert conditional["wrapperPath"] == new["path"]
                assert conditional["innerPath"] == grant["path"] == new["path"] + ".ability"
                action = grant["body"]["action"]
                assert action["associatedActionId"] == new["id"] and action["path"] == grant["path"] + ".ability"
                counts[field] += 1
    return dict(objective=after["objective"], eligiblePhysicalCards=after["counts"]["commander_eligible_cards"],
        populationSha256=shared["population_hash"](after), unchangedResults=shared["counts"](after["results"]),
        addedConditionalGrants=dict(counts))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    for argument in ("baseline", "candidate", "output"):
        parser.add_argument(argument, type=Path)
    args = parser.parse_args()
    result = {}
    seen = {}
    for commander in ("judith", "krenko", "kenrith"):
        result[commander] = []
        for objective in helper["OBJECTIVES"]:
            filename = f"{commander}-{objective}.json"
            result[commander].append(compare(shared["read"](args.baseline / filename),
                shared["read"](args.candidate / filename), seen))
            print(commander, objective, "existing report unchanged")
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
