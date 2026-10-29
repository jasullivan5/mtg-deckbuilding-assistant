#!/usr/bin/env python3
"""Validate controlled fork adoption reports without suppressing mechanical differences."""
import argparse, collections, gc, hashlib, json, pathlib, subprocess, xml.etree.ElementTree as ET
ROOT=pathlib.Path(__file__).resolve().parent.parent
REF=ROOT/"reference/fork-foundation"
OLD="ebf5bf2e9fbb80d13a556d75491dd462e10c206f"
UP="https://github.com/wingedsheep/argentum-engine"
FORK="https://github.com/jasullivan5/argentum-engine"
def read(p):return json.loads(pathlib.Path(p).read_text(encoding="utf-8-sig"))
def save(p,v):pathlib.Path(p).write_text(json.dumps(v,indent=2,ensure_ascii=False)+"\n",encoding="utf-8",newline="\n")
def text_hash(p):return hashlib.sha256(pathlib.Path(p).read_bytes().replace(b"\r\n",b"\n")).hexdigest()
def binary_hash(p):return hashlib.sha256(pathlib.Path(p).read_bytes()).hexdigest()
def git(root,*args):return subprocess.check_output(["git","-C",str(root),*args],encoding="utf-8").strip()
def suite_summary(directory):
    roots=[ET.parse(p).getroot() for p in directory.glob("TEST-*.xml")]
    assert roots,directory
    summary={k:sum(int(r.get(k,0)) for r in roots) for k in ["tests","failures","errors","skipped"]}
    summary["suites"]=len(roots)
    summary["suiteCounts"]=[{"suite":r.get("name"),"tests":int(r.get("tests",0))} for r in roots]
    return summary

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--validation-root",type=pathlib.Path,required=True)
    parser.add_argument("--output-base",type=pathlib.Path,default=ROOT/"generated/fork-foundation")
    args=parser.parse_args();base=args.output_base;validation=args.validation_root.resolve()
    pin=(ROOT/"ARGENTUM_COMMIT").read_text(encoding="utf-8").strip()
    engine=validation/"argentum"
    assert git(engine,"rev-parse","HEAD")==pin
    assert git(engine,"remote","get-url","origin")==FORK+".git"
    assert not git(engine,"status","--porcelain")
    assert git(ROOT,"rev-parse","HEAD:cli")==git(validation,"rev-parse","HEAD:cli")
    assert not git(ROOT,"diff","--","cli")
    assert git(ROOT,"show","461791c1ae531800ca5f2da868b9954f18bf6ad1:ARGENTUM_COMMIT")==OLD
    pre=read(REF/"pre-adoption-verification.json")
    assert pre["runtimeEngineRevision"]==pin and pre["results"]["native"]["tests"]==7507
    assert len(pre["focusedCases"])==17 and pre["results"]["application"]["tests"]==457
    assert read(REF/"setup-verification.json")["allPassed"]
    normal=suite_summary(validation/"cli/build/test-results/test")
    assert normal["tests"]==457 and not any(normal[k] for k in ["failures","errors","skipped"])
    mutations=read(validation/"generated/translation-reliability/mutations.json")
    assert len(mutations)==9 and all(x["controlPassed"] and x["executionDetected"] and not x["executionConfounded"] for x in mutations)
    reviews=read(REF/"benchmark-rebindings.json")
    assert reviews["enginePin"]==pin and reviews["originalReviewsPreserved"]
    for row in reviews["reviewRebindings"]:
        old_review,new_review=read(ROOT/row["original"]),read(ROOT/row["candidate"])
        assert text_hash(ROOT/row["original"])==row["originalSha256Utf8Lf"]
        assert text_hash(ROOT/row["candidate"])==row["candidateSha256Utf8Lf"]
        assert old_review["argentumCommit"]==OLD and new_review["argentumCommit"]==pin
        assert old_review["cases"]==new_review["cases"] and old_review["frozenAt"]==new_review["frozenAt"]
        assert {k:v for k,v in old_review.items() if k not in ["argentumCommit","reviewVersion","reviewMethod"]}=={k:v for k,v in new_review.items() if k not in ["argentumCommit","reviewVersion","reviewMethod"]}
    before_assurance={x["oracleId"]:x for x in read(base/"before/judith-a.json")["translationAssurance"]}
    after_assurance={x["oracleId"]:x for x in read(base/"after/judith-a.json")["translationAssurance"]}
    assert len(before_assurance)==22 and before_assurance.keys()==after_assurance.keys()
    assert collections.Counter(x["qualification"] for x in after_assurance.values())=={"PROVISIONAL":21,"BLOCKED":1}
    assurance_changes=[]
    for identity,a in before_assurance.items():
        b=after_assurance[identity];tatter=b["card"]=="Tatterkite"
        assert b["evidenceCurrent"] and b["evidence"]["independentApproval"] is None
        assert {k:v for k,v in a.items() if k not in ["engineExecution","evidence"]}=={k:v for k,v in b.items() if k not in ["engineExecution","evidence"]}
        assert b["engineExecution"]==("BOUNDED_EVIDENCE_PASSED" if tatter else a["engineExecution"])
        ea,eb=a["evidence"],b["evidence"]
        changed=["argentumCommit","tests","limitations","execution","concerns"]
        assert {k:v for k,v in ea.items() if k not in changed}=={k:v for k,v in eb.items() if k not in changed}
        assert ea["argentumCommit"]==OLD and eb["argentumCommit"]==pin
        assert eb["tests"][:-1]==ea["tests"]
        last=eb["tests"][-1];assert last["path"]=="reference/fork-foundation/pre-adoption-verification.json"
        assert last["sha256"]==text_hash(ROOT/last["path"]) and last["hashFormat"]=="SHA256_UTF8_LF"
        assert len(eb["limitations"])==len(ea["limitations"])+1
        for old_limit,new_limit in zip(ea["limitations"],eb["limitations"]):
            expected="Historical on "+OLD+": "+old_limit if tatter and "collection placement violates counter prohibition at this SDK pin" in old_limit else old_limit
            assert new_limit==expected
        assert eb["execution"]==("BOUNDED_EVIDENCE_PASSED" if tatter else ea["execution"])
        assert len(ea["concerns"])==len(eb["concerns"])
        for ca,cb in zip(ea["concerns"],eb["concerns"]):
            if not tatter:
                assert ca==cb
                continue
            assert {k:v for k,v in ca.items() if k not in ["reason","reproduction","nextAction"]}=={k:v for k,v in cb.items() if k not in ["reason","reproduction","nextAction"]}
            assert tatter and cb["state"]=="CONFIRMED" and not cb["resolutionEvidence"]
            assert cb["reason"].startswith(ca["reason"]) and OLD in cb["reason"]
            assert cb["reproduction"]=="Historical at "+OLD+": "+ca["reproduction"]
            assert cb["nextAction"].startswith("Historical action: "+ca["nextAction"])
        assurance_changes.append({"card":b["card"],"qualification":b["qualification"],"evidenceCurrent":True,"changedFields":[k for k in a if a[k]!=b[k]]})
    def canonical(value,after):
        if isinstance(value,list):return [canonical(x,after) for x in value]
        if isinstance(value,dict):
            identity=value.get("oracleId")
            if identity in before_assurance and "qualification" in value and "evidenceCurrent" in value:
                expected=(after_assurance if after else before_assurance)[identity]
                assert value==expected,("unexpected assurance occurrence",identity)
                return before_assurance[identity]
            return {k:canonical(v,after) for k,v in value.items()}
        if after and isinstance(value,str):
            if value==pin:return OLD
            if value==FORK:return UP
            if value.startswith(FORK+"/"):return value.replace(FORK,UP,1).replace(pin,OLD)
        return value
    comparisons=[]
    for slug in ["judith","krenko","oviya"]:
        for name in [f"{slug}-{suffix}.json" for suffix in ["a","b","c"]]+[f"coverage-{slug}.json",f"missing-{slug}.json"]:
            a,b=[read(base/stage/name) for stage in ["before","after"]]
            ignored=["timingsMillis","correctionValidationMillis"] if name.startswith("coverage-") else []
            for key in ignored:a.pop(key,None);b.pop(key,None)
            left,right=canonical(a,False),canonical(b,True)
            if left!=right:
                keys=[key for key in left if left.get(key)!=right.get(key)]
                raise AssertionError((name,"unexpected semantic/report difference",keys))
            comparisons.append({"report":name,"equalAfterDeclaredBindingNormalization":True,"ignoredTimingFields":ignored,"beforeSha256Utf8Lf":text_hash(base/"before"/name),"afterSha256Utf8Lf":text_hash(base/"after"/name),"counts":b.get("counts")})
            del a,b,left,right;gc.collect()
    catalog_reports={}
    for slug in ["judith","krenko"]:
        for suffix in ["a","b","c"]:
            key=f"{slug}-{suffix}";report=read(base/"after"/(key+".json"))
            payload=(json.dumps(report,sort_keys=True,separators=(",",":"),ensure_ascii=False)+"\n").encode("utf-8")
            catalog_reports[key]={"sha256CanonicalJsonUtf8":hashlib.sha256(payload).hexdigest(),"counts":report["counts"]}
    save(REF/"catalog-baseline.json",{"argentumCommit":pin,"hashFormat":"SHA256 canonical JSON UTF-8: sorted keys, compact separators, ensure_ascii=False, trailing LF","establishedFrom":"Fresh published-source outputs proven equivalent to the approved baseline except declared bindings","reports":catalog_reports})
    before_manifest=read(base/"before/build-configuration.json")
    before_dist=ROOT/"generated/engine-counter-correction/approved-dist"
    for name,h in before_manifest["jarSha256"].items():assert binary_hash(before_dist/"lib"/name)==h
    dist=validation/"cli/build/install/mtg-assistant"
    after_manifest={"configuration":"NORMAL_PUBLISHED_FORK_PIN_PROPOSAL","actualRuntimeEngineRevision":pin,"reportedEngineRevision":pin,"sourceRepository":git(engine,"remote","get-url","origin"),"applicationSourceCommit":git(validation,"rev-parse","HEAD"),"applicationCliTree":git(validation,"rev-parse","HEAD:cli"),"engineSourceClean":True,"experimentalInitUsed":False,"jarSha256":{name:binary_hash(dist/"lib"/name) for name in before_manifest["jarSha256"]}}
    save(base/"after/build-configuration.json",after_manifest)
    save(REF/"normal-verification.json",{"configuration":after_manifest,"application":normal,"mutations":mutations,"command":"scripts/bootstrap.ps1 -NoBuild; bash scripts/bootstrap.sh --no-build; dev.ps1 test --rerun :assistant-cli:installDist (experimental flags absent)","logSha256Raw":binary_hash(ROOT/"generated/fork-foundation/normal-application.log"),"approvedMainPinStill":OLD})
    save(REF/"controlled-comparison.json",{"hashFormat":"SHA256_UTF8_LF","beforeConfiguration":before_manifest,"afterConfiguration":after_manifest,"allowedBindingChanges":["Exact engine revision", "Canonical fork provenance URLs", "Validated current assurance execution/evidence binding; qualification unchanged"],"comparisons":comparisons,"assurance":assurance_changes,"tatterkiteConcernStillConfirmed":True,"independentQualificationsAdded":0,"definitionAndEligibilitySemanticsUnchanged":True,"executionNote":"After-run interrupted after coverage/missing Oviya; remaining three Oviya discovery and three audit-diff commands resumed against the same distribution with zero exits", "resumedCommands":read(base/"after/resumed-timings.json")})
    print("Validated 9 complete discovery, 3 coverage and 3 missing-card reports; semantics unchanged. Normal 457 tests and 9 mutation controls/detections pass; 21 provisional/1 blocked, no promotions.")
if __name__=="__main__":main()
