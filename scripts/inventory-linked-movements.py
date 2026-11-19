"""Reduce a physical-card/face native inventory. Candidate patterns are not proven relationships."""
import collections
import json
import pathlib
import re
import sys

MOVES = {"MoveToZone", "MoveCollection", "ExileUntilLeaves", "MoveUntilSourceLeaves", "ExileWithAurasNotingCounters", "ReturnOneFromLinkedExile", "ReturnNotedExileTappedWithAuras"}


def nodes(value, path="script"):
    if isinstance(value, dict):
        yield path, value
        for k, v in value.items():
            yield from nodes(v, f"{path}.{k}")
    elif isinstance(value, list):
        for i, v in enumerate(value):
            yield from nodes(v, f"{path}[{i}]")


def action(path):
    for key in ("triggeredAbilities", "activatedAbilities", "stateTriggeredAbilities"):
        marker = f"script.{key}["
        if path.startswith(marker):
            return path[:path.index("]")+1]
    return "script.spell" if path.startswith("script.spellEffect") else path


def main():
    rows = []
    total = collections.Counter()
    all_cards, all_faces = set(), 0
    for line in pathlib.Path(sys.argv[1]).open(encoding="utf-8-sig"):
        if not line.startswith('{'): continue
        row = json.loads(line)
        all_cards.add(row["physical"])
        all_faces += 1
        tree = list(nodes(row["script"]))
        movements = [(p,n) for p,n in tree if n.get("type") in MOVES]
        immediate = []
        linked_reads = [(p,n) for p,n in tree if n.get("type") == "FromLinkedExile"]
        for p,n in tree:
            if n.get("type") != "Composite": continue
            for i,(a,b) in enumerate(zip(n.get("effects",[]), n.get("effects",[])[1:])):
                if a.get("type") == b.get("type") == "MoveToZone" and a.get("target") == b.get("target"):
                    immediate.append(f"{p}.effects[{i}:{i+2}]")
                elif a.get("type") == b.get("type") == "MoveCollection" and (a.get("storeMovedAs") or a.get("from")) == b.get("from"):
                    immediate.append(f"{p}.effects[{i}:{i+2}]")
        producers=[p for p,n in movements if n.get("type") in {"ExileUntilLeaves","ExileWithAurasNotingCounters"} or n.get("linkToSource") is True]
        special=[(p,n) for p,n in movements if n.get("type") not in {"MoveToZone","MoveCollection"}]
        if not immediate and not linked_reads and not producers and not special and not row["links"]: continue
        row.pop("script")
        row.update(movementDeclarations=[{"path":p,"action":action(p),"native":n} for p,n in movements],
            potentialImmediate=immediate, sourceLinkedProducers=producers,
            linkedPileReads=[{"path":p,"native":n} for p,n in linked_reads])
        rows.append(row)
        total.update(movementDeclarations=len(movements),potentialImmediateSequences=len(immediate),
            potentialDeferredSourceFaces=int(bool(producers and linked_reads)),linkedPileReads=len(linked_reads),
            nativeDurationDeclarations=sum(n.get("type")=="MoveUntilSourceLeaves" for _,n in movements))
    links=[l for r in rows for l in r["links"]]
    summary=dict(total,physicalCatalogCards=len(all_cards),catalogFaces=all_faces,
        candidatePhysicalCards=len({r["physical"] for r in rows}),candidateFaces=len(rows),
        candidateRootActionContainers=len({(r["physical"],r["face"],m["action"]) for r in rows for m in r["movementDeclarations"]
            if m["action"]=="script.spell" or re.fullmatch(r"script\.(?:triggeredAbilities|activatedAbilities|stateTriggeredAbilities)\[\d+\]",m["action"])}),
        otherMovementDeclarationContainers=len({(r["physical"],r["face"],m["action"]) for r in rows for m in r["movementDeclarations"]
            if m["action"]!="script.spell" and not re.fullmatch(r"script\.(?:triggeredAbilities|activatedAbilities|stateTriggeredAbilities)\[\d+\]",m["action"])}),
        relationships=len(links),newlySupportedPhysicalCards=len({r["physical"] for r in rows if any(l["support"]=="MATCH" for l in r["links"])}),
        relationshipKinds=dict(collections.Counter(l["kind"] for l in links)),
        unknownReasons=dict(collections.Counter(s for l in links for s in l["unresolved"])))
    pathlib.Path(sys.argv[2]).write_text(json.dumps({"units":"Physical roots and exact native face/action/declaration paths; candidates do not prove linkage or source fidelity", "summary":summary,"rows":rows},indent=2)+"\n",encoding="utf-8")
    print(json.dumps(summary,indent=2))


if __name__ == "__main__": main()
