package dev.mtg.assistant.discovery

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.predicates.EnchantRestriction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentHostLeftComponent
import com.wingedsheep.engine.state.components.battlefield.GainedEnchantRestrictionComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable enum class AttachmentCheckpoint { CURRENT_ATTACHMENT, CONTINUOUS_EVALUATION, TRIGGER_DETECTION }
@Serializable enum class AttachmentOperationKind { CONTINUOUS_STATS, TRIGGERED_OPERATION, UNRESOLVED }

/** One declaration using, granting to, or potentially referring to the host. */
@Serializable data class AttachmentDependency(
    val path: String,
    val actionId: String,
    val kind: AttachmentOperationKind,
    val checkpoint: AttachmentCheckpoint,
    val verdict: Verdict,
    val reason: String,
    val native: JsonElement,
    val stats: ModifyStats? = null,
    val eventOutcomeIds: List<String> = emptyList(),
    val timingPlayer: Player? = null,
    val recipient: EffectTarget? = null
)

/** Structural source-to-host relationship. A casting selection and a current host are separate evidence. */
@Serializable data class AuraAttachment(
    val actionId: String,
    val restrictionPath: String = "script.auraTarget",
    val restriction: TargetRequirement?,
    val sourceZone: Zone = Zone.BATTLEFIELD,
    val hostReference: EffectTarget = EffectTarget.EnchantedPermanent,
    val hostZone: Zone? = null,
    val support: Verdict,
    val dependencies: List<AttachmentDependency>
) {
    /** Inspect a current native relationship. A remembered host must still be the same object generation. */
    internal fun observe(
        action: ActionEvidence, state: GameState, source: ObjectRef, registry: CardRegistry,
        evaluator: PredicateEvaluator, expectedHost: ObjectRef? = null
    ): AttachmentObservation {
        if (action.id != actionId || action.attachment != this) return AttachmentObservation(Verdict.UNKNOWN)
        if (!state.isCurrentObject(source) || state.logicalZone(source.entityId)?.zoneType != sourceZone)
            return AttachmentObservation(Verdict.NO_MATCH)
        if (state.getEntity(source.entityId)?.has<AttachmentHostLeftComponent>() == true)
            return AttachmentObservation(Verdict.NO_MATCH)
        if (state.getEntity(source.entityId)?.has<GainedEnchantRestrictionComponent>() == true)
            return AttachmentObservation(Verdict.UNKNOWN)
        val sourceCard = state.getEntity(source.entityId)?.get<CardComponent>() ?: return AttachmentObservation(Verdict.UNKNOWN)
        val definition = registry.getCard(sourceCard.cardDefinitionId) ?: return AttachmentObservation(Verdict.UNKNOWN)
        if (sourceCard.name != action.card || action.face != "front" || !definition.isAura ||
            definition.script.auraTarget != restriction) return AttachmentObservation(Verdict.UNKNOWN)
        val hostId = state.getEntity(source.entityId)?.get<AttachedToComponent>()?.targetId
            ?: return AttachmentObservation(Verdict.NO_MATCH)
        val host = state.objectRef(hostId) ?: return AttachmentObservation(Verdict.UNKNOWN)
        if (expectedHost != null && expectedHost != host) return AttachmentObservation(Verdict.NO_MATCH)
        if (support != Verdict.MATCH) return AttachmentObservation(Verdict.UNKNOWN)
        if (state.logicalZone(hostId)?.zoneType != hostZone) return AttachmentObservation(Verdict.NO_MATCH)
        val projected = state.projectedState
        val sourceController = projected.getController(source.entityId) ?: return AttachmentObservation(Verdict.UNKNOWN)
        val hostController = projected.getController(hostId) ?: return AttachmentObservation(Verdict.UNKNOWN)
        val legal = EnchantRestriction.couldAttach(state, projected, evaluator, registry, source.entityId,
            sourceCard, hostId, sourceController)
        return AttachmentObservation(if (legal) Verdict.MATCH else Verdict.NO_MATCH,
            source, host, sourceController, hostController)
    }
}

internal data class AttachmentObservation(
    val verdict: Verdict,
    val source: ObjectRef? = null, val host: ObjectRef? = null,
    val sourceController: EntityId? = null, val hostController: EntityId? = null,
    val checkpoint: AttachmentCheckpoint = AttachmentCheckpoint.CURRENT_ATTACHMENT
)

internal fun attachmentRestrictionSupport(restriction: TargetRequirement?): Verdict {
    val target = restriction as? TargetObject ?: return Verdict.UNKNOWN
    return if (target == TargetObject(filter = TargetFilter.Creature) ||
        target == TargetObject(filter = TargetFilter.Creature.youControl()) ||
        target == TargetObject(filter = TargetFilter.Permanent) ||
        target == TargetObject(filter = TargetFilter.Land)) Verdict.MATCH else Verdict.UNKNOWN
}

/** Interpret existing declarations once, retaining links to the native walker's event operations. */
internal fun auraAttachment(
    root: ActionEvidence, script: CardScript, outcomes: List<EventOutcome>
): AuraAttachment {
    val support = attachmentRestrictionSupport(script.auraTarget)
    val prefix = root.id.removeSuffix("|script")
    val dependencies = buildList {
        script.staticAbilities.forEachIndexed { index, ability ->
            val path = "script.staticAbilities[$index]"
            val stats = ability as? ModifyStats
            val attached = stats?.filter?.scope == Scope.AttachedTo
            val supported = attached && stats?.filter == GroupFilter.attachedCreature() && support == Verdict.MATCH
            add(AttachmentDependency(path, root.id,
                if (attached) AttachmentOperationKind.CONTINUOUS_STATS else AttachmentOperationKind.UNRESOLVED,
                AttachmentCheckpoint.CONTINUOUS_EVALUATION,
                if (supported) Verdict.MATCH else Verdict.UNKNOWN,
                if (supported) "Continuous modification of the current attached object; active source and legal host required"
                else "Native static declaration retained; its dependency or operation is outside bounded attachment interpretation",
                nativeJson.encodeToJsonElement(StaticAbility.serializer(), ability), stats = stats.takeIf { attached }))
        }
        script.triggeredAbilities.forEachIndexed { index, ability ->
            if (ability.binding != TriggerBinding.ATTACHED) return@forEachIndexed
            val path = "script.triggeredAbilities[$index]"
            val action = "$prefix|$path"
            val event = ability.trigger as? EventPattern.StepEvent
            val linked = outcomes.filter { it.actionId == action }
            val recipient = linked.singleOrNull()?.playerBinding?.target
            val supported = support == Verdict.MATCH && event?.player == Player.You &&
                ability.activeZones == setOf(Zone.BATTLEFIELD) && ability.interveningIf == null &&
                ability.triggerRestriction == null && ability.elseEffect == null &&
                !ability.controlledByTriggeringEntityController &&
                linked.size == 1 && linked.single().path == "$path.effect" &&
                recipient == EffectTarget.PlayerRef(Player.TriggeringPlayer) &&
                linked.single().positiveQuantity == Verdict.MATCH &&
                linked.single().kind in setOf(OutcomeKind.LOSE_LIFE, OutcomeKind.GAIN_LIFE, OutcomeKind.DEAL_PLAYER_DAMAGE)
            add(AttachmentDependency(path, action, AttachmentOperationKind.TRIGGERED_OPERATION,
                AttachmentCheckpoint.TRIGGER_DETECTION,
                if (supported) Verdict.MATCH else Verdict.UNKNOWN,
                if (supported) "At trigger detection, the attached host's controller determines the step. TriggeringPlayer is retained for resolution; the Aura controls its ability."
                else "Attachment-bound trigger retained; event, branch or recipient remains unresolved",
                nativeJson.encodeToJsonElement(TriggeredAbility.serializer(), ability),
                eventOutcomeIds = linked.map { it.id }, timingPlayer = event?.player, recipient = recipient))
        }
    }
    return AuraAttachment(root.id, restriction = script.auraTarget,
        hostZone = (script.auraTarget as? TargetObject)?.filter?.zone, support = support, dependencies = dependencies)
}
