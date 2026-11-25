# Granted triggered abilities

This increment interprets root static `GrantTriggeredAbility` declarations separately from a card's intrinsic triggers and from the existing granted-activated evidence. It does not add a Discover policy or a trigger engine. Argentum still determines availability, detection, target announcement, choices and resolution.

## Source requirements and native architecture

[Frozen requirements](../src/test/resources/regression/granted-trigger-sources.json) retain printing and Oracle identities, retrieved text and rulings, native paths and source hashes for Sixth Sense, Combat Research, Custody Battle, Dire Flail // Dire Blunderbuss, Tandem Lookout and the intrinsic-trigger negative Stab Wound. These requirements were frozen before production interpretation changed. The [November 6, 2026 Comprehensive Rules](https://media.wizards.com/2026/downloads/MagicCompRules%2020261106.txt), particularly 113.7a, 400.7 and 603, distinguish a receiving source, its controller when it triggers, and a later independently resolving ability. No whole-card qualification or historical source-fidelity status is refreshed.

The native `TriggerAbilityResolver` obtains attachment and battlefield-static grants. `TriggerDetector` captures the receiving source, controller, origin reference and event context; attachment grants also receive native `granterId` stamping. `PendingTrigger.rulesText` carries the declaration into `TriggeredAbilityOnStackComponent.resolvingTriggeredAbility`. The application reads that snapshot rather than looking up an ability by description. Native attachment-provider ordering is not reproduced.

## Shared relationship and observations

`ActionEvidence.grantedTriggers` is a parallel typed field; `grantedAbilities` keeps its existing meaning. Each relationship retains the physical card, exact face and static index/path, complete native declaration, recipient support, event support, checkpoint distinctions, persistence limits and source-fidelity UNKNOWN. Ordinary creature Auras, Equipment hosts and plain battlefield creature groups reuse existing recipient infrastructure. Soulbond, broader predicates and nested conditional grants remain bounded or UNKNOWN.

The existing walker interprets each nested body in a fresh receiving-source context. `Self` means the recipient; `GrantingSource` means the distinct provider; `YOU` and `Controller` mean the captured trigger controller. Existing action, target, fact, event-outcome and collection structures remain nested beneath the grant. They never enter intrinsic Discover actions, facts, pathways or outcomes. Optional draw is a resolution gate; native trigger restrictions and intervening-if conditions retain their separate checkpoints. Reflexive continuations retain exact native structure and explicit uncertainty rather than becoming a composed trigger.

Current availability requires supplied current ObjectRefs, battlefield presence, the exact native face/static declaration and native resolver/predicate evidence. Aura bindings additionally reuse attachment observations. An available grant does not establish an event, trigger, target, accepted choice or outcome.

Stack observation requires an explicitly supplied new native stack ObjectRef and its actual `AbilityTriggeredEvent`, captured full rule, source origin, source controller, supplying granter and a unique matching event context. Damage context distinguishes the damaged event subject from the receiving damage source. Native attack events are batches: the captured SELF source belongs to the actual attacker list, and the context records the attacking player; a singular triggering entity is not invented. Available visit timestamps are compared; absent timestamps remain absent, with generation identity supplied by the captured origin ObjectRef. Targets and last-known source snapshots are retained where native records expose them.

Persistence rechecks the established stack object and its captured source, rules, granter, controller and context. It deliberately does not require a still-current grant or recipient. Disappearance of a stack object alone is not proof of successful resolution. Actual outcomes are verified separately through native scenarios.

## Ambiguity and boundaries

Two same-name attachment granters can supply equal-ID triggers: observations read the engine's actual granter stamp for each trigger. Multiple recipients remain distinct sources. Equal-ID declarations on one granter, including unsupported static wrappers, lack an exposed declaration index and remain UNKNOWN. Intrinsic, copied or runtime trigger-ID collisions likewise remain UNKNOWN. No description matching, stamped-ID parsing or application declaration-order rule is used.

Group availability can be exposed by the native index while a resulting trigger lacks an exact supplying-granter stamp. Such a stack observation remains UNKNOWN; structural group support does not promise exact runtime provenance. A granter that changes generation within the event/placement interval cannot be attributed because no captured granter generation is exposed. Event slices with no unique cause also remain UNKNOWN.

Temporary grants, copy/replacement-added triggers, recursive grants, general soulbond, broad layer interactions and comprehensive reflexive composition are excluded. Native dynamic quantities and unsupported operation bodies are retained. Tandem Lookout's pair membership and Custody Battle's unsupported body interpretation do not erase their native declarations. Stab Wound continues to own its Aura trigger and is never relabeled a creature-source grant.

## Verification and measurements

Tested application: `97a498f5cad6448491197830cb4c18ba826c17d6`. Unchanged comparison baseline: merged main `c4350e5894d1518460b79fb86a728c65586ddbb1`. Maintained engine: `0878d800edbec56f0c82dac080ae4fec32816b5f`, clean on `app-stable`; implementation fingerprint `0b0439c946d9d3f1a60581a8250a298f37bea8ee465ca8e5c64c12b080045b9f`. No upstream revisions or engine production changes were incorporated. Delivery adds documentation and compact receipts after this tested implementation.

[Verification receipt](evidence/granted-triggers-verification.json) · [native observations](evidence/granted-triggers-native-observations.json) · [compiled census](evidence/granted-triggers-inventory.json) · [18 comparisons](evidence/granted-triggers-discover-comparison.json).

| Check | Result |
| --- | --- |
| Full application Kotlin suite, JDK 25.0.4.1 | **888 passed**, 122 classes, zero failures, errors or skips |
| New bounded tests | Eleven structural tests; fourteen native scenario tests, comprising eleven real-card tests and three isolated native controls |
| Bootstrap and installed distribution | Passed; seventeen setup guards and three Oracle snapshot helper tests also pass |
| Installed CLI and current catalog gate | Passed, zero confirmed benchmark mismatches; historical whole-report fingerprints remain different and were preserved |
| Supported historical decoding | Three historical-wire tests pass; installed CLI identical archived-snapshot diff has zero changes; absent trigger grants default to empty |
| Controlled Discover | **18 pairs pass**; every pre-existing field is unchanged, including granted-activated evidence and historical assurance |
| Frontend and browser | Production build, eight interface checks and **18 live Edge checks** pass |
| Normal Explorer | Startup script passed; health identifies the exact application/engine above; `MaxHeapSize=1073741824`; six-card live API inspection passes |

Judith's eligible population remains **7,728**, Krenko's **4,308**, and Kenrith's **20,143** physical cards for every objective. Their reports add respectively 32, 16 and 93 trigger-grant declarations. Each declaration is identical across objectives and commanders. Result rows, admissions/verdicts, intrinsic actions/facts, chosen targets, attachments, collection records, linked movements, prerequisites, unsupported structures and assurance are unchanged. No nested granted operation becomes an intrinsic contribution.

Baseline reports use freshly installed merged main on the same engine. Final comparisons invoke the installed production `discoveryCommand`, reusing one catalog per commander; the catalog gate separately exercises fresh CLI launches. No comparison normalizes away an unexplained mechanical difference. `compare-granted-triggers.py` permits only the additive nested field and verifies parent/action/fact references and objective-independent records.

### Actual native scenarios

The four principal grant definitions execute through real casts, native craft/equip, combat, upkeep, stack resolution and decisions. Stab Wound is an executed intrinsic negative. Tandem Lookout remains a structural UNKNOWN boundary; its soulbond behavior is not claimed as verified here.

| Control | Observed bounded behavior |
| --- | --- |
| Sixth Sense | Our Aura on an opponent's creature creates that creature's combat-damage trigger, controlled by the opponent. The damage recipient is a different event subject. Accepting/declining the resolution-time choice changes the correct player's hand by one/zero. Native Burning Anger noncombat damage does not trigger it. |
| Combat Research | Only the receiving creature triggers and draws. Another same-name creature does not. The granted declaration remains distinct from the Aura's two independent static declarations. |
| Custody Battle | The receiving controller's upkeep triggers it. That player chooses a land payment; without a remaining land, native control transfer occurs. The next qualifying upkeep and captured controller follow the new controller. The shared body retains the resolution-cost land sacrifice while the complete payment/control continuation remains unsupported. |
| Dire Blunderbuss | Native craft establishes the current back-face Equipment, actual equip changes hosts, and only the current host receives the grant. Its attack creates a host-source trigger stamped with the distinct Equipment ID. The reflexive continuation remains explicitly bounded; no complete sacrifice/damage composition is claimed. |
| Grant and recipient renewal | Bouncing the Aura removes future triggers without erasing its established trigger. Bouncing/recasting the receiving card renews its ObjectRef while the old trigger persists. Recasting the same Aura on another host creates a new granter instance that cannot supply the earlier trigger. |
| Control after detection | Responding with native Control Magic changes current recipient control, while the existing trigger still offers its draw to the original captured controller. |
| Multiple sources and ambiguity | Two identical Auras and two same-name recipients produce three correctly separated native triggers. Wrong-recipient stack associations, absent/duplicate causes and a substituted damage-source generation are rejected. Equal-ID declarations on one granter and intrinsic/granted collisions remain UNKNOWN. |
| Group-provider boundary | An isolated native plain-creature group grant supplies current availability and excludes an opponent's creature. Its actual trigger lacks a supplying-granter stamp, so exact declaration observation remains UNKNOWN even with one visible provider. |
| Stab Wound | The Aura owns and controls its upkeep trigger; the attached creature controller loses life. The creature is not assigned a granted trigger. |

The compact receipt retains fifteen native stack-placement observations: fourteen MATCH source/event/stack associations and one deliberate UNKNOWN group-supplier observation. These are observations, not distinct cards or complete resolutions. Later state changes, choices and outcomes are separately asserted by the revision-bound tests. The engine and SDK are unchanged; no independent full native engine/era rerun is claimed.

### Compiled catalog

One production extraction per physical card inspects **21,699 physical cards and 22,487 face/authoring containers**. `GrantedTriggerInventory.java` emits exact compiled scripts and interpreted records; `inventory-granted-triggers.py` counts native declaration shapes and paths rather than Oracle keywords.

| Unit | Physical cards | Declarations/relationships |
| --- | ---: | ---: |
| Static triggered grants, all nesting | 108 | 112 |
| Aura-relative / Equipment-relative | 39 / 29 | 41 / 30 |
| Battlefield filter groups | 31 | 31 |
| Other attached / other scopes | 3 / 7 | 3 / 7 |
| Root declarations newly retained and traversed | 101 | 103 |
| Nested declarations still untraversed | 8 | 9 |
| Bounded recipient bindings | 71 | 73 |
| Bounded recipient plus event/controller relationships | 56 | 56 |
| UNKNOWN recipient bindings | 30 | 30 |
| UNKNOWN event/controller bindings | 25 | 26 |
| Temporary triggered-grant effects, outside this subset | 107 | 116 |

Counts overlap and cannot be summed as distinct cards. The 56 combined structural relationships comprise 28 Aura, 24 Equipment and four group declarations. They do not establish current recipients, actual detection or exact runtime supplier attribution. Forty-seven relationships on 46 cards lack at least one of the bounded recipient/event bindings; one physical card also has a supported declaration. Recipient uncertainties comprise 22 broader group/pairing filters, four unsupported enchant restrictions and four other scopes. Event uncertainties include nine spell-cast and six zone-change declarations, alongside block, counter, tap/untap, life, targeting and mana-event shapes.

Nested interpretation adds **76 facts and 54 event/outcome records across 66 physical cards**: fact units include 22 draw, fifteen token, fourteen sacrifice, fifteen movement, four damage and six destroy operations. These include conditional or partially understood bodies. Draw facts and draw outcomes are different records for the same operation, not additional executions.

Existing collection interpretation now exposes **24 stages and 22 consumers across thirteen cards**. Eight stage bindings and eight consumer bindings are structurally supported; sixteen stages and fourteen consumers remain UNKNOWN. The cards are Animal Friend, Dire Flail, Dorothea, Vengeful Victim, Invocation of Saint Traft, Nadu, Winged Wisdom, Nettlevine Blight, Ninja's Blades, Pirate Hat, Spare Dagger, Summoner's Grimoire, Sunfire Torch, Trickster's Talisman and Unctus, Grand Metatect. A retained indexed consumer need not have a supported producer: Dire's native target-selection pipeline is preserved without inventing a collection-producing stage.

All nine untraversed conditional paths, including both Helm of the Ghastlord declarations, are retained in the census receipt with exact face and native file paths. They are unsupported structures in present definitions, not missing cards. Temporary, copy/replacement-added and recursive abilities remain outside this static-root increment.

Bulk logs, native catalog JSONL, installed libraries and paired reports remain in ignored `generated/granted-triggers/`. Initial retries corrected a fixture helper, runner Git configuration, observation assumptions about missing fixture timestamps and native attack batches, JSON-default assertions, and the absent default Chromium binary. An initial historical CLI call used the fixture wrapper rather than its embedded snapshot; the corrected call passes. An intermediate full run was deliberately stopped after final identity guards were added; the complete final run above supersedes it. Frozen Oracle requirements, native source expectations and historical fingerprints were not rewritten.

## Remaining source fidelity and next direction

The source expectations demonstrated here are narrow: the four granted relationships, their tested source/controller/event bindings, and the specified persistence/negative cases. Full Combat Research static interactions, Custody Battle's complete structural choice interpretation, Dire's reflexive continuation and Tandem's pairing remain outside this bounded verification. Stack persistence proves retained obligations and provenance; it does not certify generation-safe resolution of every later granting-source reference after provider renewal. Missing native declaration indices, granter-generation snapshots and group supplier stamps remain consequential provenance limits, not invented application bindings or newly confirmed Oracle contradictions.

The strongest next bounded direction is broader recipient and native provider attribution: 22 group/pairing bindings and the demonstrated group-supplier gap are concrete obstacles. Temporary grants add separate duration obligations; broader event-to-operation and reflexive composition also warrant distinct source-grounded increments. Product Studio should choose that scope. This delivery begins none of them and adds no native fork divergence.
