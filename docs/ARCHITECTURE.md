# Architecture and extension points

[README](../README.md#current-state-and-authority) is the current-status entry point. [Objective direction](OBJECTIVE_MODEL.md) owns product concepts and hypotheses; this document owns actual implementation responsibilities.

## Conceptual direction and implementation boundary

Card identity/rules → native definitions → mechanical facts/capabilities → objective discovery and selective layering → experimental deck evaluation. The last two stages combine current bounded discovery with intended capabilities; they are not completed services or a module-refactoring plan. General relationship filters, selective composition and deck evaluation remain unimplemented. A dedicated pairwise strategic synergy layer is not a prerequisite in the current hypothesis; source/object/resource/zone/timing/condition/cost/effect bindings remain necessary for causal mechanical relationships. See the product record and [bounded assessment](OBJECTIVE_BUILDING_BLOCKS.md) for the working building-block framing, optional role descriptions and empirical direction. No new schema, extractor or objective implementation follows from this explanation.

## Coverage and evidence projections

`server/CostExamination.kt` projects frozen source/native/adjudication phases and revision-bound native payment observations into the existing verification index. `CostPaymentBoundary.kt` uses genuine Argentum transactions and preserves rejected, paused and contradicted behavior separately. Raw/LF artifact identities are explicit; examination does not issue independent approval. See [phased examination and payment boundary findings](COST_EXAMINATION.md).

`coverage/VerificationCoverage.kt` joins exact scoped evidence to the retained global identity baseline and existing assurance gate. `server/SacrificeChallenge.kt` examines the unchanged pilot method with isolated adversarial definitions and explicit native execution. Neither extends discovery interpretation, Commander eligibility or independent approval. See [verification identity, ingestion and completeness boundaries](VERIFICATION_COVERAGE.md).

`coverage/ActivationCostModel.kt` projects supported native payment trees without solving payment legality. Source-first requirements and inspected ability bindings feed `ActivationCostFidelity`; `ActivationCostExecution` reuses genuine Lab transactions. Their additive records enter the existing verification index and Cards/Trust view. Unknown components fail closed, and declaration/resolution dependencies remain outside printed-base payment assurance. See [cost identities, freshness and evidence limits](ACTIVATION_COST_FIDELITY.md).

Global Commander coverage, bounded single-commander inventory and objective reach reuse the catalog audit, effective metadata and existing discovery queries. The Progress projection and immutable baseline contract are documented in [Global Commander coverage](GLOBAL_COMMANDER_COVERAGE.md#architecture-and-api); the narrow sacrifice pilot does not introduce another interpreter or qualification gate.

## Current implementation

The application is an external Kotlin Gradle application module at the repository root attached through `gradle/argentum.init.gradle` to the sibling `../argentum-engine` on the maintained fork's `app-stable` branch. `dev`/`dev.ps1` enforce branch, clean source and fork origin. The branch selects the dependency; generated build metadata and reports record the actual engine SHA. Native/application compatibility is checked before engine acceptance. See [engine workflow](ENGINE_WORKFLOW.md).

Argentum owns native `CardDefinition`/`CardScript` data and DSLs, card discovery/registry, search, pure rules execution, replacement/projection/continuation/event machinery and native scenario infrastructure. The application includes a React frontend and local JDK HTTP adapter; it does not copy the SDK or add its own rules engine, database, game server or Oracle mechanics parser. Engine implementation guidance belongs to the fork's [architecture](https://github.com/jasullivan5/argentum-engine/blob/app-stable/docs/architecture-principles.md) and [SDK reference](https://github.com/jasullivan5/argentum-engine/blob/app-stable/docs/card-sdk-language-reference.md); consult the actual recorded engine revision when changing integrations.

## Research interface

The primary interface is **Discover / Progress / Primer**. `server/ResearchProgress.kt` projects catalog provenance, the existing independent native-node audit, shared extraction facts, explicit-snapshot catalog coverage and the existing extension assurance gate. It runs under the existing bounded worker and caches measurements per server/build without replacing the latest discovery. React presents those measurements; it does not interpret mechanics. Compact historical comparison evidence is packaged separately with exact original populations and revisions. Secondary investigations remain behind the developer research index. See [measurement definitions and boundaries](RESEARCH_PROGRESS.md).

The Explorer implements an independent React / TypeScript / Vite frontend in `web/`, served by the application-owned JDK `HttpServer`. It directly calls existing Kotlin commander inspection and discovery services. The CLI and headless pipeline remain supported; React adds no mechanical acceptance rules.

```text
React research UI (application-owned)
  -> loopback HTTP /api/v1 (application-owned)
  -> existing discovery / eligibility / assurance (application-owned)
  -> sibling app-stable Argentum SDK and rules engine (fork-owned)
```

`server/ExplorerServer.kt` owns transport, strict requests, readiness, bounded execution and static assets. `server/ExplorerModels.kt` projects existing reports into a compact card index and selected-card evidence. A streamed export retains the complete unchanged report. `server/FindingExplanation.kt` describes retained operations; `server/AbilityInspection.kt` projects exact stored activations into costs, target dependencies and effects without changing acceptance. `server/DiscoveryExplanation.kt` describes recorded objective decisions, conditional death outcomes and assumptions without another acceptance test; see [objective coverage](OBJECTIVE_COVERAGE.md). `server/EvidenceSummary.kt` presents existing assurance/qualification gate outputs as scoped evidence states; it does not create another certification system. Original decisions and prerequisites remain inspectable. Ordinary browsing prioritizes cards with identified contributions, then unresolved-only relationships. Overlapping diagnostics remain optional. Mechanic filtering groups existing operation records; it introduces no strategic score. The commander presentation endpoint preserves the original inspection API and adds verified imagery/fallbacks, card text, color identity, a bounded single-card objective connection and applicable evidence. It omits the inapplicable mechanic status before discovery. Evidence uses structured display fields over the same exact technical records; absence of independent review does not block discovery. `server/CardImages.kt` verifies physical printing/face metadata for visible cards against Scryfall, with bounded batches, caching and explicit fallbacks. Image identity does not change mechanics or eligibility. Builds package the actual application revision (with +dirty when applicable) and engine SHA.

`server/CardText.kt` makes conservative whole-paragraph display associations to stored action descriptions; it never derives mechanical facts from Oracle text. The frontend renders those backend associations, prints the rules once, and displays only additional objective notes by default. `server/EvidenceDetails.kt` reads hash-bound existing source snapshots packaged by processResources to identify recorded source provenance. Missing reviewer/date/ability-review metadata remains missing. These projections do not alter the assurance contract or qualification gate.

One exclusive research worker prevents overlapping expensive queries. Four bounded HTTP workers serve loopback requests; research has a 120-second response deadline, with the worker reserved until timed-out work actually ends. Only the latest run is retained: starting another discovery expires earlier evidence URLs. Host/origin validation, bounded JSON bodies and resolved static-path containment constrain the local boundary. See [schema, lifecycle, errors and observed limits](EXPLORER.md).

The Windows launcher installs locked frontend dependencies, builds both layers and starts one foreground JVM. Live discovery, verified imagery/fallbacks, focused finding inspection and Progress are implemented. The Mechanical Verification Lab seeds curated positions and submits genuine native actions through Argentum, showing expected/observed outcomes and exact state/events; it is not an arbitrary sandbox or deck simulator. See [Lab responsibilities and limits](MECHANICAL_VERIFICATION_LAB.md#runtime-design). M1/M2 have bounded user acceptance; later merged features and tests do not imply blanket experience acceptance or translation certification. [Review history](EXPLORER_MILESTONES.md) retains earlier failures and decisions.

The frontend presents native extraction and application decisions. Accounts, multiplayer, deck editing, databases, desktop packaging, hosted infrastructure, new objectives and replacement rules execution remain outside M1. Selective reuse of Argentum client assets still requires coupling, provenance and licensing review. [Presentation safeguards](../AGENTS.md#visualization-fidelity) apply to future increments.

The [objective direction](OBJECTIVE_MODEL.md) distinguishes mechanical capabilities, objective-relative roles and contextual value. Current query coverage remains narrower; future composition and evaluation require separately approved assignments.

## Application pipeline

Native definitions → effective Commander eligibility → shared native action/branch extraction → reusable facts/constraints → objective queries and policies → evidence/reporting and research diagnostics.

Files below are relative to `src/main/kotlin/`.

| Concern | Implementation |
| --- | --- |
| Native catalog and search | `Catalog.kt`, `DefinitionSearchCard.kt`; native registry plus `mtg-search` projection |
| Application-owned native definitions | `extensions/Extensions.kt` and per-card files; provenance, duplicate/admission checks and source/test review metadata |
| Commander selection and full color identity | `discovery/Eligibility.kt`, `CommanderInspection.kt`; consumers use `Catalog.eligibility` |
| Reviewed effective metadata | `eligibility/EffectiveEligibility.kt`, packaged `eligibility/corrections.json`; source/snapshot/implementation/native-fingerprint checks |
| One native walker | `discovery/PathwayExtractor.kt`; actions, costs/effects, branches, bindings and unsupported structures |
| Reusable facts, predicates and quantities | `discovery/MechanicalFacts.kt`, `TokenFacts.kt`, `AmountBounds.kt`; `EventOutcomes.kt` joins native trigger context to same-branch draw/token outputs using existing action IDs, token facts and correlated filters |
| Death objective A | Legacy death/removal projection plus shared `EventOutcomes.kt` triggered event/outcome intersection, and `SelfReplenishment.kt` original-source movement/context joins, assembled in `Discovery.kt` |
| Historical-state dependencies | The walker records the condition, checkpoint, native alternatives and branch links to shared operation evidence in `StateDependencies.kt`. The record identifies a supported change independently of the anchor. `StateBeneficiaryReport.kt` queries these records for compatible history checks. |
| Reanimation B / token C / graveyard state | Fact compositions and common reporting in `discovery/Reanimation.kt`, using shared movement/token facts; no separate native walkers |
| Evidence and CLI | `discovery/Evidence.kt`, `Discovery.kt`, `Main.kt`; native source provenance binds the exact fork revision |
| Identity/coverage observations | `coverage/OracleSnapshot.kt`, `OracleEnrichment.kt`, `CatalogCoverage.kt`; offline source snapshots and bounded mappings |
| Translation assurance | `extensions/TranslationAssurance.kt`, packaged `translation-assurance.json`; exact definition/implementation/evidence bundle and independent-approval checks |
| Diagnostics and frozen reviews | `audit/Diagnostics.kt`, `RootDiagnostics.kt`, `Benchmark.kt`, `CrossObjective.kt`; coverage, inconsistencies, comparisons and report differences |

## Eligibility and identity boundaries

Discovery checks Commander format legality, then full identity subset, then optional conventional search. Unknown metadata and unsupported commander construction remain explicit. A candidate query does not validate a whole deck, singleton rules, companions or every special commander form.

The physical identity layer compares archived Oracle observations to authored native identities, preserves existing IDs and quarantines competing claims. Association does not edit definitions or independently authorize eligibility changes. Reviewed effective metadata is a separate application-owned overlay; the original native definitions, scripts and values remain available. Inspection, discovery, conventional identity/format projections and coverage share `Catalog.eligibility`. See [identity](ORACLE_IDENTITY.md), [eligibility](ELIGIBILITY_CORRECTIONS.md) and [coverage](CARD_COVERAGE.md).

## Mechanical and assurance boundaries

Extend the existing walker and fact predicates before adding another interpretation path. Preserve correlated owner/controller, source/host, zone, branch, target, quantity, cost/effect and timing constraints. Objective policy acceptance is distinct from structural possibility. Objective A is not fully fact-driven; shared action IDs do not establish whole-action causality. Complex copying, Amass, linked intermediate-zone reanimation and unresolved bindings remain incomplete. See [limitations](KNOWN_LIMITATIONS.md).

Application extensions are native Argentum definitions registered through `Extensions.all`. Validation/serialization tests establish structural properties, not Oracle fidelity. Native `ScenarioTestBase` cases execute actual engine decisions, states and events for bounded source-derived contracts. Generic executor regressions belong in the engine fork; application integration tests bind the exact runtime separately from recorded definition/eligibility metadata when using experimental candidate integration.

Assurance separates representation, source fidelity, execution and computed qualification. It binds archived source/requirements/test hashes to a definition and engine revision. An open material concern blocks qualification; passing author scenarios does not create independent human approval. Catalog, inspection, discovery and per-card coverage expose assurance without making provisional research definitions ineligible. Inherited engine definitions do not receive silent certification. See [reliability](TRANSLATION_RELIABILITY.md) and [authoring](COMMANDER_AUTHORING.md).

Research diagnostics inspect native structures and source-led frozen subsets. An audit traversal is not an independent Oracle adjudication; sample agreement is not whole-catalog recall. Oracle coverage retains the eligible source pool separately from native availability, mapping uncertainty and objective observations. Benchmark cases are source-derived and implementation-bound; active frozen inputs live under `src/test/resources/regression/`; unused historical reviews are exported. Exact-report baselines and compact receipts detect changes, not universal correctness. [Repository layout](REPOSITORY_LAYOUT.md) distinguishes active evidence from exported historical receipts.
