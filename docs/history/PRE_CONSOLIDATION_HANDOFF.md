# Application entry points before documentation consolidation

Archived from application commit `088f5c4e8c352816fad0e26660f6cbec23d89ef7` on 2026-10-29. These are historical documents, not current setup, test status, publication instructions or assignments. Relative paths inside the quoted text were relative to their original files. Current navigation starts at the repository README.


## Original README.md


````markdown
# Codex development entry point

Start with CODEX_HANDOFF.md and AGENTS.md. Current verification status is in HANDOFF_VERIFICATION.md; historical build reports alone are not current evidence. Initial setup: `./scripts/bootstrap.sh`; routine checks: `./scripts/verify.sh fast`; complete tests/catalog checks: `./scripts/verify.sh full`. Use Git branches/commits rather than replacing ZIP history. Architecture: docs/ARCHITECTURE.md; hosted setup: docs/CODEX_ENVIRONMENT.md.

The original Phase2.5 usage guide follows; historical reports/manifests moved to docs/history. Scripts require Git/JDK21/Bash/Python3; PowerShell setup is retained. Do not use root gradlew test; dev attaches the application module to pinned engine settings.

## Engine development foundation

Use [our Argentum fork](https://github.com/jasullivan5/argentum-engine), with `app-stable` as the development base/default. Fork `main` tracks the original project for comparison only. Feature/fix work and internal PRs normally target app-stable; upstream PRs are not our workflow. The published modern `fix/collection-counter-placement` branch is historical and remains retained.

The application always fetches the full commit in `ARGENTUM_COMMIT`, never a branch tip. Pin changes are separate reviewed application PRs. Setup retains wrong-revision and dirty-checkout refusal, migrates only the recognized old upstream origin, and rejects unrelated origins. For a different pin, use a fresh application worktree/checkout and bootstrap there; setup deliberately does not switch an existing wrong revision.

Review upstream manually approximately monthly or before an engine phase. Fetch/compare does not authorize integration. Select mechanisms rather than bulk card updates; regression-test candidates before adopting exact commits. **Do not use GitHub's fork-sync UI on default app-stable**: synchronization must explicitly target main. [Current workflow and transition evidence](docs/ENGINE_COUNTER_CORRECTION.md).

## Phase 2.7 card coverage

Commander-scoped Scryfall coverage, bounded missing-card inventories and the verified Krenko's Command/Bitterblossom native translations are documented in [CARD_COVERAGE.md](docs/CARD_COVERAGE.md). Executed counts, the 16-card Assay investigation and regression evidence are in [PHASE2_7_REPORT.md](docs/PHASE2_7_REPORT.md).

# MTG Deckbuilding Assistant — Phase 2.5

Kotlin CLI over real Argentum `CardRegistry`, `CardDefinition`, `CardScript`, and `mtg-search`. Continues Phase 2.4 with an exact reviewed engine pin in `ARGENTUM_COMMIT`.

## Setup and startup

Install **Git and JDK 21**, set `JAVA_HOME` and put its `bin` on `PATH`. Setup downloads the exact pinned source from our fork; Gradle downloads dependencies. Allow GitHub, Gradle distribution and Maven access. Expect the full upstream corpus to need several GB of disk and several minutes on the first build. No dependencies or upstream checkout are bundled.

Linux/macOS, from the extracted project root:

```bash
bash scripts/setup.sh
chmod +x gradlew dev
./dev build
./dev installDist
cli/build/install/mtg-assistant/bin/mtg-assistant card "Judith, the Scourge Diva"
cli/build/install/mtg-assistant/bin/mtg-assistant search 't:instant c:r mv=1 name:bolt'
cli/build/install/mtg-assistant/bin/mtg-assistant discover --commander "Judith, the Scourge Diva" --objective create-creature-tokens --json --output generated/judith-tokens.json
```

Run only tests with `./dev test`. The Gradle-run equivalent is:

```bash
./dev run --args="discover --commander 'Judith, the Scourge Diva' --objective create-creature-tokens"
```

Windows PowerShell, after installing Git/JDK21 and setting `JAVA_HOME`:

```powershell
.\scripts\setup.ps1
.\dev.ps1 build
.\dev.ps1 installDist
.\cli\build\install\mtg-assistant\bin\mtg-assistant.bat discover --commander "Krenko, Mob Boss" --objective create-creature-tokens --json --output generated/krenko-tokens.json
```

Historical Phase 2.5 verification ran on Linux. The fork-foundation verification also executed Windows PowerShell and Git Bash setup and safeguards, plus the complete application suite on Windows; macOS remains unexecuted. Use `dev`/`dev.ps1`: the wrapper is attached to the pinned engine build through `argentum.init.gradle`, without edits to the pinned engine checkout. On a constrained machine append `-Pkotlin.daemon.jvmargs='-Xmx2g -XX:ActiveProcessorCount=4'` to the build/test command.

## Commander integration

Use `mtg-assistant commander inspect "<exact card name>" [--json]` before discovery. Inspection separates missing pinned definitions, metadata gaps, ineligible cards and unsupported construction from recorded native mechanical test coverage. Judith and Oviya Pashiri, Sage Lifecrafter are application-owned native extensions. See [the commander authoring guide](docs/COMMANDER_AUTHORING.md) for source verification, registration safeguards and engine-test requirements.

## Three objectives

Use the same `discover --commander <exact-name> --objective <objective>` command:

| Objective | Contribution |
|---|---|
| `own-nontoken-creature-deaths` | Direct death generators; separate death-enhanced removal |
| `reanimate-own-creatures` | Direct original creature-card movement from our graveyard to our battlefield |
| `create-creature-tokens` | Creature-token creation under our control |

All apply Commander legality, then full color-identity subset eligibility. `--search '<native mtg-search query>'` narrows that eligible pool. `--json` exports all decisions, native facts/actions, uncertainty and provenance. Human output lists accepted results; `--diagnostics` includes rejected/unknown paths. Judith remains an application extension and death-event consumer.

A match is **conditional structural potential**, not guaranteed resolution, available targets/resources, complete card support, or strategic desirability. Fixed and variable token counts, source copying, branch conditions and grouped templates remain distinguishable. Noncreature tokens, opponent receivers and zero counts do not satisfy Objective C. Complex copy restrictions and unknown templates stay unknown.

## Diagnostics and reproduction

Using the installed `mtg-assistant` launcher:

```bash
mtg-assistant audit coverage --snapshot generated/judith-tokens.json --json --output generated/coverage.json
mtg-assistant audit inconsistencies --snapshot generated/judith-tokens.json --benchmark benchmark/phase25/review-fork-v1.json --json --output generated/examination.json
mtg-assistant audit benchmark --snapshot generated/judith-tokens.json --benchmark benchmark/phase25/review-fork-v1.json --json --output generated/benchmark.json
mtg-assistant audit diff --before generated/before.json --after generated/after.json --json --output generated/diff.json
mtg-assistant audit cross-objective --snapshot generated/objective-a.json --other-snapshot generated/judith-tokens.json --json --output generated/reuse.json
```

Prefix these example commands with `cli/build/install/mtg-assistant/bin/` if it is not on PATH. The all-colors reviewed-subset benchmark also executes in `Phase25ReportingTest`, so white/blue examples are evaluated without treating Judith/Krenko ineligibility as extractor failure.

The historical Phase 2.5 regeneration commands below require their historical application revision/pin. Current fork verification uses scripts/check-catalog.sh and the fork-bound review-fork-v1.json inputs; the original reviews remain intact.

Regenerate the historical full final workflow and compact evidence. To regenerate the all-colors benchmark's JSON side output rather than reuse packaged evidence, run `./dev clean` followed by `./dev test --no-build-cache` first. This cleans only the application module; the pinned upstream outputs remain reusable. A cached test result alone does not recreate this optional side output.

```bash
MTG_REPORT_DIR=generated/phase25/final-verified bash verification/phase2_5/run-reports.sh final
python3 verification/phase2_5/compact-reports.py
```

The full workflow's A/B diffs require baseline snapshots. To reproduce those without replacing this project's source:

```bash
python3 scripts/create-phase24-baseline.py ../mtg-phase24-baseline
(cd ../mtg-phase24-baseline && ./dev build :assistant-cli:installDist)
MTG_REPORT_CLI="$(cd ../mtg-phase24-baseline && pwd)/cli/build/install/mtg-assistant/bin/mtg-assistant" MTG_REPORT_DIR=generated/phase25/baseline-pinned bash verification/phase2_5/run-reports.sh baseline
MTG_REPORT_DIR=generated/phase25/final-verified bash verification/phase2_5/run-reports.sh final
python3 verification/phase2_5/compact-reports.py
```

The source-restoration patch is verified against the original Phase2.4 application bytes. It requires Python3 and `patch` (Linux/macOS or Git Bash/WSL). Baseline setup reuses the clean pinned checkout when available. Full reports can be hundreds of MB; `generated/` is deliberately excluded from the delivery. `samples/phase2_5/OUTPUT_MANIFEST.json` records executed output hashes, sizes and regeneration commands. Compact samples preserve counts, identities, verdicts and representative native evidence, not complete snapshots usable directly by `audit diff`.

See `docs/history/PHASE2_5_IMPLEMENTATION.md`, `docs/history/PHASE2_5_BUILD_VERIFICATION.md`, `KNOWN_LIMITATIONS.md`, and the frozen benchmark/source snapshots. All previous automated test sources and their benchmark dependencies are retained.

When importing the ZIP on Windows, if Git reports executable-bit-only changes, set `git config core.fileMode false` locally before reviewing status. Native Windows execution is not claimed by the Linux verification record.
````


## Original CODEX_HANDOFF.md


````markdown
# Codex handoff

## Current fork foundation

Use [our Argentum fork](https://github.com/jasullivan5/argentum-engine), with `app-stable` as the development base/default. Fork `main` tracks the original project for comparison only. Feature/fix work and internal PRs normally target app-stable; upstream PRs are not our workflow. The published modern `fix/collection-counter-placement` branch is historical and remains retained.

The application always fetches the full commit in `ARGENTUM_COMMIT`, never a branch tip. Pin changes are separate reviewed application PRs. Setup retains wrong-revision and dirty-checkout refusal, migrates only the recognized old upstream origin, and rejects unrelated origins. For a different pin, use a fresh application worktree/checkout and bootstrap there; setup deliberately does not switch an existing wrong revision.

Review upstream manually approximately monthly or before an engine phase. Fetch/compare does not authorize integration. Select mechanisms rather than bulk card updates; regression-test candidates before adopting exact commits. **Do not use GitHub's fork-sync UI on default app-stable**: synchronization must explicitly target main. [Current workflow and transition evidence](docs/ENGINE_COUNTER_CORRECTION.md).

## Historical migration record

Current development adds structured `commander inspect` diagnostics, recursive native-extension safeguards and the application-owned Oviya Pashiri, Sage Lifecrafter definition. See docs/COMMANDER_AUTHORING.md for the repeatable workflow and bounded engine-test coverage. The accepted Phase2.5 migration notes below describe the baseline delivery; they are historical publication/environment records, not the current GitHub or local setup state.

Start with AGENTS.md, README.md and docs/ARCHITECTURE.md. This is accepted Phase2.5 prepared for Git development, not a new feature phase.

Current objectives: A own nontoken creature deaths (direct vs enhanced-removal classifications/policy preserved); B own graveyard creature cards directly to our battlefield; C creature-token creation under our control. Native Argentum integration, shared MechanicalFact/FactQuery, Judith extension/gameplay tests, diagnostics, frozen reviews and all 210 Kotlin tests are preserved byte-for-byte. A retains legacy-specific projection.

Pin: ebf5bf2e9fbb80d13a556d75491dd462e10c206f. Source archive SHA256: f5d7732d7cafbd7a183bd11068ddad0333183cebf605c7f9f98cb0cedc5e12da. Git tag phase2.5-baseline identifies the accepted import. Exact delivered HEAD/baseline SHAs and sizes are in the generated REPOSITORY_STATE.json receipt; git log is authoritative.

The first unpublished migration workspace reset before its Git objects/logs were saved. That lost local history is unavailable; this recovered repository has a new legitimate baseline import, not fabricated historical phase commits. See HANDOFF_VERIFICATION.md for verified versus incomplete checks. The prior session actually reported 210 baseline tests and 48 routine tests passing, but its current cold/full outcomes and logs could not be recovered.

No GitHub destination was confirmed. The available connection listed private research repositories but did not offer repository creation; unrelated repositories were untouched. This fallback includes complete local Git history. To publish with your own authorized GitHub CLI: `gh repo create mtg-deckbuilding-assistant --private --source=. --remote=origin --push`, then `git push origin --tags`. Inspect an existing destination first; never force-push unrelated work. No remote CI/environment success is claimed.

Work on focused branches. Run appropriate test classes, then all tests/full catalog comparisons for shared extraction, eligibility or policy changes. Preserve full before/after JSON in ignored generated/ for native audit diff. Compact baselines retain decision rows and complete-evidence hashes; changed hashes require review and are not automatically policy changes. Generated catalog reports are excluded from Git.

Known unsupported behavior: complex token-copy/controller/quantity variants, Amass/populate, multi-step reanimation and incomplete bindings/conditions. Unknown eligibility metadata is a separate data gap. Shared action IDs do not fully solve cross-objective causality. Frozen source-led benchmarks are not external adjudication or full-MTG recall. No new mechanical interpretations are added here.

Recommended first Codex task (not implemented): make the existing all-colors benchmark JSON an explicit report task with declared inputs/outputs, so cached/filtered tests cannot suppress it. Acceptance: regenerate a deleted report without recompiling unchanged upstream modules; preserve 11 accepted/four excluded/two unknown-boundary/one separately unresolved Amass outcomes; all 210 tests pass; no effect walker/objective change. Currently full verification reruns the selected Test task to produce the side output.

Recovery completed: fresh cold build 210/0, routine gate 48/0, four setup-guard tests, and all six catalog/complete-evidence comparisons passed. See HANDOFF_VERIFICATION.md and verification/handoff. Remote publication and hosted CI remain unperformed.
````


## Original HANDOFF_VERIFICATION.md


````markdown
# Current fork-foundation verification

The independent-fork transition is documented in docs/ENGINE_COUNTER_CORRECTION.md and reference/fork-foundation/. Fresh execution passed 7,507 native tests (including 17 focused fixtures), 13 representative native scenarios, and 457 application tests. A separate normal build fetched the published stable fork commit and passed all 457 application tests and nine mutation controls/detections. All 16 Windows PowerShell/Git Bash setup and wrapper safeguards passed. Nine full discovery, three coverage and three missing-card report comparisons matched after declared revision/provenance/assurance bindings and coverage timings; no mechanical or eligibility changes. The separate current catalog gate passed six complete-report comparisons and six benchmarks with zero confirmed mismatches, coverage/cross-examination and empty self-diff; four local-Git setup fixtures also pass. Initial benchmark-pin refusals and a CRLF-only golden failure are preserved with their corrections.

This branch proposes engine 4133241856227d1d36269e92aaba633bbaf93f5c; application main remains on approved ebf5bf2e9fbb80d13a556d75491dd462e10c206f until an explicit application PR merge. Historical recovery counts and platform limitations below are not current transition results. No hosted CI or independent qualification is claimed.

## Historical Phase 2.5 recovery record

# Handoff verification — verified local delivery

Status: recovery cold build and all 210 Kotlin tests PASSED. All six catalog comparisons and routine verification also passed; remote CI remains unexecuted.

- Source artifact: MTG_Deckbuilding_Assistant_Phase2_5(1).zip; SHA256 f5d7732d7cafbd7a183bd11068ddad0333183cebf605c7f9f98cb0cedc5e12da, verified on import.
- Pin unchanged: ebf5bf2e9fbb80d13a556d75491dd462e10c206f.
- Required toolchain: JDK21 and Gradle9.6.1 wrapper (checksum retained); Git/Bash/Python3.
- Earlier attempt: baseline 210/0 and routine 48/0 plus 10 workflow/comparison tests passed, as recorded in the conversation. Logs/Git objects disappeared with the workspace reset; they are not shipped or claimed as fresh recovery execution evidence.
- Earlier cold build: first attempt hit Plugin Portal Foojay HTTP403; retry reached native compilation before the reset. Final result is unavailable. Full catalog result likewise unavailable.
- This recovery keeps Kotlin application/test and benchmark bytes unchanged. Setup changes are limited to refusing existing wrong-revision checkouts and correctly checking unborn HEAD with --verify. A PowerShell counterpart is provided by inspection.
- Current execution logs are added under verification/handoff. Historical Phase2.5 evidence is separate under docs/history and verification/phase2_5.
- GitHub publication and hosted CI: NOT PERFORMED; no suitable app destination/creation capability. Codex environment: NOT CREATED.
- Recovery cold build completed successfully in 10m21s, 53/53 actionable tasks executed with --no-build-cache. The checkout was freshly retrieved from public GitHub (no symlink/warm native outputs), and the user Gradle cache was initially absent. All 210 tests passed with zero failures/errors/skips. See recovery-results.json and recovery-cold-build.log.

Commands: ./scripts/bootstrap.sh; ./dev build; ./scripts/verify.sh fast; ./scripts/verify.sh full. Catalog gate: ./scripts/verify.sh catalog. It regenerates six queries/reviews, coverage, cross-examination and native identical-snapshot diff, then compares strict pathway/evidence multisets with the compact accepted baseline. Mismatches fail; no expectations are rewritten.

The final export receipt supplies exact Git commits and repository size. The package must remain <=20,971,520 bytes and excludes upstream/cache/builds/generated dumps. Platform and successful/unsuccessful commands will be recorded explicitly as execution completes.

Recovery checks: four actual local-Git setup guard tests passed. Fresh export Git fsck passed; wrapper normalization was corrected so export status is clean. All 38 application/test Kotlin files match the imported baseline bytes (source-preservation.json). Cold command: JAVA_HOME=<installed JDK21> PATH="$JAVA_HOME/bin:$PATH" ./dev build :assistant-cli:installDist --no-build-cache. This host also supplied JVM proxy/truststore configuration externally; no credentials or environment-specific helper is committed.

## Final recovery execution

- `./scripts/bootstrap.sh --no-build`: fetched a new shallow exact-pin upstream checkout from public GitHub; source-setup.log. No existing Work checkout/symlink or native build outputs were used.
- `./dev build :assistant-cli:installDist --no-build-cache`: SUCCESS 10m21s, 53/53 actionable tasks executed, 210 tests / zero failures/errors/skips. This was the cold application/native dependency build, with an initially absent Gradle user cache restored from public dependencies. No mock or unpublished local library was substituted.
- `./scripts/verify.sh fast`: SUCCESS, 48 Kotlin tests / zero failures/errors/skips plus actual Judith CLI lookup; fast-results.json and recovery-fast.log.
- `JAVA_HOME=<JDK21> PATH=... python3 scripts/test-bootstrap.py`: four local-Git guard tests passed. Tests preserve dirty/wrong-revision work and reject invalid directories; they are not rules-engine fixtures.
- `./scripts/check-catalog.sh`: SUCCESS; all A/B/C Judith/Krenko contexts regenerated, six matching frozen benchmarks reported zero confirmed mismatches, independent coverage and cross-examination executed, identical-snapshot native diff had zero changes. Strict complete-evidence and pathway/decision multiset comparisons all matched the accepted compact baseline. See catalog-summary.json/catalog.log. Full generated JSON is intentionally omitted from Git/ZIP.
- The complete verification components were executed as cold build/all-tests plus catalog script. The aggregate `verify.sh full` wrapper was syntax-checked; it was not rerun merely to duplicate the already successful components. Hosted Actions was not executed.

Platform: Linux x86_64, Temurin JDK21.0.13+9, Gradle9.6.1/Kotlin2.4.0 retained. Exact wrapper/runtime report: toolchain.log. Native upstream remains clean at the required full SHA. Windows/PowerShell/macOS are inspection-only. A trusted environment public CA/JVM proxy was configured externally for HTTPS; no TLS bypass or credentials are committed.

The initial pre-reset handoff draft/commits/logs were lost; the accepted Phase2.5 source and the completed recovery evidence are now preserved durably in this export. No previously unavailable cold/full result is claimed. There are no unresolved Linux build/test/catalog blockers in this recovered delivery. Unsupported mechanical behavior remains as documented in KNOWN_LIMITATIONS.md.

Git history is included in the fallback ZIP, beginning with the accepted source import tag. Final commit/repository byte counts are generated into REPOSITORY_STATE.json after commit (avoids a self-referential SHA). Source and complete reachable Git objects are exported by a fresh --no-local clone and validated by git fsck/clean status. ZIP CRC and <=20 MiB limits are asserted. Exact compressed size is reported with delivery. No remote repository, Codex environment or hosted CI was published.
````
