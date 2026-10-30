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

class GaladhrimAmbushScenarioTest : FunSpec({
    test("counts current attackers and prevents non-Elf combat damage while Elves still deal damage") {
        val d=scenarioDriver(GaladhrimAmbush);val me=d.player1;val bear=d.putCreatureOnBattlefield(me,"Grizzly Bears");val elf=d.putCreatureOnBattlefield(me,"Llanowar Elves")
        listOf(bear,elf).forEach(d::removeSummoningSickness)
        val spell=d.putCardInHand(me,GaladhrimAmbush.name)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS);d.declareAttackers(me,listOf(bear,elf),d.player2).error shouldBe null
        d.giveMana(me,Color.GREEN,4);d.castSpell(me,spell).error shouldBe null;d.resolvePendingActions()
        d.tokenIds(me).size shouldBe 2
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN);d.getLifeTotal(d.player2) shouldBe 19
    }
    test("zero attackers still resolves but creates no tokens") {
        val d=scenarioDriver(GaladhrimAmbush);val me=d.player1;val spell=d.putCardInHand(me,GaladhrimAmbush.name);d.giveMana(me,Color.GREEN,4)
        d.castSpell(me,spell).error shouldBe null;d.resolvePendingActions();d.tokenIds(me).size shouldBe 0
    }
})
