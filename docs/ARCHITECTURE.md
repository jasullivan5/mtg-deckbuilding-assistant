# Actual architecture and extension points

The application is an external Kotlin `cli/` Gradle module attached through `argentum.init.gradle` to the exact dependency at `argentum/`. `dev`/`dev.ps1` enforce that revision and a clean checkout. [ARGENTUM_COMMIT](../ARGENTUM_COMMIT) authorizes adoption; engine acceptance in [our fork](https://github.com/jasullivan5/argentum-engine) is separate. The original project is an upstream reference, not a floating application dependency.

Argentum owns native `CardDefinition`/`CardScript` data and DSLs, card discovery/registry, search, pure rules execution, replacement/projection/continuation/event machinery and native scenario infrastructure. The application does not copy the SDK or add its own rules engine, database, game server or Oracle mechanics parser. Engine implementation guidance belongs to the fork's [architecture](https://github.com/jasullivan5/argentum-engine/blob/app-stable/docs/architecture-principles.md) and [SDK reference](https://github.com/jasullivan5/argentum-engine/blob/app-stable/docs/card-sdk-language-reference.md); consult the application-pinned source when changing integrations.

## Application pipeline

Native definitions → effective Commander eligibility → shared native action/branch extraction → reusable facts/constraints → objective queries and policies → evidence/reporting and research diagnostics.

Files below are relative to `cli/src/main/kotlin/dev/mtg/assistant/`.

| Concern | Implementation |
|---|---|
| Native catalog and search | `Catalog.kt`, `DefinitionSearchCard.kt`; native registry plus `mtg-search` projection |
| Application-owned native definitions | `extensions/Extensions.kt` and per-card files; provenance, duplicate/admission checks and source/test review metadata |
| Commander selection and full color identity | `discovery/Eligibility.kt`, `CommanderInspection.kt`; consumers use `Catalog.eligibility` |
| Reviewed effective metadata | `eligibility/EffectiveEligibility.kt`, packaged `eligibility/phase29.json`; source/snapshot/pin/native-fingerprint checks |
| One native walker | `discovery/PathwayExtractor.kt`; actions, costs/effects, branches, bindings and unsupported structures |
| Reusable facts, predicates and quantities | `discovery/MechanicalFacts.kt`, `TokenFacts.kt`, `AmountBounds.kt` |
| Death objective A | Legacy death/removal projection and `MechanicalQuery.kt`, assembled in `Discovery.kt` |
| Reanimation B / token C | Fact compositions and common reporting in `discovery/Reanimation.kt`; no separate native walkers |
| Evidence and CLI | `discovery/Evidence.kt`, `Discovery.kt`, `Main.kt`; native source provenance binds the exact fork revision |
| Identity/coverage observations | `coverage/OracleSnapshot.kt`, `OracleEnrichment.kt`, `CatalogCoverage.kt`; offline source snapshots and bounded mappings |
| Translation assurance | `extensions/TranslationAssurance.kt`, packaged `translation-assurance.json`; exact definition/pin/evidence bundle and independent-approval checks |
| Diagnostics and frozen reviews | `audit/Diagnostics.kt`, `RootDiagnostics.kt`, `Benchmark.kt`, `CrossObjective.kt`; coverage, inconsistencies, comparisons and report differences |

## Eligibility and identity boundaries

Discovery checks Commander format legality, then full identity subset, then optional conventional search. Unknown metadata and unsupported commander construction remain explicit. A candidate query does not validate a whole deck, singleton rules, companions or every special commander form.

The physical identity layer compares archived Oracle observations to authored native identities, preserves existing IDs and quarantines competing claims. Association does not edit definitions or independently authorize eligibility changes. Reviewed effective metadata is a separate application-owned overlay; the original native definitions, scripts and values remain available. Inspection, discovery, conventional identity/format projections and coverage share `Catalog.eligibility`. See [identity](ORACLE_IDENTITY.md), [eligibility](ELIGIBILITY_CORRECTIONS.md) and [coverage](CARD_COVERAGE.md).

## Mechanical and assurance boundaries

Extend the existing walker and fact predicates before adding another interpretation path. Preserve correlated owner/controller, source/host, zone, branch, target, quantity, cost/effect and timing constraints. Objective policy acceptance is distinct from structural possibility. Objective A is not fully fact-driven; shared action IDs do not establish whole-action causality. Complex copying, Amass, linked intermediate-zone reanimation and unresolved bindings remain incomplete. See [limitations](../KNOWN_LIMITATIONS.md).

Application extensions are native Argentum definitions registered through `Extensions.all`. Validation/serialization tests establish structural properties, not Oracle fidelity. Native `ScenarioTestBase` cases execute actual engine decisions, states and events for bounded source-derived contracts. Generic executor regressions belong in the engine fork; application integration tests bind the exact runtime separately from recorded definition/eligibility metadata when using experimental candidate integration.

Assurance separates representation, source fidelity, execution and computed qualification. It binds archived source/requirements/test hashes to a definition and engine revision. An open material concern blocks qualification; passing author scenarios does not create independent human approval. Catalog, inspection, discovery and per-card coverage expose assurance without making provisional research definitions ineligible. Inherited engine definitions do not receive silent certification. See [reliability](TRANSLATION_RELIABILITY.md) and [authoring](COMMANDER_AUTHORING.md).

Research diagnostics inspect native structures and source-led frozen subsets. An audit traversal is not an independent Oracle adjudication; sample agreement is not whole-catalog recall. Oracle coverage retains the eligible source pool separately from native availability, mapping uncertainty and objective observations. Benchmark cases are source-derived and revision-strict; active `review-fork-v1.json` inputs coexist with preserved historical `review-v1.json` files. Exact-report baselines and compact receipts detect changes, not universal correctness. [Recorded verification](../HANDOFF_VERIFICATION.md) distinguishes local evidence from CI and current execution from historical results.
