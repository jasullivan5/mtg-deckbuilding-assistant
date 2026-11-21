"""Count exact compiled legacy-helper nodes, without inferring Oracle fidelity from SDK names.

Input: docs/evidence/LegacyExileInventory.java JSONL, run against the recorded installed pair.
Output: compact census, including every affected physical/face/ability/effect path.
"""
import collections
import hashlib
import json
from pathlib import Path
import re
import sys


def nodes(value, path="script"):
    if isinstance(value, dict):
        yield path, value
        for key, child in value.items():
            yield from nodes(child, f"{path}.{key}")
    elif isinstance(value, list):
        for index, child in enumerate(value):
            yield from nodes(child, f"{path}[{index}]")


def declaration(path):
    root = re.match(r"script\.(?:triggeredAbilities|activatedAbilities|stateTriggeredAbilities)\[\d+\]", path)
    if root:
        return root.group()
    chapter = re.match(r"script\.sagaChapters\[\d+\]", path)
    if chapter:
        return chapter.group()
    return path.rsplit(".effect", 1)[0]


def census(path):
    cards, containers, rows = set(), set(), []
    related = collections.Counter()
    for line in Path(path).open(encoding="utf-8-sig"):
        if not line.startswith("{"):
            continue
        row = json.loads(line)
        cards.add(row["physical"])
        containers.add((row["physical"], row["face"]))
        tree=dict(nodes(row["script"]))
        for native_path, native in tree.items():
            kind = native.get("type")
            if kind == "MoveUntilSourceLeaves" or kind == "MoveToZone" and native.get("linkToSource"):
                related[kind] += 1
            if kind != "ExileUntilLeaves":
                continue
            ability=tree.get(declaration(native_path), {})
            rows.append(dict(physical=row["physical"], face=row["face"], faceName=row["name"], nativePath=row.get("nativePath"),
                             declaration=declaration(native_path), path=native_path, native=native,
                             declarationSha256=hashlib.sha256(json.dumps(ability,sort_keys=True,separators=(",", ":"),ensure_ascii=False).encode()).hexdigest(),
                             review="UNREVIEWED"))
    rows.sort(key=lambda row: (row["physical"], row["face"], row["path"]))
    return dict(units="Native physical roots; script authoring containers are not necessarily printed faces. Declaration paths include saga chapters, not invented runtime action IDs. No source fidelity or execution inferred.",
                summary=dict(nativePhysicalCards=len(cards), scriptAuthoringContainers=len(containers),
                             affectedPhysicalCards=len({r["physical"] for r in rows}),
                             affectedFaceContainers=len({(r["physical"], r["face"]) for r in rows}),
                             abilityDeclarations=len({(r["physical"], r["face"], r["declaration"]) for r in rows}),
                             effectInstances=len(rows), relatedEffectInstances=dict(related)), rows=rows)


if __name__ == "__main__":
    result = census(sys.argv[1])
    if len(sys.argv) > 3:
        frozen_path=Path(sys.argv[3])
        frozen=json.loads(frozen_path.read_text(encoding="utf-8-sig"))
        cohort=frozen["cases"]
        result["sourceCohort"] = dict(requirementsLfSha256=hashlib.sha256(frozen_path.read_text(encoding="utf-8-sig").encode()).hexdigest(),
            engineBase=frozen["engineBase"], cases={name: dict(family=c["family"],nativePath=c["nativePath"],oracleId=c["source"]["oracleId"])
                                                     for name,c in cohort.items()},
            scope="Frozen source classification only; native execution observations are retained separately. Not whole-card qualification.")
        for row in result["rows"]:
            if row["physical"] in cohort:
                row["review"]="SOURCE_REQUIREMENTS_FROZEN"
                row["family"]=cohort[row["physical"]]["family"]
        result["summary"]["sourceReviewedLegacyPhysicalCards"]=len({r["physical"] for r in result["rows"] if r["review"]!="UNREVIEWED"})
        result["summary"]["unreviewedLegacyPhysicalCards"]=len({r["physical"] for r in result["rows"] if r["review"]=="UNREVIEWED"})
    Path(sys.argv[2]).write_text(json.dumps(result, indent=2, ensure_ascii=False)+"\n", encoding="utf-8")
    print(json.dumps(result["summary"], indent=2))
