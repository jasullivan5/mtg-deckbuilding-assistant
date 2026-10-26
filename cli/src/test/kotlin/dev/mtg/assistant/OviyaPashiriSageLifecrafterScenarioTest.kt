package dev.mtg.assistant

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.EntityId
import dev.mtg.assistant.extensions.OviyaPashiriSageLifecrafter
import io.kotest.matchers.shouldBe

/** Native ActionProcessor scenarios derived from the archived Oracle text and both WotC rulings. */
class OviyaPashiriSageLifecrafterScenarioTest : ScenarioTestBase() {
    init {
        val oviya=OviyaPashiriSageLifecrafter
        cardRegistry.register(listOf(oviya,
            card("Oviya Test Bear Maker") { manaCost="{0}";typeLine="Instant";spell { effect=Effects.CreateToken(2,2,creatureTypes=setOf("Bear")) } },
            card("Oviya Test Slay") { manaCost="{0}";typeLine="Instant";spell { val c=target(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.Creature);effect=Effects.Destroy(c) } }))
        fun TestGame.activate(index: Int,player: EntityId=player1Id)=execute(ActivateAbility(player,findPermanent(oviya.name)!!,oviya.activatedAbilities[index].id))
        fun TestGame.resolveAll() { resolveStack().forEach { it.error shouldBe null } }
        test("Servo activation pays three mana taps source and creates our colorless artifact creature") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,oviya.name).withLandsOnBattlefield(1,"Forest",3).build()
            g.activate(0).error shouldBe null
            g.state.getEntity(g.findPermanent(oviya.name)!!)!!.has<TappedComponent>() shouldBe true
            g.state.getBattlefield(g.player1Id).count { g.state.getEntity(it)!!.has<TappedComponent>() } shouldBe 4
            g.findPermanent("Servo Token") shouldBe null
            g.resolveAll()
            val token=g.findPermanent("Servo Token")!!
            g.state.getBattlefield(g.player1Id).contains(token) shouldBe true
            g.state.getEntity(token)!!.has<TokenComponent>() shouldBe true
            val c=g.state.getEntity(token)!!.get<CardComponent>()!!
            c.typeLine.isArtifact shouldBe true;c.typeLine.isCreature shouldBe true
            g.state.projectedState.getColors(token) shouldBe emptySet<String>()
            g.state.projectedState.getPower(token) shouldBe 1;g.state.projectedState.getToughness(token) shouldBe 1
        }
        test("Construct counts only our creatures before itself and keeps fixed base stats later") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,oviya.name).withCardOnBattlefield(1,"Grizzly Bears")
                .withCardOnBattlefield(2,"Grizzly Bears").withLandsOnBattlefield(1,"Forest",5).withCardInHand(1,"Oviya Test Bear Maker").build()
            g.activate(1).error shouldBe null;g.resolveAll()
            val token=g.findPermanent("Construct Token")!!
            g.state.getBattlefield(g.player1Id).contains(token) shouldBe true
            g.state.getEntity(token)!!.get<CardComponent>()!!.typeLine.isArtifact shouldBe true
            g.state.projectedState.getPower(token) shouldBe 2;g.state.projectedState.getToughness(token) shouldBe 2
            g.castSpell(1,"Oviya Test Bear Maker").error shouldBe null;g.resolveAll()
            g.state.projectedState.getPower(token) shouldBe 2;g.state.projectedState.getToughness(token) shouldBe 2
        }
        test("Construct count is read at resolution after creatures enter in response") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,oviya.name).withLandsOnBattlefield(1,"Forest",5).withCardInHand(1,"Oviya Test Bear Maker").build()
            g.activate(1).error shouldBe null
            g.castSpell(1,"Oviya Test Bear Maker").error shouldBe null;g.resolveAll()
            g.state.projectedState.getPower(g.findPermanent("Construct Token")!!) shouldBe 2
        }
        test("ability survives its source leaving and uses remaining creature count") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,oviya.name).withCardOnBattlefield(1,"Grizzly Bears")
                .withLandsOnBattlefield(1,"Forest",5).withCardInHand(1,"Oviya Test Slay").build()
            val source=g.findPermanent(oviya.name)!!
            g.activate(1).error shouldBe null
            g.castSpell(1,"Oviya Test Slay",targetId=source).error shouldBe null;g.resolveAll()
            g.isInGraveyard(1,oviya.name) shouldBe true
            g.state.projectedState.getPower(g.findPermanent("Construct Token")!!) shouldBe 1
        }
        test("zero-creature resolution creates a zero toughness token removed by native state based actions") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,oviya.name).withLandsOnBattlefield(1,"Forest",5).withCardInHand(1,"Oviya Test Slay").build()
            val source=g.findPermanent(oviya.name)!!
            g.activate(1).error shouldBe null;g.castSpell(1,"Oviya Test Slay",targetId=source).error shouldBe null;g.resolveAll()
            g.findPermanent("Construct Token") shouldBe null
        }
        for(index in 0..1) test("activation $index requires full colored mana") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,oviya.name).withLandsOnBattlefield(1,"Mountain",5).build()
            val before=g.state
            (g.activate(index).error!=null) shouldBe true
            g.state shouldBe before
        }
        for(index in 0..1) test("activation $index requires generic mana as well as green") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,oviya.name).withLandsOnBattlefield(1,"Forest",if(index==0) 2 else 4).build()
            (g.activate(index).error!=null) shouldBe true
            g.state.getEntity(g.findPermanent(oviya.name)!!)!!.has<TappedComponent>() shouldBe false
        }
        for(kind in listOf("tapped","summoning-sick","wrong-controller")) test("activation rejects $kind source") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,oviya.name,tapped=kind=="tapped",summoningSickness=kind=="summoning-sick")
                .withLandsOnBattlefield(1,"Forest",5).withLandsOnBattlefield(2,"Forest",5).withPriorityPlayer(if(kind=="wrong-controller") 2 else 1).build()
            (g.activate(0,if(kind=="wrong-controller") g.player2Id else g.player1Id).error!=null) shouldBe true
            g.findPermanent("Servo Token") shouldBe null
        }
        test("activation is available on another player's turn with priority and cannot repeat while tapped") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,oviya.name).withLandsOnBattlefield(1,"Forest",6)
                .withActivePlayer(2).withPriorityPlayer(1).build()
            g.activate(0).error shouldBe null;g.resolveAll()
            (g.activate(0).error!=null) shouldBe true
            g.findPermanents("Servo Token").size shouldBe 1
        }
    }
}
