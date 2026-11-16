"""Measure Class availability and detect regressions outside nested Class abilities."""
import argparse
from collections import Counter
import json
from pathlib import Path
from runpy import run_path

shared = run_path(str(Path(__file__).with_name("compare-counter-placement.py")))
read = shared["read"]
counts = shared["counts"]
population_hash = shared["population_hash"]
objectives = (*shared["OBJECTIVES"], "place-plus-one-counters")


def class_row(row):
    return any(".classLevels[" in (row.get(key) or "") for key in (
        "actionId", "abilityPath", "outcomePath", "path",
    ))


def indexed(rows, fields):
    # Keep occurrence counts in case multiple records share one ability/branch.
    return Counter(tuple(row.get(field) for field in fields) for row in rows)


def scope(rows):
    return [row for row in rows if class_row(row)]


def compare(before, after):
    for key in ("argentumCommit", "objective", "commander", "eligibility", "mechanicalInputCards"):
        assert before[key] == after[key], f"Population or provenance changed: {key}"
    availability = [r for r in after["actions"] if r.get("classAvailability")]
    class_cards = {r["card"] for r in availability}
    annotation_changes = []
    keys = ("results", "actions", "facts", "eventOutcomes", "selfReturns", "stateDependencies",
            "unsupported", "structuralSupport", "conditionalArrivals")
    for key in keys:
        old_scope, new_scope = ([r for r in report.get(key, []) if not class_row(r)] for report in (before, after))
        location = f"{after['commander']} / {after['objective']} / {key}"
        if key != "results":
            assert old_scope == new_scope, f"Evidence outside nested Class abilities changed: {location}"
            continue
        assert len(old_scope) == len(new_scope), location
        for old_row, new_row in zip(old_scope, new_scope):
            if old_row == new_row:
                continue
            assert old_row["card"] in class_cards
            assert old_row["interpretation"] in {
                "PARTIAL_ACTION_SUPPORTED_PATHWAY_NOT_GAMEPLAY_VERIFIED",
                "PARTIAL_ACTION_SUPPORTED_OUTCOME_NOT_GAMEPLAY_VERIFIED",
            }
            assert new_row["interpretation"] == "STRUCTURAL_POTENTIAL_NOT_GAMEPLAY_VERIFIED"
            updates = {"interpretation": new_row["interpretation"]}
            if old_row.get("localSupport") != new_row.get("localSupport"):
                assert old_row["localSupport"] == "PARTIALLY_SUPPORTED" and new_row["localSupport"] == "SUPPORTED"
                updates["localSupport"] = new_row["localSupport"]
            assert {**old_row, **updates} == new_row, location
            annotation_changes.append({"card": new_row["card"], "path": new_row["abilityPath"],
                                       "before": {field: old_row[field] for field in updates}, "after": updates})

    old = scope(before["results"])
    new = scope(after["results"])
    fields = ("card", "face", "actionId", "abilityPath", "branch", "operation", "verdict")
    a, b = indexed(old, fields), indexed(new, fields)
    assert not any(key[-1] == "MATCH" for key in a - b), "A previously supported Class pathway was lost"

    events_before = {row["id"]: row for row in scope(before.get("eventOutcomes", []))}
    event_changes = [
        {"card": r["card"], "path": r["path"], "kind": r["kind"],
         "before": events_before.get(r["id"], {}).get("confidence"), "after": r["confidence"]}
        for r in scope(after.get("eventOutcomes", []))
        if events_before.get(r["id"], {}).get("confidence") != r["confidence"]
    ]

    def verdict_deltas(deltas):
        return [{"card": k[0], "path": k[3], "operation": k[5], "verdict": k[-1], "count": n}
                for k, n in sorted(deltas.items())]

    return {
        "commander": after["commander"],
        "objective": after["objective"],
        "population": after["counts"]["commander_eligible_cards"],
        "populationSha256": population_hash(after),
        "before": counts(before["results"]),
        "after": counts(after["results"]),
        "classCards": len(class_cards),
        "classLevelEntries": len({(r["card"], r["face"], r["classAvailability"]["levelPath"]) for r in availability}),
        "availabilityByAbilityFamily": dict(sorted(Counter(
            r["path"].rsplit(".", 1)[-1].split("[", 1)[0] for r in availability
        ).items())),
        "classResultRowsRemoved": verdict_deltas(a - b),
        "classResultRowsAdded": verdict_deltas(b - a),
        "classEventConfidenceChanges": event_changes,
        "baseActionAnnotationChanges": annotation_changes,
        "classUnsupportedBefore": len(scope(before["unsupported"])),
        "classUnsupportedAfter": len(scope(after["unsupported"])),
        "classStructuralSupportBefore": len(scope(before["structuralSupport"])),
        "classStructuralSupportAfter": len(scope(after["structuralSupport"])),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for arg in ("baseline", "candidate", "output"):
        parser.add_argument(arg, type=Path)
    args = parser.parse_args()
    comparisons = []
    for context in ("kenrith", "judith", "krenko"):
        for objective in objectives:
            name = f"{context}-{objective}.json"
            comparisons.append(compare(read(args.baseline / name), read(args.candidate / name)))
    args.output.write_text(json.dumps({"comparisons": comparisons}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
