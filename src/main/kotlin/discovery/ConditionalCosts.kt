package dev.mtg.assistant.discovery

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.CreatureDiedThisTurnCondition
import com.wingedsheep.sdk.scripting.conditions.PlayerDrewCardsThisTurn
import com.wingedsheep.sdk.scripting.costs.*
import com.wingedsheep.sdk.scripting.references.Player
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable enum class PaymentCheckpoint { CASTING, ACTIVATION }
@Serializable enum class ReductionKind { GENERIC, MANA_SYMBOLS }

/** One conditional reduction, independent of action permissions and resolution effects. */
@Serializable data class ConditionalCost(
    val declarationPath: String,
    val checkpoint: PaymentCheckpoint,
    val actor: Player,
    val kind: ReductionKind,
    val condition: Condition?,
    val reductionSource: CostReductionSource?,
    val baseMana: ManaCost?,
    val reduction: ManaCost?,
    val isolatedManaWhenTrue: ManaCost?,
    val native: JsonElement,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val chosenTargetCondition: ChosenTargetCondition? = null
) {
    val verdict: Verdict get() = when {
        isolatedManaWhenTrue == null -> Verdict.UNKNOWN
        baseMana == isolatedManaWhenTrue -> Verdict.NO_MATCH
        else -> Verdict.MATCH
    }
}

private fun ManaCost.isFixed() = symbols.all { it is ManaSymbol.Generic || it is ManaSymbol.Colored || it == ManaSymbol.Colorless }

internal fun activationConditionalCost(path: String, ability: ActivatedAbility): ConditionalCost? {
    val change = ability.conditionalCostReduction ?: return null
    val base = ((ability.cost as? AbilityCost.Atom)?.atom as? CostAtom.Mana)?.cost
    val predicate = change.condition
    val known = predicate == CreatureDiedThisTurnCondition ||
        predicate is PlayerDrewCardsThisTurn && predicate.player == Player.You && predicate.atLeast > 0
    val isolated = base?.takeIf { known && it.isFixed() && change.reduction.isFixed() && ability.genericCostReduction == null }
        ?.subtract(change.reduction)
    return ConditionalCost("$path.conditionalCostReduction", PaymentCheckpoint.ACTIVATION, Player.You,
        ReductionKind.MANA_SYMBOLS, predicate, null, base, change.reduction, isolated,
        nativeJson.encodeToJsonElement(ConditionalCostReduction.serializer(), change))
}

internal fun selfCastingConditionalCost(path: String, ability: StaticAbility, base: ManaCost,
    selection: TargetSelection? = null
): ConditionalCost? {
    val change = ability as? ModifySpellCost ?: return null
    if (change.target != SpellCostTarget.SelfCast) return null
    val modification = change.modification
    val source = (modification as? CostModification.ReduceGenericBy)?.source
    val colored = modification as? CostModification.ReduceColoredIfAnyTargetMatches
    if (source == null && colored == null) return null
    val selected = when {
        source is CostReductionSource.FixedIfAnyTargetMatches -> ChosenTargetCondition(source.filter)
        colored != null -> ChosenTargetCondition(colored.filter)
        else -> null
    }
    if (source is CostReductionSource.Fixed && change.gating == CostGating.None && change.optionalLifePayment == null) return null
    val actor = Player.You
    val condition = when (source) {
        is CostReductionSource.FixedIfCreatureDiedThisTurn -> CreatureDiedThisTurnCondition
        is CostReductionSource.FixedIfControlFilter -> controlledBattlefieldExists(actor, source.filter)
        else -> null
    }
    val amount = when (source) {
        is CostReductionSource.FixedIfCreatureDiedThisTurn -> source.amount
        is CostReductionSource.FixedIfControlFilter -> source.amount
        is CostReductionSource.FixedIfAnyTargetMatches -> source.amount
        else -> null
    }?.takeIf { it >= 0 }
    val predicateSupported = when (source) {
        is CostReductionSource.FixedIfCreatureDiedThisTurn -> true
        is CostReductionSource.FixedIfControlFilter ->
            controlledBattlefieldExistenceSupport(actor, source.filter) == Verdict.MATCH
        else -> selected != null && selection?.potential(selected) == Verdict.MATCH
    }
    val reduction = if (colored == null) amount?.let { ManaCost.parse("{$it}") }
        else ManaCost.parse(colored.symbols).takeIf { it.symbols.all { symbol -> symbol is ManaSymbol.Colored } }
    val isolated = reduction?.takeIf {
        predicateSupported && base.isFixed() && change.gating == CostGating.None && change.optionalLifePayment == null
    }?.let { if (colored == null) base.reduceGeneric(requireNotNull(amount)) else base.subtract(it) }
    return ConditionalCost(path, PaymentCheckpoint.CASTING, actor,
        if (colored == null) ReductionKind.GENERIC else ReductionKind.MANA_SYMBOLS,
        condition, source, base, reduction, isolated,
        nativeJson.encodeToJsonElement(StaticAbility.serializer(), change), selected)
}
