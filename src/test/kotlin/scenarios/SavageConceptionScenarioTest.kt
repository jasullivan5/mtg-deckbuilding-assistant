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

class SavageConceptionScenarioTest : FunSpec({
    test("retrace discards land, returns to graveyard, and can be cast again") {
        val d=scenarioDriver(SavageConception);val me=d.player1;d.clearHand(me)
        val spell=d.putCardInGraveyard(me,SavageConception.name)
        repeat(2) { val land=d.putCardInHand(me,"Forest");d.giveMana(me,Color.GREEN,5)
            d.submit(CastSpell(me,spell,additionalCostPayment=AdditionalCostPayment(discardedCards=listOf(land)))).error shouldBe null
            d.resolvePendingActions();d.getGraveyard(me).contains(spell) shouldBe true;d.getGraveyard(me).contains(land) shouldBe true }
        d.tokenIds(me).size shouldBe 2;d.tokenIds(me).forEach {d.state.projectedState.getPower(it) shouldBe 3}
    }
    test("retrace cannot substitute a nonland discard") {
        val d=scenarioDriver(SavageConception);val me=d.player1;d.clearHand(me)
        val spell=d.putCardInGraveyard(me,SavageConception.name);val bear=d.putCardInHand(me,"Grizzly Bears");d.giveMana(me,Color.GREEN,5)
        (d.submit(CastSpell(me,spell,additionalCostPayment=AdditionalCostPayment(discardedCards=listOf(bear)))).error!=null) shouldBe true
        d.tokenIds(me).size shouldBe 0
    }
})
