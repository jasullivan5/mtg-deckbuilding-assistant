# Codex development entry point

Start with CODEX_HANDOFF.md and AGENTS.md. Current verification status is in HANDOFF_VERIFICATION.md; historical build reports alone are not current evidence. Initial setup: `./scripts/bootstrap.sh`; routine checks: `./scripts/verify.sh fast`; complete tests/catalog checks: `./scripts/verify.sh full`. Use Git branches/commits rather than replacing ZIP history. Architecture: docs/ARCHITECTURE.md; hosted setup: docs/CODEX_ENVIRONMENT.md.

The original Phase2.5 usage guide follows; historical reports/manifests moved to docs/history. Scripts require Git/JDK21/Bash/Python3; PowerShell setup is retained. Do not use root gradlew test; dev attaches the application module to pinned upstream settings.

# MTG Deckbuilding Assistant — Phase 2.5

Kotlin CLI over real Argentum `CardRegistry`, `CardDefinition`, `CardScript`, and `mtg-search`. Continues Phase 2.4 with the unchanged upstream pin in `ARGENTUM_COMMIT`.

## Setup and startup

Install **Git and JDK 21**, set `JAVA_HOME` and put its `bin` on `PATH`. Setup downloads the pinned upstream source; Gradle downloads dependencies. Allow GitHub, Gradle distribution and Maven access. Expect the full upstream corpus to need several GB of disk and several minutes on the first build. No dependencies or upstream checkout are bundled.

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

Linux was executed; macOS/Windows instructions are retained but not executed in this pass. Use `dev`/`dev.ps1`: the wrapper is attached to the pinned upstream build through `argentum.init.gradle`, without upstream edits. On a constrained machine append `-Pkotlin.daemon.jvmargs='-Xmx2g -XX:ActiveProcessorCount=4'` to the build/test command.

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
mtg-assistant audit inconsistencies --snapshot generated/judith-tokens.json --benchmark benchmark/phase25/review-v1.json --json --output generated/examination.json
mtg-assistant audit benchmark --snapshot generated/judith-tokens.json --benchmark benchmark/phase25/review-v1.json --json --output generated/benchmark.json
mtg-assistant audit diff --before generated/before.json --after generated/after.json --json --output generated/diff.json
mtg-assistant audit cross-objective --snapshot generated/objective-a.json --other-snapshot generated/judith-tokens.json --json --output generated/reuse.json
```

Prefix these example commands with `cli/build/install/mtg-assistant/bin/` if it is not on PATH. The all-colors reviewed-subset benchmark also executes in `Phase25ReportingTest`, so white/blue examples are evaluated without treating Judith/Krenko ineligibility as extractor failure.

Regenerate the full final workflow and compact evidence. To regenerate the all-colors benchmark's JSON side output rather than reuse packaged evidence, run `./dev clean` followed by `./dev test --no-build-cache` first. This cleans only the application module; the pinned upstream outputs remain reusable. A cached test result alone does not recreate this optional side output.

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

See `PHASE2_5_IMPLEMENTATION.md`, `BUILD_VERIFICATION.md`, `KNOWN_LIMITATIONS.md`, and the frozen benchmark/source snapshots. All previous automated test sources and their benchmark dependencies are retained.
