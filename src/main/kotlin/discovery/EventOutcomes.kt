package dev.mtg.assistant.discovery

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** This increment interprets triggered branches, not every possible event beneficiary.
 * Native replacement/prevention mechanisms are not represented by absence of these records. */
@Serializable data class EventContext(
    val trigger: EventPattern, val binding: TriggerBinding, val activeZones: Set<Zone>,
    val sourceIsCreature: Boolean, val nativeAbility: JsonElement,
    val confidence: Verdict, val requirements: List<String>
)
@Serializable enum class OutcomeKind { DRAW_CARDS, CREATE_TOKEN }
@Serializable data class EventOutcome(
    val id: String, val card: String, val face: String, val actionId: String, val path: String,
    val event: EventContext, val kind: OutcomeKind, val quantity: DynamicAmount,
    val recipient: String, val confidence: Verdict, val positiveQuantity: Verdict,
    val optional: Boolean, val prerequisites: List<String>, val native: JsonElement,
    val token: TokenCreation? = null, val factId: String? = null,
    val mechanism: String = "TRIGGERED_ABILITY", val linkage: String = "SAME_TRIGGER_BRANCH",
    val unresolved: List<String> = emptyList()
)

/** An event intersection, not a game-state or trigger-frequency solver. Controller/owner
 * refer to the event object, relative to the ability controller; they are independent. */
data class ZoneEventRequirement(val from: Zone? = null, val to: Zone? = null,
    val creature: Boolean = true, val controller: Controller? = null,
    val token: TokenStatus? = null, val owner: Controller? = null)

fun eventIntersection(context: EventContext, requirement: ZoneEventRequirement): Verdict {
    if (context.trigger is EventPattern.AnyOf) return any(context.trigger.events.map { eventIntersection(context.copy(trigger=it), requirement) })
    val event=context.trigger as? EventPattern.ZoneChangeEvent ?: return if (potentialUninterpretedDeath(context.trigger)) Verdict.UNKNOWN else Verdict.NO_MATCH
    fun zone(actual: Zone?, required: Zone?) = if(required==null || actual==null || actual==required) Verdict.MATCH else Verdict.NO_MATCH
    val zones=all(listOf(zone(event.from,requirement.from),zone(event.to,requirement.to),
        if(requirement.to!=null && event.excludeTo==requirement.to) Verdict.NO_MATCH else Verdict.MATCH))
    if(zones==Verdict.NO_MATCH) return zones
    // SELF is controlled by the source's controller, not an arbitrarily chosen opponent.
    if(context.binding==TriggerBinding.SELF && requirement.controller==Controller.OPPONENT) return Verdict.NO_MATCH
    val actor=if(context.binding==TriggerBinding.SELF) "YOU" else requirement.controller?.name ?: "ANY"
    // Reuse the correlated filter evaluator. Its owner alternatives preserve OR/AND and
    // owner versus controller; the event witness has the requested controller.
    val witness=MechanicalFact("event","", "", "", "",FactKind.OTHER_OPERATION,"EVENT","EVENT",
        event.filter,sourceZone=if(event.from==Zone.BATTLEFIELD || event.to==Zone.BATTLEFIELD || requirement.from==Zone.BATTLEFIELD || requirement.to==Zone.BATTLEFIELD) Zone.BATTLEFIELD else null,
        sourcePlayer=actor,native=context.nativeAbility)
    val filter=witness.compatible(FactConstraints(creature=requirement.creature,token=requirement.token,owner=requirement.owner))
    return all(listOf(zones,filter,context.confidence,
        if(context.binding==TriggerBinding.SELF && requirement.creature && !context.sourceIsCreature) Verdict.UNKNOWN else Verdict.MATCH,
        if(event.excludeSacrifice || event.requireCraftMaterial) Verdict.UNKNOWN else Verdict.MATCH))
}

data class EvaluatedEventOutcome(val outcome: EventOutcome,val decisions: List<Decision>) {
    val verdict get()=all(decisions.map { it.verdict })
}
fun deathResponses(outcomes: List<EventOutcome>): List<EvaluatedEventOutcome> = outcomes.map { o ->
    EvaluatedEventOutcome(o,listOf(
        Decision("qualifying-death-event",eventIntersection(o.event,ZoneEventRequirement(Zone.BATTLEFIELD,Zone.GRAVEYARD,true,Controller.YOU,TokenStatus.NONTOKEN)),"MECHANICAL",
            "Intersection with our nontoken creature's battlefield-to-graveyard event; native binding=${o.event.binding}; trigger=${o.event.trigger}"),
        Decision("same-ability-outcome",o.confidence,"MECHANICAL","${o.actionId} / ${o.path}; ${o.linkage}; unsupported conditions remain unresolved"),
        Decision("outcome-recipient",when(o.recipient) { "YOU","ANY"->Verdict.MATCH;"OPPONENT"->Verdict.NO_MATCH;else->Verdict.UNKNOWN },"MECHANICAL","Native outcome receiver=${o.recipient}; not a strategic value judgment"),
        Decision("positive-outcome-quantity",o.positiveQuantity,"MECHANICAL","Positive native quantity required: ${o.quantity}")
    ))
}

private fun potentialUninterpretedDeath(event: EventPattern): Boolean = when(event) {
    is EventPattern.CreaturesYouControlDiedEvent, is EventPattern.CreatureDealtDamageBySourceDiesEvent,
    is EventPattern.CardsPutIntoGraveyardEvent, is EventPattern.PermanentsSacrificedEvent,
    is EventPattern.ExploitedEvent, is EventPattern.DestroyEvent -> true
    is EventPattern.AnyOf -> event.events.any { potentialUninterpretedDeath(it) || potentiallyDeathZone(it) }
    else -> false
}
private fun potentiallyDeathZone(event: EventPattern): Boolean = (event as? EventPattern.ZoneChangeEvent)?.let {
    (it.from==null || it.from==Zone.BATTLEFIELD) && (it.to==null || it.to==Zone.GRAVEYARD) && it.excludeTo!=Zone.GRAVEYARD
} == true

/** Project only potentially overlapping zone events; other events remain inspectable in
 * shared records, not spurious death-objective negatives. */
internal fun deathResponseResults(catalog: dev.mtg.assistant.Catalog, outcomes: List<EventOutcome>,
    eligibility: Map<String,EligibilityEvidence>, unsupported: List<Unsupported>): List<ResultEvidence> =
    deathResponses(outcomes).filter { row ->
        potentiallyDeathZone(row.outcome.event.trigger) || potentialUninterpretedDeath(row.outcome.event.trigger)
    }.map { row ->
        val o=row.outcome; val card=catalog.card(o.card)!!; val e=eligibility.getValue(o.card)
        val provenance=catalog.provenance(o.card)
        val source=if(provenance.origin=="UPSTREAM_ARGENTUM") DefinitionSources.paths[o.card] else null
        val partial=o.confidence==Verdict.UNKNOWN || unsupported.any { it.actionId==o.actionId }
        ResultEvidence(card=o.card,identifier=card.oracleId ?: "${card.setCode}:${card.metadata.collectorNumber}:${card.name}",face=o.face,
            actionId=o.actionId,abilityPath=o.path,branch=Contribution.DEATH_TRIGGERED_OUTCOME,verdict=row.verdict,
            operation=o.kind.name,costOrEffect="EFFECT",affectedScope=o.event.trigger.description,
            prerequisites=o.prerequisites+o.event.requirements+listOf("The qualifying nontoken creature must actually die; death replacements, trigger suppression, legal targets and resolution can prevent this outcome", "Binding=${o.event.binding}; source face ${o.face} and active zones ${o.event.activeZones} must be available"),
            dependency=o.event.trigger.description,nativeOperation=o.native,decisions=row.decisions,
            sourceReferences=listOfNotNull(source?.let { "$ARGENTUM_REPOSITORY_URL/blob/$ARGENTUM_SHA/$it" },if(provenance.origin=="APPLICATION_EXTENSION") provenance.source else null),
            commanderLegality=e.format,deckEligibility=e.deck,
            interpretation=if(partial) "PARTIAL_ACTION_SUPPORTED_OUTCOME_NOT_GAMEPLAY_VERIFIED" else "STRUCTURAL_POTENTIAL_NOT_GAMEPLAY_VERIFIED",
            contributionClassification=Contribution.DEATH_TRIGGERED_OUTCOME.takeIf { row.verdict==Verdict.MATCH },
            provenance=provenance,enablingRelationship="DEATH_TRIGGERS_OUTCOME",mechanicalCompatibility=row.verdict,policyAcceptance=Verdict.MATCH,
            mechanicalCapability="DEATH_TRIGGERED_${o.kind}".takeIf { row.verdict==Verdict.MATCH },localSupport=if(partial || row.verdict==Verdict.UNKNOWN) SupportStatus.PARTIALLY_SUPPORTED else SupportStatus.SUPPORTED,
            controllerCompatibility=eventIntersection(o.event,ZoneEventRequirement(Zone.BATTLEFIELD,Zone.GRAVEYARD,controller=Controller.YOU)),
            nontokenCompatibility=eventIntersection(o.event,ZoneEventRequirement(Zone.BATTLEFIELD,Zone.GRAVEYARD,token=TokenStatus.NONTOKEN)),
            circumstances=Circumstances("TRIGGERED",if(o.optional) Verdict.NO_MATCH else Verdict.MATCH,false,o.linkage!="SAME_TRIGGER_BRANCH",o.prerequisites,false,
                listOf("Binding=${o.event.binding}"),"Per matching native event and retained turn caps/conditions; not guaranteed repeatability"),eventOutcome=o)
    }
