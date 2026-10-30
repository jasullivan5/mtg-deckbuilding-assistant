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

class SpinedTyrranaxScenarioTest : FunSpec({
    for(pay in listOf(false,true)) test("reflexive target is chosen only after optional payment: pay=$pay") {
        val d=scenarioDriver(SpinedTyrranax);val me=d.player1;d.putCreatureOnBattlefield(me,SpinedTyrranax.name);val bear=d.putCreatureOnBattlefield(me,"Grizzly Bears")
        d.passPriorityUntil(Step.BEGIN_COMBAT);d.giveMana(me,Color.GREEN,3)
        var paid=false;var targets=0
        repeat(80) {
            when(val choice=d.pendingDecision) {
                is YesNoDecision -> {d.submitYesNo(choice.playerId,pay).error shouldBe null;paid=pay}
                is SelectManaSourcesDecision -> d.submitDecision(choice.playerId,ManaSourcesSelectedResponse(choice.id,autoPay=true)).error shouldBe null
                is ChooseTargetsDecision -> {paid shouldBe true;targets++;d.submitTargetSelection(choice.playerId,listOf(bear)).error shouldBe null}
                null -> if(d.stackSize>0) d.bothPass()
                else -> d.autoResolveDecision()
            }
        }
        targets shouldBe if(pay) 1 else 0
        (d.state.getEntity(bear)!!.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe if(pay) 1 else 0
        d.state.projectedState.hasKeyword(bear,Keyword.TRAMPLE) shouldBe pay
    }
})
