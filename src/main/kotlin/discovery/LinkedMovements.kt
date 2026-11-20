package dev.mtg.assistant.discovery

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.targets.*
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.Serializable
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.json.JsonElement

@Serializable enum class MovementLinkKind { IMMEDIATE_ORDERED, DEFERRED_SOURCE_LINKED, UNRESOLVED_POSSIBLE, DURATION_BOUND }
/** Implicit native return semantics, not another declared action or stacked trigger. */
@Serializable data class DurationBoundReturn(val anchor: EffectTarget, val returnTo: Zone?,
    val controller: String = "OWNER", val mechanism: String = "ZoneReturnService",
    val checkpoint: String = "SOURCE_OBJECT_DEPARTURE_BEFORE_NEXT_INSTRUCTION_OR_PRIORITY")
@Serializable data class MovementStep(
    val actionId: String, val path: String, val factIds: List<String>,
    val from: Zone?, val to: Zone?, val controller: String, val native: JsonElement
)
/** A declaration of conditional potential. Runtime identity is supplied by native ObjectRef and
 * primary zone-transition evidence, never by a name or an unversioned entity ID alone. */
@Serializable data class LinkedMovement(
    val id: String, val card: String, val face: String, val kind: MovementLinkKind,
    val departure: MovementStep, val continuation: MovementStep?, val binding: String,
    val support: Verdict, val prerequisites: List<String>, val unresolved: List<String>,
    val checkpoint: String,
    val sourceInstance: String? = null,
    val originalSubject: String = "Same original nontoken card; tokens and copies are not original-card returns",
    val resultIdentity: String = "Each successful zone change creates a new ObjectRef generation; former attachments, counters, damage, targets, modifiers and source obligations do not persist by identity",
    val evidenceScope: String = "Native declaration relationship; neither execution success nor whole-card source fidelity",
    val sourceFidelity: Verdict = Verdict.UNKNOWN,
    val engineRevision: String = ARGENTUM_SHA,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val durationReturn: DurationBoundReturn? = null
)

internal data class TargetMovement(val scopes: List<ObjectScope>, val step: MovementStep, val departureSupported: Boolean)

/** Exact root declarations only. The existing walker supplies return facts; no recursive effect
 * walk or application exile ledger. The engine's pile is source-visit scoped, not ability scoped. */
internal fun deferredMovementLinks(card: String, face: String, script: CardScript,
    facts: List<MechanicalFact>, producerCount: Int): List<LinkedMovement> {
    val returns = script.triggeredAbilities.withIndex().mapNotNull { (i, ability) ->
        val sequence = ability.effect as? CompositeEffect ?: return@mapNotNull null
        val gather = sequence.effects.getOrNull(0) as? GatherCardsEffect ?: return@mapNotNull null
        val move = sequence.effects.getOrNull(1) as? MoveCollectionEffect ?: return@mapNotNull null
        val event = ability.trigger as? EventPattern.ZoneChangeEvent
        if (sequence.effects.size != 2 || gather.source != CardSource.FromLinkedExile() || move.from != gather.storeAs ||
            event?.from != Zone.BATTLEFIELD || event.to != null || ability.binding != TriggerBinding.SELF) return@mapNotNull null
        Triple(i, ability, move)
    }
    val departures = script.triggeredAbilities.withIndex().filter {
        val effect=it.value.effect
        effect is ExileUntilLeavesEffect || returns.isNotEmpty() && effect is MoveToZoneEffect &&
            effect.destination==Zone.EXILE && effect.linkToSource
    }
    return departures.map { (i, ability) ->
        val effect = ability.effect
        val reference = when(effect) { is ExileUntilLeavesEffect -> effect.target; is MoveToZoneEffect -> effect.target; else -> error("Unselected linked exile") }
        val ordinary = effect is MoveToZoneEffect
        val event = ability.trigger as? EventPattern.ZoneChangeEvent
        val action = "$card|$face|script.triggeredAbilities[$i]"
        val path = "script.triggeredAbilities[$i].effect"
        val departureFacts=facts.filter { it.actionId==action && it.path==path && it.kind==FactKind.MOVE_OBJECT }
        val ret = returns.singleOrNull()
        val returnAction = ret?.let { "$card|$face|script.triggeredAbilities[${it.first}]" }
        val returnFact = facts.singleOrNull { it.actionId == returnAction && it.path == "script.triggeredAbilities[${ret?.first}].effect.effects[1]" && it.kind == FactKind.MOVE_OBJECT }
        val move = ret?.third
        val destination = move?.destination as? CardDestination.ToZone
        val target=ability.allTargetRequirements.singleOrNull() as? TargetObject
        val bound=when(val t=reference) { is EffectTarget.BoundVariable -> target?.id==t.name; is EffectTarget.ContextTarget -> t.index==0 && target!=null; else -> false }
        val problems = listOfNotNull(
            if (event?.to != Zone.BATTLEFIELD || ability.binding != TriggerBinding.SELF) "Exile is not a root self-entry trigger" else null,
            if (departures.size != 1 || returns.size != 1 || producerCount != 1) "Native source pile has absent or multiple producers/consumers; ability-specific association not established" else null,
            if (script.triggeredAbilities.size!=2 || script.activatedAbilities.isNotEmpty() || script.spellEffect!=null ||
                script.staticAbilities.isNotEmpty() || script.replacementEffects.isNotEmpty())
                "Additional source declarations exceed the isolated two-trigger linked-pile subset" else null,
            if (returnFact == null || destination?.zone != Zone.BATTLEFIELD || move?.underOwnersControl != true || move.filter != null) "Exact linked-exile owner-return pipeline not established" else null,
            if (!bound || target?.count!=1 || target.filter.alternatives.isNotEmpty()) "Exiled target does not have one resolved native target requirement" else null,
            if (ability.activeZones!=setOf(Zone.BATTLEFIELD) || ret?.second?.activeZones!=setOf(Zone.BATTLEFIELD) ||
                ability.controlledByTriggeringEntityController || ret.second.controlledByTriggeringEntityController) "Source zone or ability controller context unresolved" else null,
            if (ability.triggerCondition != null || ret?.second?.triggerCondition != null) "Additional trigger condition requires separate evidence" else null)
        LinkedMovement("$action|DEFERRED_LINK", card, face,
            if (problems.isEmpty()) MovementLinkKind.DEFERRED_SOURCE_LINKED else MovementLinkKind.UNRESOLVED_POSSIBLE,
            MovementStep(action, path, departureFacts.map { it.id }, target?.filter?.zone, Zone.EXILE, "ANY", nativeJson.encodeToJsonElement(Effect.serializer(),effect)),
            returnFact?.let { MovementStep(it.actionId,it.path,listOf(it.id),Zone.EXILE,it.destinationZone,"OWNER",it.native) },
            if(ordinary) "MoveToZone(linkToSource) -> resolving source visit -> FromLinkedExile -> MoveCollection"
                else "ExileUntilLeaves -> source LinkedExileComponent -> FromLinkedExile -> MoveCollection",
            if (problems.isEmpty()) Verdict.MATCH else Verdict.UNKNOWN,
            listOf(if(ordinary) "Successful native exile links to the original resolving source visit even after departure; a newer source visit cannot inherit it"
                else "The same source battlefield visit must still be present when exile resolves; native establishment uses a source-presence guard, not a proven generation check",
                "Separate self-departure trigger gathers only cards still in this source visit's linked exile at its resolution",
                "Return resolving before establishment consumes no future link; no guaranteed return",
                "Initial source zone/target restrictions are retained on the originating native action",
                if(ordinary) "Departure and return reference the existing movement facts; both triggers retain independent resolution checkpoints"
                else "ExileUntilLeaves has no legacy MOVE_OBJECT fact; return references the existing collection movement fact"),
            problems, "Independent entry and departure trigger resolutions; not an automatic duration return",
            "Exact native battlefield visit: source ObjectRef plus sourceBattlefieldTimestamp; departedLinkedExile lookup; no sharing with another source or later visit")
    }
}

/** Only the corrected root, singular battlefield-target duration shape. No collection or delayed
 * traversal: native ZoneReturn entries remain responsible for establishment, identity and return. */
internal fun durationMovementLinks(card: String, face: String, script: CardScript): List<LinkedMovement> =
    script.triggeredAbilities.withIndex().mapNotNull { (i,ability) ->
        val effect=ability.effect as? MoveUntilSourceLeavesEffect ?: return@mapNotNull null
        val target=ability.allTargetRequirements.singleOrNull() as? TargetObject
        val bound=when(val t=effect.target) { is EffectTarget.BoundVariable -> target?.id==t.name; is EffectTarget.ContextTarget -> t.index==0 && target!=null; else -> false }
        val event=ability.trigger as? EventPattern.ZoneChangeEvent
        val problems=listOfNotNull(
            if(effect.anchor!=EffectTarget.Self || effect.destination!=Zone.EXILE) "Duration anchor or destination outside the corrected source-exile subset" else null,
            if(!bound || target?.count!=1 || target.filter.zone!=Zone.BATTLEFIELD || target.filter.alternatives.isNotEmpty()) "Duration requires one bound battlefield target; other zones and collections remain unresolved" else null,
            if(event?.to!=Zone.BATTLEFIELD || ability.binding!=TriggerBinding.SELF || ability.activeZones!=setOf(Zone.BATTLEFIELD) || ability.triggerCondition!=null || ability.controlledByTriggeringEntityController)
                "Duration establishment trigger or source context unresolved" else null,
            if(script.triggeredAbilities.size!=1 || script.activatedAbilities.isNotEmpty() || script.staticAbilities.isNotEmpty() || script.replacementEffects.isNotEmpty() || script.spellEffect!=null)
                "Additional source declarations exceed the isolated root-duration subset" else null)
        val action="$card|$face|script.triggeredAbilities[$i]"
        LinkedMovement("$action|DURATION_LINK",card,face,
            if(problems.isEmpty()) MovementLinkKind.DURATION_BOUND else MovementLinkKind.UNRESOLVED_POSSIBLE,
            MovementStep(action,"script.triggeredAbilities[$i].effect",emptyList(),target?.filter?.zone,Zone.EXILE,"ANY",nativeJson.encodeToJsonElement(Effect.serializer(),effect)),
            null,"MoveUntilSourceLeaves -> exact source ObjectRef and moved ObjectRef -> native ZoneReturn",
            if(problems.isEmpty()) Verdict.MATCH else Verdict.UNKNOWN,
            listOf("Legal selected original nontoken card and successful primary exile are prerequisites",
                "The same source battlefield visit must still exist at establishment; a returned source is a different object",
                "Only the recorded exile object may return; departure from exile invalidates that duration entry",
                "Native return occurs when the source visit ends, before the next instruction or priority; no independent return trigger"),
            problems,"Establish at entry-trigger resolution; end at exact source-object departure",
            sourceInstance="Native ZoneReturn.source ObjectRef; no application return ledger",
            durationReturn=DurationBoundReturn(effect.anchor,target?.filter?.zone))
    }

internal fun immediateMovementLink(card: String, face: String, binding: String,
    before: MovementStep, after: MovementStep, singleBoundTarget: Boolean,
    guarded: Boolean, prerequisites: List<String>): LinkedMovement {
    val pattern = Regex("^(.*)\\.effects\\[(\\d+)]$")
    val a = pattern.matchEntire(before.path)
    val b = pattern.matchEntire(after.path)
    val adjacent = a != null && b != null && a.groupValues[1] == b.groupValues[1] &&
        b.groupValues[2].toInt() == a.groupValues[2].toInt() + 1
    val reasons = listOfNotNull(
        if (!singleBoundTarget) "One unambiguous singular native target binding is required; collections, Self and other references are outside this immediate subset" else null,
        if (!adjacent || before.actionId != after.actionId) "Continuation is not adjacent in the same ordered native resolution" else null,
        if (guarded) "Optional, deferred, guarded or unresolved context requires additional continuation evidence" else null,
        if (before.from != Zone.BATTLEFIELD || before.to != Zone.EXILE || after.from != Zone.EXILE || after.to != Zone.BATTLEFIELD)
            "Zone sequence is outside bounded battlefield/exile/battlefield linkage" else null,
        if (after.controller !in setOf("YOU", "OWNER")) "Return controller binding unresolved" else null,
        if (before.factIds.size != 1 || after.factIds.size != 1) "Alternative target clauses do not identify one movement witness" else null)
    return LinkedMovement("${after.actionId}|${after.path}|LINK", card, face,
        if (reasons.isEmpty()) MovementLinkKind.IMMEDIATE_ORDERED else MovementLinkKind.UNRESOLVED_POSSIBLE,
        before, after, binding, if (reasons.isEmpty()) Verdict.MATCH else Verdict.UNKNOWN,
        prerequisites + listOf("Same selected original nontoken card; legal target at resolution",
            "Initial exile must actually succeed; native resolution follows only its authorized public-zone transition",
            "Card must remain the expected exile object; replacement, missing object or entry restrictions may prevent return"),
        reasons, "Ordered instructions in one resolution; no intervening priority")
}

