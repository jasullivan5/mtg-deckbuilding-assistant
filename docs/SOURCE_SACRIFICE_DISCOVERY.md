# Discovery reliability · source-bound sacrifice

## GitHub verification

The [previous main run](https://github.com/jasullivan5/mtg-deckbuilding-assistant/actions/runs/42203112303) reached `scripts/setup.sh` through bootstrap. Java 21 was present, but `grep -q` could exit on the matching property before `tr` finished writing. With `pipefail` set, that upstream broken pipe failed the pipeline and the script reported an incorrect JDK error. A regression fixture emitting the Java 21 property before a long stream failed with the old check under Ubuntu WSL. The new check collects the full Java probe, requires a successful exit and matches the exact specification version. Focused WSL tests passed for verbose Java 21, a different version and a failed probe that prints the right version. PowerShell passed 14 existing guards, with the three Bash-specific cases skipped by design.

GitHub [Verify on the isolated repair](https://github.com/jasullivan5/mtg-deckbuilding-assistant/actions/runs/42233332757) passed both jobs at `6d46214715713d0a34c296e4419682bc47dfd2fa`. Backend bootstrap, the bootstrap regression suite, the two Python verification suites and `./dev test` all completed successfully. The workflow still uses JDK 21 and the exact resolved sibling `app-stable` checkout.

## Source review and scope

CR 701.21a has the controller sacrifice the battlefield permanent to its owner's graveyard. CR 700.4 defines the death transition. CR 400.7 distinguishes the arriving game object from the departing one, and 111.6–7 separate tokens from cards and remove tokens outside the battlefield. CR 602.2 and 601.2c/h require legal targets before activation costs are paid, and 302.6 governs tap costs on animated permanents. Replacement effects can redirect the actual movement under CR 614.6. The event uses the creature type and controller at departure. The state uses the physical card's owner and creature type in the graveyard at the checkpoint. An arrival establishes the state only if it was false immediately before and that arrival supplies a current witness. [Comprehensive Rules, November 6, 2026](https://media.wizards.com/2026/downloads/MagicCompRules%2020261106.txt)

The printing links identify the current English Oracle records checked against the native names, types, set and collector numbers. The applicable cards below are Commander-legal. The Judith inputs are red or colorless. Lifespark Spellbomb and Rest in Peace are external scenario fixtures, with green and white identities respectively.

| Role | Source and native route | Rules expectation |
| --- | --- | --- |
| Development | [Mogg Fanatic, TMP 190](https://scryfall.com/card/tmp/190/mogg-fanatic), `AbilityCost.SacrificeSelf` | Choose a legal target and sacrifice the exact permanent you control. The damage happens later. An owned nontoken creature arriving in your graveyard can satisfy both criteria. Another copy cannot pay. |
| Development | [Spark Elemental, 5DN 79](https://scryfall.com/card/5dn/79/spark-elemental), direct `SacrificeSelfEffect` on a battlefield end-step trigger | On resolution, the same still-controlled source sacrifices itself. The owner's physical creature card can witness the state if it reaches the graveyard and remains there. |
| Development negative | [Seal of Fire, NEM 98](https://scryfall.com/card/nem/98/seal-of-fire), `AbilityCost.SacrificeSelf` followed by two damage to a legal target | Its own enchantment card pays the cost. The normal departure is noncreature and the graveyard card is noncreature. Battlefield animation could change only the event. |
| Held out | [Reckless Barbarian, CLB 193](https://scryfall.com/card/clb/193/reckless-barbarian), self-sacrifice as an untargeted mana ability | The exact creature pays for two red mana. The mana ability resolves immediately. The owned physical creature card can arrive and supply the state. |
| Held out | [Ball Lightning, DRK 57](https://scryfall.com/card/drk/57/ball-lightning), direct end-step self-sacrifice | Its step trigger requires the sacrifice when it resolves with the same source still controlled. This uses the same arrival rule as Spark Elemental. |
| Held out | [Evolving Wilds, ROE 228](https://scryfall.com/card/roe/228/evolving-wilds), tap and sacrifice this land before the search | The land is a noncreature card in the graveyard, even if [Lifespark Spellbomb, MRD 197](https://scryfall.com/card/mrd/197/lifespark-spellbomb) made it a creature on the battlefield. The animated activation needs continuous control since the start of the turn for its tap cost. |
| Historical control | [Twinshot Sniper, NEO 168](https://scryfall.com/card/neo/168/twinshot-sniper), `AbilityCost.DiscardSelf` | Channel pays by discarding the exact artifact creature from its owner's hand after choosing a legal target. The hand departure can supply the state. A death requires a battlefield departure. |
| External replacement | [Rest in Peace, RTR 18](https://scryfall.com/card/rtr/18/rest-in-peace) | The sacrifice payment can occur while the move is redirected to exile. The original card supplies neither a death nor a graveyard witness in this case. |

## What changed

The SDK already represents the cost and the effect explicitly. Argentum's `CostHandler` and `SacrificeSelfExecutor` move the source through `ZoneTransitionService`. The application already emitted a sacrifice fact and a death pathway. It omitted the nominal destination and arrival type for the exact source, so graveyard-state discovery could evaluate fixed choices of creatures but left these instructions unresolved.

The existing walker now enriches that same fact, retaining its action and ability path, source binding, exact count of one, nominal owner destination and the card's graveyard creature type. The `SELF` binding and single-face type lookup are shared with source discard. Sacrifice retains the battlefield controller independently of ownership. The state query can use the arrival type while the legacy death path still evaluates the battlefield event and its existing policy. The older death-to-state side comparison keeps its fixed-choice scope. Multi-face arrival types and nested or delayed self-sacrifice effects remain unresolved. Pit Raptor's pay-or-sacrifice branch is a concrete example. Rules and engine evidence attach to individual observations and do not promote translation assurance.

## Controlled CLI comparison

Current main at investigation start was `85054b8e796e4dbb1a8c5293d235b3b061ac6980`. The fresh baseline build used the bootstrap-only commit `6d46214715713d0a34c296e4419682bc47dfd2fa`. The mechanics build was `fa9ba4030356f2eef214d92f4c41b903c196fc7e`. Both used the clean maintained `app-stable` revision `03869c08665bf476a8b40dacbfbcd501c54a33bd` and the same 6,111 eligible physical cards for Judith, with no conventional search filter. The authored catalog contained 17,017 physical cards and 17,373 registered names including faces.

| Graveyard-state measure | Baseline | Mechanics build |
| --- | ---: | ---: |
| Matching pathways | 442 | 535 |
| Distinct physical cards with a match | 420 | 508 |
| Unresolved pathways | 892 | 576 |
| Mechanical nonmatch pathways | 112 | 335 |
| Structural support records | 12,928 | 13,244 |
| Unsupported structure records | 8,537 | 8,537 |

Of the 316 previously unresolved pathways with a new decision, 311 were activation costs and five were direct step effects. The state query gained 93 matching pathways across 88 newly identified cards. Eighty-nine matches were activation costs, four were resolving effects. The 223 nonmatches record noncreature arrivals. No previous match was reclassified, and no facts or result rows were dropped. The cohort above provides source checks for the bounded mechanic. The totals are catalog-wide structural decisions rather than an independent Oracle audit of every affected card.

The death, reanimation and token reports were byte-identical. The state report preserved Twinshot Sniper, Viscera Seer, and the same input and eligibility records. The historical event-to-state and assurance material was retained. The built-in audit confirmed a comparable pool, 316 changed pathways, and 88 added accepted card classifications. Reproduce with `mtg-assistant discover --commander 'Judith, the Scourge Diva' --objective <id> --json --output <path>` and `mtg-assistant audit diff --before <before.json> --after <after.json> --json`. The state export digests are `e04c6412ab122314245d7b4b4ffe89b7cf4c821444e337b143bdb79c1aedcdce` and `5b2dd46830f863a59ce11266c0035be8e4e83f9919a0ffe4f571d83d4074f301`.

## Executed Argentum observations

| Fixture | Actual transition and criterion |
| --- | --- |
| Owned Mogg Fanatic, with another copy on the battlefield | Only the activating source moved as payment. The battlefield reference became a new graveyard reference. The nontoken death qualified and the arrival established the initially false state. Damage resolved separately. |
| Borrowed Mogg Fanatic | The last known controller was the activating player. The card moved to the opponent owner's graveyard. The death qualified and the activating player's state stayed false. |
| Mogg Fanatic fixture marked as a token copy | The token moved to the graveyard and ceased to exist. The nontoken event predicate and creature-card state were both false. |
| Rest in Peace present | The sacrifice occurred, with requested graveyard destination and actual exile. The death and graveyard-state predicates were both false. |
| Graveyard already has an owned creature card | The new arrival supplies a second witness, while the state was already true. |
| Mogg Fanatic has no target | The activation was rejected. The same battlefield reference survived and there was no sacrifice event. |
| Evolving Wilds animated by Lifespark Spellbomb | The land paid its own tap and sacrifice cost. The creature at departure qualified for the death event. Its arriving object was a land card and supplied no creature witness. |
| Spark Elemental remains in play until its end-step trigger resolves | It stayed on the battlefield until resolution. The exact source moved, generating a qualifying death and a new creature-card witness. |

All eight scenarios passed on Windows, as did the four earlier exact-discard scenarios and the focused graveyard discovery suite. The production TypeScript/Vite build passed. Edge passed eight interface and 15 live tests, including the revised normal inspector at desktop and 390-pixel viewport widths. Explorer review and engine badges continue to use the existing formal evidence ledger. The new execution observations are recorded here separately.

### Confirmed native limitation · trigger after a zone round trip

On the same stable engine, start with one Spark Elemental and reach the end step with its trigger pending. Cast [Momentary Blink, TSP 29](https://scryfall.com/card/tsp/29/momentary-blink) on it in response, paying `{1}{W}` in that step. Resolve Blink first. Its exile and return produced the same entity ID with a different object generation, 95 before Blink and 100 after. The old end-step trigger should leave generation 100 on the battlefield under CR 400.7. The observed engine instead emitted a sacrifice move from generation 100 to graveyard generation 102. `EffectContext` tracks whether a source reference has been lost, while `SacrificeSelfExecutor` checks battlefield membership using the entity ID without that generation guard. [Pinned executor](https://github.com/jasullivan5/argentum-engine/blob/03869c08665bf476a8b40dacbfbcd501c54a33bd/rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/effects/zones/SacrificeSelfExecutor.kt)

This is an engine execution defect in the adverse branch. The ordinary same-object scenario remains a valid conditional route, and the inspector requires that same permanent. A separate engine regression should assert that the new object remains on the battlefield after the old trigger resolves. The maintained engine source and integration revision were used as-is.

## Explorer exercises

Run `.\scripts\explorer.ps1` from the application and open `http://127.0.0.1:8787` after **Explorer ready**. Choose Judith and **I have at least one creature card in my graveyard**.

1. Find **Mogg Fanatic** under **Sacrifice**. Read the activation, ownership and destination requirements. Open **Discovery** for the death comparison, then **Technical investigation** for the native movement fact.
2. Inspect **Spark Elemental** for the same arrival through a resolving trigger. Under **All findings**, compare **Seal of Fire**, whose sacrifice moves a noncreature card.
3. Open **Needs interpretation** and find **Pit Raptor**. Its nested upkeep payment branch remains unresolved in this objective.
