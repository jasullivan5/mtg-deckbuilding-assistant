package dev.mtg.assistant.discovery

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.event.*
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.state.*
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Native definition face, not an invented transformed-card model. Aggregate/split containers
 * retain declarations, but concrete binding is unknown until their exact native face is known. */
internal fun grantFace(card: CardDefinition, face: String): CardDefinition? = when(face) {
    "front" -> card
    "front.backFace" -> card.backFace
    "front.flipSide" -> card.flipSide
    else -> null
}

internal fun triggerRecipientProblems(card: CardDefinition?, grant: GrantTriggeredAbility): List<String> {
    if(card==null) return listOf("Exact native definition face unresolved")
    val kind=grantRecipientKind(card,grant.filter)
    return listOfNotNull(
        if(kind==GrantRecipientKind.UNRESOLVED) "Recipient scope beyond bounded attachment/creature groups" else null,
        if(kind==GrantRecipientKind.AURA_HOST && card.script.auraTarget !in listOf(TargetObject(filter=TargetFilter.Creature),TargetObject(filter=TargetFilter.Creature.youControl()))) "Aura enchant restriction outside bounded creature hosts" else null,
        if(kind in setOf(GrantRecipientKind.AURA_HOST,GrantRecipientKind.EQUIPMENT_HOST) && grant.filter!=GroupFilter.attachedCreature()) "Additional attached recipient restrictions" else null,
        if(kind==GrantRecipientKind.FILTER_GROUP && grant.filter.copy(excludeSelf=false) !in listOf(GroupFilter(GameObjectFilter.Creature),GroupFilter(GameObjectFilter.Creature.youControl()))) "Group filter or pairing membership unresolved" else null,
        if(grant.ability.activeZones!=setOf(Zone.BATTLEFIELD)) "Nonbattlefield/union active zones" else null)
}

internal fun grantedEventSupport(a: TriggeredAbility): Verdict = when {
    a.controlledByTriggeringEntityController -> Verdict.UNKNOWN
    a.trigger is EventPattern.DealsDamageEvent && a.binding==TriggerBinding.SELF -> Verdict.MATCH
    a.trigger is EventPattern.AttackEvent && a.binding==TriggerBinding.SELF -> Verdict.MATCH
    a.trigger is EventPattern.StepEvent && a.binding==TriggerBinding.ANY && (a.trigger as EventPattern.StepEvent).player==Player.You -> Verdict.MATCH
    else -> Verdict.UNKNOWN
}

/** Count possible declarations, including unsupported wrappers, only to reject ambiguous IDs.
 * This does not interpret wrapper availability or recursively grant abilities. */
internal fun StaticAbility.triggerDeclarations(): List<GrantTriggeredAbility> = when(this) {
    is GrantTriggeredAbility -> listOf(this)
    is ConditionalStaticAbility -> ability.triggerDeclarations()
    is GrantStaticAbility -> ability.triggerDeclarations()
    else -> emptyList()
}

@Serializable data class GrantedTrigger(
    val id: String, val actionId: String, val card: String, val face: String, val faceName: String?,
    val path: String, val staticIndex: Int, val declaration: GrantTriggeredAbility,
    val kind: GrantRecipientKind, val recipientSupport: Verdict, val eventSupport: Verdict,
    val unresolved: List<String>, val attachmentActionId: String?, val body: GrantedBody,
    val eventOutcomes: List<EventOutcome>,
    val sourceBinding: String = "RECEIVING_OBJECT",
    val controllerBinding: String = "NATIVE_CONTROLLER_CAPTURED_AT_DETECTION",
    val checkpoints: List<String> = listOf("Current grant at event; no event or recipient implied", "Native event matching and triggerRestriction at detection", "Native interveningIf at detection AND resolution", "Targets/chooser at stack announcement", "Effect gates and optional choices at resolution"),
    val persistence: String = "Native pending/stack snapshots retain source, controller, rules and event. Future grant loss does not erase an established trigger; later legality, conditions and resolution remain native.",
    val sourceFidelity: Verdict = Verdict.UNKNOWN)

@Serializable data class GrantedTriggerAvailability(val verdict: Verdict, val instance: GrantedAbilityInstance?=null, val reason: String)

/** Read the same native resolver/index used by the detector. No trigger is created here. */
fun GrantedTrigger.observeAvailability(state: GameState, granter: ObjectRef, recipient: ObjectRef,
    services: EngineServices, attachmentAction: ActionEvidence?=null): GrantedTriggerAvailability {
    fun result(v: Verdict,s: String)=GrantedTriggerAvailability(v,reason=s)
    if(!state.isCurrentObject(granter) || !state.isCurrentObject(recipient) ||
        state.logicalZone(granter.entityId)?.zoneType!=Zone.BATTLEFIELD || state.logicalZone(recipient.entityId)?.zoneType!=Zone.BATTLEFIELD)
        return result(Verdict.NO_MATCH,"Granter or recipient is not the supplied battlefield generation")
    val source=state.getEntity(granter.entityId)?.get<CardComponent>() ?: return result(Verdict.UNKNOWN,"No source card identity")
    val def=services.cardRegistry.getCard(source.cardDefinitionId) ?: return result(Verdict.UNKNOWN,"No native source definition")
    if(def.name!=faceName || def.script.staticAbilities.getOrNull(staticIndex)!=declaration)
        return result(Verdict.UNKNOWN,"Native face/declaration binding does not match")
    if(def.script.staticAbilities.flatMap { it.triggerDeclarations() }.count { it.ability.id==declaration.ability.id }!=1)
        return result(Verdict.UNKNOWN,"Equal-ID declarations on one granter lack a native declaration index")
    if(recipientSupport!=Verdict.MATCH) return result(Verdict.UNKNOWN,"Recipient declaration unresolved")
    if(state.projectedState.hasLostAllAbilities(granter.entityId) || state.projectedState.hasLostAllAbilities(recipient.entityId))
        return result(Verdict.UNKNOWN,"Ability-removal/layer interaction outside bounded observation")
    if(kind==GrantRecipientKind.AURA_HOST) {
        val root=attachmentAction ?: return result(Verdict.UNKNOWN,"Existing Aura attachment evidence required")
        if(root.id!=attachmentActionId) return result(Verdict.UNKNOWN,"Wrong attachment declaration")
        val attached=root.attachment?.observe(root,state,granter,services.cardRegistry,services.predicateEvaluator,recipient)
        if(attached?.verdict!=Verdict.MATCH) return result(attached?.verdict ?: Verdict.UNKNOWN,"Current Aura host not established")
    }
    val host=state.getEntity(recipient.entityId)?.get<CardComponent>() ?: return result(Verdict.UNKNOWN,"No recipient card identity")
    val hostDef=services.cardRegistry.getCard(host.cardDefinitionId)
    if(hostDef?.script?.triggeredAbilities.orEmpty().any { it.id==declaration.ability.id } || host.copyTriggeredAbilities.any { it.id==declaration.ability.id } ||
        state.grantedTriggeredAbilities.any { it.entityId==recipient.entityId && it.ability.id==declaration.ability.id })
        return result(Verdict.UNKNOWN,"Intrinsic/copy/runtime trigger ID collision")
    val resolver=TriggerAbilityResolver(services.cardRegistry,AbilityRegistry(),services.predicateEvaluator)
    val native=resolver.getTriggeredAbilities(recipient.entityId,host.cardDefinitionId,state)
    val gc=state.projectedState.getController(granter.entityId) ?: return result(Verdict.UNKNOWN,"Granter controller unknown")
    val rc=state.projectedState.getController(recipient.entityId) ?: return result(Verdict.UNKNOWN,"Recipient controller unknown")
    if(kind==GrantRecipientKind.FILTER_GROUP) {
        val providers=BattlefieldStaticsIndex.build(state,services.cardRegistry,services.predicateEvaluator).triggerGrantProviders
        if(providers.none { it.sourceEntityId==granter.entityId && it.grant==declaration }) return result(Verdict.NO_MATCH,"Native grant provider absent")
        if(declaration.filter.excludeSelf && granter.entityId==recipient.entityId ||
            !services.predicateEvaluator.matches(state,state.projectedState,recipient.entityId,declaration.filter.baseFilter,PredicateContext(controllerId=gc,sourceId=granter.entityId)))
            return result(Verdict.NO_MATCH,"Native recipient predicate does not qualify")
    } else if(granter.entityId !in resolver.resolveGranterIds(state,recipient.entityId,declaration.ability.id))
        return result(Verdict.NO_MATCH,"Native attachment grant lookup does not expose this granter")
    if(native.none { it==declaration.ability }) return result(Verdict.UNKNOWN,"Native resolved rule differs or is not exposed; caps/text changes require extra binding")
    fun owner(id: EntityId)=state.getEntity(id)?.get<OwnerComponent>()?.playerId
    return GrantedTriggerAvailability(Verdict.MATCH,GrantedAbilityInstance(id,granter,recipient,declaration.ability.id,owner(granter.entityId),gc,owner(recipient.entityId),rc),"Current native grant; no detection, targets, choice or outcome implied")
}

@Serializable data class GrantedTriggerObservation(val verdict: Verdict, val declarationId: String,
    val instance: GrantedAbilityInstance, val checkpoint: String, val stackObject: ObjectRef?,
    val controller: EntityId?, val nativeGranterId: EntityId?, val sourceBattlefieldTimestamp: Long?,
    val context: TriggerContext?, val event: JsonElement?, val eventCandidates: Int,
    val lastKnownSource: EntitySnapshot?, val reason: String,
    val targets: TargetsComponent? = null)

/** Correlates an explicitly supplied native stack ID and actual event slice. It does not scan
 * unrelated stack objects, detect events, parse stamped IDs or implement trigger precedence. */
fun GrantedTrigger.observeStack(before: GameState, after: GameState, events: List<GameEvent>,
    instance: GrantedAbilityInstance, stackObject: ObjectRef, services: EngineServices,
    attachmentAction: ActionEvidence?=null): GrantedTriggerObservation {
    val stack=after.getEntity(stackObject.entityId)?.get<TriggeredAbilityOnStackComponent>()
    fun result(v: Verdict,s: String,causes: List<GameEvent> = emptyList())=GrantedTriggerObservation(v,id,instance,"NATIVE_STACK_PLACEMENT",stackObject,
        stack?.controllerId,stack?.granterId,stack?.sourceBattlefieldTimestamp,stack?.triggerContext,
        causes.singleOrNull()?.let { nativeJson.encodeToJsonElement(GameEvent.serializer(),it) },causes.size,stack?.lastKnownSourceSnapshot,s,
        after.getEntity(stackObject.entityId)?.get<TargetsComponent>())
    if(instance.declarationId!=id || observeAvailability(before,instance.granter,instance.recipient,services,attachmentAction).instance!=instance)
        return result(Verdict.UNKNOWN,"Exact event-time current grant not established")
    if(!after.isCurrentObject(stackObject) || stackObject.entityId !in after.stack || before.isCurrentObject(stackObject) || stack==null)
        return result(Verdict.UNKNOWN,"No newly created native trigger stack object")
    val emitted=events.filterIsInstance<AbilityTriggeredEvent>().singleOrNull { it.abilityEntityId==stackObject.entityId }
        ?: return result(Verdict.UNKNOWN,"No unique native stack-placement event")
    if(stack.sourceId!=instance.recipient.entityId || emitted.sourceId!=stack.sourceId ||
        stack.controllerId!=instance.recipientController || emitted.controllerId!=stack.controllerId ||
        stack.granterId!=instance.granter.entityId || stack.resolvingTriggeredAbility!=declaration.ability ||
        stack.objectReferences.origin!=instance.recipient)
        return result(Verdict.UNKNOWN,"Captured native source/controller/granter/rules do not prove this declaration")
    if(!after.isCurrentObject(instance.granter)) return result(Verdict.UNKNOWN,"Granter visit changed within event/placement; no captured native granter generation")
    val context=stack.triggerContext ?: return result(Verdict.UNKNOWN,"No native trigger context")
    val causes=events.filter { e -> when(val pattern=declaration.ability.trigger) {
        is EventPattern.DealsDamageEvent -> e is DamageDealtEvent && context.damageSourceId==instance.recipient.entityId && e.sourceId==context.damageSourceId && e.targetId==context.damageRecipientId && e.amount==context.damageAmount &&
            (e.sourceLastKnown?.objectRef ?: instance.recipient.takeIf { after.isCurrentObject(it) })==instance.recipient
        // A native attack event is a batch, not a singular triggeringEntityId. The captured
        // SELF source and attacker's player bind this recipient to that actual batch.
        is EventPattern.AttackEvent -> e is AttackersDeclaredEvent && stack.sourceId in e.attackers && context.triggeringPlayerId==e.attackingPlayerId && after.isCurrentObject(instance.recipient)
        is EventPattern.StepEvent -> e is StepChangedEvent && e.newStep==pattern.step && context.step==pattern.step && (context.triggeringPlayerId ?: after.activePlayerId)==stack.controllerId && after.isCurrentObject(instance.recipient)
        else -> false
    } }
    val visit=before.getEntity(instance.recipient.entityId)?.get<BattlefieldEntryTimestampComponent>()?.timestamp
    // Some native setup/entry routes omit visit timestamps. The captured origin ObjectRef
    // above still proves the exact source generation; never synthesize a timestamp.
    if(visit!=stack.sourceBattlefieldTimestamp || eventSupport!=Verdict.MATCH || causes.size!=1)
        return result(Verdict.UNKNOWN,"Native trigger captured, but visit/event association is incomplete or nonunique",causes)
    return result(Verdict.MATCH,"Native source, supplying granter, captured controller, rules, source visit and unique event context established; resolution is a separate checkpoint",causes)
}

/** Once placement was witnessed, consult that stack snapshot, never the live grant. */
fun GrantedTrigger.observePersistence(state: GameState, established: GrantedTriggerObservation): GrantedTriggerObservation {
    val ref=established.stackObject
    if(established.declarationId!=id || established.instance.declarationId!=id || established.verdict!=Verdict.MATCH || ref==null || !state.isCurrentObject(ref) || ref.entityId !in state.stack)
        return established.copy(verdict=Verdict.UNKNOWN,checkpoint="PENDING_RESOLUTION",reason="No current established stack object; disappearance alone does not prove resolution")
    val stack=state.getEntity(ref.entityId)?.get<TriggeredAbilityOnStackComponent>()
    val same=stack!=null && stack.sourceId==established.instance.recipient.entityId && stack.objectReferences.origin==established.instance.recipient &&
        stack.granterId==established.nativeGranterId && stack.controllerId==established.controller && stack.resolvingTriggeredAbility==declaration.ability &&
        stack.triggerContext==established.context && stack.sourceBattlefieldTimestamp==established.sourceBattlefieldTimestamp
    return established.copy(verdict=if(same) Verdict.MATCH else Verdict.UNKNOWN,checkpoint="PENDING_RESOLUTION",
        lastKnownSource=stack?.lastKnownSourceSnapshot,targets=state.getEntity(ref.entityId)?.get<TargetsComponent>(),
        reason="Captured native trigger checked independently of current grant, source generation or controller; no resolution outcome implied")
}
