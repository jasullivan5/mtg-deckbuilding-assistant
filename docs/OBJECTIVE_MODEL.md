# Objective and evaluation direction

This is the living product-direction record, not authorization to implement future capabilities. The original distinctive-contribution direction was adopted on 2026-11-02. The 2026-11-09 Product Studio assignment updates the conceptual architecture, working roles and evaluation hypothesis below. Earlier vocabulary studies remain [historical research](CONTRIBUTION_VALIDATION.md); their sources and conclusions are preserved. [README](../README.md#current-state-and-authority) owns current status; [architecture](ARCHITECTURE.md) describes implementation.

## Conceptual architecture

**Card identity and rules → Native definitions → Mechanical facts and capabilities → Objective discovery, subroles and composition → Experimental deck evaluation**

The foundation progressively establishes physical identity, rules and source requirements; Commander eligibility; native representability and execution boundaries; and reusable costs, effects, conditions, events and state changes. Objectives interpret capabilities for a question. Availability, representability, source fidelity, execution, discovery and objective relevance remain separate claims. Representation does not establish understanding of every card or interaction.

This is a conceptual direction, not a new module plan. Four bounded queries exist; general role classification, role filters, selective composition and deck evaluation do not. The death query retains a legacy projection; the other objectives consume shared facts. No extraction or engine rewrite follows from this diagram.

## Distinctive contributions

Objectives can concern events, resources, states, capabilities, prerequisites or strategic outcomes. A card belongs in primary discovery when its own mechanics provide a distinctive, recognizable contribution to the objective or a necessary resource or capability. It need not independently accomplish the final event. Normal costs, targets, timing and resource requirements do not invalidate that contribution.

Reassembling Skeleton and Bloodghast replenish nontoken creature resources. That is a distinctive contribution to a death strategy even though the return itself neither causes nor observes a death. A vanilla creature supplies ordinary material without an intrinsic advantage in sustaining that strategy. An ordinary land does not gain a distinctive death contribution merely because another effect could animate it.

Keep mechanical possibility, required setup, objective-policy relevance, strategic contribution and contextual deck value separate. Policy exclusion is not mechanical impossibility; MATCH is not a deck recommendation. Printed overlapping types differ from externally established overlaps. The [Agent of Shauku / Akki Scrapchomper primary-inclusion boundary](OBJECTIVE_COVERAGE.md#primary-inclusion-decision-still-open) remains open.

## Working objective-relative roles

The preferred vocabulary has six role concepts. These descriptions guide research; they are not a complete formal ontology or implemented classification criteria.

| Role | Working question relative to an objective |
| --- | --- |
| Provider | What relevant resource or capability does this mechanic supply or replenish? |
| Event responder | How does it respond to an event relevant to this objective? |
| Event facilitator | How does it help the event happen? |
| State facilitator | How does it help establish or maintain a needed condition? |
| State responder | How does it use or benefit from an established condition? |
| Modifier | How does it change a relevant characteristic, permission or effect? |

A card or ability can have multiple roles, and the same capability can serve different objectives differently. Viscera Seer illustrates facilitating a death by consuming a creature. Grim Haruspex illustrates responding to a death and providing cards. These explanatory examples do not assert that today's death query discovers all these roles.

Earlier producer/replenisher language describes resource supply and informs Provider. Consumer describes a cost/use relationship, not a seventh role; facilitator and observer language motivate more specific event/state questions without a one-to-one relabeling. Renewability, availability across zones, repeatability, flexibility, persistence and restrictions remain resource qualities. The historical Leverage State proposal explored state dependence; it is evidence to examine when distinguishing State responder, State facilitator and Modifier, not an automatic synonym or mandatory layer.

Role boundaries, overlapping classifications, event triggers versus history checks, state access versus scaling, and modification versus facilitation still require source-grounded mechanical research and validation. Do not turn unresolved distinctions into universal labels or acceptance algorithms merely to complete the vocabulary.

## Subroles and selective layering

An intended capability is filtering contributions by role **within** an objective. A creature-death plan might use a creature-token objective for its creature-resource providers without including the token objective's event responders. Another plan might seek only responders from a complementary objective. Combining every match from both queries would conceal that distinction.

The user may eventually layer selected roles across objectives; the application may suggest complementary questions by recognizing requirements, available capabilities and missing roles. Neither subrole filtering, selective layering nor automatic suggestions are implemented.

**Grouping** retains each objective's candidates, sources and uncertainty. **Connecting** requires evidence joining appropriate sources, objects, resources, owners/controllers, zones, timing, conditions, costs, effects and causal branches. Shared keywords or membership in complementary objectives prove neither an individual interaction nor strategic value. Mechanical dependencies remain necessary even though a dedicated pairwise strategic synergy model is not currently required.

## Experimental deck evaluation hypothesis

We want to investigate how far trustworthy mechanics, objectives and selective composition can take us through empirical evaluation without a separate card-to-card strategic synergy abstraction. That abstraction is not a prerequisite in the current direction; experimentation may later establish a worthwhile need. This is an active hypothesis, not proof that sophisticated piloting can emerge without strategic guidance.

Future experiments may combine:

- Probability and resource-access analysis.
- Objective-aware piloting and native game simulation where sufficiently supported.
- Controlled deck substitutions and comparisons against defensible baselines.
- Pressure and disruption tests.
- Measurements of objective progress, resource efficiency, resilience, competitive outcomes and uncertainty.

Simulation is neither implemented deck evaluation nor an inherently authoritative substitute for validating mechanics. Results depend on rules fidelity, decision policies, objectives, baselines and environment. Bounded curated Lab executions observe specified native positions; they do not establish representative deck performance. No simulator, pilot, optimization system or strategic score is authorized by this record.

## Implemented coverage and a retained future experiment

The four queries are listed in [README](../README.md#current-objectives). The death query recognizes selected death sources and death-enhanced removal. It does not generally discover death-triggered draw, tokens, Treasure, life changes or self-recursion as renewable death fuel. Removal's privileged treatment reflects historical query design, not a permanent principle. The reanimation objective can inspect Skeleton/Bloodghast returns; those facts do not prove repeatability, a complete causal chain or useful execution.

The earlier renewable-creature experiment remains unimplemented and needs a separate assignment: test native self-return as distinctive nontoken creature supply. Keep Skeleton/Bloodghast as positive controls, Walking Corpse as ordinary material, and token copies, return-to-hand, one-use/exile costs, opponent-controlled entry and unsupported conditions as adversarial controls. Correlate the original card, graveyard source, battlefield entry, controller, optionality, payment, placement and trigger/activation within the same action. Preserve landfall and mana as prerequisites; Wolfbat's retained finality-counter evidence guards against assuming indefinite fuel. This does not imply a new interpreter, card-name exceptions or general role classifier.

The immediate mechanical priority remains the separately scoped native Explicit tap-payment correction identified in [PR #42's examination](PRODUCT_STUDIO_COST_EXAMINATION.md). It is not combined with this documentation/Primer increment. [Known limitations](KNOWN_LIMITATIONS.md) owns current boundaries; historical studies retain their observations and proposed follow-ups without becoming an implementation queue.
