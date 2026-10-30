package dev.mtg.assistant

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.*
import io.kotest.matchers.shouldBe

/** Real authored cards and actual ActionProcessor; extraction alone is not execution evidence. */
class ReanimationScenarioTest : ScenarioTestBase() {
    init {
        test("Reanimate returns our original graveyard creature and loses its mana value") {
            val g=scenario().withPlayers().withCardInHand(1,"Reanimate").withCardInGraveyard(1,"Hill Giant")
                .withLandsOnBattlefield(1,"Swamp",1).build()
            val original=g.findCardsInGraveyard(1,"Hill Giant").single()
            g.execute(CastSpell(playerId=g.player1Id,cardId=g.findCardsInHand(1,"Reanimate").single(),
                targets=listOf(ChosenTarget.Card(original,g.player1Id,Zone.GRAVEYARD)))).error shouldBe null
            g.resolveStack().forEach { it.error shouldBe null }
            g.isOnBattlefield("Hill Giant") shouldBe true
            g.isInGraveyard(1,"Hill Giant") shouldBe false
            g.getLifeTotal(1) shouldBe 16
        }
        test("Reanimate can return an opponents card under our control without changing ownership") {
            val g=scenario().withPlayers().withCardInHand(1,"Reanimate").withCardInGraveyard(2,"Hill Giant")
                .withLandsOnBattlefield(1,"Swamp",1).build()
            g.execute(CastSpell(playerId=g.player1Id,cardId=g.findCardsInHand(1,"Reanimate").single(),
                targets=listOf(ChosenTarget.Card(g.findCardsInGraveyard(2,"Hill Giant").single(),g.player2Id,Zone.GRAVEYARD)))).error shouldBe null
            g.resolveStack().forEach { it.error shouldBe null }
            g.isOnBattlefield("Hill Giant") shouldBe true
            // The scenario helper checks the actual projected controller, not a description string.
            g.state.getBattlefield(g.player1Id).contains(g.findPermanent("Hill Giant")) shouldBe true
            g.state.getEntity(g.findPermanent("Hill Giant")!!)!!.get<com.wingedsheep.engine.state.components.identity.OwnerComponent>()!!.playerId shouldBe g.player2Id
            g.isInGraveyard(2,"Hill Giant") shouldBe false
        }
        test("Zombify rejects a target from an opponents graveyard") {
            val g=scenario().withPlayers().withCardInHand(1,"Zombify").withCardInGraveyard(2,"Hill Giant")
                .withLandsOnBattlefield(1,"Swamp",4).build()
            val r=g.execute(CastSpell(playerId=g.player1Id,cardId=g.findCardsInHand(1,"Zombify").single(),
                targets=listOf(ChosenTarget.Card(g.findCardsInGraveyard(2,"Hill Giant").single(),g.player2Id,Zone.GRAVEYARD))))
            (r.error!=null) shouldBe true
            g.isInGraveyard(2,"Hill Giant") shouldBe true
        }
    }
}
