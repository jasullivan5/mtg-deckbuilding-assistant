# MTG Deckbuilding Assistant

A local Commander research application: choose a supported commander and an objective, run Kotlin discovery, and browse identified mechanics, view verified card images, see relevant card text with brief objective notes and inspect structured evidence fields in Card Discovery. Exact sources, tests and review records remain one level deeper. It explains structural possibilities rather than recommending decks or certifying translations.

## Launch on Windows

Install Git, JDK 21, Node 22.12+ and pnpm 11.25.0. Set JAVA_HOME and PATH; the launcher also recognizes user tools under `~/Tools/mtg-dev-tools`.

```powershell
.\scripts\explorer.ps1
```

Open **http://127.0.0.1:8787** after **Explorer ready** appears. The script builds both layers; catalog startup can take tens of seconds. Stop with **Ctrl+C**. See [guided exercises and API](docs/EXPLORER.md).

**Discover** searches the four existing objectives. **Progress** opens without a discovery run and measures native representation, typed-node interpretation, narrower assurance, reusable historical gains and research obstacles. **Primer** explains the system and evidence limits. Secondary investigations are in the collapsed developer research index. See [Progress definitions, snapshot launch and three guided tasks](docs/RESEARCH_PROGRESS.md).

## Current state and authority

The running application and live Git are the living sources of product direction, implementation guidance, evidence and limitations. Start here for current capabilities; use the linked documents for detail. Former permanent ChatGPT Project attachments and old Project Instructions are retired authorities.

| Available today | Detail and evidence |
| --- | --- |
| Discover: four bounded queries, card imagery/fallbacks, readable findings and prerequisites | [Explorer](docs/EXPLORER.md) and the objective list below |
| Progress: representation, bounded commander selection, objective reach, scoped trust and historical gains | [Progress](docs/RESEARCH_PROGRESS.md), [global coverage](docs/GLOBAL_COMMANDER_COVERAGE.md), [verification coverage](docs/VERIFICATION_COVERAGE.md) |
| Complete printed-payment comparison and phased examination, including two recorded Explicit tap-payment failures | [Cost fidelity](docs/ACTIVATION_COST_FIDELITY.md), [latest examination](docs/PRODUCT_STUDIO_COST_EXAMINATION.md) |
| Curated fresh native scenarios in **Developer research index** | [Mechanical Verification Lab](docs/MECHANICAL_VERIFICATION_LAB.md); specified positions, not deck simulation |
| Primer: conceptual direction, current limits and future hypotheses | Open **Primer** (`/#primer`); [objective direction](docs/OBJECTIVE_MODEL.md) records precise decisions |

PRs #37–42 merged the Lab and later Progress/trust capabilities. M1/M2 have explicit bounded experience acceptance; later merges and demonstrations do not imply blanket acceptance or independent qualification. [Milestones](docs/EXPLORER_MILESTONES.md) retains the original plan and acceptance history. Current measurements live in Progress and their revision-bound receipts. Global external measurements require an explicit compatible snapshot; see the Progress guide.

The conceptual direction is **card identity/rules → native definitions → mechanical facts/capabilities → objective discovery and selective layering → experimental deck evaluation**. The bounded [building-block assessment](docs/OBJECTIVE_BUILDING_BLOCKS.md) informs the current hypothesis: focused event, state or modification anchors, described through game elements and overlapping directional effect/outcome relationships; role names remain optional descriptions. This is not an implemented objective schema. A separate card-to-card strategic synergy model is not a prerequisite in the current hypothesis; valid mechanical dependencies still matter. General relationship filters, composition, objective-aware piloting and deck evaluation are not implemented.

| Authority | Home |
| --- | --- |
| Product decisions, objective building blocks and evaluation hypotheses | [OBJECTIVE_MODEL](docs/OBJECTIVE_MODEL.md) |
| Actual responsibilities and engine integration | [ARCHITECTURE](docs/ARCHITECTURE.md), [ENGINE_WORKFLOW](docs/ENGINE_WORKFLOW.md) |
| Current boundaries and unresolved problems | [KNOWN_LIMITATIONS](docs/KNOWN_LIMITATIONS.md), linked focused investigations |
| How Codex works, verifies and delivers | [AGENTS](AGENTS.md), [environment](docs/CODEX_ENVIRONMENT.md), [layout](docs/REPOSITORY_LAYOUT.md) |
| Source/definition/revision-bound evidence | Retained resources, focused reports and original test receipts linked from Progress |

The user owns goals and acceptance; Product Studio examines direction and evidence; Codex implements explicitly approved assignments. Hands-on review is optional when useful. The [native Explicit tap-payment correction](docs/TAP_PAYMENT_CORRECTION.md) merged separately in engine PR #7 and application PR #44, preserving historical failures. The current bounded application increment adds shared triggered draw/token relationships to the existing death objective; it changes no engine mechanics.

## Two sibling repositories

```text
MTG Deckbuilding R&D/
  mtg-deckbuilding-assistant/   # this application
  argentum-engine/             # maintained fork, normally clean on app-stable
```

The application uses the sibling [maintained engine](https://github.com/jasullivan5/argentum-engine) on **app-stable**. New engine work requires native and application compatibility checks before merging there. Each build/report records the actual SHA as provenance; there is no dependency-pin adoption step. The retired nested checkout has been removed. See [engine workflow](docs/ENGINE_WORKFLOW.md).

## Current objectives

| Objective ID | Bounded question |
|---|---|
| own-nontoken-creature-deaths | Selected death generation, death-enhanced removal and qualifying triggered draw/token responses |
| reanimate-own-creatures | Direct movement of original creature cards from our graveyard to our battlefield |
| create-creature-tokens | Creature-token creation under our control with type, quantity and controller prerequisites |
| stock-own-graveyard | Conditional creature-card arrivals in our graveyard through supported sacrifice, mill and discard routes |

Commander legality and full color identity precede mechanical extraction. Unknown eligibility and unsupported structures remain explicit. A structural match, a passed scenario, independently qualified translation and strategic value are separate claims. See [limitations](docs/KNOWN_LIMITATIONS.md).

## Build, tests and CLI

Use the application wrappers; the sibling engine owns Gradle settings. Git, JDK 21, Bash and Python 3 support the headless tools. Existing work is never reset or silently switched.

```powershell
.\scripts\bootstrap.ps1
.\dev.ps1 test
.\dev.ps1 installDist
.\build\install\mtg-assistant\bin\mtg-assistant.bat commander inspect 'Judith, the Scourge Diva' --json
```

Bash: `bash scripts/bootstrap.sh`, `./dev test`, `bash scripts/verify.sh full`. See the [complete local verification procedure](docs/ENGINE_WORKFLOW.md#complete-local-verification) for engine, browser and comparison checks. Update the local stable engine deliberately with `.\scripts\setup.ps1 -Update`, then rebuild. Generated reports belong under ignored `generated/`.

## Current documentation

- [Verification coverage and fidelity challenge](docs/VERIFICATION_COVERAGE.md)

- [Repository layout and historical export](docs/REPOSITORY_LAYOUT.md)
- [Agent instructions](AGENTS.md), [architecture](docs/ARCHITECTURE.md), [environment](docs/CODEX_ENVIRONMENT.md)
- [Explorer launch/evaluation](docs/EXPLORER.md) and [milestones](docs/EXPLORER_MILESTONES.md)
- [Native card authoring](docs/COMMANDER_AUTHORING.md), [translation assurance](docs/TRANSLATION_RELIABILITY.md)
- [Eligibility](docs/ELIGIBILITY_CORRECTIONS.md), [identity](docs/ORACLE_IDENTITY.md), [coverage tools](docs/CARD_COVERAGE.md)
- [Third-party notices](THIRD_PARTY_NOTICES.md)

Completed experiments and obsolete handoffs have been exported. Their instructions and counts are historical, not new assignments. Current tests and CI do not require the export folder.
