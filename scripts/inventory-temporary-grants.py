"""Join compiled native grant effect declarations to production extraction by card, face and path."""
import argparse
from collections import Counter
import json
from pathlib import Path

FAMILIES = {"GrantActivatedAbility": "activated", "GrantTriggeredAbility": "triggered",
            "GrantActivatedAbilityToGroup": "groupActivated"}


def declarations(node, path="script"):
    if isinstance(node, dict):
        kind = FAMILIES.get(node.get("type"))
        if kind and "ability" in node and ("target" in node or kind == "groupActivated"):
            yield kind, path, node
        for key, value in node.items():
            yield from declarations(value, f"{path}.{key}")
    elif isinstance(node, list):
        for index, value in enumerate(node):
            yield from declarations(value, f"{path}[{index}]")


def measure(stream):
    roots, extracted = {}, {}
    for line in stream:
        row = json.loads(line)
        container = (row["card"], row["face"])
        if row["path"] == "script":
            assert container not in roots
            roots[container] = row["native"]
        for grant in row.get("temporaryGrants", []):
            key = (*container, grant["path"])
            assert key not in extracted and grant["actionId"] == row["id"], key
            extracted[key] = grant
    rows = {kind: [] for kind in FAMILIES.values()}
    for (card, face), script in roots.items():
        for kind, path, node in declarations(script):
            grant = extracted.pop((card, face, path), None)
            if grant:
                assert grant["effect"] == node and grant["body"]["action"]["path"] == path + ".ability"
            rows[kind].append(dict(card=card, face=face, path=path,
                duration=node.get("duration", {"type": "EndOfTurn"}),
                recipient=node.get("target", node.get("filter")),
                traversed=grant is not None, creation=grant["support"] if grant else "UNKNOWN",
                bodyFacts=len(grant["body"]["facts"]) if grant else 0,
                bodyUnresolved=len(grant["body"]["unsupported"]) if grant else None,
                unresolved=grant["unresolved"] if grant else ["Native instruction outside the existing effect walk"]))
    assert not extracted, list(extracted)

    def summary(records):
        return dict(declarations=len(records), physicalCards=len({row["card"] for row in records}),
            authoringContainers=len({(row["card"], row["face"]) for row in records}),
            durations=dict(Counter(row["duration"]["type"] for row in records)),
            recipients=dict(Counter((row["recipient"] or {}).get("type", "GroupFilter") for row in records)),
            traversed=sum(row["traversed"] for row in records), creationMatch=sum(row["creation"] == "MATCH" for row in records),
            bodyWithFacts=sum(row["bodyFacts"] > 0 for row in records),
            bodyWithoutUnsupportedNodes=sum(row["bodyUnresolved"] == 0 for row in records),
            unresolved=dict(Counter(reason for row in records for reason in row["unresolved"])))

    return dict(physicalCards=len({card for card, _ in roots}), authoringContainers=len(roots),
        families={kind: summary(records) for kind, records in rows.items()}, declarations=rows)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("compiled", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    with args.compiled.open(encoding="utf-8-sig") as compiled:
        result = measure(compiled)
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({key: result[key] for key in ("physicalCards", "authoringContainers", "families")}))
