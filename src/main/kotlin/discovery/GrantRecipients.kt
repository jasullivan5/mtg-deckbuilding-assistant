package dev.mtg.assistant.discovery

import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.mechanics.SoulbondPairing
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.TargetObject

internal fun grantRecipientKind(card: CardDefinition, filter: GroupFilter): GrantRecipientKind = when {
    filter.scope == Scope.AttachedTo && card.isAura -> GrantRecipientKind.AURA_HOST
    filter.scope == Scope.AttachedTo && card.isEquipment -> GrantRecipientKind.EQUIPMENT_HOST
    filter.scope == Scope.Battlefield -> GrantRecipientKind.FILTER_GROUP
    filter.scope == Scope.SoulbondPair -> GrantRecipientKind.SOULBOND_PAIR
    else -> GrantRecipientKind.UNRESOLVED
}

/** Recognize whole bounded shapes. Native predicates remain the authority for current membership. */
internal fun sharedGrantRecipientProblems(card: CardDefinition, filter: GroupFilter): List<String> {
    val problem = when (grantRecipientKind(card, filter)) {
        GrantRecipientKind.AURA_HOST, GrantRecipientKind.EQUIPMENT_HOST -> when {
            filter != GroupFilter.attachedCreature() -> "Additional attached recipient restrictions"
            card.isAura && card.script.auraTarget !in listOf(
                TargetObject(filter = TargetFilter.Creature), TargetObject(filter = TargetFilter.Creature.youControl())
            ) -> "Aura enchant restriction outside bounded creature hosts"
            else -> null
        }
        GrantRecipientKind.SOULBOND_PAIR ->
            if (filter == GroupFilter.soulbondPair()) null else "Additional soulbond recipient restrictions"
        GrantRecipientKind.FILTER_GROUP ->
            if (boundedBattlefieldGrant(filter)) null else "Group filter requires unsupported or correlated predicates"
        GrantRecipientKind.UNRESOLVED -> "Recipient scope outside bounded attachment, battlefield and soulbond grants"
    }
    return listOfNotNull(problem)
}

private fun boundedBattlefieldGrant(group: GroupFilter): Boolean {
    val filter = group.baseFilter
    return group.chosenSubtypeKey == null && !group.excludeTarget && !group.excludeTriggeringEntity &&
        filter.anyOf.isEmpty() && filter.statePredicates.isEmpty() &&
        filter.controllerPredicate in listOf(null, ControllerPredicate.ControlledByYou, ControllerPredicate.ControlledByOpponent) &&
        filter.cardPredicates.any { it == CardPredicate.IsCreature || it == CardPredicate.IsPermanent || it == CardPredicate.IsArtifact } &&
        filter.cardPredicates.all {
            it == CardPredicate.IsCreature || it == CardPredicate.IsPermanent || it == CardPredicate.IsArtifact ||
                it is CardPredicate.HasSubtype || it is CardPredicate.HasColor
        }
}

/** Body-relative YOU is the recipient's controller. Granter-relative constraints stay on the declaration. */
internal fun grantBodyRecipientFilter(group: GroupFilter, supported: Boolean): GameObjectFilter {
    if (!supported) return GameObjectFilter.Any
    if (group.scope != Scope.Battlefield) return GameObjectFilter.Creature
    return group.baseFilter.copy(controllerPredicate = null)
}

internal data class CurrentGrantRecipient(val pairedPartner: ObjectRef? = null)

internal fun matchSharedGrantRecipient(
    state: GameState, services: EngineServices, filter: GroupFilter, granter: EntityId, recipient: EntityId,
    granterController: EntityId
): CurrentGrantRecipient? = when (filter.scope) {
    Scope.Battlefield -> if ((!filter.excludeSelf || granter != recipient) &&
        services.predicateEvaluator.matches(
            state, state.projectedState, recipient, filter.baseFilter,
            PredicateContext(controllerId = granterController, sourceId = granter)
        )) CurrentGrantRecipient() else null
    // The native helper rejects asymmetric/departed links. The continuous validity conditions
    // also matter before an SBA pass has cleaned a changed controller/type from the components.
    Scope.SoulbondPair -> SoulbondPairing.partnerOf(state, granter)?.takeIf { partner ->
        (recipient == granter || recipient == partner) &&
            state.projectedState.isCreature(granter) && state.projectedState.isCreature(partner) &&
            state.projectedState.getController(partner) == granterController
    }?.let(state::objectRef)?.let(::CurrentGrantRecipient)
    Scope.AttachedTo -> if (state.projectedState.isCreature(recipient)) CurrentGrantRecipient() else null
    else -> null
}
