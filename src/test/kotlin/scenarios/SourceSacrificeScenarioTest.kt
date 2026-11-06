package dev.mtg.assistant

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.projectedTypeLine
import com.wingedsheep.mtg.sets.definitions.`5dn`.cards.SparkElemental
import com.wingedsheep.mtg.sets.definitions.mrd.cards.LifesparkSpellbomb
import com.wingedsheep.mtg.sets.definitions.roe.cards.EvolvingWilds
import com.wingedsheep.mtg.sets.definitions.rtr.cards.RestInPeace
import com.wingedsheep.mtg.sets.definitions.tmp.cards.MoggFanatic
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import dev.mtg.assistant.objectives.graveyardCreatureCards
import dev.mtg.assistant.objectives.nontokenCreatureDeath
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SourceSacrificeScenarioTest : FunSpec({
    for (case in listOf("owned", "borrowed", "token copy", "replacement", "already satisfied")) {
        test("Mogg Fanatic pays with its exact source under $case conditions") {
            val driver = scenarioDriver(MoggFanatic, RestInPeace)
            val me = driver.player1
            val opponent = driver.player2
            val owner = if (case == "borrowed") opponent else me
            val source = driver.putCreatureOnBattlefield(owner, MoggFanatic.name)
            if (case == "borrowed") driver.addComponent(source, ControllerComponent(me))
            if (case == "token copy") driver.addComponent(source, TokenComponent)
            if (case == "replacement") driver.putPermanentOnBattlefield(opponent, RestInPeace.name)
            val existing = if (case == "already satisfied") driver.putCardInGraveyard(me, MoggFanatic.name) else null
            val decoy = driver.putCreatureOnBattlefield(me, MoggFanatic.name)
            val before = driver.events.size
            val departure = driver.state.objectRef(source)
            val life = driver.getLifeTotal(opponent)
            graveyardCreatureCards(driver.state, me).satisfied shouldBe (existing != null)
            driver.submit(ActivateAbility(me, source, MoggFanatic.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Player(opponent)))).error shouldBe null

            val events = driver.events.drop(before)
            events.filterIsInstance<PermanentsSacrificedEvent>().size shouldBe 1
            val move = events.filterIsInstance<ZoneChangeEvent>().single()
            move.entityId shouldBe source
            move.fromZone shouldBe Zone.BATTLEFIELD
            move.ownerId shouldBe owner
            move.lastKnown!!.controllerId shouldBe me
            move.lastKnown!!.wasToken shouldBe (case == "token copy")
            move.wasSacrificed shouldBe true
            move.oldObject shouldBe departure
            (move.oldObject != move.newObject) shouldBe true
            move.requestedDestination shouldBe Zone.GRAVEYARD
            move.toZone shouldBe if (case == "replacement") Zone.EXILE else Zone.GRAVEYARD
            nontokenCreatureDeath(move, me).qualifies shouldBe (case !in setOf("token copy", "replacement"))
            val arrival = graveyardCreatureCards(driver.state, me)
            arrival.satisfied shouldBe (case in setOf("owned", "already satisfied"))
            (move.newObject in arrival.matching) shouldBe (case in setOf("owned", "already satisfied"))
            if (existing != null) arrival.matching.size shouldBe 2
            if (case == "borrowed") (source in driver.state.getGraveyard(opponent)) shouldBe true
            if (case == "token copy") driver.state.getEntity(source) shouldBe null
            driver.state.logicalZone(decoy)?.zoneType shouldBe Zone.BATTLEFIELD
            driver.getLifeTotal(opponent) shouldBe life
            driver.resolvePendingActions()
            driver.getLifeTotal(opponent) shouldBe life - 1
        }
    }

    test("a targetless Mogg Fanatic activation preserves the battlefield object and state") {
        val driver = scenarioDriver(MoggFanatic)
        val me = driver.player1
        val source = driver.putCreatureOnBattlefield(me, MoggFanatic.name)
        val ref = driver.state.objectRef(source)
        val before = driver.events.size
        (driver.submit(ActivateAbility(me, source, MoggFanatic.activatedAbilities.single().id)).error != null) shouldBe true
        driver.state.objectRef(source) shouldBe ref
        driver.state.logicalZone(source)?.zoneType shouldBe Zone.BATTLEFIELD
        driver.events.drop(before).filterIsInstance<PermanentsSacrificedEvent>() shouldBe emptyList()
        graveyardCreatureCards(driver.state, me).satisfied shouldBe false
    }

    test("an animated Evolving Wilds pays with itself and dies as a creature but arrives as a land") {
        val driver = scenarioDriver(EvolvingWilds, LifesparkSpellbomb)
        val me = driver.player1
        val land = driver.putLandOnBattlefield(me, EvolvingWilds.name)
        val spellbomb = driver.putPermanentOnBattlefield(me, LifesparkSpellbomb.name)
        driver.giveMana(me, Color.GREEN)
        driver.submit(ActivateAbility(me, spellbomb, LifesparkSpellbomb.activatedAbilities.first().id,
            targets = listOf(ChosenTarget.Permanent(land)))).error shouldBe null
        driver.resolvePendingActions()
        projectedTypeLine(driver.state, land)?.isCreature shouldBe true
        graveyardCreatureCards(driver.state, me).satisfied shouldBe false

        val before = driver.events.size
        val ref = driver.state.objectRef(land)
        driver.submit(ActivateAbility(me, land, EvolvingWilds.activatedAbilities.single().id)).error shouldBe null
        val move = driver.events.drop(before).filterIsInstance<ZoneChangeEvent>().single()
        move.entityId shouldBe land
        move.oldObject shouldBe ref
        (move.oldObject != move.newObject) shouldBe true
        move.wasSacrificed shouldBe true
        move.toZone shouldBe Zone.GRAVEYARD
        nontokenCreatureDeath(move, me).qualifies shouldBe true
        projectedTypeLine(driver.state, land)?.isCreature shouldBe false
        graveyardCreatureCards(driver.state, me).satisfied shouldBe false
    }

    test("Spark Elemental sacrifices the original source on resolution of its end-step trigger") {
        val driver = scenarioDriver(SparkElemental)
        val me = driver.player1
        val source = driver.putCreatureOnBattlefield(me, SparkElemental.name)
        val ref = driver.state.objectRef(source)
        driver.passPriorityUntil(Step.END)
        driver.state.objectRef(source) shouldBe ref
        graveyardCreatureCards(driver.state, me).satisfied shouldBe false
        val before = driver.events.size
        driver.resolvePendingActions()
        val move = driver.events.drop(before).filterIsInstance<ZoneChangeEvent>().single { it.entityId == source }
        move.oldObject shouldBe ref
        move.wasSacrificed shouldBe true
        nontokenCreatureDeath(move, me).qualifies shouldBe true
        graveyardCreatureCards(driver.state, me).matching shouldBe setOf(requireNotNull(move.newObject))
    }
})
