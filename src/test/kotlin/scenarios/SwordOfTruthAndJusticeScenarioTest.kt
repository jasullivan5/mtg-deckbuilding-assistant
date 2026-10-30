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

import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

class SwordOfTruthAndJusticeScenarioTest : FunSpec({
    for(color in listOf(Color.WHITE,Color.BLUE,Color.GREEN)) test("equipped creature has +2/+2 and protection boundary against $color") {
        val ping=card("Sword Test $color") {manaCost="{${color.symbol}}";typeLine="Instant";spell {val t=target(TargetFilter.Creature);effect=Effects.DealDamage(1,t)}}
        val d=scenarioDriver(SwordOfTruthAndJustice,ping);val me=d.player1;val host=d.putCreatureOnBattlefield(me,"Grizzly Bears");val sword=d.putPermanentOnBattlefield(me,SwordOfTruthAndJustice.name);d.giveColorlessMana(me,2)
        d.submit(ActivateAbility(me,sword,SwordOfTruthAndJustice.activatedAbilities.single().id,targets=listOf(ChosenTarget.Permanent(host)))).error shouldBe null;d.resolvePendingActions()
        d.state.projectedState.getPower(host) shouldBe 4
        val spell=d.putCardInHand(me,ping.name);d.giveMana(me,color,1)
        (d.castSpellWithTargets(me,spell,listOf(ChosenTarget.Permanent(host))).error!=null) shouldBe (color!=Color.GREEN)
        if(color==Color.GREEN) d.resolvePendingActions()
    }
    test("combat damage chooses a creature at resolution without targeting, then proliferates its new counter") {
        val d=scenarioDriver(SwordOfTruthAndJustice);val me=d.player1;val host=d.putCreatureOnBattlefield(me,"Grizzly Bears");d.removeSummoningSickness(host)
        val sword=d.putPermanentOnBattlefield(me,SwordOfTruthAndJustice.name);d.giveColorlessMana(me,2)
        d.submit(ActivateAbility(me,sword,SwordOfTruthAndJustice.activatedAbilities.single().id,targets=listOf(ChosenTarget.Permanent(host)))).error shouldBe null;d.resolvePendingActions()
        d.putCreatureOnBattlefield(me,"Aegis Turtle")
        d.passPriorityUntil(Step.DECLARE_ATTACKERS);d.declareAttackers(me,listOf(host),d.player2).error shouldBe null
        d.passPriorityUntil(Step.COMBAT_DAMAGE)
        var choices=0
        repeat(80) {
            when(val choice=d.pendingDecision) {
                is SelectCardsDecision -> {choices++;d.submitCardSelection(choice.playerId,listOf(host)).error shouldBe null}
                is ChooseTargetsDecision -> error("Counter placement must not target")
                null -> if(d.stackSize>0) d.bothPass()
                else -> d.autoResolveDecision()
            }
        }
        choices shouldBe 2
        d.state.getEntity(host)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        d.state.projectedState.getPower(host) shouldBe 6
    }
})
