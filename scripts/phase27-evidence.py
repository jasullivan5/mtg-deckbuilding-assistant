#!/usr/bin/env python3
"""Reconcile real coverage and source eligibility, and compare unchanged discovery records."""
import argparse
import collections
import gzip
import hashlib
import json
import pathlib
import subprocess
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parent.parent
NEW_CARDS = {"Krenko's Command", "Bitterblossom"}


def read(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def digest(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def main(args):
    output = {"baseRevision":"463ce86f288db73486dcd0a712f4b8469bf3cd7f", "phase26Pr":1,
              "implementationRevision":subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip(),
              "snapshot":read(pathlib.Path(str(args.snapshot)+".manifest.json")), "coverage":{}, "regressions":{}, "tests":{}}
    with gzip.open(args.snapshot,"rt",encoding="utf-8") as source:
        cards = [json.loads(line) for line in source]
    assert len({c.get("oracle_id") for c in cards}) == len(cards), "This independent check expects the actual unique-ID snapshot"
    by_name = {c["name"]:c for c in cards}
    land_colors = {"Plains":"W","Island":"U","Swamp":"B","Mountain":"R","Forest":"G"}
    all_reports = {}
    for slug in ("judith","krenko","oviya"):
        path = args.generated / "coverage-after" / (slug+".json")
        report = read(path)
        assert report["snapshot"]["sha256"] == output["snapshot"]["sha256"] == digest(args.snapshot)
        allowed = set(report["allowedIdentity"])
        assert set(by_name[report["commander"]]["color_identity"]) == allowed
        assert by_name[report["commander"]]["legalities"]["commander"] == "legal"
        # Independent source-led ordinary inclusion. No native registry or application code is called.
        def include(c):
            types = " ".join([c.get("type_line","")]+[f.get("type_line","") for f in c.get("card_faces",[])]).replace("—"," ").split()
            return c["legalities"]["commander"] == "legal" and set(c["color_identity"]) <= allowed and all(
                color in allowed for subtype,color in land_colors.items() if subtype in types)
        expected = {c["oracle_id"] for c in cards if include(c)}
        actual = {r["external"]["oracleId"] for r in report["records"]}
        assert actual == expected, (slug,"External eligibility disagrees with independent bulk check",len(actual),len(expected))
        counts = report["counts"]
        assert len(actual) == counts["eligible_oracle_cards"] == len(report["records"])
        statuses = collections.Counter(r["status"].lower() for r in report["records"])
        assert all(counts[k] == v for k,v in statuses.items())
        assert sum(statuses.values()) == len(actual)
        assert counts["eligible_oracle_cards"]+counts["excluded_records"]+counts["unresolved_pool_records"] == len(cards)
        candidate_differences = collections.Counter()
        examples = {}
        for row in report["records"]:
            candidates = row["mapping"]["candidates"]
            if len(candidates) != 1:
                continue
            native = candidates[0]
            reasons = []
            if native["identity"] is None:
                reasons.append("native_full_identity_missing")
            elif set(native["identity"]) != set(row["external"]["colorIdentity"]):
                reasons.append("native_external_identity_disagreement")
            if native["commanderLegality"] != "MATCH":
                reasons.append("native_legality_"+native["commanderLegality"].lower())
            for reason in reasons:
                candidate_differences[reason] += 1
                examples.setdefault(reason,[])
                if len(examples[reason]) < 6:
                    examples[reason].append(row["external"]["name"])
        missing = [r for r in report["records"] if r["status"] == "NATIVE_DEFINITION_MISSING"]
        output["coverage"][slug] = {"commander":report["commander"], "counts":counts, "poolExclusions":report["poolExclusions"],
            "timingsMillis":report["timingsMillis"], "reportBytes":path.stat().st_size, "reportSha256":digest(path),
            "independentSourcePoolEqual":True, "nameCandidateEligibilityDifferences":dict(candidate_differences),
            "nameCandidateExamplesNotAuthoritativeMappings":examples,
            "missingFirst20":[{"oracleId":r["external"]["oracleId"],"name":r["external"]["name"],
                               "colorIdentity":r["external"]["colorIdentity"],"translationStatus":r["translationStatus"]} for r in missing[:20]],
            "ambiguousExamples":[r["external"]["name"] for r in report["records"] if r["status"] == "AMBIGUOUS_MAPPING"][:8]}
        all_reports[slug] = report
    representative_names = ["Sol Ring","Rakdos Guildmage","Beseech the Queen","Shatterskull Smashing // Shatterskull, the Hammer Pass",
        "Bala Ged Recovery // Bala Ged Sanctuary","Jeweled Lotus","The Prismatic Piper","Burakos, Party Leader",
        "Persistent Petitioners","Hallowed Fountain","Transguild Courier","Dryad Arbor","Fire // Ice"]
    output["sourceRepresentatives"] = []
    for name in representative_names:
        card = by_name[name]
        row = {k:card.get(k) for k in ("name","oracle_id","color_identity","type_line","oracle_text","mana_cost","layout","keywords")}
        row["commanderLegality"] = card["legalities"]["commander"]
        row["ordinaryInclusion"] = {s:any(r["external"]["oracleId"]==card["oracle_id"] for r in report["records"]) for s,report in all_reports.items()}
        row["constructionNotes"] = next((r["constructionNotes"] for report in all_reports.values() for r in report["records"] if r["external"]["oracleId"]==card["oracle_id"]),[])
        output["sourceRepresentatives"].append(row)
    for slug in ("judith","krenko"):
        for letter in "abc":
            before_path=args.generated/"before"/f"{slug}-{letter}.json"
            after_path=args.generated/"after"/f"{slug}-{letter}.json"
            before=read(before_path); after=read(after_path)
            fields=("eligibility","results","actions","facts","unsupported","structuralSupport")
            unchanged={field:[r for r in after[field] if r["card"] not in NEW_CARDS] == before[field] for field in fields}
            unchanged["mechanicalInputCards"]=[n for n in after["mechanicalInputCards"] if n not in NEW_CARDS] == before["mechanicalInputCards"]
            assert all(unchanged.values()), (slug,letter,"Unexpected preexisting record change",unchanged)
            assert before["objective"] == after["objective"] and before["allowedIdentity"] == after["allowedIdentity"]
            added=[r for r in after["results"] if r["card"] in NEW_CARDS and r["verdict"] == "MATCH"]
            expected_additions = (2 if slug=="judith" else 1) if letter=="c" else 0
            assert len(added) == expected_additions
            diff=read(args.generated/"diffs"/f"{slug}-{letter}.json")
            output["regressions"][f"{slug}-{letter}"]={"beforeSha256":digest(before_path),"afterSha256":digest(after_path),
                "unchangedPreexistingFields":unchanged,"newEligibleCards":[r["card"] for r in after["eligibility"] if r["card"] in NEW_CARDS and r["format"]==r["deck"]=="MATCH"],
                "newAcceptedPathways":[{"card":r["card"],"path":r["abilityPath"],"verdict":r["verdict"]} for r in added],
                "auditDiffCounts":diff["counts"],"auditDiffTransitions":diff["transitions"],"unexpectedRegressions":0}
    totals=collections.Counter()
    suites=[]
    for path in sorted((ROOT/"cli/build/test-results/test").glob("TEST-*.xml")):
        suite=ET.parse(path).getroot()
        counts={k:int(suite.attrib.get(k,0)) for k in ("tests","failures","errors","skipped")}
        totals.update(counts)
        suites.append({"name":suite.attrib["name"],**counts,"xmlSha256":digest(path)})
    assert totals["tests"] >= 272 and not (totals["failures"] or totals["errors"] or totals["skipped"])
    output["tests"]={"totals":dict(totals),"suites":suites,"baselineTests":240,"newEngineScenarios":11,"offlineDownloaderTests":3}
    output["timings"] = read(args.generated/"timings-after.json")
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(output,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print(json.dumps({"coverage":{k:v["counts"] for k,v in output["coverage"].items()},"tests":dict(totals),"unexpectedRegressions":0},indent=2))


if __name__ == "__main__":
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--snapshot",type=pathlib.Path,required=True)
    parser.add_argument("--generated",type=pathlib.Path,default=ROOT/"generated/phase27")
    parser.add_argument("--output",type=pathlib.Path,default=ROOT/"samples/phase2_7/verification.json")
    main(parser.parse_args())
