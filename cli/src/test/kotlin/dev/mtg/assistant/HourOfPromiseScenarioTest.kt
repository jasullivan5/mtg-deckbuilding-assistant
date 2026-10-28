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

class HourOfPromiseScenarioTest : FunSpec({
    for(existing in listOf(0,1)) test("Deserts fetched earlier in the same resolution count: existing=$existing") {
        val d=batchTwoDriver(HourOfPromise);val me=d.player1
        repeat(existing) {d.putPermanentOnBattlefield(me,"Desert")}
        val found=List(2) {d.putCardOnTopOfLibrary(me,"Desert")}
        val spell=d.putCardInHand(me,HourOfPromise.name);d.giveMana(me,Color.GREEN,5);d.castSpell(me,spell).error shouldBe null
        repeat(80) { val choice=d.pendingDecision
            if(choice is SelectCardsDecision) d.submitCardSelection(choice.playerId,found).error shouldBe null
            else if(choice!=null) d.autoResolveDecision() else if(d.stackSize>0) d.bothPass() }
        found.forEach { d.state.getBattlefield(me).contains(it) shouldBe true;d.state.getEntity(it)!!.has<TappedComponent>() shouldBe true }
        d.tokenIds(me).size shouldBe if(existing==1) 2 else 0
    }
})
