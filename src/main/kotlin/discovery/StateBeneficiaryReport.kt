package dev.mtg.assistant.discovery

import dev.mtg.assistant.Catalog
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.wingedsheep.sdk.scripting.effects.Effect

internal fun stateBeneficiaryReport(catalog: Catalog,commander: String,identity: List<String>,query: String?,
    eligibility: List<EligibilityEvidence>,counts: Map<String,Int>,extracted: PathwayExtractor,inputCards: List<String>): DiscoveryReport {
    val byName=eligibility.associateBy { it.card }
    val results=extracted.stateDependencies.filter { it.scope==StateScope.DEATH_HISTORY }.map { d ->
        val decisions=stateDependencyDecisions(d,StateAnchor.DeathHistory())
        val verdict=all(decisions.map { it.verdict })
        val c=checkNotNull(catalog.card(d.card))
        val native=nativeJson.encodeToJsonElement(Effect.serializer(),d.whenTrue)
        ResultEvidence(card=d.card,identifier=c.oracleId ?: "${c.setCode}:${c.metadata.collectorNumber}:${c.name}",face=d.face,
            actionId=d.actionId,abilityPath=d.outcomePath,branch=Contribution.STATE_BENEFICIARY,verdict=verdict,
            operation=native.jsonObject.getValue("type").jsonPrimitive.content,costOrEffect="STATE_CHECK",
            affectedScope=d.condition.description,prerequisites=d.prerequisites,dependency=d.condition.description,
            nativeOperation=native,decisions=decisions,
            sourceReferences=listOfNotNull(DefinitionSources.paths[d.card]?.let { "$ARGENTUM_REPOSITORY_URL/blob/$ARGENTUM_SHA/$it" }),
            commanderLegality=byName.getValue(d.card).format,deckEligibility=byName.getValue(d.card).deck,
            provenance=catalog.provenance(d.card),contributionClassification=Contribution.STATE_BENEFICIARY.takeIf { verdict==Verdict.MATCH },
            mechanicalCompatibility=verdict,enablingRelationship="STATE_CHECK_CONTROLS_SAME_ACTION_OUTCOME",
            localSupport=if(verdict==Verdict.UNKNOWN || extracted.unsupported.any { it.actionId==d.actionId }) SupportStatus.PARTIALLY_SUPPORTED else SupportStatus.SUPPORTED,
            stateDependency=d)
    }
    val matches=results.filter { it.verdict==Verdict.MATCH }
    return DiscoveryReport(objective=DEATH_HISTORY_OBJECTIVE,commander=commander,allowedIdentity=identity,conventionalQuery=query,
        counts=counts+mapOf("accepted_state_beneficiary_pathways" to matches.size,"accepted_state_beneficiary_cards" to matches.distinctBy { it.card }.size,
            "unknown_query_pathways" to results.count { it.verdict==Verdict.UNKNOWN },"unsupported_structure_records" to extracted.unsupported.size,
            "cards_with_unsupported_structures" to extracted.unsupported.distinctBy { it.card }.size),
        eligibility=eligibility,results=results,actions=extracted.actions,unsupported=extracted.unsupported,
        limitations=discoveryLimitations+listOf("Supported native history checks control a branch or availability. Direct dies triggers and ways of establishing history are separate.",
            "State checks and branches are structural. Native costs, targets, timing, restrictions and actual state still apply.",
            "Compound conditions, conditional discounts, replacement effects and other unsupported check locations are not fully interpreted."),
        structuralSupport=extracted.structuralSupport,facts=extracted.facts,mechanicalInputCards=inputCards,
        translationAssurance=eligibility.mapNotNull { catalog.translationAssurance(it.card) },
        eventOutcomes=extracted.eventOutcomes,selfReturns=extracted.selfReturns,stateDependencies=extracted.stateDependencies)
}
