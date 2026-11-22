# Granted activated abilities: receiving sources and availability

This increment adds shared grant evidence without admitting granted operations as intrinsic Discover actions. The maintained engine remains unchanged at `0878d800edbec56f0c82dac080ae4fec32816b5f`; no upstream revisions, native production corrections, new objectives or admission policies were introduced.

## Source cohort and dependency

[Frozen requirements](../src/test/resources/regression/granted-ability-sources.json) retain exact printing and Oracle identities, retrieved text and rulings, native paths and canonical LF hashes, expected relationships and source uncertainties. These requirements were frozen before production interpretation changed.

| Control | Printing | Evidence scope |
| --- | --- | --- |
| Burning Anger | M15 133 | Aura host taps and deals damage using its own power; controller and last-known source distinctions |
| Presence of Gond | SHM 125 | Aura host activates; its activating controller receives the token |
| Viridian Longbow | MRD 270 | Actual equip and re-equip move grant availability; equipped creature deals damage |
| Cryptolith Rite | SOI 200 | Independent current creature recipients; activator-relative mana production without a stack object |
| Etrata, Deadly Fugitive | MKM 200 | Structural boundary only: face-down recipient filter remains UNKNOWN; nested collection lineage is retained |

The assignment began on PR #66's open head `1f58707a2e5e8bc650fec959df43dd51536e18f9`, with an isolated clean baseline checkout. The repository owner subsequently merged #66 as `384665fa2e6064109493b5d3a7a3bf586f273dd5`. Their Git trees are identical. The feature branch advanced to that already-merged main and this delivery targets main directly. No pull request was merged by this work. The baseline's installed reports retain the head revision actually used to build them.

## Shared interpretation

`ActionEvidence.grantedAbilities` connects an exact static declaration and native ability to an Aura host, bounded Equipment host, filter group, or unresolved recipient scope. It retains the original native costs, target requirements, activation zone, timing, restrictions and mana classification. Its `support` evaluates the recipient binding; body support, current availability, legal activation, actual outcome, source fidelity and qualification are separate claims.

`GrantedBody` reuses the existing effect/cost walker, mechanical facts, selected-target evidence, linked movements and action-local collection evidence. Its receiving source replaces `Self`; player-relative YOU/Controller references bind to the activating player. Printed characteristics of the granting card are never substituted for the receiving source. Native dynamic quantity expressions remain intact. The bounded body projection is identical across objective-specific extraction modes; complex counter/state projections and unsupported operations keep their declarations and uncertainty.

The nested action remains associated with the granting static action. Its facts are retained inside the grant record, outside the intrinsic action/fact/pathway inputs used for objective admission. This preserves useful token, damage, mana and movement evidence without silently changing the existing policy. Old reports decode the absent field as empty; new records round-trip through the current serializer.

Concrete availability observations require current granter and recipient `ObjectRef` generations, exact native definition/declaration binding, and Argentum's projected grant lookup. Aura observations also reuse the existing source-relative Aura attachment query. Equipment uses the native current attachment/grant binding and projected creature check; it does not introduce general equip planning. Plain creature groups and creatures-you-control groups reuse native filtering and current control. Recipient counts and activation legality are never inferred.

Accepted nonmana activations are checked against the new native stack object's captured recipient source, originating ObjectRef, granter ID, controller and native ability. Mana observations use native activation/resolution events and require an unambiguous exposed supplier without colliding own/copy/runtime/emblem pools. Argentum deduplicates equal ability IDs and exposes a selected supplier; an unexposed equal supplier stays UNKNOWN. The application does not reproduce lookup precedence or continuous-effect layers. Equal-ID root declarations on the same granter remain UNKNOWN because the native lookup does not expose a declaration index. The isolated control demonstrates a legal native activation without inventing that missing association.

## Native execution

Eleven real-card scenarios exercise the four principal controls; one isolated native scenario rejects a false declaration join. Burning Anger on an opponent-controlled creature taps that creature, leaves the Aura untapped, and attributes damage to the creature. A pump after activation changes damage from three to six. Pumping the creature before it dies to a responding Murder demonstrates native last-known power after both host and Aura leave.

Presence of Gond creates a token for the opponent controlling the host, despite our Aura control. Bouncing the Aura removes future availability while the already-activated ability remains on the stack and resolves. Native haste, summoning sickness and tapped-source rejections distinguish available abilities from legal activations. A native Vedalken Orrery permits responding with Control Magic: later control changes affect future activation availability, while the existing ability still creates for its original controller.

Viridian Longbow is cast and equipped through real native actions. One damage from a Typhoid Rats host destroys a larger creature through deathtouch, confirming the receiving damage source. Re-equipping removes the old host's grant. Bouncing and recasting a host renews its generation and does not restore the old attachment.

Cryptolith Rite grants separate activations to two controlled creatures, excludes an opponent's creature, taps each chosen host, and produces one red mana per activation with native recipient-source mana provenance and no response stack. Source control changes and creature control changes alter current recipients. Native animation and expiry make a land eligible and ineligible. Bounced/recast sources and recipients cannot reuse old generation bindings. Same-name sources preserve the native selected-supplier distinction.

These are bounded author/source-led execution observations. They do not independently certify whole cards, resolve unrelated concerns, or replace historical failures. Cryptolith Rite's generated native definition remains provisional. Etrata was not executed or qualified in this increment. Relevant official rules are retained in the frozen cohort: CR 113.7/113.7a, 302.6, 602.2, 605 and 608.2h.

## Catalog measurements

One streaming production extraction per physical card uses the ordinary 1 GiB heap. The population is 21,699 physical cards and 22,487 authored face containers, including aggregate authoring roots; containers are not printed-face counts. The compiled census distinguishes shared serial names by native declaration shape and scope, including the SDK's omitted-filter defaults.

| Compiled shape | Physical cards | Declarations |
| --- | ---: | ---: |
| Static activated grants, all nesting | 161 | 168 |
| Aura-relative activated grants | 43 | 44 |
| Equipment-relative activated grants | 21 | 23 |
| Filter-group activated grants | 90 | 93 |
| Other attachment / other static recipient scopes | 2 / 5 | 3 / 5 |
| Root static activated grants traversed | 147 | 153 |
| Nested static activated grants not traversed | 14 | 15 |
| Temporary single-target / group activated grants | 19 / 10 | 20 / 11 |
| Static / temporary triggered grants | 108 / 107 | 112 / 116 |
| Copy-exception-added activated / triggered abilities | 1 / 6 | 1 / 6 |
| Three explicit donor-derived activated-ability shapes | 17 | 17 |

Categories overlap and cannot be summed as distinct cards. Copy counts describe explicit native copy exceptions, not every copy-derived runtime ability. The compiled census also retains other static grants and state-triggered forms separately. The three donor shapes are `HasAllActivatedAbilitiesOfCards`, `GainActivatedAbilitiesOfPermanents` and `GainAllActivatedAbilitiesOf`; the separate `HasAbilitiesOfChosenLinkedExiledCard` at Koh, the Face Stealer, `script.staticAbilities[0]`, is also outside traversal and is not included in that 17.

The application adds 153 grant relationships on 147 cards: **54 supported recipient bindings on 52 cards**, and **99 UNKNOWN bindings on 95 cards**. Unknown reasons overlap: 77 broader group filters, 20 unsupported Aura enchant restrictions, 14 loyalty cases, three nonfront bindings, three nonbattlefield cases, two other scopes and two additional attached restrictions. Body interpretation retains **127 facts on 105 cards**: 63 other operations including mana declarations, 20 sacrifices, 18 damage operations, 16 movements, nine token operations and one toughness operation. These counts include partially understood grants and do not establish activation or objective relevance.

Twenty-one collection stages and twelve consumers on seven cards become inspectable inside granted bodies: Etrata, Gleam of Authority, Hard Cover, Kasmina, Mindwhip Sliver, Screaming Shield and Way of the Deathbringer. Five consumer bindings are structurally supported and seven remain UNKNOWN. This reuses the existing collection model; it does not qualify face-down or loyalty grants. Replacement-added abilities such as Estrid's Invocation remain untraversed.

## Verification

Verified implementation: `e840093620711ed7fa209fa05c4cae09cfbd6d46`, maintained engine `0878d800edbec56f0c82dac080ae4fec32816b5f`, engine implementation fingerprint `0b0439c946d9d3f1a60581a8250a298f37bea8ee465ca8e5c64c12b080045b9f`. The delivery commit adds only documentation and compact retained evidence after this tested implementation.

[Verification receipt](evidence/granted-abilities-verification.json) · [native observations](evidence/granted-abilities-native-observations.json) · [compiled census](evidence/granted-abilities-inventory.json) · [18 comparisons](evidence/granted-abilities-discover-comparison.json).

| Check | Observed result |
| --- | --- |
| Complete application Kotlin suite, JDK 25.0.4.1 | **863 passed**, zero failures, errors or skips; 120 classes |
| New bounded evidence | Nine structural tests; eleven real-card native scenarios and one isolated native ambiguity scenario |
| Bootstrap, installed distribution and CLI | Passed; 17 bootstrap guards and three Oracle snapshot helper tests passed |
| Current catalog gate | Passed; retained historical whole-report fingerprints differ and were preserved |
| Historical decoding | Three supported historical-wire tests pass; installed CLI identical-snapshot diff has zero changes; absent grant evidence defaults to empty |
| Frontend and browser | Production build, eight interface checks and **18 live Edge checks** pass |
| Normal Explorer | Exact clean application/engine SHAs confirmed by health; `MaxHeapSize=1073741824`; real API inspection exposes the four principal grants and Etrata's unresolved boundary |
| Controlled Discover | All **18 pairs** preserve every pre-existing field; only nested grant evidence is added. Each grant record is identical across objectives and commanders |

Judith's eligible physical population remains **7,728**, Krenko's **4,308**, and Kenrith's **20,143** for each objective. Result rows, verdicts, intrinsic actions/facts, chosen targets, attachments, collection lineage, linked movements, prerequisites, unsupported structures and historical assurance remain unchanged. Direct reanimation and intrinsic creature-input admission were not broadened.

The final 18 reports are also byte-identical to the pre-ambiguity-guard candidate's reports. The guarded catalog interpretation stream is byte-identical to the earlier stream, SHA-256 `04b7ec9f719c3f5db92c1a842bf489e5a21792a169985e42b1467f9e9c22cb80`. The added guard changes concrete observation queries, not extraction output.

The two-worker export wrapper initially failed while collecting completed jobs because `Receive-Job` promoted informational JVM stderr to a terminating PowerShell error. All reports were produced; the first sixteen command exits were checked as their loops advanced. Both workers' final commands were explicitly rerun with zero exits and identical output, then every report passed the strict comparison. This was a receipt-harness failure, not a native or application result discrepancy.

`GrantedAbilityInventory.java` performs one production extraction per integrated physical card and emits exact compiled scripts, face-container paths and grant records. `inventory-granted-abilities.py` distinguishes declaration categories and body evidence. `compare-granted-abilities.py` permits only the additive grant field and enforces objective-independent records. These diagnostics do not establish source fidelity or whole-card qualification.

Bulk logs, installed libraries, catalog JSONL and 36 Discover reports remain under ignored `generated/granted-abilities/`. The initial focused retries caught an unavailable fixture card, sorcery-speed casting with a nonempty stack, a raw rather than projected controller assertion, and the native same-ID supplier deduplication boundary. Fixtures/observations were corrected; no source expectation was weakened. Bootstrap retries corrected Windows sandbox, shell and JDK runner settings. Intermediate full suites passed 861 and 862 tests. The 862-test candidate also passed all 18 controlled comparisons, the catalog gate, all 18 live browser checks and the API cohort inspection. Final review then added the same-source declaration ambiguity control; the final receipt records the repeated exact-revision checks. Original logs are retained.

## Limits and next opportunity

Supported current recipients are bounded to front-definition battlefield creature attachments and plain creature / creatures-you-control groups. Compound/subtype/face-down filters, conditional/nested grants, loyalty, nonbattlefield recipients, temporary grants, donor/copy-derived abilities, recursive grants, granted triggers and full layer/ability-removal interactions remain unsupported or UNKNOWN. No dynamic-power evaluator, payment validator, activation legality solver or universal source tracker was added. Stored current instances and actual activations remain distinct from structural possibilities and outcomes.

Granted triggered abilities are the strongest next source-attribution candidate: 112 static declarations on 108 cards now have a useful comparison foundation, but require their own event/controller/source investigation. Broader recipient filters account for 77 unresolved bindings and may offer a smaller bounded increment. Temporary grants add duration obligations and should remain separate. Product Studio should choose the next scope; this work does not begin it.
