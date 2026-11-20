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

Original card identity does not preserve the departed permanent. Exile and return create successive object generations. Tests distinguish old source visits from renewed sources, stale exile objects from current membership, tokens from original nontoken cards, and owner from prior controller. Attachment, counter, marked-damage and temporary-effect setup is identified as fixture state; native spells perform the observed exile, bounce, flicker and return actions.

## Shared application interpretation

The existing matcher accepts one root, controller-decided resolution-time `MayDecide` wrapper around a singular ordinary source-linked exile. It references the walker's existing inner `.effect.then` movement fact and the separate return-action fact. The originating action retains its complete native gate and mandatory target requirement; relationship prerequisites explicitly distinguish announcement from resolution and require acceptance and successful movement. Alternate outcomes, other decision-makers, trigger-inline choices, source-presence gates and feasibility restrictions remain UNKNOWN. Wrapped legacy helpers are not added to the supported relationship subset: their source-instance semantics are unverified, and the existing walker retains their unsupported native structure.

Fiend Hunter can therefore retain supported `DEFERRED_SOURCE_LINKED` evidence without making exile mandatory. Savior remains UNKNOWN because its training event, union target and additional declarations exceed the isolated entry/return subset. Werefox retains typed implicit duration-return evidence and no invented continuation action, while its extra activated ability keeps support UNKNOWN within the existing isolated-duration subset. Mirror Golem remains a pile consumer, not a return relationship. No objective, direct-reanimation admission rule or intrinsic creature-input policy changes.

## Catalog census and unresolved scope

[Baseline inventory](evidence/legacy-exile-baseline-inventory.json) is generated from compiled SDK nodes, not card-name or Oracle-word searches. It contains 79 `ExileUntilLeaves` effect instances in 78 ability declarations, on 75 physical cards and 75 affected face containers. The full native population has 21,689 physical roots and 22,477 script authoring containers; the latter are not a count of printed faces. Back/flip aliases and meld results are excluded from physical roots. Saga chapter authoring paths are counted as declarations without inventing runtime action IDs.

Only three of those 75 legacy-helper cards belong to the six-card frozen source cohort. The remaining 72 physical cards are unreviewed; structural resemblance is not an Oracle classification or an execution result. The earlier 81 count describes its historical population and is not reused as a present census.

Reproduce the raw inventory by compiling [the Java driver](evidence/LegacyExileInventory.java) against the installed application libraries, running it with `-Xmx1g`, then passing its JSONL to `scripts/inventory-legacy-exile.py` with the frozen source file as the third argument. Exact native file, face, declaration and effect paths, serialized declaration hashes, units and review boundaries are retained.

Werefox's native `COMMON` rarity disagrees with the frozen printing's `RARE` metadata. This separate metadata concern is retained and is not changed by the mechanical correction. The cohort does not receive whole-card qualification, and extraction source fidelity remains UNKNOWN. Other complex helper uses, collections, multiple producers and source-linked permissions require individual source review.

## Verification and delivery

Current corrected verification is in progress. Final revision-bound counts, comparisons, runtime checks and coordinated PR links will be retained here before delivery.
