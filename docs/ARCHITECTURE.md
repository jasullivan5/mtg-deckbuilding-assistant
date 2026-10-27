# Actual architecture and extension points

Native Argentum definitions → shared action/branch extraction → mechanical facts/constraints → composable predicates → objective evidence → coverage/benchmark/regression diagnostics.

The application is a small external `cli/` Gradle module attached through argentum.init.gradle to the pinned checkout at `argentum/`. Upstream supplies conventions, SDK, authored sets, search, tooling, engine and test fixtures. No SDK copy, database or server is introduced.

Under `cli/src/main/kotlin/dev/mtg/assistant/`:

| Concern | Files |
|---|---|
| External Oracle metadata and catalog mappings | coverage/OracleSnapshot.kt, coverage/CatalogCoverage.kt (offline input, identity indexes, independent coverage) |
| Catalog/conventional search | Catalog.kt, DefinitionSearchCard.kt (native registry/search) |
| Native extensions | extensions/Extensions.kt, JudithTheScourgeDiva.kt, OviyaPashiriSageLifecrafter.kt, KrenkosCommand.kt, Bitterblossom.kt (provenance/duplicates) |
| Commander legality/full identity | discovery/Eligibility.kt, CommanderInspection.kt |
| Native effects, costs, branches/bindings | discovery/PathwayExtractor.kt (one walker) |
| Reusable facts/predicates/quantities | discovery/MechanicalFacts.kt, TokenFacts.kt, AmountBounds.kt |
| Objective A | Legacy projection in extractor plus MechanicalQuery.kt and Discovery.kt |
| Objectives B/C | Fact compositions; shared discovery/Reanimation.kt reporting; no separate walkers |
| Evidence/CLI | discovery/Evidence.kt, Discovery.kt, Main.kt |
| Diagnostics/reviews/diffs | audit/Diagnostics.kt, RootDiagnostics.kt, Benchmark.kt, CrossObjective.kt |

Add generic SDK handling to the existing walker after inspecting the pinned type and freezing positive/adversarial examples. Add fact predicates in MechanicalFacts/TokenFacts; A's existing predicates remain in MechanicalQuery. New objectives should compose facts and use Discovery's common eligibility pipeline, but require a user assignment.

Native extensions use Argentum CardDefinition objects and Extensions.all; verify metadata, duplicate detection/provenance and real engine scenarios. Reviewed cases live in benchmark/*/review-v1.json with source references and expected path/conditions; do not derive expectations from search output. Tests live in cli/src/test/kotlin/dev/mtg/assistant, using FunSpec and ScenarioTestBase; assert exact pathways/constraints.

A is not completely fact-driven. Local support is not whole-action/card support; shared action IDs do not imply every branch executes. Unknown conditions/copy/collection bindings, Amass and multi-step reanimation remain incomplete. See KNOWN_LIMITATIONS.md. Migration must not expand mechanical coverage.

Commander inspection has separate availability, selection-blocker and bounded mechanical-review fields. The same structured inspection drives selection; it does not add an effect walker or certify complete mechanical correctness. See [COMMANDER_AUTHORING.md](COMMANDER_AUTHORING.md).

External coverage retains the complete snapshot-eligible Oracle pool separately from native availability and optionally calls existing discovery objectives. Missing/uncertain mappings stay unevaluated; shared physical face names affect mapping uncertainty rather than removing eligible IDs. Snapshot updates and bounded research sampling are separate scripts. See CARD_COVERAGE.md and PHASE2_7_REPORT.md for data contracts, measured outcomes and native metadata limitations.
