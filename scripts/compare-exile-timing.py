"""Compare the two native timing corrections without hiding unrelated mechanical drift."""
import argparse
from collections import Counter, defaultdict
import hashlib
import json
from pathlib import Path

CORRECTED = {"Oblivion Ring", "Banisher Priest"}


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def normalize(value, old, new):
    if isinstance(value, dict):
        return {k: (old if k == "engineRevision" and v == new else normalize(v, old, new))
                for k, v in value.items()}
    if isinstance(value, list):
        return [normalize(v, old, new) for v in value]
    if isinstance(value, str):
        for kind in ("blob", "tree"):
            value = value.replace(f"https://github.com/jasullivan5/argentum-engine/{kind}/{new}/",
                                  f"https://github.com/jasullivan5/argentum-engine/{kind}/{old}/")
    return value


def grouped(rows):
    result = defaultdict(list)
    for row in rows:
        result[row["card"]].append(row)
    return result


def compare(before, after):
    assert before.keys() == after.keys()
    # Historical evidence is never normalized or refreshed by a new engine build.
    assert before["translationAssurance"] == after["translationAssurance"]
    old, new = before["argentumCommit"], after["argentumCommit"]
    after = normalize(after, old, new)
    after["argentumCommit"] = old
    changes, links = {}, []
    for key in before:
        a, b = before[key], after[key]
        if key == "counts":
            changes[key] = {k: [a.get(k), b.get(k)] for k in a.keys() | b.keys() if a.get(k) != b.get(k)}
            continue
        if isinstance(a, list) and a and all(isinstance(row, dict) and "card" in row for row in a):
            aa, bb = grouped(a), grouped(b)
            changed = []
            for card in aa.keys() | bb.keys():
                x, y = aa[card], bb[card]
                if key == "actions":
                    lx = [(r["id"], r.get("linkedMovements", [])) for r in x]
                    ly = [(r["id"], r.get("linkedMovements", [])) for r in y]
                    if lx != ly:
                        links.append({"card": card, "before": lx, "after": ly})
                    x = [{k:v for k,v in r.items() if k != "linkedMovements"} for r in x]
                    y = [{k:v for k,v in r.items() if k != "linkedMovements"} for r in y]
                if x != y:
                    assert card in CORRECTED, (key, card, "unrelated mechanical drift")
                    changed.append({"card":card, "beforeRows":len(x), "afterRows":len(y)})
            if changed:
                changes[key] = sorted(changed, key=lambda r:r["card"])
        else:
            assert a == b, (key, "unexpected report drift")
    # All admissions remain exact, including those for the corrected definitions.
    assert [r for r in before["results"] if r["verdict"] == "MATCH"] == [
        r for r in after["results"] if r["verdict"] == "MATCH"]
    relations = [r for a in after["actions"] for r in a.get("linkedMovements", [])]
    return {"engineBefore":old,"engineAfter":new,
            "population":len(after["mechanicalInputCards"]),
            "populationSha256":hashlib.sha256(json.dumps(after["mechanicalInputCards"],ensure_ascii=False).encode()).hexdigest(),
            "verdictsBefore":dict(Counter(r["verdict"] for r in before["results"])),
            "verdictsAfter":dict(Counter(r["verdict"] for r in after["results"])),
            "attributedChanges":changes,
            "relationshipKinds":dict(Counter(r["kind"] for r in relations)),
            "relationshipChanges":sorted(links,key=lambda r:r["card"])}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("baseline", "candidate", "output"):
        parser.add_argument(name, type=Path)
    args = parser.parse_args()
    receipt = {}
    for path in sorted(args.baseline.glob("*.json")):
        candidate = args.candidate/path.name
        summary = compare(json.loads(path.read_text(encoding="utf-8")),json.loads(candidate.read_text(encoding="utf-8")))
        summary.update(baselineSha256=digest(path),candidateSha256=digest(candidate))
        receipt[path.stem] = summary
        print(path.stem, "PASS", summary["verdictsAfter"])
    assert len(receipt) == 18
    args.output.write_text(json.dumps(receipt,indent=2)+"\n",encoding="utf-8")


if __name__ == "__main__":
    main()
