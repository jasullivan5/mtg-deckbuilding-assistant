package dev.mtg.assistant.discovery

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.conditions.*
import com.wingedsheep.sdk.scripting.costs.*
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.json.*

private fun Effect.tree() = nativeJson.encodeToJsonElement(Effect.serializer(), this)
private data class Frame(val targets: List<TargetRequirement>, val tokenController: String? = null, val tokenEvent: String? = null, val actor: String = "YOU",
    val rootTargetSelection: Boolean = true, val receivingSource: ObjectScope? = null, val grantingSource: ObjectScope? = null,
    val iteration: ObjectScope? = null, val assumptions: List<String> = emptyList(),
    val movementTargets: MutableMap<String,TargetMovement> = mutableMapOf(),
    val collectionBindings: MutableMap<String,String> = mutableMapOf(),
    val collections: MutableMap<String,ObjectScope> = mutableMapOf(), val collectionMax: MutableMap<String,Int?> = mutableMapOf(), val guardConfidence: Verdict = Verdict.MATCH, val factGuard: Verdict = Verdict.MATCH, val resolutionCost: Boolean = false, val selfZone: Zone? = Zone.BATTLEFIELD, val relationship: String = "UNLINKED_RESOLUTION", val optional: Boolean = false, val delayed: Boolean = false, val objectRestrictions: List<String> = emptyList(),
    val directBattlefieldStepTrigger: Boolean = false, val event: EventContext? = null,
    val outcomeGuard: Verdict = Verdict.MATCH, val returnSource: ReturnSourceContext? = null,
    val returnGuard: Verdict = Verdict.MATCH, val returnLimits: List<ReturnLimit> = emptyList(),
    val operationGuard: Verdict = Verdict.MATCH, val operationPlayers: Player? = null,
    val counterTargetIteration: Boolean = false, val classAvailability: ClassAvailability? = null)
private data class Removal(val effect: Effect, val target: EffectTarget?, val operation: String, val magnitude: Int? = null, val prerequisites: List<String> = emptyList(), val confidence: Verdict = Verdict.MATCH)
private fun removal(e: Effect): Removal? = when(e) {
    is MoveToZoneEffect -> when {
        e.fromZone != null && e.fromZone != Zone.BATTLEFIELD -> null
        e.byDestruction -> Removal(e,e.target,"DESTROY")
        e.destination == Zone.EXILE -> Removal(e,e.target,"EXILE_REMOVAL")
        e.destination == Zone.GRAVEYARD -> Removal(e,e.target,"MOVE_TO_GRAVEYARD")
        else -> null
    }
    is MoveCollectionEffect -> if((e.destination as? CardDestination.ToZone)?.zone==Zone.GRAVEYARD && e.moveType!=MoveType.Discard)
        Removal(e,null,when(e.moveType) { MoveType.Destroy -> "DESTROY"; MoveType.Sacrifice -> "SACRIFICE"; else -> "MOVE_TO_GRAVEYARD" }) else null
    is DealDamageEffect -> amountBounds(e.amount).positivePotential().let { confidence ->
        if(confidence==Verdict.NO_MATCH) null else Removal(e,e.target,"DAMAGE_POTENTIALLY_LETHAL",(e.amount as? DynamicAmount.Fixed)?.amount,
            prerequisites=listOf("Damage amount must be positive and lethal in the actual state"),confidence=confidence)
    }
    is ModifyStatsEffect -> {
        val t = (e.toughnessModifier as? DynamicAmount.Fixed)?.amount
        val confidence=amountBounds(e.toughnessModifier).negativePotential()
        if(confidence==Verdict.NO_MATCH) null else Removal(e,e.target,"TOUGHNESS_REDUCTION",t?.let { -it },
            prerequisites=listOf("Toughness modifier must be strictly negative; actual toughness/death threshold and native conditions still apply"),confidence=confidence)
    }
    is ForceSacrificeEffect -> Removal(e,e.target,"SACRIFICE")
    else -> null
}
private fun removals(e: Effect?): List<Removal> = when(e) {
    null -> emptyList()
    is CompositeEffect -> e.effects.flatMap(::removals)
    is GatedEffect -> removals(e.then).map { it.copy(prerequisites=it.prerequisites+"Nested gate succeeds: ${e.gate}") } + removals(e.otherwise).map { it.copy(prerequisites=it.prerequisites+"Nested gate fails: ${e.gate}") }
    is ForEachEffect -> removals(e.body).map { it.copy(prerequisites=it.prerequisites+"Nested iteration: ${e.space}") }
    is ModalEffect -> e.modes.flatMap { removals(it.effect) }
    is ConditionalOnCollectionEffect -> removals(e.ifNotEmpty).map { it.copy(prerequisites=it.prerequisites+"Collection ${e.collection} satisfies native threshold/filter") } + removals(e.ifEmpty).map { it.copy(prerequisites=it.prerequisites+"Collection ${e.collection} fails native threshold/filter") }
    else -> listOfNotNull(removal(e))
}
private fun intersect(a: GameObjectFilter,b: GameObjectFilter): GameObjectFilter {
    val controllers=listOfNotNull(a.controllerPredicate,b.controllerPredicate)
    val branches=when { a.anyOf.isEmpty() -> b.anyOf; b.anyOf.isEmpty() -> a.anyOf; else -> a.anyOf.flatMap { x -> b.anyOf.map { y -> intersect(x,y) } } }
    return a.copy(cardPredicates=a.cardPredicates+b.cardPredicates,statePredicates=a.statePredicates+b.statePredicates,
        controllerPredicate=when(controllers.size) { 0 -> null; 1 -> controllers.single(); else -> com.wingedsheep.sdk.scripting.predicates.ControllerPredicate.And(controllers) },anyOf=branches)
}
private fun actor(player: Player): String = when(player) {
    Player.You, Player.ControllerOfSource -> "YOU"
    Player.Each, Player.Any, Player.EachStartingWithYou, Player.ActivePlayerFirst, Player.TargetPlayer -> "ANY"
    Player.AnOpponent, Player.EachOpponent, Player.TargetOpponent -> "OPPONENT"
    else -> "UNKNOWN"
}
private fun relativeActor(player: Player,frame: Frame): String = when {
    frame.actor=="YOU" -> actor(player)
    player==Player.You || player==Player.ControllerOfSource -> frame.actor
    else -> "UNKNOWN"
}
private fun ObjectScope.withActor(a: String) = copy(actor = a)
private fun scope(filter: com.wingedsheep.sdk.scripting.filters.unified.TargetFilter): ObjectScope =
    if(filter.alternatives.isEmpty()) ObjectScope(filter.baseFilter,battlefield = filter.zone == Zone.BATTLEFIELD,zone=filter.zone,zonePlayer=if(filter.zone==Zone.BATTLEFIELD) "UNKNOWN" else "ANY")
    else ObjectScope(unresolved = "Cross-zone target union requires branch-specific interpretation")
private fun targetScope(target: EffectTarget, frame: Frame, selfCreature: Boolean): ObjectScope = when(target) {
    EffectTarget.Self -> frame.receivingSource ?: ObjectScope(actor="YOU",printedSelfCreature=selfCreature,battlefield=frame.selfZone == Zone.BATTLEFIELD,zone=frame.selfZone,zonePlayer=if(frame.selfZone==Zone.GRAVEYARD) "ANY" else "UNKNOWN")
    EffectTarget.GrantingSource -> frame.grantingSource ?: ObjectScope(unresolved="Unresolved target: ${target.javaClass.simpleName}")
    is EffectTarget.ContextTarget -> requirementScope(frame.targets.getOrNull(target.index))
    is EffectTarget.BoundVariable -> requirementScope(frame.targets.find { it.id == target.name })
    is EffectTarget.FilteredTarget -> scope(target.filter)
    is EffectTarget.GroupRef -> ObjectScope(target.filter.baseFilter, unresolved=if(target.filter.scope == com.wingedsheep.sdk.scripting.filters.unified.Scope.Battlefield) null else "Non-battlefield group scope: ${target.filter.scope}")
    is EffectTarget.PipelineTarget -> {
        val source=frame.collections[target.collectionName]
        val max=frame.collectionMax[target.collectionName]
        when {
            target.index<0 || max!=null && max<=target.index -> ObjectScope(battlefield=false,possible=false)
            source==null -> ObjectScope(unresolved="Unbound pipeline target ${target.collectionName}[${target.index}]")
            else -> source
        }
    }
    EffectTarget.IterationEntity -> frame.iteration ?: ObjectScope(unresolved="Unbound iteration entity")
    is EffectTarget.PlayerRef, EffectTarget.Controller -> ObjectScope(battlefield=false)
    else -> ObjectScope(unresolved="Unresolved target: ${target.javaClass.simpleName}")
}
private fun requirementScope(t: TargetRequirement?): ObjectScope = when(t) {
    is TargetObject -> scope(t.filter)
    is AnyTarget -> ObjectScope(t.filter)
    is TargetCreatureOrPlayer, is TargetCreatureOrPlaneswalker -> ObjectScope(GameObjectFilter.Creature)
    is TargetPlayer, is TargetOpponent -> ObjectScope(battlefield=false)
    else -> ObjectScope(unresolved="Unresolved target requirement: ${t?.javaClass?.simpleName}")
}
private fun playerScope(target: EffectTarget, frame: Frame): String = when(target) {
    is EffectTarget.PlayerRef -> if(target.player == Player.You) frame.actor else actor(target.player)
    EffectTarget.Controller -> frame.actor
    is EffectTarget.ContextTarget -> when(frame.targets.getOrNull(target.index)) { is TargetOpponent -> "OPPONENT"; is TargetPlayer -> "ANY"; else -> "UNKNOWN" }
    is EffectTarget.BoundVariable -> when(frame.targets.find { it.id == target.name }) { is TargetOpponent -> "OPPONENT"; is TargetPlayer -> "ANY"; else -> "UNKNOWN" }
    else -> "UNKNOWN"
}

private fun targetRequirement(target: EffectTarget, frame: Frame): TargetRequirement? = when (target) {
    is EffectTarget.ContextTarget -> frame.targets.getOrNull(target.index)
    is EffectTarget.BoundVariable -> frame.targets.find { it.id == target.name }
    else -> null
}

private fun isPossiblePlayerDamage(target: EffectTarget, frame: Frame): Boolean =
    target == EffectTarget.Controller || target is EffectTarget.PlayerRef ||
        target == EffectTarget.ControllerOfTriggeringEntity ||
        when (targetRequirement(target, frame)) {
            is TargetPlayer, is TargetOpponent, is AnyTarget, is TargetCreatureOrPlayer,
            is TargetPermanentOrPlayer, is TargetPlayerOrPlaneswalker -> true
            else -> false
        }

private fun eventOperationRecipient(target: EffectTarget, frame: Frame): Pair<String, String> {
    val player = when (target) {
        EffectTarget.Controller -> frame.operationPlayers ?: Player.You
        is EffectTarget.PlayerRef -> if (target.player == Player.You) frame.operationPlayers ?: Player.You else target.player
        else -> null
    }
    return when (player) {
        Player.You -> "YOU" to "you"
        Player.Each, Player.ActivePlayerFirst, Player.EachStartingWithYou -> "ANY" to "each player"
        Player.EachOpponent -> "OPPONENT" to "each opponent"
        Player.AnOpponent -> "OPPONENT" to "an opponent"
        Player.OwnerOfSource -> "OWNER" to "the source's owner"
        else -> when (targetRequirement(target, frame)) {
            is TargetPlayer -> "ANY" to "the targeted player"
            is TargetOpponent -> "OPPONENT" to "the targeted opponent"
            else -> "UNKNOWN" to "the unresolved player"
        }
    }
}

/** Resolve the same native player reference used by the collection source and destination. */
private fun cardEntryPlayer(player: Player, frame: Frame): String {
    val requirement = when (player) {
        is Player.ContextPlayer -> frame.targets.getOrNull(player.index)
        is Player.BoundVariable -> frame.targets.find { it.id == player.name }
        Player.TargetPlayer, Player.TargetOpponent -> frame.targets.filter { it is TargetPlayer || it is TargetOpponent }.singleOrNull()
        else -> return when (player) {
            Player.You, Player.EachOpponent, Player.AnOpponent -> relativeActor(player, frame)
            Player.ControllerOfSource -> if (frame.actor == "YOU") "YOU" else "UNKNOWN"
            else -> "UNKNOWN"
        }
    }
    return when (requirement) {
        is TargetOpponent -> if (requirement.count > 0) "OPPONENT" else "UNKNOWN"
        is TargetPlayer -> if (requirement.count > 0 && requirement.restriction == null && player != Player.TargetOpponent) "ANY" else "UNKNOWN"
        else -> "UNKNOWN"
    }
}

/** Desired event can satisfy a native condition; other conjuncts remain prerequisites. */
internal fun deathDependency(c: Condition): Pair<Verdict,ObjectScope> = when(c) {
    CreatureDiedThisTurnCondition -> Verdict.MATCH to ObjectScope(GameObjectFilter.Creature)
    ControlledCreatureDiedThisTurnCondition -> Verdict.MATCH to ObjectScope(GameObjectFilter.Creature,actor="YOU")
    is CreatureDiedThisTurnMatching -> Verdict.MATCH to ObjectScope(c.filter,actor(c.player))
    is AllConditions, is AnyCondition -> Verdict.UNKNOWN to ObjectScope(unresolved="Composite condition causality not solved; native conjuncts/alternatives retained")
    is NotCondition -> Verdict.UNKNOWN to ObjectScope(unresolved="Negated condition causality not solved")
    IsYourTurn, IsNotYourTurn, IsOpponentsTurn, CouldCastSorcery -> Verdict.NO_MATCH to ObjectScope()
    else -> Verdict.UNKNOWN to ObjectScope(unresolved="Condition not interpreted as a death dependency: ${c.javaClass.simpleName}")
}

/** Native objects in, individual operations out. No card names or Oracle text are inspected. */
class PathwayExtractor(private val inspectCardEntries: Boolean = false, private val inspectStateDependencies: Boolean = false,
                       private val inspectCounters: Boolean = false) {
    val counterPlacements = mutableListOf<CounterPlacement>()
    val counterModifiers = mutableListOf<CounterPlacementModifier>()
    val stateDependencies = mutableListOf<StateDependency>()
    private var stateRootPath: String? = null
    val eventOutcomes = mutableListOf<EventOutcome>()
    val selfReturns = mutableListOf<SelfReturnLink>()
    val linkedMovements: List<LinkedMovement> get() = actions.flatMap { it.linkedMovements }
    private var sourceReplacementLimits = emptyList<ReturnLimit>()
    private var linkedExileProducers = 0
    val facts = mutableListOf<MechanicalFact>()
    val pathways = mutableListOf<Pathway>()
    val actions = mutableListOf<ActionEvidence>()
    val unsupported = mutableListOf<Unsupported>()
    val structuralSupport = mutableListOf<StructuralSupport>()
    private lateinit var card: CardDefinition
    private var face = ""
    private var selfCreature = false
    private var grantedSource = false
    private var actionId = ""
    private var rootEffect: Effect? = null
    private var baseCastingCosts: List<AdditionalCost> = emptyList()
    private var actionKind: String = "UNKNOWN"
    private var actionNative: JsonElement = JsonNull
    private fun circumstances(origin: String,scope: ObjectScope,frame: Frame): Circumstances {
        val trigger=(actionNative as? JsonObject)?.get("trigger") as? JsonObject
        return Circumstances(actionKind,
            if(frame.delayed && !frame.optional) Verdict.MATCH else if(frame.optional || actionKind in setOf("ACTIVATED","SPELL")) Verdict.NO_MATCH else if(actionKind in setOf("TRIGGERED","STATE_TRIGGERED")) Verdict.MATCH else Verdict.UNKNOWN,
            trigger?.get("type")?.jsonPrimitive?.contentOrNull=="StepEvent" || frame.delayed,frame.delayed,
            frame.assumptions,scope.printedSelfCreature!=null && scope.battlefield,
            frame.objectRestrictions+scope.details+listOfNotNull(scope.unresolved),
            when(actionKind) { "ACTIVATED" -> "Each activation subject to native costs/timing/limits"; "SPELL" -> "Each cast subject to native costs/timing/permissions"; else -> "Per matching trigger/branch; source deaths consume that battlefield object" })
    }
    private fun opposingRemoval(frame: Frame): Verdict = any(removals(rootEffect).map { r ->
        when(val e=r.effect) {
            is ForceSacrificeEffect -> ObjectScope(e.filter,playerScope(e.target,frame)).compatible(Requirements(controller=Controller.OPPONENT))
            is MoveCollectionEffect -> (frame.collections[e.from] ?: ObjectScope(unresolved="Opposing-removal collection unresolved")).compatible(Requirements(controller=Controller.OPPONENT))
            else -> r.target?.let { targetScope(it,frame,selfCreature).compatible(Requirements(controller=Controller.OPPONENT)) } ?: Verdict.UNKNOWN
        }
    })

    fun extract(cards: List<CardDefinition>): PathwayExtractor {
        counterPlacements.clear(); counterModifiers.clear()
        pathways.clear(); actions.clear(); unsupported.clear(); structuralSupport.clear(); facts.clear(); eventOutcomes.clear(); selfReturns.clear(); stateDependencies.clear()
        for(c in cards) { card=c; inspectDefinition(c,"front") }
        if (inspectStateDependencies) linkConditionalBranches()
        return this
    }

    private fun linkConditionalBranches() {
        val factsByAction = facts.groupBy { it.actionId }
        val outcomesByAction = eventOutcomes.groupBy { it.actionId }
        for ((index, dependency) in stateDependencies.withIndex()) {
            fun links(path: String?): ConditionalBranchLinks {
                if (path == null) return ConditionalBranchLinks()
                fun under(candidate: String) = candidate == path || candidate.startsWith("$path.") || candidate.startsWith("$path[")
                return ConditionalBranchLinks(
                    factsByAction[dependency.actionId].orEmpty().filter { under(it.path) }.map { it.id },
                    outcomesByAction[dependency.actionId].orEmpty().filter { under(it.path) }.map { it.id }
                )
            }
            stateDependencies[index] = dependency.copy(
                trueLinks = links(dependency.outcomePath),
                falseLinks = links(dependency.otherwisePath)
            )
        }
    }
    private fun support(path: String, value: Any, status: SupportStatus, reason: String) {
        structuralSupport += StructuralSupport(card.name,face,path,value.javaClass.simpleName,status,reason,actionId)
    }
    private fun issue(path: String, value: Any, reason: String, status: SupportStatus = SupportStatus.UNSUPPORTED) {
        unsupported += Unsupported(card.name,face,path,value.javaClass.name,reason,actionId)
        support(path,value,status,reason)
    }
    private fun add(path: String, operation: String, origin: String, objectScope: ObjectScope,
                    native: JsonElement, frame: Frame, incidental: Boolean = false, operationCompatibility: Verdict = Verdict.MATCH,
                    sacrificeChoice: SacrificeChoice? = null, sourceEntry: CardEntry? = null) {
        val kind=when(operation) { "SACRIFICE"->FactKind.SACRIFICE;"DAMAGE_POTENTIALLY_LETHAL"->FactKind.DAMAGE;"TOUGHNESS_REDUCTION"->FactKind.TOUGHNESS_CHANGE;else->FactKind.OTHER_OPERATION }
        val id="$actionId|$path|$operation|${facts.count { it.actionId==actionId && it.path==path && it.operation==operation }}"
        val sacrificeGuard = if (sacrificeChoice == null && sourceEntry == null) Verdict.MATCH else all(listOf(frame.factGuard,
            if ((actionNative as? JsonObject)?.get("controlledByTriggeringEntityController") == JsonPrimitive(true)) Verdict.UNKNOWN else Verdict.MATCH))
        facts += MechanicalFact(id,card.name,face,actionId,path,kind,operation,if(frame.resolutionCost && origin=="EFFECT") "RESOLUTION_COST" else origin,
            objectScope.filter,sourceZone=objectScope.zone ?: if(objectScope.battlefield) Zone.BATTLEFIELD else null,sourcePlayer=objectScope.actor,
            destinationZone=if(sacrificeChoice != null || sourceEntry != null) Zone.GRAVEYARD else null,
            destinationController=if(sacrificeChoice != null || sourceEntry != null) "OWNER" else "UNKNOWN",
            confidence=all(listOf(operationCompatibility,frame.guardConfidence,sacrificeGuard)),native=native,prerequisites=frame.assumptions,
            binding=sourceEntry?.let { "SELF" },selection=if(sourceEntry!=null) "Exactly the source permanent; no sacrifice choice" else "Native node/targets define quantity and chooser",
            circumstances=circumstances(origin,objectScope,frame),unresolved=listOfNotNull(objectScope.unresolved),
            ownerScope=objectScope.ownerScope,sacrificeChoice=sacrificeChoice,cardEntry=sourceEntry)
        pathways += Pathway(card,face,actionId,path,operation,if(frame.resolutionCost && origin == "EFFECT") "RESOLUTION_COST" else origin,objectScope,
            prerequisites=frame.assumptions + "Face $face must be available; transformation/flip prerequisites are not simulated" + "Potential only: legal choices, resources, applicable zones, and no death-replacing effects are assumed",native=native,
            factId=id, incidentalRemoval=incidental, operationCompatibility=all(listOf(operationCompatibility,frame.guardConfidence)),
            circumstances=circumstances(origin,objectScope,frame),
            opposingRemovalAssociation=if(origin.endsWith("COST") || frame.resolutionCost) Verdict.NO_MATCH else if(incidental) opposingRemoval(frame) else Verdict.NO_MATCH,
            relationship=when {
                origin.endsWith("COST") || frame.resolutionCost -> "SACRIFICE_PAYS_FOR_ACTION"
                frame.relationship != "UNLINKED_RESOLUTION" -> frame.relationship
                actionKind == "ACTIVATED" && objectScope.printedSelfCreature == true && native.jsonObject["type"]?.jsonPrimitive?.content == "ModifyStats" &&
                    (nativeJson.decodeFromJsonElement(Effect.serializer(),native) as? ModifyStatsEffect)?.let { ((it.powerModifier as? DynamicAmount.Fixed)?.amount ?: 0) > 0 && ((it.toughnessModifier as? DynamicAmount.Fixed)?.amount ?: 0) < 0 } == true -> "SELECTABLE_SELF_TOUGHNESS_EXCHANGE"
                actionKind == "TRIGGERED" || actionKind == "STATE_TRIGGERED" -> "MANDATORY_OR_INCIDENTAL_TRIGGER"
                else -> "UNLINKED_RESOLUTION"
            })
    }
    private fun enhanced(path: String, dependency: String, dep: Pair<Verdict,ObjectScope>,
                         removal: Removal, frame: Frame, improvement: Verdict) {
        // Player damage is not a creature-removal capability.
        val targetCompatible = when(val effect=removal.effect) {
            is ForceSacrificeEffect -> ObjectScope(effect.filter,playerScope(effect.target,frame)).compatible(Requirements())
            is MoveCollectionEffect -> (frame.collections[effect.from] ?: ObjectScope(unresolved="Removal collection source unresolved")).compatible(Requirements())
            else -> removal.target?.let { targetScope(it,frame,selfCreature).compatible(Requirements()) } ?: Verdict.UNKNOWN
        }
        if(targetCompatible == Verdict.NO_MATCH) return
        pathways += Pathway(card,face,actionId,path,removal.operation,"DEPENDENCY",dep.second,
            Contribution.DEATH_ENHANCED_REMOVAL,frame.assumptions + removal.prerequisites + "Death event must precede/trigger the removal action; all other native requirements still apply",
            removal.effect.tree(),dependency=dependency,dependencyVerdict=all(listOf(dep.first,improvement,targetCompatible,removal.confidence,frame.guardConfidence)),circumstances=circumstances("DEPENDENCY",dep.second,frame))
    }
    private fun inspectDefinition(def: CardDefinition, label: String) {
        inspectScript(def.script,label,def.isCreature,def.isClass,def.manaCost,def.keywordAbilities,def.isAura)
        def.backFace?.let { inspectDefinition(it,"$label.backFace") }
        def.flipSide?.let { inspectDefinition(it,"$label.flipSide") }
        def.cardFaces.forEachIndexed { i,f -> inspectScript(f.script,"$label.cardFaces[$i]:${f.name}",f.typeLine.isCreature,f.typeLine.isClass,f.manaCost,aura=f.typeLine.isAura) }
    }
    private fun inspectScript(script: CardScript, label: String, creature: Boolean, isClass: Boolean, manaCost: ManaCost, keywordAbilities: List<KeywordAbility> = emptyList(), aura: Boolean = false) {
        face=label; selfCreature=creature; baseCastingCosts=script.additionalCosts; linkedExileProducers=0
        if (inspectCounters) script.replacementEffects.forEachIndexed { i, effect -> recordCounterReplacement("script.replacementEffects[$i]", effect) }
        sourceReplacementLimits=script.replacementEffects.mapIndexed { i,r -> ReturnLimit("SOURCE_REPLACEMENT_UNRESOLVED","script.replacementEffects[$i]","An intrinsic source replacement effect may constrain entry or later reuse; it is not fully interpreted.",nativeJson.encodeToJsonElement(ReplacementEffect.serializer(),r)) }
        actionId="${card.name}|$label|script"
        val attachmentRootIndex = actions.size
        val attachmentOutcomeStart = eventOutcomes.size
        actions += ActionEvidence(actionId,card.name,label,"script",emptyList(),nativeJson.encodeToJsonElement(CardScript.serializer(),script))
        fun action(path: String, value: JsonElement, effect: Effect?, targets: List<TargetRequirement>,
                   assumptions: List<String>, sourceZone: Zone? = Zone.BATTLEFIELD,
                   directBattlefieldStepTrigger: Boolean = false, event: EventContext? = null, returnSource: ReturnSourceContext? = null,
                   availability: ClassAvailability? = null, conditionalCosts: List<ConditionalCost> = emptyList(), targetSelection: TargetSelection? = null, costs: () -> Unit = {}) {
            actionId="${card.name}|$label|$path"; rootEffect=effect; stateRootPath=if(path=="script.spell") "script.spellEffect" else "$path.effect"; actionNative=value
            actionKind=when { path.contains("activatedAbilities") -> "ACTIVATED"; path.contains("stateTriggeredAbilities") -> "STATE_TRIGGERED"; path.contains("triggeredAbilities") -> "TRIGGERED"; path == "script.spell" -> "SPELL"; else -> "OTHER" }
            actions += ActionEvidence(actionId,card.name,label,path,assumptions,value,classAvailability=availability,conditionalCosts=conditionalCosts,targetSelection=targetSelection,unionTargets=unionTargets(actionId,targets,if(actionKind=="SPELL") "CAST_ANNOUNCEMENT" else "TRIGGER_OR_ABILITY_ANNOUNCEMENT"))
            val frame=Frame(targets,assumptions=assumptions,selfZone=sourceZone,directBattlefieldStepTrigger=directBattlefieldStepTrigger,event=event,returnSource=returnSource,classAvailability=availability)
            costs()
            effect?.let { walk(it,if(path == "script.spell") "script.spellEffect" else "$path.effect",frame) }
        }
        val castAssumptions=listOf("Cast timing, mana and additional costs, cast restrictions and legal targets from native script")
        val castingTargets = script.targetRequirements.takeIf { it.isNotEmpty() && script.spellEffect !is ModalEffect }
            ?.let { TargetSelection("${card.name}|$label|script.spell", it) }
        val conditionalCastingCosts = script.staticAbilities.mapIndexedNotNull { i, ability ->
            selfCastingConditionalCost("script.staticAbilities[$i]", ability, manaCost, castingTargets)
        }
        if(script.spellEffect != null || script.additionalCosts.isNotEmpty() || script.selfAlternativeCost != null || conditionalCastingCosts.isNotEmpty()) action("script.spell",
            nativeJson.encodeToJsonElement(CardScript.serializer(),script),script.spellEffect,script.targetRequirements,castAssumptions,sourceZone=Zone.STACK,
            conditionalCosts=conditionalCastingCosts,targetSelection=castingTargets) {
            if(script.spellEffect !is ModalEffect) script.additionalCosts.forEachIndexed { i,c -> castingCost(c,"script.additionalCosts[$i]",Frame(script.targetRequirements,assumptions=castAssumptions)) }
        }
        script.selfAlternativeCost?.let { alternative ->
            val path="script.selfAlternativeCost"
            action(path,nativeJson.encodeToJsonElement(SelfAlternativeCost.serializer(),alternative),null,script.targetRequirements,
                listOf("Alternative mana cost=${alternative.manaCost}; condition=${alternative.condition}; mana sources=${alternative.manaSources}; chosen instead of normal mana cost")) {
                alternative.additionalCosts.forEachIndexed { i,c -> castingCost(c,"$path.additionalCosts[$i]",Frame(script.targetRequirements,
                    assumptions=listOf("Self alternative casting route chosen; condition=${alternative.condition}; mana sources=${alternative.manaSources}")),"ALTERNATIVE_CASTING_COST") }
            }
            actions[actions.lastIndex]=actions.last().copy(associatedActionId="${card.name}|$face|script")
            support(path,alternative,if(alternative.condition==null && alternative.manaSources==null) SupportStatus.SUPPORTED else SupportStatus.PARTIALLY_SUPPORTED,
                "Alternative-cost components traversed; native availability/resource gates are prerequisites, not whole-state satisfiability proofs")
        }
        fun activated(ability: ActivatedAbility,path: String, availability: ClassAvailability? = null) {
            val extra = availability?.prerequisites().orEmpty()
            val frame=Frame(ability.targetRequirements,selfZone=ability.activateFromZone,assumptions=extra + "Timing=${ability.timing}; restrictions=${ability.restrictions}; activateFrom=${ability.activateFromZone}",classAvailability=availability)
            action(path,nativeJson.encodeToJsonElement(ActivatedAbility.serializer(),ability),ability.effect,frame.targets,frame.assumptions,sourceZone=ability.activateFromZone,returnSource=ReturnSourceContext("ACTIVATED",setOf(ability.activateFromZone),ability.description,ability.cost.description,
                if(ability.restrictions.any { it is ActivationRestriction.MaxPerTurn && it.count<=0 }) Verdict.NO_MATCH else if(extra.isEmpty() && ability.restrictions.none { it is ActivationRestriction.OnlyIfCondition || it==ActivationRestriction.AnyPlayerMay || it is ActivationRestriction.All }) Verdict.MATCH else Verdict.UNKNOWN,
                sourceCostCompatibility=sourceCostCompatibility(ability.cost,ability.activateFromZone),requirements=frame.assumptions+"Pay all activation costs, with legal resources; the original return card must remain available",
                limits=ability.restrictions.mapIndexed { i,r -> ReturnLimit("ACTIVATION_RESTRICTION","$path.restrictions[$i]",when(r) {
                    ActivationRestriction.Once -> "This ability is limited to one activation; do not assume the allowance resets across zone changes."
                    ActivationRestriction.OncePerTurn -> "At most one activation each turn; an unused allowance is required."
                    is ActivationRestriction.MaxPerTurn -> "At most ${r.count} activations each turn; an unused allowance is required."
                    else -> "Native activation restriction: $r; do not infer unbounded reuse across zone changes"
                },nativeJson.encodeToJsonElement(ActivationRestriction.serializer(),r)) }+
                    if(ability.timing==TimingRule.SorcerySpeed) listOf(ReturnLimit("ACTIVATION_TIMING",path,"Activate only as a sorcery: your main phase, your priority and an empty stack are required.",nativeJson.encodeToJsonElement(TimingRule.serializer(),ability.timing))) else emptyList()), availability=availability,
                conditionalCosts=listOfNotNull(activationConditionalCost(path, ability))) {
                abilityCost(ability.cost,"$path.cost",frame)
            }
            ability.restrictions.forEachIndexed { i, restriction ->
                if(restriction is ActivationRestriction.OnlyIfCondition) recordState(
                    restriction.condition,"$path.restrictions[$i].condition","$path.effect",ability.effect,null,
                    StateCheckpoint.ACTIVATION,frame,
                    extra.isEmpty() && ability.restrictions.size==1 && ability.conditionalCostReduction==null && ability.genericCostReduction==null)
            }
            ability.conditionalCostReduction?.let { reduction ->
                removals(ability.effect).forEach { enhanced("$path.conditionalCostReduction", reduction.condition.description + "; cost reduced by ${reduction.reduction}",deathDependency(reduction.condition),it,frame,
                    if(reduction.reduction.cmc > 0) Verdict.MATCH else Verdict.UNKNOWN) }
            }
            ability.genericCostReduction?.let { issue("$path.genericCostReduction",it,"Dynamic discount not interpreted") }
        }
        fun triggered(ability: TriggeredAbility,path: String,availability: ClassAvailability? = null) {
            val extra = availability?.prerequisites().orEmpty()
            val assumptions=extra + listOf("Trigger=${ability.trigger}; binding=${ability.binding}; activeZones=${ability.activeZones}",
                "InterveningIf=${ability.interveningIf}; restriction=${ability.triggerRestriction}; cap=${ability.perTurnTriggerCap}")
            val trigger=ability.trigger
            val levelTransition = availability != null && ability.binding == TriggerBinding.SELF &&
                trigger is EventPattern.ZoneChangeEvent && trigger.to == Zone.BATTLEFIELD
            val event=EventContext(trigger,ability.binding,ability.activeZones,creature,
                nativeJson.encodeToJsonElement(TriggeredAbility.serializer(),ability),
                if(ability.activeZones.isEmpty()) Verdict.NO_MATCH else if(ability.interveningIf!=null || ability.triggerRestriction!=null || ability.controlledByTriggeringEntityController ||
                    ability.binding==TriggerBinding.ATTACHED || ability.activeZones!=setOf(Zone.BATTLEFIELD) ||
                    levelTransition || availability?.support == Verdict.UNKNOWN ||
                    trigger is EventPattern.ZoneChangeEvent && trigger.requireEmergeSacrifice) Verdict.UNKNOWN else Verdict.MATCH,
                assumptions + "Native active-zone rules and last-known information must permit triggering; trigger/turn limits must remain; actual resolution is not guaranteed" +
                    listOfNotNull("This source's ability triggers only once during its battlefield lifetime".takeIf { ability.triggersOnce },
                        "Do this only once each turn: resolution action budget must remain; declining does not spend it".takeIf { ability.effectOncePerTurn }))
            // A trigger observing another object can itself be active in a non-battlefield zone.
            // Keep self zone-change triggers bound to their resulting zone; do not infer a
            // single location for multi-zone abilities in this bounded correction.
            val sourceZone=if(trigger is EventPattern.ZoneChangeEvent && ability.binding == TriggerBinding.SELF) trigger.to
                else ability.activeZones.singleOrNull() ?: Zone.BATTLEFIELD
            val returnSource=ReturnSourceContext("TRIGGERED",ability.activeZones,ability.description,confidence=
                    if(ability.interveningIf!=null || ability.triggerRestriction!=null || ability.controlledByTriggeringEntityController || extra.isNotEmpty() ||
                        ability.binding==TriggerBinding.ATTACHED || ability.binding==TriggerBinding.SELF && Zone.GRAVEYARD in ability.activeZones ||
                        trigger is EventPattern.ZoneChangeEvent && (trigger.filter.statePredicates.isNotEmpty() || trigger.requireEmergeSacrifice)) Verdict.UNKNOWN else Verdict.MATCH,
                    selfDeathTransition=when {
                        ability.binding!=TriggerBinding.SELF -> Verdict.NO_MATCH
                        trigger is EventPattern.ZoneChangeEvent && trigger.requireEmergeSacrifice -> Verdict.UNKNOWN
                        trigger is EventPattern.ZoneChangeEvent && trigger.from==Zone.BATTLEFIELD && trigger.to==Zone.GRAVEYARD && trigger.excludeTo!=Zone.GRAVEYARD -> Verdict.MATCH
                        trigger is EventPattern.ZoneChangeEvent && (trigger.to!=null && trigger.to!=Zone.GRAVEYARD || trigger.excludeTo==Zone.GRAVEYARD) -> Verdict.NO_MATCH
                        else -> Verdict.UNKNOWN
                    },
                    requirements=assumptions+listOf("Trigger timing, native conditions and limits must permit this occurrence; source in a graveyard is owned by the ability controller unless native rebinding changes that", "Trigger cap=${ability.perTurnTriggerCap}; lifetime cap=${ability.triggersOnce}; resolution-once-per-turn=${ability.effectOncePerTurn}"),
                    limits=buildList {
                        val native=nativeJson.encodeToJsonElement(TriggeredAbility.serializer(),ability)
                        ability.perTurnTriggerCap?.let { add(ReturnLimit("TRIGGER_LIMIT",path,"At most $it matching triggers each turn; an unused trigger allowance is required.",native)) }
                        if(ability.triggersOnce) add(ReturnLimit("TRIGGER_LIMIT",path,"The native lifetime trigger limit applies; further use across zone changes is not established.",native))
                        if(ability.effectOncePerTurn) add(ReturnLimit("TRIGGER_LIMIT",path,"The action can be taken only once each turn; its resolution budget must remain.",native))
                        if(trigger is EventPattern.ZoneChangeEvent && trigger.excludeSacrifice) add(ReturnLimit("TRIGGER_RESTRICTION",path,"This trigger excludes sacrifices; sacrificing the source does not supply this return opportunity.",native))
                        if(trigger is EventPattern.ZoneChangeEvent && trigger.requireEmergeSacrifice) add(ReturnLimit("UNRESOLVED_TRIGGER_RESTRICTION",path,"Only a sacrifice while casting with emerge supplies this trigger; that payment relationship is not interpreted.",native))
                    })
            action(path,nativeJson.encodeToJsonElement(TriggeredAbility.serializer(),ability),ability.effect,ability.allTargetRequirements,assumptions,sourceZone=sourceZone,
                event=event,returnSource=returnSource,availability=availability,directBattlefieldStepTrigger=trigger is EventPattern.StepEvent && ability.activeZones == setOf(Zone.BATTLEFIELD) &&
                    ability.interveningIf == null && ability.triggerRestriction == null && !ability.controlledByTriggeringEntityController)
            if (levelTransition) issue("$path.trigger", trigger, "Native self-entry placeholder can also dispatch on leveling up")
            ability.interveningIf?.let { condition ->
                recordState(condition,"$path.interveningIf","$path.effect",ability.effect,null,StateCheckpoint.TRIGGER_AND_RESOLUTION,
                    Frame(ability.allTargetRequirements,assumptions=assumptions,selfZone=sourceZone,event=event),
                    ability.triggerRestriction==null && ability.elseEffect==null && !ability.controlledByTriggeringEntityController &&
                        ability.binding!=TriggerBinding.ATTACHED && ability.activeZones==setOf(Zone.BATTLEFIELD) && extra.isEmpty())
            }
            if(trigger is EventPattern.ZoneChangeEvent && trigger.from == Zone.BATTLEFIELD && trigger.to == Zone.GRAVEYARD) {
                var eventScope=ObjectScope(trigger.filter)
                if(ability.binding == TriggerBinding.SELF) eventScope=eventScope.copy(actor="YOU",printedSelfCreature=creature)
                if(ability.binding == TriggerBinding.ATTACHED) eventScope=eventScope.copy(unresolved="Attached death-event binding not resolved against aura/attachment restrictions")
                if(ability.controlledByTriggeringEntityController) eventScope=eventScope.copy(unresolved="Trigger controller rebound to event entity")
                if(trigger.requireEmergeSacrifice) eventScope=eventScope.copy(unresolved="Native emerge-sacrifice trigger restriction is not interpreted")
                removals(ability.effect).forEach { enhanced("$path.trigger",trigger.toString(),Verdict.MATCH to eventScope,it,Frame(ability.allTargetRequirements,assumptions=assumptions,selfZone=sourceZone),Verdict.MATCH) }
            }
            ability.elseEffect?.let { walk(it,"$path.elseEffect",Frame(ability.allTargetRequirements,assumptions=assumptions+"Intervening condition failed",selfZone=sourceZone,event=event,outcomeGuard=Verdict.UNKNOWN,returnSource=returnSource,returnGuard=Verdict.UNKNOWN,classAvailability=availability)) }
        }
        script.activatedAbilities.forEachIndexed { i,a -> activated(a,"script.activatedAbilities[$i]") }
        script.triggeredAbilities.forEachIndexed { i,a -> triggered(a,"script.triggeredAbilities[$i]") }
        script.stateTriggeredAbilities.forEachIndexed { i,a -> action("script.stateTriggeredAbilities[$i]",
            nativeJson.encodeToJsonElement(StateTriggeredAbility.serializer(),a),a.effect,emptyList(),listOf("State trigger condition=${a.condition}; zone=${a.activeZone}")) }
        script.sagaChapters.forEachIndexed { i,a -> action("script.sagaChapters[$i]",nativeJson.encodeToJsonElement(SagaChapterAbility.serializer(),a),a.effect,
            listOfNotNull(a.targetRequirement)+a.additionalTargetRequirements,listOf("Saga chapter ${a.chapter}")) }
        script.classLevels.forEachIndexed { i,level ->
            val availability = classAvailability(script.classLevels, i, isClass)
            val path = availability.levelPath
            level.activatedAbilities.forEachIndexed { j,a -> activated(a,"$path.activatedAbilities[$j]",availability) }
            level.triggeredAbilities.forEachIndexed { j,a -> triggered(a,"$path.triggeredAbilities[$j]",availability) }
            level.staticAbilities.forEachIndexed { j,a ->
                retainClassAbility("$path.staticAbilities[$j]", a, nativeJson.encodeToJsonElement(StaticAbility.serializer(),a), availability)
            }
            level.replacementEffects.forEachIndexed { j,a ->
                val child = "$path.replacementEffects[$j]"
                if (inspectCounters) recordCounterReplacement(child, a, availability)
                else retainClassAbility(child, a, nativeJson.encodeToJsonElement(ReplacementEffect.serializer(),a), availability)
            }
        }
        // Self-cast static discounts are intra-card dependencies; deck-wide discounts are out of scope.
        script.staticAbilities.forEachIndexed { i,a ->
            actionId="${card.name}|$label|script.staticAbilities[$i]"
            actionKind="STATIC"; actionNative=nativeJson.encodeToJsonElement(StaticAbility.serializer(),a)
            actions += ActionEvidence(actionId,card.name,label,"script.staticAbilities[$i]",emptyList(),actionNative)
            if(a is MayCastSelfFromZones) facts += MechanicalFact("$actionId|CAST_PERMISSION",card.name,face,actionId,"script.staticAbilities[$i]",
                FactKind.CAST_PERMISSION,"CAST_FROM_ZONE","PERMISSION",GameObjectFilter.Any,confidence=Verdict.UNKNOWN,
                prerequisites=listOf("Native casting permission only; costs/timing/condition remain: $a"),native=actionNative)
            if(a is ModifySpellCost && a.target == SpellCostTarget.SelfCast && a.modification is CostModification.ReduceGenericBy) {
                val source=(a.modification as CostModification.ReduceGenericBy).source
                if(source is CostReductionSource.FixedIfCreatureDiedThisTurn && source.amount > 0) {
                    removals(script.spellEffect).forEach { enhanced("script.staticAbilities[$i]","A creature died this turn reduces this spell's generic cost by ${source.amount}",
                        deathDependency(CreatureDiedThisTurnCondition),it,Frame(script.targetRequirements,assumptions=listOf("Cost gating=${a.gating}")),Verdict.MATCH) }
                } else issue("script.staticAbilities[$i]",a,"Static cost dependency not interpreted")
            } else issue("script.staticAbilities[$i]",a,"Static ability not interpreted for this objective")
            val wrapper = a as? ConditionalStaticAbility
            val grant = wrapper?.ability ?: a
            val staticPath = "script.staticAbilities[$i]"
            val conditional = wrapper?.takeIf { grant is GrantActivatedAbility || grant is GrantTriggeredAbility }
                ?.let { conditionalGrantEvidence(staticPath, it.condition, grant) }
            val grantPath = conditional?.innerPath ?: staticPath
            if(grant is GrantActivatedAbility) {
                val staticIndex=actions.indexOfLast { it.id==actionId }
                val declarationAction=actions[staticIndex]
                val problems=grantRecipientProblems(card,label,grant,conditional!=null)
                // Keep this additive shared evidence independent of the requesting objective's
                // optional projections. Complex counter/state bodies retain native uncertainty.
                val body=PathwayExtractor()
                    .grantedBody(card,label,"$grantPath.ability",grant,declarationAction.id,conditional)
                actions[staticIndex]=declarationAction.copy(grantedAbilities=listOf(GrantedAbility(
                    "${declarationAction.id}|${if (conditional == null) "GRANT" else "CONDITIONAL_GRANT"}",declarationAction.id,card.name,label,grantPath,i,
                    grantRecipientKind(card,grant.filter,conditional!=null),grant,if(card.isAura) "${card.name}|$label|script" else null,
                    if(problems.isEmpty()) Verdict.MATCH else Verdict.UNKNOWN,problems,
                    listOf("Active native granter and eligible current recipient; no recipient, payment, activation or outcome guaranteed",
                        "Availability uses granter-relative filters; the granted body uses recipient Self and activator-relative YOU"),body,conditional=conditional)))
            }
            if(grant is GrantTriggeredAbility) {
                val index=actions.indexOfLast { it.id==actionId };val parent=actions[index]
                val definition=grantFace(card,label);val problems=triggerRecipientProblems(definition,grant,conditional!=null)
                val nested=PathwayExtractor().grantedTriggerBody(card,label,"$grantPath.ability",grant,parent.id,definition,problems.isEmpty(),conditional)
                actions[index]=parent.copy(grantedTriggers=listOf(GrantedTrigger("${parent.id}|${if (conditional == null) "TRIGGER_GRANT" else "CONDITIONAL_TRIGGER_GRANT"}",parent.id,card.name,label,definition?.name,
                    grantPath,i,grant,definition?.let { grantRecipientKind(it,grant.filter,conditional!=null) } ?: GrantRecipientKind.UNRESOLVED,
                    if(problems.isEmpty()) Verdict.MATCH else Verdict.UNKNOWN,grantedEventSupport(grant.ability,conditional!=null && grant.filter == GroupFilter.source()),problems,
                    if(definition?.isAura==true) "${card.name}|$label|script" else null,nested.first,nested.second,conditional=conditional)))
            }
        }
        actionId="${card.name}|$label|script.unsupported"
        val scriptTree=nativeJson.encodeToJsonElement(CardScript.serializer(),script).jsonObject
        val handled=setOf("spellEffect","additionalCosts","targetRequirements","activatedAbilities","triggeredAbilities","stateTriggeredAbilities","sagaChapters","classLevels","staticAbilities","selfAlternativeCost")
        val remainder=JsonObject(scriptTree.filterKeys { it !in handled && !(inspectCounters && it == "replacementEffects") })
        if(remainder.isNotEmpty()) {
            actions += ActionEvidence(actionId,card.name,label,"script.unsupported",emptyList(),remainder)
            remainder.forEach { (key,_) -> issue("script.$key",script,"Additional script field retained but not interpreted") }
        }
        if(keywordAbilities.isNotEmpty()) {
            val id="${card.name}|$label|keywordAbilities"; actionId=id
            actions += ActionEvidence(id,card.name,label,"keywordAbilities",emptyList(),JsonArray(keywordAbilities.map { nativeJson.encodeToJsonElement(KeywordAbility.serializer(),it) }))
            keywordAbilities.forEachIndexed { i,k ->
                val path="keywordAbilities[$i]"
                val cost=when(k) { is KeywordAbility.Flashback -> k.additionalCost; is KeywordAbility.OptionalAdditionalCost -> k.additionalCost; else -> null }
                if(k is KeywordAbility.Flashback || k is KeywordAbility.OptionalAdditionalCost) {
                    actionId="${card.name}|$face|$path"; rootEffect=script.spellEffect; actionKind="SPELL"
                    actionNative=nativeJson.encodeToJsonElement(KeywordAbility.serializer(),k)
                    val assumptions=when(k) {
                        is KeywordAbility.Flashback -> listOf("Flashback route: cast this card from GRAVEYARD; mana=${k.cost}; condition=${k.condition}; minimumX=${k.minimumXValue}; exile if it leaves stack; spell timing and legal targets still required")
                        is KeywordAbility.OptionalAdditionalCost -> listOf("Optional additional casting route chosen: mana=${k.manaCost}; multi=${k.multi}; native choice slot and conditions retained")
                        else -> emptyList()
                    }
                    actions += ActionEvidence(actionId,card.name,face,path,assumptions,actionNative,"${card.name}|$face|script")
                    cost?.let { castingCost(it,"$path.additionalCost",Frame(script.targetRequirements,assumptions=assumptions,optional=true),
                        if(k is KeywordAbility.Flashback) "ALTERNATIVE_CASTING_COST" else "OPTIONAL_CASTING_COST") }
                    support(path,k,if(cost==null || unsupported.none { it.actionId==actionId }) SupportStatus.SUPPORTED else SupportStatus.PARTIALLY_SUPPORTED,
                        "Casting cost route only; associated native script supplies effect/targets. Keyword timing/lowering/execution not exhaustively interpreted")
                } else issue(path,k,"Keyword-granted actions are not expanded")
            }
        }
        val deferred = deferredMovementLinks(card.name,label,script,facts,linkedExileProducers) + durationMovementLinks(card.name,label,script)
        if (deferred.isNotEmpty()) actions[attachmentRootIndex] = actions[attachmentRootIndex].copy(linkedMovements=deferred)
        if (aura || script.auraTarget != null) {
            val root = actions[attachmentRootIndex]
            actions[attachmentRootIndex] = root.copy(
                targetSelection = script.auraTarget?.let {
                    TargetSelection(root.id, listOf(it), origin = TargetRequirementOrigin.AURA_ENCHANT)
                },
                attachment = auraAttachment(root, script,
                eventOutcomes.subList(attachmentOutcomeStart, eventOutcomes.size)))
        }
    }
    /** Interpret one native granted declaration with a receiving source, using this same walker.
     * Its output stays inside the grant relationship and never enters intrinsic objective inputs. */
    internal fun grantedBody(origin: CardDefinition,label: String,path: String,grant: GrantActivatedAbility,parent: String, conditional: ConditionalGrant? = null): GrantedBody {
        card=origin;face=label;selfCreature=false;grantedSource=true
        actionId="${origin.name}|$label|$path";actionKind="GRANTED_ACTIVATED";rootEffect=grant.ability.effect
        actionNative=nativeJson.encodeToJsonElement(ActivatedAbility.serializer(),grant.ability)
        val assumptions=listOf("Granted source is the receiving object; granting card supplies provenance only",
            "YOU/Controller and player resources belong to the activator, not necessarily the granter's controller",
            "Current recipient and native availability, activation costs, timing and legal targets required",
            "Timing=${grant.ability.timing}; restrictions=${grant.ability.restrictions}; mana=${grant.ability.isManaAbility}") +
            listOfNotNull(conditional?.let { "Native grant condition at ${it.wrapperPath}.condition governs activation availability" })
        actions+=ActionEvidence(actionId,origin.name,label,path,assumptions,actionNative,associatedActionId=parent,
            targetSelection=grant.ability.targetRequirements.takeIf { it.isNotEmpty() }?.let { TargetSelection(actionId,it,TargetCheckpoint.ACTIVATION_ANNOUNCEMENT,TargetRequirementOrigin.GRANTED_ACTIVATED) },
            unionTargets=unionTargets(actionId,grant.ability.targetRequirements,"ACTIVATION_ANNOUNCEMENT"))
        val recipientSupported = grantRecipientProblems(origin, label, grant, conditional!=null).isEmpty()
        val receiver=ObjectScope(filter=grantBodyRecipientFilter(grant.filter, recipientSupported),
            actor="YOU",printedSelfCreature=null,battlefield=true,zone=grant.recipientZone,
            details=listOf("Current recipient of $parent; printed characteristics of the granting card are not recipient characteristics"),
            unresolved=if(recipientSupported) null else "Recipient characteristics/binding unresolved")
        val frame=Frame(grant.ability.targetRequirements,receivingSource=receiver,selfZone=grant.ability.activateFromZone,assumptions=assumptions)
        abilityCost(grant.ability.cost,"$path.cost",frame)
        walk(grant.ability.effect,"$path.effect",frame)
        return GrantedBody(actions.first(),facts.toList(),unsupported.toList(),structuralSupport.toList())
    }

    internal fun grantedTriggerBody(origin: CardDefinition,label: String,path: String,grant: GrantTriggeredAbility,
        parent: String,definition: CardDefinition?,recipientKnown: Boolean, conditional: ConditionalGrant? = null): Pair<GrantedBody,List<EventOutcome>> {
        card=origin;face=label;selfCreature=false;grantedSource=true
        val ability=grant.ability;actionId="${origin.name}|$label|$path";actionKind="GRANTED_TRIGGERED";rootEffect=ability.effect
        actionNative=nativeJson.encodeToJsonElement(TriggeredAbility.serializer(),ability)
        val targets=listOfNotNull(ability.targetRequirement)+ability.additionalTargetRequirements
        val assumptions=listOf("Triggered source Self is the receiving object; GrantingSource is the distinct native granter",
            "YOU/Controller refer to the captured native trigger controller, not automatically the granter or current host controller",
            "Native grant, qualifying event and detection required; no occurrence or resolution implied",
            "triggerRestriction=${ability.triggerRestriction} at detection; interveningIf=${ability.interveningIf} at detection AND resolution",
            "Targets/chooser at stack placement; effect gates/choices at resolution; cap=${ability.perTurnTriggerCap}; lifetime=${ability.triggersOnce}") +
            listOfNotNull(conditional?.let { "Native grant condition at ${it.wrapperPath}.condition is checked at ${it.checkpoint}" })
        actions+=ActionEvidence(actionId,origin.name,label,path,assumptions,actionNative,associatedActionId=parent,
            targetSelection=targets.takeIf { it.isNotEmpty() }?.let { TargetSelection(actionId,it,TargetCheckpoint.TRIGGER_STACK_ANNOUNCEMENT,TargetRequirementOrigin.GRANTED_TRIGGERED) },
            unionTargets=unionTargets(actionId,targets,"TRIGGER_STACK_ANNOUNCEMENT"))
        val receiver=ObjectScope(filter=grantBodyRecipientFilter(grant.filter, recipientKnown),
            actor="YOU",printedSelfCreature=null,battlefield=true,zone=Zone.BATTLEFIELD,
            details=listOf("Receiving object of $parent; printed granter characteristics are not source characteristics"),
            unresolved=if(recipientKnown) null else "Recipient source binding unresolved")
        val provider=ObjectScope(filter=when { definition?.isEquipment==true -> GameObjectFilter.Artifact; definition?.isAura==true -> GameObjectFilter.Enchantment; else -> GameObjectFilter.Any },
            actor="GRANTER_CONTROLLER",printedSelfCreature=definition?.isCreature,battlefield=true,zone=Zone.BATTLEFIELD,
            details=listOf("Native granterId of $parent, distinct from receiving source"))
        val conditionalSelf = conditional != null && grant.filter == GroupFilter.source()
        val confidence = if (recipientKnown && grantedEventSupport(ability, conditionalSelf) == Verdict.MATCH &&
            conditional?.support != Verdict.UNKNOWN && ability.triggerRestriction == null && ability.interveningIf == null
        ) Verdict.MATCH else Verdict.UNKNOWN
        val recipientIsCreature = CardPredicate.IsCreature in receiver.filter.cardPredicates
        val event=EventContext(ability.trigger,ability.binding,ability.activeZones,recipientIsCreature,actionNative,confidence,assumptions)
        val frame=Frame(targets,receivingSource=receiver,grantingSource=provider,event=event,assumptions=assumptions)
        walk(ability.effect,"$path.effect",frame)
        ability.elseEffect?.let { walk(it,"$path.elseEffect",frame.copy(rootTargetSelection=false,outcomeGuard=Verdict.UNKNOWN,
            assumptions=assumptions+"Announcement-time target selection produced nothing; alternate effect")) }
        return GrantedBody(actions.first(),facts.toList(),unsupported.toList(),structuralSupport.toList(),controller="TRIGGER_CONTROLLER",
            playerReferences="YOU/Controller are the native controller captured when the trigger fired; Self/source characteristics use the recipient; GrantingSource uses the recorded granter.") to eventOutcomes.toList()
    }

    private fun sourceCardCreature(): Boolean? = if(grantedSource) null else selfCreature.takeIf {
        card.layout == CardLayout.NORMAL && card.backFace == null && card.flipSide == null && card.cardFaces.isEmpty()
    }

    private fun selfSacrifice(path: String, origin: String, instruction: Any, native: JsonElement, frame: Frame,
                              supportedArrival: Boolean, incidental: Boolean = false) {
        val entry = if (inspectCardEntries && supportedArrival && frame.selfZone == Zone.BATTLEFIELD) {
            val arrivalType = when (sourceCardCreature()) {
                true -> Verdict.MATCH
                false -> Verdict.NO_MATCH
                null -> Verdict.UNKNOWN
            }
            support(path,instruction,if(arrivalType == Verdict.UNKNOWN) SupportStatus.PARTIALLY_SUPPORTED else SupportStatus.SUPPORTED,
                "Exact source permanent and owner destination retained; graveyard card type=$arrivalType")
            CardEntry(GraveyardRoute.SACRIFICE,1,sourceCreatureAtArrival=arrivalType)
        } else null
        val scope = targetScope(EffectTarget.Self,frame,selfCreature)
        add(path,"SACRIFICE",origin,scope,native,frame,incidental,sourceEntry=entry)
    }

    private fun abilityCost(c: AbilityCost,path: String,frame: Frame) {
        when(c) {
            is AbilityCost.Composite -> c.costs.forEachIndexed { i,child -> abilityCost(child,"$path.costs[$i]",frame) }
            is AbilityCost.Atom -> atom(c.atom,"$path.atom","ACTIVATION_COST",frame)
            AbilityCost.DiscardSelf -> {
                if (!inspectCardEntries || frame.selfZone != Zone.HAND) {
                    issue(path,c,"Activation cost not interpreted")
                    return
                }
                val arrivalCreature = sourceCardCreature()
                val source = ObjectScope(battlefield = false, zone = Zone.HAND, zonePlayer = frame.actor,
                    ownerScope = frame.actor, printedSelfCreature = arrivalCreature,
                    unresolved = if (arrivalCreature != null) null else "Source card characteristics in the graveyard need face-specific interpretation")
                recordMovement(path,nativeJson.encodeToJsonElement(AbilityCost.serializer(),c),source,
                    Zone.GRAVEYARD,"OWNER",frame,binding="SELF",selection="Exactly the source card; no discard choice",
                    role="ACTIVATION_COST",cardEntry=CardEntry(GraveyardRoute.DISCARD,1,discardsSource=true))
            }
            AbilityCost.SacrificeSelf -> selfSacrifice(path,"ACTIVATION_COST",c,nativeJson.encodeToJsonElement(AbilityCost.serializer(),c),frame,
                supportedArrival=true)
            AbilityCost.Free, AbilityCost.Tap, AbilityCost.Untap, is AbilityCost.Loyalty -> Unit
            else -> issue(path,c,"Activation cost not interpreted")
        }
    }
    private fun castingCost(c: AdditionalCost,path: String,frame: Frame,origin: String = "CASTING_COST") {
        when(c) {
            is AdditionalCost.Atom -> atom(c.atom,"$path.atom",origin,frame)
            is AdditionalCost.Choice -> {
                support(path,c,SupportStatus.SUPPORTED,"Exactly one additional-cost branch paid; alternatives are not cumulative; slot=${c.choiceSlot}")
                c.options.forEachIndexed { i,child -> castingCost(child,"$path.options[$i]",frame.copy(assumptions=frame.assumptions+"Additional-cost choice $i selected; other branches not paid; slot=${c.choiceSlot}"),origin) }
            }
            is AdditionalCost.OrPay -> {
                support(path,c,SupportStatus.SUPPORTED,"Non-mana cost OR extra mana; mana-only branch has no sacrifice")
                castingCost(c.cost,"$path.cost",frame.copy(assumptions=frame.assumptions+"Pay native non-mana branch rather than extra ${c.alternativeManaCost}"),origin)
            }
            is AdditionalCost.Composite -> c.steps.forEachIndexed { i,child -> castingCost(child,"$path.steps[$i]",frame,origin) }
            is AdditionalCost.AnyNumberForCostReduction -> if(c.action == PermanentCostAction.SACRIFICE)
                add(path,"SACRIFICE",origin,ObjectScope(c.filter,"YOU"),nativeJson.encodeToJsonElement(AdditionalCost.serializer(),c),frame.copy(assumptions=frame.assumptions+"Optional any-number sacrifice")) else Unit
            else -> issue(path,c,"Casting cost not interpreted")
        }
    }
    private fun atom(c: CostAtom,path: String,origin: String,frame: Frame) {
        if (inspectCardEntries && (c is CostAtom.Discard || c is CostAtom.Mill)) {
            val discard = c as? CostAtom.Discard
            val route = if (discard == null) GraveyardRoute.MILL else GraveyardRoute.DISCARD
            val count = discard?.count ?: (c as CostAtom.Mill).count
            val zone = if (discard == null) Zone.LIBRARY else Zone.HAND
            recordMovement(path,nativeJson.encodeToJsonElement(CostAtom.serializer(),c),
                ObjectScope(discard?.filter ?: GameObjectFilter.Any,battlefield=false,zone=zone,zonePlayer=frame.actor,ownerScope=frame.actor),
                Zone.GRAVEYARD,"OWNER",frame,selection=c.description,confidence=if(count>0) Verdict.MATCH else Verdict.NO_MATCH,
                role=origin,cardEntry=CardEntry(route,count,random=discard?.random == true))
            support(path,c,SupportStatus.SUPPORTED,"Native fixed-count graveyard entry payment; actual cards and destination remain conditional")
            return
        }
        val filter=when(c) {
            is CostAtom.Sacrifice -> c.filter
            is CostAtom.SacrificeAll -> c.filter
            is CostAtom.VariablePermanents -> if(c.action == PermanentCostAction.SACRIFICE) c.filter else null
            else -> null
        }
        if(filter != null) {
            val constraints=when(c) {
                is CostAtom.Sacrifice -> listOf("Sacrifice count=${c.count}; each qualifying creature produces its own death event if not replaced; excludeSelf=${c.excludeSelf}; distinctNames=${c.distinctNames}; all required objects must be payable")
                is CostAtom.SacrificeAll -> listOf("Sacrifice all matching permanents; at least one qualifying object must exist for a death")
                else -> listOf("Variable sacrifice count requires a positive payable choice")
            }
            val zero=c is CostAtom.Sacrifice && c.count<=0
            add(path,"SACRIFICE",origin,ObjectScope(filter,frame.actor,battlefield=!zero),nativeJson.encodeToJsonElement(CostAtom.serializer(),c),frame.copy(assumptions=frame.assumptions+constraints),
                sacrificeChoice=(c as? CostAtom.Sacrifice)?.takeIf { it.count > 0 }?.let { SacrificeChoice(it.count,it.excludeSelf) })
            support(path,c,SupportStatus.SUPPORTED,"Native sacrifice cost restrictions/count retained; per-object death events conditional on payment and no replacement")
        }
        else if(c !is CostAtom.Mana && c !is CostAtom.PayLife && c !is CostAtom.TapPermanents && c !is CostAtom.Discard && c !is CostAtom.ExileFrom)
            issue(path,c,"Cost atom not interpreted")
    }
    private fun payCost(c: PayCost,path: String,frame: Frame) {
        when(c) {
            is PayCost.Atom -> atom(c.atom,"$path.atom","RESOLUTION_COST",frame)
            is PayCost.Choice -> c.options.forEachIndexed { i,child -> payCost(child,"$path.options[$i]",frame.copy(assumptions=frame.assumptions+"Payment alternative $i chosen")) }
            PayCost.OwnManaCost, is PayCost.DynamicLife -> Unit
        }
    }
    private fun recordMovement(path: String,native: JsonElement,obj: ObjectScope,destination: Zone?,controller: String,frame: Frame,
        binding: String?=null,selection: String="Native target requirement",extra: List<String> = emptyList(),confidence: Verdict=Verdict.MATCH,
        role: String="EFFECT",cardEntry: CardEntry?=null) {
        val problems=listOfNotNull(obj.unresolved)+extra
        val filter=if(obj.printedSelfCreature==true) intersect(obj.filter,GameObjectFilter.Creature) else if(obj.printedSelfCreature==false) intersect(obj.filter,GameObjectFilter.Any.copy(cardPredicates=listOf(com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNoncreature))) else obj.filter
        facts += MechanicalFact("$actionId|$path|MOVE_OBJECT|${facts.count { it.actionId==actionId && it.path==path && it.kind==FactKind.MOVE_OBJECT }}",
            card.name,face,actionId,path,FactKind.MOVE_OBJECT,cardEntry?.route?.name ?: "MOVE_OBJECT",role,filter,obj.zone,obj.zonePlayer,destination,controller,
            all(listOf(confidence,frame.guardConfidence,frame.factGuard,if((actionNative as? JsonObject)?.get("controlledByTriggeringEntityController")==JsonPrimitive(true)) Verdict.UNKNOWN else Verdict.MATCH,if(obj.possible) Verdict.MATCH else Verdict.NO_MATCH)),frame.assumptions+"Only actual successful movement; legal choices/resources and no replacing effects assumed"+obj.details,
            native,binding,selection,circumstances(role,obj,frame),problems,ownerScope=obj.ownerScope,cardEntry=cardEntry)
        val f=facts.last()
        if(binding=="Self" || f.sourceZone==Zone.GRAVEYARD && f.destinationZone==Zone.BATTLEFIELD) {
            val movement=native as? JsonObject
            val limits=frame.returnLimits+sourceReplacementLimits
            selfReturns+=SelfReturnLink(f.id,card.name,face,actionId,path,
                when(sourceCardCreature()) { true->Verdict.MATCH;false->Verdict.NO_MATCH;null->Verdict.UNKNOWN },
                if(binding=="Self" && movement?.get("target")?.jsonObject?.get("type")?.jsonPrimitive?.content=="Self") Verdict.MATCH else Verdict.UNKNOWN,
                frame.returnSource ?: ReturnSourceContext(actionKind,emptySet(),"Native action $actionId; source activation/trigger context not established",confidence=Verdict.UNKNOWN),
                all(listOf(frame.returnGuard,frame.factGuard,frame.guardConfidence)),frame.optional,frame.delayed,
                frame.assumptions,limits,sourceZone=f.sourceZone,destinationZone=f.destinationZone,sourcePlayer=f.sourcePlayer,destinationController=f.destinationController)
        }
    }
    private fun recordTargetMove(e: MoveToZoneEffect,path: String,frame: Frame) {
        val requirement=when(val t=e.target) { is EffectTarget.ContextTarget->frame.targets.getOrNull(t.index);is EffectTarget.BoundVariable->frame.targets.find { it.id==t.name };else->null }
        val alternatives=(requirement as? TargetObject)?.filter?.clauses()
        val bindingKey=requirement?.id?.let { "target:$it" } ?: e.target.toString()
        val previous=frame.movementTargets[bindingKey]
        val firstFact=facts.size
        val objects=previous?.scopes ?: alternatives?.map { scope(it) } ?: listOf(targetScope(e.target,frame,selfCreature))
        objects.forEach { raw ->
            val incompatible=e.fromZone!=null && raw.zone!=null && e.fromZone!=raw.zone
            val obj=raw.copy(zone=e.fromZone ?: raw.zone,zonePlayer=if(e.target==EffectTarget.Self && actionKind=="ACTIVATED" && frame.selfZone==Zone.GRAVEYARD) "YOU" else raw.zonePlayer)
            val controller=if(e.controllerOverride==null) "OWNER" else playerScope(e.controllerOverride!!,frame)
            recordMovement(path,e.tree(),obj,e.destination,controller,frame.copy(returnLimits=frame.returnLimits+
                if(e.entryCounters.any { it.counterType==com.wingedsheep.sdk.core.CounterType.FINALITY }) listOf(ReturnLimit("DEATH_REPLACEMENT",path,"A finality entry counter exiles this creature instead of a later death while present; counter removal is not assumed.",e.tree())) else emptyList()),binding=e.target.toString(),
                selection=requirement?.let { "targeted=true; count=${it.count}; min=${it.minCount}; optional=${it.optional}; chooser=${it.chooser}; native=$it" } ?: "Binding=${e.target}; selection not inferred as targeting",
                confidence=if(incompatible || requirement?.count==0) Verdict.NO_MATCH else Verdict.MATCH,
                extra=listOfNotNull(if(e.faceDown!=null) "Face-down arrival unresolved" else null))
        }
        // The same bound target is not still in its original zone after an earlier move.
        // Model only successful arrivals; actual replacement/failure remains an assumption.
        val actionIndex=actions.indexOfLast { it.id==actionId }
        val selectedRequirementIndex=when(val target=e.target) {
            is EffectTarget.ContextTarget -> target.index
            is EffectTarget.BoundVariable -> frame.targets.indices.filter { frame.targets[it].id==target.name }.singleOrNull()
            else -> null
        }
        if(actionIndex>=0 && frame.rootTargetSelection) actions[actionIndex]=actions[actionIndex].copy(unionTargets=actions[actionIndex].unionTargets.map { u ->
            if(u.requirementIndex==selectedRequirementIndex && u.requirement===requirement) u.copy(operationPaths=u.operationPaths+path,movementFactIds=u.movementFactIds+facts.subList(firstFact,facts.size).map { it.id }) else u
        })
        val step=MovementStep(actionId,path,facts.subList(firstFact,facts.size).map { it.id },
            e.fromZone ?: objects.map { it.zone }.distinct().singleOrNull(),e.destination,
            if(e.controllerOverride==null) "OWNER" else playerScope(e.controllerOverride!!,frame),e.tree())
        if(previous!=null) {
            val link=immediateMovementLink(card.name,face,bindingKey,previous.step,step,
                requirement is TargetObject && requirement.count==1 && requirement.minCount==1 && !requirement.optional &&
                    frame.targets.count { it.id==requirement.id }==1,
                frame.optional || frame.delayed || frame.factGuard!=Verdict.MATCH || frame.guardConfidence!=Verdict.MATCH ||
                    frame.returnGuard!=Verdict.MATCH || e.faceDown!=null || e.byDestruction || !previous.departureSupported,frame.assumptions)
            val index=actions.indexOfLast { it.id==actionId }
            actions[index]=actions[index].copy(linkedMovements=actions[index].linkedMovements+link)
        }
        frame.movementTargets[bindingKey]=TargetMovement(objects.map { obj -> obj.copy(zone=e.destination,
            zonePlayer=if(e.destination==Zone.BATTLEFIELD) "UNKNOWN" else "ANY",
            battlefield=e.destination==Zone.BATTLEFIELD,
            unresolved=listOfNotNull(obj.unresolved,"Later reference requires the same tracked card and successful earlier movement; original targeting restrictions are retained").joinToString("; ")) },step,
            objects.all { it.unresolved==null && it.possible } &&
                facts.subList(firstFact,facts.size).all { it.confidence==Verdict.MATCH && it.unresolved.isEmpty() } && !e.byDestruction && e.faceDown==null &&
                frame.factGuard==Verdict.MATCH && frame.guardConfidence==Verdict.MATCH && !frame.delayed && !frame.optional)
    }
    /** Called only by the shared native effect walk; token facts do not project death pathways. */
    private fun recordToken(e: Effect,path: String,frame: Frame): Boolean {
        var receiver: EffectTarget? = null
        var amount: DynamicAmount = DynamicAmount.Fixed(1)
        var creature = Verdict.UNKNOWN
        var template = e.javaClass.simpleName
        var copySource: String? = null
        var unresolved = emptyList<String>()
        var characteristics = emptyList<String>()
        fun copiedType(ex: CopyExceptions,base: Verdict): Verdict = ex.overrideCardTypes?.let {
            if(com.wingedsheep.sdk.core.CardType.CREATURE in it) Verdict.MATCH else Verdict.NO_MATCH
        } ?: if(com.wingedsheep.sdk.core.CardType.CREATURE in ex.addedCardTypes) Verdict.MATCH else base
        fun explicitCreature(filter: GameObjectFilter): Verdict {
            val ps=filter.cardPredicates
            val yes=ps.any { it==com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature }
            val no=ps.any { it==com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNoncreature }
            val own=when { yes && no -> Verdict.UNKNOWN;yes -> Verdict.MATCH;no -> Verdict.NO_MATCH;else -> Verdict.UNKNOWN }
            if(filter.anyOf.isEmpty()) return own
            val bs=filter.anyOf.map(::explicitCreature)
            return if(own!=Verdict.UNKNOWN) own else if(bs.all { it==Verdict.MATCH }) Verdict.MATCH else if(bs.all { it==Verdict.NO_MATCH }) Verdict.NO_MATCH else Verdict.UNKNOWN
        }
        when(e) {
            is CreateTokenEffect -> {
                receiver=e.controller;amount=e.count;creature=Verdict.MATCH
                template="Native creature template: ${e.name ?: e.creatureTypes.joinToString()}"
                characteristics=listOf("Creature", "colors=${e.colors}","subtypes=${e.creatureTypes}","P/T=${e.dynamicPower ?: e.power}/${e.dynamicToughness ?: e.toughness}","entry/delayed riders and granted abilities retained in native node")
            }
            is CreatePredefinedTokenEffect -> {
                receiver=e.controller;amount=e.dynamicCount ?: DynamicAmount.Fixed(e.count)
                val def=com.wingedsheep.mtg.sets.tokens.PredefinedTokens.allTokens.firstOrNull { it.name==e.tokenType }
                creature=def?.let { if(it.isCreature) Verdict.MATCH else Verdict.NO_MATCH } ?: Verdict.UNKNOWN
                template="PredefinedTokens:${e.tokenType}"
                characteristics=def?.let { listOf("Native type line=${it.typeLine}","Native template=${it.name}") } ?: emptyList()
                if(def==null) unresolved=listOf("Unregistered predefined token template ${e.tokenType}")
            }
            is CreateTokenCopyOfSourceEffect -> {
                amount=DynamicAmount.Fixed(e.count);copySource="Self copiable values"
                creature=copiedType(e.copyExceptions,if(grantedSource) Verdict.UNKNOWN else if(selfCreature) Verdict.MATCH else Verdict.NO_MATCH)
                template="Native source copy; merged exceptions=${e.copyExceptions}"
            }
            is CreateTokenCopyOfTargetEffect -> {
                receiver=e.controller;amount=e.count;copySource=e.target.toString()
                val requirement=when(val t=e.target) { is EffectTarget.ContextTarget->frame.targets.getOrNull(t.index);is EffectTarget.BoundVariable->frame.targets.find { it.id==t.name };else->null }
                val moved=frame.movementTargets[requirement?.id?.let { "target:$it" } ?: e.target.toString()]
                val obj=if(moved!=null) ObjectScope(unresolved="Copy source was moved earlier; last known copiable characteristics unresolved") else targetScope(e.target,frame,selfCreature)
                creature=copiedType(e.copyExceptions,if(obj.printedSelfCreature!=null) { if(obj.printedSelfCreature) Verdict.MATCH else Verdict.NO_MATCH } else explicitCreature(obj.filter))
                val ps=obj.filter.cardPredicates
                val simple=ps.all { it in setOf(com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature,com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNoncreature,com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsPermanent,com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsArtifact,com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsToken,com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNontoken) }
                val contradiction=ps.contains(com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature) && ps.contains(com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNoncreature) || ps.contains(com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsToken) && ps.contains(com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNontoken)
                unresolved=listOfNotNull(obj.unresolved,if(!obj.possible || requirement?.count==0) "Copy selection is impossible or zero-sized" else null,
                    if(!simple || contradiction || obj.filter.anyOf.isNotEmpty() || obj.filter.statePredicates.isNotEmpty()) "Complex/contradictory copy-source selection restrictions require correlated interpretation" else null)
                template="Native target copy; merged exceptions=${e.copyExceptions}"
                characteristics=listOf("Copy source filter=${obj.filter}; zone=${obj.zone}; source controller=${obj.actor}")
            }
            else -> return false
        }
        val controller=receiver?.let { if(it is EffectTarget.PlayerRef) relativeActor(it.player,frame) else playerScope(it,frame) } ?: frame.tokenController ?: frame.actor
        val positive=tokenQuantityPotential(amount)
        val confidence=all(listOf(frame.guardConfidence,frame.factGuard,
            if((actionNative as? JsonObject)?.get("controlledByTriggeringEntityController")==JsonPrimitive(true)) Verdict.UNKNOWN else Verdict.MATCH))
        val prerequisites=frame.assumptions+listOf("Native action costs, choices, timing and legal resources must be satisfied; source face must be available",
            "Quantity must evaluate strictly positive at resolution: $amount; a conditional capability is not a guaranteed outcome",
            "Native token template/copiable values, entry/delayed riders and replacement effects apply; no gameplay outcome is guaranteed")+
            if(copySource!=null) listOf("Copy requires a legal available source with appropriate copiable characteristics, not merely a temporary animation") else emptyList()
        val detail=TokenCreation(creature,amount,positive,template,copySource,frame.tokenEvent ?: path,characteristics)
        facts+=MechanicalFact("$actionId|$path|CREATE_TOKEN",card.name,face,actionId,path,FactKind.CREATE_TOKEN,"CREATE_TOKEN","EFFECT",
            GameObjectFilter.Any,destinationZone=Zone.BATTLEFIELD,destinationController=controller,confidence=confidence,
            prerequisites=prerequisites,native=e.tree(),binding=copySource,selection="One correlated native token template/group; quantity=$amount",
            circumstances=circumstances("EFFECT",ObjectScope(battlefield=false),frame).let { if(actionKind in setOf("TRIGGERED","STATE_TRIGGERED")) it.copy(repeatability="Per native matching trigger/event and retained conditions; future trigger availability is not guaranteed") else it },unresolved=unresolved,movesOriginalCard=false,tokenCreation=detail)
        frame.event?.let { context ->
            eventOutcomes += EventOutcome("$actionId|$path|TOKEN",card.name,face,actionId,path,context,OutcomeKind.CREATE_TOKEN,amount,controller,
                all(listOf(confidence,frame.outcomeGuard,context.confidence,if(frame.delayed || unresolved.isNotEmpty()) Verdict.UNKNOWN else Verdict.MATCH)),positive,frame.optional,
                prerequisites.map { it.replace("Intervening condition failed", "Announcement-time target selection produced nothing (unresolved alternative branch)") },e.tree(),detail,facts.last().id,linkage=if(frame.delayed) "UNRESOLVED_DEFERRED_LINK" else "SAME_TRIGGER_BRANCH",
                unresolved=unresolved + if(frame.outcomeGuard==Verdict.UNKNOWN || frame.delayed) listOf("Nested payment, iteration, branch or deferred context is not resolved for this relationship") else emptyList())
        }
        val partial=confidence==Verdict.UNKNOWN || creature==Verdict.UNKNOWN || controller=="UNKNOWN" || positive==Verdict.UNKNOWN || unresolved.isNotEmpty()
        support(path,e,if(partial) SupportStatus.PARTIALLY_SUPPORTED else SupportStatus.SUPPORTED,
            "Token variant retains correlated template/controller/quantity and native action; granted token abilities are retained but not recursively claimed as source-card actions")
        if(partial) issue("$path.tokenConstraints",e,"Unresolved token variant: creature=$creature; controller=$controller; quantity=$positive; $unresolved",SupportStatus.PARTIALLY_SUPPORTED)
        return true
    }
    private fun recordState(condition: Condition, conditionPath: String, outcomePath: String, yes: Effect, no: Effect?,
                            checkpoint: StateCheckpoint, frame: Frame, boundedContext: Boolean, otherwisePath: String? = null) {
        if(!inspectStateDependencies) return
        val scope=stateScope(condition)
        stateDependencies += StateDependency(card.name,face,actionId,conditionPath,outcomePath,condition,scope,checkpoint,
            yes,no,if(boundedContext && scope!=StateScope.SOURCE_ABILITY_HISTORY) Verdict.MATCH else Verdict.UNKNOWN,
            frame.assumptions + listOfNotNull("Source checkpoint zone: ${frame.selfZone}".takeIf { frame.selfZone!=null },
                "Native targets: ${frame.targets}".takeIf { frame.targets.isNotEmpty() }),
            frame.event, otherwisePath, causalChange = conditionalEffectChange(yes, no))
    }
    private fun recordPlayerOperation(effect: Effect, path: String, frame: Frame, sacrifice: MechanicalFact? = null): EventOutcome? {
        val event = frame.event ?: return null
        if (frame.resolutionCost) return null
        val (kind, quantity, target) = when (effect) {
            is ForceSacrificeEffect -> Triple(OutcomeKind.FORCED_SACRIFICE,
                effect.dynamicCount ?: DynamicAmount.Fixed(effect.count), effect.target)
            is LoseLifeEffect -> Triple(OutcomeKind.LOSE_LIFE, effect.amount, effect.target)
            is GainLifeEffect -> Triple(OutcomeKind.GAIN_LIFE, effect.amount, effect.target)
            is DealDamageEffect -> {
                if (!isPossiblePlayerDamage(effect.target, frame)) return null
                Triple(OutcomeKind.DEAL_PLAYER_DAMAGE, effect.amount, effect.target)
            }
            else -> return null
        }
        val (recipient, description) = eventOperationRecipient(target, frame)
        val positive = (quantity as? DynamicAmount.Fixed)?.let {
            if (it.amount > 0) Verdict.MATCH else Verdict.NO_MATCH
        } ?: Verdict.UNKNOWN
        val branch = all(listOf(frame.guardConfidence, frame.factGuard, frame.outcomeGuard, frame.operationGuard,
            if (frame.delayed) Verdict.UNKNOWN else Verdict.MATCH))
        val objectScope = sacrifice?.compatible(FactConstraints(kind = FactKind.SACRIFICE, from = Zone.BATTLEFIELD)) ?: Verdict.MATCH
        val sourceSupported = effect !is DealDamageEffect || effect.damageSource == null || effect.damageSource == EffectTarget.Self
        val unresolved = buildList {
            if (event.confidence == Verdict.UNKNOWN || branch == Verdict.UNKNOWN) add("The trigger or branch prerequisites are not fully interpreted.")
            if (recipient == "UNKNOWN") add("The affected player binding is unresolved.")
            if (positive == Verdict.UNKNOWN) add("The native quantity depends on game state and is not interpreted here.")
            if (objectScope == Verdict.UNKNOWN) add("The eligible sacrifice objects are not fully interpreted.")
            if (!sourceSupported) add("The native damage source is not fully interpreted.")
        }
        val requirements = frame.assumptions + when (kind) {
            OutcomeKind.FORCED_SACRIFICE -> listOf(
                "Each affected player chooses from eligible permanents they control, if able.",
                "A later death requires a creature to reach a graveyard from the battlefield. Replacements can change that result.")
            OutcomeKind.DEAL_PLAYER_DAMAGE -> listOf("Native damage source, prevention and replacement rules apply.")
            else -> emptyList()
        }
        val confidence = all(listOf(event.confidence, branch, objectScope, if (sourceSupported) Verdict.MATCH else Verdict.UNKNOWN))
        return EventOutcome("$actionId|$path|$kind", card.name, face, actionId, path, event, kind,
            quantity, recipient, confidence, positive, frame.optional, requirements, effect.tree(),
            factId = sacrifice?.id, linkage = if (frame.delayed) "UNRESOLVED_DEFERRED_LINK" else "SAME_TRIGGER_BRANCH",
            unresolved = unresolved, playerBinding = EventPlayerBinding(target, frame.operationPlayers, description))
            .also { eventOutcomes += it }
    }

    private fun retainClassAbility(path: String, ability: Any, native: JsonElement, availability: ClassAvailability) {
        actionId = "${card.name}|$face|$path"
        actions += ActionEvidence(actionId, card.name, face, path, availability.prerequisites(), native, classAvailability = availability)
        issue(path, ability, "Class availability retained. This ability shape is outside the current interpretation.")
    }

    private fun recordCounterReplacement(path: String, effect: ReplacementEffect, availability: ClassAvailability? = null) {
        val id = "${card.name}|$face|$path"
        actionId = id
        val native = nativeJson.encodeToJsonElement(ReplacementEffect.serializer(), effect)
        val checkpoint = when (effect) {
            is ModifyCounterPlacement, is ScaleCounterPlacement -> "A separate positive counter placement is required. The modifier must be active when that placement would happen."
            is EntersWithCounters, is EntersWithDynamicCounters -> "Counters are placed as part of entering the battlefield. The native entry and casting requirements apply."
            else -> {
                actions += ActionEvidence(id, card.name, face, path, availability?.prerequisites().orEmpty(), native, classAvailability = availability)
                issue(path, effect, "Replacement declaration retained without counter-placement interpretation")
                return
            }
        }
        val origin = CounterAction(card.name, face, id, path, native, effect.description, listOf(checkpoint) + availability?.prerequisites().orEmpty(), availability)
        actions += ActionEvidence(id, card.name, face, path, origin.prerequisites, native, classAvailability = availability)
        val confidence = when (effect) {
            is ModifyCounterPlacement -> {
                val modifier = CounterPlacementModifier(origin, effect.appliesTo, effect.modifier,
                    effect.placer == Player.You, effect.activeZones, selfCreature, placedByOpponent = effect.placer == Player.EachOpponent)
                counterModifiers += modifier
                modifier.recipientCompatibility(Requirements(creature = false))
            }
            is ScaleCounterPlacement -> {
                val modifier = CounterPlacementModifier(origin, effect.appliesTo, null,
                    effect.placer == Player.You, effect.activeZones, selfCreature, effectsOnly = effect.effectsOnly,
                    transformation = CounterTransformation.Multiplicative(2).takeIf { effect.scale == CounterPlacementScale.DOUBLE },
                    placedByOpponent = effect.placer == Player.EachOpponent)
                counterModifiers += modifier
                if (effect.scale == CounterPlacementScale.DOUBLE) modifier.recipientCompatibility(Requirements(creature = false)) else Verdict.UNKNOWN
            }
            is EntersWithCounters -> recordEntryCounters(origin, effect.counterType, DynamicAmount.Fixed(effect.count),
                effect.appliesTo, effect.activeZones, effect.selfOnly, effect.otherOnly, effect.condition?.description)
            is EntersWithDynamicCounters -> recordEntryCounters(origin, effect.counterType, effect.count,
                effect.appliesTo, effect.activeZones, !effect.otherOnly, effect.otherOnly, null)
            else -> Verdict.UNKNOWN
        }
        recordCounterSupport(path, effect, if (availability?.support == Verdict.UNKNOWN) Verdict.UNKNOWN else confidence)
    }

    private fun recordEntryCounters(origin: CounterAction, type: CounterType, amount: DynamicAmount, pattern: EventPattern,
                                    active: Set<Zone>, self: Boolean, other: Boolean, condition: String?): Verdict {
        val entry = pattern as? EventPattern.ZoneChangeEvent
        val recipient = when {
            self && !other -> CounterRecipient(controller = "YOU", sourceCreature = selfCreature)
            other && !self && entry != null -> CounterRecipient(filter = entry.filter)
            else -> CounterRecipient(unresolved = "Entry source versus other entrants is not fully interpreted")
        }
        val simple = entry?.to == Zone.BATTLEFIELD && !entry.excludeSacrifice && !entry.requireCraftMaterial && !entry.requireEmergeSacrifice &&
            entry.excludeTo == null && (!self || entry.from == null)
        val context = when {
            Zone.BATTLEFIELD !in active || self && other -> Verdict.NO_MATCH
            origin.classAvailability != null && self && !other -> Verdict.NO_MATCH
            origin.classAvailability?.support == Verdict.UNKNOWN -> Verdict.UNKNOWN
            !simple || condition != null -> Verdict.UNKNOWN
            else -> Verdict.MATCH
        }
        val castX = amount == DynamicAmount.XValue && self && card.manaCost.xCount > 0
        val prerequisites = origin.prerequisites + listOfNotNull(
            "The entrant must meet the entry pattern: ${pattern.description}",
            "The native entry condition must hold: $condition".takeIf { condition != null },
            "Cast this permanent with X greater than zero and pay the native X cost.".takeIf { castX }
        )
        val quantity = if (castX) Verdict.MATCH else amountBounds(amount).positivePotential()
        counterPlacements += CounterPlacement(origin.copy(prerequisites = prerequisites), type, amount, PlacementTiming.ENTRY,
            recipient, EffectTarget.Self.takeIf { self && !other }, emptyList(), false, quantity, context)
        return counterSupportConfidence(context, quantity, recipient)
    }

    private fun counterSupportConfidence(context: Verdict, quantity: Verdict, recipient: CounterRecipient): Verdict =
        if (Verdict.UNKNOWN in listOf(context, quantity, recipient.compatible(Requirements(creature = false)))) Verdict.UNKNOWN else Verdict.MATCH

    private fun recordCounterSupport(path: String, native: Any, confidence: Verdict) {
        if (confidence == Verdict.UNKNOWN)
            issue(path, native, "Counter operation retained with unresolved quantity, applicability or context", SupportStatus.PARTIALLY_SUPPORTED)
        else support(path, native, SupportStatus.SUPPORTED, "Native counter operation and its scope are interpreted")
    }

    private fun recordCounterInstruction(effect: Effect, path: String, frame: Frame): Boolean {
        val (type, amount, target) = when (effect) {
            is AddCountersEffect -> Triple(effect.counterType, DynamicAmount.Fixed(effect.count), effect.target)
            is AddDynamicCountersEffect -> Triple(effect.counterType, effect.amount, effect.target)
            is AddCountersWithLimitEffect -> Triple(effect.counterType, effect.amount, effect.target)
            is AddCountersUpToEffect -> Triple(effect.counterType, effect.max, effect.target)
            else -> return false
        }
        val requirement = if (frame.counterTargetIteration && target == EffectTarget.ContextTarget(0))
            frame.targets.singleOrNull() else targetRequirement(target, frame)
        val scope = if (frame.counterTargetIteration && target == EffectTarget.ContextTarget(0))
            requirementScope(requirement) else targetScope(target, frame, selfCreature)
        val recipient = CounterRecipient(scope.filter, scope.actor, scope.printedSelfCreature, scope.battlefield, scope.unresolved)
        val countSupport = when {
            requirement?.dynamicMaxCount != null || requirement?.dynamicMinCount != null -> Verdict.UNKNOWN
            requirement != null && !requirement.unlimited && requirement.count == 0 -> Verdict.NO_MATCH
            else -> Verdict.MATCH
        }
        val context = all(listOf(frame.guardConfidence, frame.factGuard, frame.outcomeGuard, countSupport,
            if (frame.delayed || frame.resolutionCost || effect is AddCountersWithLimitEffect || frame.classAvailability?.support == Verdict.UNKNOWN ||
                frame.event?.confidence == Verdict.UNKNOWN) Verdict.UNKNOWN else Verdict.MATCH))
        val prerequisites = frame.assumptions + listOfNotNull(
            "Chosen targets must still be legal. At least one qualifying recipient must be chosen.".takeIf { requirement != null },
            "Only the current chosen target receives this quantity in each iteration.".takeIf { frame.counterTargetIteration },
            "A positive quantity must be chosen.".takeIf { effect is AddCountersUpToEffect }
        )
        val quantity = amountBounds(amount).positivePotential()
        counterPlacements += CounterPlacement(CounterAction(card.name, face, actionId, path, effect.tree(), effect.description,
            prerequisites, frame.classAvailability), type, amount, PlacementTiming.RESOLUTION, recipient, target, frame.targets,
            frame.optional || requirement?.optional == true || effect is AddCountersUpToEffect,
            quantity, context)
        recordCounterSupport(path, effect, counterSupportConfidence(context, quantity, recipient))
        return true
    }

    private fun collectionStage(id: String?): CollectionStage? = id?.let { key ->
        actions.lastOrNull { it.id==actionId }?.collectionStages?.lastOrNull { it.id==key }
    }

    /** Uses are recorded before writes, so overwriting a variable cannot bind its own input. */
    private fun traceCollectionUse(e: Effect,path: String,frame: Frame) {
        val refs=mutableListOf<Triple<String,String,Int?>>()
        when(e) {
            is GatherCardsEffect -> (e.source as? CardSource.FromVariable)?.let { refs+=Triple(it.variableName,"FROM_VARIABLE",null) }
            is SelectFromCollectionEffect -> refs+=Triple(e.from,"SELECTION",null)
            is FilterCollectionEffect -> refs+=Triple(e.from,"FILTER",null)
            is MoveCollectionEffect -> refs+=Triple(e.from,"MOVEMENT",null)
            is ConditionalOnCollectionEffect -> refs+=Triple(e.collection,"COLLECTION_CONDITION",null)
            is ForEachEffect -> (e.space as? IterationSpace.Collection)?.let { refs+=Triple(it.collection,"ITERATION",null) }
            else -> Unit
        }
        // Only this instruction's direct target. Nested effects are visited with their own frame.
        val target=when(e) {
            is MoveToZoneEffect -> e.target
            is GrantKeywordEffect -> e.target
            is AttachEquipmentEffect -> e.target
            is SacrificeTargetEffect -> e.target
            else -> null
        } as? EffectTarget.PipelineTarget
        if(target!=null) refs+=Triple(target.collectionName,"INDEXED_OBJECT",target.index)
        if(refs.isEmpty()) return
        val a=actions.indexOfLast { it.id==actionId }
        val uses=refs.map { (variable,kind,index) ->
            val producer=collectionStage(frame.collectionBindings[variable])
            val problems=listOfNotNull(
                if(producer==null) "No producer in this action and branch" else null,
                if(frame.delayed) "Deferred consumer is outside action-local continuation support" else null,
                if(producer?.support==Verdict.UNKNOWN) "Producer membership or bounds unresolved" else null,
                if(producer?.membership==CollectionMembership.RETAINED_INPUT) "Original input IDs are not a successful-movement subset; current zones require native observation" else null)
            val maximum=listOfNotNull(producer?.maximum,frame.collectionMax[variable]).minOrNull()
            val impossible=index!=null && (index<0 || maximum?.let { index>=it }==true)
            CollectionUse(actionId,path,variable,producer?.id,kind,index,
                if(index!=null && index<0) Verdict.NO_MATCH else if(problems.isNotEmpty()) Verdict.UNKNOWN else if(impossible) Verdict.NO_MATCH else Verdict.MATCH,
                frame.assumptions+listOfNotNull(if(index!=null) "Member $index must exist in the actual collection and still reference the appropriate current game object" else null,
                    "Native choices, filters, successful transitions and replacements determine actual membership; no movement or continuation is guaranteed"),problems)
        }
        actions[a]=actions[a].copy(collectionUses=actions[a].collectionUses+uses)
    }

    private fun traceCollectionProduction(e: Effect,path: String,frame: Frame,movementFactIds: List<String>) {
        val a=actions.indexOfLast { it.id==actionId }
        fun stage(variable: String,membership: CollectionMembership,input: CollectionStage?,maximum: Int?,zone: Zone?,
                  destination: Zone?=null,problems: List<String> = emptyList()) {
            val unresolved=problems+input?.unresolved.orEmpty().filterNot { it=="Unbounded or dynamic collection cardinality" }+listOfNotNull(if(frame.delayed) "Deferred producer outside action-local scope" else null,
                if(membership==CollectionMembership.NATIVE_MOVED_RESULT && variable!=((e as? MoveCollectionEffect)?.from) && variable in frame.collectionBindings)
                    "Repeated result binding requires native overwrite/empty-result evidence" else null,
                if(maximum==null) "Unbounded or dynamic collection cardinality" else null)
            val record=CollectionStage("$actionId|$path|COLLECTION|$variable|${actions[a].collectionStages.size}",actionId,path,variable,
                membership,input?.id,maximum=maximum,sourceZone=zone,destinationZone=destination,
                movementFactIds=movementFactIds,
                prerequisites=frame.assumptions+"Structural membership may be empty; native execution owns actual selection and movement results"+
                    "Requested-zone arrivals require actual transition evidence; native moved results may contain redirected moves",
                support=if(unresolved.isEmpty()) Verdict.MATCH else Verdict.UNKNOWN,unresolved=unresolved,
                selection=(e as? SelectFromCollectionEffect)?.selection.takeIf { membership==CollectionMembership.SELECTED },
                chooser=(e as? SelectFromCollectionEffect)?.chooser.takeIf { membership==CollectionMembership.SELECTED })
            actions[a]=actions[a].copy(collectionStages=actions[a].collectionStages+record)
            frame.collectionBindings[variable]=record.id
        }
        fun input(variable: String)=collectionStage(frame.collectionBindings[variable])
        fun upper(variable: String)=listOfNotNull(input(variable)?.maximum,frame.collectionMax[variable]).minOrNull()
        when(e) {
            is GatherCardsEffect -> {
                val parent=(e.source as? CardSource.FromVariable)?.let { input(it.variableName) }
                val bound=when(val source=e.source) {
                    is CardSource.TopOfLibrary -> (source.count as? DynamicAmount.Fixed)?.amount?.coerceAtLeast(0)
                    CardSource.ChosenTargets -> frame.targets.singleOrNull()?.let { if(it.unlimited || it.dynamicMaxCount!=null) null else it.count }
                    is CardSource.Entity -> 1
                    else -> parent?.maximum ?: frame.collectionMax[e.storeAs]
                }
                val zone=when(val source=e.source) {
                    is CardSource.TopOfLibrary -> Zone.LIBRARY
                    else -> parent?.destinationZone ?: parent?.sourceZone ?: frame.collections[e.storeAs]?.zone
                }
                val problems=listOfNotNull(
                    if(e.source is CardSource.FromVariable && parent==null) "Unbound variable source" else null,
                    if(e.source !is CardSource.TopOfLibrary && e.source !is CardSource.FromVariable && frame.collections[e.storeAs]?.unresolved!=null)
                        frame.collections[e.storeAs]?.unresolved else null)
                stage(e.storeAs,if(e.source is CardSource.FromVariable) CollectionMembership.ALIAS else CollectionMembership.GATHERED,parent,bound,zone,problems=problems)
            }
            is SelectFromCollectionEffect -> {
                val parent=input(e.from)
                val count=when(val s=e.selection) { is SelectionMode.ChooseExactly -> s.count; is SelectionMode.ChooseUpTo -> s.count; is SelectionMode.Random -> s.count; else -> null }
                val fixed=(count as? DynamicAmount.Fixed)?.amount?.coerceAtLeast(0)
                val max=if(fixed==null) upper(e.from) else upper(e.from)?.let { minOf(it,fixed) } ?: fixed
                val problems=listOfNotNull(if(parent==null) "Unbound selection source" else null,
                    if(e.selection !is SelectionMode.ChooseExactly && e.selection !is SelectionMode.ChooseUpTo && e.selection !is SelectionMode.Random &&
                        e.selection!=SelectionMode.All && e.selection!=SelectionMode.ChooseAnyNumber) "Unsupported native selection mode" else null,
                    if(count!=null && fixed==null) "Dynamic selection count" else null,
                    if(e.restrictions.isNotEmpty() || e.matchChosenCreatureType || e.chooser!=Chooser.Controller) "Additional native selection restriction or chooser" else null)
                stage(e.storeSelected,CollectionMembership.SELECTED,parent,max,parent?.destinationZone ?: parent?.sourceZone,problems=problems)
                e.storeRemainder?.let { stage(it,CollectionMembership.COMPLEMENT,parent,upper(e.from),parent?.sourceZone,problems=listOf("Complement membership unresolved")) }
            }
            is FilterCollectionEffect -> {
                val parent=input(e.from)
                val problems=listOfNotNull(if(parent==null) "Unbound filter source" else null,if(e.collectionFilter!=null) "Collection-relative predicate unresolved" else null)
                stage(e.storeMatching,CollectionMembership.MATCHING,parent,upper(e.from),parent?.destinationZone ?: parent?.sourceZone,problems=problems)
                e.storeNonMatching?.let { stage(it,CollectionMembership.COMPLEMENT,parent,parent?.maximum,parent?.sourceZone,problems=listOf("Complement predicate unresolved")) }
            }
            is MoveCollectionEffect -> {
                val parent=input(e.from)
                val max=upper(e.from)
                val zone=parent?.destinationZone ?: parent?.sourceZone
                val dest=(e.destination as? CardDestination.ToZone)?.zone
                val problems=listOfNotNull(if(parent==null) "Unbound movement source" else null,
                    if(dest==null) "Dynamic destination unresolved" else null)
                // An input is not the result subset; some executors can also narrow that input.
                stage(e.from,CollectionMembership.RETAINED_INPUT,parent,max,zone,problems=problems+
                    "Post-movement input membership and mixed zones require native observation")
                e.storeMovedAs?.let { stage(it,CollectionMembership.NATIVE_MOVED_RESULT,parent,max,zone,dest,problems) }
            }
            else -> Unit
        }
    }

    private fun walk(e: Effect,path: String,frame: Frame) {
        val instructionFactStart=facts.size
        traceCollectionUse(e,path,frame)
        if(e is ExileUntilLeavesEffect || e is ExileWithAurasNotingCountersEffect ||
            e is MoveToZoneEffect && e.linkToSource || e is MoveCollectionEffect && e.linkToSource) linkedExileProducers++
        if (inspectCounters && recordCounterInstruction(e, path, frame)) return
        if(grantedSource && (e is AddManaEffect || e is AddManaOfChoiceEffect || e is AddColorlessManaEffect || e is AddDynamicManaEffect)) {
            add(path,"MANA_PRODUCTION","EFFECT",ObjectScope(battlefield=false,actor="YOU"),e.tree(),frame)
            support(path,e,SupportStatus.SUPPORTED,"Native mana production declaration retained in receiving-source context; actual amount, color choices and payment remain native")
            return
        }
        if(recordToken(e,path,frame)) return
        if (e is DealDamageEffect) recordPlayerOperation(e, path, frame)
        if(e is DrawCardsEffect && frame.event!=null) {
            if(actionKind=="GRANTED_TRIGGERED") add(path,"DRAW_CARDS","EFFECT",ObjectScope(battlefield=false,actor=playerScope(e.target,frame)),e.tree(),frame)
            val context=frame.event
            val recipient=playerScope(e.target,frame)
            val confidence=all(listOf(frame.guardConfidence,frame.factGuard,frame.outcomeGuard,context.confidence,
                if(frame.delayed) Verdict.UNKNOWN else Verdict.MATCH))
            eventOutcomes += EventOutcome("$actionId|$path|DRAW",card.name,face,actionId,path,context,OutcomeKind.DRAW_CARDS,e.count,recipient,confidence,
                tokenQuantityPotential(e.count),frame.optional,frame.assumptions.map { it.replace("Intervening condition failed", "Announcement-time target selection produced nothing (unresolved alternative branch)") }+context.requirements+
                    "Native trigger, targets, choices, resources and draw replacements apply; drawing is not guaranteed or a deck-value score",e.tree(),
                linkage=if(frame.delayed) "UNRESOLVED_DEFERRED_LINK" else "SAME_TRIGGER_BRANCH",
                unresolved=if(confidence==Verdict.UNKNOWN || recipient=="UNKNOWN") listOf("Trigger/branch/receiver context is not fully interpreted") else emptyList())
        }
        if(e is CreateTokenBatchEffect) {
            val controller=e.controller?.let { playerScope(it,frame) } ?: frame.actor
            support(path,e,SupportStatus.SUPPORTED,"One creation event; each native group retains its own characteristics/quantity and shared receiver")
            e.groups.forEachIndexed { i,group -> walk(group,"$path.groups[$i]",frame.copy(tokenController=controller,tokenEvent=path)) }
            return
        }
        if(e is MoveToZoneEffect) recordTargetMove(e,path,frame)
        when(e) {
            is ConditionalOnCollectionEffect -> {
                val bound=frame.collections[e.collection]
                val max=frame.collectionMax[e.collection]
                val raw= !e.countDistinctCardTypes && e.filter==GameObjectFilter.Any
                val known=bound!=null && raw
                val yesPossible=if(e.countDistinctCardTypes) max!=0 || e.minSize<=0 else max==null || max>=e.minSize
                val noPossible=e.minSize>0
                val reason="Collection guard ${e.collection}: threshold=${e.minSize}; filter=${e.filter}; distinctTypes=${e.countDistinctCardTypes}; knownMax=$max; branch must actually satisfy native measurement"
                support(path,e,if(known) SupportStatus.SUPPORTED else SupportStatus.PARTIALLY_SUPPORTED,
                    if(known) "Raw-size conditional branches traversed with bounded reachability; condition is a prerequisite, not a guaranteed outcome" else "Branches visible but unbound/filtered/distinct-type guard satisfiability unresolved")
                if(!known) issue("$path.guard",e,"Collection measurement variant unresolved; child pathways retain UNKNOWN guard confidence")
                if(yesPossible) {
                    val branch=frame.copy(assumptions=frame.assumptions+"$reason; measured size >= threshold",movementTargets=frame.movementTargets.toMutableMap(),collectionBindings=frame.collectionBindings.toMutableMap(),collections=frame.collections.toMutableMap(),collectionMax=frame.collectionMax.toMutableMap(),guardConfidence=all(listOf(frame.guardConfidence,if(known) Verdict.MATCH else Verdict.UNKNOWN)))
                    walk(e.ifNotEmpty,"$path.ifNotEmpty",branch)
                }
                if(noPossible) e.ifEmpty?.let {
                    val branch=frame.copy(assumptions=frame.assumptions+"$reason; measured size < threshold",movementTargets=frame.movementTargets.toMutableMap(),collectionBindings=frame.collectionBindings.toMutableMap(),collections=frame.collections.toMutableMap(),collectionMax=frame.collectionMax.toMutableMap(),guardConfidence=all(listOf(frame.guardConfidence,if(known) Verdict.MATCH else Verdict.UNKNOWN)))
                    if(raw) branch.collectionMax[e.collection]=minOf(max ?: Int.MAX_VALUE,e.minSize-1)
                    walk(it,"$path.ifEmpty",branch)
                }
            }
            is PayOrSufferEffect -> {
                val payer=playerScope(e.player,frame)
                payCost(e.cost,"$path.cost",frame.copy(actor=payer,resolutionCost=true,optional=true,
                    assumptions=frame.assumptions+"Optional native payment by $payer; this branch avoids the suffer outcome"))
                walk(e.suffer,"$path.suffer",frame.copy(returnGuard=Verdict.UNKNOWN,outcomeGuard=Verdict.UNKNOWN,assumptions=frame.assumptions+"Native PayOrSuffer: payment ${e.cost} by $payer was not made; failure branch executes"))
            }
            is CompositeEffect -> {
                val local=frame.copy(movementTargets=frame.movementTargets.toMutableMap(),collectionBindings=frame.collectionBindings.toMutableMap(),collections=frame.collections.toMutableMap(),collectionMax=frame.collectionMax.toMutableMap())
                e.effects.forEachIndexed { i,child ->
                    val following=e.effects.drop(i+1).mapIndexedNotNull { j,next -> followingReturnLimit(next,"$path.effects[${i+j+1}]") }
                    walk(child,"$path.effects[$i]",local.copy(returnLimits=frame.returnLimits+following))
                }
            }
            is GatedEffect -> {
                val condition=(e.gate as? Gate.WhenCondition)?.condition
                if(condition!=null) recordState(condition,"$path.gate.condition","$path.then",e.then,e.otherwise,
                    StateCheckpoint.RESOLUTION,frame,stateRootPath==path && e.decisionMaker==null && !frame.delayed &&
                        (frame.event==null || frame.event.confidence==Verdict.MATCH) && frame.factGuard==Verdict.MATCH,
                    otherwisePath = e.otherwise?.let { "$path.otherwise" })
                val assumptions=frame.assumptions+"Gate=${nativeJson.encodeToJsonElement(Gate.serializer(),e.gate)}; success branch"
                val branch=frame.copy(returnGuard=all(listOf(frame.returnGuard,returnGateSupport(e.gate,frame.selfZone,e.decisionMaker))),outcomeGuard=if(e.gate is Gate.MayDecide) frame.outcomeGuard else Verdict.UNKNOWN,factGuard=if(condition!=null) Verdict.UNKNOWN else frame.factGuard,assumptions=assumptions,movementTargets=frame.movementTargets.toMutableMap(),collectionBindings=frame.collectionBindings.toMutableMap(),collections=frame.collections.toMutableMap(),collectionMax=frame.collectionMax.toMutableMap())
                when(val gate=e.gate) {
                    is Gate.DoAction -> walk(gate.action,"$path.gate.action",branch.copy(assumptions=frame.assumptions+"Action outcome gates associated payoff: ${gate.successCriterion}",relationship=if(gate.successCriterion is SuccessCriterion.PermanentsSacrificed) "SACRIFICE_SUCCESS_ENABLES_OUTCOME" else "UNRESOLVED_SUCCESS_DEPENDENCY"))
                    is Gate.MayPay -> walk(gate.cost,"$path.gate.cost",branch.copy(assumptions=frame.assumptions+"Optional resolution payment",resolutionCost=true))
                    is Gate.WhenCondition, is Gate.MayDecide -> Unit
                    else -> issue("$path.gate",gate,"Gate payment/decision semantics not interpreted")
                }
                walk(e.then,"$path.then",branch.copy(optional=frame.optional || e.gate is Gate.MayDecide || e.gate is Gate.MayPay,
                    operationGuard = if (e.gate is Gate.MayDecide && e.decisionMaker != null) Verdict.UNKNOWN else frame.operationGuard))
                e.otherwise?.let { walk(it,"$path.otherwise",frame.copy(returnGuard=Verdict.UNKNOWN,outcomeGuard=Verdict.UNKNOWN,factGuard=if(condition!=null) Verdict.UNKNOWN else frame.factGuard,assumptions=frame.assumptions+"Gate failed: ${e.gate}",movementTargets=frame.movementTargets.toMutableMap(),collectionBindings=frame.collectionBindings.toMutableMap(),collections=frame.collections.toMutableMap(),collectionMax=frame.collectionMax.toMutableMap())) }
                if(condition != null) {
                    val baseline=removals(e.otherwise)
                    removals(e.then).forEach { r ->
                        val same=baseline.find { it.operation == r.operation && it.target == r.target }
                        val improved=when {
                            same == null && baseline.isEmpty() -> Verdict.MATCH
                            same == null -> Verdict.UNKNOWN
                            same.effect == r.effect -> Verdict.NO_MATCH
                            r.magnitude != null && same.magnitude != null -> if(r.magnitude > same.magnitude) Verdict.MATCH else Verdict.NO_MATCH
                            else -> Verdict.UNKNOWN
                        }
                        enhanced("$path.gate.condition",condition.toString(),deathDependency(condition),r,frame,improved)
                    }
                }
            }
            is ModalEffect -> {
                e.additionalCostPerExtraMode?.let { issue("$path.additionalCostPerExtraMode",it,"Optional extra-mode cost not expanded") }
                e.modes.forEachIndexed { i,m ->
                val parent=rootEffect; rootEffect=m.effect
                val branch=frame.copy(returnGuard=Verdict.UNKNOWN,outcomeGuard=Verdict.UNKNOWN,targets=frame.targets+m.targetRequirements,
                    assumptions=frame.assumptions+"Mode $i selected; chooseCount=${e.chooseCount}; mode mana=${m.additionalManaCost}; extra-mode cost=${e.additionalCostPerExtraMode}",movementTargets=frame.movementTargets.toMutableMap(),collectionBindings=frame.collectionBindings.toMutableMap(),collections=frame.collections.toMutableMap(),collectionMax=frame.collectionMax.toMutableMap())
                if(actionKind=="SPELL" && path=="script.spellEffect") {
                    val costs=m.additionalCosts ?: baseCastingCosts
                    costs.forEachIndexed { j,c -> castingCost(c,if(m.additionalCosts==null) "script.additionalCosts[$j]" else "$path.modes[$i].additionalCosts[$j]",branch.copy(assumptions=branch.assumptions+"Mode costs override base costs when present")) }
                    support("$path.modes[$i]",m,SupportStatus.SUPPORTED,"Root spell mode cost and target association interpreted; this is not a claim that every child effect is supported")
                } else if(m.additionalCosts!=null || m.additionalManaCost!=null) issue("$path.modes[$i]",m,"Nested/non-casting mode cost activation semantics unresolved; no casting cost inferred")
                walk(m.effect,"$path.modes[$i].effect",branch)
                rootEffect=parent
                }
            }
            is ForEachEffect -> {
                val next=when(val s=e.space) {
                    is IterationSpace.Group -> frame.copy(iteration=ObjectScope(s.filter.baseFilter,unresolved=if(s.filter.scope == com.wingedsheep.sdk.scripting.filters.unified.Scope.Battlefield) null else "Non-battlefield iteration scope: ${s.filter.scope}"))
                    is IterationSpace.Collection -> frame.copy(iteration=(frame.collections[s.collection] ?: ObjectScope(unresolved="Unbound collection ${s.collection}")).let { if(frame.collectionMax[s.collection]==0) it.copy(battlefield=false) else it })
                    is IterationSpace.Players -> frame.copy(actor=actor(s.players))
                    else -> frame.copy(iteration=ObjectScope(unresolved="Iteration space not interpreted: ${s.javaClass.simpleName}"))
                }
                val players = (e.space as? IterationSpace.Players)?.players?.takeIf {
                    it in setOf(Player.Each, Player.EachOpponent, Player.ActivePlayerFirst, Player.EachStartingWithYou) &&
                        (e.body is ForceSacrificeEffect || e.body is LoseLifeEffect || e.body is GainLifeEffect || e.body is DealDamageEffect)
                }
                val counterTargets = inspectCounters && e.space == IterationSpace.Targets &&
                    frame.targets.singleOrNull() is TargetObject &&
                    (e.body is AddCountersEffect || e.body is AddDynamicCountersEffect)
                val child=next.copy(returnGuard=Verdict.UNKNOWN,outcomeGuard=if (players == null && !counterTargets) Verdict.UNKNOWN else frame.outcomeGuard,
                    counterTargetIteration=counterTargets,
                    operationPlayers=players,movementTargets=frame.movementTargets.toMutableMap(),collectionBindings=frame.collectionBindings.toMutableMap(),collections=frame.collections.toMutableMap(),collectionMax=frame.collectionMax.toMutableMap(),assumptions=frame.assumptions+"Iteration=${e.space}")
                if(e.space is IterationSpace.Players) { child.collectionBindings.clear();child.collections.clear();child.collectionMax.clear() }
                walk(e.body,"$path.body",child)
            }
            is GatherCardsEffect -> {
                frame.collectionMax[e.storeAs]=(e.source as? CardSource.FromVariable)?.let { frame.collectionMax[it.variableName] }
                frame.collections[e.storeAs]=when(val s=e.source) {
                    is CardSource.BattlefieldMatching -> ObjectScope(s.filter,relativeActor(s.player,frame),zone=Zone.BATTLEFIELD,zonePlayer=relativeActor(s.player,frame),details=listOf("Native source restrictions: $s"))
                    is CardSource.FromZone -> {
                        val hand = inspectCardEntries && s.zone == Zone.HAND
                        val player = if(hand) cardEntryPlayer(s.player,frame) else relativeActor(s.player,frame)
                        ObjectScope(s.filter,player,battlefield=s.zone == Zone.BATTLEFIELD,zone=s.zone,zonePlayer=player,
                            ownerScope=if(s.zone in setOf(Zone.GRAVEYARD,Zone.HAND,Zone.LIBRARY)) player else "ANY",
                            details=listOf("Native source restrictions: $s"),entryPlayer=s.player.takeIf { hand })
                    }
                    is CardSource.TopOfLibrary -> if(inspectCardEntries && s.isMill) {
                        val player = cardEntryPlayer(s.player,frame)
                        val count = (s.count as? DynamicAmount.Fixed)?.amount
                        frame.collectionMax[e.storeAs] = count?.coerceAtLeast(0)
                        ObjectScope(actor=player,battlefield=false,zone=Zone.LIBRARY,zonePlayer=player,ownerScope=player,
                            details=listOf("Native source restrictions: $s"),entryPlayer=s.player,cardEntry=CardEntry(GraveyardRoute.MILL,count))
                    } else ObjectScope(unresolved="Collection source not interpreted: ${s.javaClass.simpleName}")
                    is CardSource.ControlledPermanents -> ObjectScope(s.filter,relativeActor(s.player,frame),zone=Zone.BATTLEFIELD,zonePlayer=relativeActor(s.player,frame),details=listOf("Native source restrictions: $s"))
                    CardSource.ChosenTargets -> if(frame.targets.size==1) requirementScope(frame.targets.single()) else ObjectScope(unresolved="Multiple/absent chosen-target collection not unified")
                    is CardSource.Entity -> targetScope(s.entity,frame,selfCreature)
                    is CardSource.FromVariable -> frame.collections[s.variableName] ?: ObjectScope(unresolved="Unbound source variable ${s.variableName}")
                    else -> ObjectScope(unresolved="Collection source not interpreted: ${s.javaClass.simpleName}")
                }
                val obj=frame.collections.getValue(e.storeAs)
                support(path,e,if(obj.unresolved==null) SupportStatus.SUPPORTED else SupportStatus.PARTIALLY_SUPPORTED,
                    "Collection provenance/filter/controller retained; source=${e.source}; cardinality remains conditional")
                if(obj.unresolved!=null) issue(path,e,obj.unresolved,SupportStatus.PARTIALLY_SUPPORTED)
            }
            is SelectFromCollectionEffect -> {
                val source=frame.collections[e.from] ?: ObjectScope(unresolved="Unbound selection source ${e.from}")
                val count=when(val selection=e.selection) { is SelectionMode.ChooseExactly -> selection.count; is SelectionMode.ChooseUpTo -> selection.count; is SelectionMode.Random -> selection.count; else -> null }
                val zero=(count as? DynamicAmount.Fixed)?.amount?.let { it <= 0 } == true
                val knownSelection=e.selection is SelectionMode.ChooseExactly || e.selection is SelectionMode.ChooseUpTo || e.selection == SelectionMode.All || e.selection == SelectionMode.ChooseAnyNumber || e.selection is SelectionMode.Random
                val problems=listOfNotNull(source.unresolved,
                    if(!knownSelection || count != null && count !is DynamicAmount.Fixed) "Selection mode/count requires game-state interpretation" else null,
                    if(e.restrictions.isNotEmpty() || e.matchChosenCreatureType) "Additional selection restrictions retained: ${e.restrictions}; chosenType=${e.matchChosenCreatureType}" else null)
                val entry = if (inspectCardEntries && source.zone == Zone.HAND)
                    CardEntry(GraveyardRoute.DISCARD,(count as? DynamicAmount.Fixed)?.amount
                        .takeIf { e.selection is SelectionMode.ChooseExactly || e.selection is SelectionMode.Random },e.selection is SelectionMode.Random)
                    else source.cardEntry?.copy(count=null)
                frame.collections[e.storeSelected]=source.copy(filter=intersect(source.filter,e.filter),possible=source.possible && !zero,battlefield=source.battlefield && !zero,details=source.details+"Selection=${e.selection}; chooser=${e.chooser}; filter=${e.filter}; restrictions=${e.restrictions}; chosenType=${e.matchChosenCreatureType}",cardEntry=entry,
                    unresolved=problems.takeIf { it.isNotEmpty() }?.joinToString("; "))
                val fixed=(count as? DynamicAmount.Fixed)?.amount?.coerceAtLeast(0)
                val inputMax=frame.collectionMax[e.from]
                frame.collectionMax[e.storeSelected]=when { fixed==null -> inputMax; inputMax==null -> fixed; else -> minOf(fixed,inputMax) }
                e.storeRemainder?.let { frame.collectionMax[it]=inputMax; frame.collections[it]=source.copy(unresolved="Selection remainder membership and nonemptiness unresolved") }
                if(problems.isNotEmpty()) issue(path,e,"Selection provenance known; restrictions/count remain unresolved")
            }
            is FilterCollectionEffect -> {
                val source=frame.collections[e.from] ?: ObjectScope(unresolved="Unbound filtered collection ${e.from}")
                frame.collectionMax[e.storeMatching]=frame.collectionMax[e.from]
                frame.collections[e.storeMatching]=source.copy(filter=intersect(source.filter,e.filter),
                    cardEntry=source.cardEntry?.copy(count=null),
                    unresolved=if(e.collectionFilter != null) "Collection-relative filter unresolved: ${e.collectionFilter}" else source.unresolved)
                e.storeNonMatching?.let { frame.collections[it]=source.copy(unresolved="Complement collection filter not solved") }
                if(e.collectionFilter != null || e.storeNonMatching != null) issue(path,e,"Positive per-card filter propagated; relative/complement membership unresolved")
            }
            is MoveCollectionEffect -> {
                val source=frame.collections[e.from] ?: ObjectScope(unresolved="Unbound pipeline collection: ${e.from}")
                val obj=if(e.filter==null) source else source.copy(filter=intersect(source.filter,e.filter!!))
                val destination=e.destination as? CardDestination.ToZone
                val entry = when {
                    !inspectCardEntries -> null
                    e.moveType == MoveType.Discard -> obj.cardEntry?.takeIf { it.route == GraveyardRoute.DISCARD } ?: CardEntry(GraveyardRoute.DISCARD,null)
                    e.moveType == MoveType.Default && obj.zone == Zone.LIBRARY -> obj.cardEntry?.takeIf { it.route == GraveyardRoute.MILL }
                    else -> null
                }
                val samePlayer = entry != null && obj.entryPlayer != null && obj.entryPlayer == destination?.player
                recordMovement(path,e.tree(),obj,destination?.zone,when {
                    e.underOwnersControl || samePlayer -> "OWNER"
                    entry != null -> "UNKNOWN"
                    else -> destination?.let { relativeActor(it.player,frame) } ?: "UNKNOWN"
                },frame,
                    binding=e.from,selection="collectionMax=${frame.collectionMax[e.from]}; ${obj.details}; operation=${e.moveType}",
                    extra=listOfNotNull(if(e.faceDown!=null) "Face-down arrival characteristics unresolved" else null,if(destination==null) "Dynamic destination unresolved" else null,
                        if(entry?.route == GraveyardRoute.MILL && e.filter != null) "Filtered mill membership unresolved" else null),
                    confidence=if(frame.collectionMax[e.from]==0) Verdict.NO_MATCH else Verdict.MATCH,cardEntry=entry)
                val restrictions=listOfNotNull(source.unresolved,
                    if(destination==null) "Dynamic destination not interpreted" else null,
                    if(source.battlefield && destination?.zone==Zone.BATTLEFIELD) "Battlefield-to-battlefield movement/control semantics unresolved" else null,
                    if(e.faceDown!=null) "Face-down characteristic changes unresolved" else null,
                    if(e.underOwnersControl) "Owner-to-controller relationship not established" else null)
                val assumptions=frame.assumptions+"Collection ${e.from} move: sourceBattlefield=${source.battlefield}; destination=${e.destination}; operation=${e.moveType}; filter=${e.filter}; order=${e.order}; ownerControl=${e.underOwnersControl}; faceDown=${e.faceDown}; only actual arrivals qualify; replacements/failures may prevent entry or death"
                val r=removal(e)
                if(r!=null) add(path,r.operation,"EFFECT",obj.copy(battlefield=obj.battlefield && frame.collectionMax[e.from]!=0),e.tree(),frame.copy(assumptions=assumptions),incidental=obj.actor!="YOU")
                val moved=if(e.faceDown!=null) ObjectScope(unresolved="Face-down destination characteristics unresolved") else obj.copy(
                    filter=if(destination?.zone==Zone.BATTLEFIELD) obj.filter.copy(controllerPredicate=null) else obj.filter,
                    actor=if(destination?.zone==Zone.BATTLEFIELD) { if(e.underOwnersControl) "UNKNOWN" else relativeActor(destination.player,frame) } else obj.actor,
                    battlefield=destination?.zone==Zone.BATTLEFIELD,zone=destination?.zone,zonePlayer=if(destination?.zone==Zone.BATTLEFIELD) "UNKNOWN" else "ANY",details=obj.details+assumptions.last(),
                    unresolved=restrictions.takeIf { it.isNotEmpty() }?.joinToString("; "),entryPlayer=null,cardEntry=null)
                // Original collections keep IDs, not a success count. Model the no-replacement arrival
                // branch explicitly; filtered partitions remain unresolved rather than treating all IDs as moved.
                frame.collections[e.from]=if(e.filter==null) moved else source.copy(battlefield=source.battlefield || moved.battlefield,actor=if(source.actor==moved.actor) source.actor else "UNKNOWN",zone=null,zonePlayer="UNKNOWN",unresolved="Filtered move can leave mixed zones; source membership is not only moved objects")
                e.storeMovedAs?.let { frame.collections[it]=moved;frame.collectionMax[it]=frame.collectionMax[e.from] }
                val status=if(restrictions.isEmpty()) SupportStatus.SUPPORTED else SupportStatus.PARTIALLY_SUPPORTED
                support(path,e,status,"Zone-flow variant interpreted; ${assumptions.last()}; indexed later references require the same object to remain in that zone")
                if(status==SupportStatus.PARTIALLY_SUPPORTED) issue(path,e,restrictions.joinToString("; "),status)
            }
            is SacrificeEffect -> add(path,"SACRIFICE","EFFECT",ObjectScope(
                if(frame.actor != "YOU" && e.filter.controllerPredicate != null) e.filter.copy(controllerPredicate=null) else e.filter,
                frame.actor,unresolved=if(frame.actor != "YOU" && e.filter.controllerPredicate != null) "Controller filter rebound by player iteration" else null),e.tree(),frame.copy(objectRestrictions=frame.objectRestrictions+if(e.excludeSource) listOf("EXCLUDES_SOURCE") else emptyList()),frame.actor != "YOU" || removals(rootEffect).isNotEmpty(),
                sacrificeChoice=e.takeIf { it === rootEffect && !it.any && it.count > 0 }?.let { SacrificeChoice(it.count,it.excludeSource) })
            SacrificeSelfEffect -> selfSacrifice(path,"EFFECT",e,e.tree(),frame,
                supportedArrival=frame.directBattlefieldStepTrigger && e === rootEffect && path.endsWith(".effect") && !frame.delayed,
                incidental=removals(rootEffect).isNotEmpty())
            is SacrificeTargetEffect -> {
                val obj=targetScope(e.target,frame,selfCreature)
                val bound=e.target as? EffectTarget.PipelineTarget
                val actorScope=if(e.sacrificedByItsController) obj else obj.copy(actor=if(obj.actor=="ANY") "YOU" else obj.actor)
                // ANY includes our legal choice; OPPONENT remains incompatible. Never erase the
                // native controller filter or an unresolved binding to manufacture own control.
                val assumptions=if(bound==null) emptyList() else listOf("Pipeline target ${bound.collectionName}[${bound.index}] must exist, actually have entered/remained on battlefield under the required controller, and still be the same game object; leaving and returning does not preserve a delayed obligation")
                add(path,"SACRIFICE","EFFECT",actorScope,e.tree(),frame.copy(assumptions=frame.assumptions+assumptions),true)
                if(bound!=null) support(path,e,if(obj.unresolved==null && obj.compatible(Requirements())!=Verdict.UNKNOWN) SupportStatus.SUPPORTED else SupportStatus.PARTIALLY_SUPPORTED,"Indexed collection binding retains filter, controller, current zone, cardinality and survival assumptions")
            }
            is ForceSacrificeEffect -> {
                val a=playerScope(e.target,frame)
                add(path,"SACRIFICE","EFFECT",ObjectScope(e.filter,a),e.tree(),frame,incidental=a != "YOU")
                recordPlayerOperation(e, path, frame, facts.last())
            }
            is LoseLifeEffect, is GainLifeEffect -> {
                val operation = recordPlayerOperation(e, path, frame)
                if (operation != null && operation.recipient != "UNKNOWN" && operation.positiveQuantity != Verdict.UNKNOWN)
                    support(path, e, SupportStatus.SUPPORTED, "Direct life change retains its native player and fixed amount")
                else issue(path, e, "Effect not interpreted; native action retained (may contain further capabilities)")
            }
            is ReflexiveTriggerEffect -> {
                if(actionKind=="GRANTED_TRIGGERED") issue(path,e,"Reflexive continuation is a separate trigger; source/granter references retained without composing the triggers",SupportStatus.PARTIALLY_SUPPORTED)
                walk(e.action,"$path.action",frame.copy(collectionBindings=frame.collectionBindings.toMutableMap(),returnGuard=Verdict.UNKNOWN,outcomeGuard=Verdict.UNKNOWN,assumptions=frame.assumptions+"Reflexive action optional=${e.optional}",relationship="REFLEXIVE_ACTION_ENABLES_OUTCOME"))
                walk(e.reflexiveEffect,"$path.reflexiveEffect",frame.copy(rootTargetSelection=false,collectionBindings=mutableMapOf(),returnGuard=Verdict.UNKNOWN,outcomeGuard=Verdict.UNKNOWN,targets=e.reflexiveTargetRequirements,
                    assumptions=frame.assumptions+"Reflexive trigger after successful action"))
            }
            is CreateDelayedTriggerEffect -> walk(e.effect,"$path.effect",frame.copy(collectionBindings=frame.collectionBindings.toMutableMap(),returnGuard=Verdict.UNKNOWN,assumptions=frame.assumptions+"Delayed trigger: $e",delayed=true))
            else -> {
                val r=removal(e)
                if(r != null && r.target != null) {
                    val obj=targetScope(r.target,frame,selfCreature)
                    if(r.operation != "EXILE_REMOVAL") add(path,r.operation,"EFFECT",obj,e.tree(),
                        frame.copy(assumptions=frame.assumptions+r.prerequisites+when(r.operation) {
                            "DAMAGE_POTENTIALLY_LETHAL" -> "Damage amount must be sufficient after prevention; indestructibility and replacement effects matter"
                            "TOUGHNESS_REDUCTION" -> "Toughness must reach zero or damage become lethal in the actual game state"
                            "DESTROY" -> "Destruction must succeed (indestructible/regeneration/protection may prevent it)"
                            else -> "Zone move must actually be from the battlefield to graveyard"
                        }),operationCompatibility=r.confidence)
                    val dynamic=when(e) { is DealDamageEffect -> e.amount; is ModifyStatsEffect -> e.toughnessModifier; else -> null }
                    if(dynamic is DynamicAmount.Conditional) {
                        val yes=(dynamic.ifTrue as? DynamicAmount.Fixed)?.amount
                        val no=(dynamic.ifFalse as? DynamicAmount.Fixed)?.amount
                        val better=if(yes == null || no == null) Verdict.UNKNOWN else if(
                            if(e is ModifyStatsEffect) yes < no else yes > no) Verdict.MATCH else Verdict.NO_MATCH
                        enhanced("$path.dynamicAmount.condition",dynamic.condition.toString(),deathDependency(dynamic.condition),r,frame,better)
                    }
                } else if(e !is DrawCardsEffect && e !is AddManaEffect && e !is AddColorlessManaEffect && e !is ScryEffect && e !is CantBeRegeneratedEffect)
                    issue(path,e,"Effect not interpreted; native action retained (may contain further capabilities)")
            }
        }
        // Inspect only facts emitted by this instruction, rather than rescanning the catalog's
        // growing fact list for every collection stage and consumer.
        val movements=facts.subList(instructionFactStart,facts.size).filter {
            it.actionId==actionId && it.path==path && it.kind==FactKind.MOVE_OBJECT
        }.map { it.id }
        traceCollectionProduction(e,path,frame,movements)
        val collectionAction=actions.indexOfLast { it.id==actionId }
        if(collectionAction>=0 && actions[collectionAction].collectionUses.any { it.path==path }) {
            actions[collectionAction]=actions[collectionAction].copy(collectionUses=actions[collectionAction].collectionUses.map { if(it.path==path) it.copy(movementFactIds=movements) else it })
        }
    }
}
