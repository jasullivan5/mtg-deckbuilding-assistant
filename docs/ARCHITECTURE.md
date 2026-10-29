# Architecture and extension points

## Current implementation

The application is an external Kotlin `cli/` Gradle module attached through `argentum.init.gradle` to the sibling `../argentum-engine` on the maintained fork's `app-stable` branch. `dev`/`dev.ps1` enforce branch, clean source and fork origin. The branch selects the dependency; generated build metadata and reports record the actual engine SHA. Native/application compatibility is checked before engine acceptance. See [engine workflow](ENGINE_WORKFLOW.md).

Argentum owns native `CardDefinition`/`CardScript` data and DSLs, card discovery/registry, search, pure rules execution, replacement/projection/continuation/event machinery and native scenario infrastructure. The application currently has no frontend or HTTP API; it does not copy the SDK or add its own rules engine, database, game server or Oracle mechanics parser. Engine implementation guidance belongs to the fork's [architecture](https://github.com/jasullivan5/argentum-engine/blob/app-stable/docs/architecture-principles.md) and [SDK reference](https://github.com/jasullivan5/argentum-engine/blob/app-stable/docs/card-sdk-language-reference.md); consult the actual recorded engine revision when changing integrations.

## Planned research interface

The working direction selected following Technology Audit 001 is a focused independent React / TypeScript / Vite interface in the application repository. Its initial experience is a Commander Discovery Explorer over existing commander inspection and discovery objectives. This records the direction; it does not implement or start the Explorer. The local audit experiment is separate research evidence, not an integrated application feature or proof of live API operation.

```text
React research UI [planned, application-owned]
  -> local Kotlin integration [planned, application-owned]
  -> existing discovery / eligibility / assurance logic [implemented, application-owned]
  -> sibling app-stable Argentum SDK and rules engine [implemented, fork-owned]
```

| Boundary | Ownership and constraints |
|---|---|
| Research UI — planned | Application: supported commander and existing-objective selection, genuine discovery, verified imagery, browsable grids/lists, readable mechanical explanations, prerequisites/decisions/evidence/limitations and interactive research visualizations. Responsive, keyboard-accessible and pleasant to explore. |
| Local Kotlin integration — planned | Application: a narrow adapter exposing real application capabilities. Lightweight HTTP is favored; framework, schema, process lifecycle and error handling remain to be implemented and reviewed. Ktor is not an implemented or mandatory dependency. |
| Existing research pipeline | Application: Commander eligibility, objective interpretation, discovery integration, translation assurance, source/identity observations, diagnostics and exact engine adoption. Retain the CLI and headless tests as entry points and regression evidence. |
| Native mechanics | Maintained fork: card structures, SDK semantics, rules execution, generic mechanical corrections and native regressions. Develop in the independent sibling repository; maintain the normal sibling on app-stable and validate native/application compatibility before engine acceptance. |

The UI must present native extraction and application decisions, never introduce a second MTG rules interpreter. An imported genuine report may support an early bounded prototype, but must be labeled with its source/revisions and not presented as live discovery. Genuine discovery execution remains part of the intended Explorer capability.

Argentum's existing web client is a possible source of selected presentation components, imagery utilities and icons after coupling, provenance and licensing review. Do not import the complete client or default to its hosted gameplay architecture. Accounts, multiplayer, general deck editing, persistent databases, desktop packaging, cloud infrastructure, a comprehensive game client, new objectives and replacement rules execution remain outside this direction without demonstrated need and appropriate approval.

The first bounded integration must validate the adapter contract and parity with existing CLI behavior, lifecycle/readiness/shutdown, error and blocked/unknown states, verified image associations/fallbacks, and actual local launch and interaction. Catalog startup, report size and browsing responsiveness need measurement on that implementation; the audit experiment does not settle them. Keep expectations, historical execution, fresh execution and illustrative data distinguishable. [Agent guidance](../AGENTS.md#visualization-fidelity) owns the presentation safeguards and [prototype delivery expectations](../AGENTS.md#collaboration-and-delivery); do not infer user acceptance or translation qualification from an attractive screen or passing tests.

## Application pipeline

Native definitions → effective Commander eligibility → shared native action/branch extraction → reusable facts/constraints → objective queries and policies → evidence/reporting and research diagnostics.

Files below are relative to `cli/src/main/kotlin/dev/mtg/assistant/`.

| Concern | Implementation |
|---|---|
| Native catalog and search | `Catalog.kt`, `DefinitionSearchCard.kt`; native registry plus `mtg-search` projection |
| Application-owned native definitions | `extensions/Extensions.kt` and per-card files; provenance, duplicate/admission checks and source/test review metadata |
| Commander selection and full color identity | `discovery/Eligibility.kt`, `CommanderInspection.kt`; consumers use `Catalog.eligibility` |
| Reviewed effective metadata | `eligibility/EffectiveEligibility.kt`, packaged `eligibility/phase29.json`; source/snapshot/implementation/native-fingerprint checks |
| One native walker | `discovery/PathwayExtractor.kt`; actions, costs/effects, branches, bindings and unsupported structures |
| Reusable facts, predicates and quantities | `discovery/MechanicalFacts.kt`, `TokenFacts.kt`, `AmountBounds.kt` |
| Death objective A | Legacy death/removal projection and `MechanicalQuery.kt`, assembled in `Discovery.kt` |
| Reanimation B / token C | Fact compositions and common reporting in `discovery/Reanimation.kt`; no separate native walkers |
| Evidence and CLI | `discovery/Evidence.kt`, `Discovery.kt`, `Main.kt`; native source provenance binds the exact fork revision |
| Identity/coverage observations | `coverage/OracleSnapshot.kt`, `OracleEnrichment.kt`, `CatalogCoverage.kt`; offline source snapshots and bounded mappings |
| Translation assurance | `extensions/TranslationAssurance.kt`, packaged `translation-assurance.json`; exact definition/implementation/evidence bundle and independent-approval checks |
| Diagnostics and frozen reviews | `audit/Diagnostics.kt`, `RootDiagnostics.kt`, `Benchmark.kt`, `CrossObjective.kt`; coverage, inconsistencies, comparisons and report differences |

## Eligibility and identity boundaries

Discovery checks Commander format legality, then full identity subset, then optional conventional search. Unknown metadata and unsupported commander construction remain explicit. A candidate query does not validate a whole deck, singleton rules, companions or every special commander form.

The physical identity layer compares archived Oracle observations to authored native identities, preserves existing IDs and quarantines competing claims. Association does not edit definitions or independently authorize eligibility changes. Reviewed effective metadata is a separate application-owned overlay; the original native definitions, scripts and values remain available. Inspection, discovery, conventional identity/format projections and coverage share `Catalog.eligibility`. See [identity](ORACLE_IDENTITY.md), [eligibility](ELIGIBILITY_CORRECTIONS.md) and [coverage](CARD_COVERAGE.md).

## Mechanical and assurance boundaries

Extend the existing walker and fact predicates before adding another interpretation path. Preserve correlated owner/controller, source/host, zone, branch, target, quantity, cost/effect and timing constraints. Objective policy acceptance is distinct from structural possibility. Objective A is not fully fact-driven; shared action IDs do not establish whole-action causality. Complex copying, Amass, linked intermediate-zone reanimation and unresolved bindings remain incomplete. See [limitations](../KNOWN_LIMITATIONS.md).

Application extensions are native Argentum definitions registered through `Extensions.all`. Validation/serialization tests establish structural properties, not Oracle fidelity. Native `ScenarioTestBase` cases execute actual engine decisions, states and events for bounded source-derived contracts. Generic executor regressions belong in the engine fork; application integration tests bind the exact runtime separately from recorded definition/eligibility metadata when using experimental candidate integration.

Assurance separates representation, source fidelity, execution and computed qualification. It binds archived source/requirements/test hashes to a definition and engine revision. An open material concern blocks qualification; passing author scenarios does not create independent human approval. Catalog, inspection, discovery and per-card coverage expose assurance without making provisional research definitions ineligible. Inherited engine definitions do not receive silent certification. See [reliability](TRANSLATION_RELIABILITY.md) and [authoring](COMMANDER_AUTHORING.md).

Research diagnostics inspect native structures and source-led frozen subsets. An audit traversal is not an independent Oracle adjudication; sample agreement is not whole-catalog recall. Oracle coverage retains the eligible source pool separately from native availability, mapping uncertainty and objective observations. Benchmark cases are source-derived and implementation-bound; active `review-fork-v1.json` inputs coexist with preserved historical `review-v1.json` files. Exact-report baselines and compact receipts detect changes, not universal correctness. [Recorded verification](../HANDOFF_VERIFICATION.md) distinguishes local evidence from CI and current execution from historical results.
