package dev.mtg.assistant.discovery

import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import kotlinx.serialization.Serializable

@Serializable enum class Verdict { MATCH, NO_MATCH, UNKNOWN }
fun all(values: Iterable<Verdict>): Verdict = values.toList().let {
    when { Verdict.NO_MATCH in it -> Verdict.NO_MATCH; Verdict.UNKNOWN in it -> Verdict.UNKNOWN; else -> Verdict.MATCH }
}
fun any(values: Iterable<Verdict>): Verdict = values.toList().let {
    when { Verdict.MATCH in it -> Verdict.MATCH; Verdict.UNKNOWN in it -> Verdict.UNKNOWN; else -> Verdict.NO_MATCH }
}
private fun Verdict.inverse() = when(this) {
    Verdict.MATCH -> Verdict.NO_MATCH; Verdict.NO_MATCH -> Verdict.MATCH; Verdict.UNKNOWN -> this
}
enum class Controller { YOU, OPPONENT }
enum class TokenStatus { TOKEN, NONTOKEN }
data class Requirements(val creature: Boolean = true, val controller: Controller? = null, val token: TokenStatus? = null)

/** Existential structural compatibility, not a claim that a payable/legal object exists in a game. */
data class ObjectScope(
    val filter: GameObjectFilter = GameObjectFilter.Any,
    val actor: String = "ANY",
    val printedSelfCreature: Boolean? = null,
    val unresolved: String? = null,
    val battlefield: Boolean = true, val details: List<String> = emptyList(),
    val zone: com.wingedsheep.sdk.core.Zone? = null, val zonePlayer: String = "UNKNOWN",
    val possible: Boolean = true, val ownerScope: String = "ANY",
) {
    val description: String get() = "${filter.description}; actor=$actor; selfCreature=$printedSelfCreature; battlefieldCompatible=$battlefield; ${unresolved ?: ""}; constraints=$details"
    fun compatible(r: Requirements): Verdict {
        if (!battlefield || (r.creature && printedSelfCreature == false)) return Verdict.NO_MATCH
        val actorMatch = when {
            r.controller == null -> Verdict.MATCH
            actor == "ANY" -> Verdict.MATCH
            actor == "YOU" -> if (r.controller == Controller.YOU) Verdict.MATCH else Verdict.NO_MATCH
            actor == "OPPONENT" -> if (r.controller == Controller.OPPONENT) Verdict.MATCH else Verdict.NO_MATCH
            else -> Verdict.UNKNOWN
        }
        return all(listOf(actorMatch, matchFilter(filter, r), if (unresolved == null) Verdict.MATCH else Verdict.UNKNOWN))
    }
}
private fun matchFilter(f: GameObjectFilter, r: Requirements): Verdict = all(
    f.cardPredicates.map { cardPredicate(it, r) } +
        controllerPredicate(f.controllerPredicate, r.controller) +
        (if (f.statePredicates.isEmpty()) Verdict.MATCH else Verdict.UNKNOWN) +
        (if (f.anyOf.isEmpty()) Verdict.MATCH else any(f.anyOf.map { matchFilter(it, r) }))
)
internal fun cardPredicate(p: CardPredicate, r: Requirements): Verdict = when(p) {
    CardPredicate.IsCreature -> Verdict.MATCH
    CardPredicate.IsNoncreature -> if (r.creature) Verdict.NO_MATCH else Verdict.MATCH
    CardPredicate.IsPermanent, CardPredicate.IsArtifact -> Verdict.MATCH
    CardPredicate.IsToken -> if (r.token == TokenStatus.NONTOKEN) Verdict.NO_MATCH else Verdict.MATCH
    CardPredicate.IsNontoken -> if (r.token == TokenStatus.TOKEN) Verdict.NO_MATCH else Verdict.MATCH
    CardPredicate.IsInstant, CardPredicate.IsSorcery -> if (r.creature) Verdict.NO_MATCH else Verdict.UNKNOWN
    is CardPredicate.And -> all(p.predicates.map { cardPredicate(it,r) })
    is CardPredicate.Or -> any(p.predicates.map { cardPredicate(it,r) })
    is CardPredicate.Not -> when(p.predicate) {
        CardPredicate.IsCreature -> if (r.creature) Verdict.NO_MATCH else Verdict.UNKNOWN
        CardPredicate.IsToken -> if (r.token == null) Verdict.MATCH else cardPredicate(p.predicate, r).inverse()
        CardPredicate.IsNontoken -> if (r.token == null) Verdict.MATCH else cardPredicate(p.predicate, r).inverse()
        else -> Verdict.UNKNOWN
    }
    else -> Verdict.UNKNOWN // No guessed satisfiability for other property/state predicates.
}
private fun controllerPredicate(p: ControllerPredicate?, requested: Controller?): Verdict = when {
    p == null || requested == null || p == ControllerPredicate.ControlledByAny -> Verdict.MATCH
    p == ControllerPredicate.ControlledByYou -> if (requested == Controller.YOU) Verdict.MATCH else Verdict.NO_MATCH
    p == ControllerPredicate.ControlledByOpponent || p == ControllerPredicate.ControlledByTargetOpponent ->
        if (requested == Controller.OPPONENT) Verdict.MATCH else Verdict.NO_MATCH
    p is ControllerPredicate.And -> all(p.predicates.map { controllerPredicate(it, requested) })
    p is ControllerPredicate.Or -> any(p.predicates.map { controllerPredicate(it, requested) })
    p is ControllerPredicate.Not -> controllerPredicate(p.predicate, requested).inverse()
    else -> Verdict.UNKNOWN
}

@Serializable data class Decision(val stage: String, val verdict: Verdict, val basis: String, val reason: String)
data class MechanicalPredicate(val name: String, val add: (Requirements) -> Requirements = { it },
                               val assess: (Pathway, Requirements) -> Decision)
fun canAffectCreature() = MechanicalPredicate("potential-creature-death", assess = { p, r ->
    Decision("potential-creature-death", all(listOf(p.scope.compatible(r),p.operationCompatibility)), "MECHANICAL", p.scope.description)
})
fun canAffectController(controller: Controller) = MechanicalPredicate("controller-$controller",
    { it.copy(controller = controller) }, { p,r -> Decision("controller-$controller",p.scope.compatible(r),"MECHANICAL",p.scope.description) })
fun canAffectTokenStatus(token: TokenStatus) = MechanicalPredicate("token-$token",
    { it.copy(token = token) }, { p,r -> Decision("token-$token",p.scope.compatible(r),"MECHANICAL",p.scope.description) })
data class DeathGenerationPolicy(val includeMandatorySacrifice: Boolean = true,
    val excludeIncidentalOpposingRemoval: Boolean = true, val includeIntrinsicSelfDeath: Boolean = true)
fun satisfiesDeathGenerationPolicy(policy: DeathGenerationPolicy = DeathGenerationPolicy()) = MechanicalPredicate("death-generation-policy", assess = { p,_ ->
    val enabling=p.relationship in setOf("SACRIFICE_SUCCESS_ENABLES_OUTCOME","REFLEXIVE_ACTION_ENABLES_OUTCOME")
    val (result,reason) = when {
        p.origin.endsWith("COST") && p.operation=="SACRIFICE" -> Verdict.MATCH to "Sacrifice is a cost of this action; no utility assumption"
        p.operation=="SACRIFICE" && enabling -> Verdict.MATCH to "Native success/reflexive relationship enables the associated action"
        policy.excludeIncidentalOpposingRemoval && p.opposingRemovalAssociation==Verdict.MATCH -> Verdict.NO_MATCH to "Policy: symmetrical/incidental opposing removal is not enabled by our death; compatibility is retained"
        policy.excludeIncidentalOpposingRemoval && p.opposingRemovalAssociation==Verdict.UNKNOWN -> Verdict.UNKNOWN to "Policy: relationship to possible opposing removal is unresolved"
        p.operation=="SACRIFICE" && !policy.includeMandatorySacrifice && p.circumstances?.mandatory==Verdict.MATCH -> Verdict.NO_MATCH to "Configured policy excludes mandatory sacrifices; mechanical capability is unchanged"
        p.operation=="SACRIFICE" -> Verdict.MATCH to "Non-removal sacrifice capability; mandatory, conditional, delayed or unlinked timing does not require a beneficial payoff"
        policy.includeIntrinsicSelfDeath && p.scope.printedSelfCreature==true -> Verdict.MATCH to "Intrinsic own-source death operation; its timing, resources and state requirements are retained without utility scoring"
        else -> Verdict.NO_MATCH to "Policy: ordinary friendly-target removal; choosing our creature alone does not qualify"
    }
    Decision("death-generation-policy",result,"POLICY",reason)
})
fun hasDeathEnhancement() = MechanicalPredicate("death-enhancement", assess = { p,_ ->
    Decision("death-enhancement", p.dependencyVerdict, "MECHANICAL", p.dependency ?: "No identified death/removal relationship")
})
data class EvaluatedPathway(val pathway: Pathway, val decisions: List<Decision>) {
    val verdict: Verdict get() = all(decisions.map { it.verdict })
}
/** Constraints accumulate before re-evaluating the native filter, preserving correlations in ORs. */
class MechanicalQuery(private val paths: List<Pathway>, private val filters: List<MechanicalPredicate> = emptyList()) {
    fun where(predicate: MechanicalPredicate) = MechanicalQuery(paths, filters + predicate)
    fun execute(): List<EvaluatedPathway> = paths.map { path ->
        var requirements = Requirements()
        EvaluatedPathway(path, filters.map { f -> requirements = f.add(requirements); f.assess(path, requirements) })
    }
}
fun creatureDeathPathways(paths: List<Pathway>) = MechanicalQuery(paths.filter { it.branch == Contribution.DIRECT_DEATH_GENERATOR })
