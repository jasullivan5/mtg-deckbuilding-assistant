# MTG Deckbuilding Assistant

A Commander deckbuilding research CLI that finds structural mechanical possibilities in native Argentum card definitions. It records eligibility, prerequisites, unsupported structures and evidence rather than recommending decks or claiming that a matching action will resolve successfully.

The implementation uses Kotlin, JDK 21 and Gradle, with Argentum's native `CardRegistry`, `CardDefinition`, `CardScript`, rules engine and `mtg-search`. The foundation is already selected; this project extends its existing extraction and research tools.

Explorer M1 adds a live React / TypeScript / Vite interface over existing Kotlin inspection and the three discovery objectives. Browse findings, inspect conditional pathways and original evidence, and change the research question without generating JSON manually. User acceptance is pending; imagery and later milestones remain deferred.

## Launch the Explorer on Windows

Install Git, JDK 21, Node 22.12+ and pnpm 11.25.0; configure JAVA_HOME and PATH. From this checkout:

```powershell
.\scripts\explorer.ps1
```

Open **http://127.0.0.1:8787** after **Explorer ready** appears. Initial installation requires network access; catalog loading takes tens of seconds. Stop with **Ctrl+C**. The command builds both layers using the normal sibling app-stable engine. See [guided exercises, API and limitations](docs/EXPLORER_M1.md).
## Two repositories

- [This application](https://github.com/jasullivan5/mtg-deckbuilding-assistant) owns discovery, effective Commander eligibility, external identity observations, translation assurance and research diagnostics, including the research frontend and local API integration.
- [Our Argentum fork](https://github.com/jasullivan5/argentum-engine) owns engine/SDK changes and native mechanical regressions. Its default `app-stable` is the normal development base; `main` tracks the [original project](https://github.com/wingedsheep/argentum-engine) for comparison only. Internal engine PRs target `app-stable`; upstream PRs require explicit user authorization.

The application uses the sibling `../argentum-engine` checkout on the maintained fork's `app-stable` branch. Engine corrections require native and application compatibility checks before acceptance there. Each application build and research result records the actual engine SHA; no separate dependency-pin approval is required. See [engine workflow](docs/ENGINE_WORKFLOW.md). The earlier exact-pin transition is preserved as [historical evidence](docs/ENGINE_COUNTER_CORRECTION.md).

## Implemented discovery

For a supported single, fixed-identity commander, discovery checks Commander legality and the full color-identity subset before examining native mechanics. Optional conventional `mtg-search` narrows that eligible pool.

| Objective | Bounded question |
|---|---|
| `own-nontoken-creature-deaths` | Direct death generation and separately classified death-enhanced removal |
| `reanimate-own-creatures` | Direct original creature-card movement from our graveyard to our battlefield |
| `create-creature-tokens` | Creature-token creation under our control, with quantity/type/controller prerequisites |

`commander inspect` distinguishes missing definitions, selection blockers and bounded mechanical evidence. Diagnostics cover native structures, frozen benchmarks, report differences, snapshot-based Oracle coverage and missing-card inventories. A structural match, a confident identity association, a passed engine scenario and an independently qualified translation are distinct claims. Provisional research definitions remain accessible; unknowns remain visible. This is not whole-deck validation, exhaustive MTG coverage or strategic ranking.

## Setup and verification

Install Git, JDK 21, Bash and Python 3; set `JAVA_HOME` and its `bin` on `PATH`. Gradle dependencies require network access. Setup creates the sibling `argentum-engine/` on app-stable if absent, or validates and reuses it. No server, database or API key is required.

```bash
bash scripts/bootstrap.sh
./dev test
./dev installDist
bash scripts/verify.sh fast
# Full application tests and catalog gate:
bash scripts/verify.sh full
```

PowerShell entry points:

```powershell
.\scripts\bootstrap.ps1
.\dev.ps1 test
.\dev.ps1 installDist
```

Use `dev`/`dev.ps1`, not root `gradlew test`: the sibling engine owns the Gradle settings. Setup refuses wrong branches, dirty source and unrelated origins. Existing work is never switched or overwritten. To obtain newly merged stable changes explicitly, run `.\scripts\setup.ps1 -Update` or `bash scripts/setup.sh --update`; updates are fast-forward only. See [environment prerequisites](docs/CODEX_ENVIRONMENT.md) and [engine workflow](docs/ENGINE_WORKFLOW.md).

```bash
cli/build/install/mtg-assistant/bin/mtg-assistant commander inspect 'Judith, the Scourge Diva' --json
cli/build/install/mtg-assistant/bin/mtg-assistant discover --commander 'Judith, the Scourge Diva' --objective create-creature-tokens --json --output generated/judith-tokens.json
```

On Windows, use the adjacent `mtg-assistant.bat` launcher. Full generated reports, builds, dependencies and machine-local environment files remain ignored.

For example, from the application root in PowerShell:

```powershell
.\cli\build\install\mtg-assistant\bin\mtg-assistant.bat commander inspect 'Judith, the Scourge Diva' --json
.\cli\build\install\mtg-assistant\bin\mtg-assistant.bat discover --commander 'Judith, the Scourge Diva' --objective create-creature-tokens --json --output generated/judith-tokens.json
```

Run these commands from the application checkout. Keep application and engine as independent siblings; the engine normally stays on app-stable. The old ignored nested `argentum/` is retained but unused. Paths may contain spaces and `&`: quote paths and use `Set-Location -LiteralPath` in PowerShell. No temporary drive alias is required.

## Everyday workflow and documentation ownership

Use a feature/fix branch from application `main`, or from engine `app-stable` for engine work, in the existing primary checkout. A new branch does not require a worktree. Reserve temporary worktrees for simultaneous revisions, isolated candidate integration, separate build outputs or protecting active work. Reuse a suitable clean checkout where possible. After preserving evidence and checking clean/untracked/unpushed work, active processes and consumers, remove ended temporary worktrees with `git worktree remove` without force; retain their branches/history. Keep ambiguous clones and necessary validation checkouts for review.

ChatGPT Project references own long-term vision and research principles. This repository owns current assistant behavior and integration; the fork owns engine/SDK implementation and native tests. Link authoritative detail instead of copying it.

- [Agent instructions](AGENTS.md), [handoff navigation](CODEX_HANDOFF.md), [actual architecture](docs/ARCHITECTURE.md).
- [Authoring](docs/COMMANDER_AUTHORING.md), [translation reliability](docs/TRANSLATION_RELIABILITY.md), [eligibility](docs/ELIGIBILITY_CORRECTIONS.md), [identity observations](docs/ORACLE_IDENTITY.md).
- [Coverage](docs/CARD_COVERAGE.md), [limitations](KNOWN_LIMITATIONS.md), [third-party notices](THIRD_PARTY_NOTICES.md).
- [Historical entry-point text](docs/history/PRE_CONSOLIDATION_HANDOFF.md) and [Phase 2.5 implementation](docs/history/PHASE2_5_IMPLEMENTATION.md). Historical counts, ZIP/publication instructions and suggested tasks are not current assignments.
