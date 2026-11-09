# Mechanical Verification Lab

From the application checkout, run `.\scripts\explorer.ps1` with JDK 21 and the clean maintained sibling on `app-stable`. Open [Explorer](http://127.0.0.1:8787) when the launcher prints **Explorer ready**.

## Three investigations

1. Choose **I have at least one creature card in my graveyard**, select **Find cards**, and filter for **Mogg Fanatic**. Open **Investigate execution** and run **Pay the sacrifice cost**. Compare the opponent's life during payment and after resolution. Run **Omit the required target** and check the rejection, the empty graveyard and the unchanged battlefield.
2. Open **Developer research index → Mechanical Verification Lab** and select **Evolving Wilds**. Run **Animate and sacrifice the land**. Compare the land's type in the zone-change event with its type at the final checkpoint. The checkpoint stops before the basic land search resolves.
3. Select **Spark Elemental** and run **Let the end-step trigger resolve**. Then run **Blink in response to the trigger**. Compare the zone movements and final battlefield. Expand the exact native evidence to compare the original object's generation, the source reference on the trigger, and the returned object's generation.

The result applies to the stated position. It can agree with the rules even when the selected objective does not occur. Discovery matches describe conditional potential, and the Lab carries no whole-card certification or independent qualification.

## Rules expectations

The retained [printing and rulings extracts](../src/main/resources/lab-sources.json) were captured on November 6, 2026, from the exact Scryfall printing and its `/rulings` endpoint. They retain English Oracle text, Oracle and printing IDs, dated rulings and the SHA-256 digests of the original responses. Spark Elemental and Evolving Wilds returned empty rulings lists. Mogg Fanatic's combat-damage timing ruling applies outside the tested position. Lifespark Spellbomb's August 1, 2008 ruling confirms that the tap restriction looks at time under control, including time before the permanent became a creature. Momentary Blink's return rulings inform the new-object expectation. [Comprehensive Rules effective November 6, 2026](https://media.wizards.com/2026/downloads/MagicCompRules%2020261106.txt)

| Position | Expected observation |
| --- | --- |
| Mogg Fanatic, TMP 190, targeting the opponent | CR 602.2b applies CR 601.2c and 601.2h in order. The legal target is announced before the sacrifice is paid. The owner receives the creature card while the ability is on the stack, and damage follows on resolution under CR 113.7a and 608.2. |
| Mogg Fanatic, missing target | CR 602.2 requires a legal activation, including all required target choices. This submitted action leaves the cost unpaid and the original object in play. |
| Evolving Wilds, ROE 228, with Lifespark Spellbomb, MRD 197 | CR 302.6 permits the tap cost because the land has been controlled since the turn began. The animation supplies a creature characteristic at departure. The creature dies under CR 700.4, and the new graveyard object has the land card's own type under CR 400.7 and 611.2. |
| Spark Elemental, 5DN 79, ordinary end step | CR 603.3 puts the trigger on the stack. When it resolves, the original creature is still available for the trigger's instruction and is sacrificed. |
| Spark Elemental with Momentary Blink, TSP 29 | CR 400.7 makes the returned permanent a new game object. The previous ability still exists on the stack under CR 113.7a and resolves without sacrificing that new object. |

Under CR 701.21a the controller sacrifices the permanent and its owner receives it. A replacement can redirect the move under CR 614.6, which can change whether the creature died or a creature card arrived. CR 111.6–7 distinguish a token from a card. These are owned nontoken positions without replacement effects. The historical borrowed, token and replacement runs and the historical Spark Elemental failure retain their original revisions. [Source-sacrifice investigation](SOURCE_SACRIFICE_DISCOVERY.md) · [Event-to-state evidence](EVENT_TO_STATE.md) · [Translation assurance](TRANSLATION_RELIABILITY.md)

## Runtime design

- `LabScenarios` registers the supported positions, source-reviewed expectations and scripts. `LabExecution` seeds a small two-player native state with `CardEntityFactory`, then submits every game action through `ActionProcessor`. The native engine handles costs, targets, priority, effects, triggers and zone transitions. These setups skip gameplay before the relevant interaction.
- `GET /api/v1/lab/scenarios` returns the curated descriptions, captured sources and imagery checked through the existing printing and face verifier. `POST /api/v1/lab/execute` accepts only `{scenarioId}`. It runs through the single existing research worker and the existing request and time limits. A run keeps all native actions, outcomes, events and states, and exposes concise state and event readings. These snapshots contain only the Lab's own curated position.
- Results carry their own application and engine revisions. The UI resets execution evidence on a scenario change, on returning to discovery, and on leaving the Lab. A linked finding is scoped to one discovery run. The presentation uses the native events and existing application event-to-state predicates. It does not evaluate target legality, timing, sacrifice, type changes or identity rules.
- Fresh completed, incomplete and unstarted execution have distinct states. An expected illegal action can agree with the rules while both measured objectives remain false. Unexpected rejection or a pending decision without a curated response stops the script and retains the evidence already collected. Timeouts and server errors use the existing HTTP error envelope. The Lab supports these curated positions only.

The smallest follow-up to consider after trying the Lab is a single Mogg Fanatic comparison with Rest in Peace. It would test whether the existing payment and destination displays make it clear that a sacrifice can be paid even when the creature goes to exile.
