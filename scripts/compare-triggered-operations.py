#!/usr/bin/env python3
"""Compare supported event operations against the unchanged PR #51 eligibility pools."""
import argparse
from collections import Counter, defaultdict
import hashlib
import json
from pathlib import Path

DEATH = "own-nontoken-creature-deaths"
HISTORY = "creature-died-this-turn"
NEW_KINDS = {"FORCED_SACRIFICE", "LOSE_LIFE", "GAIN_LIFE", "DEAL_PLAYER_DAMAGE"}


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def digest(path):
    value = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def counts(rows):
    return {
        verdict: {
            "cards": len({row["card"] for row in rows if row["verdict"] == verdict}),
            "pathways": sum(row["verdict"] == verdict for row in rows),
        }
        for verdict in ("MATCH", "UNKNOWN", "NO_MATCH")
    }


def key(row):
    state = row.get("stateDependency") or {}
    return tuple(row[field] for field in ("card", "face", "actionId", "abilityPath", "branch", "operation")) + (
        state.get("conditionPath"), row["verdict"],
    )


def index_outcomes(report):
    rows = report.get("eventOutcomes", [])
    index = {row["id"]: row for row in rows}
    assert len(index) == len(rows), "Duplicate event operation IDs"
    return index


def state_core(record):
    return {field: value for field, value in record.items() if field not in {"trueLinks", "falseLinks"}}


def compare(before, after):
    old, new = load(before), load(after)
    for field in ("argentumCommit", "objective", "commander", "eligibility", "mechanicalInputCards", "actions", "facts"):
        assert old[field] == new[field], (before.name, field)

    old_index, new_index = index_outcomes(old), index_outcomes(new)
    for identifier, row in old_index.items():
        assert new_index[identifier] == row, (before.name, identifier)
    added_operations = [row for identifier, row in new_index.items() if identifier not in old_index]
    assert all(row["kind"] in NEW_KINDS for row in added_operations), before.name

    original, candidate = old["results"], new["results"]
    previous = Counter(map(key, original))
    current = Counter(map(key, candidate))
    assert not previous - current, (before.name, previous - current)
    assert len(candidate) == len(original) or old["objective"] == DEATH, before.name
    remaining = previous.copy()
    added = []
    for row in candidate:
        identity = key(row)
        if remaining[identity]:
            remaining[identity] -= 1
        else:
            assert row["branch"] == "DEATH_TRIGGERED_OUTCOME" and row["operation"] in NEW_KINDS
            added.append(row)

    admitted = {row["card"] for row in original if row["verdict"] == "MATCH"}
    new_cards = sorted({row["card"] for row in added if row["verdict"] == "MATCH"} - admitted)
    families = defaultdict(list)
    for row in added:
        families[row["operation"]].append(row)

    if old["objective"] == HISTORY:
        old_deps, new_deps = old["stateDependencies"], new["stateDependencies"]
        assert list(map(state_core, old_deps)) == list(map(state_core, new_deps)), before.name
        for a, b in zip(old_deps, new_deps):
            for field in ("trueLinks", "falseLinks"):
                prior = a.get(field, {})
                latest = b.get(field, {})
                assert prior.get("factIds", []) == latest.get("factIds", [])
                assert set(prior.get("eventOutcomeIds", [])) <= set(latest.get("eventOutcomeIds", []))

    policy_rows = [row for row in candidate if any(d["stage"] == "legacy-draw-token-recipient" for d in row["decisions"])]
    return {
        "eligible": len(new["mechanicalInputCards"]),
        "beforeSha256": digest(before),
        "afterSha256": digest(after),
        "before": counts(original),
        "after": counts(candidate),
        "newResponseRows": counts(added),
        "newResponseKinds": {kind: counts(rows) for kind, rows in sorted(families.items())},
        "newlyMatchedCards": new_cards,
        "newRows": [
            {field: row[field] for field in ("card", "face", "actionId", "abilityPath", "operation", "verdict")}
            | {"affectedPlayers": row["eventOutcome"]["playerBinding"]["description"]}
            for row in added
        ],
        "newSharedOperations": dict(Counter(row["kind"] for row in added_operations)),
        "legacyRecipientPolicy": sorted({row["card"] for row in policy_rows}),
        "changedSections": sorted(field for field in old.keys() | new.keys() if old.get(field) != new.get(field)),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("legacy_baseline", type=Path)
    parser.add_argument("history_baseline", type=Path)
    parser.add_argument("candidate", type=Path)
    parser.add_argument("receipt", type=Path)
    args = parser.parse_args()
    objectives = (DEATH, HISTORY, "reanimate-own-creatures", "create-creature-tokens", "stock-own-graveyard")
    names = {f"{commander}-{objective}.json" for commander in ("judith", "krenko") for objective in objectives}
    names |= {"kenrith-death.json", "kenrith-history.json"}
    receipt = {}
    for name in sorted(names):
        base = args.history_baseline if HISTORY in name or name == "kenrith-history.json" else args.legacy_baseline
        item = compare(base / name, args.candidate / name)
        receipt[name] = item
        print(name, item["eligible"], item["before"], item["after"], "new admitted cards", len(item["newlyMatchedCards"]))
    args.receipt.write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
