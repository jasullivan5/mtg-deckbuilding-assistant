# Conditional casting and activation costs

## Chosen targets at casting

The walker records the native target requirements and their order on the casting action. Each fixed target-dependent modifier references an any-selected-target filter at the casting-cost checkpoint. The requirement and its selection group retain the target's kind, zone and native restrictions. Related generic and colored modifiers keep separate declaration paths and isolated prices.

For a concrete announcement, the shared target query can ask Argentum to bind and validate the chosen groups before filtering those same selected objects. It can return a matched object condition, a legal selection with no matching object, or unresolved evidence. The query is independent of price and can also inspect who controls a chosen object. The native engine may estimate affordability with possible targets before selection. That estimate and a target present elsewhere in the game cannot substitute for the announced target at payment.

The bounded structural projection describes a compatible target within supported requirements. The remaining targeting groups, costs, and actual game state are separate prerequisites. The application calculates each native modifier in isolation. Brush Off's generic reduction yields `{1}{U}{U}` on that basis, and its blue reduction yields `{2}{U}`. Its combined price is an observation from native execution. [Frozen source and native bindings](../src/test/resources/regression/chosen-target-cost-sources.json)

### Native observations and catalog comparison

- **Dire Downdraft.** The selected tapped creature reduced payment from four mana to three. Selecting an untapped creature retained the four-mana price even with another tapped creature present. Three mana was insufficient for that selection. Moving the selected creature to the graveyard after casting prevented its later move to the library and left the payment spent.
- **No One Left Behind.** A mana-value-two creature card selected in the caster's graveyard enabled a two-mana cast and returned to the battlefield. Selecting the same card in hand was rejected without payment or a return.
- **This Town Ain't Big Enough.** Two selected opposing permanents required five mana. One or two selected caster-controlled permanents required two mana. An unselected permanent controlled by the caster did not supply the discount.
- **Brush Off and Titanic Brawl.** Brush Off paid two mana when targeting an instant spell on the stack. A separate target query on Titanic Brawl identified the chosen controller scope and returned two groups in native order. Swapping the announced targets left the binding unresolved. The query did not read any cost record.

| Commander | Eligible cards | Cast actions with target selection evidence | Cards / actions with fixed chosen-target costs | Generic declarations (supported / total) | Colored declarations (supported / total) | Distinct newly supported conditions |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Kenrith | 15,955 | 2,198 | 33 / 33 | 24 / 33 | 1 / 1 | 24 |
| Judith | 6,111 | 923 | 7 / 7 | 4 / 7 | 0 / 0 | 4 |
| Krenko | 3,472 | 449 | 2 / 2 | 1 / 2 | 0 / 0 | 1 |

All generic declarations started UNKNOWN. Brush Off's colored declaration was absent from the previous cost export. Its two modifiers share one action and condition. No declaration transitioned to NO_MATCH. The commander populations overlap. These are counts of native interpretation and action evidence, independent of the existing Discover verdicts.

Nine generic declarations remain UNKNOWN under Kenrith. Bitter Downfall uses damage history. Bouncer's Beatdown and Torch Breath use mixed creature-or-planeswalker targets. Knockout Blow restricts target eligibility by combat state. Out of the Way requires opponent-controlled targets, while Ride's End and Roadside Blowout use type alternatives. Run Over has a compound cost filter across a two-group cast. Swampsnare Trap uses an implicit Aura target outside these explicit spell target requirements. Their native declarations are retained. No source discrepancy was established in the five frozen controls. The earlier controlled-permanent fidelity concerns remain separate.

The adjacent Kenrith inventory includes Fireball's target-count increase and the fixed self-cast increases on Dragon's Prey and Vanish into Eternity. These are outside the reduction family. Hinata and the three opponent-targeting taxes also retain their existing evidence.

The merged baseline is application `cb3a36ee6c73508547a1544b74f34aa55cfe8947`. Its CLI runtime sources, packaged resources and build inputs equal the previously tested `8b8fafd9b22d26de8eadf491e00f79501f655a86` exports. The source cohort is `474a0a8bc1518a4cc747b8a83a70b43bd5abf5f6`. The candidate is `9d7d45407c3979ea8f0b414a2b0d668286bc4ba5`, using clean Argentum `bd26b756b8b4d347f19e92bc77ef020919ebfa48`.

All 18 CLI comparisons preserved the exact eligibility, objective rows and verdicts, Class evidence, counter facts, triggered operations, state dependencies and unrelated costs. Only casting-action selection metadata and the selected cost family changed. Reproduce with `python scripts/compare-chosen-target-costs.py <baseline-dir> <candidate-dir> <receipt>`. The [comparison receipt](evidence/chosen-target-cost-comparison.json) records all declarations, exact population digests and unchanged card and pathway counts by verdict.

All 11 focused checks passed. The frozen source snapshot and five normalized native hashes were recomputed. The built CLI decoded an archived PR #58 report through `audit coverage --snapshot`. The running Explorer served the application and exported a Judith reanimation run with the existing No One Left Behind match, its graveyard target requirement and its casting discount. The executable source revision was tagged `+dirty` because the report and comparison script were in progress. `bash scripts/verify.sh full` passed all 740 application tests with zero failures, errors or skips, the distribution build and the current catalog contract. The older historical fingerprints still differ, as in PR #58. Browser automation and the separate full engine suite were outside this verification.

**Next Card Integration decision.** Prioritize source-relative attachment relationships. A bounded Aura cohort can test how the cast target becomes the attached object, how active-zone and controller changes affect the relationship, and which supported operations refer to that object later. This can inform triggers and continuous modifications across multiple objectives. Linked movement through intermediate zones is another high-value identity gap. Both have broader reuse than adding another cost-only predicate.

## Controlled battlefield witnesses

A fixed generic self-casting discount can depend on at least one permanent the caster controls matching the complete native filter. The shared condition uses the SDK's battlefield count with a minimum of one. The enclosing record binds that condition to the casting-cost checkpoint and the affected spell. A relative caster binding and the battlefield count keep the witness separate from the spell and from its owner. A matching spell on the stack, permanent in another zone, or permanent controlled only by an opponent cannot supply the witness.

Structural support covers bounded permanent types, subtype restrictions, and selected characteristics such as flying and minimum power. The native engine checks concrete witnesses against the current projected battlefield. The extractor derives the isolated generic change to the fixed printed payment using the SDK's mana operation. A false condition contributes no discount. Each declaration retains its own condition and amount, so independent discounts on one spell remain separately inspectable. The calculated price does not settle other payments, competing modifiers, alternate prices or affordability.

The source cohort retains printing and Oracle IDs separately from native code paths. A supported native interpretation may expose differences from Oracle. For example, Arcane Epiphany, Geistlight Snare's Spirit branch, Neonate's Rush and Venom's Hunger use native creature restrictions where the source text specifies a subtype alone. These differences remain visible in the native filter. [Frozen controls](../src/test/resources/regression/controlled-permanent-cost-sources.json)

### Native execution and catalog impact

The native scenarios submitted real spells with explicit mana pools and checked both mana spent and battlefield entry. Academy Journeymage spent five mana with no Wizard and four with the caster's Merfolk Trickster. Four mana was insufficient with only an opponent's Wizard, a caster-controlled Walking Corpse, or a Wizard moved from battlefield to graveyard. Three mana was insufficient even with a qualifying Wizard. Rejected casts preserved the mana pool and produced no arrival. Cactarantula spent six mana with Forest and five with Eroded Canyon.

| Commander | Eligible cards | Cards / casting actions | Declarations | UNKNOWN → MATCH | Remain UNKNOWN |
| --- | ---: | ---: | ---: | ---: | ---: |
| Kenrith | 15,955 | 16 / 16 | 18 | 17 | 1 |
| Judith | 6,111 | 6 / 6 | 7 | 7 | 0 |
| Krenko | 3,472 | 3 / 3 | 3 | 3 | 0 |

There were no UNKNOWN-to-NO_MATCH transitions in these exports. Each row is a different view of the catalog, so the rows overlap. Assassin's Ink and Geistlight Snare each carry two separate declarations on one casting action. Wolfkin Outcast's Wolf-or-Werewolf disjunction remains unresolved for structural support while its complete native filter and typed existence condition are preserved. These verdicts describe native mechanical interpretation, independently of Discover admission and source fidelity. No existing objective admits a battlefield-witness discount on that basis alone.

The baseline is merged application `397b74df9275817b115f29eded79a498f58a1bc1`. The archived CLI reports were produced at `4413c8be33e48fb94576c652f3bfdd8ee11ca10c` with identical runtime sources, resources and build inputs to merged main. The source cohort was frozen at `b604e75984beaa79577728c1165f1d28b9c81d9c`. The implementation and tests are at `8b8fafd9b22d26de8eadf491e00f79501f655a86` and use clean Argentum `bd26b756b8b4d347f19e92bc77ef020919ebfa48`.

All 18 built-CLI comparisons preserved exact eligibility, objective rows, verdicts, Class evidence, counter facts, triggered operations, state dependencies and unrelated costs. Only the derived condition, reduction amount and isolated price fields for the selected family changed. Reproduce with `python scripts/compare-controlled-permanent-costs.py <baseline-dir> <candidate-dir> <receipt>`. The [comparison receipt](evidence/controlled-permanent-cost-comparison.json) records each transition, card and pathway counts by verdict, and exact population digests.

All 16 focused extraction and native casting checks passed after setting JDK 21 on PATH. All five frozen source and normalized native hashes were independently recomputed against the retained snapshot and engine files. The built CLI read the archived PR #57 report through `audit coverage --snapshot`. The running Explorer served its existing page and its Kenrith death-history export retained Academy Journeymage's typed casting record without adding a result. Explorer stamped the tested implementation with `+dirty` while this report and comparison script were in progress. `bash scripts/verify.sh full` passed 729 application tests with zero failures, errors or skips. The distribution and current catalog contract passed. Historical report fingerprints still differ from the older frozen baseline, as at PR #57. The full browser and separate engine suites were outside this application-only verification.

A useful next research candidate is fixed chosen-target reductions. That work needs explicit target binding at payment and source-led checks for selection, origin and multiple targets.

## Original conditional-cost increment (PR #57)

The shared walker attaches conditional mana changes to the existing action record. Each record keeps its declaration path, casting or activation checkpoint, actor, native predicate or reduction source, printed mana component and original declaration. The containing action retains the source card, face, native costs, targets, availability and other restrictions. Older reports can omit the added fields.

The supported activation form uses Argentum's pip-by-pip subtraction for a fixed mana atom. It understands the activator's draw threshold and the broad creature-death predicate. The supported casting form uses Argentum's generic reduction for a self-cast declaration based on a creature dying earlier that turn. The derived amount applies to that printed mana component in isolation. Other payments, competing adjustments and actual affordability require native execution.

The existing death-history query recognizes a payment relationship separately from resolution and permission. Its legacy death-enhanced-removal projection is unchanged. No synthetic effect is created to represent a discount. [Frozen source and native bindings](../src/test/resources/regression/conditional-cost-sources.json)

## Controls

| Control | Interpretation |
| --- | --- |
| Kami of Jealous Thirst | Three draws by the activator reduce both the four generic mana and the black symbol. Its once-per-turn restriction still belongs to the action. |
| Dreaded Bat-Cloud | Table-wide creature-death history can reduce the printed casting requirement from `{4}{B}` to `{1}{B}`. |
| Grim Reaper's Sprint | The same native self-casting predicate supplies a generic reduction alongside separate enchant targeting and resolution effects. |
| Academy Journeymage | The native Wizard-control filter and declaration are retained. The resulting relationship is unresolved in this scope. |
| Tragic Slip and Innkeeper's Talent | The former changes a resolution outcome. The latter has separate Class progression payments. |

## Verification

The native payment cases constructed the relevant turn-history components and submitted real Argentum actions with explicit mana pools. Bat-Cloud's cast failed without a death and two black mana, and succeeded without a death at five. A death tally on the opponent enabled the two-mana cast. One black mana was still insufficient. Rejected attempts caused no battlefield arrival. Kami resolved at five black mana without draws, and at zero mana after three activator draws. Two activator draws with three opponent draws and no mana were insufficient. The opponent lost two life only in the successful cases.

| Commander | Unchanged eligible population | Cost records | Supported / unresolved | Death-history MATCH cards / pathways |
| --- | ---: | ---: | ---: | ---: |
| Kenrith | 15,955 | 122 | 3 / 119 | 15 / 15 → 17 / 17 |
| Judith | 6,111 | 45 | 3 / 42 | 7 / 7 → 9 / 9 |
| Krenko | 3,472 | 21 | 1 / 20 | 0 / 0 → 1 / 1 |

These counts are declarations, so a card can contribute more than one record. Kenrith covers 119 casting and three activation declarations in the selected families. The unresolved records include both unsupported predicates and dynamic native reductions. The new death-history findings are Bat-Cloud and Grim Reaper's Sprint where eligible. The legacy result rows and existing state-dependency records are unchanged. Kenrith, Judith, and Krenko retain respectively 14, 8, and 1 UNKNOWN death-history pathways with zero NO_MATCH pathways in these reports.

Merged application baseline `457ba2a` uses the same CLI sources and packaged inputs as the archived PR #56 reports at `7039f00249245fc1075e9941e3871e6055406fae`. The frozen cohort is at `c6ff6c5`. Candidate implementation and native tests were built at `4413c8be33e48fb94576c652f3bfdd8ee11ca10c` with clean Argentum `bd26b756b8b4d347f19e92bc77ef020919ebfa48`.

- All 18 controlled CLI comparisons passed. Existing results, state conditions, Class and counter evidence, eligibility and other shared records remained stable. Changes were limited to typed payment metadata, new spell actions where no spell action had been needed before, and qualifying payment results in death-history reports. Exports used `mtg-assistant discover --commander "<commander>" --objective <id> --json --output <path>`. The receipt is reproduced with `python scripts/compare-conditional-costs.py <baseline-dir> <candidate-dir> docs/evidence/conditional-cost-comparison.json`. [Comparison receipt](evidence/conditional-cost-comparison.json)
- The installed candidate read an archived PR #56 report through `audit coverage --snapshot <old-report>`. Both live Edge checks passed at 1365 and 390 px. The existing death-history panel showed Bat-Cloud's casting checkpoint and isolated `{1}{B}` amount while Tragic Slip and Gravelighter kept their resolution explanations.
- `bash scripts/verify.sh full` passed on the implementation revision with 716 application tests and zero failures, errors, or skips. The distribution and current catalog contract passed. The historical fingerprint check returned false, as at PR #56. The separate full engine and browser suites were outside this verification.
- The test-only revision `3a95084bbf1dd6718b7b2ee3959ec2964ea46a6d` passed `./dev test --tests '*ConditionalCostTest'`. The supplemental synthetic fixture confirmed that a reduction of the black symbol on one activation preserves its two generic mana and leaves the sibling activation unchanged.

A bounded next candidate is the fixed controlled-permanent filter at the casting checkpoint, exemplified by Academy Journeymage. It accounts for 18 unresolved Kenrith declarations and requires explicit caster and battlefield-filter binding. The 33 fixed target-dependent declarations need a separate check of chosen-target binding. Composite and alternative payments, cost floors and competing changes remain outside this isolated calculation. Complete printed-base payment fidelity keeps its existing scope.
