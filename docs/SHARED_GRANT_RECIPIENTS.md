# Shared grant recipients and native provider attribution

Activated and triggered grants use the same recipient interpretation. The native declaration retains the full group filter, including the grant scope, granter-relative controller clause and explicit self-exclusion. The application recognizes conjunctions of creature, permanent or artifact type constraints with fixed subtype and color predicates and a bounded controller clause. Unions, state predicates, stored choices and exclusions tied to a target or triggering entity remain unresolved. The receiving source's body scope retains its supported projected object constraints. Its player context still belongs to the activator or trigger controller.

For current observations, Argentum evaluates the filter against projected state. Activation requires the existing exact native supplier lookup and transaction evidence. Trigger availability additionally requires the native grant provider and resolved rule. Availability, event support, actual trigger placement and supplying declaration identity remain distinct.

For exact native soulbond scope, both members are candidates. Current availability requires the engine's reciprocal pair lookup, current battlefield references, projected creature types and a shared projected controller. This also rejects a stale component before state-based cleanup. Each current instance records the granter's partner as an exact ObjectRef, including when the granter receives its own grant. Independently captured activations and triggers continue using their native source references.

## Source controls

The [frozen cohort](../src/test/resources/regression/shared-grant-recipient-sources.json) binds Oracle text, printing identities, rulings and native definition hashes to merged application baseline `d8aafe4c588bc8cb36ff2b6fb2b4b5acc1c97d0c` and maintained engine `0878d800edbec56f0c82dac080ae4fec32816b5f`. It covers Gemhide Sliver, Manaweft Sliver, Unctus, Grand Metatect, Deadeye Navigator, Tandem Lookout and Synapse Sliver.

Gemhide's permanent subtype predicate applies across controllers. Manaweft requires a Sliver creature controlled by the granter's controller. Unctus uses a projected color condition and source exclusion. Its tap event remains outside the existing supported event subset. Deadeye and Tandem use the same native pair scope for different ability families. Synapse supplies an independent source-grounded group trigger with a supported combat damage event.

## Native provider boundary

`BattlefieldStaticsIndex` exposes the group provider and exact grant while computing eligible recipients. `TriggerAbilityResolver.getStaticGrantedFromProviders` returns only the resolved triggered rule, including native per-provider ID transformations for capped cases. The rule enters several matching paths as an ordinary `TriggeredAbility`. `TriggerDetector.assignGranterIds` runs later and uses the attachment lookup to stamp a raw granter ID. The pending and stacked rule then retain that stamp when present.

Attaching a provider after matching would need to guess which instance supplied an equal-ID rule, including conflicts with other ability pools, doubled triggers, and event lookback. Changing the rule ID or reusing `granterId` as a diagnostic stamp can affect native caps or resolution references. Exact group attribution would need the matched provider and its object generation carried from lookup through the existing detector paths. The application leaves exact stack attribution UNKNOWN when the native snapshot lacks this information.

## Catalog measurements

The compiled catalog census uses one shared production walk per card for each reporting family. It covers 21,699 physical cards and 22,487 authoring containers. The native declaration populations are identical on both application revisions.

| Relationship | Before | After |
| --- | ---: | ---: |
| Activated root declarations | 153 on 147 cards | 153 on 147 cards |
| Activated supported recipient bindings | 54 on 52 cards | 93 on 91 cards |
| Activated UNKNOWN recipient bindings | 99 on 95 cards | 60 on 56 cards |
| Triggered root declarations | 103 on 101 cards | 103 on 101 cards |
| Triggered supported recipient bindings | 73 on 71 cards | 90 on 88 cards |
| Triggered UNKNOWN recipient bindings | 30 on 30 cards | 13 on 13 cards |
| Triggered recipient plus event/controller support | 56 on 56 cards | 69 on 69 cards |
| Soulbond recipient declarations supported | 0 | 1 activated and 1 triggered |

Of the newly supported declarations, 32 activated and 11 triggered use a fixed subtype. Twenty activated and ten triggered use a relative controller restriction. One declaration in each family uses a color predicate, and two in each family use explicit source exclusion. These clauses overlap.

The principal remaining group boundaries are 39 activated and six triggered declarations with broader or correlated filters. Four triggered and 18 activated records retain unsupported enchant restrictions. Loyalty, nonbattlefield, unresolved definition faces and unsupported attachment scopes contribute overlapping activated UNKNOWN reasons. Fifteen nested activated declarations and nine nested triggered declarations remain outside the root walk. The census retains the previously exposed native paths for the latter group.

## Native scenarios

Seven focused native tests use frozen controls and real actions. Three additional tests check source hashes, shared classifier behavior and unsupported predicate shapes.

| Control | Observed behavior |
| --- | --- |
| Gemhide and Manaweft | An opponent's Sliver receives Gemhide's grant but fails Manaweft's control restriction. A native Amoeboid Changeling activation gives a non-Sliver the projected subtype and both providers then qualify it. The opponent activates Gemhide's mana grant and taps the receiving permanent. |
| Unctus | The source is excluded and an opponent's blue creature is ineligible. Native activation makes the granter's red creature blue and the grant becomes available until the turn ends. Recipient support remains independent of the tap event. |
| Deadeye Navigator | Real ETB and soulbond actions bind both members and no unrelated third creature. The partner's accepted activation retains its own source and the Navigator's provider. A blink renews the partner and the declined re-pair leaves both members without their former grant. A control change and loss of creature type separately end current eligibility. |
| Tandem Lookout | Two paired attackers each supply their own combat damage trigger and the recipient controller owns both. Native bounce of the partner ends the grant. The two existing mandatory draws still resolve. |
| Synapse Sliver | Two identical providers controlled by different players supply two triggers per qualifying recipient. Two Sliver attackers produce four triggers, the unrelated attacker produces none, and resolving the four optional draws benefits the receiving controller. Every tested stack object lacks a supplying ID and retains the correct receiving generation. |
| Synapse departure and renewal | With one provider, the pending draw resolves after that provider departs. Recasting the same card establishes a new provider generation and one new trigger on subsequent combat. Neither old nor current provider can be proven from the unstamped stack record. |

The scenarios assert 17 current MATCH bindings, counted by scenario, declaration and exact granter/recipient generations. Eight involve activated grants and nine involve triggered grants. Concrete activation attribution is demonstrated for two native transactions, Gemhide's mana activation and Deadeye's partner blink. The scenarios directly check two Tandem and six Synapse trigger placements for receiving source or controller. Neither group nor pair results add an exact supplying-provider stack observation. A repeated attack, placement or resolution is a scenario observation rather than a catalog coverage unit.

The native gap is in attribution. Synapse's tested counts, controller, optional choice and resolution outcomes are consistent with the frozen source. The engine remains at the tested stable revision. A future provider improvement would need an explicit provenance field independent of the native rule ID and the resolving effect's granter reference.

A bounded follow-up could investigate conditional static declarations. Fifteen activated and nine triggered native declarations are still outside root traversal, and their wrappers expose concrete conditions to study. Temporary grants add duration obligations and broader event composition deals with different checkpoints.

## Discover and verification

The comparison baseline is merged main `d8aafe4c588bc8cb36ff2b6fb2b4b5acc1c97d0c`. Its tree matches the previous verified delivery `a690646442184a2e2eb4d04984c3b799de7e934d` exactly. Both compiled baseline inventories were reproduced from the verified installed code. The final application `b84f9f764baaeb42173a8fd3221bc217b62c475b` reproduced both measured candidate catalog streams byte for byte. Engine `0878d800edbec56f0c82dac080ae4fec32816b5f` remained clean on app-stable throughout. The tests use JDK 25.0.4.1.

All 18 Discover pairs preserve every field outside the two nested grant lists, with identical effective populations across all six objectives. Existing native grant declarations, body declarations, parent action IDs and event-support fields also compare equal. Changed fields are recipient verdicts, kind and unresolved reasons, plus dependent object filters and restrictions in nested body facts, movement uncertainties, and event outcome confidence or source-creature evidence. No intrinsic admission changes.

| Commander | Eligible physical cards per objective | Widened activated / triggered bindings per report |
| --- | ---: | ---: |
| Judith, the Scourge Diva | 7,728 | 11 / 3 |
| Krenko, Mob Boss | 4,308 | 5 / 1 |
| Kenrith, the Returned King | 20,143 | 37 / 14 |

Verification uses the application wrappers and installed runtime.

- `scripts/bootstrap.ps1`, `scripts/test-bootstrap.py` and `scripts/test-oracle-snapshot.py` pass. The helper suites pass 17 and three tests.
- `dev.ps1 test --tests '*Grant*Test'` and `dev.ps1 test` pass on the clean application and engine pair. The complete application suite passes 898 tests across 124 classes.
- `bash scripts/verify.sh catalog` passes the current gate. The current gate reports the retained historical fingerprint difference separately. The installed CLI's audit diff reads the archived discovery snapshot with zero changes.
- Installed production discovery commands export all six objectives for each commander. `scripts/compare-shared-grant-recipients.py` compares all 18 pairs and verifies that shared grant records are independent of commander and objective.
- `pnpm build`, `pnpm test:ui` and `pnpm test:live` pass, including eight interface tests and 18 live Edge tests. The installed Explorer runs at 1 GiB, as confirmed by JVM flags. The live health endpoint returns both tested revisions.
- The live Kenrith token run exposes grant evidence for Gemhide, Manaweft, Deadeye, Tandem and Synapse. Its complete export confirms Unctus has pre-existing UNKNOWN effective deck eligibility and is excluded from mechanical input. Unctus's recipient behavior is covered instead by its native application scenario.
- The engine and SDK are unchanged. The native scenarios ran in the application suite. Independent full engine suites were outside this application-only verification.

Initial retries corrected a fixture import and argument type, followed by a clean rebuild after a compile overlapped a source edit. Intermediate CLI runs that overlapped installation were repeated on the final installed artifacts. The Explorer probe first assumed availability for all six cards, then verified the pre-existing eligibility boundary using the full export. The original source requirements, historical assurance and qualification records remain preserved.

[Verification receipt](evidence/shared-grant-recipients-verification.json) · [Catalog measurement](evidence/shared-grant-recipients-inventory.json) · [All 18 comparisons](evidence/shared-grant-recipients-discover.json)
