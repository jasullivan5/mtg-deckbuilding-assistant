# Class-level ability availability

The shared walker now attaches `ClassAvailability` to each identifiable higher-level action. It records the minimum level, source battlefield requirement, exact native level and ability paths, and the ordered mana costs of advancing from level one. Advancement has its own sorcery timing. An unlock cost is paid to advance, separately from any cost of the unlocked ability. The minimum-level check is cumulative. A structural finding describes a reachable prerequisite, with no claim about the current game state or available mana.

The typed record is carried by `ActionEvidence`. Trigger outcomes and other shared evidence join it by their existing action IDs. Counter facts also carry it so an isolated counter query can distinguish lower levels from a qualifying known level. An unknown or malformed progression remains unresolved. Ordinary higher-level triggers no longer lose confidence solely for requiring a supported Class level. Existing state-dependency and self-return gates retain their prior checks.

## Native coverage

The [frozen cohort](../src/test/resources/regression/class-availability-sources.json) records exact Oracle printings, the snapshot digest, normalized native-definition hashes and the source inventory. The authored inventory identified **23 Class definitions**, with **46 higher-level entries** declaring **30 triggered abilities, 15 static abilities, and 3 replacements**. No higher-level activated ability appeared in this inventory. The walker retains typed availability on an activated ability when the native script supplies one.

- **Innkeeper's Talent** uses the existing doubling interpreter for level three. Its controller must place the counters. Unlocking requires reaching level two for `{G}` and level three for `{3}{G}`. A separate positive placement remains required. Its level-two ward declaration is retained as unsupported static interpretation.
- **Stormchaser's Talent** uses the same level requirement on the level-three cast-triggered Otter outcome. The base entry token remains a separate action. Its level-two return is represented natively with a self-entry placeholder. Argentum's level-change dispatch can invoke that placeholder when the Class becomes level two, so the shared event interpreter keeps that trigger unresolved.
- **Caretaker's Talent and Fisher's Talent** retain native paths and level prerequisites for stat modification and token replacements. The increment does not project general static semantics or multiply token outcomes through those declarations.

Argentum creates a level-one component at entry. The native level-up activation checks the next level, uses that entry's mana cost, and requires sorcery timing. Its executor advances the component on resolution and refreshes replacement and continuous-effect components. The engine's effective ability lookups include earlier levels. [Native engine revision](https://github.com/jasullivan5/argentum-engine/tree/bd26b756b8b4d347f19e92bc77ef020919ebfa48)

## Verification

- Native execution cast Innkeeper's Talent and used the genuine paid level-up actions. The Class emitted level-change events for levels two and three. At levels one, two, and three, Travel Preparations emitted additions of one, one, and two counters on the same creature, for a final count of four. A separate creature received one counter from the opponent's Hunger of the Howlpack while the Class was level three. That recipient began without counters, so ward did not obstruct this control.
- The Stormchaser's Talent scenarios seeded native level-one and level-three states. Casting Unsummon yielded zero and one Otters respectively. These two scenarios exercise trigger availability from constructed states. Together with the three other focused checks, they passed `./dev test --tests '*ClassAvailabilityTest' --tests '*ClassAvailabilityScenarioTest'`. They establish bounded behavior and are separate from independent whole-card qualification.

### Controlled catalogs

| Commander | Identical eligible population | Eligible Classes / level entries / ability records | Counter MATCH cards / pathways | Counter UNKNOWN cards / pathways | Counter NO_MATCH cards / pathways |
| --- | ---: | ---: | ---: | ---: | ---: |
| Kenrith | 15,955 | 23 / 46 / 48 | 663 / 699 → 666 / 703 | 600 / 647 → 597 / 644 | 537 / 615 |
| Judith | 6,111 | 8 / 16 / 18 | 176 / 184 | 136 / 140 | 198 / 234 |
| Krenko | 3,472 | 5 / 10 / 11 | 83 / 88 | 62 / 64 | 110 / 136 |

Kenrith gains Innkeeper's Talent's level-three modifier on a card already matching through its base trigger. Three other cards move from UNKNOWN to MATCH for a placement. Builder's Talent requires level two and a qualifying noncreature, nonland entry. Its native batching trigger defaults to permanents the observer controls when the filter omits an explicit controller predicate. Does Machines can place three on an artifact creature you already control at level three. Its subsequent animation branch is a separate operation. Wizard Class can place one on a target creature you control when you draw after it reaches level three. [Builder's Talent](https://scryfall.com/card/blb/5/builders-talent) · [Does Machines](https://scryfall.com/card/tmt/34/does-machines) · [Wizard Class](https://scryfall.com/card/afr/81/wizard-class)

Six further Kenrith event-outcome records moved from UNKNOWN to MATCH at the shared confidence layer. They cover draw, damage, and tokens, including Stormchaser's Talent. Their existing objective admissions were stable. These six records and the four new counter matches count distinct relationship paths. Each Class ability is counted once, even when it remains available at subsequent levels. A card with multiple pathways can appear in several verdict totals.

All six objectives were compared in each population. The other five had identical admission verdicts across all three populations. Some base-level result annotations improved because unsupported children now attach to their own paths. The receipt records those changes separately and verifies all other evidence outside nested Class abilities. Kenrith's counter report has 46 Class-related unsupported entries, up from 41. Individual static and non-counter replacements, and unresolved level-transition trigger placeholders, are now visible at their own paths. This change in granularity does not measure a loss of supported relationships. [Comparison receipt](evidence/class-availability-comparison.json)

The merged application baseline is `c64bd2c2ef231ea623945a0a55b2be1993ec5c6f`. Its CLI sources and packaged inputs are identical to the archived PR #55 exports built at `e2d46e15ed8d145f1209fffd4bf80a37c893e18b`. The source cohort was frozen at `ba51c5d` and verified against the retained snapshot and native files. Implementation and scenario revision `7039f00249245fc1075e9941e3871e6055406fae` was verified with the unchanged clean engine `bd26b756b8b4d347f19e92bc77ef020919ebfa48`.

Export each commander and objective with `mtg-assistant discover --commander "<commander>" --objective <id> --json --output <path>` and compare using `python scripts/compare-class-availability.py <baseline-dir> <candidate-dir> docs/evidence/class-availability-comparison.json`.

- The built CLI read a PR #55 Class-containing report with `audit coverage --snapshot <old-report>`, preserving compatibility with entries that omit the new field.
- Two live Edge checks passed at 1365 and 390 px. The first browser run stopped at the card's default base ability. The rerun explicitly selected the replacement through the existing inspector. The selected explanation showed factor two, level three, both unlock payments, sorcery timing, and the placing-player restriction. The desktop capture shows the existing fallback for unverified Class imagery. [Discover screenshot](images/counter-placement/innkeepers-talent-1365.png)
- `bash scripts/verify.sh full` passed with 706 application tests and zero failures, errors, or skips. The distribution built and the current catalog contract passed. The historical fingerprint check returned false, as it did at PR #55. The separate full engine and browser suites were outside this verification.

## Remaining questions

Level transitions and persistent level-gated triggers have different checkpoints. A focused interpretation of the native transition placeholder would need to account for both dispatch sites and preserve uncertainty about its source fidelity. Class-level continuous effects, token replacement composition, and progression planning remain separate work.
