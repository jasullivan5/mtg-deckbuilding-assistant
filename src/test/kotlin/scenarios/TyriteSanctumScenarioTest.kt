package dev.mtg.assistant

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import dev.mtg.assistant.extensions.*
import io.kotest.matchers.shouldBe

class TyriteSanctumScenarioTest : ScenarioTestBase() {
    init {
        cardRegistry.register(listOf(TyriteSanctum,OviyaPashiriSageLifecrafter,
            card("Sanctum Test God") { typeLine="Creature — God";power=2;toughness=2 }))
        test("legendary target gains God and counter; nonlegendary target is rejected") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,TyriteSanctum.name)
                .withCardOnBattlefield(2,OviyaPashiriSageLifecrafter.name).withCardOnBattlefield(2,"Grizzly Bears")
                .withLandsOnBattlefield(1,"Forest",2).build()
            val land=g.findPermanent(TyriteSanctum.name)!!;val target=g.findPermanent(OviyaPashiriSageLifecrafter.name)!!
            fun action(id: com.wingedsheep.sdk.model.EntityId)=ActivateAbility(g.player1Id,land,TyriteSanctum.activatedAbilities[1].id,targets=listOf(ChosenTarget.Permanent(id)))
            (g.execute(action(g.findPermanent("Grizzly Bears")!!)).error!=null) shouldBe true
            g.execute(action(target)).error shouldBe null
            g.resolveStack().forEach { it.error shouldBe null }
            g.state.projectedState.getSubtypes(target).contains("God") shouldBe true
            g.state.getEntity(target)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }
        test("sacrificing Sanctum is a cost and an existing God receives an indestructible counter") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,TyriteSanctum.name)
                .withCardOnBattlefield(2,"Sanctum Test God").withLandsOnBattlefield(1,"Forest",4).build()
            val land=g.findPermanent(TyriteSanctum.name)!!;val god=g.findPermanent("Sanctum Test God")!!
            g.execute(ActivateAbility(g.player1Id,land,TyriteSanctum.activatedAbilities[2].id,targets=listOf(ChosenTarget.Permanent(god)))).error shouldBe null
            g.isInGraveyard(1,TyriteSanctum.name) shouldBe true
            g.state.projectedState.hasKeyword(god,Keyword.INDESTRUCTIBLE) shouldBe false
            g.resolveStack().forEach { it.error shouldBe null }
            g.state.getEntity(god)!!.get<CountersComponent>()!!.getCount(CounterType.INDESTRUCTIBLE) shouldBe 1
            g.state.projectedState.hasKeyword(god,Keyword.INDESTRUCTIBLE) shouldBe true
        }
    }
}
