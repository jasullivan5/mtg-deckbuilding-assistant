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

class MagusOfTheLibraryScenarioTest : FunSpec({
    for(size in listOf(6,7,8)) test("drawing requires exactly seven at activation: hand=$size") {
        val d=scenarioDriver(MagusOfTheLibrary);val me=d.player1;d.clearHand(me);repeat(size) {d.putCardInHand(me,"Forest")}
        val magus=d.putCreatureOnBattlefield(me,MagusOfTheLibrary.name);d.removeSummoningSickness(magus)
        val result=d.submit(ActivateAbility(me,magus,MagusOfTheLibrary.activatedAbilities[1].id))
        (result.error==null) shouldBe (size==7)
        if(size==7) { d.resolvePendingActions();d.getHand(me).size shouldBe 8 }
    }
    test("two activations announced with seven both resolve after the hand becomes eight") {
        val d=scenarioDriver(MagusOfTheLibrary);val me=d.player1;d.clearHand(me);repeat(7) {d.putCardInHand(me,"Forest")}
        repeat(2) { val id=d.putCreatureOnBattlefield(me,MagusOfTheLibrary.name);d.removeSummoningSickness(id)
            d.submit(ActivateAbility(me,id,MagusOfTheLibrary.activatedAbilities[1].id)).error shouldBe null }
        d.resolvePendingActions();d.getHand(me).size shouldBe 9
    }
})
