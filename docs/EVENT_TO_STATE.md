# Atomic objective relationship experiment

## Objective Semantics Foundations · Increment 1

This is a proposed research workstream pending Product Studio's milestone decision.

### Additional frozen expectations

| Case | Setup and executed action | State B before the action | Event A | State B at the checkpoint | Arrival supplies B |
| --- | --- | --- | --- | --- | --- |
| Animated land | Activate Raging Ravine's animation, resolve it, then sacrifice the land to Viscera Seer in the same turn | No | Yes | No | No |
| Prefilled graveyard | Start with one Wolfbat in our graveyard and sacrifice a different, opponent-owned Wolfbat we control | Yes | Yes | Yes | No |

The animated-land fixture uses the maintained fork's Raging Ravine definition (WWK/141). Wizards lists the card as a land with a temporary self-animation that retains the land type. Rules 700.4 and 603.10a explain the battlefield departure and the use of pre-departure characteristics. Under rule 400.7 the arrival is a new object, so the battlefield animation does not make the graveyard card a creature. In the prefilled control, rule 404.1 puts the sacrificed card into its owner's graveyard. [Wizards' card notes](https://magic.wizards.com/en/news/feature/edge-of-eternities-release-notes), [Comprehensive Rules](https://magic.wizards.com/en/rules).

## PR #30 baseline

## Frozen expectations

Every case begins with no creature cards in the observing player's graveyard. Read the state after the stack and pending decisions settle, with no intervening new action. The actual engine move determines the event. Count actual creature cards in the observing player's graveyard at that checkpoint.

| Case | Setup and native action | Expected transition of the tracked object | Event A | State B | A establishes B through the tracked move |
| --- | --- | --- | --- | --- | --- |
| Owned | Activate Viscera Seer by sacrificing an owned and controlled Wolfbat | Battlefield → own graveyard | Yes | Yes | Yes |
| Borrowed | Same cost with an opponent-owned Wolfbat under our control | Battlefield → owner's graveyard | Yes | No | No |
| Token | Same cost with a controlled creature token | Battlefield → own graveyard, then token ceases to exist | No | No | No |
| Finality | Same cost with an owned and controlled Wolfbat bearing a finality counter | Battlefield → exile instead of graveyard | No | No | No |
| Mill | Target ourselves with Tome Scour with Wolfbat on top of the library | Library → own graveyard | No | Yes | No |

A token can die without satisfying the nontoken criterion. Controller and owner are independent. Finality applies to the move even if the sacrifice cost was paid. Sources include the retained Seer and Wolfbat printings, [Wizards' rules glossary](https://magic.wizards.com/en/keyword-glossary), the [finality release notes](https://magic.wizards.com/en/news/feature/duskmourn-house-of-horror-release-notes) and the [Comprehensive Rules](https://magic.wizards.com/en/rules) (dies, zones, tokens and state-based actions).

## Try it in Explorer

From the application checkout run `.\scripts\explorer.ps1` and open <http://127.0.0.1:8787> when the launcher prints **Explorer ready**. Use **Research and learning → Open Contribution Model Study → Open event-to-state experiment**. The case picker reads a retained record of five executed scenarios. It can be used before any live search.

1. **Owned creature.** Follow Wolfbat from Seer’s sacrifice cost to your graveyard. Find the two independently satisfied criteria and check why the recorded arrival still supplies the state.
2. **Opponent-owned creature.** Compare the last-known controller with the owner and both players’ graveyards. Explain why A still holds and B does not.
3. **Finality counter.** Compare the requested and actual destination. Then try **Creature token**, which really dies but is outside A, and **Mill a creature card**, which establishes B independently.

**Rules-derived inputs.** The native examples use [Viscera Seer M11/120](https://scryfall.com/card/m11/120/viscera-seer) (`6179f847-e334-4f7f-9a4e-0013942a394f`), [Wolfbat TLA/122](https://scryfall.com/card/tla/122/wolfbat) (`2ae83b4e-e5a4-4c98-8d16-eef3c71b8ff2`) and [Tome Scour M10/76](https://scryfall.com/card/m10/76/tome-scour) (`fdbdf96b-e7c5-42e5-9a16-03daafde40af`). The front-face Oracle records for those printings were checked for this experiment. The fixture sets the finality counter directly, then tests its effect on the actual departure.

**Historical discovery.** [Explorer milestones](EXPLORER_MILESTONES.md) describe M3.1’s recognized Seer sacrifice and M3.2’s historical graveyard-active return coverage, including Wolfbat. Those records answer separate discovery questions and do not verify the controlled combinations here.

**New execution.** All five rows matched the frozen expectations. The retained [receipt](../web/src/event-to-state-receipt.json) contains actual destination, owner, last-known controller, battlefield creature and token status, both graveyards, the tracked card’s types and zone at the checkpoint, and the verdicts. The developer-authored test executed real Seer activations and a native Tome Scour cast. Borrowed control and token identity were fixture setup. The run does not independently qualify these cards or verify those setup mechanics in gameplay.

## Product Studio assessment

**Use independent event and state definitions for the next bounded trial.** The borrowed Wolfbat shows a true event with a false state. Milling shows the reverse. The token and finality examples separate an actual but nonqualifying death from a sacrifice paid without a death. In the owned case the arrived object is still current and is one of the creature cards supplying the state. This result depends on its actual destination and its type at the checkpoint. A later departure could invalidate B while the original A remains true.

This separation preserves direct responses to a death under their own trigger filters. A graveyard payoff can have separate ownership, type, timing or other access requirements. The experiment supports inspecting those dependencies explicitly. It does not establish admission to a search or contextual deck value.

| Evidence layer | Available information | Boundary for this relationship |
| --- | --- | --- |
| Native definition and static extraction | Sacrifice cost and its object filter, trigger filters, nominal movement zones, target and controller restrictions, and some replacement-related representation | Static extraction does not observe a particular object's departure, resolve its destination after replacement, or take a later snapshot. Existing discovery checks specific structural pathways. A missing emission is not a mechanical nonmatch. |
| Native execution | `ZoneChangeEvent` exposes requested and actual destinations, owner, last-known controller/types/token status and old/new object references. `GameState` exposes the player’s graveyard and current object reference. | The two predicates and their join live in a test. The sample demonstrates reuse of these native records and needs no new engine representation for these five cases. |
| Future query semantics | Evaluate event A from the actual departure and state B from the checkpoint, then test whether the recorded arrival is still current and supplies B. | A production query would need to distinguish which facts are known statically, which depend on execution, and when missing information leaves a relationship unresolved. There is no evidence here for catalog-wide handling of arbitrary effects. |

The join is justified when the question is whether a particular move established the state. For a standalone trigger or a standalone state check, the extra join adds complexity without answering a different question. Animation, competing replacement effects, and changes after the checkpoint remain outside the executed population. The next bounded experiment should animate a native noncreature card, sacrifice it, and verify that its battlefield creature status qualifies A while its graveyard card type fails B. This would test whether the same separation works when the types differ within a single move.

## Reproduce and verify

- Windows/JDK 21: `.\dev.ps1 test --tests '*EventToStateScenarioTest' --tests '*DiscoveryCoverageTest' --tests '*ReanimationIntegrationTest'`. Fifteen Kotlin tests passed, including one scenario test covering all five controlled cases. It writes `generated/event-to-state/observations.json` after the checks pass.
- Exact application revision for the retained execution: `9c8ced9167550322765dcf75f81b23434b9cf24d`. Resolved maintained Argentum `app-stable` revision: `03869c08665bf476a8b40dacbfbcd501c54a33bd`.
- The Windows launcher completed its TypeScript check, Vite production build and Kotlin distribution build. On Edge, `pnpm --dir web test:ui` passed 8 tests and `pnpm --dir web test:live` passed 13. The live suite covers all five cases, keyboard selection, a 390 px viewport, zero discovery requests from the panel, and existing discovery/research navigation.
- Browser-tested application code: `87e3a14d12d8f137070f279da31492f14484eb74`, with the test-only request assertion from `837a2adea419261b8191040104b2a2b83790080c` in the working tree during packaging. The health endpoint therefore reported `87e3a14d12d8f137070f279da31492f14484eb74+dirty`. It reported the same Argentum SHA as the scenario run.
