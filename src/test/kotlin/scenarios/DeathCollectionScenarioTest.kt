package dev.mtg.assistant

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe

/** Real authored definitions, real ActionProcessor, no mocked card execution. */
class DeathCollectionScenarioTest : ScenarioTestBase() {
    init {
        test("Dread Return flashback pays three creature deaths before reanimation and exiles the spell") {
            val g=scenario().withPlayers().withCardInGraveyard(1,"Dread Return")
                .withCardInGraveyard(1,"Hill Giant").withCardOnBattlefield(1,"Grizzly Bears")
                .withCardOnBattlefield(1,"Glory Seeker").withCardOnBattlefield(1,"Devoted Hero").build()
            val fodder=listOf("Grizzly Bears","Glory Seeker","Devoted Hero").map { g.findPermanent(it)!! }
            g.execute(CastSpell(playerId=g.player1Id,cardId=g.findCardsInGraveyard(1,"Dread Return").single(),
                targets=listOf(ChosenTarget.Card(g.findCardsInGraveyard(1,"Hill Giant").single(),g.player1Id,Zone.GRAVEYARD)),
                additionalCostPayment=AdditionalCostPayment(sacrificedPermanents=fodder),useAlternativeCost=true,
                alternativeCostType=AlternativeCostType.FLASHBACK)).error shouldBe null
            listOf("Grizzly Bears","Glory Seeker","Devoted Hero").forEach { g.isInGraveyard(1,it) shouldBe true }
            g.isInGraveyard(1,"Hill Giant") shouldBe true
            g.resolveStack().forEach { it.error shouldBe null }
            g.isOnBattlefield("Hill Giant") shouldBe true
            g.isInExile(1,"Dread Return") shouldBe true
        }
        test("two creatures cannot pay Dread Return's three-creature flashback cost") {
            val g=scenario().withPlayers().withCardInGraveyard(1,"Dread Return").withCardInGraveyard(1,"Hill Giant")
                .withCardOnBattlefield(1,"Grizzly Bears").withCardOnBattlefield(1,"Glory Seeker").build()
            val r=g.execute(CastSpell(playerId=g.player1Id,cardId=g.findCardsInGraveyard(1,"Dread Return").single(),
                targets=listOf(ChosenTarget.Card(g.findCardsInGraveyard(1,"Hill Giant").single(),g.player1Id,Zone.GRAVEYARD)),
                additionalCostPayment=AdditionalCostPayment(sacrificedPermanents=listOf("Grizzly Bears","Glory Seeker").map { g.findPermanent(it)!! }),
                useAlternativeCost=true,alternativeCostType=AlternativeCostType.FLASHBACK))
            (r.error!=null) shouldBe true
            g.isOnBattlefield("Grizzly Bears") shouldBe true
        }
        test("Through the Breach selected hand creature enters then dies at next end step") {
            val g=scenario().withPlayers().withCardInHand(1,"Through the Breach").withCardInHand(1,"Hill Giant")
                .withLandsOnBattlefield(1,"Mountain",5).withCardInLibrary(1,"Mountain").withCardInLibrary(2,"Mountain").build()
            val giant=g.findCardsInHand(1,"Hill Giant").single()
            g.castSpell(1,"Through the Breach").error shouldBe null
            g.resolveStack().forEach { it.error shouldBe null }
            g.hasPendingDecision() shouldBe true
            g.selectCards(listOf(giant)).error shouldBe null
            g.resolveStack().forEach { it.error shouldBe null }
            g.isOnBattlefield("Hill Giant") shouldBe true
            g.passUntilPhase(Phase.ENDING,Step.END)
            g.resolveStack().forEach { it.error shouldBe null }
            g.isInGraveyard(1,"Hill Giant") shouldBe true
        }
        test("Through the Breach empty selection creates no sacrifice obligation") {
            val g=scenario().withPlayers().withCardInHand(1,"Through the Breach").withCardInHand(1,"Hill Giant")
                .withLandsOnBattlefield(1,"Mountain",5).withCardInLibrary(1,"Mountain").withCardInLibrary(2,"Mountain").build()
            g.castSpell(1,"Through the Breach").error shouldBe null
            g.resolveStack()
            g.selectCards(emptyList()).error shouldBe null
            g.resolveStack()
            g.passUntilPhase(Phase.ENDING,Step.END)
            g.resolveStack().forEach { it.error shouldBe null }
            g.isInHand(1,"Hill Giant") shouldBe true
            g.isInGraveyard(1,"Hill Giant") shouldBe false
        }
    }
}
