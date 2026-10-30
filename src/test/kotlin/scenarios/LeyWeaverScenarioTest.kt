package dev.mtg.assistant

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.dsl.*
import dev.mtg.assistant.extensions.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

class LeyWeaverScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().also {
        // A named tutor target fixture; this does not claim implementation of Lore Weaver's abilities.
        it.registerCards(TestCards.all+listOf(LeyWeaver,card("Lore Weaver") { typeLine="Creature — Human Wizard";power=2;toughness=2 }))
        it.initMirrorMatch(deck=Deck.of("Forest" to 40));it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    test("partner search belongs to targeted opponent and can be accepted") {
        val d=driver();val me=d.player1;val opp=d.player2
        val lore=d.putCardOnTopOfLibrary(opp,"Lore Weaver");val spell=d.putCardInHand(me,LeyWeaver.name);d.giveMana(me,Color.GREEN,4)
        d.castSpell(me,spell).error shouldBe null
        repeat(40) {
            when(val choice=d.pendingDecision) {
                is ChooseTargetsDecision -> d.submitTargetSelection(choice.playerId,listOf(opp)).error shouldBe null
                is YesNoDecision -> { choice.playerId shouldBe opp;d.submitYesNo(opp,true).error shouldBe null }
                is SelectCardsDecision -> { choice.playerId shouldBe opp;d.submitCardSelection(opp,listOf(lore)).error shouldBe null }
                null -> if(d.stackSize>0) d.bothPass() else return@repeat
                else -> d.autoResolveDecision()
            }
        }
        d.getHand(opp).contains(lore) shouldBe true;d.getHand(me).contains(lore) shouldBe false
    }
    test("untap requires two different lands and taps the Weaver") {
        val d=driver();val me=d.player1
        val weaver=d.putCreatureOnBattlefield(me,LeyWeaver.name);d.removeSummoningSickness(weaver)
        val one=d.putPermanentOnBattlefield(me,"Forest");val two=d.putPermanentOnBattlefield(d.player2,"Forest")
        d.replaceState(d.state.updateEntity(one) { it.with(TappedComponent) }.updateEntity(two) { it.with(TappedComponent) })
        fun action(targets: List<ChosenTarget>)=ActivateAbility(me,weaver,LeyWeaver.activatedAbilities.single().id,targets=targets)
        (d.submit(action(listOf(ChosenTarget.Permanent(one)))).error!=null) shouldBe true
        d.submit(action(listOf(ChosenTarget.Permanent(one),ChosenTarget.Permanent(two)))).error shouldBe null
        d.bothPass()
        d.state.getEntity(one)!!.has<TappedComponent>() shouldBe false;d.state.getEntity(two)!!.has<TappedComponent>() shouldBe false
        d.state.getEntity(weaver)!!.has<TappedComponent>() shouldBe true
    }
})
