package dev.mtg.assistant

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import io.kotest.matchers.shouldBe

/** Actual pinned ActionProcessor scenarios, separate from fact/benchmark assertions. */
class TokenCreationScenarioTest : ScenarioTestBase() {
    init {
        test("Dragon Fodder resolves into two creature tokens under our control") {
            val g=scenario().withPlayers().withCardInHand(1,"Dragon Fodder").withLandsOnBattlefield(1,"Mountain",2).build()
            g.castSpell(1,"Dragon Fodder").error shouldBe null
            g.resolveStack().forEach { it.error shouldBe null }
            val ts=g.findPermanents("Goblin Token")
            ts.size shouldBe 2
            ts.forEach { t -> g.state.getBattlefield(g.player1Id).contains(t) shouldBe true;g.state.projectedState.getPower(t) shouldBe 1;g.state.projectedState.getToughness(t) shouldBe 1 }
        }
        test("Hunted Horror creates its two creature tokens for the opponent") {
            val g=scenario().withPlayers().withCardInHand(1,"Hunted Horror").withLandsOnBattlefield(1,"Swamp",2).build()
            g.castSpell(1,"Hunted Horror").error shouldBe null
            g.resolveStack().forEach { it.error shouldBe null }
            val ts=g.findPermanents("Centaur Token");ts.size shouldBe 2
            ts.forEach { g.state.getBattlefield(g.player2Id).contains(it) shouldBe true }
        }
        test("Heat Shimmer copying an opposing creature creates our creature with haste") {
            val g=scenario().withPlayers().withCardInHand(1,"Heat Shimmer").withLandsOnBattlefield(1,"Mountain",3).withCardOnBattlefield(2,"Grizzly Bears").build()
            val original=g.findPermanent("Grizzly Bears")!!
            g.castSpell(1,"Heat Shimmer",original).error shouldBe null
            g.resolveStack().forEach { it.error shouldBe null }
            val copy=g.findPermanents("Grizzly Bears").single { it!=original }
            g.state.getBattlefield(g.player1Id).contains(copy) shouldBe true
            g.state.projectedState.hasKeyword(copy,Keyword.HASTE) shouldBe true
            g.state.getBattlefield(g.player2Id).contains(original) shouldBe true
        }
    }
}
