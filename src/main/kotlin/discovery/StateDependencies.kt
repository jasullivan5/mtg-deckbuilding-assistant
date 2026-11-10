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
@Serializable data class StateDependency(
    val card: String, val face: String, val actionId: String, val conditionPath: String,
    val outcomePath: String, val condition: Condition, val scope: StateScope, val checkpoint: StateCheckpoint,
    val whenTrue: Effect, val whenFalse: Effect? = null, val contextSupport: Verdict,
    val prerequisites: List<String>, val incomingEvent: EventContext? = null
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

private fun fixed(amount: DynamicAmount) = (amount as? DynamicAmount.Fixed)?.amount
private fun increase(a: Int?, b: Int?): Verdict = when {
    a==null || b==null -> Verdict.UNKNOWN
    a>b && b>=0 -> Verdict.MATCH
    else -> Verdict.NO_MATCH
}

/** Compare only known, like-for-like effect magnitudes. No ranking of unlike effects. */
internal fun stateEffectBenefit(a: Effect,b: Effect?): Verdict = when {
    a==b -> Verdict.NO_MATCH
    a is DrawCardsEffect && (b==null || b is DrawCardsEffect && a.target==b.target) ->
        increase(fixed(a.count),if(b==null) 0 else fixed((b as DrawCardsEffect).count))
    a is AddCountersEffect && (b==null || b is AddCountersEffect && a.target==b.target && a.counterType==b.counterType) ->
        increase(a.count,(b as? AddCountersEffect)?.count ?: 0)
    a is DealDamageEffect && (b==null || b is DealDamageEffect && a.copy(amount=b.amount)==b) ->
        increase(fixed(a.amount),if(b==null) 0 else fixed((b as DealDamageEffect).amount))
    a is ModifyStatsEffect && (b==null || b is ModifyStatsEffect && a.target==b.target && a.duration==b.duration) -> {
        val ap=fixed(a.powerModifier); val at=fixed(a.toughnessModifier)
        val bp=if(b==null) 0 else fixed((b as ModifyStatsEffect).powerModifier)
        val bt=if(b==null) 0 else fixed((b as ModifyStatsEffect).toughnessModifier)
        if(ap==null || at==null || bp==null || bt==null) Verdict.UNKNOWN
        else if(ap<=bp && at<=bt && ap<=0 && at<=0 && bp<=0 && bt<=0 && (ap<bp || at<bt)) Verdict.MATCH
        else if(ap>=bp && at>=bt && ap>=0 && at>=0 && bp>=0 && bt>=0 && (ap>bp || at>bt)) Verdict.MATCH
        else Verdict.UNKNOWN
    }
    a is CreateTokenEffect && b==null && a.controller==null && !a.attacking && a.dynamicPower==null && a.dynamicToughness==null ->
        increase(fixed(a.count),0)
    else -> Verdict.UNKNOWN
}

fun stateDependencyDecisions(d: StateDependency, anchor: StateAnchor): List<Decision> {
    val predicate=predicateCompatibility(d.condition,anchor)
    // A stronger false branch could be the benefit under a negated or compound condition.
    // Do not use the true-branch comparison to reject an unresolved anchor polarity.
    val change=if(predicate==Verdict.UNKNOWN && d.whenTrue!=d.whenFalse) Verdict.UNKNOWN
        else stateEffectBenefit(d.whenTrue,d.whenFalse)
    return listOf(
    Decision("state-predicate",predicate,"MECHANICAL",d.condition.description),
    Decision("state-checkpoint-and-branch",d.contextSupport,"MECHANICAL",
        "${d.checkpoint}: ${d.actionId}, ${d.conditionPath} controls ${d.outcomePath}; additional prerequisites remain"),
    Decision("state-effect-change",change,"MECHANICAL",
        "When true: ${d.whenTrue.description}; otherwise: ${d.whenFalse?.description ?: "this outcome is unavailable"}")
)
}
