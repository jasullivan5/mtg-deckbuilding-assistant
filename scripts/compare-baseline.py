#!/usr/bin/env python3
"""Check current catalog benchmarks and report immutable historical fingerprints."""
import argparse
import hashlib
import json
from pathlib import Path


def canonical_digest(value):
    data = json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False) + "\n"
    return hashlib.sha256(data.encode("utf-8")).hexdigest()


def normalize_report(report, runtime_revision, baseline_revision, legacy_paths):
    def provenance(value):
        if isinstance(value, dict):
            return {key: provenance(item) for key, item in value.items()}
        if isinstance(value, list):
            return [provenance(item) for item in value]
        if isinstance(value, str):
            value = legacy_paths.get(value, value)
            for kind in ("blob", "tree"):
                value = value.replace(
                    f"https://github.com/jasullivan5/argentum-engine/{kind}/{runtime_revision}",
                    f"https://github.com/jasullivan5/argentum-engine/{kind}/{baseline_revision}",
                )
        return value

    normalized = provenance(report)
    normalized["argentumCommit"] = baseline_revision
    return normalized


def compare_reports(directory, baseline, legacy_paths, *, mode="historical"):
    runtime = None
    checks = {}
    for name, expected in baseline["reports"].items():
        report = json.loads((directory / f"{name}.json").read_text(encoding="utf-8"))
        runtime = runtime or report["argentumCommit"]
        normalized = normalize_report(report, runtime, baseline["argentumCommit"], legacy_paths)
        benchmark = json.loads((directory / f"{name}-benchmark.json").read_text(encoding="utf-8"))
        checks[name] = {
            "sameActualEngineAcrossRuns": report["argentumCommit"] == runtime,
            "sameCompleteReportExceptDeclaredProvenance": canonical_digest(normalized) == expected["sha256CanonicalJsonUtf8"],
            "zeroConfirmedBenchmarkMismatches": benchmark["numerators"]["confirmed_mismatch_cases"] == 0,
        }
    current_passed = all(check["sameActualEngineAcrossRuns"] and check["zeroConfirmedBenchmarkMismatches"]
                         for check in checks.values())
    historical_match = all(check["sameCompleteReportExceptDeclaredProvenance"] for check in checks.values())
    identical = json.loads((directory / "diff-identical.json").read_text(encoding="utf-8"))
    checks["identicalDiffEmpty"] = not identical["changes"]
    current_passed = current_passed and checks["identicalDiffEmpty"]
    return {
        "mode": mode,
        "passed": current_passed and (mode == "current" or historical_match),
        "currentChecksPassed": current_passed,
        "historicalFingerprintsMatch": historical_match,
        "actualEngineCommit": runtime,
        "historicalBaselineEngineCommit": baseline["argentumCommit"],
        "normalization": "Only runtime/native URL revision and declared exact path relocations; historical evidence contents and mechanical fields untouched",
        "checks": checks,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", type=Path)
    parser.add_argument("baseline", type=Path)
    parser.add_argument("--mode", choices=("current", "historical"), default="historical")
    args = parser.parse_args()
    locations = Path(__file__).resolve().parent.parent / "src/test/resources/regression/path-relocations.json"
    aliases = json.loads(locations.read_text(encoding="utf-8"))
    legacy_paths = {row["to"]: row["from"] for row in aliases["paths"]}
    baseline = json.loads(args.baseline.read_text(encoding="utf-8"))
    result = compare_reports(args.directory, baseline, legacy_paths, mode=args.mode)
    (args.directory / "comparison-summary.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(f"Catalog comparison ({args.mode}):", result["passed"])
    print("Historical fingerprints match:", result["historicalFingerprintsMatch"])
    return 0 if result["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
