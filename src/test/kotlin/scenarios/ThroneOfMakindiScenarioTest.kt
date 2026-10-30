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

class ThroneOfMakindiScenarioTest : FunSpec({
    test("charged mana is consumed for a kicked spell and refused for an unkicked spell") {
        val d=scenarioDriver(ThroneOfMakindi,MightOfMurasa);val me=d.player1;val land=d.putPermanentOnBattlefield(me,ThroneOfMakindi.name)
        d.giveMana(me,Color.GREEN,1);d.submit(ActivateAbility(me,land,ThroneOfMakindi.activatedAbilities[1].id)).error shouldBe null;d.resolvePendingActions()
        d.state.getEntity(land)!!.get<CountersComponent>()!!.getCount(CounterType.CHARGE) shouldBe 1
        d.replaceState(d.state.updateEntity(land) {it.without<TappedComponent>()})
        d.submit(ActivateAbility(me,land,ThroneOfMakindi.activatedAbilities[2].id)).error shouldBe null;d.resolvePendingActions()
        d.state.getEntity(land)!!.get<CountersComponent>()!!.getCount(CounterType.CHARGE) shouldBe 0
        val pool=d.state.getEntity(me)!!.get<ManaPoolComponent>()!!;pool.restrictedMana.size shouldBe 2
        pool.restrictedMana.all {it.restriction==ManaRestriction.KickedSpellsOnly && it.color==Color.GREEN} shouldBe true
        val bear=d.putCreatureOnBattlefield(me,"Grizzly Bears");val spell=d.putCardInHand(me,MightOfMurasa.name)
        (d.submit(CastSpell(me,spell,targets=listOf(ChosenTarget.Permanent(bear)),paymentStrategy=PaymentStrategy.FromPool)).error!=null) shouldBe true
        d.giveMana(me,Color.GREEN,3)
        d.submit(CastSpell(me,spell,targets=listOf(ChosenTarget.Permanent(bear)),declaredCostSlot=ChoiceSlot.KICKED,paymentStrategy=PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions();d.state.projectedState.getPower(bear) shouldBe 7
    }
    test("without a charge counter the restricted mana ability cannot activate") {
        val d=scenarioDriver(ThroneOfMakindi);val me=d.player1;val land=d.putPermanentOnBattlefield(me,ThroneOfMakindi.name)
        val before=d.state;(d.submit(ActivateAbility(me,land,ThroneOfMakindi.activatedAbilities[2].id)).error!=null) shouldBe true;d.state shouldBe before
    }
})
