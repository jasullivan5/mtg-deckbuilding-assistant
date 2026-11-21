# Legacy exile-helper source fidelity

This bounded audit separates independent exile/return triggers, automatic duration returns, and the choice to perform an optional effect. It does not migrate the catalog or qualify whole cards.

## Frozen population and source requirements

The application baseline is `90a6e31fe5a4f1613bdadaed8b8c47c3687ed810`; native baseline is `23b91dfe270620a58dca1530dd5341951829b1c8`. These merged revisions have the same tracked implementation trees as the preceding PR #64/#10 deliveries. No newer upstream revisions were integrated. The stable sibling checkout was not advanced.

[Frozen sources](../src/test/resources/regression/legacy-exile-sources.json) retain six printing/Oracle identities, Oracle text, rulings, native paths and baseline LF hashes/scripts. Application commit `915278bad9097186bc12b1ecdb6bb482c39b8013` froze them before production edits. Official Comprehensive Rules sections 113.7a, 400.7, 601.2c, 603.3d, 603.5, 608.2b, 608.2d and 610.3 were checked against the retained November 6, 2026 rules text. The previous [linked-movement](LINKED_MOVEMENTS.md) and [timing-correction](EXILE_RETURN_TIMING.md) requirements and receipts remain historical and unchanged.

| Investigated printing | Source family and checkpoint | Bounded disposition |
| --- | --- | --- |
| Fiend Hunter, ISD #15 | Independent triggers; required other-creature target at announcement, optional exile at resolution | Ordinary source-linked exile inside the existing optional-effect wrapper |
| Savior of Ollenbock, VOW #34 | Independent training exile and departure return; up to one battlefield creature or graveyard creature card | Ordinary source-linked exile; retain optional target and native training |
| Werefox Bodyguard, WOE #39 | Optional non-Fox creature target; exile until this exact source leaves | Existing source-duration movement; remove the artificial departure trigger; retain sacrifice/life ability |
| Oblivion Ring, LRW #34 | Independent entry and departure triggers | Unchanged fixed control |
| Banisher Priest, M14 #7 | Source-presence establishment and automatic duration return | Unchanged fixed control |
| Mirror Golem, MRD #208 | Required graveyard target, optional resolution exile, linked-pile protection reader | Unchanged choice/zone/permission control; no invented return |

## Fresh unchanged-engine observations

Test-only native commit `45c511279fcafefd9b2bbc148a5776cc6598a967` leaves all production definitions identical to the baseline. Its real native actions produced six source-required failures and four passing ordinary controls, with no test errors. [Baseline receipt](evidence/legacy-exile-baseline.json) retains the failed assertions, test hashes and 17 native checkpoints: source visits, object generations, zones, stack entries, decisions, priority and transition events.

* Fiend Hunter accepted a targetless announcement, exiled without a resolution-time Yes/No decision, and skipped exile after departure and an empty return had already resolved. Its original ordinary exile/return and targetless-decline tests passed; the latter encoded the wrong checkpoint and is corrected rather than retained as a valid requirement.
* Savior skipped the late legal training exile from both battlefield and graveyard. Its ordinary battlefield return and genuine zero-target choice passed.
* Werefox's sacrifice cost left its victim in exile while an independent return trigger remained on the stack. A real Opt response could be announced before that return. The required automatic duration return had not occurred.

These are fresh observations, not reinterpretations of the prior Ring failure or Priest concern.

## Native corrections and identity

The three card definitions compose existing effects. Fiend Hunter uses a mandatory `OtherCreature` target and the ability builder's optional resolution wrapper around `ExileLinkedToSource`. Savior switches only its exile primitive to `ExileLinkedToSource`, retaining its optional cross-zone target, training and separate return. Werefox uses `MoveUntilSourceLeaves(..., EXILE)` and no independent return trigger. Its activated sacrifice/life ability remains separate.

No SDK types, generic executors, source-pile ledgers or return services are added or changed. In particular, the legacy helper's source-presence guard remains intact for unreviewed uses. The independent form uses `LinkedExileLookup` and the resolving source battlefield timestamp; the duration form uses native `ZoneReturnService` entries with exact source and moved `ObjectRef`s.

Original card identity does not preserve the departed permanent. Exile and return create successive object generations. Tests distinguish old source visits from renewed sources, stale exile objects from current membership, tokens from original nontoken cards, and owner from prior controller. Attachment, counter, marked-damage and temporary-effect setup is identified as fixture state; native spells and triggered/activated abilities perform the observed exile, bounce, flicker and return actions.

## Shared application interpretation

The existing matcher accepts one root, controller-decided resolution-time `MayDecide` wrapper around a singular ordinary source-linked exile. It references the walker's existing inner `.effect.then` movement fact and the separate return-action fact. The originating action retains its complete native gate and mandatory target requirement; relationship prerequisites explicitly distinguish announcement from resolution and require acceptance and successful movement. Alternate outcomes, other decision-makers, trigger-inline choices, source-presence gates and feasibility restrictions remain UNKNOWN. Wrapped legacy helpers are not added to the supported relationship subset: their source-instance semantics are unverified, and the existing walker retains their unsupported native structure.

Fiend Hunter can therefore retain supported `DEFERRED_SOURCE_LINKED` evidence without making exile mandatory. Savior remains UNKNOWN because its training event, union target and additional declarations exceed the isolated entry/return subset. Werefox retains typed implicit duration-return evidence and no invented continuation action, while its extra activated ability keeps support UNKNOWN within the existing isolated-duration subset. Mirror Golem remains a pile consumer, not a return relationship. No objective, direct-reanimation admission rule or intrinsic creature-input policy changes.

## Catalog census and unresolved scope

[Baseline inventory](evidence/legacy-exile-baseline-inventory.json) is generated from compiled SDK nodes, not card-name or Oracle-word searches. It contains 79 `ExileUntilLeaves` effect instances in 78 ability declarations, on 75 physical cards and 75 affected face containers. The full native population has 21,689 physical roots and 22,477 script authoring containers; the latter are not a count of printed faces. Back/flip aliases and meld results are excluded from physical roots. Saga chapter authoring paths are counted as declarations without inventing runtime action IDs.

Only three of those 75 legacy-helper cards belong to the six-card frozen source cohort. The remaining 72 physical cards are unreviewed; structural resemblance is not an Oracle classification or an execution result. The earlier 81 count describes its historical population and is not reused as a present census.

Reproduce the raw inventory by compiling [the Java driver](evidence/LegacyExileInventory.java) against the installed application libraries, running it with `-Xmx1g`, then passing its JSONL to `scripts/inventory-legacy-exile.py` with the frozen source file as the third argument. Exact native file, face, declaration and effect paths, serialized declaration hashes, units and review boundaries are retained.

Werefox's native `COMMON` rarity disagrees with the frozen printing's `RARE` metadata. This separate metadata concern is retained and is not changed by the mechanical correction. The cohort does not receive whole-card qualification, and extraction source fidelity remains UNKNOWN. Other complex helper uses, collections, multiple producers and source-linked permissions require individual source review.

## Verification and delivery

The tested application is `5a01cfbc633eb792213c6a8ad99d9ef8ebcec48a`, against clean engine `98492f6691401c14e1444c5a6ecb4fa3d305c730`. Later delivery commits retain evidence/documentation and trim trailing empty lines in the diagnostic driver and application linkage test. Runtime sources and test assertions are unchanged; the receipt records both LF hashes for the whitespace cleanup. Java/javac are Temurin 25.0.4.1. [Verification receipt](evidence/legacy-exile-verification.json) retains exact module counts, commands, hashes, retries, skips and unexecuted checks. [Current execution](evidence/legacy-exile-current-execution.json) retains all 51 passing cohort scenarios, source/test bindings, 17 native checkpoints and two application observations of the native pile. The additional Ossification and generic source/zone controls are included in the focused and full gates.

| Check | Result |
| --- | --- |
| Focused native / application linkage gates | 105 / 8 passed |
| Full SDK | 713 passed; one existing opt-in tail-report writer skipped |
| Full rules engine | 9,479 passed |
| All nine era scenario suites | 23,506 passed; no failures, errors or skips |
| Strict native catalog, snapshots and lint | 493 passed; snapshot update mode disabled |
| Mandatory native total | 34,191 passed; no failures or errors |
| AI diagnostic | One report test passed; Fiend 2/2, Savior 3/3 and Werefox 2/2 abilities read fully |
| Full application Kotlin suite | 816 passed; no failures, errors or skips |
| Bootstrap / Oracle snapshot / catalog comparator safeguards | 17 / 3 / 5 passed |
| Installed CLI and current catalog gate | Passed; zero confirmed benchmark mismatches, empty self-diff |
| Frontend / existing Edge interface / live Edge suite | Build passed / 8 passed / 18 passed |
| Normal Explorer | Exact pair confirmed by health; actual maximum heap 1,073,741,824 bytes |
| Live evidence | Cold/retained Progress, eight evidence controls and three rendered card inspections passed |
| Supported historical report | Typed decode and empty offline self-diff passed; current-catalog reprocessing correctly rejected by snapshot pin |

The [current inventory](evidence/legacy-exile-candidate-inventory.json) contains 76 legacy effects in 75 ability declarations, on 72 physical cards/faces. The native physical population remains 21,689. Source-linked ordinary movement declarations increase from 47 to 49 and source-duration declarations from 26 to 27. Exactly the three corrected cards leave the old-helper inventory; the remaining 72 cards are still unreviewed. Six source investigations, three corrected definitions, four demonstrated mechanical discrepancies and six failed baseline assertions are different units, not a catalog-wide fidelity score.

## Controlled Discover comparison

[All 18 comparisons](evidence/legacy-exile-comparison.json) use the same eligible physical populations and exact six objectives. Every MATCH pathway remains byte-equivalent after declared current-engine provenance normalization. Eligibility, historical assurance, source-bound self-return relationships, chosen-target/attachment/cost evidence and unrelated mechanical records remain unchanged.

| Commander | Eligible mechanical input cards | Objective pairs |
| --- | ---: | ---: |
| Judith, the Scourge Diva | 7,728 | 6 |
| Krenko, Mob Boss | 4,308 | 6 |
| Kenrith, the Returned King | 20,143 | 6 |

Only Kenrith's six reports include the three white corrected cards. Fiend gains the inner conditional ordinary movement fact; Savior gains two source-zone alternatives; Werefox loses the separately declared return fact/action and retains implicit duration evidence. This gives a net two additional movement facts. Werefox's generated native activated-ability ID changes from `:3` to `:2` after its spurious trigger is removed; the application action path remains `script.activatedAbilities[0]`.

For Kenrith reanimation specifically, movement facts change 7,368 → 7,370, rejected movement pathways 6,609 → 6,612, and UNKNOWN pathways 527 → 526. No MATCH is added or removed. The removed Werefox return declaration and newly explicit exile movements account for these changes; exile → battlefield is never admitted as direct graveyard → battlefield. The detailed receipt attributes every changed action, fact, unsupported structure and relationship to the corrected cohort.

The original whole-report benchmark fingerprints remain frozen and unequal, as before; the current catalog gate passes independently. The older archived `LandTappedForMana` SDK incompatibility remains outside this increment. Source requirements, past failures, concern history and qualification decisions are unchanged.

The first focused correction run found a test-setup NPE: a seeded Savior had no native battlefield-visit timestamp. The renewal control now casts the Savior through the engine; the identity assertions were preserved, and focused/full reruns pass. Workflow retries for native Git Bash/JDK selection and the Windows Assay launcher are recorded separately from rules discrepancies. Assay reads Werefox whole but declines portions of the independent-trigger cards; it remains a diagnostic, not a source or execution oracle. A paused additional UI inspection was initially not executed when automatic approval review hit a usage limit; the same action was later approved and passed. The native game-client GUI was not manually played; stack/priority/decision behavior was verified through real native test-driver actions, with application UI verification reported separately.

## Maintenance and next opportunity

The correction is suitable for an eventual upstream contribution: three canonical definitions and their regressions compose existing SDK/source-visit/duration facilities. No generic handler, SDK compatibility layer or extra ledger is maintained. No upstream revisions were integrated or upstream PR submitted, and neither review PR is automatically merged.

The most consequential remaining foundation work is another bounded, source-led audit of helper users with separate departure triggers, rather than treating the remaining 72 as verified. For a subsequent Card Integration increment, bounded collection-linked movement and union-target provenance now have a useful native anchor in Savior's tested graveyard/training cases. Granted-ability attribution remains a separate candidate; this audit supplies stronger immediate evidence for the collection/union direction. No follow-on assignment is started.

## Coordinated review

[Engine PR #11](https://github.com/jasullivan5/argentum-engine/pull/11) targets `app-stable`; [application PR #65](https://github.com/jasullivan5/mtg-deckbuilding-assistant/pull/65) targets `main`. Both remain open for review. The engine source and tested application revisions above are the compatibility pair; delivery-only documentation/evidence commits do not refresh qualification. Neither PR is automatically merged.
