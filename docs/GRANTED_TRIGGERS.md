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

Final revision-bound results and catalog measurements are recorded with the delivery receipts. Bulk logs and paired reports remain in ignored `generated/granted-triggers/`; source requirements, diagnostics and compact evidence are retained in the repository. The engine remains unchanged from `0878d800edbec56f0c82dac080ae4fec32816b5f`.
