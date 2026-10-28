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

import com.wingedsheep.engine.handlers.effects.FaceDownTurnUp
import com.wingedsheep.sdk.scripting.effects.FaceDownMode

class ExperimentTwelveScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all+ExperimentTwelve)
        it.initMirrorMatch(deck=Deck.of("Forest" to 40));it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.faceDown(owner: EntityId): EntityId {
        val id=putCreatureOnBattlefield(owner,ExperimentTwelve.name)
        replaceState(state.updateEntity(id) { c -> c.with(FaceDownComponent).with(FaceDownModeComponent(FaceDownMode.DISGUISE))
            .with(FaceDownTurnUp.dataFor(ExperimentTwelve,ExperimentTwelve.name,FaceDownMode.DISGUISE)!!) })
        return id
    }
    test("self disguise turn-up creates a stack trigger and doubles resolution power") {
        val d=driver();val me=d.player1;val id=d.faceDown(me);d.giveMana(me,Color.GREEN,5)
        d.submit(TurnFaceUp(me,id,paymentStrategy=PaymentStrategy.FromPool)).error shouldBe null
        d.state.projectedState.getPower(id) shouldBe 4
        (d.stackSize>0) shouldBe true
        d.bothPass()
        d.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4
        d.state.projectedState.getPower(id) shouldBe 8
    }
    test("power is read at resolution and opposing face-up events do not trigger our Twelve") {
        val d=driver();val me=d.player1;val other=d.player2
        d.putCreatureOnBattlefield(me,ExperimentTwelve.name)
        val id=d.faceDown(other);d.giveMana(other,Color.GREEN,5)
        // Give the opponent priority without changing turns.
        d.passPriority(me)
        d.submit(TurnFaceUp(other,id,paymentStrategy=PaymentStrategy.FromPool)).error shouldBe null
        // Only the opponent's newly face-up Twelve triggers, not the one we control.
        d.stackSize shouldBe 1
        d.replaceState(d.state.updateEntity(id) { it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2))) })
        d.bothPass()
        d.state.getEntity(id)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 8
    }
})
