package dev.mtg.assistant.discovery

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.Serializable
import kotlinx.serialization.EncodeDefault

@Serializable enum class TargetCheckpoint { CAST_ANNOUNCEMENT }
@Serializable enum class TargetRequirementOrigin { EXPLICIT_SPELL, AURA_ENCHANT }
@Serializable enum class TargetQuantifier { ANY_SELECTED }

@Serializable data class ChosenTargetCondition(
    val filter: GameObjectFilter,
    val quantifier: TargetQuantifier = TargetQuantifier.ANY_SELECTED
)

@Serializable data class TargetSelection(
    val actionId: String,
    val requirements: List<TargetRequirement>,
    val checkpoint: TargetCheckpoint = TargetCheckpoint.CAST_ANNOUNCEMENT,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val origin: TargetRequirementOrigin = TargetRequirementOrigin.EXPLICIT_SPELL
) {
    internal fun potential(condition: ChosenTargetCondition): Verdict =
        if (requirements.isNotEmpty() && requirements.all { it is TargetObject && it.supportsFixedBinding() } &&
            requirements.filterIsInstance<TargetObject>().any { it.supportsIntersection(condition.filter) }) Verdict.MATCH
        else Verdict.UNKNOWN

    /** Query observed choices using the same native validator and filter evaluator as casting. */
    internal fun atAnnouncement(
        action: ActionEvidence,
        definition: CardDefinition,
        cast: CastSpell,
        state: GameState,
        evaluator: PredicateEvaluator,
        condition: ChosenTargetCondition
    ): SelectedTargetAssessment {
        val nativeRequirements = when (origin) {
            TargetRequirementOrigin.EXPLICIT_SPELL -> definition.script.targetRequirements
            TargetRequirementOrigin.AURA_ENCHANT -> {
                if (definition.script.targetRequirements.isNotEmpty()) return SelectedTargetAssessment(Verdict.UNKNOWN)
                listOfNotNull(definition.script.auraTarget)
            }
        }
        if (action.id != actionId || action.targetSelection != this || action.card != definition.name ||
            state.getEntity(cast.cardId)?.get<CardComponent>()?.name != definition.name ||
            nativeRequirements != requirements ||
            cast.chosenModes.isNotEmpty() || cast.castFaceDown || cast.castPrototyped || cast.castFused || cast.declaredCostSlot != null ||
            cast.alternativePayment != null || cast.useAlternativeCost || cast.faceIndex != null || cast.splicedCardIds.isNotEmpty()) return SelectedTargetAssessment(Verdict.UNKNOWN)
        val binding = TargetValidator(evaluator).bindTargetGroups(
            state, cast.targets, requirements, cast.playerId, definition.colors,
            definition.typeLine.subtypes.map { it.toString() }.toSet(), cast.cardId, cast.xValue,
            TargetingSourceType.SPELL, explicitCounts = cast.targetGroupCounts
        )
        if (binding.error != null || binding.counts.sum() != cast.targets.size)
            return SelectedTargetAssessment(Verdict.UNKNOWN)

        var offset = 0
        val groups = binding.counts.map { count ->
            cast.targets.subList(offset, offset + count).toList().also { offset += count }
        }
        val projected = state.projectedState
        val context = PredicateContext(controllerId = cast.playerId)
        val verdicts = cast.targets.map { target ->
            when (target) {
                is ChosenTarget.Player -> Verdict.UNKNOWN
                is ChosenTarget.Card -> matchesTarget(condition.filter, target.zone, target.cardId, state, projected, context, evaluator)
                is ChosenTarget.Permanent -> matchesTarget(condition.filter, Zone.BATTLEFIELD, target.entityId, state, projected, context, evaluator)
                is ChosenTarget.Spell -> matchesTarget(condition.filter, Zone.STACK, target.spellEntityId, state, projected, context, evaluator)
            }
        }
        return SelectedTargetAssessment(any(verdicts), groups)
    }
}

/** Group index identifies the native requirement, and values are actual announced target identities. */
internal data class SelectedTargetAssessment(val verdict: Verdict, val groups: List<List<ChosenTarget>> = emptyList())

private fun matchesTarget(
    filter: GameObjectFilter, zone: Zone, id: EntityId,
    state: GameState, projected: ProjectedState,
    context: PredicateContext, evaluator: PredicateEvaluator
): Verdict = when {
    !filter.supportedAt(zone) -> Verdict.UNKNOWN
    evaluator.matches(state, projected, id, filter, context) -> Verdict.MATCH
    else -> Verdict.NO_MATCH
}

private fun TargetObject.supportsFixedBinding() =
    !unlimited && dynamicMaxCount == null && dynamicMinCount == null && !filter.isUnion &&
        !filter.excludeTriggeringEntity && !sameController && !sameOwner && ownerDistribution == null &&
        sameNumericProperty == null && differentNumericProperty == null && !sameCreatureType &&
        !sameCardType && totalAtMost == null && !differentNames && !differentControllers &&
        !onePerCardType && !onePerColor && chooser == com.wingedsheep.sdk.scripting.targets.TargetChooser.Controller

/** Recognize bounded intersections with broadly targeted objects in the selected zone. */
private fun TargetObject.supportsIntersection(discount: GameObjectFilter): Boolean {
    val base = filter.baseFilter
    if (count <= 0 || base.anyOf.isNotEmpty() || base.statePredicates.isNotEmpty() ||
        !discount.supportedAt(filter.zone)) return false
    return when (filter.zone) {
        Zone.STACK -> base == GameObjectFilter.Any
        Zone.GRAVEYARD -> base == TargetFilter.CreatureInYourGraveyard.baseFilter
        Zone.BATTLEFIELD -> base.controllerPredicate in setOf(null, ControllerPredicate.ControlledByYou) &&
            base.cardPredicates.toSet() in setOf(
            setOf(CardPredicate.IsCreature), setOf(CardPredicate.IsPermanent),
            setOf(CardPredicate.IsPermanent, CardPredicate.IsNonland)
        )
        else -> false
    }
}

private fun GameObjectFilter.supportedAt(zone: Zone): Boolean {
    if (anyOf.isNotEmpty()) return false
    return when (zone) {
        Zone.STACK -> statePredicates.isEmpty() && controllerPredicate == null &&
            (this == GameObjectFilter.InstantOrSorcery || cardPredicates == listOf(CardPredicate.IsCreature))
        Zone.GRAVEYARD -> statePredicates.isEmpty() && controllerPredicate == null &&
            CardPredicate.IsCreature in cardPredicates && cardPredicates.all {
                it == CardPredicate.IsCreature || it is CardPredicate.ManaValueAtMost && it.max >= 0
            }
        Zone.BATTLEFIELD -> (controllerPredicate == null || controllerPredicate == ControllerPredicate.ControlledByYou) &&
            (CardPredicate.IsCreature in cardPredicates || CardPredicate.IsPermanent in cardPredicates) &&
            cardPredicates.count { it != CardPredicate.IsCreature && it != CardPredicate.IsPermanent } <= 1 &&
            statePredicates.size <= 1 &&
            cardPredicates.all { it.supportedPermanentProperty() } && statePredicates.all { it.supportedBattlefieldState() }
        else -> false
    }
}

private fun CardPredicate.supportedPermanentProperty() = when (this) {
    CardPredicate.IsCreature, CardPredicate.IsPermanent, CardPredicate.IsLegendary,
    CardPredicate.IsToken, CardPredicate.IsNontoken, is CardPredicate.HasColor,
    is CardPredicate.HasSubtype, is CardPredicate.HasKeyword -> true
    is CardPredicate.ManaValueEquals -> value >= 0
    else -> false
}

private fun StatePredicate.supportedBattlefieldState(): Boolean = when (this) {
    StatePredicate.IsOnBattlefield, StatePredicate.IsTapped, StatePredicate.IsAttacking,
    StatePredicate.IsBlocking, is StatePredicate.HasCounter -> true
    is StatePredicate.Or -> predicates.isNotEmpty() && predicates.all { it.supportedBattlefieldState() }
    else -> false
}
