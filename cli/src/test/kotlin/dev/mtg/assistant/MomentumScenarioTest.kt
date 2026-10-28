package dev.mtg.assistant

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.combat.*
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import dev.mtg.assistant.extensions.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MomentumScenarioTest : FunSpec({
    for(accept in listOf(false,true)) test("optional upkeep growth belongs to the Aura and continuously boosts its host: accept=$accept") {
        val d=batchTwoDriver(Momentum);val me=d.player1;val bear=d.putCreatureOnBattlefield(d.player2,"Grizzly Bears")
        val spell=d.putCardInHand(me,Momentum.name);d.giveMana(me,Color.GREEN,3)
        d.castSpellWithTargets(me,spell,listOf(ChosenTarget.Permanent(bear))).error shouldBe null;d.drainBatchTwo()
        val aura=d.findPermanent(me,Momentum.name)!!;d.state.projectedState.getPower(bear) shouldBe 2
        d.passPriorityUntil(Step.UPKEEP,activePlayer=me);d.drainBatchTwo(accept)
        (d.state.getEntity(aura)!!.get<CountersComponent>()?.getCount(CounterType.GROWTH) ?: 0) shouldBe if(accept) 1 else 0
        d.state.projectedState.getPower(bear) shouldBe if(accept) 3 else 2
        d.replaceState(d.state.updateEntity(aura) {it.with(CountersComponent(mapOf(CounterType.GROWTH to 3)))})
        d.state.projectedState.getPower(bear) shouldBe 5
    }
})
