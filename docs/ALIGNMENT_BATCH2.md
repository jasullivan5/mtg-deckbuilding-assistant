# Systematic native card coverage, batch 2

## Scope and frozen selection

Started from authoritative main `79d4b5cde31546cb29e6f529dd3a8faba5410d5d` with a clean checkout. Argentum remains unchanged and clean at `ebf5bf2e9fbb80d13a556d75491dd462e10c206f`. Production changes are twelve complete application-owned definitions and registration; no SDK, engine, extractor, eligibility, objective or dependency changes.

Selection used Oviya's verified green color identity and the existing archived Oracle snapshot, SHA256 `081a50ef82c767523f955e914153cf988ff045a72e3765b6aa2382567f9eb53a`. The original report contained 7,420 eligible physical identities, including 3,436 confidently missing definitions. Slots are Creature, Instant, Sorcery, Enchantment, Artifact, Land, repeated twice. Each takes the unused missing identity with minimum SHA256(snapshot SHA + `:alignment-batch-2:` + Oracle ID), without text, objective, popularity, ease, familiarity or Assay filtering. Prior investigations do not affect ranking; none of these twelve had been investigated before.

`reference/alignment-batch2/selection.json` binds the original coverage report, identities and hashes. Selection was frozen before diagnosis and later reproduced against the saved original report; no card was replaced. The selector now rejects changed frozen inputs rather than overwriting evidence.

## Implemented cards and bounded execution

All twelve definitions passed source-led review against archived Oracle information, earliest real printing metadata and published rulings. This was review by the implementing reviewer, not independent human adjudication. Complete representation and passing scenarios do not imply exhaustive engine verification.

| Physical card | Significant native behavior exercised | New cases |
|---|---|---:|
| Spined Tyrranax | Optional payment before reflexive target choice; pay and decline | 2 |
| Might of Murasa | Kicked +5/+5 replaces +3/+3; end-of-turn cleanup; insufficient mana | 3 |
| Savage Conception | Beast creation; repeat graveyard casts with land discard; nonland rejection | 2 |
| Predatory Urge | Granted tap ability; sequential damage; host removed in response with last-known power | 2 |
| Paradise Mantle | Equipped creature supplies/taps for mana; summoning sickness rejects activation | 2 |
| Labyrinth of Skophos | Actual attacker removed from combat; remains tapped; noncombat target rejected | 2 |
| Magus of the Library | Six/seven/eight-card activation boundary; condition not rechecked at resolution | 4 |
| Galadhrim Ambush | Actual attackers counted at resolution; non-Elf prevention and Elf damage; zero attackers | 2 |
| Hour of Promise | Any lands enter tapped; fetched Deserts count toward the two-Zombie condition | 2 |
| Momentum | Optional upkeep growth on Aura; opponent's host; continuously recalculated bonus | 2 |
| Sword of Truth and Justice | White/blue protection targeting and green control; actual combat choice then proliferate | 4 |
| Throne of Makindi | Charge cost; restricted mana rejected for unkicked spell and accepted for kicked spell | 2 |

Every selected card has native scenarios, metadata assertions, severity-aware admission, registry identity protection, a CardExporter golden, serialization round-trip and authoring-boundary checks. Original ten goldens are unchanged. The 29 new cases use real native actions, priority, decisions and state, not mocked effects. `review.json` enumerates exact cases and meaningful untested interactions per card, including protection blocking/damage, multiplayer, replacement effects, control changes and additional timing boundaries where applicable.

## Assay, failures and workflow evidence

Parse and explain executed for all twelve. Complete Assay compilation succeeded for Paradise Mantle and Throne of Makindi, and declined for ten. Both successful proposals have scoped structural comparisons against the independently authored definitions, normalizing only activated-ability identifiers; target bindings remain compared. This agreement is not an accuracy proof. All twelve were expressible with existing SDK structures.

Initial compilation exposed authoring mistakes in kicker syntax, imports, a test helper and a combat-trigger facade name. The first compiling focused run had two failures: Predatory Urge explicitly resolved `Self` as a live damage dealer and failed after its host left; Sword's test expected a choice where a singleton was automatically chosen. The corrected Urge uses the ability's normal source to preserve last-known information, rather than a live-object override copied from a comparator. The Sword fixture now supplies two eligible creatures. Both corrected scenarios pass; these do not establish engine defects. `initial-failures.json` retains the actual messages, log hash and diagnoses.

Existing typed payment restrictions, reflexive triggers, conditions, pipelines, granted abilities and continuous effects handled the cards. Source/host/dealer binding and operation sequencing required the most interpretation. The shared authoring guide now explains the demonstrated last-known-source pitfall; no new runtime abstraction was necessary. Per-card effort is qualitative in `review.json`; active human engineering time was not instrumented.

Measured command times: archived source acquisition 15.633 seconds total; 36 Assay commands 88.412 seconds total; final full Gradle invocation 5m9s. Acquisition includes network/pacing and Assay measurement includes orchestration overhead. Whole-context coverage/discovery/diff runs took 555.921 seconds before and 750.569 seconds after, including JVM startup. These are automated elapsed measurements, not translation productivity or a controlled performance benchmark. Batch 1's full run took 4m27s; cache, load and test-count differences prevent causal comparison.

## Verification and discovery

The complete application regression run passed **419 tests in 53 suites, zero failures, errors or skips**, including 29 new card scenarios and four BatchTwoIntegrationTest cases. Focused corrected bindings, admission, structure, catalog and Commander integration checks also passed before the full run. Goldens were deliberately generated, then verified without update mode. The fast gate now selects all ScenarioTest classes and the Batch Two integration class; its shell syntax was checked, but the fast wrapper was not separately executed after the full suite.

| Context | Eligible external pool, unchanged | Missing before → after | New confident identities |
|---|---:|---:|---:|
| Judith | 12,828 | 6,163 → 6,159 | 4 |
| Krenko | 7,552 | 3,630 → 3,626 | 4 |
| Oviya | 7,420 | 3,436 → 3,424 | 12 |

Native physical definitions increase from 17,005 to 17,017; application extensions from 10 to 22. Across all nine commander/objective reports, removing only the new card rows and aggregate counts leaves identical retained evidence: eligibility/provenance, actions, facts, verdicts, causal evidence, constraints, dependencies, unsupported structures and support records. External coverage identity pools are unchanged, and retained rows are unchanged apart from permitted review-status fields. `verification.json` records strict assertions, before/after hashes, counts, command timings and scenario names.

Savage Conception and Galadhrim Ambush contribute token-creation MATCH results. Retrace does not manufacture a reanimation match. Hour of Promise's library search remains NO_MATCH for reanimation, while its conditional Zombie creation remains UNKNOWN in discovery despite successful native conditional scenarios. Several granted or continuous structures retain unsupported records; Magus has no facts relevant to these objectives. Empty result lists are not relabeled as explicit NO_MATCH. No uncertainty was removed by assumption and no strategic value is claimed.

## Reproduction and compact evidence

Use JDK 21 and the repository Windows environment (`. .\.env.ps1`). Build main into an immutable `before-dist` before authoring, and the final branch into a distinct `after-dist`; never install over a running comparison. Full generated reports and logs stay ignored. Tracked evidence contains compact selected-source projections, provenance, rulings, three linked token records, meaningful Assay diagnostics, two compiler proposals, per-card review and the strict receipt. The shared full Oracle catalog is not duplicated per batch.

Executed selection/source/tool commands (after the fresh pre-authoring Oviya catalog-coverage audit):

```powershell
python scripts/coverage-batch.py select --batch 2 --base 79d4b5cde31546cb29e6f529dd3a8faba5410d5d --report generated/alignment-batch2/selection-coverage.json --snapshot generated/oracle/oracle-cards-20261026210159.jsonl.gz
python scripts/coverage-batch.py sources --batch 2
python scripts/coverage-batch.py assay --batch 2 --snapshot generated/oracle/oracle-cards-20261026210159.jsonl.gz
```

The before command preceded authoring; final commands were:

```powershell
python scripts/phase29-verify.py before --output-base generated/alignment-batch2 --dist generated/alignment-batch2/before-dist --translation-review reference/alignment-batch1/coverage-status.json --snapshot generated/oracle/oracle-cards-20261026210159.jsonl.gz
.\dev.ps1 test --rerun
python scripts/coverage-batch.py status --batch 2
python scripts/phase29-verify.py after --output-base generated/alignment-batch2 --dist generated/alignment-batch2/after-dist --translation-review reference/alignment-batch2/coverage-status.json --snapshot generated/oracle/oracle-cards-20261026210159.jsonl.gz
python scripts/coverage-batch.py receipt --batch 2
```

The receipt consumes the full-run XML, not a focused rerun. Both Python scripts passed compilation. Existing scripts retain Batch 1/default behavior; additions are limited to a batch switch, output directory, frozen replay protection, shared hash-bound Assay cache and command measurements.

## Comparison and smallest next question

Batch 1 completed six of eight; Batch 2 completed twelve of twelve. Together these are eighteen completed definitions from twenty distinct investigations, not a representative catalog-wide success rate. Revealing Wind and Bitterthorn remain unresolved and unmodified. Their hidden-information and atomic creation/attachment questions were not reproduced by this sample.

Assay declines despite SDK representability recur strongly (all eight prior cards and ten current cards). Manual cost/sequence/binding work also recurs. This batch establishes no genuine new SDK or engine gap. Discovery remains a separate limitation, particularly conditional token gates and granted/continuous structures; native execution success does not make extraction complete.

The smallest evidence-backed follow-up question is whether one repeated Assay decline family can compile to the already reviewed native shape without losing conditions or source bindings. A narrowly sampled grammar investigation could measure that benefit before broader tooling work. Hour of Promise is also a concrete bounded conditional-discovery example for a separately authorized correction. Neither follow-up is implemented here.
