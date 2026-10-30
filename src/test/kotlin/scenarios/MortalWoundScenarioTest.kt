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

class MortalWoundScenarioTest : ScenarioTestBase() {
    init {
        cardRegistry.register(MortalWound)
        for(amount in 0..1) cardRegistry.register(card("Wound Test Ping $amount") {
            manaCost="{0}";typeLine="Instant";spell { val c=target(TargetFilter.Creature);effect=Effects.DealDamage(amount,c) }
        })
        for(amount in 0..1) test("enchanted opponent creature: $amount damage tests actual damage trigger and destruction") {
            val g=scenario().withPlayers().withCardInHand(1,MortalWound.name).withLandsOnBattlefield(1,"Forest",1)
                .withCardInHand(1,"Wound Test Ping $amount").withCardOnBattlefield(2,"Aegis Turtle").build()
            val turtle=g.findPermanent("Aegis Turtle")!!
            g.castSpell(1,MortalWound.name,targetId=turtle).error shouldBe null
            g.resolveStack().forEach { it.error shouldBe null }
            g.castSpell(1,"Wound Test Ping $amount",targetId=turtle).error shouldBe null
            g.resolveStack().forEach { it.error shouldBe null }
            g.isInGraveyard(2,"Aegis Turtle") shouldBe (amount==1)
            g.isOnBattlefield(MortalWound.name) shouldBe (amount==0)
        }
    }
}
