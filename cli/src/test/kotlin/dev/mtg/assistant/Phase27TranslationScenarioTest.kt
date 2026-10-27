package dev.mtg.assistant

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCantLoseGame
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import dev.mtg.assistant.extensions.Bitterblossom
import dev.mtg.assistant.extensions.KrenkosCommand
import io.kotest.matchers.shouldBe

/** Actual native casting/priority/step transitions and resolution, not just SDK tree assertions. */
class Phase27TranslationScenarioTest : ScenarioTestBase() {
    init {
        val command=KrenkosCommand.name
        val blossom=Bitterblossom.name
        cardRegistry.register(listOf(KrenkosCommand,Bitterblossom,
            card("Phase27 Test Immortality") {
                typeLine="Enchantment";staticAbility { ability=GrantCantLoseGame }
            },
            card("Phase27 Test Disenchant") {
                manaCost="{0}";typeLine="Instant"
                spell { val e=target(TargetFilter(GameObjectFilter.Enchantment));effect=Effects.Destroy(e) }
            }))
        fun TestGame.resolveAll() { resolveStack().forEach { it.error shouldBe null } }
        fun TestGame.tokens(player: Int) = state.getBattlefield(if(player==1) player1Id else player2Id)
            .filter { state.getEntity(it)!!.has<TokenComponent>() }

        for(owner in 1..2) test("Krenko command cast by player $owner pays mana and creates exactly two owned red Goblins on resolution") {
            val g=scenario().withPlayers().withCardInHand(owner,command).withLandsOnBattlefield(owner,"Mountain",2)
                .withActivePlayer(owner).build()
            g.castSpell(owner,command).error shouldBe null
            g.tokens(owner).size shouldBe 0
            g.resolveAll()
            g.tokens(owner).size shouldBe 2;g.tokens(3-owner).size shouldBe 0
            g.tokens(owner).forEach { token ->
                val card=g.state.getEntity(token)!!.get<CardComponent>()!!
                card.typeLine.isCreature shouldBe true
                g.state.projectedState.getSubtypes(token).contains("Goblin") shouldBe true
                g.state.projectedState.getColors(token) shouldBe setOf("RED")
                g.state.projectedState.getPower(token) shouldBe 1;g.state.projectedState.getToughness(token) shouldBe 1
                g.state.projectedState.hasKeyword(token,Keyword.HASTE) shouldBe false
            }
            g.isInGraveyard(owner,command) shouldBe true
        }
        for((land,count) in listOf("Mountain" to 1,"Forest" to 2)) test("Krenko command rejects insufficient or incorrectly colored mana: $land $count") {
            val g=scenario().withPlayers().withCardInHand(1,command).withLandsOnBattlefield(1,land,count).build()
            val before=g.state
            (g.castSpell(1,command).error!=null) shouldBe true
            g.state shouldBe before;g.tokens(1).size shouldBe 0
        }
        test("Krenko command cannot be cast at instant timing on the opponent turn") {
            val g=scenario().withPlayers().withCardInHand(1,command).withLandsOnBattlefield(1,"Mountain",2)
                .withActivePlayer(2).withPriorityPlayer(1).build()
            (g.castSpell(1,command).error!=null) shouldBe true
            g.tokens(1).size shouldBe 0
        }
        for(owner in 1..2) test("Bitterblossom owner $owner upkeep produces one black flying Faerie Rogue and mandatory life loss") {
            val g=scenario().withPlayers().withCardOnBattlefield(owner,blossom)
                .withCardInLibrary(1,"Swamp").withCardInLibrary(2,"Swamp")
                .withActivePlayer(3-owner).inPhase(Phase.POSTCOMBAT_MAIN,Step.POSTCOMBAT_MAIN).build()
            g.tokens(owner).size shouldBe 0
            g.passUntilPhase(Phase.BEGINNING,Step.UPKEEP)
            g.tokens(owner).size shouldBe 0;g.getLifeTotal(owner) shouldBe 20
            g.resolveAll()
            g.getLifeTotal(owner) shouldBe 19;g.getLifeTotal(3-owner) shouldBe 20
            g.tokens(owner).size shouldBe 1;g.tokens(3-owner).size shouldBe 0
            val token=g.tokens(owner).single()
            g.state.projectedState.getColors(token) shouldBe setOf("BLACK")
            g.state.projectedState.getSubtypes(token) shouldBe setOf("Faerie","Rogue")
            g.state.projectedState.hasKeyword(token,Keyword.FLYING) shouldBe true
            g.state.projectedState.getPower(token) shouldBe 1;g.state.projectedState.getToughness(token) shouldBe 1
            g.state.getEntity(g.findPermanent(blossom)!!)!!.get<CardComponent>()!!.typeLine.isCreature shouldBe false
        }
        test("Bitterblossom does not trigger during the opponent upkeep") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,blossom)
                .withCardInLibrary(1,"Swamp").withCardInLibrary(2,"Swamp")
                .withActivePlayer(1).inPhase(Phase.POSTCOMBAT_MAIN,Step.POSTCOMBAT_MAIN).build()
            g.passUntilPhase(Phase.BEGINNING,Step.UPKEEP);g.resolveAll()
            g.state.activePlayerId shouldBe g.player2Id
            g.tokens(1).size shouldBe 0;g.getLifeTotal(1) shouldBe 20
        }
        test("Bitterblossom trigger survives source removal in response") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,blossom).withCardInHand(1,"Phase27 Test Disenchant")
                .withCardInLibrary(1,"Swamp").withCardInLibrary(2,"Swamp")
                .withActivePlayer(2).inPhase(Phase.POSTCOMBAT_MAIN,Step.POSTCOMBAT_MAIN).build()
            g.passUntilPhase(Phase.BEGINNING,Step.UPKEEP)
            g.castSpell(1,"Phase27 Test Disenchant",targetId=g.findPermanent(blossom)!!).error shouldBe null
            g.resolveAll()
            g.isInGraveyard(1,blossom) shouldBe true;g.tokens(1).size shouldBe 1;g.getLifeTotal(1) shouldBe 19
        }
        test("Bitterblossom mandatory life loss at one life still creates the token before state based loss") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,blossom).withLifeTotal(1,1)
                .withCardInLibrary(1,"Swamp").withCardInLibrary(2,"Swamp")
                .withActivePlayer(2).inPhase(Phase.POSTCOMBAT_MAIN,Step.POSTCOMBAT_MAIN).build()
            g.passUntilPhase(Phase.BEGINNING,Step.UPKEEP);g.resolveAll()
            g.getLifeTotal(1) shouldBe 0;g.tokens(1).size shouldBe 1
            g.state.gameOver shouldBe true;g.state.winnerId shouldBe g.player2Id
        }
        test("Bitterblossom at zero protected life creates a token because life loss is not payment") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,blossom).withCardOnBattlefield(1,"Phase27 Test Immortality")
                .withLifeTotal(1,0).withCardInLibrary(1,"Swamp").withCardInLibrary(2,"Swamp")
                .withActivePlayer(2).inPhase(Phase.POSTCOMBAT_MAIN,Step.POSTCOMBAT_MAIN).build()
            g.passUntilPhase(Phase.BEGINNING,Step.UPKEEP);g.resolveAll()
            g.getLifeTotal(1) shouldBe -1;g.tokens(1).size shouldBe 1;g.state.gameOver shouldBe false
        }
    }
}
