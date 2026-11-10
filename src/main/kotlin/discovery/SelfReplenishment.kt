package dev.mtg.assistant.discovery

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/** Context of the source's own action, independent of the object observed by a trigger. */
@Serializable data class ReturnSourceContext(
    val kind: String, val activeZones: Set<Zone>, val opportunity: String,
    val activationCost: String? = null, val confidence: Verdict = Verdict.MATCH,
    val selfDeathTransition: Boolean = false, val sourceCostCompatibility: Verdict = Verdict.MATCH,
    val requirements: List<String> = emptyList(), val limits: List<ReturnLimit> = emptyList()
)
@Serializable data class ReturnLimit(val kind: String, val path: String, val explanation: String, val native: JsonElement)
/** An additive join to an existing movement fact; never a new native walker or proof of a cycle. */
@Serializable data class SelfReturnLink(
    val factId: String, val card: String, val face: String, val actionId: String, val path: String,
    val sourceCreature: Verdict, val binding: Verdict, val context: ReturnSourceContext,
    val branchSupport: Verdict, val optional: Boolean, val delayed: Boolean,
    val requirements: List<String>, val limits: List<ReturnLimit>,
    val sourceZone: Zone? = null, val destinationZone: Zone? = null,
    val sourcePlayer: String = "UNKNOWN", val destinationController: String = "UNKNOWN"
)

/** Bounded source-preservation check, not a payment solver. Unknown costs never become free. */
internal fun sourceCostCompatibility(cost: AbilityCost,zone: Zone): Verdict = when(cost) {
    AbilityCost.ExileSelf,AbilityCost.ReturnSelfToHand,is AbilityCost.Craft -> Verdict.NO_MATCH
    AbilityCost.SacrificeSelf -> if(zone==Zone.BATTLEFIELD) Verdict.UNKNOWN else Verdict.NO_MATCH
    AbilityCost.DiscardSelf -> if(zone==Zone.HAND) Verdict.UNKNOWN else Verdict.NO_MATCH
    AbilityCost.Tap,AbilityCost.Untap,AbilityCost.Exert -> if(zone==Zone.BATTLEFIELD) Verdict.MATCH else Verdict.NO_MATCH
    AbilityCost.Free,AbilityCost.OwnManaCost -> Verdict.MATCH
    is AbilityCost.Atom -> when(cost.atom) {
        is CostAtom.Mana,is CostAtom.PayLife,is CostAtom.Discard,CostAtom.DiscardHand -> Verdict.MATCH
        is CostAtom.Sacrifice,is CostAtom.SacrificeAll,is CostAtom.TapPermanents -> if(zone==Zone.GRAVEYARD) Verdict.MATCH else Verdict.UNKNOWN
        else -> Verdict.UNKNOWN
    }
    is AbilityCost.Composite -> all(cost.costs.map { sourceCostCompatibility(it,zone) })
    else -> Verdict.UNKNOWN
}
internal fun returnGateSupport(gate: Gate,sourceZone: Zone?,decisionMaker: EffectTarget?): Verdict =
    if(decisionMaker!=null) Verdict.UNKNOWN else when(gate) {
    is Gate.MayDecide -> when {
        gate.feasibility!=null -> Verdict.UNKNOWN
        gate.sourceRequiredZone==null -> Verdict.MATCH
        sourceZone==null -> Verdict.UNKNOWN
        gate.sourceRequiredZone==sourceZone -> Verdict.MATCH
        else -> Verdict.NO_MATCH
    }
    is Gate.MayPay -> (gate.cost as? PayManaCostEffect)?.let { if(!it.waterbend) Verdict.MATCH else Verdict.UNKNOWN } ?: Verdict.UNKNOWN
    else -> Verdict.UNKNOWN
}
/** Direct instructions after the movement in this same sequence. Nested instructions remain
 * opaque; neither another ability nor the unchosen alternative lends its riders to this return. */
internal fun followingReturnLimit(effect: Effect, path: String): ReturnLimit? = when(effect) {
    is AddCountersEffect -> if(effect.target==EffectTarget.Self && effect.count>0 && effect.counterType==CounterType.FINALITY)
        ReturnLimit("DEATH_REPLACEMENT",path,"A finality counter exiles this creature instead of a later death while the counter remains. No counter-removal mechanism or further graveyard return is inferred.",nativeJson.encodeToJsonElement(Effect.serializer(),effect)) else null
    is DrawCardsEffect, is ScryEffect -> null
    else -> ReturnLimit("UNRESOLVED_FOLLOWING_STEP",path,"A subsequent instruction in this branch may change availability; its consequences are not fully interpreted.",nativeJson.encodeToJsonElement(Effect.serializer(),effect))
}

/** Preserve legacy facts while retaining uncertainty about their single-zone/type fallback. */
private fun returnMovementView(link: SelfReturnLink,f: MechanicalFact): MechanicalFact =
    if(f.confidence==Verdict.NO_MATCH && link.binding==Verdict.MATCH &&
        (link.context.activeZones.size>1 && Zone.GRAVEYARD in link.context.activeZones || link.context.sourceCostCompatibility==Verdict.UNKNOWN))
        f.copy(confidence=Verdict.UNKNOWN) else f

data class EvaluatedSelfReturn(val link: SelfReturnLink,val fact: MechanicalFact,val decisions: List<Decision>) {
    val verdict get()=all(decisions.map { it.verdict })
}
fun originalCreatureSelfReturns(links: List<SelfReturnLink>,facts: List<MechanicalFact>): List<EvaluatedSelfReturn> {
    val byId=facts.associateBy { it.id }
    return links.map { link ->
        val f=byId.getValue(link.factId);val c=link.context
        val movement=returnMovementView(link,f)
        val active=when {
            c.kind=="ACTIVATED" && c.activeZones==setOf(Zone.GRAVEYARD) -> Verdict.MATCH
            c.kind=="TRIGGERED" && c.activeZones==setOf(Zone.GRAVEYARD) -> Verdict.MATCH
            c.kind=="TRIGGERED" && c.activeZones==setOf(Zone.BATTLEFIELD) && c.selfDeathTransition -> Verdict.MATCH
            c.activeZones.size!=1 || c.kind !in setOf("ACTIVATED","TRIGGERED") || c.sourceCostCompatibility==Verdict.UNKNOWN -> Verdict.UNKNOWN
            else -> Verdict.NO_MATCH
        }
        EvaluatedSelfReturn(link,f,listOf(
            Decision("originating-card-binding",link.binding,"MECHANICAL","Explicit original source binding; fact=${f.id}; action=${link.actionId}; face=${link.face}. A target or collection is not assumed to be this card."),
            Decision("original-creature-card",all(listOf(link.sourceCreature,if(f.movesOriginalCard && f.kind==FactKind.MOVE_OBJECT) Verdict.MATCH else Verdict.NO_MATCH)),"MECHANICAL","Normal original creature card required; multi-face/type-changing interpretations remain unresolved. A token copy is a different resource."),
            Decision("own-graveyard-to-own-battlefield",movement.compatible(FactConstraints(kind=FactKind.MOVE_OBJECT,creature=link.sourceCreature!=Verdict.UNKNOWN,token=TokenStatus.NONTOKEN,owner=Controller.YOU,controller=Controller.YOU,from=Zone.GRAVEYARD,to=Zone.BATTLEFIELD)),"MECHANICAL","Original card in its owner's graveyard; owner and returning controller must be us. Native movement/filter/zone constraints are retained."),
            Decision("source-availability",all(listOf(active,c.confidence,c.sourceCostCompatibility)),"MECHANICAL","${c.kind}; active zones=${c.activeZones}; bound self-death transition=${c.selfDeathTransition}; source-consuming cost=${c.sourceCostCompatibility}. Source must remain available for this native route."),
            Decision("same-supported-return-branch",all(listOf(link.branchSupport,if(link.delayed) Verdict.UNKNOWN else Verdict.MATCH)),"MECHANICAL","Native action and branch ${link.actionId}/${link.path}; conditions, payments, timing and other resources remain prerequisites.")
        ))
    }
}
/** Death-objective constraint over the reusable return route. A supported return step does
 * not establish availability for a qualifying later death or any repeatable cycle. */
fun selfReplenishment(links: List<SelfReturnLink>,facts: List<MechanicalFact>): List<EvaluatedSelfReturn> =
    originalCreatureSelfReturns(links,facts).map { e -> e.copy(decisions=e.decisions+Decision("future-death-barriers",
        if(e.link.limits.any { it.kind in setOf("DEATH_REPLACEMENT","UNRESOLVED_FOLLOWING_STEP","SOURCE_REPLACEMENT_UNRESOLVED") }) Verdict.UNKNOWN else Verdict.MATCH,
        "MECHANICAL","${e.link.limits.map { it.explanation }} Supported return is not proof of a later death or a repeatable sacrifice/return cycle.")) }

internal fun creatureInputResults(catalog: dev.mtg.assistant.Catalog, extracted: PathwayExtractor,
    eligibility: Map<String,EligibilityEvidence>): List<ResultEvidence> = selfReplenishment(extracted.selfReturns,extracted.facts).map { e ->
    val f=e.fact;val link=e.link;val card=catalog.card(f.card)!!;val eligible=eligibility.getValue(f.card)
    val provenance=catalog.provenance(f.card)
    val source=DefinitionSources.paths[f.card]?.let { "$ARGENTUM_REPOSITORY_URL/blob/$ARGENTUM_SHA/$it" }
    val partial=e.verdict==Verdict.UNKNOWN || extracted.unsupported.any { it.actionId==f.actionId }
    ResultEvidence(card=f.card,identifier=card.oracleId ?: "${card.setCode}:${card.metadata.collectorNumber}:${card.name}",face=f.face,
        actionId=f.actionId,abilityPath=f.path,branch=Contribution.INTRINSIC_CREATURE_INPUT,verdict=e.verdict,
        operation=f.operation,costOrEffect=f.costOrEffect,affectedScope="Original source creature; ${f.sourceZone}/${f.sourcePlayer} → ${f.destinationZone}/${f.destinationController}",
        prerequisites=f.prerequisites+link.requirements+link.context.requirements+listOf("Source card must be ours and remain available; all native costs, conditions, choices and replacement effects apply", "The return does not cause a death; another legal mechanism is needed. No sacrifice/return loop or unlimited recursion is demonstrated"),
        dependency=link.context.opportunity,nativeOperation=f.native,decisions=e.decisions,sourceReferences=listOfNotNull(source,if(provenance.origin=="APPLICATION_EXTENSION") provenance.source else null),
        commanderLegality=eligible.format,deckEligibility=eligible.deck,interpretation=if(partial) "PARTIAL_ACTION_SELF_RETURN_NOT_GAMEPLAY_VERIFIED" else "STRUCTURAL_SELF_RETURN_NOT_GAMEPLAY_VERIFIED",
        contributionClassification=Contribution.INTRINSIC_CREATURE_INPUT.takeIf { e.verdict==Verdict.MATCH },
        provenance=provenance,enablingRelationship="ORIGINAL_SOURCE_CREATURE_REPLENISHMENT",mechanicalCompatibility=e.verdict,policyAcceptance=Verdict.MATCH,
        mechanicalCapability="INTRINSIC_ORIGINAL_CREATURE_RETURN".takeIf { e.verdict==Verdict.MATCH },
        controllerCompatibility=returnMovementView(link,f).compatible(FactConstraints(owner=Controller.YOU,controller=Controller.YOU)),
        nontokenCompatibility=all(listOf(link.sourceCreature,returnMovementView(link,f).compatible(FactConstraints(creature=link.sourceCreature!=Verdict.UNKNOWN,token=TokenStatus.NONTOKEN)))),
        localSupport=if(partial) SupportStatus.PARTIALLY_SUPPORTED else SupportStatus.SUPPORTED,circumstances=f.circumstances,selfReturn=link)
}
