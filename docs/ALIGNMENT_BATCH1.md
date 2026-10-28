# Alignment corrections and systematic coverage batch 1

## Scope and changes

Started from current authoritative main `d07a16f7f328963f1b5d0babc4d18ac05965ab0f`. Argentum remains pinned to `ebf5bf2e9fbb80d13a556d75491dd462e10c206f`; no upstream source, SDK vocabulary, engine behavior, Assay grammar, objective or extractor was changed.

Stage A (`39be129`) preserves native validation severity. ERROR findings still block registration/selection; warnings are inspectable through `Catalog.validationWarnings` and the additive commander JSON/text field `nativeValidationWarnings`, without inventing mechanical incompleteness. Required source provenance, Oracle identities, full color identity, legality and completeness checks remain enforced.

Registration reserves root and registered back/flip aliases, not embedded spell-face names. The real native Emeritus of Conflict/Lightning Bolt structure exercises admission through Catalog, and preserves the independently registered Lightning Bolt and its upstream provenance. Actual back/flip conflicts and duplicate identities remain rejected. Embedded faces no longer claim independent provenance entries.

`ExtensionStructureTest` applies native CardExporter goldens and CardLoader.fromJsonPreservingIds round-trips only to application extensions, including nested/embedded test structures. A focused source guard adapts upstream facade/pipeline rules; CardValidator remains responsible for semantic/dataflow lint. It is a lightweight source-text guard, not a complete Kotlin analyzer. Snapshot changes establish structural stability, not Oracle accuracy. Intentional updates require `UPDATE_EXTENSION_SNAPSHOTS=1`, an explicit test rerun and review of every golden diff; ordinary verification leaves update mode unset. The fast gate includes the new safeguards and batch tests.

## Frozen selection and outcomes

The pre-batch Oviya audit has 7,420 eligible physical Oracle cards and 3,442 confidently missing definitions. The exact snapshot SHA256 is `081a50ef82c767523f955e914153cf988ff045a72e3765b6aa2382567f9eb53a`.

The eight ordered slots are Creature, Instant, Sorcery, Enchantment, Artifact, Land, Creature, Artifact. Each selects the unused confirmed-missing card with minimum SHA256(snapshot hash + `:alignment-batch-1:` + Oracle ID), using primary card types only. Selection preceded Assay/SDK diagnosis; no objective, popularity, strength or parser-success screen was applied. No inconvenient card was replaced. `reference/alignment-batch1/selection.json` binds the original names, identities, input report and selection hashes.

| Selected physical card | Outcome | New engine cases |
|---|---|---:|
| Experiment Twelve | Native definition; disguise/controlled face-up trigger; power sampled at resolution | 2 |
| Revealing Wind | Unresolved filtered face-down inspection; not registered | 0 |
| Planewide Celebration | Native definition; four repeatable modes, targets, tokens, proliferate and life | 5 |
| Mortal Wound | Native Aura and damage-received destruction trigger | 2 |
| Tatterkite | Native flying artifact creature and all-counter prohibition | 6 |
| Tyrite Sanctum | Native mana, permanent God subtype and counter abilities; sacrifice cost | 2 |
| Ley Weaver | Native partner keyword plus explicit optional target-player tutor, reveal/shuffle, and two-land untap | 2 |
| Bitterthorn, Nissa's Animus | Unresolved atomic Germ creation/attachment plus equipped attack/search; not registered | 0 |

All eight produced parse/explain diagnostics; all eight declined complete Assay compilation. Six were nevertheless expressible through existing native structures and manually authored after separate Oracle/ruling review. A grammar decline was not treated as SDK absence. Differential comparison of complete Assay proposals was inapplicable; no complete proposals existed, and no zero-divergence claim is made.

The six definitions retain full identity, relevant mechanics, earliest real-printing metadata and reviewed rulings. They are available to every applicable commander. The source-led review caught Ley Weaver's required reveal (specified in its rulings even though omitted from reminder text) and the targeted player's control of the search. Lore Weaver itself is only a tutor-target test fixture, not another implemented card.

Review is by the implementing reviewer against separately retrieved authoritative sources, not independent human adjudication. Per-card limitations and exact executed cases are preserved in `reference/alignment-batch1/review.json`.

## Reused upstream facilities

Native DSL facades/Patterns, CardValidator's existing lint, CardRegistry, CardExporter/CardLoader, ScenarioTestBase, GameTestDriver, native priority/actions/decisions and Assay parse/explain/compile are reused. Existing comparisons include Mire Blight, Pine Walker, Cosmium Confluence, Argothian Elder, Blossombind and Okina. There is no runtime Oracle parser or JSON card loader.

## Reproduction and commands

Use the installed JDK 21 and the repository environment/bootstrap instructions. Baseline CLI binaries were copied after Stage A and before registering batch cards. Build that revision in a separate checkout for a fresh replay; the comparison runner accepts an external `--dist` so installations cannot overwrite a running baseline.

Executed Stage A test invocations (the first explicitly created the original four goldens; the second ran without update mode):

```powershell
.\dev.ps1 test --tests dev.mtg.assistant.ExtensionAdmissionTest --tests dev.mtg.assistant.ExtensionStructureTest --tests dev.mtg.assistant.CommanderIntegrationTest
.\dev.ps1 test --tests dev.mtg.assistant.ExtensionAdmissionTest --tests dev.mtg.assistant.ExtensionStructureTest --tests dev.mtg.assistant.CommanderIntegrationTest :assistant-cli:installDist :oracle-assay:installDist
```

Batch selection and source/tool evidence:

```powershell
.\cli\build\install\mtg-assistant\bin\mtg-assistant.bat audit catalog-coverage --commander 'Oviya Pashiri, Sage Lifecrafter' --oracle-cards generated/oracle/oracle-cards-20261026210159.jsonl.gz --json --output generated/alignment-batch1/selection-coverage.json
python scripts/coverage-batch.py select --report generated/alignment-batch1/selection-coverage.json --snapshot generated/oracle/oracle-cards-20261026210159.jsonl.gz
python scripts/coverage-batch.py sources
python scripts/coverage-batch.py assay --snapshot generated/oracle/oracle-cards-20261026210159.jsonl.gz
```

The sources action preserves existing archived files. Citizen token metadata was also retrieved from its exact Scryfall related-part URI, retained as `sources/citizen-token.json` and used for the token image. Assay parse/explain use a private cache containing the exact archived bulk; compile uses each archived printing object. Full reproducible stdout remains ignored under generated; tracked diagnostics retain verdicts, decline locations, exit codes and output hashes.

After authoring, ran the focused extension tests plus each of the six named ScenarioTest classes; then explicitly refreshed the goldens with `ExtensionStructureTest :assistant-cli:installDist` in update mode. Final full verification used:

```powershell
.\dev.ps1 test --rerun
python scripts/phase29-verify.py before --dist generated/alignment-batch1/before-dist --snapshot generated/oracle/oracle-cards-20261026210159.jsonl.gz
python scripts/coverage-batch.py status
python scripts/phase29-verify.py after --dist generated/alignment-batch1/after-dist --translation-review reference/alignment-batch1/coverage-status.json --snapshot generated/oracle/oracle-cards-20261026210159.jsonl.gz
python scripts/coverage-batch.py receipt
```

The before invocation actually preceded batch authoring. `phase29-verify.py` retains its original defaults; only the optional review-ledger argument was added. `coverage-status.json` derives the existing flat CLI vocabulary from the previous and current reviews; the richer review retains independent evidence dimensions.

## Verified results

The complete application run passed **386 tests in 40 suites, with zero failures, errors or skipped tests**, including **19 new card-specific engine scenarios**. The full Gradle invocation took 4m27s. Both Stage A focused runs passed 25 tests; the first batch-focused run passed 26 tests before the additional proliferate case. The ten extension goldens pass without update mode; the four original goldens are unchanged.

All nine commander/objective comparisons preserve every pre-existing non-count field after removing only the six new cards' rows. This includes eligibility, actions, native evidence, facts, results, verdicts, unsupported structures, support records, limitations and input-card lists. Aggregate counts are recorded separately; authored physical definitions grow from 16,999 to 17,005. These are controlled retained-evidence comparisons, not byte-identical full reports.

| Context | External eligible pool (unchanged) | Confirmed missing before → after | New confident native identities |
|---|---:|---:|---:|
| Judith | 12,828 | 6,165 → 6,163 | 2 |
| Krenko | 7,552 | 3,632 → 3,630 | 2 |
| Oviya | 7,420 | 3,442 → 3,436 | 6 |

Retained external coverage records are unchanged apart from intentional research-status updates. The seven existing eligibility corrections retain their source-bound behavior. Whole-context verification took 740.9 seconds before and 731.1 seconds after, including JVM startup and the existing coverage/discovery/diff commands; these are measured automated-run times, not human translation effort.

Planewide Celebration contributes a conditional token-creation MATCH to objective C. Its graveyard-to-hand mode is correctly NO_MATCH for battlefield reanimation. Mortal Wound demonstrates why dimensions remain separate: native scenarios execute its damage/destruction ability, while objective A retains UNKNOWN mechanical compatibility for `EnchantedCreature` binding and NO_MATCH policy acceptance for ordinary friendly-target removal. Its final policy NO_MATCH is not evidence that its native mechanics are wrong or absent. Other new cards retain unsupported structures or objective nonmatches without manufactured relevance. Per-result mechanical/policy decisions are retained in the batch review.

`reference/alignment-batch1/verification.json` contains strict assertions, report hashes, counts, command timing summaries and executed scenario names. `review.json` records each card's independent dimensions and untested interactions. The two unresolved cards remain missing native definitions and gain investigated status; no incomplete script is registered.

## Execution limits and recovered setup failures

The pre-existing installed CLI lacked current main's enrichment option; it was rebuilt. An initial baseline overlapped distribution installation and failed with class loading; the successful baseline and after comparisons use immutable distribution copies. A Windows default-encoding error in new research files was corrected to UTF-8. An initial coverage run rejected new status labels; the derived ledger now uses the existing accepted vocabulary. These were tooling/evidence-run failures, not demonstrated card-engine failures; failed attempts are not counted as successful comparisons.

No new SDK or engine gap was established for the six admitted cards. Revealing Wind's current LookAtFaceDown scopes do not directly express the required combat-filtered group, but filtered iteration may compose it; hidden-information and optional-choice execution still need proof. Bitterthorn needs a typed creation-to-attachment binding in one resolution before state-based actions; isolated 0/0 creation is not an acceptable approximation. Neither case proves categorical SDK incapacity, and both remain explicit unresolved research results.

## Implications

This batch demonstrates reusable safeguards and six successful manual translations despite zero whole-card Assay compilations. It is a small type-stratified sample, not a throughput estimate or whole-pool coverage claim. Runtime measurements are retained for automated checks; selection/authoring/review human effort was not instrumented and no per-card timing is presented as measured.

Existing primitives and native scenarios are productive, but review of bindings, choices and rulings remains material work. The smallest next engineering question is whether native typed composition can prove Bitterthorn's creation/attachment sequence without new SDK vocabulary; Revealing Wind separately needs scoped visibility/iteration proof. No broader architecture or bulk-translation work is implied.
