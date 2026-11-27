"""Classify compiled nested declarations and the direct wrappers emitted by the shared walker."""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import re
from runpy import run_path

nodes = run_path(str(Path(__file__).with_name("inventory-legacy-exile.py")))["nodes"]


def inventory(path, family):
    node_type = f"Grant{family}Ability"
    support_key = "support" if family == "Activated" else "recipientSupport"
    cards, containers = set(), set()
    declarations = []
    for line in path.open(encoding="utf-8-sig"):
        if not line.startswith("{"):
            continue
        row = json.loads(line)
        card, face = row["physical"], row["face"]
        cards.add(card)
        containers.add((card, face))
        compiled = dict(nodes(row["script"]))
        records = {g["path"]: g for g in row["grants"] if g.get("conditional")}
        for native_path, node in compiled.items():
            if node.get("type") != node_type or "target" in node or "duration" in node:
                continue
            if re.fullmatch(r"script.staticAbilities\[\d+\]", native_path):
                continue
            owner_path = native_path.rsplit(".", 1)[0]
            wrapper = compiled.get(owner_path, {})
            direct = (wrapper.get("type") == "ConditionalStaticAbility" and
                      bool(re.fullmatch(r"script.staticAbilities\[\d+\]\.ability", native_path)))
            record = records.pop(native_path, None)
            assert record is None or direct
            declarations.append(dict(card=card, face=face, path=native_path, source=row["source"],
                directConditional=direct, wrapper=wrapper.get("type"), nativeCondition=wrapper.get("condition"),
                traversed=record is not None, recipientSupport=record and record[support_key],
                conditionSupport=record and record["conditional"]["support"],
                eventSupport=record and record.get("eventSupport"),
                unresolved=record and record["unresolved"]))
        assert not records, (card, face, records)
    return dict(physicalCards=len(cards), authoringContainers=len(containers),
        nestedDeclarations=len(declarations), nestedPhysicalCards=len({d["card"] for d in declarations}),
        directConditional=sum(d["directConditional"] for d in declarations),
        traversed=sum(d["traversed"] for d in declarations),
        conditionSupport=dict(Counter(d["conditionSupport"] for d in declarations if d["traversed"])),
        recipientSupport=dict(Counter(d["recipientSupport"] for d in declarations if d["traversed"])),
        bothSupported=sum(d["recipientSupport"] == d["conditionSupport"] == "MATCH" for d in declarations),
        sourceSha256=hashlib.file_digest(path.open("rb"), "sha256").hexdigest(),
        declarations=declarations)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    for argument in ("activated", "triggered", "output"):
        parser.add_argument(argument, type=Path)
    args = parser.parse_args()
    catalog = {name: inventory(path, name.capitalize()) for name, path in
               (("activated", args.activated), ("triggered", args.triggered))}
    args.output.write_text(json.dumps(catalog, indent=2) + "\n", encoding="utf-8")
    for family, results in catalog.items():
        print(family, {k: v for k, v in results.items() if k != "declarations"})
