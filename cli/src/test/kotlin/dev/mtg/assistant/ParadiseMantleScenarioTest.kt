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

class ParadiseMantleScenarioTest : FunSpec({
    for(ready in listOf(false,true)) test("the equipped creature supplies the tap and mana, ready=$ready") {
        val d=batchTwoDriver(ParadiseMantle);val me=d.player1;val bear=d.putCreatureOnBattlefield(me,"Grizzly Bears")
        val mantle=d.putPermanentOnBattlefield(me,ParadiseMantle.name);d.giveMana(me,Color.GREEN,1)
        d.submit(ActivateAbility(me,mantle,ParadiseMantle.activatedAbilities.single().id,targets=listOf(ChosenTarget.Permanent(bear)))).error shouldBe null
        d.drainBatchTwo();if(ready) d.removeSummoningSickness(bear)
        val grant=ParadiseMantle.staticAbilities.filterIsInstance<GrantActivatedAbility>().single().ability
        val result=d.submit(ActivateAbility(me,bear,grant.id))
        (result.error==null) shouldBe ready
        if(ready) { d.drainBatchTwo();d.state.getEntity(me)!!.get<ManaPoolComponent>()!!.green shouldBe 1
            d.state.getEntity(bear)!!.has<TappedComponent>() shouldBe true;d.state.getEntity(mantle)!!.has<TappedComponent>() shouldBe false }
    }
})
