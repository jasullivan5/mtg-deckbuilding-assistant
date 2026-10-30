# Project layout and maintenance

The application has one Kotlin module at the repository root and one web frontend. There are no phase-specific source trees or parallel reference/benchmark/sample repositories.

```text
build.gradle.kts           Kotlin application build
src/main/kotlin/           Main.kt, Catalog.kt and behavior folders
  discovery/               Extraction, facts, objective policies and reports
  extensions/              Native card definitions and translation assurance
  eligibility/             Reviewed Commander metadata
  coverage/                External identities and coverage
  audit/                   Research diagnostics
  server/                  Local Explorer HTTP boundary
src/main/resources/        Packaged assurance, eligibility and engine bindings
src/test/kotlin/           Tests grouped by behavior; support/ holds shared helpers
src/test/resources/        Named card evidence, identity fixtures, assurance and regression data
web/                       React/TypeScript source, locked dependencies and browser tests
scripts/                   Maintained launch, setup, snapshot and verification entry points
docs/                      Current operating guidance, architecture and product direction
generated/                 Ignored outputs from current runs
build/                     Ignored Kotlin build/distribution output
```

`src/main/kotlin` and `src/test/kotlin` are standard Gradle source sets. Kotlin does not require the physical directory chain to mirror package names, so the unnecessary dev/mtg/assistant nesting was removed. Package identities remain stable. The separate cli module was removed: the root application supplies both CLI and local HTTP entry points. dev/dev.ps1 attaches it as :assistant to the maintained sibling engine's Gradle build. Use those wrappers rather than invoking an unrelated engine root test task.

## Tests and evidence

Test names describe behavior: death pathways, reanimation, token creation, reporting, identity, eligibility, extension scenarios and assurance. There are no Phase*or Batch* test files. Duplicate source/registry checks now cover the current registered extensions together, rather than preserving cohorts from past assignments. Existing unique scenario and proposal assertions remain. Experiment Twelve is an actual Magic card name and is intentionally retained as a card/scenario name.

All retained test material lives under [src/test/resources](../src/test/resources/README.md). Oracle snapshots/rulings use card names; identity, assurance and regression fixtures use their purpose. The fixture integrity test binds 82 pre-existing data files by LF-normalized SHA256. Those files were verified unchanged against their pre-move hashes.

The packaged assurance ledger points to the new retained locations. Source hashes, definition bindings, qualification and concern states are preserved; no independent approvals were transferred or invented. Immutable review IDs, original revisions and old paths inside dated evidence remain historical identifiers, not current directory instructions.

Frozen expected reviews and complete-report hashes were not regenerated. The catalog gate normalizes only runtime engine URL provenance and the exact path strings declared in regression/path-relocations.json. It cannot normalize mechanical facts, verdicts, quantities, prerequisites, source content or concern state. Native and application compatibility remain required before engine acceptance.

## Historical export

`archive-export-2026-10-30/` is a user-owned ignored folder. Move it anywhere outside this checkout as a unit; builds, tests and CI must work without it. It contains completed experiments, old handoffs, unused sources/reviews, samples, receipts, one-off scripts and previously generated outputs. MANIFEST.json records original paths, sizes and SHA256; its README explains reproduction against original source revision **f3560407ff2cf12a795180f5c32fe1225c4ffce1**. Cleanup validation and relocation records are stored separately inside the export.

Git history is unchanged. Removing old files from the current branch does not shrink historical Git objects. A fresh checkout after merge will not contain this local ignored export; retain a backup or recover tracked history from the original revision. The current code and docs are authoritative; archived instructions are not new assignments.

## Rules for future work

- Add source under an existing behavior folder, or create a folder for a lasting application responsibility. Never create a source/test directory per assignment or milestone.
- Name tests and fixtures after the behavior, card or evidence role they validate. New card source fixtures are automatically checked by the shared extension source contract.
- Keep fixtures with tests and runtime metadata with runtime resources. New retained data needs a current consumer and explicit provenance; do not keep unused samples just because a past task produced them.
- Keep one maintained set of scripts and current operating guides. Put run output under generated/ and export completed one-off work when it ceases to be consumed.
- Preserve source-first expectations and assurance bindings. Path cleanup does not authorize changed mechanical expectations or qualification.
