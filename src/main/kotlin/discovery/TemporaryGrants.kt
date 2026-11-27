package dev.mtg.assistant.discovery

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.event.AbilityRegistry
import com.wingedsheep.engine.event.TriggerAbilityResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ownActivatedAbilities
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/** An authored effect. Only an observed resolution can bind this to a runtime grant store entry. */
@Serializable data class TemporaryGrant(
    val id: String, val actionId: String, val card: String, val face: String, val actionPath: String, val path: String,
    val effect: Effect, val target: EffectTarget, val requirement: TargetObject?, val duration: Duration,
    val mode: Int?, val support: Verdict, val prerequisites: List<String>, val unresolved: List<String>,
    val body: GrantedBody, val eventOutcomes: List<EventOutcome>,
    val creationCheckpoint: String = "NATIVE_RESOLUTION",
    val expiryCheckpoint: String? = if (duration == Duration.EndOfTurn) "NATIVE_TURN_CLEANUP" else null,
    val sourceFidelity: Verdict = Verdict.UNKNOWN
)

internal data class TemporaryRoute(val mode: Int?, val orderedIndices: List<Int>)

private val ordinaryGrantAction = Regex("script\\.(activatedAbilities|triggeredAbilities)\\[(\\d+)]")
private val modePath = Regex("^\\.modes\\[(\\d+)]\\.effect")
private val orderedPath = Regex("\\.effects\\[(\\d+)]")

internal fun temporaryRoute(action: String, path: String): TemporaryRoute? {
    val root = when {
        action == "script.spell" -> "script.spellEffect"
        ordinaryGrantAction.matches(action) -> "$action.effect"
        else -> return null
    }
    if (!path.startsWith(root)) return null
    var tail = path.removePrefix(root)
    val mode = modePath.find(tail)
    if (mode != null) tail = tail.removePrefix(mode.value)
    val ordered = orderedPath.findAll(tail).toList()
    if (ordered.joinToString("") { it.value } != tail) return null
    return TemporaryRoute(mode?.groupValues?.get(1)?.toIntOrNull(), ordered.map { it.groupValues[1].toIntOrNull() ?: return null })
}

internal fun temporaryTargetSlot(target: EffectTarget, requirements: List<TargetRequirement>): Int? {
    val required = requirements.singleOrNull() as? TargetObject ?: return null
    if (required.count != 1 || required.minCount != 1 || required.optional || required.unlimited ||
        required.dynamicMinCount != null || required.dynamicMaxCount != null || required.ownerDistribution != null ||
        required.filter.zone != Zone.BATTLEFIELD || required.filter.alternatives.isNotEmpty()) return null
    return when (target) {
        is EffectTarget.ContextTarget -> 0.takeIf { target.index == 0 }
        is EffectTarget.BoundVariable -> 0.takeIf { target.name == required.id }
        else -> null
    }
}

@Serializable data class TemporaryGrantInstance internal constructor(
    val declarationId: String, val resolvingObject: ObjectRef, val origin: ObjectRef, val originController: EntityId,
    val recipient: ObjectRef, val turn: Int
)

@Serializable data class TemporaryGrantObservation(
    val verdict: Verdict, val reason: String, val instance: TemporaryGrantInstance? = null,
    val recipientController: EntityId? = null, val stackObject: ObjectRef? = null,
    val nativeEvents: List<GameEvent> = emptyList(), val storeMultiplicity: List<Int> = emptyList()
)

private val TemporaryGrant.abilityId: AbilityId? get() = when (val grant = effect) {
    is GrantActivatedAbilityEffect -> grant.ability.id
    is GrantTriggeredAbilityEffect -> grant.ability.id
    else -> null
}

private fun TemporaryGrant.storeCount(state: GameState, recipient: EntityId, origin: EntityId, controller: EntityId): Int =
    when (val grant = effect) {
        is GrantActivatedAbilityEffect -> state.grantedActivatedAbilities.count {
            it.entityId == recipient && it.ability == grant.ability && it.duration == duration &&
                it.sourceId == origin && it.controllerId == controller && it.sourceObject == null
        }
        is GrantTriggeredAbilityEffect -> state.grantedTriggeredAbilities.count {
            it.entityId == recipient && it.ability == grant.ability && it.duration == duration &&
                it.sourceId == origin && it.timestamp == null
        }
        else -> 0
    }

private fun TemporaryGrant.matchesInstance(instance: TemporaryGrantInstance): Boolean =
    support == Verdict.MATCH && duration == Duration.EndOfTurn && instance.declarationId == id

private fun TemporaryGrant.matchesEffect(): Boolean = when (val declaration = effect) {
    is GrantActivatedAbilityEffect -> target == declaration.target && duration == declaration.duration &&
        declaration.ability.activateFromZone == Zone.BATTLEFIELD
    is GrantTriggeredAbilityEffect -> target == declaration.target && duration == declaration.duration &&
        declaration.ability.activeZones == setOf(Zone.BATTLEFIELD)
    else -> false
}

private data class GrantResolution(
    val source: ObjectRef, val controller: EntityId, val effect: Effect, val targets: List<TargetRequirement>,
    val modes: List<Int>, val modeTargets: List<List<ChosenTarget>>, val finished: Boolean
)

/** Bind only real, paired states from one native resolution transaction. Stack provenance and
 * a multiset increase can identify one creation. The store alone provides no instruction ID. */
fun TemporaryGrant.observeCreation(
    before: GameState, after: GameState, events: List<GameEvent>, resolving: ObjectRef, services: EngineServices
): TemporaryGrantObservation {
    fun result(v: Verdict, why: String) = TemporaryGrantObservation(v,why)
    val route = temporaryRoute(actionPath,path)
    if (support != Verdict.MATCH || face != "front" || duration != Duration.EndOfTurn ||
        route == null || route.mode != mode || !matchesEffect())
        return result(Verdict.UNKNOWN,"Unresolved creation path, face, recipient or duration")
    if (before.stack.lastOrNull() != resolving.entityId || !before.isCurrentObject(resolving) ||
        resolving.entityId in after.stack || before.turnNumber != after.turnNumber)
        return result(Verdict.UNKNOWN,"No single finished resolution of the supplied stack generation")
    val objectOnStack = before.getEntity(resolving.entityId) ?: return result(Verdict.UNKNOWN,"Stack object absent")
    val stampedTargets = objectOnStack.get<TargetsComponent>()
    val spell = objectOnStack.get<SpellOnStackComponent>()
    val activated = objectOnStack.get<ActivatedAbilityOnStackComponent>()
    val triggered = objectOnStack.get<TriggeredAbilityOnStackComponent>()
    val source = if (spell != null) resolving else activated?.objectReferences?.origin ?: triggered?.objectReferences?.origin
        ?: return result(Verdict.UNKNOWN,"No captured native source generation")
    val sourceCard = before.getEntity(source.entityId)?.get<CardComponent>()
        ?: return result(Verdict.UNKNOWN,"Missing native source card component")
    val definition = services.cardRegistry.getCard(sourceCard.cardDefinitionId)
    if (!before.isCurrentObject(source) || definition?.name != card)
        return result(Verdict.UNKNOWN,"Native definition or source generation unresolved")
    val script = definition.script
    val action = ordinaryGrantAction.matchEntire(actionPath)
    val index = action?.groupValues?.get(2)?.toIntOrNull()
    fun abilityFinished(controller: EntityId) = events.filterIsInstance<AbilityResolvedEvent>().count {
        it.abilityEntityId == resolving.entityId && it.sourceId == source.entityId && it.controllerId == controller
    } == 1
    val resolution = when {
        spell != null && actionPath == "script.spell" && spell.splicedCardNames.isEmpty() && !spell.wasOverloaded &&
            !spell.wasCleaved && !spell.castFaceDown -> {
            val native = script.spellEffect ?: return result(Verdict.UNKNOWN,"Missing native spell instruction")
            GrantResolution(source,spell.casterId,native,script.targetRequirements,spell.chosenModes,spell.modeTargetsOrdered,
                events.filterIsInstance<ResolvedEvent>().count { it.entityId == resolving.entityId } == 1)
        }
        activated != null && action?.groupValues?.get(1) == "activatedAbilities" && index != null -> {
            val native = script.activatedAbilities.getOrNull(index)
            if (native == null || activated.activatedAbility != native || activated.effect != native.effect ||
                activated.abilityIdentity != AbilityIdentity(sourceCard.cardDefinitionId,native.id) ||
                script.activatedAbilities.count { it.id == native.id } != 1)
                return result(Verdict.UNKNOWN,"Activated origin declaration is ambiguous or changed")
            GrantResolution(source,activated.controllerId,native.effect,native.targetRequirements,activated.chosenModes,
                activated.modeTargetsOrdered,abilityFinished(activated.controllerId))
        }
        triggered != null && action?.groupValues?.get(1) == "triggeredAbilities" && index != null -> {
            val native = script.triggeredAbilities.getOrNull(index)
            if (native == null || triggered.resolvingTriggeredAbility != native || triggered.effect != native.effect ||
                triggered.copyIndex != null || triggered.triggerContext == null ||
                script.triggeredAbilities.count { it.id == native.id } != 1 || triggered.granterId != null)
                return result(Verdict.UNKNOWN,"Triggered origin declaration is ambiguous or changed")
            GrantResolution(source,triggered.controllerId,native.effect,listOfNotNull(native.targetRequirement)+native.additionalTargetRequirements,
                triggered.chosenModes,triggered.modeTargetsOrdered,abilityFinished(triggered.controllerId))
        }
        else -> return result(Verdict.UNKNOWN,"Origin is outside supported ordinary spells and abilities")
    }
    val modal = resolution.effect as? ModalEffect
    val selected = route.mode?.let { modal?.modes?.getOrNull(it) }
    var node = selected?.effect ?: resolution.effect
    for (slot in route.orderedIndices) {
        node = (node as? CompositeEffect)?.effects?.getOrNull(slot)
            ?: return result(Verdict.UNKNOWN,"Ordered native path does not bind")
    }
    if (node != effect || id != "$actionId|$path|TEMPORARY_GRANT" || actionId != "$card|$face|$actionPath")
        return result(Verdict.UNKNOWN,"Exact native instruction differs")
    val modeIndex = route.mode?.let { requested ->
        if (requested !in resolution.modes && resolution.modes.isNotEmpty())
            return result(Verdict.NO_MATCH,"The native resolution did not choose this mode")
        resolution.modes.indices.singleOrNull { resolution.modes[it] == requested }
            ?: return result(Verdict.UNKNOWN,"Mode choice is not uniquely captured")
    }
    val requirements = selected?.targetRequirements ?: resolution.targets
    val chosen = if (modeIndex == null) stampedTargets?.targets else resolution.modeTargets.getOrNull(modeIndex)
    val recipient = if (target == EffectTarget.Self) resolution.source else {
        val slot = temporaryTargetSlot(target,requirements) ?: return result(Verdict.UNKNOWN,"Target slot is unresolved")
        if (requirements.getOrNull(slot) != requirement) return result(Verdict.UNKNOWN,"Native target requirement differs")
        val selectedTarget = chosen?.singleOrNull() as? ChosenTarget.Permanent
            ?: return result(Verdict.UNKNOWN,"No uniquely captured permanent target")
        stampedTargets?.targetObjectRefs?.get(selectedTarget.entityId)
            ?: return result(Verdict.UNKNOWN,"Missing target generation")
    }
    if (events.filterIsInstance<SpellFizzledEvent>().any { it.spellEntityId == resolving.entityId } ||
        !before.isCurrentObject(recipient))
        return result(Verdict.NO_MATCH,"Native fizzle or the selected object generation is already gone")
    if (!resolution.finished || !after.isCurrentObject(recipient) || after.logicalZone(recipient.entityId)?.zoneType != Zone.BATTLEFIELD)
        return result(Verdict.UNKNOWN,"No finished native resolution with the same battlefield recipient")
    val roots = if (modal == null) listOf(resolution.effect)
        else resolution.modes.mapNotNull { modal.modes.getOrNull(it)?.effect }
    val nativeId = abilityId?.let { nativeJson.encodeToJsonElement(AbilityId.serializer(),it) }
    if (nativeId == null || roots.sumOf { it.treeForTemporaryGrant().countGrantAbility(nativeId) } != 1)
        return result(Verdict.UNKNOWN,"The resolution has multiple or unresolved matching grant instructions")
    val previous = storeCount(before,recipient.entityId,source.entityId,resolution.controller)
    val current = storeCount(after,recipient.entityId,source.entityId,resolution.controller)
    val multiplicity = listOf(previous,current)
    if (previous != 0 || current != 1)
        return TemporaryGrantObservation(Verdict.UNKNOWN,"A unique new grant store entry is not established",storeMultiplicity=multiplicity)
    val instance = TemporaryGrantInstance(id,resolving,source,resolution.controller,recipient,before.turnNumber)
    return TemporaryGrantObservation(Verdict.MATCH,"Finished native resolution added one matching runtime grant",instance,
        after.projectedState.getController(recipient.entityId),nativeEvents=events.filter {
            it is ResolvedEvent && it.entityId == resolving.entityId ||
                it is AbilityResolvedEvent && it.abilityEntityId == resolving.entityId
        },storeMultiplicity=multiplicity)
}

private fun Effect.treeForTemporaryGrant() = nativeJson.encodeToJsonElement(Effect.serializer(),this)

private fun JsonElement.countGrantAbility(id: JsonElement): Int = when (this) {
    is JsonObject -> {
        val type = (get("type") as? JsonPrimitive)?.content
        val ability = get("ability") as? JsonObject
        val sameId = ability?.get("id") == id
        (if (type in setOf("GrantActivatedAbility","GrantTriggeredAbility","GrantActivatedAbilityToGroup") && sameId) 1 else 0) +
            values.sumOf { it.countGrantAbility(id) }
    }
    is JsonArray -> sumOf { it.countGrantAbility(id) }
    else -> 0
}

fun TemporaryGrant.observeAvailability(state: GameState, instance: TemporaryGrantInstance): TemporaryGrantObservation {
    if (!matchesInstance(instance)) return TemporaryGrantObservation(Verdict.UNKNOWN,"Creation declaration is unresolved")
    val recipient = instance.recipient
    if (!state.isCurrentObject(recipient) || state.logicalZone(recipient.entityId)?.zoneType != Zone.BATTLEFIELD)
        return TemporaryGrantObservation(Verdict.NO_MATCH,"The receiving battlefield generation is gone")
    val count = storeCount(state,recipient.entityId,instance.origin.entityId,instance.originController)
    if (count == 0) return TemporaryGrantObservation(Verdict.NO_MATCH,"Native runtime grant is absent")
    val controller = state.projectedState.getController(recipient.entityId)
    if (count != 1 || state.turnNumber != instance.turn || controller == null || state.projectedState.hasLostAllAbilities(recipient.entityId))
        return TemporaryGrantObservation(Verdict.UNKNOWN,"Runtime grant identity, turn, controller or projected ability is unresolved")
    return TemporaryGrantObservation(Verdict.MATCH,"Native runtime grant is present; legal use remains a separate observation",instance,controller)
}

fun TemporaryGrant.observeExpiry(before: GameState, after: GameState, events: List<GameEvent>,
    instance: TemporaryGrantInstance): TemporaryGrantObservation {
    val recipient = instance.recipient
    val cleanup = events.filterIsInstance<StepChangedEvent>().any { it.newStep == Step.CLEANUP } || before.step == Step.CLEANUP
    if (!cleanup || after.turnNumber != before.turnNumber+1 || !after.isCurrentObject(recipient) ||
        observeAvailability(before,instance).verdict != Verdict.MATCH ||
        observeAvailability(after,instance).verdict != Verdict.NO_MATCH)
        return TemporaryGrantObservation(Verdict.UNKNOWN,"No bounded native cleanup removal of this receiving generation")
    return TemporaryGrantObservation(Verdict.MATCH,"The unique runtime record ended across native cleanup",instance,
        nativeEvents=events.filterIsInstance<StepChangedEvent>(),storeMultiplicity=listOf(1,0))
}

fun TemporaryGrant.observeActivation(before: GameState, after: GameState, events: List<GameEvent>,
    instance: TemporaryGrantInstance, action: ActivateAbility, services: EngineServices): TemporaryGrantObservation {
    fun unknown(reason: String) = TemporaryGrantObservation(Verdict.UNKNOWN,reason)
    val ability = (effect as? GrantActivatedAbilityEffect)?.ability ?: return unknown("Activated grant required")
    val current = observeAvailability(before,instance)
    val recipient = instance.recipient.entityId
    if (current.instance != instance || action.sourceId != recipient || action.abilityId != ability.id ||
        current.recipientController != action.playerId) return unknown("Receiving source, ability or controller not established")
    val host = before.getEntity(recipient) ?: return unknown("Missing native recipient")
    val card = host.get<CardComponent>() ?: return unknown("Missing native recipient definition")
    val definition = services.cardRegistry.getCard(card.cardDefinitionId)
    if (before.grantedActivatedAbilities.count { it.entityId == recipient && it.ability.id == ability.id } != 1 ||
        ownActivatedAbilities(card,definition,host.get<ClassLevelComponent>()?.currentLevel).any { it.id == ability.id } ||
        services.castPermissionUtils.getStaticGrantedAbilitiesWithGranter(recipient,before).any { it.ability.id == ability.id } ||
        services.castPermissionUtils.getEmblemGrantedActivatedAbilities(recipient,before).any { it.id == ability.id })
        return unknown("Native activation cannot distinguish all same-ID suppliers")
    val event = events.filterIsInstance<AbilityActivatedEvent>().singleOrNull {
        it.sourceId == recipient && it.controllerId == action.playerId && it.isManaAbility == ability.isManaAbility
    } ?: return unknown("Missing unique native activation event")
    if (ability.isManaAbility) {
        val mana = events.filterIsInstance<ManaAbilityResolvedEvent>().singleOrNull {
            it.sourceId == recipient && it.playerId == action.playerId
        }
        if (event.abilityEntityId != null || mana == null)
            return unknown("Immediate native mana resolution not established")
        return TemporaryGrantObservation(Verdict.MATCH,"Native mana activation resolved",instance,action.playerId,nativeEvents=listOf(event,mana))
    }
    val stack = event.abilityEntityId?.let(after::objectRef) ?: return unknown("No native activation stack generation")
    val snapshot = after.getEntity(stack.entityId)?.get<ActivatedAbilityOnStackComponent>()
    if (before.isCurrentObject(stack) || stack.entityId !in after.stack || snapshot?.activatedAbility != ability ||
        snapshot.granterId != null || snapshot.objectReferences.origin != instance.recipient ||
        snapshot.sourceId != recipient || snapshot.controllerId != action.playerId) return unknown("Native activation snapshot differs")
    return TemporaryGrantObservation(Verdict.MATCH,"Independent native activation on the stack",instance,action.playerId,stack,listOf(event))
}

fun TemporaryGrant.observeTrigger(before: GameState, after: GameState, events: List<GameEvent>,
    instance: TemporaryGrantInstance, stack: ObjectRef, services: EngineServices): TemporaryGrantObservation {
    fun unknown(reason: String) = TemporaryGrantObservation(Verdict.UNKNOWN,reason)
    val ability = (effect as? GrantTriggeredAbilityEffect)?.ability ?: return unknown("Triggered grant required")
    val pattern = ability.trigger as? EventPattern.DealsDamageEvent ?: return unknown("Event is outside bounded temporary trigger attribution")
    if (pattern.damageType != DamageType.Combat || pattern.recipient != Recipient.AnyPlayer ||
        pattern.batch || pattern.sourceFilter != null || pattern.requires.isNotEmpty() || pattern.requireExcess)
        return unknown("Only single self combat damage to a player is attributed")
    val available = observeAvailability(before,instance)
    val recipient = instance.recipient.entityId
    if (available.instance != instance || ability.binding != TriggerBinding.SELF ||
        ability.controlledByTriggeringEntityController || before.isCurrentObject(stack) ||
        !after.isCurrentObject(stack) || !after.isCurrentObject(instance.recipient) || stack.entityId !in after.stack)
        return unknown("Event-time grant or new stack generation unresolved")
    val card = before.getEntity(recipient)?.get<CardComponent>() ?: return unknown("Missing recipient definition")
    val resolver = TriggerAbilityResolver(services.cardRegistry,AbilityRegistry(),services.predicateEvaluator)
    val native = resolver.getTriggeredAbilities(recipient,card.cardDefinitionId,before)
    if (native.filter { it.id == ability.id } != listOf(ability) ||
        before.grantedTriggeredAbilities.count { it.entityId == recipient && it.ability.id == ability.id } != 1)
        return unknown("Native trigger lookup cannot distinguish all same-ID suppliers")
    val snapshot = after.getEntity(stack.entityId)?.get<TriggeredAbilityOnStackComponent>()
    val context = snapshot?.triggerContext ?: return unknown("No native trigger event snapshot")
    val controller = available.recipientController
    val placement = events.filterIsInstance<AbilityTriggeredEvent>().singleOrNull {
        it.abilityEntityId == stack.entityId && it.sourceId == recipient && it.controllerId == controller
    }
    val cause = events.filterIsInstance<DamageDealtEvent>().singleOrNull {
        it.sourceId == recipient && context.damageSourceId == recipient && it.targetId == context.damageRecipientId &&
            it.amount == context.damageAmount && it.isCombatDamage && it.targetId in before.turnOrder &&
            (it.sourceLastKnown?.objectRef ?: instance.recipient) == instance.recipient
    }
    if (snapshot.sourceId != recipient || snapshot.controllerId != controller || snapshot.granterId != null ||
        snapshot.objectReferences.origin != instance.recipient || snapshot.resolvingTriggeredAbility != ability ||
        placement == null || cause == null) return unknown("Native source, controller, stack or damage association differs")
    return TemporaryGrantObservation(Verdict.MATCH,"Unique native runtime trigger and matching damage event",
        instance,controller,stack,listOf(cause,placement))
}

/** An independently established stack obligation can outlive the runtime grant and its recipient. */
fun TemporaryGrant.observePersistence(state: GameState, established: TemporaryGrantObservation): TemporaryGrantObservation {
    val instance = established.instance
    val ref = established.stackObject
    if (established.verdict != Verdict.MATCH || instance == null || !matchesInstance(instance) || ref == null)
        return TemporaryGrantObservation(Verdict.UNKNOWN,"No attributed native stack creation")
    if (!state.isCurrentObject(ref) || ref.entityId !in state.stack)
        return TemporaryGrantObservation(Verdict.NO_MATCH,"The established object has left the stack")
    val stack = state.getEntity(ref.entityId)
    val matches = when (val grant = effect) {
        is GrantActivatedAbilityEffect -> stack?.get<ActivatedAbilityOnStackComponent>()?.let {
            it.activatedAbility == grant.ability && it.objectReferences.origin == instance.recipient &&
                it.controllerId == established.recipientController
        }
        is GrantTriggeredAbilityEffect -> stack?.get<TriggeredAbilityOnStackComponent>()?.let {
            it.resolvingTriggeredAbility == grant.ability && it.objectReferences.origin == instance.recipient &&
                it.controllerId == established.recipientController
        }
        else -> null
    }
    return if (matches == true) established.copy(reason="Previously established native stack object persists")
    else TemporaryGrantObservation(Verdict.UNKNOWN,"Native stack snapshot does not match the prior observation")
}
