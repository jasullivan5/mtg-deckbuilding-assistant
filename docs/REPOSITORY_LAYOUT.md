# Current repository and historical export

The active objective is the local Commander research application: the Explorer, its three existing Kotlin objectives, eligibility, source/assurance boundaries, CLI and regression safety. Later Explorer milestones require their own assignments.

| Location | Current purpose |
|---|---|
| cli/src/main | Kotlin discovery, API, native extensions and packaged evidence/eligibility metadata |
| cli/src/test | Current regression and scenario tests; historical class names still test retained behavior |
| cli/src/test/resources/benchmarks | Four byte-preserved frozen reviews and one complete-report baseline used by tests/catalog checks |
| explorer | React/TypeScript UI, locked dependencies and browser tests |
| reference | Exact source/assurance records and fixtures still consumed by current tests; see its README |
| scripts | Maintained setup, launch, snapshot download and verification entry points |
| docs | Current operating guidance, architecture, boundaries and product milestones |
| generated | Ignored outputs from new runs; not authoritative source or required setup |
| licenses, THIRD_PARTY_NOTICES.md | Retained legal notices |

## Movable historical export

`archive-export-2026-10-30/` is a user-owned, ignored folder. Move the entire folder anywhere outside the checkout. The application, tests and CI must continue to work without it.

It contains completed phase reports, old handoffs, unused benchmark/source material, compact samples, historical verification receipts, one-off research/packaging scripts and previously generated logs/reports. Original versions of rewritten documentation are retained there too. `MANIFEST.json` records the source commit, original path, archived path, SHA256 and byte count for every exported file. Its README explains historical reproduction. All content was preserved rather than discarded.

The source revision before cleanup is **f3560407ff2cf12a795180f5c32fe1225c4ffce1**. This is an archive origin, not the application's engine selection. Git history is unchanged; removing files from the current branch does not shrink old Git history or alter the past. A fresh checkout after merge will not contain the ignored user-owned export; obtain it from the user's backup or recover tracked history from the recorded commit.

## Why some old-looking data stays

Frozen regression reviews still check the three current objectives. They moved into test resources without changing any expected result. The catalog comparison still checks complete reports and preserves every historical evidence binding.

`reference/` retains 55 files that tests or the persisted translation-assurance ledger still read. Their original paths and bytes remain intact so source hashes, concerns and provisional/blocked status do not silently change. Dates and phase names identify the evidence origin; they do not mean the inputs are unused or newly verified. This is the only active source-evidence directory. Bulk experiments and unused records have moved out.

Do not add a new global handoff, parallel instruction set or phase-specific script for every session. Keep current guidance here, current behavior in tests, and run outputs under generated/. Archive completed one-off work with an inventory when it is no longer consumed.
