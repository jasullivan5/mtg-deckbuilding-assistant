package dev.mtg.assistant.discovery

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.conditions.*
import com.wingedsheep.sdk.scripting.costs.*
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.json.*

private fun Effect.tree() = nativeJson.encodeToJsonElement(Effect.serializer(), this)
private data class Frame(val targets: List<TargetRequirement>, val tokenController: String? = null, val tokenEvent: String? = null, val actor: String = "YOU",
    val iteration: ObjectScope? = null, val assumptions: List<String> = emptyList(),
    val movementTargets: MutableMap<String,List<ObjectScope>> = mutableMapOf(),
    val collections: MutableMap<String,ObjectScope> = mutableMapOf(), val collectionMax: MutableMap<String,Int?> = mutableMapOf(), val guardConfidence: Verdict = Verdict.MATCH, val factGuard: Verdict = Verdict.MATCH, val resolutionCost: Boolean = false, val selfZone: Zone? = Zone.BATTLEFIELD, val relationship: String = "UNLINKED_RESOLUTION", val optional: Boolean = false, val delayed: Boolean = false, val objectRestrictions: List<String> = emptyList())
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
    EffectTarget.Self -> ObjectScope(actor="YOU",printedSelfCreature=selfCreature,battlefield=frame.selfZone == Zone.BATTLEFIELD,zone=frame.selfZone,zonePlayer=if(frame.selfZone==Zone.GRAVEYARD) "ANY" else "UNKNOWN")
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
class PathwayExtractor {
    val facts = mutableListOf<MechanicalFact>()
    val pathways = mutableListOf<Pathway>()
    val actions = mutableListOf<ActionEvidence>()
    val unsupported = mutableListOf<Unsupported>()
    val structuralSupport = mutableListOf<StructuralSupport>()
    private lateinit var card: CardDefinition
    private var face = ""
    private var selfCreature = false
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
        pathways.clear(); actions.clear(); unsupported.clear(); structuralSupport.clear(); facts.clear()
        for(c in cards) { card=c; inspectDefinition(c,"front") }
        return this
    }
    private fun support(path: String, value: Any, status: SupportStatus, reason: String) {
        structuralSupport += StructuralSupport(card.name,face,path,value.javaClass.simpleName,status,reason,actionId)
    }
    private fun issue(path: String, value: Any, reason: String, status: SupportStatus = SupportStatus.UNSUPPORTED) {
        unsupported += Unsupported(card.name,face,path,value.javaClass.name,reason,actionId)
        support(path,value,status,reason)
    }
    private fun add(path: String, operation: String, origin: String, objectScope: ObjectScope,
                    native: JsonElement, frame: Frame, incidental: Boolean = false, operationCompatibility: Verdict = Verdict.MATCH) {
        val kind=when(operation) { "SACRIFICE"->FactKind.SACRIFICE;"DAMAGE_POTENTIALLY_LETHAL"->FactKind.DAMAGE;"TOUGHNESS_REDUCTION"->FactKind.TOUGHNESS_CHANGE;else->FactKind.OTHER_OPERATION }
        val id="$actionId|$path|$operation|${facts.count { it.actionId==actionId && it.path==path && it.operation==operation }}"
        facts += MechanicalFact(id,card.name,face,actionId,path,kind,operation,if(frame.resolutionCost && origin=="EFFECT") "RESOLUTION_COST" else origin,
            objectScope.filter,sourceZone=objectScope.zone ?: if(objectScope.battlefield) Zone.BATTLEFIELD else null,sourcePlayer=objectScope.actor,
            confidence=all(listOf(operationCompatibility,frame.guardConfidence)),native=native,prerequisites=frame.assumptions,
            circumstances=circumstances(origin,objectScope,frame),unresolved=listOfNotNull(objectScope.unresolved))
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
        inspectScript(def.script,label,def.isCreature,def.keywordAbilities)
        def.backFace?.let { inspectDefinition(it,"$label.backFace") }
        def.flipSide?.let { inspectDefinition(it,"$label.flipSide") }
        def.cardFaces.forEachIndexed { i,f -> inspectScript(f.script,"$label.cardFaces[$i]:${f.name}",f.typeLine.isCreature) }
    }
    private fun inspectScript(script: CardScript, label: String, creature: Boolean, keywordAbilities: List<KeywordAbility> = emptyList()) {
        face=label; selfCreature=creature; baseCastingCosts=script.additionalCosts
        actionId="${card.name}|$label|script"
        actions += ActionEvidence(actionId,card.name,label,"script",emptyList(),nativeJson.encodeToJsonElement(CardScript.serializer(),script))
        fun action(path: String, value: JsonElement, effect: Effect?, targets: List<TargetRequirement>,
                   assumptions: List<String>, sourceZone: Zone? = Zone.BATTLEFIELD, costs: () -> Unit = {}) {
            actionId="${card.name}|$label|$path"; rootEffect=effect; actionNative=value
            actionKind=when { path.contains("activatedAbilities") -> "ACTIVATED"; path.contains("stateTriggeredAbilities") -> "STATE_TRIGGERED"; path.contains("triggeredAbilities") -> "TRIGGERED"; path == "script.spell" -> "SPELL"; else -> "OTHER" }
            actions += ActionEvidence(actionId,card.name,label,path,assumptions,value)
            val frame=Frame(targets,assumptions=assumptions,selfZone=sourceZone)
            costs()
            effect?.let { walk(it,if(path == "script.spell") "script.spellEffect" else "$path.effect",frame) }
        }
        val castAssumptions=listOf("Cast timing, mana and additional costs, cast restrictions and legal targets from native script")
        if(script.spellEffect != null || script.additionalCosts.isNotEmpty() || script.selfAlternativeCost != null) action("script.spell",
            nativeJson.encodeToJsonElement(CardScript.serializer(),script),script.spellEffect,script.targetRequirements,castAssumptions,sourceZone=Zone.STACK) {
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
        fun activated(ability: ActivatedAbility,path: String, extra: List<String> = emptyList()) {
            val frame=Frame(ability.targetRequirements,selfZone=ability.activateFromZone,assumptions=extra + "Timing=${ability.timing}; restrictions=${ability.restrictions}; activateFrom=${ability.activateFromZone}")
            action(path,nativeJson.encodeToJsonElement(ActivatedAbility.serializer(),ability),ability.effect,frame.targets,frame.assumptions,sourceZone=ability.activateFromZone) {
                abilityCost(ability.cost,"$path.cost",frame)
            }
            ability.conditionalCostReduction?.let { reduction ->
                removals(ability.effect).forEach { enhanced("$path.conditionalCostReduction", reduction.condition.description + "; cost reduced by ${reduction.reduction}",deathDependency(reduction.condition),it,frame,
                    if(reduction.reduction.cmc > 0) Verdict.MATCH else Verdict.UNKNOWN) }
            }
            ability.genericCostReduction?.let { issue("$path.genericCostReduction",it,"Dynamic discount not interpreted") }
        }
        fun triggered(ability: TriggeredAbility,path: String,extra: List<String> = emptyList()) {
            val assumptions=extra + listOf("Trigger=${ability.trigger}; binding=${ability.binding}; activeZones=${ability.activeZones}",
                "InterveningIf=${ability.interveningIf}; restriction=${ability.triggerRestriction}; cap=${ability.perTurnTriggerCap}")
            val trigger=ability.trigger
            val sourceZone=if(trigger is EventPattern.ZoneChangeEvent && ability.binding == TriggerBinding.SELF) trigger.to else Zone.BATTLEFIELD
            action(path,nativeJson.encodeToJsonElement(TriggeredAbility.serializer(),ability),ability.effect,ability.allTargetRequirements,assumptions,sourceZone=sourceZone)
            if(trigger is EventPattern.ZoneChangeEvent && trigger.from == Zone.BATTLEFIELD && trigger.to == Zone.GRAVEYARD) {
                var eventScope=ObjectScope(trigger.filter)
                if(ability.binding == TriggerBinding.SELF) eventScope=eventScope.copy(actor="YOU",printedSelfCreature=creature)
                if(ability.binding == TriggerBinding.ATTACHED) eventScope=eventScope.copy(unresolved="Attached death-event binding not resolved against aura/attachment restrictions")
                if(ability.controlledByTriggeringEntityController) eventScope=eventScope.copy(unresolved="Trigger controller rebound to event entity")
                removals(ability.effect).forEach { enhanced("$path.trigger",trigger.toString(),Verdict.MATCH to eventScope,it,Frame(ability.allTargetRequirements,assumptions=assumptions,selfZone=sourceZone),Verdict.MATCH) }
            }
            ability.elseEffect?.let { walk(it,"$path.elseEffect",Frame(ability.allTargetRequirements,assumptions=assumptions+"Intervening condition failed",selfZone=sourceZone)) }
        }
        script.activatedAbilities.forEachIndexed { i,a -> activated(a,"script.activatedAbilities[$i]") }
        script.triggeredAbilities.forEachIndexed { i,a -> triggered(a,"script.triggeredAbilities[$i]") }
        script.stateTriggeredAbilities.forEachIndexed { i,a -> action("script.stateTriggeredAbilities[$i]",
            nativeJson.encodeToJsonElement(StateTriggeredAbility.serializer(),a),a.effect,emptyList(),listOf("State trigger condition=${a.condition}; zone=${a.activeZone}")) }
        script.sagaChapters.forEachIndexed { i,a -> action("script.sagaChapters[$i]",nativeJson.encodeToJsonElement(SagaChapterAbility.serializer(),a),a.effect,
            listOfNotNull(a.targetRequirement)+a.additionalTargetRequirements,listOf("Saga chapter ${a.chapter}")) }
        script.classLevels.forEachIndexed { i,level ->
            level.activatedAbilities.forEachIndexed { j,a -> activated(a,"script.classLevels[$i].activatedAbilities[$j]",listOf("Class level >= ${level.level}; level-up cost=${level.cost}")) }
            level.triggeredAbilities.forEachIndexed { j,a -> triggered(a,"script.classLevels[$i].triggeredAbilities[$j]",listOf("Class level >= ${level.level}")) }
            if(level.staticAbilities.isNotEmpty() || level.replacementEffects.isNotEmpty()) issue("script.classLevels[$i]",level,"Level static/replacement abilities not expanded; see face script evidence")
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
        }
        actionId="${card.name}|$label|script.unsupported"
        val scriptTree=nativeJson.encodeToJsonElement(CardScript.serializer(),script).jsonObject
        val handled=setOf("spellEffect","additionalCosts","targetRequirements","activatedAbilities","triggeredAbilities","stateTriggeredAbilities","sagaChapters","classLevels","staticAbilities","selfAlternativeCost")
        val remainder=JsonObject(scriptTree.filterKeys { it !in handled })
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
    }
    private fun abilityCost(c: AbilityCost,path: String,frame: Frame) {
        when(c) {
            is AbilityCost.Composite -> c.costs.forEachIndexed { i,child -> abilityCost(child,"$path.costs[$i]",frame) }
            is AbilityCost.Atom -> atom(c.atom,"$path.atom","ACTIVATION_COST",frame)
            AbilityCost.SacrificeSelf -> add(path,"SACRIFICE","ACTIVATION_COST",ObjectScope(actor="YOU",printedSelfCreature=selfCreature,battlefield=frame.selfZone == Zone.BATTLEFIELD,zone=frame.selfZone,zonePlayer=if(frame.selfZone==Zone.GRAVEYARD) "ANY" else "UNKNOWN"),nativeJson.encodeToJsonElement(AbilityCost.serializer(),c),frame)
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
            add(path,"SACRIFICE",origin,ObjectScope(filter,frame.actor,battlefield=!zero),nativeJson.encodeToJsonElement(CostAtom.serializer(),c),frame.copy(assumptions=frame.assumptions+constraints))
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
        binding: String?=null,selection: String="Native target requirement",extra: List<String> = emptyList(),confidence: Verdict=Verdict.MATCH) {
        val problems=listOfNotNull(obj.unresolved)+extra
        val filter=if(obj.printedSelfCreature==true) intersect(obj.filter,GameObjectFilter.Creature) else if(obj.printedSelfCreature==false) intersect(obj.filter,GameObjectFilter.Any.copy(cardPredicates=listOf(com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNoncreature))) else obj.filter
        facts += MechanicalFact("$actionId|$path|MOVE_OBJECT|${facts.count { it.actionId==actionId && it.path==path && it.kind==FactKind.MOVE_OBJECT }}",
            card.name,face,actionId,path,FactKind.MOVE_OBJECT,"MOVE_OBJECT","EFFECT",filter,obj.zone,obj.zonePlayer,destination,controller,
            all(listOf(confidence,frame.guardConfidence,frame.factGuard,if((actionNative as? JsonObject)?.get("controlledByTriggeringEntityController")==JsonPrimitive(true)) Verdict.UNKNOWN else Verdict.MATCH,if(obj.possible) Verdict.MATCH else Verdict.NO_MATCH)),frame.assumptions+"Only actual successful movement; legal choices/resources and no replacing effects assumed"+obj.details,
            native,binding,selection,circumstances("EFFECT",obj,frame),problems,ownerScope=obj.ownerScope)
    }
    private fun recordTargetMove(e: MoveToZoneEffect,path: String,frame: Frame) {
        val requirement=when(val t=e.target) { is EffectTarget.ContextTarget->frame.targets.getOrNull(t.index);is EffectTarget.BoundVariable->frame.targets.find { it.id==t.name };else->null }
        val alternatives=(requirement as? TargetObject)?.filter?.clauses()
        val bindingKey=requirement?.id?.let { "target:$it" } ?: e.target.toString()
        val objects=frame.movementTargets[bindingKey] ?: alternatives?.map { scope(it) } ?: listOf(targetScope(e.target,frame,selfCreature))
        objects.forEach { raw ->
            val incompatible=e.fromZone!=null && raw.zone!=null && e.fromZone!=raw.zone
            val obj=raw.copy(zone=e.fromZone ?: raw.zone,zonePlayer=if(e.target==EffectTarget.Self && actionKind=="ACTIVATED" && frame.selfZone==Zone.GRAVEYARD) "YOU" else raw.zonePlayer)
            val controller=if(e.controllerOverride==null) "OWNER" else playerScope(e.controllerOverride!!,frame)
            recordMovement(path,e.tree(),obj,e.destination,controller,frame,binding=e.target.toString(),
                selection=requirement?.let { "targeted=true; count=${it.count}; min=${it.minCount}; optional=${it.optional}; chooser=${it.chooser}; native=$it" } ?: "Binding=${e.target}; selection not inferred as targeting",
                confidence=if(incompatible || requirement?.count==0) Verdict.NO_MATCH else Verdict.MATCH,
                extra=listOfNotNull(if(e.faceDown!=null) "Face-down arrival unresolved" else null))
        }
        // The same bound target is not still in its original zone after an earlier move.
        // Model only successful arrivals; actual replacement/failure remains an assumption.
        frame.movementTargets[bindingKey]=objects.map { obj -> obj.copy(zone=e.destination,
            zonePlayer=if(e.destination==Zone.BATTLEFIELD) "UNKNOWN" else "ANY",
            battlefield=e.destination==Zone.BATTLEFIELD,
            unresolved=listOfNotNull(obj.unresolved,"Later reference requires the same tracked card and successful earlier movement; original targeting restrictions are retained").joinToString("; ")) }
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
                creature=copiedType(e.copyExceptions,if(selfCreature) Verdict.MATCH else Verdict.NO_MATCH)
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
        val partial=confidence==Verdict.UNKNOWN || creature==Verdict.UNKNOWN || controller=="UNKNOWN" || positive==Verdict.UNKNOWN || unresolved.isNotEmpty()
        support(path,e,if(partial) SupportStatus.PARTIALLY_SUPPORTED else SupportStatus.SUPPORTED,
            "Token variant retains correlated template/controller/quantity and native action; granted token abilities are retained but not recursively claimed as source-card actions")
        if(partial) issue("$path.tokenConstraints",e,"Unresolved token variant: creature=$creature; controller=$controller; quantity=$positive; $unresolved",SupportStatus.PARTIALLY_SUPPORTED)
        return true
    }
    private fun walk(e: Effect,path: String,frame: Frame) {
        if(recordToken(e,path,frame)) return
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
                    val branch=frame.copy(assumptions=frame.assumptions+"$reason; measured size >= threshold",movementTargets=frame.movementTargets.toMutableMap(),collections=frame.collections.toMutableMap(),collectionMax=frame.collectionMax.toMutableMap(),guardConfidence=all(listOf(frame.guardConfidence,if(known) Verdict.MATCH else Verdict.UNKNOWN)))
                    walk(e.ifNotEmpty,"$path.ifNotEmpty",branch)
                }
                if(noPossible) e.ifEmpty?.let {
                    val branch=frame.copy(assumptions=frame.assumptions+"$reason; measured size < threshold",movementTargets=frame.movementTargets.toMutableMap(),collections=frame.collections.toMutableMap(),collectionMax=frame.collectionMax.toMutableMap(),guardConfidence=all(listOf(frame.guardConfidence,if(known) Verdict.MATCH else Verdict.UNKNOWN)))
                    if(raw) branch.collectionMax[e.collection]=minOf(max ?: Int.MAX_VALUE,e.minSize-1)
                    walk(it,"$path.ifEmpty",branch)
                }
            }
            is PayOrSufferEffect -> {
                val payer=playerScope(e.player,frame)
                payCost(e.cost,"$path.cost",frame.copy(actor=payer,resolutionCost=true,optional=true,
                    assumptions=frame.assumptions+"Optional native payment by $payer; this branch avoids the suffer outcome"))
                walk(e.suffer,"$path.suffer",frame.copy(assumptions=frame.assumptions+"Native PayOrSuffer: payment ${e.cost} by $payer was not made; failure branch executes"))
            }
            is CompositeEffect -> {
                val local=frame.copy(movementTargets=frame.movementTargets.toMutableMap(),collections=frame.collections.toMutableMap(),collectionMax=frame.collectionMax.toMutableMap())
                e.effects.forEachIndexed { i,child -> walk(child,"$path.effects[$i]",local) }
            }
            is GatedEffect -> {
                val condition=(e.gate as? Gate.WhenCondition)?.condition
                val assumptions=frame.assumptions+"Gate=${nativeJson.encodeToJsonElement(Gate.serializer(),e.gate)}; success branch"
                val branch=frame.copy(factGuard=if(condition!=null) Verdict.UNKNOWN else frame.factGuard,assumptions=assumptions,movementTargets=frame.movementTargets.toMutableMap(),collections=frame.collections.toMutableMap(),collectionMax=frame.collectionMax.toMutableMap())
                when(val gate=e.gate) {
                    is Gate.DoAction -> walk(gate.action,"$path.gate.action",branch.copy(assumptions=frame.assumptions+"Action outcome gates associated payoff: ${gate.successCriterion}",relationship=if(gate.successCriterion is SuccessCriterion.PermanentsSacrificed) "SACRIFICE_SUCCESS_ENABLES_OUTCOME" else "UNRESOLVED_SUCCESS_DEPENDENCY"))
                    is Gate.MayPay -> walk(gate.cost,"$path.gate.cost",branch.copy(assumptions=frame.assumptions+"Optional resolution payment",resolutionCost=true))
                    is Gate.WhenCondition, is Gate.MayDecide -> Unit
                    else -> issue("$path.gate",gate,"Gate payment/decision semantics not interpreted")
                }
                walk(e.then,"$path.then",branch.copy(optional=frame.optional || e.gate is Gate.MayDecide || e.gate is Gate.MayPay))
                e.otherwise?.let { walk(it,"$path.otherwise",frame.copy(factGuard=if(condition!=null) Verdict.UNKNOWN else frame.factGuard,assumptions=frame.assumptions+"Gate failed: ${e.gate}",movementTargets=frame.movementTargets.toMutableMap(),collections=frame.collections.toMutableMap(),collectionMax=frame.collectionMax.toMutableMap())) }
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
                val branch=frame.copy(targets=frame.targets+m.targetRequirements,
                    assumptions=frame.assumptions+"Mode $i selected; chooseCount=${e.chooseCount}; mode mana=${m.additionalManaCost}; extra-mode cost=${e.additionalCostPerExtraMode}",movementTargets=frame.movementTargets.toMutableMap(),collections=frame.collections.toMutableMap(),collectionMax=frame.collectionMax.toMutableMap())
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
                val child=next.copy(movementTargets=frame.movementTargets.toMutableMap(),collections=frame.collections.toMutableMap(),collectionMax=frame.collectionMax.toMutableMap(),assumptions=frame.assumptions+"Iteration=${e.space}")
                if(e.space is IterationSpace.Players) { child.collections.clear();child.collectionMax.clear() }
                walk(e.body,"$path.body",child)
            }
            is GatherCardsEffect -> {
                frame.collectionMax[e.storeAs]=(e.source as? CardSource.FromVariable)?.let { frame.collectionMax[it.variableName] }
                frame.collections[e.storeAs]=when(val s=e.source) {
                    is CardSource.BattlefieldMatching -> ObjectScope(s.filter,relativeActor(s.player,frame),zone=Zone.BATTLEFIELD,zonePlayer=relativeActor(s.player,frame),details=listOf("Native source restrictions: $s"))
                    is CardSource.FromZone -> ObjectScope(s.filter,relativeActor(s.player,frame),battlefield=s.zone == Zone.BATTLEFIELD,zone=s.zone,zonePlayer=relativeActor(s.player,frame),ownerScope=if(s.zone in setOf(Zone.GRAVEYARD,Zone.HAND,Zone.LIBRARY)) relativeActor(s.player,frame) else "ANY",details=listOf("Native source restrictions: $s"))
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
                frame.collections[e.storeSelected]=source.copy(filter=intersect(source.filter,e.filter),possible=source.possible && !zero,battlefield=source.battlefield && !zero,details=source.details+"Selection=${e.selection}; chooser=${e.chooser}; filter=${e.filter}; restrictions=${e.restrictions}; chosenType=${e.matchChosenCreatureType}",
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
                frame.collections[e.storeMatching]=source.copy(filter=intersect(source.filter,e.filter),unresolved=if(e.collectionFilter != null) "Collection-relative filter unresolved: ${e.collectionFilter}" else source.unresolved)
                e.storeNonMatching?.let { frame.collections[it]=source.copy(unresolved="Complement collection filter not solved") }
                if(e.collectionFilter != null || e.storeNonMatching != null) issue(path,e,"Positive per-card filter propagated; relative/complement membership unresolved")
            }
            is MoveCollectionEffect -> {
                val source=frame.collections[e.from] ?: ObjectScope(unresolved="Unbound pipeline collection: ${e.from}")
                val obj=if(e.filter==null) source else source.copy(filter=intersect(source.filter,e.filter!!))
                val destination=e.destination as? CardDestination.ToZone
                recordMovement(path,e.tree(),obj,destination?.zone,if(e.underOwnersControl) "OWNER" else destination?.let { relativeActor(it.player,frame) } ?: "UNKNOWN",frame,
                    binding=e.from,selection="collectionMax=${frame.collectionMax[e.from]}; ${obj.details}; operation=${e.moveType}",
                    extra=listOfNotNull(if(e.faceDown!=null) "Face-down arrival characteristics unresolved" else null,if(destination==null) "Dynamic destination unresolved" else null),
                    confidence=if(frame.collectionMax[e.from]==0) Verdict.NO_MATCH else Verdict.MATCH)
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
                    unresolved=restrictions.takeIf { it.isNotEmpty() }?.joinToString("; "))
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
                frame.actor,unresolved=if(frame.actor != "YOU" && e.filter.controllerPredicate != null) "Controller filter rebound by player iteration" else null),e.tree(),frame.copy(objectRestrictions=frame.objectRestrictions+if(e.excludeSource) listOf("EXCLUDES_SOURCE") else emptyList()),frame.actor != "YOU" || removals(rootEffect).isNotEmpty())
            SacrificeSelfEffect -> add(path,"SACRIFICE","EFFECT",ObjectScope(actor="YOU",printedSelfCreature=selfCreature,battlefield=frame.selfZone == Zone.BATTLEFIELD,zone=frame.selfZone,zonePlayer=if(frame.selfZone==Zone.GRAVEYARD) "ANY" else "UNKNOWN"),e.tree(),frame,removals(rootEffect).isNotEmpty())
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
            }
            is ReflexiveTriggerEffect -> {
                walk(e.action,"$path.action",frame.copy(assumptions=frame.assumptions+"Reflexive action optional=${e.optional}",relationship="REFLEXIVE_ACTION_ENABLES_OUTCOME"))
                walk(e.reflexiveEffect,"$path.reflexiveEffect",frame.copy(targets=e.reflexiveTargetRequirements,
                    assumptions=frame.assumptions+"Reflexive trigger after successful action"))
            }
            is CreateDelayedTriggerEffect -> walk(e.effect,"$path.effect",frame.copy(assumptions=frame.assumptions+"Delayed trigger: $e",delayed=true))
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
    }
}
