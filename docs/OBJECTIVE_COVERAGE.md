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
