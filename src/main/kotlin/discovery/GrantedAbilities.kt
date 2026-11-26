package dev.mtg.assistant.discovery

import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.AbilityActivatedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ManaAbilityResolvedEvent
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.ownActivatedAbilities
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.TargetObject
import kotlinx.serialization.Serializable

@Serializable enum class GrantRecipientKind { AURA_HOST, EQUIPMENT_HOST, FILTER_GROUP, UNRESOLVED }

/** Existing action/fact/collection vocabulary, interpreted with a receiving source. These facts
 * are intentionally separate from intrinsic Discover inputs and carry no admission decision. */
@Serializable data class GrantedBody(val action: ActionEvidence, val facts: List<MechanicalFact>,
    val unsupported: List<Unsupported>, val structuralSupport: List<StructuralSupport>,
    val source: String = "RECEIVING_OBJECT", val controller: String = "ACTIVATOR",
    val playerReferences: String = "YOU and Controller refer to the granted ability's activating player; Self and source characteristics refer to its recipient, never its granting card.")

@Serializable data class GrantedAbility(
    val id: String, val actionId: String, val card: String, val face: String,
    val path: String, val staticIndex: Int, val kind: GrantRecipientKind,
    val declaration: GrantActivatedAbility, val attachmentActionId: String?,
    /** Recipient binding only; nested body support and source fidelity remain separate. */
    val support: Verdict, val unresolved: List<String>, val prerequisites: List<String>,
    val body: GrantedBody,
    val sourceBinding: String = "RECEIVING_OBJECT",
    val activationController: String = "RECIPIENT_CONTROLLER_AT_ACTIVATION",
    val costBinding: String = "Tap/Self costs use the recipient; player resources use the activator. Other explicit cost bindings remain native declarations.",
    val availability: String = "Current native projection and grant lookup; active battlefield granter, eligible current recipient and actual attachment/filter required. Availability is separate from legal activation.",
    val resolution: String = "An accepted nonmana activation has its own stack object and captured native ability. Losing the grant prevents future activations, not automatic cancellation of that stack object. Native source/LKI and target legality still govern resolution.",
    val sourceFidelity: Verdict = Verdict.UNKNOWN)

internal fun grantRecipientKind(card: CardDefinition, grant: GrantActivatedAbility) = grantRecipientKind(card,grant.filter)
internal fun grantRecipientKind(card: CardDefinition, filter: GroupFilter): GrantRecipientKind = when {
    filter.scope==Scope.AttachedTo && card.isAura -> GrantRecipientKind.AURA_HOST
    filter.scope==Scope.AttachedTo && card.isEquipment -> GrantRecipientKind.EQUIPMENT_HOST
    filter.scope==Scope.Battlefield -> GrantRecipientKind.FILTER_GROUP
    else -> GrantRecipientKind.UNRESOLVED
}

internal fun grantRecipientProblems(card: CardDefinition, face: String, grant: GrantActivatedAbility): List<String> {
    val kind=grantRecipientKind(card,grant)
    return listOfNotNull(
        if(grant.recipientZone!=Zone.BATTLEFIELD || grant.ability.activateFromZone!=Zone.BATTLEFIELD) "Nonbattlefield recipient/activation zone" else null,
        if(kind==GrantRecipientKind.UNRESOLVED) "Recipient scope outside bounded attachment/group grants" else null,
        if(kind==GrantRecipientKind.AURA_HOST && card.script.auraTarget !in listOf(TargetObject(filter=TargetFilter.Creature),TargetObject(filter=TargetFilter.Creature.youControl()))) "Aura recipient is not a bounded creature enchant restriction" else null,
        if(kind in setOf(GrantRecipientKind.AURA_HOST,GrantRecipientKind.EQUIPMENT_HOST) && grant.filter!=GroupFilter.attachedCreature()) "Additional attached recipient restrictions" else null,
        if(kind==GrantRecipientKind.FILTER_GROUP && grant.filter.copy(excludeSelf=false) !in listOf(GroupFilter(GameObjectFilter.Creature),GroupFilter(GameObjectFilter.Creature.youControl()))) "Group filter beyond plain creatures / creatures you control" else null,
        if(grant.ability.isPlaneswalkerAbility) "Granted loyalty semantics outside supported subset" else null,
        if(face!="front") "Concrete observation requires exact front-definition binding" else null)
}

@Serializable data class GrantedAbilityInstance(val declarationId: String, val granter: ObjectRef,
    val recipient: ObjectRef, val abilityId: AbilityId, val granterOwner: EntityId?, val granterController: EntityId,
    val recipientOwner: EntityId?, val recipientController: EntityId)

@Serializable data class GrantAvailabilityObservation(val verdict: Verdict, val instance: GrantedAbilityInstance? = null,
    val reason: String, val activationLegality: Verdict = Verdict.UNKNOWN)

/** Uses the engine's projected state and static grant lookup, not an application layer engine.
 * The caller selects an exact recipient generation; no recipient count or activation is invented. */
fun GrantedAbility.observeAvailability(state: GameState, granter: ObjectRef, recipient: ObjectRef,
    services: EngineServices, attachmentAction: ActionEvidence? = null): GrantAvailabilityObservation {
    fun result(v: Verdict, why: String)=GrantAvailabilityObservation(v,reason=why)
    if(!state.isCurrentObject(granter) || !state.isCurrentObject(recipient) ||
        state.logicalZone(granter.entityId)?.zoneType!=Zone.BATTLEFIELD ||
        state.logicalZone(recipient.entityId)?.zoneType!=declaration.recipientZone)
        return result(Verdict.NO_MATCH,"Source or recipient is no longer the supplied battlefield generation")
    val source=state.getEntity(granter.entityId)?.get<CardComponent>() ?: return result(Verdict.UNKNOWN,"No native card identity")
    val definition=services.cardRegistry.getCard(source.cardDefinitionId) ?: return result(Verdict.UNKNOWN,"No native definition")
    if(face!="front" || definition.name!=card || definition.script.staticAbilities.getOrNull(staticIndex)!=declaration)
        return result(Verdict.UNKNOWN,"Native source definition/face/declaration does not match")
    if(definition.script.staticAbilities.filterIsInstance<GrantActivatedAbility>().count { it.ability.id==declaration.ability.id }!=1)
        return result(Verdict.UNKNOWN,"Native lookup does not distinguish same-ID declarations on one granter")
    if(support!=Verdict.MATCH) return result(Verdict.UNKNOWN,"Structural recipient binding is unresolved")
    if(kind==GrantRecipientKind.AURA_HOST) {
        val root=attachmentAction ?: return result(Verdict.UNKNOWN,"Existing Aura attachment evidence required")
        if(root.id!=attachmentActionId) return result(Verdict.UNKNOWN,"Different attachment declaration")
        val attachment=root.attachment?.observe(root,state,granter,services.cardRegistry,services.predicateEvaluator,recipient)
            ?: return result(Verdict.UNKNOWN,"No existing Aura attachment relationship")
        if(attachment.verdict!=Verdict.MATCH) return result(attachment.verdict,"Existing Aura current-host query does not establish this recipient")
    }
    val grants=services.castPermissionUtils.getStaticGrantedAbilitiesWithGranter(recipient.entityId,state)
    if(grants.none { it.granterId==granter.entityId && it.ability==declaration.ability } &&
        grants.any { it.ability.id==declaration.ability.id })
        return result(Verdict.UNKNOWN,"Native lookup exposes a different same-ID supplier; equivalent-grant deduplication does not prove this declaration absent")
    if(grants.none { it.granterId==granter.entityId && it.ability==declaration.ability })
        return result(Verdict.NO_MATCH,"Native projected static grant is absent for this exact granter and recipient")
    if(state.projectedState.hasLostAllAbilities(recipient.entityId))
        return result(Verdict.UNKNOWN,"Recipient ability removal/layer interactions require native activation validation")
    val gc=state.projectedState.getController(granter.entityId) ?: return result(Verdict.UNKNOWN,"Granter controller unresolved")
    val rc=state.projectedState.getController(recipient.entityId) ?: return result(Verdict.UNKNOWN,"Recipient controller unresolved")
    if(!services.predicateEvaluator.matches(state,state.projectedState,recipient.entityId,GameObjectFilter.Creature,
            PredicateContext(controllerId=gc,sourceId=granter.entityId)))
        return result(Verdict.NO_MATCH,"The current recipient is not a projected creature")
    fun owner(id: EntityId)=state.getEntity(id)?.get<OwnerComponent>()?.playerId
    return GrantAvailabilityObservation(Verdict.MATCH,GrantedAbilityInstance(id,granter,recipient,declaration.ability.id,
        owner(granter.entityId),gc,owner(recipient.entityId),rc),"Native current grant exists; costs, priority, sickness, targets and activation restrictions remain native checks")
}

@Serializable data class GrantedActivationObservation(val verdict: Verdict, val declarationId: String,
    val instance: GrantedAbilityInstance, val activator: EntityId, val stackObject: ObjectRef?,
    val manaAbility: Boolean, val reason: String)

/** The caller supplies one accepted native transaction's before/after states and events. An
 * activation is not reconstructed from a name, a current grant, or an unversioned source ID. */
fun GrantedAbility.observeActivation(before: GameState, after: GameState, action: ActivateAbility,
    events: List<GameEvent>, instance: GrantedAbilityInstance, services: EngineServices,
    attachmentAction: ActionEvidence? = null): GrantedActivationObservation {
    fun result(v: Verdict, why: String, stack: ObjectRef?=null)=GrantedActivationObservation(v,id,instance,action.playerId,stack,declaration.ability.isManaAbility,why)
    if(instance.declarationId!=id || action.sourceId!=instance.recipient.entityId || action.abilityId!=instance.abilityId ||
        action.playerId!=instance.recipientController)
        return result(Verdict.UNKNOWN,"Activation does not bind this declaration, receiving generation and controller")
    val available=observeAvailability(before,instance.granter,instance.recipient,services,attachmentAction)
    if(available.instance!=instance) return result(Verdict.UNKNOWN,"Exact native grant instance not demonstrated before activation")
    val activated=events.filterIsInstance<AbilityActivatedEvent>().singleOrNull {
        it.sourceId==action.sourceId && it.controllerId==action.playerId && it.isManaAbility==declaration.ability.isManaAbility
    } ?: return result(Verdict.UNKNOWN,"No unique native activation event")
    if(declaration.ability.isManaAbility) {
        // Mana activations have no stack snapshot carrying granterId. Require a unique native
        // supplier, including the engine's own/copy/runtime/emblem ability pools; never choose
        // between same-ID grants by reproducing the engine's lookup precedence here.
        val recipient=before.getEntity(action.sourceId)
        val card=recipient?.get<CardComponent>()
        val definition=card?.let { services.cardRegistry.getCard(it.cardDefinitionId) }
        val suppliers=services.castPermissionUtils.getStaticGrantedAbilitiesWithGranter(action.sourceId,before)
            .filter { it.ability.id==action.abilityId }
        if(card==null || suppliers.size!=1 || suppliers.single().granterId!=instance.granter.entityId ||
            ownActivatedAbilities(card,definition,recipient.get<ClassLevelComponent>()?.currentLevel).any { it.id==action.abilityId } ||
            before.grantedActivatedAbilities.any { it.entityId==action.sourceId && it.ability.id==action.abilityId } ||
            services.castPermissionUtils.getEmblemGrantedActivatedAbilities(action.sourceId,before).any { it.id==action.abilityId })
            return result(Verdict.UNKNOWN,"Mana activation observed, but a unique supplying grant is not demonstrated")
        val resolved=events.filterIsInstance<ManaAbilityResolvedEvent>().any { it.sourceId==action.sourceId && it.playerId==action.playerId }
        return result(if(resolved && activated.abilityEntityId==null) Verdict.MATCH else Verdict.UNKNOWN,
            "Native mana activation/resolution checkpoint; pauses and unsupported mana continuations remain unresolved")
    }
    val stackId=activated.abilityEntityId ?: return result(Verdict.UNKNOWN,"No native ability stack object")
    val stack=after.objectRef(stackId) ?: return result(Verdict.UNKNOWN,"No stack generation")
    if(before.isCurrentObject(stack)) return result(Verdict.UNKNOWN,"Stack object predates this transaction")
    val captured=after.getEntity(stackId)?.get<ActivatedAbilityOnStackComponent>()
        ?: return result(Verdict.UNKNOWN,"No native activated-ability snapshot")
    if(captured.sourceId!=instance.recipient.entityId || captured.controllerId!=action.playerId ||
        captured.granterId!=instance.granter.entityId || captured.activatedAbility!=declaration.ability ||
        captured.objectReferences.origin!=instance.recipient)
        return result(Verdict.UNKNOWN,"Native stack snapshot does not establish the receiving source and granting instance")
    return result(Verdict.MATCH,"Accepted native activation captured recipient source, activator, granter and ability; future grant loss does not erase this stack object. Resolution and LKI remain native.",stack)
}
