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

class TatterkiteScenarioTest : ScenarioTestBase() {
    init {
        cardRegistry.register(Tatterkite)
        for(kind in listOf(CounterType.PLUS_ONE_PLUS_ONE,CounterType.MINUS_ONE_MINUS_ONE,CounterType.CHARGE)) {
            val spell="Kite Test $kind"
            cardRegistry.register(card(spell) { manaCost="{0}";typeLine="Instant";spell {
                val c=target(TargetFilter.Creature)
                effect=Effects.AddCounters(kind,1,c) then Effects.GainLife(1)
            } })
            for(blocked in listOf(true,false)) test("$kind counter prohibition=$blocked preserves other spell effects") {
                val name=if(blocked) Tatterkite.name else "Grizzly Bears"
                val g=scenario().withPlayers().withCardOnBattlefield(1,name).withCardInHand(1,spell).build()
                val id=g.findPermanent(name)!!
                g.castSpell(1,spell,targetId=id).error shouldBe null
                g.resolveStack().forEach { it.error shouldBe null }
                (g.state.getEntity(id)?.get<CountersComponent>()?.getCount(kind) ?: 0) shouldBe if(blocked) 0 else 1
                g.getLifeTotal(1) shouldBe 21
                g.state.projectedState.hasKeyword(id,Keyword.FLYING) shouldBe blocked
            }
        }
    }
}
