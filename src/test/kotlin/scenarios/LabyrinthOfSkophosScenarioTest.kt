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

class LabyrinthOfSkophosScenarioTest : FunSpec({
    test("removes an attacker without untapping it or letting it deal combat damage") {
        val d=scenarioDriver(LabyrinthOfSkophos);val me=d.player1;val bear=d.putCreatureOnBattlefield(me,"Grizzly Bears");d.removeSummoningSickness(bear)
        val land=d.putPermanentOnBattlefield(me,LabyrinthOfSkophos.name)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS);d.declareAttackers(me,listOf(bear),d.player2).error shouldBe null
        d.giveColorlessMana(me,4)
        d.submit(ActivateAbility(me,land,LabyrinthOfSkophos.activatedAbilities[1].id,targets=listOf(ChosenTarget.Permanent(bear)))).error shouldBe null
        d.resolvePendingActions();d.state.getEntity(bear)!!.has<AttackingComponent>() shouldBe false;d.state.getEntity(bear)!!.has<TappedComponent>() shouldBe true
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN);d.getLifeTotal(d.player2) shouldBe 20
    }
    test("a creature outside combat is not a legal target") {
        val d=scenarioDriver(LabyrinthOfSkophos);val me=d.player1;val bear=d.putCreatureOnBattlefield(me,"Grizzly Bears");val land=d.putPermanentOnBattlefield(me,LabyrinthOfSkophos.name);d.giveColorlessMana(me,4)
        (d.submit(ActivateAbility(me,land,LabyrinthOfSkophos.activatedAbilities[1].id,targets=listOf(ChosenTarget.Permanent(bear)))).error!=null) shouldBe true
    }
})
