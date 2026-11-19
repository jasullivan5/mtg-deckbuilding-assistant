package dev.mtg.assistant

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.handlers.effects.linkedexile.LinkedExileLookup
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.EntityId
import dev.mtg.assistant.discovery.*
import kotlinx.serialization.json.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LinkedMovementScenarioTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun card(name: String)=checkNotNull(catalog.card(name))
    fun relation(name: String)=PathwayExtractor().extract(listOf(card(name))).linkedMovements.single()
    fun GameTestDriver.cast(name: String, target: EntityId? = null) {
        giveMana(player1,Color.WHITE,8);giveMana(player1,Color.BLUE,8)
        submit(CastSpell(player1,putCardInHand(player1,name),
            target?.let { listOf(ChosenTarget.Permanent(it)) } ?: emptyList(),
            paymentStrategy=PaymentStrategy.FromPool)).error shouldBe null
    }
    fun GameTestDriver.source(name: String, target: EntityId): EntityId {
        cast(name);bothPass().error shouldBe null
        val choice=pendingDecision as ChooseTargetsDecision
        choice.legalTargets[0].orEmpty().contains(target) shouldBe true
        submitTargetSelection(choice.playerId,listOf(target)).error shouldBe null
        resolvePendingActions()
        return getPermanents(player1).single { state.getEntity(it)?.get<CardComponent>()?.name==name }
    }
    for (name in listOf("Cloudshift","Momentary Blink")) {
        test("$name uses native ordered transitions and renews the selected original card") {
            val d=scenarioDriver(card(name),card("Hill Giant"),card("Holy Strength"),card("Giant Growth"))
            val host=d.putCreatureOnBattlefield(d.player1,"Hill Giant")
            val unrelated=d.putCreatureOnBattlefield(d.player1,"Hill Giant")
            d.cast("Holy Strength",host);d.resolvePendingActions()
            val aura=d.getPermanents(d.player1).single { d.state.getEntity(it)?.get<CardComponent>()?.name=="Holy Strength" }
            d.replaceState(d.state.updateEntity(host) { it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2))).with(DamageComponent(1)) })
            d.giveMana(d.player1,Color.GREEN,1); d.cast("Giant Growth",host);d.resolvePendingActions()
            val old=checkNotNull(d.state.objectRef(host)); val mark=d.events.size
            d.cast(name,host);d.resolvePendingActions()
            val observed=relation(name).observeResolution(d.state,old,d.events.drop(mark))
            println("LINKED_OBSERVATION " + buildJsonObject { put("card",name); put("kind","immediate"); put("observation",reportJson.encodeToJsonElement(MovementObservation.serializer(),observed)) })
            observed.verdict shouldBe Verdict.MATCH
            observed.returnedObject?.entityId shouldBe old.entityId
            (observed.returnedObject?.generation != old.generation) shouldBe true
            d.state.isCurrentObject(old) shouldBe false
            d.state.getEntity(host)?.get<CountersComponent>()?.counters.orEmpty().isEmpty() shouldBe true
            (d.state.getEntity(host)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
            d.state.projectedState.getPower(host) shouldBe 3
            d.state.getGraveyard(d.player1).contains(aura) shouldBe true
            d.getController(host) shouldBe d.player1
            relation(name).observeResolution(d.state,checkNotNull(d.state.objectRef(unrelated)),d.events.drop(mark)).verdict shouldBe Verdict.UNKNOWN
            relation(name).observeResolution(d.state,ObjectRef(old.entityId,old.generation+100),d.events.drop(mark)).verdict shouldBe Verdict.UNKNOWN
        }
        test("$name rejects a land and an opposing creature as targets") {
            val d=scenarioDriver(card(name),card("Hill Giant"))
            for (target in listOf(d.putLandOnBattlefield(d.player1,"Forest"),d.putCreatureOnBattlefield(d.player2,"Hill Giant"))) {
                d.giveMana(d.player1,Color.WHITE,5)
                (d.submit(CastSpell(d.player1,d.putCardInHand(d.player1,name),listOf(ChosenTarget.Permanent(target)),paymentStrategy=PaymentStrategy.FromPool)).error!=null) shouldBe true
            }
        }
        test("$name does not invent a return when the sole target becomes illegal") {
            val d=scenarioDriver(card(name),card("Hill Giant"),card("Unsummon"))
            val host=d.putCreatureOnBattlefield(d.player1,"Hill Giant")
            val old=checkNotNull(d.state.objectRef(host));val mark=d.events.size
            d.cast(name,host);d.cast("Unsummon",host);d.resolvePendingActions()
            d.getHand(d.player1).contains(host) shouldBe true
            relation(name).observeResolution(d.state,old,d.events.drop(mark)).verdict shouldBe Verdict.UNKNOWN
            d.events.drop(mark).filterIsInstance<ZoneChangeEvent>().none { it.entityId==host && it.toZone==Zone.EXILE } shouldBe true
        }
        test("$name exiles a token without returning an original card") {
            val d=scenarioDriver(card(name),card("Hill Giant"))
            val token=d.putCreatureOnBattlefield(d.player1,"Hill Giant")
            d.replaceState(d.state.updateEntity(token) { it.with(TokenComponent) })
            val old=checkNotNull(d.state.objectRef(token)); val mark=d.events.size
            d.cast(name,token);d.resolvePendingActions()
            d.getPermanents(d.player1).contains(token) shouldBe false
            relation(name).observeResolution(d.state,old,d.events.drop(mark)).verdict shouldBe Verdict.NO_MATCH
        }
        test("$name retains its distinct owner versus controller return semantics") {
            val d=scenarioDriver(card(name),card("Hill Giant"),card("Control Magic"))
            val host=d.putCreatureOnBattlefield(d.player2,"Hill Giant")
            d.cast("Control Magic",host);d.resolvePendingActions()
            d.state.projectedState.getController(host) shouldBe d.player1
            d.cast(name,host);d.resolvePendingActions()
            d.getController(host) shouldBe if(name=="Cloudshift") d.player1 else d.player2
        }
    }
    for (name in listOf("Oblivion Ring","Banisher Priest")) {
        test("$name establishes an actual native source pile and departure returns its card") {
            val d=scenarioDriver(card(name),card("Hill Giant"),card("Boomerang"))
            val host=d.putCreatureOnBattlefield(d.player2,"Hill Giant")
            val old=checkNotNull(d.state.objectRef(host))
            val source=d.source(name,host)
            val visit=checkNotNull(captureLinkedSource(d.state,checkNotNull(d.state.objectRef(source))))
            relation(name).linkedExileNow(d.state,visit)?.map { it.entityId } shouldBe listOf(host)
            d.state.getEntity(source)?.get<LinkedExileComponent>()?.exiledIds shouldBe listOf(host)
            d.getExile(d.player2).contains(host) shouldBe true
            val stranger=d.putCreatureOnBattlefield(d.player1,name)
            LinkedExileLookup.exiledCards(d.state,stranger).isEmpty() shouldBe true
            d.cast("Boomerang",source);d.resolvePendingActions()
            d.getPermanents(d.player2).contains(host) shouldBe true
            d.state.isCurrentObject(old) shouldBe false
            d.getController(host) shouldBe d.player2
            relation(name).linkedExileNow(d.state,visit)?.isEmpty() shouldBe true
            println("LINKED_OBSERVATION " + buildJsonObject { put("card",name);put("kind","deferred");put("sourceVisit",reportJson.encodeToJsonElement(LinkedSourceVisit.serializer(),visit));put("startingObject",reportJson.encodeToJsonElement(ObjectRef.serializer(),old));put("returnedObject",reportJson.encodeToJsonElement(ObjectRef.serializer(),checkNotNull(d.state.objectRef(host))));put("returnedUnderOwner",d.getController(host)==d.player2) })
            relation(name).kind shouldBe MovementLinkKind.DEFERRED_SOURCE_LINKED
        }
        test("$name keeps simultaneous same-name sources independent and removes stale exile membership") {
            val d=scenarioDriver(card(name),card("Hill Giant"),card("Boomerang"))
            val a=d.putCreatureOnBattlefield(d.player2,"Hill Giant")
            val b=d.putCreatureOnBattlefield(d.player2,"Hill Giant")
            val first=d.source(name,a)
            d.cast(name);d.bothPass()
            val choice=d.pendingDecision as ChooseTargetsDecision
            d.submitTargetSelection(choice.playerId,listOf(b));d.resolvePendingActions()
            val second=d.getPermanents(d.player1).single { it!=first && d.state.getEntity(it)?.get<CardComponent>()?.name==name }
            LinkedExileLookup.exiledCards(d.state,first) shouldBe listOf(a)
            LinkedExileLookup.exiledCards(d.state,second) shouldBe listOf(b)
            // Native zone service constructs an intervening exit/reentry, without claiming a real spell did it.
            d.replaceState(d.zones.moveToZone(d.state,b,Zone.HAND).state)
            d.replaceState(d.zones.moveToZone(d.state,b,Zone.EXILE).state)
            LinkedExileLookup.exiledCards(d.state,second).isEmpty() shouldBe true
            d.cast("Boomerang",first);d.resolvePendingActions()
            d.getPermanents(d.player2).contains(a) shouldBe true
            d.getExile(d.player2).contains(b) shouldBe true
            d.cast("Boomerang",second);d.resolvePendingActions()
            d.getExile(d.player2).contains(b) shouldBe true
            d.getPermanents(d.player2).contains(b) shouldBe false
            // Recast the same source card: it is a different visit, with no inherited pile.
            d.giveMana(d.player1,Color.WHITE,4)
            d.submit(CastSpell(d.player1,first,paymentStrategy=PaymentStrategy.FromPool)).error shouldBe null
            d.bothPass()
            d.state.getEntity(first)?.get<LinkedExileComponent>() shouldBe null
        }
        test("$name departure before entry-trigger resolution records native timing boundary") {
            val d=scenarioDriver(card(name),card("Hill Giant"),card("Boomerang"))
            val host=d.putCreatureOnBattlefield(d.player2,"Hill Giant")
            d.cast(name);d.bothPass()
            val choice=d.pendingDecision as ChooseTargetsDecision
            d.submitTargetSelection(choice.playerId,listOf(host))
            val source=d.getPermanents(d.player1).single { d.state.getEntity(it)?.get<CardComponent>()?.name==name }
            d.cast("Boomerang",source);d.resolvePendingActions()
            println("LINKED_OBSERVATION " + buildJsonObject { put("card",name);put("kind","departure-before-exile");put("oracleRequiresLateExile",name=="Oblivion Ring");put("observedLateExile",d.getExile(d.player2).contains(host));put("sourceGone",!d.getPermanents(d.player1).contains(source)) })
            // Observed engine behavior. Correct for Priest's no-exile boundary, contradicts Ring's frozen Oracle requirement.
            d.getPermanents(d.player2).contains(host) shouldBe true
            d.getExile(d.player2).contains(host) shouldBe false
        }
    }
})
