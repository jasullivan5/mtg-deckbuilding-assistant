package dev.mtg.assistant.discovery

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Small operation view of native SDK nodes. No card model, strategy labels, or second walker. */
@Serializable enum class FactKind { MOVE_OBJECT, SACRIFICE, DAMAGE, TOUGHNESS_CHANGE, CAST_PERMISSION, CREATE_TOKEN, OTHER_OPERATION }
@Serializable data class MechanicalFact(
    val id: String, val card: String, val face: String, val actionId: String, val path: String,
    val kind: FactKind, val operation: String, val costOrEffect: String,
    val objectFilter: GameObjectFilter, val sourceZone: Zone? = null,
    val sourcePlayer: String = "UNKNOWN", val destinationZone: Zone? = null,
    /** YOU/OPPONENT/ANY are relative to the acting player; OWNER is correlated to the chosen card. */
    val destinationController: String = "UNKNOWN", val confidence: Verdict = Verdict.MATCH,
    val prerequisites: List<String> = emptyList(), val native: JsonElement,
    val binding: String? = null, val selection: String = "Native node/targets define quantity and chooser",
    val circumstances: Circumstances? = null, val unresolved: List<String> = emptyList(),
    /** Original physical card, not a token copy; zone changes still create a new game object (CR 400.7). */
    val movesOriginalCard: Boolean = true, val ownerScope: String = "ANY",
    val tokenCreation: TokenCreation? = null
)

data class FactConstraints(val impossible: Boolean = false, val kind: FactKind? = null, val creature: Boolean = false,
    val token: TokenStatus? = null, val owner: Controller? = null, val controller: Controller? = null,
    val createdCreature: Boolean? = null, val positiveQuantity: Boolean = false,
    val from: Zone? = null, val to: Zone? = null, val role: String? = null, val sourceController: Controller? = null)
data class FactPredicate(val name: String, val constrain: (FactConstraints)->FactConstraints)
data class EvaluatedFact(val fact: MechanicalFact,val decisions: List<Decision>) {
    val verdict get()=all(decisions.map { it.verdict })
}
/** Each stage rechecks all accumulated constraints against the SAME native branch and owner. */
class FactQuery(private val facts: List<MechanicalFact>,private val filters: List<FactPredicate> = emptyList()) {
    fun where(predicate: FactPredicate)=FactQuery(facts,filters+predicate)
    fun execute()=facts.map { f ->
        var c=FactConstraints()
        EvaluatedFact(f,filters.map { p -> c=p.constrain(c); Decision(p.name,f.compatible(c),"MECHANICAL",
            "${f.kind}; source=${f.sourceZone}/${f.sourcePlayer}; destination=${f.destinationZone}/${f.destinationController}; filter=${f.objectFilter}; binding=${f.binding}; confidence=${f.confidence}; unresolved=${f.unresolved}") })
    }
}
fun operationIs(kind: FactKind)=FactPredicate("operation-$kind") { it.copy(kind=kind) }
fun objectIsCreatureCard()=FactPredicate("creature-card") { it.copy(creature=true,token=TokenStatus.NONTOKEN) }
fun sourceZoneIs(zone: Zone)=FactPredicate("source-$zone") { it.copy(from=zone) }
fun destinationZoneIs(zone: Zone)=FactPredicate("destination-$zone") { it.copy(to=zone) }
fun ownedBy(player: Controller)=FactPredicate("owner-$player") { it.copy(owner=player) }
fun entersUnder(player: Controller)=FactPredicate("entry-controller-$player") { it.copy(controller=player,impossible=it.impossible || it.controller!=null && it.controller!=player) }
fun sourceControlledBy(player: Controller)=FactPredicate("source-controller-$player") { it.copy(sourceController=player) }
fun operationRoleIs(role: String)=FactPredicate("role-$role") { it.copy(role=role) }
fun reanimationFacts(facts: List<MechanicalFact>)=FactQuery(facts.filter { it.kind==FactKind.MOVE_OBJECT })
    .where(operationIs(FactKind.MOVE_OBJECT)).where(objectIsCreatureCard())
    .where(sourceZoneIs(Zone.GRAVEYARD)).where(ownedBy(Controller.YOU))
    .where(destinationZoneIs(Zone.BATTLEFIELD)).where(entersUnder(Controller.YOU))

/** Existential compatibility, not a legal-target/resource/gameplay solver.
 * Enumerating owner alternatives prevents (own+noncreature) OR (opponent+creature) false positives.
 * Negation is evaluated on a specific assignment, never on an existential result.
 */
fun MechanicalFact.compatible(c: FactConstraints): Verdict {
    if(c.impossible) return Verdict.NO_MATCH
    fun eq(actual: Any?,expected: Any?)=if(expected==null) Verdict.MATCH else if(actual==null) Verdict.UNKNOWN else if(actual==expected) Verdict.MATCH else Verdict.NO_MATCH
    val structural=all(listOf(eq(kind,c.kind),eq(sourceZone,c.from),eq(destinationZone,c.to),eq(costOrEffect,c.role),confidence,
        if(unresolved.isEmpty()) Verdict.MATCH else Verdict.UNKNOWN))
    if(structural==Verdict.NO_MATCH) return structural
    if(kind==FactKind.CREATE_TOKEN) {
        val token=tokenCreation
        val type=if(c.createdCreature==null) Verdict.MATCH else token?.creature?.let {
            if(c.createdCreature) it else when(it) { Verdict.MATCH->Verdict.NO_MATCH;Verdict.NO_MATCH->Verdict.MATCH;else->Verdict.UNKNOWN }
        } ?: Verdict.UNKNOWN
        val controller=when { c.controller==null -> Verdict.MATCH; destinationController=="UNKNOWN" -> Verdict.UNKNOWN
            destinationController=="ANY" -> Verdict.MATCH
            destinationController==c.controller.name -> Verdict.MATCH; else -> Verdict.NO_MATCH }
        // Token ownership is the creating player's (CR 111.2), not ownership of a copied object.
        val ownership=if(c.owner==null) Verdict.MATCH else if(destinationController=="UNKNOWN") Verdict.UNKNOWN
            else if(destinationController=="ANY" || destinationController==c.owner.name) Verdict.MATCH else Verdict.NO_MATCH
        return all(listOf(structural,type,controller,ownership,
            if(c.positiveQuantity) token?.positiveQuantity ?: Verdict.UNKNOWN else Verdict.MATCH,
            if(c.token==TokenStatus.NONTOKEN || c.sourceController!=null || c.from!=null) Verdict.NO_MATCH else Verdict.MATCH,
            if(c.creature) token?.creature ?: Verdict.UNKNOWN else Verdict.MATCH))
    }
    val matches=Controller.entries.map { owner ->
        fun player(actual: String,required: Controller?): Verdict = when {
            required==null -> Verdict.MATCH
            actual=="ANY" -> Verdict.MATCH
            actual=="YOU" -> eq(Controller.YOU,required)
            actual=="OPPONENT" -> eq(Controller.OPPONENT,required)
            actual=="OWNER" -> eq(owner,required)
            else -> Verdict.UNKNOWN
        }
        fun cp(p: ControllerPredicate?): Verdict = when(p) {
            null,ControllerPredicate.ControlledByAny -> Verdict.MATCH
            ControllerPredicate.OwnedByYou -> eq(owner,Controller.YOU)
            ControllerPredicate.OwnedByOpponent -> eq(owner,Controller.OPPONENT)
            ControllerPredicate.ControlledByYou -> if(sourceZone==Zone.BATTLEFIELD) player(sourcePlayer,Controller.YOU) else Verdict.UNKNOWN
            ControllerPredicate.ControlledByOpponent,ControllerPredicate.ControlledByTargetOpponent -> if(sourceZone==Zone.BATTLEFIELD) player(sourcePlayer,Controller.OPPONENT) else Verdict.UNKNOWN
            is ControllerPredicate.And -> all(p.predicates.map(::cp))
            is ControllerPredicate.Or -> any(p.predicates.map(::cp))
            is ControllerPredicate.Not -> when(cp(p.predicate)) { Verdict.MATCH->Verdict.NO_MATCH;Verdict.NO_MATCH->Verdict.MATCH;else->Verdict.UNKNOWN }
            else -> Verdict.UNKNOWN
        }
        fun filter(f: GameObjectFilter): Verdict=all(f.cardPredicates.map { cardPredicate(it,Requirements(c.creature,token=c.token)) }+
            cp(f.controllerPredicate)+(if(f.statePredicates.isEmpty()) Verdict.MATCH else Verdict.UNKNOWN)+
            (if(f.anyOf.isEmpty()) Verdict.MATCH else any(f.anyOf.map(::filter))))
        val zoneOwner=if(sourceZone!=null && sourceZone!=Zone.BATTLEFIELD && sourceZone!=Zone.STACK)
            player(sourcePlayer,owner) else Verdict.MATCH
        all(listOf(if(c.owner==null || c.owner==owner) Verdict.MATCH else Verdict.NO_MATCH,zoneOwner,
            player(destinationController,c.controller),player(ownerScope,owner),
            if(c.sourceController==null) Verdict.MATCH else if(sourceZone==Zone.BATTLEFIELD) player(sourcePlayer,c.sourceController) else Verdict.NO_MATCH,filter(objectFilter)))
    }
    return all(listOf(structural,any(matches)))
}
