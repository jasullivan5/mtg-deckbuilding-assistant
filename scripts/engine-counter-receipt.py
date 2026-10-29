#!/usr/bin/env python3
"""Validate retained controlled outputs; does not build an engine or authorize pin adoption."""
import argparse, collections, hashlib, json, pathlib, subprocess
ROOT = pathlib.Path(__file__).resolve().parent.parent
REF = ROOT / "reference/engine-counter-correction"

def read(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))

def text_hash(path):
    return hashlib.sha256(path.read_text(encoding="utf-8").replace("\r\n", "\n").encode("utf-8")).hexdigest()

def git(*args):
    return subprocess.check_output(["git", "-C", str(ROOT), *args], text=True).strip()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-base", type=pathlib.Path, default=ROOT / "generated/engine-counter-correction")
    args = parser.parse_args()
    base = args.output_base
    candidate = read(REF / "candidate.json")
    approved = (ROOT / "reference/fork-foundation/ARGENTUM_COMMIT").read_text().strip()
    assert approved == candidate["approvedRevision"]
    assert git("-C", str(ROOT / "argentum"), "rev-parse", "HEAD") == approved
    assert not git("-C", str(ROOT / "argentum"), "status", "--porcelain")
    assert not git("diff", "6e1c26028fd9dffe6335d9494edce8aa362e32e7", "--", "ARGENTUM_COMMIT", "cli/src/main", "reference/translation-reliability")
    assert text_hash(REF / candidate["patch"]) == candidate["patchSha256"]
    before = read(REF / "native-before.json")
    after = read(REF / "native-focused-after.json")
    assert before["fixtureSha256"] == after["fixtureSha256"]
    assert (before["tests"], before["failures"], after["tests"], after["failures"]) == (17, 10, 17, 0)
    full = read(REF / "candidate-full.json")
    assert full["actualEngineRevision"] == candidate["candidateRevision"]
    assert not git("diff", "ae41683281b63286585ddb287b48f8a7b9ed586d", "46b2e98e5a534221478206a14698312b6166d22e", "--", "cli")
    assert full["native"]["tests"] == 7507 and full["application"]["tests"] == 457
    assert read(REF / "approved-application.json")["tests"] == 457
    assert text_hash(REF / "engine-candidate.init.gradle") == full["initFileSha256Utf8Lf"]
    manifests = {stage: read(base / stage / "build-configuration.json") for stage in ["before", "after"]}
    assert manifests["before"]["actualRuntimeEngineRevision"] == approved
    assert manifests["after"]["actualRuntimeEngineRevision"] == candidate["candidateRevision"]
    assert manifests["before"]["jarSha256"]["rules-engine.jar"] != manifests["after"]["jarSha256"]["rules-engine.jar"]
    for manifest in manifests.values():
        for name, expected in manifest["jarSha256"].items():
            path = base / manifest["distribution"] / "lib" / name
            assert hashlib.sha256(path.read_bytes()).hexdigest() == expected, path
    comparisons = []
    for slug in ["judith", "krenko", "oviya"]:
        for suffix in ["a", "b", "c"]:
            name = f"{slug}-{suffix}.json"
            old, new = [read(base / stage / name) for stage in ["before", "after"]]
            assert old == new, name
            assert new["argentumCommit"] == approved
            assurance = new["translationAssurance"]
            assert all(row["evidenceCurrent"] for row in assurance)
            assert collections.Counter(row["qualification"] for row in assurance) == {"PROVISIONAL": 21, "BLOCKED": 1}
            assert next(row for row in assurance if row["card"] == "Tatterkite")["qualification"] == "BLOCKED"
            comparisons.append({"report": name, "exactJsonEqual": True, "sha256Utf8Lf": text_hash(base / "after" / name), "counts": new["counts"]})
        name = f"coverage-{slug}.json"
        old, new = [read(base / stage / name) for stage in ["before", "after"]]
        excluded = ["timingsMillis", "correctionValidationMillis"]
        assert {k:v for k,v in old.items() if k not in excluded} == {k:v for k,v in new.items() if k not in excluded}, name
        comparisons.append({"report": name, "equalExcept": excluded, "beforeSha256Utf8Lf": text_hash(base / "before" / name), "afterSha256Utf8Lf": text_hash(base / "after" / name), "counts": new["counts"]})
    mutations = {stage: read(base / f"{stage}-mutations.json") for stage in ["approved", "candidate"]}
    assert len(mutations["candidate"]) == 9
    assert all(row["controlPassed"] and row["executionDetected"] and not row["executionConfounded"] for row in mutations["candidate"])
    assert not next(row for row in mutations["approved"] if row["id"] == "M7")["controlPassed"]
    receipt = {"hashFormat": "SHA256_UTF8_LF", "actualConfigurations": manifests, "nativeFocusedBeforeAfter": {"tests": 17, "failuresBefore": 10, "failuresAfter": 0, "fixtureSha256": before["fixtureSha256"]}, "comparisons": comparisons, "mutationOutcomes": mutations, "pinAndAssuranceUnchanged": True, "candidateAdopted": False}
    (REF / "controlled-comparison.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8", newline="\n")
    print("Validated 9 exact discovery comparisons, 3 coverage comparisons, 9 discriminating candidate mutations; pin/assurance unchanged.")

if __name__ == "__main__":
    main()
