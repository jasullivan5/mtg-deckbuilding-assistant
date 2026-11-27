package dev.mtg.assistant.discovery

import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.conditions.*
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.TurnTracker
import kotlinx.serialization.Serializable

/** The parent ActionEvidence retains the full wrapper. This binds its condition to the inner grant. */
@Serializable data class ConditionalGrant(
    val wrapperPath: String,
    val innerPath: String,
    val condition: Condition,
    val support: Verdict,
    val checkpoint: ConditionalGrantCheckpoint,
    val unresolved: String? = null
)

@Serializable enum class ConditionalGrantCheckpoint { ACTIVATION_AVAILABILITY, TRIGGER_EVENT, PRE_DEPARTURE }

@Serializable data class GrantConditionObservation(
    val granter: ObjectRef,
    val recipient: ObjectRef,
    val granterController: EntityId,
    val verdict: Verdict,
    val checkpoint: String = "CURRENT_PROJECTED_STATE"
)

private fun boundedConditionFilter(filter: GameObjectFilter): Boolean =
    filter.anyOf.isEmpty() && filter.statePredicates.isEmpty() && filter.controllerPredicate == null &&
        filter.cardPredicates.isNotEmpty() && filter.cardPredicates.all {
            it == CardPredicate.IsCreature || it == CardPredicate.IsPermanent ||
                it is CardPredicate.HasColor || it is CardPredicate.HasSubtype
        }

internal fun conditionalGrantEvidence(path: String, condition: Condition, grant: StaticAbility): ConditionalGrant {
    val supported = when (condition) {
        is EntityMatches -> condition.entity in setOf(EffectTarget.Self, EffectTarget.EnchantedPermanent) &&
            boundedConditionFilter(condition.filter)
        is Exists -> condition.player == Player.You && !condition.negate &&
            condition.zone in setOf(Zone.BATTLEFIELD, Zone.GRAVEYARD) && boundedConditionFilter(condition.filter)
        is Compare -> condition.left == DynamicAmount.TurnTracking(Player.You, TurnTracker.SURVEILED) &&
            condition.operator == ComparisonOperator.GTE && condition.right == DynamicAmount.Fixed(1)
        else -> false
    }
    val trigger = (grant as? GrantTriggeredAbility)?.ability?.trigger
    val checkpoint = when {
        grant is GrantActivatedAbility -> ConditionalGrantCheckpoint.ACTIVATION_AVAILABILITY
        trigger is EventPattern.ZoneChangeEvent && trigger.from == Zone.BATTLEFIELD -> ConditionalGrantCheckpoint.PRE_DEPARTURE
        else -> ConditionalGrantCheckpoint.TRIGGER_EVENT
    }
    return ConditionalGrant(path, "$path.ability", condition, if (supported) Verdict.MATCH else Verdict.UNKNOWN,
        checkpoint, if (supported) null else "Condition is outside the bounded native grant condition shapes")
}

internal fun matchesGrantDeclaration(
    native: StaticAbility?, declaration: StaticAbility, index: Int, path: String, conditional: ConditionalGrant?
): Boolean {
    val root = "script.staticAbilities[$index]"
    if (conditional == null) return path == root && native == declaration
    return path == "$root.ability" && conditional.wrapperPath == root && conditional.innerPath == path &&
        native == ConditionalStaticAbility(declaration, conditional.condition) &&
        conditional == conditionalGrantEvidence(root, conditional.condition, declaration)
}

internal fun ConditionalGrant.observeCondition(
    state: GameState, services: EngineServices, granter: ObjectRef, recipient: ObjectRef, controller: EntityId
): GrantConditionObservation {
    val verdict = if (support != Verdict.MATCH) Verdict.UNKNOWN else if (
        services.conditionEvaluator.evaluate(state, condition, EffectContext(sourceId = granter.entityId, controllerId = controller))
    ) Verdict.MATCH else Verdict.NO_MATCH
    return GrantConditionObservation(granter, recipient, controller, verdict)
}

internal fun EventPattern.isBoundedGrantDeath(): Boolean =
    this is EventPattern.ZoneChangeEvent && from == Zone.BATTLEFIELD && to == Zone.GRAVEYARD &&
        !excludeSacrifice && !requireCraftMaterial && !requireEmergeSacrifice && excludeTo == null
