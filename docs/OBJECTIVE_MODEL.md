# Product direction and the three working models

This document records adopted product direction, the working discovery model, and the research direction for deck evaluation. Current capabilities are listed in the [README](../README.md#current-state-and-authority). Implementation details and evidence live in [Architecture](ARCHITECTURE.md) and [Known limitations](KNOWN_LIMITATIONS.md).

| Model | Core question |
| --- | --- |
| Card Integration | What does this card do, and how confidently do we understand it? |
| Composable Objective Discovery | Which cards relate to what the player wants to accomplish? |
| Deck Construction and Contextual Evaluation | How reliably and effectively does a particular deck accomplish those goals? |

Card Integration supplies native definitions, interpreted relationships, and evidence. Discovery queries that understanding for objective relevance. Deck evaluation uses it to study a particular configuration under explicit assumptions. Each downstream model reuses the same mechanical interpretation and Argentum rules.

**Near-term priority:** Expand trustworthy, reusable mechanical understanding and card integration. Substantial, measurable gains in what the application understands across the card pool are worthwhile before returning to major UI, composition, or deck-evaluation work. This priority sets no fixed sequence of milestones.

## A. Card Integration

Establish what cards do, recognize reusable relationships in their mechanics, and determine how much confidence the evidence supports.

- **Identity and native representation.** Bind the correct physical card and face, Oracle rules, characteristics, and source requirements. Represent abilities in the maintained Argentum engine, preserving costs, effects, conditions, targets, choices, timing, quantities, zones, ownership, control, and causal dependencies.
- **Shared mechanical interpretation.** Extract reusable facts and relationships independently of a particular objective. Examples include requirements, conditional outcome selection, parameter changes, event-to-operation relationships, and counter-placement replacement. Preserve the affected operation and its ability or branch, the condition or other prerequisite, the evaluation checkpoint, the quantity, restrictions, and exact native and source evidence. Develop relationship families through demonstrated cases. The same family can support several objective-relative contributions.
- **Verification and trust.** Distinguish catalog availability, native representability, fidelity to source, observed native execution, the application's structural interpretation, and independent qualification. Preserve provenance, the scope of each observation, and unresolved behavior. A native definition, a passed scenario, and a structural match each support a bounded claim. Whole-card qualification requires separate evidence.

Interpretation is outcome-neutral. A condition may select, enable, or suppress an operation regardless of whether the outcome helps a deck. Keep a direct dies trigger separate from a check of earlier death history, and a replacement of an impending operation separate from a response to the completed event. A Treasure token can be a later input to mana production. Creating it records a token operation. The subjects of such mechanics include cards, tokens, counters, players, resources, and permissions, each with its own properties and rules.

Improvements to shared interpretation should add understanding across previously integrated cards as well as future objectives. [Building-block research](OBJECTIVE_BUILDING_BLOCKS.md) · [Conditional causal evidence](CONDITIONAL_CAUSAL_DISCOVERY.md) · [Triggered operations evidence](TRIGGERED_OPERATIONS_DISCOVERY.md) · [Translation assurance](TRANSLATION_RELIABILITY.md)

## B. Composable Objective Discovery

Let players define what they want to accomplish and discover cards and commanders with relevant mechanical relationships. The working model has two component types:

- A **focused objective** finds rules-text-supported causal contributions to one precise event, state, or modification.
- An **attribute selector** finds candidates by characteristics such as type, subtype, color, mana cost, name, and printed power or toughness.

| Focused anchor | Contribution modes |
| --- | --- |
| Event | Generates the event, responds to the event, supplies inputs |
| State | Contributes to the state, depends on the state |
| Modification | Provides the modification, is subject to the modification |

These modes describe contributions relative to the chosen anchor and can overlap on one card or ability. The shared relationship families supply the detailed evidence. For example, original-creature self-return can supply an input to a later death. A direct instruction can initiate counter placement. Hardened Scales modifies a separate qualifying placement. Placing a counter, changing power, and reaching a power threshold ask different mechanical questions. Shared eligibility for an objective establishes neither a complete interaction nor contextual value. [Creature-input evidence](CREATURE_INPUT_DISCOVERY.md) · [Counter placement evidence](COUNTER_PLACEMENT_DISCOVERY.md)

### Focused admission and basic lands

Adopted product policy requires at least one qualifying causal relationship supported by the card's rules text. Printed characteristics can establish whether a rules-text mechanic applies. Characteristics alone cannot qualify a card, and an unrelated printed ability does not supply the missing relationship. Apply no further categorical exclusion based on card strength, ordinary mechanics, interaction, or assumed strategic purpose.

Explicit product policy excludes basic lands from all discovery results, including attribute-selector results. A future deck assembler can still use basic lands in a legal mana base.

Today's fixed queries use their existing objective-specific policies, including the specialized creature-death admission and its historical removal pathway. The discovery-wide basic-land exclusion is an adopted policy for future implementation. PRs #48 and #49 merged their research records. Earlier role proposals, distinctive-contribution criteria, ordinary-capability and interaction hypotheses, and experimental selector results retain their original findings and evidence. [Contribution research](CONTRIBUTION_VALIDATION.md) · [Admission assessment](OBJECTIVE_ADMISSION_PRINCIPLES.md) · [Selection and composition experiment](SELECTION_COMPOSITION.md)

### Composition

- **Include** adds matching candidates.
- **Require** restricts the result to candidates that satisfy the component.
- **Exclude** removes matching candidates.

The intended result is **(Include A OR Include B) AND Require C AND Require D, excluding E**. Preserve candidate identity, overlapping inclusion reasons, uncertainty, and provenance across the operations. The historical research CLI demonstrated a bounded union and restriction. General production composition, objective construction, and attribute selection remain future capabilities.

### Commander exploration

Commander-first and objective-first are equally valid entry points in the intended system.

- **Commander-first** starts with a commander, inspects its recognized mechanics, and explores related objectives and selectors.
- **Objective-first** starts by constructing objectives and discovering relevant cards and commander candidates, then selects a commander.

Selecting a commander adds Commander legality and color-identity constraints while preserving the underlying objectives. Selection is optional during exploration and required before assembling a complete Commander deck. Guided exploration should suggest connections grounded in recognized mechanical capabilities. A shared noun or complementary candidate list is insufficient evidence for a card-to-card interaction.

Today Discover uses a selected commander and six fixed objectives, including the merged counter-placement query. Objective-first commander discovery and guided suggestions remain future work. [Current objective inventory](../README.md#current-objectives)

## C. Deck Construction and Contextual Evaluation

The research direction is to construct provisional decks and investigate how reliably and effectively each configuration meets the chosen objectives. The methods below are proposals for investigation.

- **Initial deck assembly.** Use a selected commander, composed objectives, discovered candidates, necessary resources, and transparent construction heuristics. Players can supply an existing deck in place of automatic initial assembly. Either starting point is provisional.
- **Contextual evaluation.** Investigate probabilities and resource access, simple explicit sequencing policies, and optional player-supplied sequencing profiles for priorities at the deck, objective, card, or card-combination level. Use isolated native experiments and controlled pressure or disruption tests. Measure consistency, timing, objective progress, efficiency, resilience, and uncertainty under stated conditions.
- **Iterative refinement.** Identify bottlenecks and compare substitutions or alternative configurations under matched assumptions. Account for what a displaced card contributed, and refine the deck using measured results within that context.

Favor simple statistical methods, controlled native experiments, and interpretable heuristics. The research can proceed without sophisticated learning models, AI API dependencies, full multiplayer simulation, a mandatory pairwise synergy graph, or a universal card rating. The existing Lab observes curated native positions. Its evidence and the proposed methods have not been validated as predictors of actual Commander performance. [Mechanical Verification Lab](MECHANICAL_VERIFICATION_LAB.md) · [Known limitations](KNOWN_LIMITATIONS.md)
