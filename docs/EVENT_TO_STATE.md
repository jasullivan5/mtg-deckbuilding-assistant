# Atomic objective relationship experiment

## Objective Semantics Foundations · Increment 3

### Discover creature cards for your graveyard

From the application checkout, run `.\scripts\explorer.ps1`. Open [Explorer](http://127.0.0.1:8787) after the launcher prints **Explorer ready**.

1. Choose **Judith, the Scourge Diva** and **I have at least one creature card in my graveyard**, then select **Find cards**. Set **Mechanic** to **Mill** and browse the results. On **Stitcher's Supplier**, switch between its enters and death triggers. Both move cards from the library. Its death trigger responds to a prior death.
2. Filter by **Discard** and compare **Faithless Looting**, which discards during resolution, with **Thrill of Possibility**, which requires a discard as you cast it. Either can supply the state if a creature card is among the cards actually discarded and it remains in your graveyard.
3. Filter by **Sacrifice** and inspect **Viscera Seer** for a possible contribution to both objectives. Then open **Needs interpretation** and inspect **Mogg Fanatic** for an unresolved source-bound sacrifice. In **All findings**, **Elderfang Disciple** shows why an opponent's discard does not advance your own graveyard state.

### Independent state results for Judith

The eligible authored input was **6,111 physical cards**, with black and red full color identity. The state search evaluated 1,431 pathways. A card may contribute through more than one route.

| Route | Conditional pathways | Distinct cards with a conditional pathway | Unresolved pathways | Mechanical nonmatches |
| --- | ---: | ---: | ---: | ---: |
| Sacrifice | 150 | 147 | 803 | 3 |
| Mill | 58 | 55 | 11 | 2 |
| Discard | 225 | 218 | 78 | 101 |
| Total | 433 | 412 | 892 | 106 |

The state search has no separate policy exclusions beyond eligibility and its supported mechanics. The same-fact comparison found 150 conditional state paths also accepted for death generation, with the same fact IDs as the Increment 2 conditional sacrifice results. It recorded 32 other mechanically death-compatible paths excluded by the event policy, including Fleshbag Marauder's unresolved state route. These figures describe event policy. Murder stays excluded under the existing event policy and ordinary destruction is outside the state search. Tome Scour remains outside Judith's color identity. Reassembling Skeleton's return is a different movement.

| Source-reviewed control | Observed discovery |
| --- | --- |
| Viscera Seer and Accursed Centaur | The activation payment and resolving trigger each support a conditional sacrifice arrival. The same movement could produce a qualifying death. |
| Stitcher's Supplier | Two mill paths retain separate native action IDs and their distinct triggers. Each mill is separate from a battlefield departure. |
| Faithless Looting and Thrill of Possibility | Both have conditional discard paths, with resolution and casting payment recorded separately. |
| Mire Triton and Elderfang Disciple | Held-out eligible controls. Triton's own mill qualifies conditionally. Disciple's opponent-only discard is a mechanical nonmatch. |
| Codex Shredder and Millstone | The standalone native Codex mill can select you, but effective color identity is unresolved and the live run excludes it. Millstone demonstrates an eligible mill that can target yourself. |
| Dread Summons and Mogg Fanatic | Dynamic mill quantity/player binding and source-bound sacrifice remain unresolved, respectively. |

The cohort checks current Oracle expectations against the maintained definitions. Exact definition and action references are attached to live findings. The reviewed cohort does not independently qualify the catalog. [Stitcher's Supplier](https://scryfall.com/card/tdc/196/stitchers-supplier), [Faithless Looting and casting rulings](https://magic.wizards.com/en/news/feature/innistrad-remastered-release-notes), [Thrill of Possibility rulings](https://magic.wizards.com/en/news/feature/core-set-2021-and-jumpstart-release-notes-2020-06-20), [Codex Shredder](https://scryfall.com/card/cmr/304/codex-shredder), [Mire Triton](https://scryfall.com/card/cmm/174/mire-triton), [Elderfang Disciple](https://magic.wizards.com/en/news/feature/kaldheim-release-notes-2021-01-22)

### What the query establishes

The shared walker retains mill from a marked library-top gather and its bound move, and discard from a hand selection and its discard move. Fixed-count native mill and discard costs supply their own facts. The state check reuses the sacrifice choice, correlated owner and destination constraints, and tests for a possible original nontoken creature card. A state result remains independent of the death policy. The death comparison is attached afterward by fact ID. A mill or discard can be triggered by a death without itself causing one.

Actual availability, destination after replacements, card type on arrival and presence at the checkpoint depend on play. An owned animated land may die and arrive as a land card. A borrowed creature can die and go to another player's graveyard. A token leaves no creature card, and finality can prevent the graveyard arrival. A qualifying arrival can supply a state that already holds. The seven controlled engine cases below are a separate execution baseline.

The reusable part is the movement record with action, source, affected player, ownership, destination and choice restrictions. Another objective could query those facts with its own arrival criteria. The creature-card witness and the current fixed-count bounds remain specific to this search. Unbound targets, variable choices, other movement mechanisms and incomplete definitions remain outside positive coverage.

### Comparison and browser evidence

- Fresh CLI runs compared the feature with baseline main `d556cff41f11fd713c44f47267fc7b81aa320af0` on identical eligible inputs and engine `03869c08665bf476a8b40dacbfbcd501c54a33bd`. The existing Windows baseline package contained the same application code as baseline main. The three original reports compared equal after omitting the new default-null fields for legacy rows and facts. Death remained at 499 accepted pathways on 487 cards, reanimation at 106 on 104, and token creation at 363 on 346. The local summary is in `generated/explorer/state-comparison.json`.
- The Windows launcher built and served application `a65668b7e80e0231be207211cff211a3fa1abfac` with clean Argentum `app-stable` at the SHA above. The locked frontend install, TypeScript/Vite build and Kotlin installation passed. Edge passed eight fixture tests and fifteen live tests, including selecting the state objective directly, discovering a mill card by mechanic and inspecting two Supplier triggers, Looting's discard, Seer's event comparison and Fanatic's unresolved route. Later test changes preserve the application and frontend code.
- Actual inspection captures are `generated/explorer/state-discovery-desktop.png` and `generated/explorer/state-discovery-mobile.png`. The mobile browser ran at a 390 px viewport. The images show the relevant card inspectors cropped from each viewport.

**Next decision.** Test a variable self-mill route against the existing amount bounds. Require a positive payable or resolving quantity, preserve zero and unresolved cases, and keep the affected player tied to the same graveyard.

## Objective Semantics Foundations · Increment 2

### Try it in Explorer

Run `.\scripts\explorer.ps1` from the application checkout. Open [Explorer](http://127.0.0.1:8787) when the terminal prints **Explorer ready**.

1. Select **Judith, the Scourge Diva** and **My nontoken creatures die**, then choose **Find cards → Potential graveyard contributions**. Open the first result, Accursed Centaur. The sacrifice is required when the enters trigger resolves. Open **Native evidence** for the exact action, fact and definition.
2. In that view, inspect **Viscera Seer**. Compare its activation payment with Centaur's triggered instruction. Then inspect **Blood Bairn** for a payment that needs another creature. Open **Separate engine validation** to compare the conditions with the seven recorded game cases.
3. Open **Needs interpretation** and search for **Agent of Shauku**. A land must also be a nontoken creature to qualify for the death, and a land's battlefield type does not establish its graveyard type. In **All findings**, inspect **Murder** for the existing ordinary-removal policy exclusion.

### What the search found

The Judith run evaluated 6,111 Commander-eligible authored physical cards at the same full color identity and engine revision as the baseline. The research view contains **147 distinct cards across 150 conditional sacrifice paths**. Those paths comprise 108 activation payments, 27 ordinary casting payments, 12 resolving effects, one alternative casting payment, one optional casting payment and one payment during resolution. They are a subset of accepted death pathways. The run kept **474 accepted direct paths on 462 cards** and **25 accepted death-enhanced removal paths on 25 cards**, with 487 distinct cards across both groups.

The report also retains 464 unresolved sacrifice relationships across 447 cards. In 158 paths the death criterion or policy is unresolved. In 198 the fixed-choice movement is outside the supported slice. In 108 the joint creature and destination compatibility is unresolved. Cards may have several paths, including both conditional and unresolved findings. These figures measure the authored Judith input pool and the implemented lens.

| Control | What the live records show |
| --- | --- |
| Viscera Seer | Conditional activation payment. The cost needs a creature you control and allows Seer itself if eligible. An additional creature is needed to pay while keeping Seer. |
| Accursed Centaur | Conditional resolving trigger. It can be the sacrificed creature if it is still an eligible choice. |
| Blood Bairn | Conditional activation payment that excludes the source. |
| Carrion Feeder and Village Rites | Additional mechanically found controls. Feeder can sacrifice itself even though the counter effect would then do nothing. Village Rites needs a creature to cast. |
| Agent of Shauku and Mogg Fanatic | The land restriction is unresolved for the first. The second has a recognized primary death route, but self-bound movement is outside this relationship slice. |
| Murder and Fleshbag Marauder | Both retain the existing policy exclusions. Symmetrical or incidental removal stays separate from an accepted own-death generator. |
| Reassembling Skeleton | Authored return from the graveyard can replenish sacrifice material. It produces no death-to-graveyard relationship by itself. |

The relevant Oracle text was checked via Scryfall, and the retained source/rulings fixtures cover Centaur, Seer, Blood Bairn, Agent of Shauku, Murder and Skeleton. Wizards' rulings distinguish Feeder's payment from its payoff and give the exact count for Village Rites. Source review checked these instructions and restrictions. The catalog-wide findings still describe native representation. [Centaur Oracle](https://scryfall.com/card/ons/123/accursed-centaur), [Seer Oracle](https://scryfall.com/card/soc/229/viscera-seer), [Carrion Feeder rulings](https://magic.wizards.com/en/news/feature/eternal-masters-release-notes-2016-06-02), [Village Rites rulings](https://magic.wizards.com/en/news/feature/innistrad-remastered-release-notes)

### How the two criteria connect

The existing death query supplies the event and policy decision. The application joins that exact path by fact ID to a native fixed-count sacrifice choice, then checks whether a legal departing object could be a nontoken creature you control and own with a nominal move to your graveyard. The bounded positive case requires an explicit creature choice with supported restrictions. Ownership and controller constraints apply to the same choice. The exact action, role, quantity, source exclusions, timing and unsupported records remain available in the report. The CLI exposes the additional records through `discover --json`.

The ordinary sacrifice destination is the owner's graveyard. The event can hold for a borrowed creature, while the owner's destination makes that arrival insufficient for your state. A finality counter can redirect the move and prevent the death. An animated land can die as a creature and arrive as a land card. The graveyard state is satisfied whenever it has a qualifying witness, and the new arrival supplies it only while that same arrival is a witness. It establishes the state only if the state was previously false. Milling illustrates a path to the same state without a death. [Comprehensive Rules](https://magic.wizards.com/en/rules), [Finality rulings](https://magic.wizards.com/en/news/feature/duskmourn-house-of-horror-release-notes), [Controlled execution receipt](../web/src/objective-semantics-receipt.json)

The application leaves actual resources, legal timing and choices, replacement effects, arrival characteristics, later moves and the prior graveyard state to execution. Broad permanent and artifact selectors, subtype or branch restrictions, variable or bound selections, and self-bound sacrifices can need further evidence. Source identity, translation assurance and contextual deck value remain separate from this mechanical check. The reusable boundary is the native choice with its movement route and object constraints. Future event queries can supply other routes, and future checkpoint criteria can impose their own destination and object requirements.

### Validation and revisions

- Compared fresh Judith CLI exports from baseline application `b36b1dc8b098e9ef5a26d3a2312be2f24ca9f5a3` and the feature build. Primary result rows, counts, actions, eligible input, unsupported records, structural support and assurance were identical for all three objectives. Reanimation remained at 106 accepted pathways on 104 cards, and token creation at 363 on 346. The additive fact data records fixed-choice sacrifice destinations and preserves native owner scope on 14 previously emitted records. The local comparison is in `generated/explorer/relationship-primary-comparison.json`.
- Feature implementation and test source `6855d3be4e0adb3d55ded0340e2be58ff4ddb71f` resolve Argentum `03869c08665bf476a8b40dacbfbcd501c54a33bd` on clean `app-stable`. The Windows launcher passed the locked frontend install, TypeScript/Vite build and Kotlin installation. Explorer reported a pre-commit build label of `116ac3ad8803358fa01cc5d6f20fcb8a70bf9b93+dirty`. A SHA-256 comparison of all 564 packaged file entries against the clean feature build found only the application revision resource differed.
- `.\dev.ps1 test --offline` passed all 524 application tests at the clean feature revision. The seven freshly generated scenario case records exactly match the retained Increment 1 receipt. The new static tests exercise authored cards and distinct activation, casting and resolution structures, plus independent owner, token, type, zone, eligibility and policy controls.
- Edge passed eight fixture tests and fourteen live tests. The live journey selected a card directly from the research view, inspected cost and trigger examples, checked an unresolved case and a policy exclusion, and verified horizontal containment at 390 px. Local captures are `generated/explorer/relationship-desktop.png` and `generated/explorer/relationship-mobile.png`.

**Next decision.** Extend the arrival check to one source-bound self-sacrifice case such as Mogg Fanatic. Use the native source identity and graveyard card characteristics as evidence, with a face or animation control. This would test whether a second kind of object binding can reuse the destination check before attempting another objective pair.

## Objective Semantics Foundations · Increment 1

This is a proposed research workstream pending Product Studio's milestone decision.

### Additional frozen expectations

| Case | Setup and executed action | State B before the action | Event A | State B at the checkpoint | Arrival supplies B |
| --- | --- | --- | --- | --- | --- |
| Animated land | Activate Raging Ravine's animation, resolve it, then sacrifice the land to Viscera Seer in the same turn | No | Yes | No | No |
| Prefilled graveyard | Start with one Wolfbat in our graveyard and sacrifice a different, opponent-owned Wolfbat we control | Yes | Yes | Yes | No |

The animated-land fixture uses the maintained fork's Raging Ravine definition (WWK/141). Wizards lists the card as a land with a temporary self-animation that retains the land type. Rules 700.4 and 603.10a explain the battlefield departure and the use of pre-departure characteristics. Under rule 400.7 the arrival is a new object, so the battlefield animation does not make the graveyard card a creature. In the prefilled control, rule 404.1 puts the sacrificed card into its owner's graveyard. [Wizards' card notes](https://magic.wizards.com/en/news/feature/edge-of-eternities-release-notes), [Comprehensive Rules](https://magic.wizards.com/en/rules).

### Try it in Explorer

Run `.\scripts\explorer.ps1` from the application checkout and open [Explorer](http://127.0.0.1:8787) when the launcher prints **Explorer ready**. Choose **Research and learning → Open Contribution Model Study → Open event-to-state experiment**.

1. **Owned creature.** Follow Wolfbat into your graveyard. Compare the application verdicts with PR #30 under **Rules-derived expectation and recorded inputs**.
2. **Animated land.** Compare Raging Ravine's last-known battlefield creature status with the graveyard card's land type. Check which criterion each observation answers.
3. **Prefilled graveyard.** Compare the graveyard state before and after the move. Both criteria hold at the checkpoint. Follow the new arrival to the opponent's graveyard and identify the card that supplies your state.

### Reuse and evidence boundaries

- `nontokenCreatureDeath` reads one native move. Last-known controller, type and token status qualify the departure. The actual destination controls the result when the requested move was redirected.
- `graveyardCreatureCards` reads a player's graveyard at a caller-supplied checkpoint. It returns current object references for the cards that satisfy the condition, with unresolved entries tracked separately. One confirmed creature card suffices even when another entry is unresolved.
- `attributeArrival` joins a qualifying move to a set of witnesses from the same execution. It compares the recorded arrival reference, including generation, with the qualifying checkpoint objects. A later visit by the same entity has a different reference. The join answers whether the arrival supplies a witness. Establishing a previously false condition also depends on the starting state, which these fixtures record separately.
- Static facts can identify potential costs, object filters and nominal movement. They lack the actual destination, pre-departure characteristics and checkpoint references needed for attribution. Missing execution evidence can yield an unresolved verdict. The scenario fixtures still supply setup, action and checkpoint selection. Explorer displays the resulting test receipt.

### Observed comparison

The new Kotlin run matched all five PR #30 event, state and link verdicts below. Actual and requested movement, owner, last-known controller, creature and token status, finality, sacrifice, current arrival status, checkpoint types and graveyard contents also matched. The receipt records both additional outcomes against the expectations above. [Retained application evaluation](../web/src/objective-semantics-receipt.json)

- **Animated land.** The engine resolved Raging Ravine's animation, then recorded a nontoken creature leaving the battlefield for your graveyard. The current arrival had type Land. A held and B was false.
- **Prefilled graveyard.** A held and B was true before and after the move. The application found the same original witness in your graveyard at both checkpoints. The new arrival went to its owner's graveyard and supplied no witness for B.
- **Missing evidence checks.** A missing move, missing last-known information, missing arrival reference and missing checkpoint each left the relevant attribution unresolved. These checks are separate from the seven executed cases in the receipt.

### Reproduce and verify

- Run `.\dev.ps1 test --offline` with JDK 21 and a populated Gradle cache. The full application suite passed 521 tests, including the seven-case receipt and the missing-evidence checks. To regenerate just the receipt, run `.\dev.ps1 test --tests '*EventToStateScenarioTest'`. The test writes `generated/event-to-state/observations.json`.
- The receipt records application `37267b5f4e9977a8a61a8ca6b353e4fb4c564166` and resolved Argentum `03869c08665bf476a8b40dacbfbcd501c54a33bd` on clean `app-stable`. The Windows launcher passed TypeScript, Vite and Kotlin packaging. Explorer health reported application `1fa281c0171c0b733037c276cec15f05664b91c5` and the same Argentum revision.
- Against the launched Explorer in headless Edge, `pnpm --dir web test:ui` passed 8 tests and `pnpm --dir web test:live` passed 13. The live checks exercised all seven cases, keyboard selection, the 390 px viewport, the historical comparison and zero discovery requests from the panel. The existing live Commander discovery tests also passed.

**Next research question.** Can a controlled return-to-hand and redeath sequence show that a later graveyard visit supplies the current state while the original death remains true? This would exercise object generation and checkpoint timing with a real repeat visit.

## PR #30 baseline

### Frozen expectations

Every case begins with no creature cards in the observing player's graveyard. Read the state after the stack and pending decisions settle, with no intervening new action. The actual engine move determines the event. Count actual creature cards in the observing player's graveyard at that checkpoint.

| Case | Setup and native action | Expected transition of the tracked object | Event A | State B | A establishes B through the tracked move |
| --- | --- | --- | --- | --- | --- |
| Owned | Activate Viscera Seer by sacrificing an owned and controlled Wolfbat | Battlefield → own graveyard | Yes | Yes | Yes |
| Borrowed | Same cost with an opponent-owned Wolfbat under our control | Battlefield → owner's graveyard | Yes | No | No |
| Token | Same cost with a controlled creature token | Battlefield → own graveyard, then token ceases to exist | No | No | No |
| Finality | Same cost with an owned and controlled Wolfbat bearing a finality counter | Battlefield → exile instead of graveyard | No | No | No |
| Mill | Target ourselves with Tome Scour with Wolfbat on top of the library | Library → own graveyard | No | Yes | No |

A token can die without satisfying the nontoken criterion. Controller and owner are independent. Finality applies to the move even if the sacrifice cost was paid. Sources include the retained Seer and Wolfbat printings, [Wizards' rules glossary](https://magic.wizards.com/en/keyword-glossary), the [finality release notes](https://magic.wizards.com/en/news/feature/duskmourn-house-of-horror-release-notes) and the [Comprehensive Rules](https://magic.wizards.com/en/rules) (dies, zones, tokens and state-based actions).

### Original Explorer walkthrough

PR #30 originally displayed a retained record of these five executed scenarios. Its verdicts are still available in the new case picker.

1. **Owned creature.** Follow Wolfbat from Seer’s sacrifice cost to your graveyard. Find the two independently satisfied criteria and check why the recorded arrival still supplies the state.
2. **Opponent-owned creature.** Compare the last-known controller with the owner and both players’ graveyards. Explain why A still holds and B does not.
3. **Finality counter.** Compare the requested and actual destination. Then try **Creature token**, which really dies but is outside A, and **Mill a creature card**, which establishes B independently.

**Rules-derived inputs.** The native examples use [Viscera Seer M11/120](https://scryfall.com/card/m11/120/viscera-seer) (`6179f847-e334-4f7f-9a4e-0013942a394f`), [Wolfbat TLA/122](https://scryfall.com/card/tla/122/wolfbat) (`2ae83b4e-e5a4-4c98-8d16-eef3c71b8ff2`) and [Tome Scour M10/76](https://scryfall.com/card/m10/76/tome-scour) (`fdbdf96b-e7c5-42e5-9a16-03daafde40af`). The front-face Oracle records for those printings were checked for this experiment. The fixture sets the finality counter directly, then tests its effect on the actual departure.

**Historical discovery.** [Explorer milestones](EXPLORER_MILESTONES.md) describe M3.1’s recognized Seer sacrifice and M3.2’s historical graveyard-active return coverage, including Wolfbat. Those records answer separate discovery questions and do not verify the controlled combinations here.

**New execution.** All five rows matched the frozen expectations. The retained [receipt](../web/src/event-to-state-receipt.json) contains actual destination, owner, last-known controller, battlefield creature and token status, both graveyards, the tracked card’s types and zone at the checkpoint, and the verdicts. The developer-authored test executed real Seer activations and a native Tome Scour cast. Borrowed control and token identity were fixture setup. The run does not independently qualify these cards or verify those setup mechanics in gameplay.

### Original Product Studio assessment

**Use independent event and state definitions for the next bounded trial.** The borrowed Wolfbat shows a true event with a false state. Milling shows the reverse. The token and finality examples separate an actual but nonqualifying death from a sacrifice paid without a death. In the owned case the arrived object is still current and is one of the creature cards supplying the state. This result depends on its actual destination and its type at the checkpoint. A later departure could invalidate B while the original A remains true.

This separation preserves direct responses to a death under their own trigger filters. A graveyard payoff can have separate ownership, type, timing or other access requirements. The experiment supports inspecting those dependencies explicitly. It does not establish admission to a search or contextual deck value.

| Evidence layer | Available information | Boundary for this relationship |
| --- | --- | --- |
| Native definition and static extraction | Sacrifice cost and its object filter, trigger filters, nominal movement zones, target and controller restrictions, and some replacement-related representation | Static extraction does not observe a particular object's departure, resolve its destination after replacement, or take a later snapshot. Existing discovery checks specific structural pathways. A missing emission is not a mechanical nonmatch. |
| Native execution | `ZoneChangeEvent` exposes requested and actual destinations, owner, last-known controller/types/token status and old/new object references. `GameState` exposes the player’s graveyard and current object reference. | The two predicates and their join live in a test. The sample demonstrates reuse of these native records and needs no new engine representation for these five cases. |
| Future query semantics | Evaluate event A from the actual departure and state B from the checkpoint, then test whether the recorded arrival is still current and supplies B. | A production query would need to distinguish which facts are known statically, which depend on execution, and when missing information leaves a relationship unresolved. There is no evidence here for catalog-wide handling of arbitrary effects. |

The join is justified when the question is whether a particular move established the state. For a standalone trigger or a standalone state check, the extra join adds complexity without answering a different question. Animation, competing replacement effects, and changes after the checkpoint remain outside the executed population. The next bounded experiment should animate a native noncreature card, sacrifice it, and verify that its battlefield creature status qualifies A while its graveyard card type fails B. This would test whether the same separation works when the types differ within a single move.

### Original reproduction and verification

- Windows/JDK 21: `.\dev.ps1 test --tests '*EventToStateScenarioTest' --tests '*DiscoveryCoverageTest' --tests '*ReanimationIntegrationTest'`. Fifteen Kotlin tests passed, including one scenario test covering all five controlled cases. It writes `generated/event-to-state/observations.json` after the checks pass.
- Exact application revision for the retained execution: `9c8ced9167550322765dcf75f81b23434b9cf24d`. Resolved maintained Argentum `app-stable` revision: `03869c08665bf476a8b40dacbfbcd501c54a33bd`.
- The Windows launcher completed its TypeScript check, Vite production build and Kotlin distribution build. On Edge, `pnpm --dir web test:ui` passed 8 tests and `pnpm --dir web test:live` passed 13. The live suite covers all five cases, keyboard selection, a 390 px viewport, zero discovery requests from the panel, and existing discovery/research navigation.
- Browser-tested application code: `87e3a14d12d8f137070f279da31492f14484eb74`, with the test-only request assertion from `837a2adea419261b8191040104b2a2b83790080c` in the working tree during packaging. The health endpoint therefore reported `87e3a14d12d8f137070f279da31492f14484eb74+dirty`. It reported the same Argentum SHA as the scenario run.
