package dev.mtg.assistant.discovery

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.ZoneTransitionCause
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.linkedexile.LinkedExileLookup
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Zone
import kotlinx.serialization.Serializable

@Serializable data class MovementObservation(val verdict: Verdict, val departureObject: ObjectRef,
    val intermediateObject: ObjectRef? = null, val returnedObject: ObjectRef? = null,
    val currentResult: Boolean = false, val reason: String)

@Serializable data class LinkedSourceVisit(val source: ObjectRef, val battlefieldTimestamp: Long,
    val controller: EntityId)

/** Capture only a current native battlefield visit. The handle records no exiled cards. */
fun captureLinkedSource(state: GameState, source: ObjectRef): LinkedSourceVisit? {
    if (!state.isCurrentObject(source) || source.entityId !in state.getBattlefield()) return null
    val entity=state.getEntity(source.entityId) ?: return null
    return LinkedSourceVisit(source,entity.get<BattlefieldEntryTimestampComponent>()?.timestamp ?: return null,
        state.projectedState.getController(source.entityId) ?: return null)
}

/** Read Argentum's existing live/departed pile. An empty pile is only empty now: it is never
 * a promise to consume cards exiled after an earlier return operation resolved. */
fun LinkedMovement.linkedExileNow(state: GameState, visit: LinkedSourceVisit): List<ObjectRef>? {
    if (kind==MovementLinkKind.DURATION_BOUND && support==Verdict.MATCH) {
        return state.zoneReturns.filter { it.source==visit.source && state.isCurrentObject(it.movedObject) &&
            state.logicalZone(it.movedObject.entityId)?.zoneType==Zone.EXILE }.map { it.movedObject }
    }
    if (kind!=MovementLinkKind.DEFERRED_SOURCE_LINKED || support!=Verdict.MATCH) return null
    val context=EffectContext(controllerId=visit.controller,sourceId=visit.source.entityId,
        sourceBattlefieldTimestamp=visit.battlefieldTimestamp)
    return LinkedExileLookup.exiledCards(state,context).mapNotNull(state::objectRef)
}

/** Caller supplies the event slice of one native resolution and the selected starting object.
 * Events are observations, not a substitute for native target legality or replacement execution.
 * A raw ID/name match never advances an object reference. */
fun LinkedMovement.observeResolution(state: GameState, selected: ObjectRef,
    events: List<GameEvent>): MovementObservation {
    fun missing(reason: String, intermediate: ObjectRef? = null) =
        MovementObservation(Verdict.UNKNOWN,selected,intermediate,reason=reason)
    if (kind != MovementLinkKind.IMMEDIATE_ORDERED || support != Verdict.MATCH)
        return missing("No supported immediate declaration; deferred relationships require native source-visit lookup")
    val changes=events.filterIsInstance<ZoneChangeEvent>()
    val exile=changes.singleOrNull { it.oldObject==selected && it.fromZone==Zone.BATTLEFIELD &&
        it.toZone==Zone.EXILE && it.transitionCause==ZoneTransitionCause.PRIMARY }
        ?: return missing("No unique successful primary exile of the selected generation")
    if (exile.lastKnown?.wasToken == true)
        return MovementObservation(Verdict.NO_MATCH,selected,exile.newObject,reason="A token is not an original card that can return")
    if (exile.lastKnown == null) return missing("Departure lacks native original-card/token provenance")
    val intermediate=exile.newObject ?: return missing("Exile produced no new object")
    if (intermediate.entityId!=selected.entityId || intermediate==selected)
        return missing("Exile did not preserve the original card with a new object generation")
    val next=changes.drop(changes.indexOf(exile)+1).firstOrNull { it.oldObject==intermediate }
        ?: return missing("No continuation from the exile generation",intermediate)
    val result=next.newObject
    if (next.fromZone!=Zone.EXILE || next.toZone!=Zone.BATTLEFIELD ||
        next.transitionCause!=ZoneTransitionCause.PRIMARY || result==null ||
        result.entityId!=selected.entityId || result==selected || result==intermediate)
        return missing("Next movement is not a primary return of this original card to a new battlefield object",intermediate)
    return MovementObservation(Verdict.MATCH,selected,intermediate,result,state.isCurrentObject(result),
        "Observed native transition chain; success applies only to this supplied resolution, not future returns")
}
