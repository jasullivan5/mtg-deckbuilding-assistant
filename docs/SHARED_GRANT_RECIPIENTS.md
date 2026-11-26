# Shared grant recipients and native provider attribution

Activated and triggered grants use the same recipient interpretation. The native declaration retains the full group filter, including the grant scope, granter-relative controller clause and explicit self-exclusion. The application recognizes conjunctions of creature, permanent or artifact type constraints with fixed subtype and color predicates and a bounded controller clause. Unions, state predicates, stored choices and exclusions tied to a target or triggering entity remain unresolved. The receiving source's body scope retains its supported projected object constraints. Its player context still belongs to the activator or trigger controller.

For current observations, Argentum evaluates the filter against projected state. Activation requires the existing exact native supplier lookup and transaction evidence. Trigger availability additionally requires the native grant provider and resolved rule. Availability, event support, actual trigger placement and supplying declaration identity remain distinct.

For exact native soulbond scope, both members are candidates. Current availability requires the engine's reciprocal pair lookup, current battlefield references, projected creature types and a shared projected controller. This also rejects a stale component before state-based cleanup. The pair is read from the supplied state only. Historical observations cannot recreate a pair from current membership. Independently captured activations and triggers continue using their native source references.

## Source controls

The [frozen cohort](../src/test/resources/regression/shared-grant-recipient-sources.json) binds Oracle text, printing identities, rulings and native definition hashes to merged application baseline `d8aafe4c588bc8cb36ff2b6fb2b4b5acc1c97d0c` and maintained engine `0878d800edbec56f0c82dac080ae4fec32816b5f`. It covers Gemhide Sliver, Manaweft Sliver, Unctus, Grand Metatect, Deadeye Navigator, Tandem Lookout and Synapse Sliver.

Gemhide's permanent subtype predicate applies across controllers. Manaweft requires a Sliver creature controlled by the granter's controller. Unctus uses a projected color condition and source exclusion. Its tap event remains outside the existing supported event subset. Deadeye and Tandem use the same native pair scope for different ability families. Synapse supplies an independent source-grounded group trigger with a supported combat damage event.

## Native provider boundary

`BattlefieldStaticsIndex` exposes the group provider and exact grant while computing eligible recipients. `TriggerAbilityResolver.getStaticGrantedFromProviders` returns only the resolved triggered rule, including native per-provider ID transformations for capped cases. The rule enters several matching paths as an ordinary `TriggeredAbility`. `TriggerDetector.assignGranterIds` runs later and uses the attachment lookup to stamp a raw granter ID. The pending and stacked rule then retain that stamp when present.

Attaching a provider after matching would need to guess which instance supplied an equal-ID rule, including conflicts with other ability pools, doubled triggers, and event lookback. Changing the rule ID or reusing `granterId` as a diagnostic stamp can affect native caps or resolution references. Exact group attribution would need the matched provider and its object generation carried from lookup through the existing detector paths. The application leaves exact stack attribution UNKNOWN when the native snapshot lacks this information.
