# Translation reliability procedure and experiment

## What admission currently means

At base `4e047c796e2fd88335fa65fd95ed6fa745f48bd7`, Extensions.validate requires dated application source provenance, an Oracle ID, full identity/legality metadata, no declared known omissions, unique root/back/flip identities and zero native validation ERRORs. Warnings remain visible. DefinitionReview contains scenario paths and known limitations; registration does not execute those scenarios or adjudicate their assertions. ExtensionStructureTest stabilizes serialization, snapshots and authoring boundaries, not correspondence to Oracle.

Catalog preserves native definitions and origin; Commander inspection already distinguishes scenario references from independent verification. Discovery MATCH means structural potential under prerequisites. Coverage NATIVE_AVAILABLE means a confident authored mapping with adequate eligibility metadata. Neither label proves fidelity. Historical VERIFIED_NATIVE_EXTENSION is a completed-authoring research label; it never supplies independent approval. The old ledgers, definitions and their test history are preserved. Historical wording such as “independent manual review” is not itself an independent human approval record.

The risk was most visible where discovery carried provenance but no separate translation-assurance state, or a reader treated a historical VERIFIED label or a list of tests as certification. Additive assurance metadata now travels through Catalog, card/Commander inspection, discovery JSON/text and per-card external coverage. It does not change Commander eligibility, identity association, query predicates or provisional-card access. Upstream cards have no application assurance assessment; they are not silently certified or accused.

## Repeatable source-first procedure

1. Choose a bounded risk sample and record why it was chosen. Confirm physical identity, relevant faces, authoritative Oracle text and published rulings. Reference existing archived sources by path and hash rather than copying catalogs.
2. Before inspecting implementation notes, write explicit requirements with stable IDs and consequential questions. Commit that version. Preserve costs/effects, mandatory/optional actions, controller/owner/source/host bindings, target versus resolution choice, timing, sequential operations and last-known information. Correct a mistaken requirement in a new version, never silently redefine it to pass a test.
3. Reconcile each requirement against native relationships. Record what structural inspection establishes and what needs execution. Native examples are implementation evidence, not the specification.
4. Derive the cheapest meaningful adversarial test for each consequential question. Assert native actions/decisions and resulting state/events; inspect positive, negative and boundary cases. Keep the source expectation unchanged when it fails.
5. Diagnose failures as source interpretation, representation, engine execution, discovery extraction or harness errors. Preserve first failure evidence and reproduction before correcting anything. Untested cases are gaps, not automatically suspected defects.
6. Exercise the same checks against unmodified controls and isolated, structurally valid semantic mutations. Assertion helpers receive only the definition, not a defect description. Record validator findings, source-probe findings, execution discrimination, control failures and confounded results separately.
7. Record a qualification decision bound to the exact native definition, SDK pin, sources, requirement version, test evidence and concern history. Agent review and author scenarios support provisional research; independent qualification requires explicit separate human review of the bundle and remaining limitations.

`reference/translation-reliability/requirements.json` freezes 23 source-derived requirements for six assignment-selected cards across both batches: Predatory Urge, Planewide Celebration, Hour of Promise, Sword of Truth and Justice, Ley Weaver and the simpler Tatterkite. They were not chosen by existing test strength. Source archives were confirmed locally by identity/content/hash. Requirements were committed before this assignment inspected implementations and author explanations.

The implementing agent also authored earlier batches and retains prior familiarity. This is **source-first agent review**, not a blind experiment or independent human certification. Source probes are small checks of the reviewed native shapes, not a general semantic equivalence checker. The same agent knows the mutations; passing controls and separation of inputs reduce circularity but do not establish independent judgment.

## Assurance and promotion

DefinitionReview can carry TranslationEvidence. New definitions without evidence are provisional, unreviewed and not tested by default. Historical evidence is explicitly preserved as author/source-led bounded evidence. Representation (complete/partial/unresolved), source fidelity, execution and computed qualification are separate. Per-objective discovery results and unsupported records remain the discovery dimension rather than a new universal score.

The qualification function is pure and recomputes the definition digest and SDK binding. A review can be qualified only with complete representation, source-reviewed fidelity, bounded passed execution, source/requirements/test references, no open material concerns and explicit independent human approval by a reviewer distinct from the author. Approval binds the entire evidence bundle digest, including limitations and concern history. Changing definition, SDK pin or referenced source/requirements/tests invalidates approval. Previous approval becomes WITHDRAWN; an unapproved material failure is BLOCKED. Missing approval leaves PROVISIONAL. No actual card is independently qualified by this assignment.

The approval record is Git review evidence, not a cryptographic identity service. Humans must verify reviewer identity, independence, source revision and review adequacy. Runtime checks bind recorded reference hashes; TranslationAssuranceTest checks those hashes against the retained files. A source update must update the ledger reference and supporting review in the same reviewed change; tests detect a stale file hash. Runtime does not fetch current Oracle information or independently prove source authenticity.

Promotion is a concrete review action: reconcile the frozen requirements, inspect executed evidence and limitations, resolve or explicitly block material concerns, then have a separate human record a dated approval reference and the exact bundle digest. Passing automation cannot fill that record. Isolated approval fixtures demonstrate PROVISIONAL versus QUALIFIED reporting, author-self approval rejection and revocation on changed evidence. They never enter Extensions.all.

## Concern workflow

A concern identifies physical identity, source hashes/revision, native definition or shared mechanism, disputed claim, evidence, reproduction status, category, state, next action and resolution evidence. The lightweight Git record is `concerns.json`; runtime-relevant concerns are retained in the same card's assurance evidence. Suspected/confirmed/unresolved material concerns block independent qualification while research access remains. Discovery-only uncertainty stays distinct. Corrected or disproven records retain their history and regression reference; absence of testing alone is not a defect accusation.

## Reproduction

Use the repository JDK 21 environment and dev wrapper. Requirements and first failure receipts are tracked; full reports, logs and distributions stay in ignored generated directories.

```powershell
. .\.env.ps1
.\dev.ps1 test --tests dev.mtg.assistant.TranslationReliabilityScenarioTest --tests dev.mtg.assistant.TranslationMutationTest --tests dev.mtg.assistant.TranslationAssuranceTest
# Explicitly expose the known pinned engine failure as a failing Oracle assertion:
$env:VERIFY_KNOWN_ENGINE_GAPS='1'
.\dev.ps1 test --tests dev.mtg.assistant.TranslationReliabilityScenarioTest
Remove-Item Env:VERIFY_KNOWN_ENGINE_GAPS
```

The standard suite's known-failure contract verifies that the documented zero-counter assertion still fails in the expected way; it is not a passing correctness claim. The opt-in command leaves the source assertion uncaught and is expected to fail until the shared executor is corrected. This makes the unresolved failure visible without silently ignoring a test or loosening its Oracle expectation.

## Reassessment findings

| Card | Source requirements | New adversarial coverage and finding |
|---|---:|---|
| Predatory Urge | 4 | Opponent-controlled host, target loss, Aura removal and host last-known damage pass. Self-targeting and more damage replacement interactions remain gaps. |
| Planewide Celebration | 4 | All targets illegal, partial targets legal, repeated same target and printed modal order pass. Player-counter and copying interactions remain gaps. |
| Hour of Promise | 3 | Zero-card search/shuffle, opponent Deserts excluded and fetched-Desert threshold pass. Search restrictions and response-time Desert removal remain gaps. |
| Sword of Truth and Justice | 4 | Shroud choice, no creatures but proliferate, noncombat exclusion and prohibited-counter recipient exercised. The last exposed a real shared execution failure; the local native composition correction passes the same assertion. |
| Ley Weaver | 4 | Targeted opponent declines or accepts, actual reveal/shuffle and failure to find pass. Search restrictions and partial untap legality remain gaps. |
| Tatterkite | 4 | Source shape is correct and old direct-placement tests pass; collection placement violates its counter prohibition. Execution concern remains confirmed and qualification blocked. Cost/wither/entry cases remain untested here. |

`assessment.json` reconciles all 23 requirements individually, distinguishing structural evidence, new cases, retained evidence and remaining uncertainty. Eighteen new real-card scenario contracts were added; one is the explicit known-failure contract. No general completeness conclusion is inferred from their count.

Prior scenarios were useful but insufficient to find the collection-path gap: Tatterkite tested three direct counter kinds with a life-gain continuation; Sword tested normal counter choice/proliferation without a prohibited recipient. They did not establish all-path counter prohibition. No source-to-native mismatch was confirmed in the six original definitions; an engine path used by correct-looking source compositions was demonstrably wrong. The local Sword change avoids that path with existing ForEachInCollection and single-object AddCounters, preserving choice and sequence. Its original failing test is committed before the correction. Only Sword's native golden changes; Argentum is unchanged. The tested implementation is recorded at `4a69578f14c6425e1f50a37b64d6a271808aabe0`; later commits contain the final documentation/receipt.

The broader AddCountersToCollection executor still lacks the canReceiveCounters check used by AddCountersExecutor. Tatterkite remains registered for research with COMPLETE representation, SOURCE_REVIEWED fidelity, FAILURE_OBSERVED execution and BLOCKED qualification. This is an execution diagnosis, not a claim that Tatterkite's native definition omitted its ability. Sword's concern retains suspected → confirmed → resolved history and the regression reference; its corrected definition remains PROVISIONAL. The shared-engine concern stays open. No upstream source or pin change is made.

## Deliberate defect experiment

Nine isolated native copies cover live-source/LKI loss, missing tap cost, mandatory-versus-optional search, wrong choosing controller, condition before movement, wrong destination zone, omitted counter prohibition, targeting replacing a nontargeting choice and omitted reveal. All nine are meaningfully different from their controls and accepted by CardValidator with zero errors and zero warnings. None enters Extensions.all.

Source-derived native-shape probes detect all nine. Identical adversarial execution assertions discriminate eight from passing controls. The omitted Tatterkite prohibition is **not discriminated by execution**: its source-correct control already fails through the collection engine path, and the mutant fails too. That confounded result is explicitly retained, not counted as successful execution detection. Source inspection still detects the omitted prohibition.

There were no observed source-probe false alarms in the controls. One execution-control failure was a genuine engine gap, not proof of a translation mismatch. Initial shuffle accounting, Kotlin assertion precedence and unsuitable single-target fixture composition produced harness alarms and were diagnosed before conclusions. A named-case Gradle filter selected no tests; the actual Sword reproduction used the whole class. `initial-execution.json`, `counter-gap-failure.json`, `sword-failure.json` and `concerns.json` preserve these distinctions.

`mutations.json` records each validator outcome, violated requirement IDs, exact observed assertion, control result and qualification outcome. The verifier helpers were not given mutation descriptions, but their designer knew the mutations; this is a small same-agent detection experiment, not a measured estimate of general accuracy. There is no independently certified promotion. Even a plausible mutant with passing author tests remains provisional without separate approval; material recorded failures block it.

## Executed verification and remaining decision

The complete run `dev.ps1 test --rerun :assistant-cli:installDist` passed **454 test contracts in 56 suites**, with no escaped failures, errors or skips, in **1m50s**. This includes the known-failure contract and must not be read as 454 successful Oracle correctness checks. The opt-in reproduction ran separately, failed exactly the retained TK2 source expectation, and took 38s. The corrected focused run passed 39 contracts in 1m19s. Both Python receipt compilation and fast-gate shell syntax checks passed; the fast wrapper was not separately run after the complete suite. These are command elapsed times, not engineering labor estimates.

Before/after coverage and discovery use the existing phase29 runner, the existing hash-identified Oracle snapshot and immutable distributions. The baseline distribution is the unchanged PR #6 head merged by current main; it has identical native/application behavior to that base merge. Assurance is additive metadata. Sword's native action change is intentional and separately retained. The strict receipt passed all nine comparisons: physical pools, coverage counts and eligibility are unchanged; every other retained field is identical after removing only assurance metadata and the corrected Sword's mechanical rows. It does not normalize arbitrary verdict changes. Each report exposes 21 provisional entries, one blocked entry and zero independently qualified entries. Sword's changed rows retain compact counts/type summaries/hashes rather than duplicating its action trees nine times; the full reproducible reports remain ignored.

```powershell
python scripts/phase29-verify.py before --output-base generated/translation-reliability --dist generated/alignment-batch2/after-dist --translation-review reference/alignment-batch2/coverage-status.json --snapshot generated/oracle/oracle-cards-20261026210159.jsonl.gz
python scripts/phase29-verify.py after --output-base generated/translation-reliability --dist generated/translation-reliability/after-dist --translation-review reference/alignment-batch2/coverage-status.json --snapshot generated/oracle/oracle-cards-20261026210159.jsonl.gz
python scripts/translation-reliability-receipt.py
```

The procedure now demonstrates useful semantic defect detection and an honest blocking decision, but does not supply independent approval. Another small **provisional** batch could use it, with risk-based source-first requirements and explicit unresolved issues. Substantial independently qualified expansion should wait for separate human review and resolution of the demonstrated collection-counter gap. The smallest next engineering question is whether the collection executor can reuse prohibition-aware placement while preserving per-recipient replacements, events and continuation behavior; that requires a separately reviewed engine correction at the settled pin policy. No next batch or engine project is started here.
