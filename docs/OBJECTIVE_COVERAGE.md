# Creature-death objective coverage

`own-nontoken-creature-deaths` retains the label **My nontoken creatures die** as a research goal. Its implemented search is narrower than general death synergy: it accepts selected death sources and death-related removal. The name alone does not promise all payoffs; About objective and search-scope details state the boundary explicitly.

## What the code accepts

Discovery.kt evaluates two separate branches over the existing PathwayExtractor:

1. Direct death generation: a supported action can affect a creature, can affect one you control, and can affect a nontoken object. The death-generation policy then admits sacrifice capabilities, explicit sacrifice-success/reflexive dependencies and intrinsic own-source death operations. It excludes ordinary removal merely aimed at your own creature and incidental opposing removal not enabled by your death; unresolved relationships stay UNKNOWN. A sacrifice need not be strategically beneficial to qualify.
2. Death-enhanced removal: a matching death can trigger, enable, strengthen or discount a recognized removal effect. The target must be creature-compatible; player-only damage is not creature removal. Recognized removal includes destruction, relevant graveyard movement, sacrifice, exile, potentially lethal damage and toughness reduction. Current supported dependency forms include direct death triggers, supported positive death conditions, conditional magnitudes and supported activation/self-cast discounts. Compound/negated/unresolved dependencies remain UNKNOWN.

For direct generation, creature/control/nontoken compatibility is existential structural possibility, not a legal game action or observed death. Pathway prerequisites explicitly assume legal resources, choices and applicable zones, with no replacement preventing the death. A creature dies only when it actually reaches the graveyard from the battlefield. For the removal branch, those three compatibility checks describe the preceding death event, not necessarily the object removed. A generic morbid condition may be supplied by your creature or somebody else's.

The historical two-branch design is explicit in the extractor, query and regression tests; it separates causing a death from using a death for removal. It was never a generalized payoff query. No assignment-time change to mechanical acceptance was warranted by the bounded cases below.

## Source-bound examples

These are fresh, bounded comparisons for this coverage investigation, not new card-level approvals or engine gameplay tests. Existing Centaur/Judith sources and seven exact native-printing Scryfall snapshots are retained under src/test/resources/cards. The new sources, retrieval URLs, printing/Oracle identities and raw hashes are listed in objective-coverage-sources.json; DeathObjectiveCoverageTest checks them. Native definitions remain unchanged on app-stable.

| Card | Source relationship | Observed death-objective behavior | Meaning |
| --- | --- | --- | --- |
| Accursed Centaur | On entry, sacrifice a creature | MATCH: direct sacrifice effect | Can cause your nontoken creature's death, conditional on the actual sacrifice reaching the graveyard. |
| Judith, the Scourge Diva | Your nontoken creature dies, then damage to any target | MATCH: death-triggered creature-compatible removal | A response to an earlier death; does not cause that first death. Player targets remain possible, but creature-removal potential is why this branch admits it. |
| Tragic Slip | A preceding creature death improves toughness reduction | MATCH: death-enhanced removal | The morbid death need not be yours. Your nontoken creature is one possible supplier; ordinary friendly-target removal checks remain separately excluded. |
| Grim Haruspex | Another nontoken creature you control dies, then draw | No death-objective result for the draw trigger | The native draw is present. Draw leaves are intentionally skipped by this death walker; there is no general payoff branch. Its separate Morph diagnostic is not a draw rejection. |
| Pitiless Plunderer | Another creature you control dies, then Treasure | No death-objective result | Token creation is represented as a fact, but this objective does not join it to the death trigger as a payoff. It can be absent from the findings index despite being eligible and authored. |
| Doomed Dissenter | Its own death creates a Zombie | No accepted death-objective relationship; MATCH under Create creature tokens | Shared token extraction works; the omitted death-payoff association is an objective/query gap, not proof the engine cannot create the token. |
| Blood Artist | A creature dies, then life loss and life gain | No MATCH; life-change instructions retained as unsupported for this analysis | Neither life changes nor damage to a player are treated as creature removal. The native behavior is present; unsupported discovery is not an observed engine failure. |
| Midnight Reaper | Your nontoken creature dies, then damage to you and draw | NO_MATCH for the player-damage check; draw not queried | The negative record concerns damage to a player, not whether the draw trigger benefits the deck. |
| Murder | Destroy a chosen creature | Mechanical compatibility MATCH; policy NO_MATCH | Choosing your own creature is possible, but ordinary friendly-target removal is deliberately excluded. It is not mechanically impossible. |

All examples were checked against the actual eligible Judith input. An absent result is different from an emitted NO_MATCH. General death-triggered draw, direct mana, token, life-change and other reward discovery is not implemented here; a card can still qualify through another supported cost or removal interaction. The existing Create creature tokens objective independently recognizes supported token effects; that does not imply the death objective searches for token payoffs.

Rules reference: the official Comprehensive Rules linked at https://magic.wizards.com/en/rules, effective 2026-09-25, were checked on 2026-11-02. Relevant boundaries include 700.4 (death), 701.21a (sacrifice), 603.2 (trigger events), 601.2c/602.2b (targets during activation) and 614 (replacement effects). These source observations do not replace scenario execution.

## Delivered corrections

Discovery panels now state the actual mechanism, the qualifying relationship and conditional death outcome, with the authored ability context and explicitly assumed gameplay conditions. Causes and responses are described separately. Unresolved land sacrifice remains unresolved. Player-only damage rejection takes precedence over a generic policy explanation; policy rejection is identified separately when the mechanical checks pass. No-result cards are described as unevaluated relationships rather than blanket negative card judgments.

Commander artwork is larger on desktop. Judith's connection no longer singles out sacrifice. Review metadata distinguishes fields the current card-review format does not capture from a check that failed or a comparison that was never registered. Raw decisions, snapshots, concerns, revisions and qualification remain intact.

## Smallest coherent follow-up requiring approval

Extend this same objective with an explicit **death-response/payoff contribution**, separate from direct death sources and removal. Use the existing native walker to correlate a supported battlefield-to-graveyard trigger's object/controller/token restrictions with the effect in that same action. A bounded first increment could cover card draw and token creation (including Treasure), using Grim Haruspex, Doomed Dissenter and Pitiless Plunderer as source-bound controls. Unsupported bindings, extra conditions and other payoff types must remain explicit rather than guessed.

This is consequential new discovery behavior and is not implemented in this PR. It would require reviewed expectations, correlated negative/unknown controls and deliberate pool/count comparisons. Life changes, direct mana and broader conditional rewards need separately defined coverage; there is no proposed universal benefit interpreter or strategic score. No engine correction is currently needed for the demonstrated representation/query gap.

## Sacrifice type boundary

Finalization investigation on 2026-11-02, following merged PR #22. The earlier death-payoff investigation above is retained; this section asks specifically about overlapping permanent types and renewable creature resources. No objective policy changed. A source-zone extraction correction described below affects the existing reanimation objective.

### Source and representation controls

Eleven exact native-printing Scryfall responses, retrieval dates/URLs, Oracle IDs and raw hashes are retained in `cards/sacrifice-boundary-sources.json`. Existing Centaur, Attrition, Agent, Acolyte and Judith snapshots supply the earlier controls. This is a bounded source/native comparison, not a registered whole-card rules review or engine scenario. Tests bind the new snapshots to actual selected native printing IDs.

Rules checked against the official [Comprehensive Rules](https://magic.wizards.com/en/rules), effective 2026-09-25: 205.2b/300.2 establish overlapping types; 205.1b addresses type changes; 700.4 and 701.21a distinguish actual death and sacrifice; 602.2b/601.2c retain legal activation targets; 302.6 constrains creature tap costs. An artifact creature is already both types. A land creature can also be both inherently: Dryad Arbor supplies a real printed control, but its green identity excludes it from Judith's deck. Animation is one possible setup, not the definition of all land/creature overlap. A token can be a creature too but does not satisfy the nontoken event.

All following sacrifice sources are authored and eligible under Judith. The actual SDK action, cost/effect, source/target/zone restrictions, facts and UNKNOWN/negative decisions remain retained.

| Control | Native representation and source expectation | Existing result | Finding |
| --- | --- | --- | --- |
| Accursed Centaur | Self enters trigger; required creature sacrifice; source is not excluded | MATCH | Mandatory on resolution, not an optional repeated outlet; the source can be chosen if suitable. |
| Viscera Seer | Creature sacrifice cost, no tap/mana cost, scry effect, source not excluded | MATCH | Optional activation; can repeat while source, suitable creatures and legal timing remain available. Consumes a creature and facilitates its death; does not observe deaths. |
| Blood Bairn | Creature sacrifice cost with excludeSelf; source receives stats | MATCH | Another creature is required; source exclusion remains explicit. |
| Attrition | Creature sacrifice and black mana cost with required nonblack creature target | MATCH for cost; separate excluded removal check with unresolved restriction | Preserve complete activation and legal-target prerequisite; the policy exclusion does not invalidate the cost. |
| Atog | Artifact sacrifice cost | MATCH | Artifact/creature compatibility is recognized; a nontoken artifact creature can pay without animation. Noncreature artifacts do not cause creature deaths. |
| Akki Scrapchomper | Artifact OR land sacrifice, mana and tap costs, draw effect | MATCH | OR is evaluated as alternatives: the artifact branch matches even though the land branch is UNKNOWN. This is not proof that every artifact or land is a creature, nor acceptance of a death-triggered draw payoff. |
| Agent of Shauku | Land sacrifice plus mana, required creature target and stats effect | UNKNOWN mechanically; sacrifice-cost policy MATCH | IsLand has no satisfiability rule here. Genuine possible overlap is not proven impossible or policy-rejected. No specific animation or eligible land-creature partner is established. |
| Claws of Gix | Permanent sacrifice plus mana; life gain | MATCH | The broad permanent filter permits an ordinary nontoken creature directly. Life gain is not the contribution being accepted. |
| Mogg Fanatic | Self-sacrifice cost of a printed creature, then targeted damage | MATCH for source cost | Source creature is consumed. Reusing that same source requires it to become available again; no free infinite repeatability is inferred. |
| Walking Corpse | Eligible nontoken creature with no scripted behavior | No emitted relationship | An ordinary body, not a distinctive death capability; absence is not a whole-card NO_MATCH verdict. |
| Judith / Grim Haruspex | Native death observers with damage / draw | Judith's removal response MATCH; Haruspex draw not queried | Observer relevance is real; present coverage is payoff-specific, not a strategic hierarchy. |
| Reassembling Skeleton | Graveyard-activated self move to battlefield tapped, mana cost | No death result; shared movement fact and separate reanimation MATCH | Distinctive renewable resource contribution is conceptually adopted, not yet a death-objective branch. |
| Bloodghast | Graveyard-active landfall trigger with optional self return | No death result; shared movement fact and separate reanimation MATCH | Landfall is a prerequisite; no current resource-role or recurring-fuel discovery is implied. |

Ornithopter is an authored Judith-eligible printed artifact creature and supplies a concrete inherent-overlap witness. Dryad Arbor is authored and inherently a land creature, but is not Judith-eligible. Neither witness is a gameplay execution or a new cross-card connection inferred by discovery.

### Why the classifications differ

`MechanicalQuery.cardPredicate` explicitly admits IsArtifact and IsPermanent as creature-compatible; IsLand remains UNKNOWN. Or combines branches existentially. The death policy then accepts sacrifice costs without scoring their payoff. Thus Akki and Atog qualify, Claws qualifies, and Agent remains unresolved. The extractor preserves their actual filters, costs and action associations. These sacrifice classifications are coherent as a bounded current capability; it is not a general satisfiability solver or a complete implementation of the adopted primary-discovery principle.

No reviewed sacrifice requirement justifies making IsLand impossible, blanket-promoting land sacrifice, suppressing inherent artifact overlap, or changing the eligible catalog. There is no engine-layer defect established by these cases. A focused presentation defect was found: statuses labeled every accepted sacrifice as creature sacrifice, concealing artifact/permanent restrictions. They now name the represented restriction. Akki/Atog explain the inherent artifact-creature choice; Agent's explanation separates the unresolved overlap from possible type-changing setup. Optional repeated activation remains conditional on source availability, costs, timing and limits.

### Primary inclusion decision still open

The adopted distinctive-contribution principle does not fully choose between these defensible boundaries:

- Preserve supported inherent type overlap in ordinary results: Akki/Atog remain included, Agent unresolved. A later source-bound witness approach could resolve appropriate land-creature cases, with Commander eligibility retained. This preserves more capabilities but needs a defined policy for limited resources and externally created overlaps.
- Prioritize explicit creature/broad-permanent capabilities, showing narrower type-dependent possibilities separately: Akki/Atog could move out of primary discovery while their mechanical MATCH remains visible. This is more selective but can hide useful printed artifact-creature sacrifice capabilities and requires an actual secondary presentation/policy contract.

No alternative was silently adopted. IsLand reasoning and primary inclusion are separate changes: making a possibility mechanically known must not automatically decide its primary relevance. Current M1 behavior stays visible and investigable; Product Studio should choose before any broader admission/filter change. No strategic usefulness threshold, whole-deck simulation, animation solver or role ontology was added. The [adopted objective direction v1](OBJECTIVE_MODEL.md) records resource replenishment and future grouping versus evidence-backed connecting without implementing them.

### Demonstrated extraction defect — graveyard-active observers

A new source-bound test initially failed for Bloodghast under the existing reanimation objective. Its authored trigger explicitly has activeZones=[Graveyard], observes another object's entry (binding ANY), and moves Self from Graveyard to Battlefield. The extractor nevertheless defaulted the source to Battlefield unless the trigger bound SELF to a zone-change event. The later explicit fromZone=Graveyard conflicted with that assumption, producing confidence NO_MATCH, a false reanimation rejection and a misleading source-must-remain-on-battlefield condition.

The generic correction uses an explicitly single active zone for a non-SELF-bound trigger. Existing SELF zone-change triggers retain their resulting-zone binding; multi-zone triggers keep their existing treatment pending a separate bounded analysis. Actual incompatible source/from-zone movement still rejects. Synthetic graveyard/exile/hand positives and a contradictory exile/graveyard negative exercise the boundary. This corrects existing represented movement, not death-objective resource admission. Skeleton already had a correct graveyard activation; Bloodghast's false rejection is the new correction. No engine code, native definition or eligibility metadata changed.
