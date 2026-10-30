package dev.mtg.assistant

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.dsl.*
import dev.mtg.assistant.extensions.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PlanewideCelebrationScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all+PlanewideCelebration)
        it.initMirrorMatch(deck=Deck.of("Forest" to 40));it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.finish() { repeat(40) {
        if(pendingDecision!=null) autoResolveDecision() else if(stackSize>0) bothPass() else return
    };error("Unsettled celebration") }
    for(mode in listOf(0,3)) test("four repeats of mode $mode execute separately") {
        val d=driver();val me=d.player1;val spell=d.putCardInHand(me,PlanewideCelebration.name);d.giveMana(me,Color.GREEN,7)
        d.submit(CastSpell(me,spell,chosenModes=listOf(mode,mode,mode,mode))).error shouldBe null
        d.finish()
        d.getLifeTotal(me) shouldBe if(mode==3) 36 else 20
        val tokens=d.state.getBattlefield(me).filter { d.state.getEntity(it)!!.has<TokenComponent>() }
        tokens.size shouldBe if(mode==0) 4 else 0
        tokens.forEach { d.state.projectedState.getColors(it) shouldBe Color.entries.map { c -> c.name }.toSet() }
    }
    test("targeted permanent returns to hand alongside three life modes") {
        val d=driver();val me=d.player1;val land=d.putCardInGraveyard(me,"Forest")
        val spell=d.putCardInHand(me,PlanewideCelebration.name);d.giveMana(me,Color.GREEN,7)
        val chosen=ChosenTarget.Card(land,me,Zone.GRAVEYARD)
        d.submit(CastSpell(me,spell,targets=listOf(chosen),chosenModes=listOf(3,1,3,3),
            modeTargetsOrdered=listOf(emptyList(),listOf(chosen),emptyList(),emptyList()))).error shouldBe null
        d.finish();d.getHand(me).contains(land) shouldBe true;d.getLifeTotal(me) shouldBe 32
    }
    test("proliferation chosen at resolution adds every existing counter kind") {
        val d=driver();val me=d.player1;val bear=d.putCreatureOnBattlefield(d.player2,"Grizzly Bears")
        d.replaceState(d.state.updateEntity(bear) { it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1,CounterType.CHARGE to 1))) })
        val spell=d.putCardInHand(me,PlanewideCelebration.name);d.giveMana(me,Color.GREEN,7)
        d.submit(CastSpell(me,spell,chosenModes=listOf(2,3,3,3))).error shouldBe null
        repeat(40) {
            val choice=d.pendingDecision
            if(choice is SelectCardsDecision) d.submitCardSelection(choice.playerId,listOf(bear)).error shouldBe null
            else if(choice!=null) d.autoResolveDecision() else if(d.stackSize>0) d.bothPass()
        }
        val counters=d.state.getEntity(bear)!!.get<CountersComponent>()!!
        counters.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2;counters.getCount(CounterType.CHARGE) shouldBe 2
        d.getLifeTotal(me) shouldBe 32
    }
    test("three chosen modes cannot substitute for four") {
        val d=driver();val me=d.player1;val spell=d.putCardInHand(me,PlanewideCelebration.name);d.giveMana(me,Color.GREEN,7)
        (d.submit(CastSpell(me,spell,chosenModes=listOf(3,3,3))).error!=null) shouldBe true
        d.getLifeTotal(me) shouldBe 20
    }
})
