package dev.mtg.assistant.discovery

import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.*
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.*
import kotlinx.serialization.Serializable

const val DEATH_HISTORY_OBJECTIVE = "creature-died-this-turn"

@Serializable enum class StateScope { DEATH_HISTORY, BATTLEFIELD_COUNT, SOURCE_ABILITY_HISTORY, UNRESOLVED }
@Serializable enum class StateCheckpoint { RESOLUTION, TRIGGER_AND_RESOLUTION, ACTIVATION }
/** References to existing walker evidence. Polarity follows the literal native predicate. */
@Serializable data class ConditionalBranchLinks(
    val factIds: List<String> = emptyList(),
    val eventOutcomeIds: List<String> = emptyList()
)

@Serializable data class StateDependency(
    val card: String, val face: String, val actionId: String, val conditionPath: String,
    val outcomePath: String, val condition: Condition, val scope: StateScope, val checkpoint: StateCheckpoint,
    val whenTrue: Effect, val whenFalse: Effect? = null, val contextSupport: Verdict,
    val prerequisites: List<String>, val incomingEvent: EventContext? = null,
    val otherwisePath: String? = null,
    val trueLinks: ConditionalBranchLinks = ConditionalBranchLinks(),
    val falseLinks: ConditionalBranchLinks = ConditionalBranchLinks(),
    val causalChange: Verdict = conditionalEffectChange(whenTrue, whenFalse)
)

sealed interface StateAnchor {
    data class DeathHistory(val controller: Controller? = null, val token: TokenStatus? = null) : StateAnchor
    /** A concrete measurement used to test a threshold, not a fabricated battlefield. */
    data class BattlefieldCount(val player: Player, val filter: GameObjectFilter, val count: Int) : StateAnchor
}

private fun hasDeathHistory(c: Condition): Boolean = when(c) {
    CreatureDiedThisTurnCondition, ControlledCreatureDiedThisTurnCondition, is CreatureDiedThisTurnMatching -> true
    is AllConditions -> c.conditions.any(::hasDeathHistory)
    is AnyCondition -> c.conditions.any(::hasDeathHistory)
    is NotCondition -> hasDeathHistory(c.condition)
    else -> false
}

internal fun stateScope(c: Condition): StateScope {
    if(hasDeathHistory(c)) return StateScope.DEATH_HISTORY
    if(c is Compare) {
        val operands=listOf(c.left,c.right)
        if(operands.any { it is DynamicAmount.EntityProperty && it.entity==EffectTarget.Self &&
                it.numericProperty==EntityNumericProperty.AbilityResolutionsThisTurn }) return StateScope.SOURCE_ABILITY_HISTORY
        if(operands.any { it is DynamicAmount.AggregateBattlefield && it.aggregation==Aggregation.COUNT }) return StateScope.BATTLEFIELD_COUNT
    }
    return StateScope.UNRESOLVED
}

private fun predicateCompatibility(c: Condition, anchor: StateAnchor): Verdict = when(anchor) {
    is StateAnchor.DeathHistory -> if(!hasDeathHistory(c)) {
        if(stateScope(c)==StateScope.UNRESOLVED) Verdict.UNKNOWN else Verdict.NO_MATCH
    } else {
        val (support,scope)=deathDependency(c)
        val player=(c as? CreatureDiedThisTurnMatching)?.player
        if(player!=null && player !in setOf(Player.Each,Player.You)) Verdict.UNKNOWN
        else all(listOf(support,scope.compatible(Requirements(controller=anchor.controller,token=anchor.token))))
    }
    is StateAnchor.BattlefieldCount -> if(stateScope(c)!=StateScope.BATTLEFIELD_COUNT) {
        if(stateScope(c)==StateScope.UNRESOLVED) Verdict.UNKNOWN else Verdict.NO_MATCH
    } else {
        c as Compare
        val count=c.left as? DynamicAmount.AggregateBattlefield
        val limit=(c.right as? DynamicAmount.Fixed)?.amount
        when {
            count==null || limit==null || count.aggregation!=Aggregation.COUNT || count.excludeSelf || count.excludeTriggeringEntity ||
                count.property!=null || count.counterType!=null -> Verdict.UNKNOWN
            count.player!=anchor.player -> if(count.player==Player.You && anchor.player==Player.EachOpponent ||
                count.player==Player.EachOpponent && anchor.player==Player.You) Verdict.NO_MATCH else Verdict.UNKNOWN
            count.filter!=anchor.filter -> Verdict.UNKNOWN
            anchor.count<0 -> Verdict.NO_MATCH
            else -> (when(c.operator) {
                ComparisonOperator.LT -> anchor.count<limit
                ComparisonOperator.LTE -> anchor.count<=limit
                ComparisonOperator.EQ -> anchor.count==limit
                ComparisonOperator.NEQ -> anchor.count!=limit
                ComparisonOperator.GT -> anchor.count>limit
                ComparisonOperator.GTE -> anchor.count>=limit
            }).let { if(it) Verdict.MATCH else Verdict.NO_MATCH }
        }
    }
}

private fun DynamicAmount.positiveFixed() = this is DynamicAmount.Fixed && amount > 0

/** Bounded operation recognition. Native execution and the output of a copy have separate evidence. */
private fun Effect.recognizedConditionalOperation(): Boolean = when (this) {
    is DrawCardsEffect -> count.positiveFixed()
    is AddCountersEffect -> count > 0
    is DealDamageEffect -> amount.positiveFixed()
    is LoseLifeEffect -> amount.positiveFixed()
    is GainLifeEffect -> amount.positiveFixed()
    is ModifyStatsEffect -> {
        val power = powerModifier as? DynamicAmount.Fixed
        val toughness = toughnessModifier as? DynamicAmount.Fixed
        power != null && toughness != null && (power.amount != 0 || toughness.amount != 0)
    }
    is CreateTokenEffect -> count.positiveFixed()
    is CreateTokenCopyOfSourceEffect -> count > 0
    is ForceSacrificeEffect -> dynamicCount == null && count > 0
    is ForEachEffect -> (space as? IterationSpace.Players)?.players in setOf(Player.Each, Player.EachOpponent) &&
        body is ForceSacrificeEffect && body.target == EffectTarget.Controller && body.recognizedConditionalOperation()
    else -> false
}

/** A zero fixed life change is the identity operation for the supported life effects. */
private fun Effect?.withoutZeroLifeChange(): Effect? = when (this) {
    is LoseLifeEffect -> takeUnless { amount == DynamicAmount.Fixed(0) }
    is GainLifeEffect -> takeUnless { amount == DynamicAmount.Fixed(0) }
    else -> this
}

internal fun conditionalEffectChange(whenTrue: Effect, whenFalse: Effect?): Verdict {
    val yes = whenTrue.withoutZeroLifeChange()
    val no = whenFalse.withoutZeroLifeChange()
    if (yes == no) return Verdict.NO_MATCH
    if (listOfNotNull(yes, no).any { !it.recognizedConditionalOperation() }) return Verdict.UNKNOWN
    // These operation types have payload fields whose equality is not a proof of different gameplay.
    // Support selecting their distinct instruction from another operation while leaving same-type
    // property comparisons unresolved.
    if ((yes is CreateTokenCopyOfSourceEffect && no is CreateTokenCopyOfSourceEffect) ||
        (yes is CreateTokenEffect && no is CreateTokenEffect)) return Verdict.UNKNOWN
    return Verdict.MATCH
}

fun stateDependencyDecisions(d: StateDependency, anchor: StateAnchor): List<Decision> {
    val predicate=predicateCompatibility(d.condition,anchor)
    return listOf(
        Decision("state-predicate",predicate,"MECHANICAL",d.condition.description),
        Decision("state-checkpoint-and-branch",d.contextSupport,"MECHANICAL",
            "${d.checkpoint}: ${d.actionId}, ${d.conditionPath} controls ${d.outcomePath}; additional prerequisites remain"),
        Decision("state-effect-change",d.causalChange,"MECHANICAL",
            "When true: ${d.whenTrue.description}; otherwise: ${d.whenFalse?.description ?: "this outcome is unavailable"}")
    )
}
