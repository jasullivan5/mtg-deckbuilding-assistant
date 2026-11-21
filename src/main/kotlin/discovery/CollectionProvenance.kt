package dev.mtg.assistant.discovery

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.Chooser
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.ZoneTransitionCause
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.sdk.model.EntityId

/** Alternative clauses belong to ONE requirement. Neither selection nor movement is asserted. */
@Serializable data class UnionTarget(
    val actionId: String, val requirementIndex: Int, val binding: String?,
    val requirement: TargetObject, val clauses: List<TargetFilter>,
    val minimum: Int, val maximum: Int?, val checkpoint: String,
    val support: Verdict, val unresolved: List<String>,
    val operationPaths: List<String> = emptyList(), val movementFactIds: List<String> = emptyList())

internal fun unionTargets(actionId: String, targets: List<TargetRequirement>, checkpoint: String): List<UnionTarget> =
    targets.mapIndexedNotNull { index, raw ->
        val t = raw as? TargetObject ?: return@mapIndexedNotNull null
        if (!t.filter.isUnion) return@mapIndexedNotNull null
        val problems = listOfNotNull(
            if (targets.size != 1 || t.count != 1 || t.unlimited || t.dynamicMaxCount != null || t.dynamicMinCount != null)
                "Only one fixed object-target requirement is supported" else null,
            if (t.chooser != TargetChooser.Controller) "Chooser requires additional binding" else null,
            // Retain all native correlated restrictions rather than interpreting their English.
            if (nativeJson.encodeToJsonElement(TargetRequirement.serializer(), t).jsonObject.keys.any {
                    it !in setOf("type", "count", "minCount", "optional", "filter", "id", "chooser", "descriptionOverride")
                }) "Additional target restriction requires native validation" else null)
        UnionTarget(actionId,index,t.id,t,t.filter.clauses(),t.effectiveMinCount,
            t.count.takeUnless { t.unlimited || t.dynamicMaxCount != null },checkpoint,
            if (problems.isEmpty()) Verdict.MATCH else Verdict.UNKNOWN,problems)
    }

@Serializable enum class CollectionMembership { GATHERED, SELECTED, MATCHING, COMPLEMENT, ALIAS, NATIVE_MOVED_RESULT, RETAINED_INPUT }

/** An action-local, versioned binding. Zero is always a possible structural membership count. */
@Serializable data class CollectionStage(
    val id: String, val actionId: String, val path: String, val variable: String,
    val membership: CollectionMembership, val inputId: String?, val minimum: Int = 0,
    val maximum: Int?, val sourceZone: Zone?, val destinationZone: Zone?,
    val movementFactIds: List<String>,
    val prerequisites: List<String>, val support: Verdict, val unresolved: List<String>,
    val selection: SelectionMode? = null, val chooser: Chooser? = null,
    val identity: String = "Original cards are related only through native transitions; each zone transition creates a new ObjectRef generation. Native stored IDs alone do not establish current membership or arrival.")

@Serializable data class CollectionUse(
    val actionId: String, val path: String, val variable: String, val producerId: String?,
    val consumer: String, val index: Int?, val support: Verdict, val prerequisites: List<String>,
    val unresolved: List<String>, val movementFactIds: List<String> = emptyList())

@Serializable data class CollectionMovementObservation(
    val stageId: String, val input: List<ObjectRef>, val transitions: List<ZoneChangeEvent>,
    val nativeStoredIds: List<EntityId>?, val arrived: List<ObjectRef>, val currentArrivals: List<ObjectRef>,
    val verdict: Verdict, val reason: String)

/** Supply ONE movement instruction's event slice and its native result binding, when observable.
 * Raw stored IDs never establish identity: only a transition from a supplied generation can do so.
 * This observes execution; it neither computes collection membership nor executes a replacement. */
fun CollectionStage.observeMovement(state: GameState, input: List<ObjectRef>, events: List<GameEvent>,
    nativeStoredIds: List<EntityId>? = null): CollectionMovementObservation {
    val transitions=events.filterIsInstance<ZoneChangeEvent>().filter {
        it.oldObject in input && it.transitionCause==ZoneTransitionCause.PRIMARY
    }
    val arrivals=transitions.filter { it.toZone==destinationZone && it.lastKnown?.wasToken!=true }
        .mapNotNull { event -> event.newObject?.takeIf { it.entityId==event.oldObject?.entityId && it!=event.oldObject } }
    val invalid=membership!=CollectionMembership.NATIVE_MOVED_RESULT || destinationZone==null ||
        transitions.any { it.lastKnown?.wasToken==true || (it.fromZone==Zone.BATTLEFIELD && it.lastKnown==null) ||
            (sourceZone!=null && it.fromZone!=sourceZone) || it.newObject==null || it.meldedPermanent!=null } ||
        (nativeStoredIds==null && input.isNotEmpty() && transitions.isEmpty()) ||
        nativeStoredIds?.any { id -> transitions.none { it.newObject?.entityId==id } }==true
    return CollectionMovementObservation(id,input,transitions,nativeStoredIds,arrivals,
        arrivals.filter(state::isCurrentObject),if(invalid) Verdict.UNKNOWN else Verdict.MATCH,
        "Observed only the supplied instruction and generations. Stored native movement results and requested-zone arrivals remain distinct; empty input creates no object. No whole-card source fidelity or future continuation is established.")
}
