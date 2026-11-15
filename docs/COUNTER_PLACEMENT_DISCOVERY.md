# Counter placement and modification in Discover

## Implementation

**Put +1/+1 counters on creatures I control** (`place-plus-one-counters`) queries two independent records from the existing native walker. A placement retains the issuing action and leaf path, counter type, amount expression, affected object or target group, choice and binding, branch prerequisites and entry or resolution checkpoint. A modifier retains its native declaration, the impending-operation pattern, active zones, placing-player restrictions and signed change. It requires a separate positive placement. Both records retain the typed native node and link to the existing ability/action evidence. The shared predicates can ask about other counter types and recipients and can test an isolated replacement against a concrete count and source zone.

The objective intersects the recipient with a creature we control. An unrestricted target can qualify when one of our creatures is a legal choice. The objective places no requirement on who places the counters. A different counter type or provably incompatible recipient fails the intersection. An unresolved amount or binding remains UNKNOWN when the other checks do not rule it out. Card type alone supplies no operation. A signed decrease can qualify mechanically and the system does not evaluate its desirability. State changes that may follow placement, responses to completed placement and continuous stat changes are separate questions. One record's eligibility or admission says nothing about whether it can be combined with another card in a particular position.

Argentum models an impending replacement opportunity with `CounterPlacementEvent`. The `ModifyCounterPlacement` declaration has a recipient/type applicability pattern, a signed delta, active zones and an optional placing-player restriction. Its supported execution applies the change to the amount before addition and skips zero at the initial checkpoint. Ordinary addition and entry-counter execution reach this check through distinct engine paths. A supported entry declaration remains an entry pathway. The emitted `CountersAddedEvent` reports the final addition, including any replacement's adjustment. The replacement is not modeled as a trigger or a second placement.

[SDK replacement declarations](https://github.com/jasullivan5/argentum-engine/blob/bd26b756b8b4d347f19e92bc77ef020919ebfa48/mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/ReplacementEffect.kt) · [Engine replacement path](https://github.com/jasullivan5/argentum-engine/blob/bd26b756b8b4d347f19e92bc77ef020919ebfa48/rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/effects/ReplacementEffectUtils.kt) · [Engine entry path](https://github.com/jasullivan5/argentum-engine/blob/bd26b756b8b4d347f19e92bc77ef020919ebfa48/rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/effects/EntersWithReplacements.kt)

## Source and execution controls

The [frozen cohort](../src/test/resources/regression/counter-placement-sources.json) predates the interpreter changes. It preserves the retained Oracle snapshot hash, printing and Oracle identities, exact source text and structural expectations. Native cards were checked on the pinned engine. The synthetic fixtures isolate signed reduction, placing-player restrictions, zero, wrong type, an unresolved X, a conditional guard and an optional operation.

| Control | Structural finding and boundary |
| --- | --- |
| Hardened Scales | One additive replacement and no issuing placement or triggered response. One or more +1/+1 counters on a creature its controller controls qualify. The placer can be either player. Zero, charge counters, an opponent-controlled creature and an inactive source fail the applicability checks. |
| Travel Preparations | Resolving spell directly places one counter per chosen target, up to two. The target declaration permits creatures controlled by either player. Flashback is a separate casting permission for the same operation. |
| Abzan Falconer | Outlast is the issuing action, with mana, tap and sorcery timing retained in the action. The static flying grant does not supply a placement pathway. |
| Walking Ballista | Standalone interpretation separates entry with X, the four-mana activated addition and the other ability's counter removal for damage. The native execution checks use the authored definition regardless of Commander eligibility. |
| Winding Constrictor | The creature/artifact recipient pattern can overlap our creature requirement. The separate player-counter replacement cannot. |
| Wall of Roots and Goblin Chieftain | Source-only controls where authored definitions are unavailable. The former concerns a different counter type used as a cost. The latter grants a continuous stat bonus. |

Four fresh native positions used authored cards. Travel Preparations placed one counter on each of two chosen creatures without Hardened Scales. With Scales active, the controller's creature received two and the opponent's creature received one. Both positions emitted one addition per target. With Scales active, Walking Ballista cast for X=1 entered with two and emitted one addition of two. Cast for X=0, it received no counters and emitted no addition. The wrong-type, inactive-source and signed-decrease checks are structural. These observations have no independent whole-card qualification and do not establish correctness under competing replacements.

## Catalog and Discover verification

| Commander | Eligible cards | MATCH cards / pathways | UNKNOWN cards / pathways | NO_MATCH cards / pathways | MATCH pathways, placement / modification |
| --- | ---: | ---: | ---: | ---: | ---: |
| Kenrith, the Returned King | 15,955 | 658 / 694 | 605 / 652 | 537 / 615 | 687 / 7 |
| Judith, the Scourge Diva | 6,111 | 176 / 184 | 136 / 140 | 198 / 234 | 184 / 0 |
| Krenko, Mob Boss | 3,472 | 83 / 88 | 62 / 64 | 110 / 136 | 88 / 0 |

A card can have several recorded pathways and appear in multiple verdict columns. Cards without an emitted counter pathway do not contribute to any verdict column. The seven Kenrith modifiers are Caradora, Heart of Alacria, Doc Samson, Super Psychiatrist, Hardened Scales, Kami of Whispered Hopes, Michelangelo, Weirdness to 11, Ozolith, the Shattered Spire, and Winding Constrictor. Ozolith also places counters through a separate action.

Travel Preparations and Abzan Falconer each have one direct placement. Winding Constrictor's player-counter replacement and Izzet Generatorium's energy replacement fail the query. Branching Evolution retains its native pattern with UNKNOWN transformation support. Academy Drake's entry amount is recorded, and its conditional entry remains UNKNOWN. Walking Ballista has known format legality and unresolved full identity. Wall of Roots and Goblin Chieftain were absent from the authored eligibility inventory.

All five existing objectives produced byte-identical CLI reports for each of the three commanders. That compares the full results, eligibility, actions, prerequisites, facts and support evidence across 15 reports. The ordered eligible population of each new counter report matched the corresponding baseline population. The reused PR #52 baseline reports came from `98c4ef4`, with the additional Kenrith reports exported from `20f0773`. Their CLI sources and packaged inputs match merged main. The comparison receipt records both hashes for every legacy report, plus the new counts, structural support, populations and individual controls. [Comparison receipt](evidence/counter-placement-comparison.json)

In Edge, the normal Kenrith workflow exposed both relationships via **Mechanic**. Travel Preparations showed the native target choice and per-target quantity. Hardened Scales showed the active zone and the need for a separate placement. The focused browser checks passed at 1365 px and 390 px without document horizontal overflow. The four existing live state and triggered-operation checks also passed. The first browser run reached the correct card and failed on an assertion whose wording did not match the rendered prerequisite. The rerun asserted the actual explanation.

- [Hardened Scales in the desktop inspector](images/counter-placement/hardened-scales-1365.png)
- [Travel Preparations in the 390 px layout](images/counter-placement/travel-preparations-390.png)

## Verification and limits

- Live application main `2ed2117939ec00b4af11237effccc149d622ad97`. Oracle control freeze `b84e9d5`. Initial application runtime `af7d67de59273af4862acc609e22e624812ed315`. Corrected counter-support diagnostics at `6c564304f80ca03f7f142944e4f10e42da2330b5`. The corrected browser and CLI package was marked `+dirty` because the worktree briefly held the untracked Playwright test file. The runtime sources matched that commit.
- Maintained Argentum `app-stable` `bd26b756b8b4d347f19e92bc77ef020919ebfa48`. No engine change.
- `dev.ps1 installDist` and `dev.ps1 test --tests '*CounterPlacementTest' --tests '*CounterPlacementScenarioTest'` passed.
- Reproduction uses `mtg-assistant discover --commander "<name>" --objective <id> --json --output <path>` for all six objectives with the same catalog. Run `python scripts/compare-counter-placement.py <baseline-dir> <candidate-dir> docs/evidence/counter-placement-comparison.json` against the resulting reports. The first comparison run overlapped an installation refresh and lost its final export to a missing class. The corrected comparison ran from a stable copy of the installed libraries.
- `pnpm build` passed. Against the running application, `pnpm exec playwright test counter-placement.spec.ts triggered-operations.spec.ts state-dependency.spec.ts` passed six Edge checks. The full browser suite and separate full engine test suite were not run.
- `bash scripts/verify.sh full` passed on the corrected revision. All 694 application tests passed with no failures, errors or skips. The distribution built, the five catalog comparator tests passed, and the current catalog contract returned true. Frozen historical fingerprints differed as they already did on merged main after PR #52. The initial sandboxed Bash launch was denied. The successful gate used Git Bash with sandbox approval.
- Complex target/collection bindings, opaque guards and amounts, multiplier replacements, mixed entry-source patterns and replacement ordering remain partial or unsupported. The native evidence is retained where interpreted and other unsupported structures remain in the report. The inspected cohort and catalog summaries measure this bounded projection, not precision or recall over Oracle.
- Existing policies remain in their respective queries. In particular, death-enhanced removal still has its historical separate path.

The next increment should extend this same applicability model to native multiplicative counter replacements. Use a source-bound multiplier, zero and wrong-type controls, and an isolated native execution scenario. Treat interactions between competing replacements as a separate question.
