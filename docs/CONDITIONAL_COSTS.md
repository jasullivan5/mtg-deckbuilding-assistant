# Conditional casting and activation costs

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
