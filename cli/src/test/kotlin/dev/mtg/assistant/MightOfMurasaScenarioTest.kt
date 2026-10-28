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

class MightOfMurasaScenarioTest : FunSpec({
    for(kicked in listOf(false,true)) test("kicker=$kicked replaces the bonus and expires at cleanup") {
        val d=batchTwoDriver(MightOfMurasa);val me=d.player1;val bear=d.putCreatureOnBattlefield(me,"Grizzly Bears")
        val spell=d.putCardInHand(me,MightOfMurasa.name);d.giveMana(me,Color.GREEN,if(kicked) 5 else 2)
        d.submit(CastSpell(me,spell,targets=listOf(ChosenTarget.Permanent(bear)),declaredCostSlot=if(kicked) ChoiceSlot.KICKED else null)).error shouldBe null
        d.drainBatchTwo();d.state.projectedState.getPower(bear) shouldBe if(kicked) 7 else 5
        d.passPriorityUntil(Step.CLEANUP);d.state.projectedState.getPower(bear) shouldBe 2
    }
    test("declared kicker cannot be paid with the base spell cost alone") {
        val d=batchTwoDriver(MightOfMurasa);val me=d.player1;val target=d.putCreatureOnBattlefield(me,"Grizzly Bears")
        val spell=d.putCardInHand(me,MightOfMurasa.name);d.giveMana(me,Color.GREEN,2)
        (d.submit(CastSpell(me,spell,targets=listOf(ChosenTarget.Permanent(target)),declaredCostSlot=ChoiceSlot.KICKED)).error!=null) shouldBe true
    }
})
