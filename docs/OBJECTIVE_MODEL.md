# Objective direction — decision record v1

Adopted by Product Studio on 2026-11-02. This records direction, not authorization for a new objective engine. M1 remains a bounded implementation awaiting hands-on acceptance.

## Distinctive contributions

Objectives can concern events, resources, states, capabilities, prerequisites and strategic outcomes. A card belongs in primary discovery when its own mechanics provide a distinctive, recognizable contribution to the objective or a necessary resource or capability. It need not independently accomplish the final event. Normal costs, targets, timing and resource requirements do not invalidate that contribution.

Reassembling Skeleton and Bloodghast intrinsically replenish nontoken creature resources. They are direct resource contributors to a death strategy, despite neither causing nor observing a death through that recursion. A vanilla creature supplies a body but no distinctive mechanical advantage for sustaining the strategy. An ordinary land does not acquire a distinctive creature-death contribution merely because another effect could animate it.

Mechanical participation and resource qualities are different. Producers/replenishers supply resources; facilitators enable actions; consumers spend resources; observers respond to events. These roles can overlap. Viscera Seer consumes a creature and facilitates a death; scry does not make it a death observer. Judith observes a death without consuming the event: several triggers can observe the same event. Renewability, availability across zones, repeatability, flexibility, persistence and restrictions describe resource qualities, not additional participation roles. This is explanatory vocabulary, not an implemented universal taxonomy.

Keep five claims separate: mechanical possibility, required setup, objective-policy relevance, strategic contribution and contextual deck value. Policy exclusion is not mechanical impossibility; a structural MATCH is not a deck recommendation. Printed overlapping permanent types differ from externally created overlaps. The specific primary-inclusion boundary for noncreature type restrictions remains a separate decision; see [the investigation](OBJECTIVE_COVERAGE.md#sacrifice-type-boundary).

## Existing implementation versus direction

M1's death query still recognizes selected death sources and death-enhanced removal. It does not generally discover death-triggered draw, tokens, Treasure or life changes, or self-recursion as creature-death resource replenishment. This is implementation coverage, not a strategic judgment that these contributions do not matter. Removal's privileged treatment is historical query design, not a permanent product principle. Objective names must not disguise those gaps.

The existing native actions retain costs, source zones, triggers and effects. Shared movement facts already represent Skeleton's and Bloodghast's graveyard-to-battlefield return; the separate reanimation objective can inspect that movement. Those facts do not by themselves prove repeatable fuel, a complete causal chain or useful game execution. Current unsupported instructions and uncertainty remain available.

## Small future experiment — renewable creatures

Not implemented or authorized by this record. Ask whether native self-return mechanics can identify distinctive renewable nontoken creature resources without retrieving ordinary bodies or arbitrary constructed synergies. Use Skeleton and Bloodghast as source-bound positive controls, Walking Corpse as ordinary-resource control, and token copies, return-to-hand, one-use/exile costs, opponent-controlled entry and unsupported conditions as adversarial controls.

Correlate the original self card, graveyard source, battlefield entry, controller, optionality, costs, placement and native trigger/activation within the same action using existing facts and constraints. Preserve landfall and mana as prerequisites, not research defects. Wolfbat’s source-recorded finality counter is a useful control against assuming every return supplies indefinitely reusable fuel. Establish source expectations and compare actual candidate/pathway changes before selecting admission policy. No new engine, card-name exceptions, Oracle parser, strategic score or generic role classifier is implied.

## Composable investigation

Future objectives can investigate creature deaths, land animation and land sacrifice separately. **Grouping** retains each objective's candidates, relationships, sources and limitations. **Connecting** requires evidence joining the same objects, types, resources, costs, zones, timing and causal conditions. Grouping and shared keywords alone establish no valid interaction.

User-composed strategies and system-assisted discovery may complement each other; the user need not necessarily author every complementary objective. Future filters may expose operations, participation or demonstrated resource qualities. M1 retains its mechanic filter. Objective authoring, role filters, composition, dependency graphs and automatic combinations remain outside this increment; no M2 continuation is authorized.
