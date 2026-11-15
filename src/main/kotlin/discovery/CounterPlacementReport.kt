package dev.mtg.assistant.discovery

import com.wingedsheep.sdk.core.CounterType
import dev.mtg.assistant.Catalog

internal fun counterPlacementReport(catalog: Catalog, commander: String, identity: List<String>, query: String?,
    eligibility: List<EligibilityEvidence>, counts: Map<String, Int>, extracted: PathwayExtractor, inputCards: List<String>): DiscoveryReport {
    val byName = eligibility.associateBy { it.card }
    val recipients = Requirements(creature = true, controller = Controller.YOU)
    val question = CounterQuestion(CounterType.PLUS_ONE_PLUS_ONE, recipients)
    fun row(origin: CounterAction, branch: Contribution, decisions: List<Decision>,
            placement: CounterPlacement? = null, modifier: CounterPlacementModifier? = null): ResultEvidence {
        val card = checkNotNull(catalog.card(origin.card))
        val eligible = byName.getValue(origin.card)
        val provenance = catalog.provenance(origin.card)
        val verdict = all(decisions.map { it.verdict })
        return ResultEvidence(origin.card, card.oracleId ?: "${card.setCode}:${card.metadata.collectorNumber}:${card.name}",
            origin.face, origin.actionId, origin.path, branch, verdict,
            if (placement != null) "PLACE_COUNTERS" else "MODIFY_COUNTER_PLACEMENT",
            if (placement?.timing == PlacementTiming.ENTRY) "ENTRY" else if (placement != null) "EFFECT" else "REPLACEMENT",
            placement?.recipient?.filter?.description ?: modifier?.pattern?.description.orEmpty(),
            origin.prerequisites, modifier?.pattern?.description, origin.native, decisions,
            listOfNotNull(DefinitionSources.paths[origin.card]?.let { "$ARGENTUM_REPOSITORY_URL/blob/$ARGENTUM_SHA/$it" },
                if (provenance.origin == "APPLICATION_EXTENSION") provenance.source else "native:${origin.face}.${origin.path}"),
            eligible.format, eligible.deck, contributionClassification = branch.takeIf { verdict == Verdict.MATCH },
            provenance = provenance, mechanicalCompatibility = verdict, policyAcceptance = Verdict.MATCH,
            enablingRelationship = if (placement != null) "INITIATES_COUNTER_PLACEMENT" else "REPLACES_AN_IMPENDING_COUNTER_PLACEMENT",
            localSupport = if (verdict == Verdict.UNKNOWN) SupportStatus.PARTIALLY_SUPPORTED else SupportStatus.SUPPORTED,
            counterPlacement = placement, counterModifier = modifier)
    }
    val results = extracted.counterPlacements.map {
        row(it.action, Contribution.COUNTER_PLACEMENT, counterPlacementDecisions(it, question.type, recipients), placement = it)
    } + extracted.counterModifiers.map {
        row(it.action, Contribution.COUNTER_MODIFICATION, counterModifierDecisions(it, question), modifier = it)
    }
    val matches = results.filter { it.verdict == Verdict.MATCH }
    return DiscoveryReport(objective = COUNTER_OBJECTIVE, commander = commander, allowedIdentity = identity, conventionalQuery = query,
        counts = counts + mapOf("accepted_counter_pathways" to matches.size,
            "accepted_counter_cards" to matches.distinctBy { it.card }.size,
            "unknown_query_pathways" to results.count { it.verdict == Verdict.UNKNOWN }),
        eligibility = eligibility, results = results, actions = extracted.actions, unsupported = extracted.unsupported,
        limitations = discoveryLimitations + listOf(
            "Supported placement instructions and bounded additive replacement declarations. A modifier requires a separate qualifying placement.",
            "Compound amounts, recipient bindings and replacement ordering can remain unresolved. These results do not establish gameplay or a card pairing."),
        structuralSupport = extracted.structuralSupport, facts = extracted.facts, mechanicalInputCards = inputCards,
        translationAssurance = eligibility.mapNotNull { catalog.translationAssurance(it.card) },
        eventOutcomes = extracted.eventOutcomes, selfReturns = extracted.selfReturns)
}
