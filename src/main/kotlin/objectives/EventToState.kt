package dev.mtg.assistant.objectives

import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.projectedTypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

internal class EvaluatedMove(val move: ZoneChangeEvent, val qualifies: Boolean?)

/** The objects that can witness an existential state at one checkpoint. */
internal class CheckpointWitnesses(
    val matching: Set<ObjectRef>,
    val unresolved: Set<EntityId>
) {
    val satisfied: Boolean?
        get() = when {
            matching.isNotEmpty() -> true
            unresolved.isNotEmpty() -> null
            else -> false
        }
}

internal enum class ArrivalAttribution {
    NO_QUALIFYING_EVENT,
    STATE_UNSATISFIED,
    ARRIVAL_DOES_NOT_SUPPLY,
    ARRIVAL_SUPPLIES,
    UNRESOLVED
}

internal fun nontokenCreatureDeath(move: ZoneChangeEvent, controller: EntityId): EvaluatedMove {
    val before = move.lastKnown
    val qualifies = when {
        !move.isBattlefieldDeparture || move.toZone != Zone.GRAVEYARD -> false
        before == null -> null
        before.wasToken || before.typeLine?.isCreature == false -> false
        before.controllerId != null && before.controllerId != controller -> false
        before.typeLine == null || before.controllerId == null -> null
        else -> true
    }
    return EvaluatedMove(move, qualifies)
}

internal fun graveyardCreatureCards(state: GameState, player: EntityId): CheckpointWitnesses {
    val matching = mutableSetOf<ObjectRef>()
    val unresolved = mutableSetOf<EntityId>()
    for (id in state.getGraveyard(player)) {
        if (state.getEntity(id)?.has<TokenComponent>() == true) continue
        val creature = projectedTypeLine(state, id)?.isCreature
        val ref = state.objectRef(id)
        when {
            creature == false -> Unit
            creature == true && ref != null -> matching += ref
            else -> unresolved += id
        }
    }
    return CheckpointWitnesses(matching, unresolved)
}

/**
 * Join an evaluated move to a later checkpoint from the same execution. This tests whether the
 * exact arrival supplies a witness, including when other witnesses already satisfy the state.
 * A missing event or checkpoint (for example, static-only evidence) leaves the join unresolved.
 */
internal fun attributeArrival(event: EvaluatedMove?, state: CheckpointWitnesses?): ArrivalAttribution {
    if (event?.qualifies == false) return ArrivalAttribution.NO_QUALIFYING_EVENT
    if (state?.satisfied == false) return ArrivalAttribution.STATE_UNSATISFIED
    if (event?.qualifies != true || state == null) return ArrivalAttribution.UNRESOLVED

    val move = event.move
    val departed = move.oldObject
    val arrived = move.newObject
    return when {
        departed == null || arrived == null || departed == arrived -> ArrivalAttribution.UNRESOLVED
        departed.entityId != move.entityId || arrived.entityId != move.entityId -> ArrivalAttribution.UNRESOLVED
        arrived in state.matching -> ArrivalAttribution.ARRIVAL_SUPPLIES
        arrived.entityId in state.unresolved -> ArrivalAttribution.UNRESOLVED
        else -> ArrivalAttribution.ARRIVAL_DOES_NOT_SUPPLY
    }
}
