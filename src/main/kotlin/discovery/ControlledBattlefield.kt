package dev.mtg.assistant.discovery

import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/** An existential battlefield condition, with the complete native filter retained. */
internal fun controlledBattlefieldExists(controller: Player, filter: GameObjectFilter) =
    Compare(DynamicAmount.AggregateBattlefield(controller, filter), ComparisonOperator.GTE, DynamicAmount.Fixed(1))

/**
 * Structural support for the bounded filter shapes observed in self-cast costs.
 * Actual witnesses are evaluated by Argentum against the projected battlefield.
 */
internal fun controlledBattlefieldExistenceSupport(controller: Player, filter: GameObjectFilter): Verdict {
    if (controller != Player.You) return Verdict.UNKNOWN
    if (filter.statePredicates.isNotEmpty() || filter.anyOf.isNotEmpty()) return Verdict.UNKNOWN
    val controllerScope = ObjectScope(filter.copy(cardPredicates = emptyList()), actor = "YOU")
        .compatible(Requirements(creature = false, controller = Controller.YOU))
    if (controllerScope != Verdict.MATCH) return controllerScope

    val types = filter.cardPredicates.filter { it in setOf(
        CardPredicate.IsCreature, CardPredicate.IsLand, CardPredicate.IsArtifact,
        CardPredicate.IsEnchantment, CardPredicate.IsPermanent
    ) }
    val restrictions = filter.cardPredicates - types.toSet()
    if (types.size > 1 || restrictions.size > 1) return Verdict.UNKNOWN
    return when (val restriction = restrictions.singleOrNull()) {
        null, is CardPredicate.HasSubtype, CardPredicate.IsLegendary -> Verdict.MATCH
        is CardPredicate.HasAnyOfSubtypes -> if (restriction.subtypes.isEmpty()) Verdict.UNKNOWN else Verdict.MATCH
        is CardPredicate.HasKeyword, is CardPredicate.PowerAtLeast ->
            if (CardPredicate.IsCreature in types) Verdict.MATCH else Verdict.UNKNOWN
        else -> Verdict.UNKNOWN
    }
}
