package dev.mtg.assistant

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.mtg.sets.definitions.big.cards.HarvesterOfMisery
import com.wingedsheep.mtg.sets.definitions.neo.cards.SokenzanCrucibleOfDefiance
import com.wingedsheep.mtg.sets.definitions.neo.cards.TwinshotSniper
import com.wingedsheep.mtg.sets.definitions.rtr.cards.RestInPeace
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import dev.mtg.assistant.objectives.graveyardCreatureCards
import dev.mtg.assistant.objectives.nontokenCreatureDeath
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SourceDiscardScenarioTest : FunSpec({
    test("Twinshot Sniper pays with the exact source before damage resolves") {
        val driver = scenarioDriver(TwinshotSniper)
        val me = driver.player1
        val opponent = driver.player2
        val source = driver.putCardInHand(me, TwinshotSniper.name)
        val otherCopy = driver.putCardInHand(me, TwinshotSniper.name)
        driver.giveMana(me, Color.RED, 2)
        val lifeBefore = driver.getLifeTotal(opponent)
        val before = driver.events.size
        driver.submit(ActivateAbility(me, source, TwinshotSniper.activatedAbilities.single().id,
            targets = listOf(ChosenTarget.Player(opponent)))).error shouldBe null

        val events = driver.events.drop(before)
        val move = events.filterIsInstance<ZoneChangeEvent>().single { it.entityId == source }
        move.fromZone shouldBe Zone.HAND
        move.toZone shouldBe Zone.GRAVEYARD
        move.ownerId shouldBe me
        val discard = events.filterIsInstance<CardsDiscardedEvent>().single()
        discard.playerId shouldBe me
        discard.cardIds shouldBe listOf(source)
        nontokenCreatureDeath(move, me).qualifies shouldBe false
        graveyardCreatureCards(driver.state, me).matching shouldBe setOf(requireNotNull(move.newObject))
        (otherCopy in driver.getHand(me)) shouldBe true
        driver.getLifeTotal(opponent) shouldBe lifeBefore
        driver.resolvePendingActions()
        driver.getLifeTotal(opponent) shouldBe lifeBefore - 2
    }

    test("Harvester of Misery cannot pay the discard cost without a legal creature target") {
        val driver = scenarioDriver(HarvesterOfMisery)
        val me = driver.player1
        val source = driver.putCardInHand(me, HarvesterOfMisery.name)
        driver.giveMana(me, Color.BLACK, 2)
        val before = driver.events.size
        val result = driver.submit(ActivateAbility(me, source, HarvesterOfMisery.activatedAbilities.single().id))
        (result.error != null) shouldBe true
        (source in driver.getHand(me)) shouldBe true
        driver.events.drop(before).filterIsInstance<CardsDiscardedEvent>() shouldBe emptyList()
        graveyardCreatureCards(driver.state, me).satisfied shouldBe false
    }

    test("Rest in Peace redirects the card while the discard still pays the activation cost") {
        val driver = scenarioDriver(TwinshotSniper, RestInPeace)
        val me = driver.player1
        val opponent = driver.player2
        driver.putPermanentOnBattlefield(opponent, RestInPeace.name)
        val source = driver.putCardInHand(me, TwinshotSniper.name)
        driver.giveMana(me, Color.RED, 2)
        val before = driver.events.size
        driver.submit(ActivateAbility(me, source, TwinshotSniper.activatedAbilities.single().id,
            targets = listOf(ChosenTarget.Player(opponent)))).error shouldBe null

        val events = driver.events.drop(before)
        val move = events.filterIsInstance<ZoneChangeEvent>().single { it.entityId == source }
        move.fromZone shouldBe Zone.HAND
        move.requestedDestination shouldBe Zone.GRAVEYARD
        move.toZone shouldBe Zone.EXILE
        events.filterIsInstance<CardsDiscardedEvent>().single().cardIds shouldBe listOf(source)
        graveyardCreatureCards(driver.state, me).satisfied shouldBe false
    }

    test("Sokenzan discards a land and separately creates two creature tokens on resolution") {
        val driver = scenarioDriver(SokenzanCrucibleOfDefiance)
        val me = driver.player1
        val source = driver.putCardInHand(me, SokenzanCrucibleOfDefiance.name)
        driver.giveMana(me, Color.RED, 4)
        val ability = SokenzanCrucibleOfDefiance.activatedAbilities.single { it.activateFromZone == Zone.HAND }
        val before = driver.events.size
        driver.submit(ActivateAbility(me, source, ability.id)).error shouldBe null
        val move = driver.events.drop(before).filterIsInstance<ZoneChangeEvent>().single { it.entityId == source }
        move.fromZone shouldBe Zone.HAND
        move.toZone shouldBe Zone.GRAVEYARD
        (source in driver.state.getGraveyard(me)) shouldBe true
        graveyardCreatureCards(driver.state, me).satisfied shouldBe false
        driver.tokenIds(me) shouldBe emptyList()
        driver.resolvePendingActions()
        driver.tokenIds(me).size shouldBe 2
        graveyardCreatureCards(driver.state, me).satisfied shouldBe false
    }
})
