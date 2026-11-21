package dev.mtg.assistant

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.effects.linkedexile.LinkedExileLookup
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.*
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.*
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*

class CollectionMovementScenarioTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun card(name: String)=checkNotNull(catalog.card(name))
    fun GameTestDriver.cast(name: String,target: EntityId?=null): EntityId {
        Color.entries.forEach { giveMana(player1,it,10) }
        val id=putCardInHand(player1,name)
        submit(CastSpell(player1,id,target?.let { listOf(ChosenTarget.Permanent(it)) } ?: emptyList(),paymentStrategy=PaymentStrategy.FromPool)).error shouldBe null
        return id
    }
    fun GameTestDriver.training(source: EntityId): ChooseTargetsDecision {
        val partner=putCreatureOnBattlefield(player1,"Hill Giant")
        removeSummoningSickness(source);removeSummoningSickness(partner)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1,listOf(source,partner),player2).error shouldBe null
        repeat(20) {
            if(pendingDecision !is ChooseTargetsDecision) {
                if(pendingDecision==null) bothPass().error shouldBe null else autoResolveDecision()
            }
        }
        return pendingDecision as ChooseTargetsDecision
    }
    for(graveyard in listOf(false,true)) test("native optional union selects the opponent's creature from the correct zone: graveyard=$graveyard") {
        val d=scenarioDriver(card("Savior of Ollenbock"),card("Boomerang"))
        val source=d.cast("Savior of Ollenbock");d.resolvePendingActions()
        val victim=if(graveyard) d.putCardInGraveyard(d.player2,"Hill Giant") else d.putCreatureOnBattlefield(d.player2,"Hill Giant")
        val original=checkNotNull(d.state.objectRef(victim))
        val actualZone=d.state.logicalZone(victim)?.zoneType
        val choice=d.training(source)
        choice.targetRequirements.single().minTargets shouldBe 0
        choice.legalTargets[0].orEmpty().contains(victim) shouldBe true
        choice.legalTargets[0].orEmpty().contains(source) shouldBe false
        d.submitTargetSelection(d.player1,listOf(victim)).error shouldBe null
        d.resolvePendingActions()
        d.getExile(d.player2).contains(victim) shouldBe true
        LinkedExileLookup.exiledCards(d.state,source) shouldBe listOf(victim)
        val exile=d.events.filterIsInstance<ZoneChangeEvent>().single { it.oldObject==original && it.toZone==Zone.EXILE }
        exile.fromZone shouldBe actualZone
        (exile.newObject!=original) shouldBe true
        d.cast("Boomerang",source);d.resolvePendingActions()
        d.getController(victim) shouldBe d.player2
        d.state.isCurrentObject(original) shouldBe false
        println("COLLECTION_NATIVE "+buildJsonObject {
            put("case","union-$graveyard");put("engine",EngineBuild.revision)
            put("original",reportJson.encodeToJsonElement(ObjectRef.serializer(),original))
            put("exile",reportJson.encodeToJsonElement(ZoneChangeEvent.serializer(),exile))
            put("result",reportJson.encodeToJsonElement(ObjectRef.serializer(),checkNotNull(d.state.objectRef(victim))))
        })
    }
    test("optional zero-target training has no selected-object movement and invalid noncreatures are rejected") {
        val d=scenarioDriver(card("Savior of Ollenbock"))
        val source=d.cast("Savior of Ollenbock");d.resolvePendingActions()
        val land=d.putLandOnBattlefield(d.player2,"Forest")
        val graveLand=d.putCardInGraveyard(d.player2,"Forest")
        val choice=d.training(source)
        choice.legalTargets[0].orEmpty().contains(land) shouldBe false
        choice.legalTargets[0].orEmpty().contains(graveLand) shouldBe false
        (d.submitTargetSelection(d.player1,listOf(land)).error!=null) shouldBe true
        val mark=d.events.size
        d.submitTargetSelection(d.player1,emptyList()).error shouldBe null
        d.resolvePendingActions()
        LinkedExileLookup.exiledCards(d.state,source) shouldBe emptyList()
        d.events.drop(mark).filterIsInstance<ZoneChangeEvent>().none { it.toZone==Zone.EXILE } shouldBe true
    }
    test("a union target leaving its chosen zone before resolution is not reidentified by its raw ID") {
        val d=scenarioDriver(card("Savior of Ollenbock"),card("Boomerang"))
        val source=d.cast("Savior of Ollenbock");d.resolvePendingActions()
        val victim=d.putCreatureOnBattlefield(d.player2,"Hill Giant")
        d.training(source)
        d.submitTargetSelection(d.player1,listOf(victim)).error shouldBe null
        d.cast("Boomerang",victim);d.resolvePendingActions()
        d.getHand(d.player2).contains(victim) shouldBe true
        LinkedExileLookup.exiledCards(d.state,source) shouldBe emptyList()
    }
    test("Cryptic Coat attaches to the actual cloaked original with a new battlefield generation") {
        val d=scenarioDriver(card("Cryptic Coat"))
        val top=d.putCardOnTopOfLibrary(d.player1,"Forest")
        val original=checkNotNull(d.state.objectRef(top))
        val mark=d.events.size
        val coat=d.cast("Cryptic Coat");d.resolvePendingActions()
        d.state.getEntity(top)?.has<FaceDownComponent>() shouldBe true
        d.state.getEntity(coat)?.get<AttachedToComponent>()?.targetId shouldBe top
        d.state.isCurrentObject(original) shouldBe false
        val stage=PathwayExtractor().extract(listOf(card("Cryptic Coat"))).actions.flatMap { it.collectionStages }
            .single { it.membership==CollectionMembership.NATIVE_MOVED_RESULT }
        val observed=stage.observeMovement(d.state,listOf(original),d.events.drop(mark))
        observed.arrived shouldBe listOf(checkNotNull(d.state.objectRef(top)))
        observed.verdict shouldBe Verdict.MATCH
        stage.observeMovement(d.state,listOf(ObjectRef(top,original.generation+100)),d.events.drop(mark)).arrived shouldBe emptyList()
        println("COLLECTION_NATIVE "+reportJson.encodeToString(CollectionMovementObservation.serializer(),observed))
    }
    test("empty library creates no cloaked member or attachment") {
        val d=scenarioDriver(card("Cryptic Coat"))
        // Explicit fixture: empty library, before casting the real Equipment.
        val key=ZoneKey(d.player1,Zone.LIBRARY)
        d.replaceState(d.state.copy(zones=d.state.zones+(key to emptyList())))
        val coat=d.cast("Cryptic Coat");d.resolvePendingActions()
        d.state.getEntity(coat)?.get<AttachedToComponent>() shouldBe null
        d.getPermanents(d.player1).count { d.state.getEntity(it)?.has<FaceDownComponent>()==true } shouldBe 0
    }
    for(indestructible in listOf(false,true)) test("Come Back Wrong only returns the successfully destroyed original: indestructible=$indestructible") {
        val d=scenarioDriver(card("Come Back Wrong"),card("Darksteel Myr"))
        val victim=d.putCreatureOnBattlefield(d.player2,if(indestructible) "Darksteel Myr" else "Hill Giant")
        val old=checkNotNull(d.state.objectRef(victim));val mark=d.events.size
        d.cast("Come Back Wrong",victim);d.resolvePendingActions()
        d.getController(victim) shouldBe if(indestructible) d.player2 else d.player1
        d.state.isCurrentObject(old) shouldBe indestructible
        val moves=d.events.drop(mark).filterIsInstance<ZoneChangeEvent>().filter { it.entityId==victim }
        moves.map { it.toZone } shouldBe if(indestructible) emptyList() else listOf(Zone.GRAVEYARD,Zone.BATTLEFIELD)
    }
    test("native moved-result excludes failed members in a mixed destruction batch; original input stays separate") {
        val d=scenarioDriver(card("Darksteel Myr"))
        val ordinary=d.putCreatureOnBattlefield(d.player2,"Hill Giant")
        val protected=d.putCreatureOnBattlefield(d.player2,"Darksteel Myr")
        val refs=listOf(ordinary,protected).map { checkNotNull(d.state.objectRef(it)) }
        val context=EffectContext(sourceId=null,controllerId=d.player1,pipeline=PipelineState(storedCollections=mapOf("input" to listOf(ordinary,protected))))
        val effect=MoveCollectionEffect("input",CardDestination.ToZone(Zone.GRAVEYARD),moveType=MoveType.Destroy,storeMovedAs="moved")
        // Isolated executor control supplements the real-card cast above; it is not a card scenario.
        val result=d.services.effectExecutorRegistry.execute(d.state,effect,context)
        result.updatedCollections["moved"] shouldBe listOf(ordinary)
        context.pipeline.storedCollections["input"] shouldBe listOf(ordinary,protected)
        result.newState.getBattlefield().contains(protected) shouldBe true
        val stage=CollectionStage("isolated","isolated","isolated","moved",CollectionMembership.NATIVE_MOVED_RESULT,null,
            maximum=2,sourceZone=Zone.BATTLEFIELD,destinationZone=Zone.GRAVEYARD,movementFactIds=emptyList(),
            prerequisites=emptyList(),support=Verdict.MATCH,unresolved=emptyList())
        val observation=stage.observeMovement(result.newState,refs,result.events,result.updatedCollections["moved"])
        observation.arrived.map { it.entityId } shouldBe listOf(ordinary)
        observation.verdict shouldBe Verdict.MATCH
        val next=context.copy(pipeline=context.pipeline.copy(storedCollections=context.pipeline.storedCollections+result.updatedCollections),
            objectReferences=context.objectReferences.authorize(result.events))
        val alias=d.services.effectExecutorRegistry.execute(result.state,GatherCardsEffect(CardSource.FromVariable("moved"),"later"),next)
        alias.updatedCollections["later"] shouldBe listOf(ordinary)
        println("COLLECTION_NATIVE "+reportJson.encodeToString(CollectionMovementObservation.serializer(),observation))
    }
    test("Kheru's paid native upkeep grants abilities to its actual random-one returned original") {
        val d=scenarioDriver(card("Kheru Lich Lord"))
        d.putCreatureOnBattlefield(d.player1,"Kheru Lich Lord")
        val creature=d.putCardInGraveyard(d.player1,"Hill Giant")
        val noncreature=d.putCardInGraveyard(d.player1,"Forest")
        val old=checkNotNull(d.state.objectRef(creature))
        // Two turns consume the current pool; payment is supplied at the actual upkeep checkpoint.
        d.passPriorityUntil(Step.UPKEEP,d.player1)
        d.giveMana(d.player1,Color.BLACK,3)
        d.resolvePendingActions(accept=true)
        d.getPermanents(d.player1).contains(creature) shouldBe true
        d.getGraveyard(d.player1).contains(noncreature) shouldBe true
        d.state.isCurrentObject(old) shouldBe false
        for(keyword in listOf(Keyword.FLYING,Keyword.TRAMPLE,Keyword.HASTE))
            d.state.projectedState.hasKeyword(creature,keyword) shouldBe true
    }
    test("movement's positive filter produces only matching actual members") {
        val d=scenarioDriver()
        val creature=d.putCreatureOnBattlefield(d.player1,"Hill Giant")
        val land=d.putLandOnBattlefield(d.player1,"Forest")
        val context=EffectContext(sourceId=null,controllerId=d.player1,pipeline=PipelineState(storedCollections=mapOf("input" to listOf(creature,land))))
        val effect=MoveCollectionEffect("input",CardDestination.ToZone(Zone.EXILE),filter=com.wingedsheep.sdk.scripting.GameObjectFilter.Creature,storeMovedAs="moved")
        val result=d.services.effectExecutorRegistry.execute(d.state,effect,context)
        result.updatedCollections["moved"] shouldBe listOf(creature)
        result.state.getBattlefield().contains(land) shouldBe true
    }
})
