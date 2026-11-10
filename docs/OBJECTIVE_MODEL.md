# Objective and evaluation direction

This is the living product-direction record, not authorization to implement future capabilities. The original distinctive-contribution direction was adopted on 2026-11-02. The 2026-11-09 [bounded building-block assessment](OBJECTIVE_BUILDING_BLOCKS.md) refines the earlier six-role proposal: use mechanically precise subjects and directional relationships as the working foundation, with role labels as optional descriptions. This is supported as a research/explanation direction in the retained sample, not a validated universal schema or implemented objective builder. Earlier [four-/five-role research](CONTRIBUTION_VALIDATION.md) retains its original evidence and disagreements. [README](../README.md#current-state-and-authority) owns current status; [architecture](ARCHITECTURE.md) describes implementation.

## Conceptual architecture

**Card identity and rules → Native definitions → Mechanical facts and capabilities → Objective discovery and selective layering → Experimental deck evaluation**

The foundation progressively establishes physical identity, rules and source requirements; Commander eligibility; native representability and execution boundaries; and reusable costs, effects, conditions, events and state changes. Objectives interpret capabilities for a question. Availability, representability, source fidelity, execution, discovery and objective relevance remain separate claims. Representation does not establish understanding of every card or interaction.

This is a conceptual direction, not a new module plan. Four bounded queries exist; general relationship filters, objective construction, selective composition and deck evaluation do not. The death query retains its legacy death/removal projection and adds shared triggered event-to-draw/token and intrinsic original-creature self-return relationships; the other objectives consume shared facts. No extraction or engine rewrite follows from this diagram.

## Focused anchors and mechanical relationships

The current working hypothesis investigates one focused **event, state or modification** anchor per objective. Mechanics can produce/establish it, supply required inputs, or benefit from/enhance it; these relationships overlap. Game elements describe subjects, requirements and consequences; effects/outcomes connect them. Hardened Scales can depend on a counter-placement opportunity while modifying it through replacement, rather than a triggered ability. These anchors are neither exclusive native categories nor proof that every desired goal has one anchor. Bounded intrinsic original-creature self-return is now an input-supply contribution to the death objective. General resource-provider discovery and composition remain future work.

The earlier subject assessment below remains useful background; game elements are not required to be an equivalent anchor category.

| Working concept | Objective-level question | Mechanics to preserve |
| --- | --- | --- |
| Events | What should happen, or happen repeatedly? | Cause/enable, respond to, prevent, or check history of a qualifying occurrence; a trigger and a history check are different. |
| States | What condition should hold, and when? | Establish/maintain/change a predicate, or depend on it for access, a branch or magnitude. Preserve the checkpoint or continuous evaluation. |
| Game elements | What mechanically relevant thing or resource do we need to supply or use? | Supply, replenish, preserve, consume, target, move or transform it; retain type, quantity, identity, owner/controller, zone and permissions. |
| Effects and outcomes | What operation connects inputs and conditions to a possible result? | Counter placement, movement, draw, damage, life changes and modifications have different semantics and possible consequences. An effect is not defined by whether it helps win. |

These concepts provide useful descriptions of an anchor and its requirements. Effects/outcomes supply connecting vocabulary, not necessarily an equivalent fourth objective-subject category. A requested operation such as *put +1/+1 counters on my creatures* is also a useful precise criterion. These concepts overlap: an effect can produce an event and establish a state. They are not four exclusive card types or an adopted software schema.

**Game elements** is provisional and broader than Magic's technical game objects. A counter is not a token; a player, mana quantity, characteristic, permission and zone do not share one object's lifecycle. Do not erase those differences to fit the umbrella term.

Use one ability/branch as the explanatory unit. Pitiless Plunderer's qualifying death starts a trigger whose output is Treasure; the token is a possible later input to mana production, not mana already produced by that trigger. Abzan Falconer's outlast places a counter, while its separate static ability gives flying to qualifying counter-bearing creatures. A card-wide label cannot substitute for either chain.

## State dependence without a role for every condition

Ask whether establishing the chosen condition makes a mechanic available, more frequent or more effective. This can unite discovery questions about requirements and leveraging without equating their mechanics. Phoenix requires power 4 at combat's beginning and again on resolution; Swarm's six-land condition selects output at resolution; Plating continuously counts artifacts. Garruk's Packleader tests an entering creature's power for triggering, without rechecking it at resolution. Threshold, amount, duration, recipients and evaluation time remain necessary even if all are described as state dependence.

The counter comparison separates *place +1/+1 counters*, *increase power* and *control power 4*. A continuous Goblin bonus can support the latter two without placing counters. Scales modifies another placement; it does not initiate one. A 0/-1 counter does not raise power. A large body can establish a threshold without boosting a creature already present. These distinctions improve the source-defined research filters; no new query or general state solver is implemented. [The assessment](OBJECTIVE_BUILDING_BLOCKS.md) records entry-counter and observation-window ambiguities rather than inventing a universal rule.

## Distinctive contributions

Objectives can concern events, resources, states, capabilities, prerequisites or strategic outcomes. A card belongs in primary discovery when its own mechanics provide a distinctive, recognizable contribution to the objective or a necessary resource or capability. It need not independently accomplish the final event. Normal costs, targets, timing and resource requirements do not invalidate that contribution.

Reassembling Skeleton and Bloodghast replenish nontoken creature resources. That is a distinctive contribution to a death strategy even though the return itself neither causes nor observes a death. A vanilla creature supplies ordinary material without an intrinsic advantage in sustaining that strategy. An ordinary land does not gain a distinctive death contribution merely because another effect could animate it.

Keep mechanical possibility, required setup, objective-policy relevance, strategic contribution and contextual deck value separate. Policy exclusion is not mechanical impossibility; MATCH is not a deck recommendation. Printed overlapping types differ from externally established overlaps. The [Agent of Shauku / Akki Scrapchomper primary-inclusion boundary](OBJECTIVE_COVERAGE.md#primary-inclusion-decision-still-open) remains open.

## Admission hypotheses under review

The [general admission study](OBJECTIVE_ADMISSION_PRINCIPLES.md) investigates, rather than adopts, two further rules: ordinary eligibility alone is insufficient evidence of input supply, and interaction-only relevance might be insufficient for primary build-around discovery. Its recommendation is narrower supplier evidence plus objective-relative judgments, not a universal ordinary-participation exclusion or interaction classifier. A printed body can directly establish a power/count state; one operation can both interact and produce an objective event. Whether a particular contribution belongs in primary discovery remains separate from its mechanical compatibility. Existing death-generation and enhanced-removal policies are unchanged, including Tragic Slip's current admission.

## Working objective-relative roles

The preceding six-role proposal used the descriptions below. Retain them as optional, objective-relative summaries of relationships, not foundational categories requiring every card to be classified. The bounded comparison found useful communication cues but no demonstration that adding labels supplies the underlying mechanical information or improves user comprehension. No final numerical taxonomy is adopted.

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

Event versus history, state access versus scaling, and modification versus facilitation still need precise relationships. Exploration's extra land-play permission remains an example where two role labels describe one change. Roles may summarize a relationship after it is established; they cannot decide source fidelity, objective admission or contextual value.

## Directional filtering and selective layering

An intended capability is selecting contributions by how they relate **within** an objective. A creature-death plan might borrow creature-producing effects from a token objective without taking every mechanic that benefits from tokens. Another plan might seek only responses to a complementary event. Role words can be useful display shorthand, but the selection needs the underlying operation and direction, not a permanent card category.

The application might eventually suggest complementary questions from a mechanic's requirements: establishing a power threshold could enable Phoenix, for example. That is a hypothesis about connecting available capabilities, not evidence of a tested two-card interaction. Neither general directional filtering, selective layering nor automatic suggestions are implemented.

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

The four queries are listed in [README](../README.md#current-objectives). The death query recognizes selected death sources, supported triggered draw/token responses, intrinsic original-creature self-return inputs and death-enhanced removal. Life-change and general payoff discovery remain unsupported. Removal's privileged treatment reflects historical query design, not a permanent principle. Skeleton/Bloodghast return relationships do not prove repeatability, a complete sacrifice/return cycle or useful execution.

The earlier renewable-creature proposal was implemented in the bounded [creature-input increment](CREATURE_INPUT_DISCOVERY.md), retaining original-card/action/branch binding, zones, controller, optionality, payment and limits. Walking Corpse remains ordinary material; token copies and return-to-hand do not qualify. Unsupported requirements and Wolfbat's finality limit remain unresolved for death input supply. This does not establish a general provider classifier or indefinite fuel.

The [source-tap correction](TAP_PAYMENT_CORRECTION.md) merged separately in engine PR #7 and application PR #44; its original receipts remain revision-bound. This increment changes no mechanics or payment evidence. A bounded shared **triggered event-to-draw/token outcome relationship** is now implemented and consumed by the death objective. A subsequent bounded intrinsic creature-input increment consumes original-card self-return relationships; this does not implement general replacement interpretation, all input providers, objective construction or composition. [Known limitations](KNOWN_LIMITATIONS.md) owns current boundaries; historical follow-ups remain evidence, not an automatic implementation queue.
