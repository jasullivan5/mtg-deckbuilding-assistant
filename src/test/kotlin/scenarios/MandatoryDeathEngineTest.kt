package dev.mtg.assistant

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.*
import io.kotest.matchers.shouldBe

/** Generic mandatory/timed-sacrifice scenarios using real authored cards and the native ActionProcessor. */
class MandatoryDeathEngineTest : ScenarioTestBase() {
    init {
        for(name in listOf("Ball Lightning","Spark Elemental")) test("$name remains until end step then sacrifices itself") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,name)
                .withCardInLibrary(1,"Mountain").withCardInLibrary(2,"Mountain")
                .inPhase(Phase.POSTCOMBAT_MAIN,Step.POSTCOMBAT_MAIN).build()
            g.isOnBattlefield(name) shouldBe true
            g.passUntilPhase(Phase.ENDING,Step.END)
            g.resolveStack().forEach { it.error shouldBe null }
            g.isInGraveyard(1,name) shouldBe true
        }
        test("Pit Raptor without payable mana sacrifices on upkeep through native PayOrSuffer") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,"Pit Raptor")
                .withCardInLibrary(1,"Swamp").withCardInLibrary(2,"Swamp")
                .withActivePlayer(2).inPhase(Phase.POSTCOMBAT_MAIN,Step.POSTCOMBAT_MAIN).build()
            g.passUntilPhase(Phase.BEGINNING,Step.UPKEEP)
            g.state.activePlayerId shouldBe g.player1Id
            g.resolveStack().forEach { it.error shouldBe null }
            g.isInGraveyard(1,"Pit Raptor") shouldBe true
        }
        test("Demonic Taskmaster cannot sacrifice itself when no other creature exists") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,"Demonic Taskmaster")
                .withCardInLibrary(1,"Swamp").withCardInLibrary(2,"Swamp")
                .withActivePlayer(2).inPhase(Phase.POSTCOMBAT_MAIN,Step.POSTCOMBAT_MAIN).build()
            g.passUntilPhase(Phase.BEGINNING,Step.UPKEEP)
            g.resolveStack().forEach { it.error shouldBe null }
            g.isOnBattlefield("Demonic Taskmaster") shouldBe true
        }
        test("Taskmaster leaving after upkeep trigger does not remove the sacrifice obligation") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,"Demonic Taskmaster")
                .withCardOnBattlefield(1,"Aegis Turtle").withCardInHand(1,"Murder")
                .withLandsOnBattlefield(1,"Swamp",3)
                .withCardInLibrary(1,"Swamp").withCardInLibrary(2,"Swamp")
                .withActivePlayer(2).inPhase(Phase.POSTCOMBAT_MAIN,Step.POSTCOMBAT_MAIN).build()
            g.passUntilPhase(Phase.BEGINNING,Step.UPKEEP)
            g.castSpell(1,"Murder",targetId=g.findPermanent("Demonic Taskmaster")!!).error shouldBe null
            g.resolveStack().forEach { it.error shouldBe null }
            g.isInGraveyard(1,"Demonic Taskmaster") shouldBe true
            g.isInGraveyard(1,"Aegis Turtle") shouldBe true
        }
        test("Accursed Centaur alone must sacrifice itself after its ETB resolves") {
            val g=scenario().withPlayers().withCardInHand(1,"Accursed Centaur").withLandsOnBattlefield(1,"Swamp",1).build()
            g.castSpell(1,"Accursed Centaur").error shouldBe null
            g.resolveStack().forEach { it.error shouldBe null }
            g.isInGraveyard(1,"Accursed Centaur") shouldBe true
        }
    }
}
