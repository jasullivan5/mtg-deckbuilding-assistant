package dev.mtg.assistant.discovery

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.event.*
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

internal fun triggerRecipientProblems(card: CardDefinition?, grant: GrantTriggeredAbility, conditional: Boolean = false): List<String> {
    if(card==null) return listOf("Exact native definition face unresolved")
    return sharedGrantRecipientProblems(card, grant.filter, conditional) + listOfNotNull(
        if (grant.ability.activeZones != setOf(Zone.BATTLEFIELD)) "Nonbattlefield/union active zones" else null
    )
}

internal fun grantedEventSupport(a: TriggeredAbility, conditionalSelf: Boolean = false): Verdict = when {
    a.controlledByTriggeringEntityController -> Verdict.UNKNOWN
    a.trigger is EventPattern.DealsDamageEvent && a.binding==TriggerBinding.SELF -> Verdict.MATCH
    a.trigger is EventPattern.AttackEvent && a.binding==TriggerBinding.SELF -> Verdict.MATCH
    a.trigger is EventPattern.StepEvent && a.binding==TriggerBinding.ANY && (a.trigger as EventPattern.StepEvent).player==Player.You -> Verdict.MATCH
    conditionalSelf && a.trigger.isBoundedGrantDeath() && a.binding in setOf(TriggerBinding.SELF, TriggerBinding.ANY) -> Verdict.MATCH
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
    val sourceFidelity: Verdict = Verdict.UNKNOWN,
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    val conditional: ConditionalGrant? = null)

@Serializable data class GrantedTriggerAvailability(val verdict: Verdict, val instance: GrantedAbilityInstance?=null, val reason: String, val condition: GrantConditionObservation? = null)

/** Read the same native resolver/index used by the detector. No trigger is created here. */
fun GrantedTrigger.observeAvailability(state: GameState, granter: ObjectRef, recipient: ObjectRef,
    services: EngineServices, attachmentAction: ActionEvidence?=null): GrantedTriggerAvailability {
    var condition: GrantConditionObservation? = null
    fun result(v: Verdict,s: String)=GrantedTriggerAvailability(v,reason=s,condition=condition)
    if(!state.isCurrentObject(granter) || !state.isCurrentObject(recipient) ||
        state.logicalZone(granter.entityId)?.zoneType!=Zone.BATTLEFIELD || state.logicalZone(recipient.entityId)?.zoneType!=Zone.BATTLEFIELD)
        return result(Verdict.NO_MATCH,"Granter or recipient is not the supplied battlefield generation")
    val source=state.getEntity(granter.entityId)?.get<CardComponent>() ?: return result(Verdict.UNKNOWN,"No source card identity")
    val def=services.cardRegistry.getCard(source.cardDefinitionId) ?: return result(Verdict.UNKNOWN,"No native source definition")
    if(def.name!=faceName || !matchesGrantDeclaration(def.script.staticAbilities.getOrNull(staticIndex), declaration, staticIndex, path, conditional))
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
    condition = conditional?.observeCondition(state, services, granter, recipient, gc)
    condition?.takeIf { it.verdict != Verdict.MATCH }?.let { return result(it.verdict, "Conditional grant enabling condition not established") }
    if(kind==GrantRecipientKind.FILTER_GROUP || kind==GrantRecipientKind.SOULBOND_PAIR) {
        val providers=BattlefieldStaticsIndex.build(state,services.cardRegistry,services.predicateEvaluator).triggerGrantProviders
        if(providers.none { it.sourceEntityId==granter.entityId && it.grant==declaration }) return result(Verdict.NO_MATCH,"Native grant provider absent")
    } else if(kind != GrantRecipientKind.SELF && granter.entityId !in resolver.resolveGranterIds(state,recipient.entityId,declaration.ability.id))
        return result(Verdict.NO_MATCH,"Native attachment grant lookup does not expose this granter")
    val match = matchSharedGrantRecipient(state, services, declaration.filter, granter.entityId, recipient.entityId, gc)
        ?: return result(Verdict.NO_MATCH, "Current native recipient restriction does not qualify")
    if(native.none { it==declaration.ability }) return result(Verdict.UNKNOWN,"Native resolved rule differs or is not exposed; caps/text changes require extra binding")
    fun owner(id: EntityId)=state.getEntity(id)?.get<OwnerComponent>()?.playerId
    return GrantedTriggerAvailability(Verdict.MATCH,GrantedAbilityInstance(id,granter,recipient,declaration.ability.id,
        owner(granter.entityId),gc,owner(recipient.entityId),rc,match.pairedPartner),"Current native grant; no detection, targets, choice or outcome implied",condition)
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
    val selfGrant = kind == GrantRecipientKind.SELF && conditional != null && instance.granter == instance.recipient
    val expectedGranter = if (selfGrant) null else instance.granter.entityId
    if(stack.sourceId!=instance.recipient.entityId || emitted.sourceId!=stack.sourceId ||
        stack.controllerId!=instance.recipientController || emitted.controllerId!=stack.controllerId ||
        stack.granterId!=expectedGranter || stack.resolvingTriggeredAbility!=declaration.ability ||
        stack.objectReferences.origin!=instance.recipient)
        return result(Verdict.UNKNOWN,"Captured native source/controller/granter/rules do not prove this declaration")
    val departureGrant = selfGrant && conditional?.checkpoint == ConditionalGrantCheckpoint.PRE_DEPARTURE
    if(!departureGrant && !after.isCurrentObject(instance.granter))
        return result(Verdict.UNKNOWN,"Granter visit changed within event/placement; no captured native granter generation")
    val context=stack.triggerContext ?: return result(Verdict.UNKNOWN,"No native trigger context")
    val causes=events.filter { e -> when(val pattern=declaration.ability.trigger) {
        is EventPattern.DealsDamageEvent -> e is DamageDealtEvent && context.damageSourceId==instance.recipient.entityId && e.sourceId==context.damageSourceId && e.targetId==context.damageRecipientId && e.amount==context.damageAmount &&
            (e.sourceLastKnown?.objectRef ?: instance.recipient.takeIf { after.isCurrentObject(it) })==instance.recipient
        // A native attack event is a batch, not a singular triggeringEntityId. The captured
        // SELF source and attacker's player bind this recipient to that actual batch.
        is EventPattern.AttackEvent -> e is AttackersDeclaredEvent && stack.sourceId in e.attackers && context.triggeringPlayerId==e.attackingPlayerId && after.isCurrentObject(instance.recipient)
        is EventPattern.StepEvent -> e is StepChangedEvent && e.newStep==pattern.step && context.step==pattern.step && (context.triggeringPlayerId ?: after.activePlayerId)==stack.controllerId && after.isCurrentObject(instance.recipient)
        is EventPattern.ZoneChangeEvent -> departureGrant && e is ZoneChangeEvent &&
            e.isBattlefieldDeparture && e.toZone == Zone.GRAVEYARD && e.oldObject != null &&
            context.triggeringOrigin == e.oldObject && context.triggeringEntityId == e.entityId &&
            before.isCurrentObject(checkNotNull(e.oldObject)) &&
            e.lastKnown?.lookBackGrants?.let { frozen ->
                if (e.oldObject == instance.recipient) {
                    declaration.ability.id in frozen.conditionalSelfGrantIds
                } else {
                    after.isCurrentObject(instance.recipient) &&
                        declaration.ability.id in frozen.observerSelfGrantIds[instance.recipient.entityId].orEmpty()
                }
            } == true
        else -> false
    } }
    val visit=before.getEntity(instance.recipient.entityId)?.get<BattlefieldEntryTimestampComponent>()?.timestamp
    // Some native setup/entry routes omit visit timestamps. The captured origin ObjectRef
    // above still proves the exact source generation; never synthesize a timestamp.
    if(visit!=stack.sourceBattlefieldTimestamp || eventSupport!=Verdict.MATCH || causes.size!=1)
        return result(Verdict.UNKNOWN,"Native trigger captured, but visit/event association is incomplete or nonunique",causes)
    val evidence = if (departureGrant) "Native pre-departure grant IDs and captured source generation establish the historical grant"
        else "Native source, supplying granter, captured controller, rules, source visit and unique event context established"
    return result(Verdict.MATCH, "$evidence; resolution is a separate checkpoint", causes)
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
