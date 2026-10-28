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
import com.wingedsheep.engine.state.components.battlefield.DamageComponent

class PredatoryUrgeScenarioTest : FunSpec({
    fun driver(): GameTestDriver = batchTwoDriver(PredatoryUrge,card("Urge Test Kill") { manaCost="{0}";typeLine="Instant";spell {val t=target(TargetFilter.Creature);effect=Effects.Destroy(t)}},card("Urge Test Prey") {typeLine="Creature";power=3;toughness=5})
    for(remove in listOf(false,true)) test("granted sequential damage uses the original host and LKI after removal=$remove") {
        val d=driver();val me=d.player1;val host=d.putCreatureOnBattlefield(me,"Grizzly Bears");d.removeSummoningSickness(host)
        val prey=d.putCreatureOnBattlefield(d.player2,"Urge Test Prey");val aura=d.putCardInHand(me,PredatoryUrge.name);d.giveMana(me,Color.GREEN,4)
        d.castSpellWithTargets(me,aura,listOf(ChosenTarget.Permanent(host))).error shouldBe null;d.drainBatchTwo()
        val grant=PredatoryUrge.staticAbilities.filterIsInstance<GrantActivatedAbility>().single().ability
        d.submit(ActivateAbility(me,host,grant.id,targets=listOf(ChosenTarget.Permanent(prey)))).error shouldBe null
        if(remove) {
            d.passPriority(me);val kill=d.putCardInHand(d.player2,"Urge Test Kill")
            d.castSpellWithTargets(d.player2,kill,listOf(ChosenTarget.Permanent(host))).error shouldBe null;d.bothPass()
            d.getGraveyard(me).contains(host) shouldBe true
        }
        d.drainBatchTwo()
        d.state.getEntity(prey)!!.get<DamageComponent>()!!.amount shouldBe 2
    }
})
