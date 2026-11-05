# Contribution model validation

## Comparison protocol

The comparison unit is a single contribution to a stated objective requirement. Describe the capability, its causal connection, and its prerequisites before assigning labels. Keep objective admission and deck value separate.

Compare the two models against three checks:

1. Can the reader tell what changes, what starts the effect, and when any prerequisite or magnitude is evaluated?
2. Does an extra role explain an additional contribution, or repeat a dependency already required to describe the same mechanic?
3. Can the model preserve two causally linked roles, such as responding to a death and thereby providing a resource, without implying two independent actions?

For the four-role model, record separately which objects qualify, continuous quantity inputs, branches checked at resolution, and earlier-event history. A response means the event starts an ability. A spell that checks event history on resolution is tested against that boundary. In the five-role comparison, try Leverage State wherever a meaningful advantage varies with a threshold, count or established history. An ordinary recipient filter alone is insufficient. Record ambiguous role boundaries and keep the same mechanical facts for both interpretations.

Development set: Reassembling Skeleton, Viscera Seer, Pitiless Plunderer, Walking Corpse, Murder, Exploration, Lotus Cobra, Scute Swarm, Goblin Chieftain and Cranial Plating. Reserve **Goblin Piledriver** and **Tragic Slip** for evaluation against these criteria without changing them. These are purposive, known-card controls held out from rule development, with no claim of blinding or statistical generalization.

## Prototype Lab

Run `.scriptsexplorer.ps1` and open **Contribution Model Study**. The study needs no search.

1. Under **Creature deaths**, compare Skeleton, Seer, Plunderer, Walking Corpse and Murder. Explain the supply, consumption, payoff, ordinary material and deliberate policy exclusion. Use the existing Judith search and Discovery Coverage for live detail.
2. Open **Events versus states**. Compare Chieftain’s fixed recipient bonus, Plating’s continuously changing amount, and Swarm’s choice of token on resolution. Decide what the fifth role communicates beyond the state properties before opening the assessment.
3. Open **Held-out controls**. Explain Piledriver’s attack trigger and resolution count, then contrast it with Tragic Slip’s cast spell and earlier death history. Try **Exploration** under **Repeated land entry** if neither model’s explanation of permission feels sufficient.

## Assessment

**Recommend four roles with separate state-dependency semantics for the next Product Studio discussion.** Provide distinguishes an output such as mana or Treasure. Facilitate explains enabling a death or land play. Respond to Event identifies what starts an ability. Modify describes changing an existing characteristic, permission or effect. In this sample, Leverage State draws attention to an investment in board state, but it groups three different mechanisms: continuous quantity, a resolution-time quantity, and a branch selected at resolution. The added label does not replace any of those distinctions. Chieftain’s recipient scope still depends on the board, even though the per-creature bonus is fixed.

The two held-out controls fit the event/history distinction without changing the protocol. Piledriver also exposes a possible fidelity edge: native code subtracts one from the controlled attacking-Goblin count, which can undercount if Piledriver stops being a Goblin while attacking and the trigger still resolves. This is a source/code discrepancy hypothesis without an executed scenario. Tragic Slip only checks prior death history when the cast spell resolves. The objective recognizes death-enhanced removal, so an accepted relationship alone cannot identify a trigger.

Overlap helps when it preserves causality, as in Plunderer’s death → trigger → Treasure. It obscures the action when Facilitate and Modify both describe Exploration’s permission increase. Both models leave that boundary awkward. Both also need a distinctiveness rule to separate Skeleton from an ordinary creature body, and neither predicts Murder’s policy exclusion or contextual deck value. Keep those failures visible.

## What existing code can support

- The native action records and shared movement facts can anchor Skeleton’s graveyard return. The reanimation predicate can recognize that return. Sustained death fuel still needs recurring costs and a separately supported sacrifice route. The death query does not currently compose those relationships.
- Sacrifice and destroy are available in the death projection, which separates compatibility from objective admission. Tragic Slip’s positive history condition is supported in that bounded projection. The token query and reanimation query instead share fact predicates for controller, zone, type and constraints. Reuse the extracted action identity, branch and facts where available. Avoid using a matching effect from another ability on the same card.
- Plunderer and Swarm have native triggers with nested effects. Shared token facts describe outputs and can retain action/branch references. A payoff needs a join to the exact trigger with timing, object identity, controller and token restrictions intact. A copied Swarm can change later trigger capacity without changing the count made per resolution. Unrecognized predicates or lost effect context need an explicit unknown.
- Cobra’s mana output and Exploration’s land-play permission are native. The extractor skips standalone mana/draw leaves in the bounded death analysis and leaves most static effects uninterpreted. No general mana, land-entry or combat contribution query consumes these actions. The Chieftain and Plating printings had no authored definitions in the inspected baseline, so they are source-only cases. Piledriver is authored but raises a fidelity question separate from query coverage.

A subsequent bounded engineering experiment could correlate a native death trigger with output in the same ability for Plunderer, Doomed Dissenter and Grim Haruspex. Check controller and token filters, self versus another creature, unrelated abilities and unsupported branches against frozen source expectations. This would test causal linking without implementing a taxonomy. It remains a separate Product Studio decision.

## Evidence boundary

The source-reviewed sample has 12 physical card printings across three families, including two held-out controls. It is purposive. There is no estimate of catalog coverage, frequency or generality. The UI binds source names, printing and Oracle IDs, source URLs, front images and snapshot hashes in `web/src/contribution-sources.json`. Snapshots are under `src/test/resources/cards`. Six existing exact printings were rechecked against Scryfall for identity and unchanged Oracle wording, and six new exact printings were retained. The native review used clean app-stable `03869c08665bf476a8b40dacbfbcd501c54a33bd`. Official release notes corroborate [Plating](https://magic.wizards.com/en/news/feature/time-spiral-remastered-release-notes-2021-03-23), [Cobra and Swarm](https://magic.wizards.com/en/news/feature/zendikar-rising-release-notes-2020-09-10), and [Slip](https://www.magic.wizards.com/en/news/feature/eternal-masters-release-notes-2016-06-02).

Recorded death evidence comes from [objective coverage](OBJECTIVE_COVERAGE.md) and the [M3.1 live validation](EXPLORER.md), whose final coverage run used application `3187a1113bc0542359a44a53bf9e24337c6912d1` and the same engine. The stable Skeleton reanimation control comes from [M3.2](OBJECTIVE_COVERAGE.md#controlled-comparison-and-source-review), applications `45782ae4e537cdaab0a2e13de22308466409ecb1` and `877f9ee90fea7120f85096de583db7d898e6fd56`, same engine. The conceptual annotations are separate from those discoveries and establish neither gameplay execution nor independent qualification. The Integration Primer still uses the earlier cause/replenish/reward/body vocabulary. Alignment can follow a Product Studio decision.

## Verification

The clean Explorer build reported application `6b49b4eb1eb17693ae9922705ad50d3672e9c99d` and app-stable engine `03869c08665bf476a8b40dacbfbcd501c54a33bd`. Subsequent edits document the study and results.

- `.scriptsexplorer.ps1 -Port 8793` passed TypeScript/Vite, Kotlin installation and local startup.
- `.dev.ps1 test --tests '*DeathObjectiveCoverageTest' --tests '*DiscoveryCoverageTest' --tests '*ReanimationIntegrationTest' --tests '*TokenCreationIntegrationTest'` passed 34 focused Kotlin checks. These are existing discovery tests, including the Murder and Tragic Slip distinction.
- `pnpm --dir web test:ui` passed 8 synthetic interface checks, and `pnpm --dir web test:live` passed 10 live browser checks. Both ran with `EXPLORER_URL=http://127.0.0.1:8793` and `EXPLORER_BROWSER_CHANNEL=msedge`. The new check covered keyboard entry, both models at 390 px, held-out evidence, and zero discovery requests. Existing discovery, card inspection, M3.1, M3.2 and the primer passed.
- Inspected desktop and 390 px screenshots and confirmed that the retained Plating image decoded. Verified 12 tracked snapshot hashes and identities and resolved 10 native paths at the reviewed engine revision.

No native gameplay scenarios, whole-suite Kotlin runs or qualification reviews were performed for this increment.
