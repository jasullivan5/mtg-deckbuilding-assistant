"""Compare counter multiplication against the six baseline objective exports."""

import argparse
from collections import Counter
import json
from pathlib import Path
from runpy import run_path

pipeline = run_path(str(Path(__file__).with_name("compare-counter-placement.py")))
OBJECTIVES = pipeline["OBJECTIVES"]
counts = pipeline["counts"]
read = pipeline["read"]
sha256 = pipeline["sha256"]
population_hash = pipeline["population_hash"]
COUNTER = "place-plus-one-counters"


def multiplier(row):
    return row["nativeOperation"].get("type") == "DoubleCounterPlacement"


def existing_row(row):
    row = row.copy()
    if row.get("counterModifier"):
        modifier = row["counterModifier"].copy()
        modifier.pop("effectsOnly", None)
        modifier.pop("transformation", None)
        row["counterModifier"] = modifier
    return row


def compare_counter(before, after):
    changing_keys = {"counts", "results", "structuralSupport", "unsupported"}
    assert {k: v for k, v in before.items() if k not in changing_keys} == {
        k: v for k, v in after.items() if k not in changing_keys
    }, "Population, actions, facts, or other report evidence changed"
    assert [existing_row(row) for row in before["results"] if not multiplier(row)] == [
        existing_row(row) for row in after["results"] if not multiplier(row)
    ], "A direct placement or additive pathway changed"
    old, new = [
        {(row["actionId"], row["abilityPath"]): row for row in report["results"] if multiplier(row)}
        for report in (before, after)
    ]
    assert old.keys() == new.keys(), "Multiplier pathway identities changed"
    for key in old:
        assert old[key]["nativeOperation"] == new[key]["nativeOperation"]
        assert new[key]["counterModifier"]["transformation"] == {"type": "MULTIPLICATIVE", "factor": 2}

    ids = {action for action, _ in old}
    for key in ("unsupported", "structuralSupport"):
        assert [row for row in before[key] if row["actionId"] not in ids] == [
            row for row in after[key] if row["actionId"] not in ids
        ], key

    def summarize(report):
        rows = report["results"]
        return {
            "total": counts(rows),
            "direct": counts([row for row in rows if row["branch"] == "COUNTER_PLACEMENT"]),
            "additive": counts([row for row in rows if row["branch"] == "COUNTER_MODIFICATION" and not multiplier(row)]),
            "multiplicative": counts([row for row in rows if multiplier(row)]),
            "unsupportedStructures": len(report["unsupported"]),
            "structuralSupportRecords": len(report["structuralSupport"]),
        }

    transitions = Counter((old[key]["verdict"], new[key]["verdict"]) for key in old)
    return {
        "commander": after["commander"],
        "population": after["counts"]["commander_eligible_cards"],
        "populationSha256": population_hash(after),
        "before": summarize(before),
        "after": summarize(after),
        "transitions": {f"{old} -> {new}": n for (old, new), n in sorted(transitions.items())},
        "multipliers": [
            {"card": row["card"], "actionId": row["actionId"], "path": row["abilityPath"],
             "before": old[key]["verdict"], "after": row["verdict"],
             "effectsOnly": row["counterModifier"]["effectsOnly"],
             "unresolved": [x["stage"] for x in row["decisions"] if x["verdict"] == "UNKNOWN"]}
            for key, row in new.items()
        ],
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for arg in ("baseline", "candidate", "output"):
        parser.add_argument(arg, type=Path)
    args = parser.parse_args()
    unchanged, counters = {}, []
    for key in ("kenrith", "judith", "krenko"):
        for objective in OBJECTIVES:
            name = f"{key}-{objective}.json"
            before, after = (sha256(folder / name) for folder in (args.baseline, args.candidate))
            assert before == after, f"Existing objective report changed: {name}"
            unchanged[name] = after
        name = f"{key}-{COUNTER}.json"
        counters.append(compare_counter(read(args.baseline / name), read(args.candidate / name)))
    args.output.write_text(json.dumps({"identicalReportSha256": unchanged, "counterObjective": counters},
                                     indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
