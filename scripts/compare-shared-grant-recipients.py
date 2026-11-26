"""Verify that Discover changes are confined to recipient evidence within existing grants."""
import argparse
from collections import Counter
import json
from pathlib import Path
from runpy import run_path

shared = run_path(str(Path(__file__).with_name("compare-granted-triggers.py")))
read = shared["shared"]["read"]
fields = {"grantedAbilities": "support", "grantedTriggers": "recipientSupport"}


def changed_paths(before, after, path=""):
    if before == after:
        return
    if isinstance(before, dict) and isinstance(after, dict):
        for key in sorted(before.keys() | after.keys()):
            yield from changed_paths(before.get(key), after.get(key), f"{path}.{key}".strip("."))
    elif isinstance(before, list) and isinstance(after, list) and len(before) == len(after):
        for first, second in zip(before, after):
            yield from changed_paths(first, second, path + "[]")
    else:
        yield path


def compare(before, after, seen):
    assert before.keys() == after.keys()
    assert all(before[key] == after[key] for key in before if key != "actions")
    changes = Counter()
    evidence_paths = Counter()
    for old, new in zip(before["actions"], after["actions"], strict=True):
        assert {k: v for k, v in old.items() if k not in fields} == {
            k: v for k, v in new.items() if k not in fields
        }, old["id"]
        for field, support in fields.items():
            for previous, current in zip(old.get(field, []), new.get(field, []), strict=True):
                identity = current["id"]
                assert identity == previous["id"]
                assert seen.setdefault(identity, current) == current, identity
                paths = set(changed_paths(previous, current))
                if not paths:
                    continue
                widened = previous[support] == "UNKNOWN" and current[support] == "MATCH"
                assert previous[support] == current[support] or widened, identity
                # Rewalking a newly understood recipient may refine nested scopes. The
                # declaration, native body, event support and every intrinsic field stay pinned.
                allowed = {"kind", support, "unresolved", "unresolved[]"}
                if widened:
                    allowed |= {"body", "eventOutcomes"}
                assert all(path.split(".")[0].removesuffix("[]") in allowed for path in paths), (identity, paths)
                assert previous["body"]["action"]["native"] == current["body"]["action"]["native"], identity
                assert previous["body"]["action"]["id"] == current["body"]["action"]["id"], identity
                changes[field + ("Widened" if widened else "DescriptorOnly")] += 1
                evidence_paths.update(paths)
    return {
        "objective": after["objective"],
        "eligiblePhysicalCards": after["counts"]["commander_eligible_cards"],
        "populationSha256": shared["shared"]["population_hash"](after),
        "unchangedResults": shared["shared"]["counts"](after["results"]),
        "grantChanges": dict(changes),
        "nestedDiffPaths": dict(sorted(evidence_paths.items())),
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    for argument in ("baseline", "candidate", "output"):
        parser.add_argument(argument, type=Path)
    args = parser.parse_args()
    report = {}
    seen = {}
    for commander in ("judith", "krenko", "kenrith"):
        report[commander] = []
        for objective in shared["OBJECTIVES"]:
            filename = f"{commander}-{objective}.json"
            report[commander].append(compare(read(args.baseline / filename), read(args.candidate / filename), seen))
            print(commander, objective, "existing admissions, intrinsic actions and assurance unchanged")
    args.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
