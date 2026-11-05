package dev.mtg.assistant

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.mtg.sets.definitions.m10.cards.TomeScour
import com.wingedsheep.mtg.sets.definitions.m11.cards.VisceraSeer
import com.wingedsheep.mtg.sets.definitions.tla.cards.Wolfbat
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.util.Properties

@Serializable
private data class EventStateReceipt(val application: String, val engine: String, val cases: List<Observation>)

@Serializable
private data class Observation(
    val id: String,
    val from: Zone,
    val requested: Zone,
    val actual: Zone,
    val owner: String,
    val controllerBefore: String?,
    val battlefieldCreature: Boolean?,
    val token: Boolean?,
    val finalityCounters: Int,
    val sacrificed: Boolean,
    val fromGeneration: Long,
    val toGeneration: Long,
    val atArrivalReference: Boolean,
    val observedZone: Zone?,
    val observedTypes: List<String>,
    val myGraveyard: List<String>,
    val opponentGraveyard: List<String>,
    val event: Boolean?,
    val state: Boolean,
    val linked: Boolean?
)

private fun GameState.creatureCardsInGraveyard(player: EntityId): List<EntityId> =
    getGraveyard(player).filter { id ->
        val entity = requireNotNull(getEntity(id))
        !entity.has<TokenComponent>() && requireNotNull(entity.get<CardComponent>()).typeLine.isCreature
    }

private fun observe(id: String, move: ZoneChangeEvent, state: GameState, me: EntityId, opponent: EntityId): Observation {
    val before = move.lastKnown
    val departed = requireNotNull(move.oldObject)
    val arrived = requireNotNull(move.newObject)
    require(departed.entityId == move.entityId && arrived.entityId == move.entityId && departed != arrived)
    fun player(id: EntityId): String = when (id) {
        me -> "You"
        opponent -> "Opponent"
        else -> error("Unexpected player in two-player scenario")
    }
    fun names(player: EntityId) = state.getGraveyard(player).map {
        requireNotNull(state.getEntity(it)?.get<CardComponent>()).name
    }
    val event = when {
        !move.isBattlefieldDeparture || move.toZone != Zone.GRAVEYARD -> false
        before?.typeLine == null || before.controllerId == null -> null
        else -> before.controllerId == me && before.typeLine!!.isCreature && !before.wasToken
    }
    val cards = state.creatureCardsInGraveyard(me)
    val atArrival = state.isCurrentObject(arrived)
    val types = state.getEntity(move.entityId)?.get<CardComponent>()?.typeLine?.cardTypes?.map { it.name }.orEmpty()
    return Observation(id, requireNotNull(move.fromZone), move.requestedDestination, move.toZone, player(move.ownerId),
        before?.controllerId?.let(::player), before?.typeLine?.isCreature, before?.wasToken,
        before?.counters?.get(CounterType.FINALITY) ?: 0, move.wasSacrificed, departed.generation, arrived.generation,
        atArrival, state.logicalZone(move.entityId)?.zoneType, types, names(me), names(opponent),
        event, cards.isNotEmpty(), event?.let { it && atArrival && move.entityId in cards })
}

class EventToStateScenarioTest : FunSpec({
    test("record the same object's move and independently check death and graveyard state") {
        val expected = listOf(
            Triple("owned", Zone.GRAVEYARD, listOf(true, true, true)),
            Triple("borrowed", Zone.GRAVEYARD, listOf(true, false, false)),
            Triple("token", Zone.GRAVEYARD, listOf(false, false, false)),
            Triple("finality", Zone.EXILE, listOf(false, false, false)),
            Triple("mill", Zone.GRAVEYARD, listOf(false, true, false))
        )
        val cases = expected.map { (id, destination, outcomes) ->
            val driver = scenarioDriver(VisceraSeer, Wolfbat, TomeScour)
            val me = driver.player1
            val opponent = driver.player2
            val owner = if (id == "borrowed") opponent else me
            val victim = if (id == "mill") driver.putCardOnTopOfLibrary(me, Wolfbat.name)
                else driver.putCreatureOnBattlefield(owner, Wolfbat.name)
            if (id == "borrowed") driver.addComponent(victim, ControllerComponent(me))
            if (id == "token") driver.addComponent(victim, TokenComponent)
            if (id == "finality") driver.addComponent(victim, CountersComponent(mapOf(CounterType.FINALITY to 1)))
            driver.state.creatureCardsInGraveyard(me) shouldBe emptyList()
            val priorEvents = driver.events.size
            if (id == "mill") {
                val spell = driver.putCardInHand(me, TomeScour.name)
                driver.giveMana(me, Color.BLUE)
                driver.castSpell(me, spell, listOf(me)).error shouldBe null
            } else {
                val seer = driver.putCreatureOnBattlefield(me, VisceraSeer.name)
                driver.getController(victim) shouldBe me
                driver.submit(ActivateAbility(me, seer, VisceraSeer.activatedAbilities.single().id,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(victim)))).error shouldBe null
            }
            driver.resolvePendingActions()
            val move = driver.events.drop(priorEvents).filterIsInstance<ZoneChangeEvent>().single { it.entityId == victim }
            val observation = observe(id, move, driver.state, me, opponent)
            observation.actual shouldBe destination
            observation.requested shouldBe Zone.GRAVEYARD
            observation.from shouldBe if (id == "mill") Zone.LIBRARY else Zone.BATTLEFIELD
            observation.owner shouldBe if (id == "borrowed") "Opponent" else "You"
            if (id != "mill") {
                observation.controllerBefore shouldBe "You"
                observation.battlefieldCreature shouldBe true
                observation.token shouldBe (id == "token")
                observation.sacrificed shouldBe true
            }
            observation.finalityCounters shouldBe if (id == "finality") 1 else 0
            observation.atArrivalReference shouldBe (id != "token")
            if (id == "token") driver.state.getEntity(victim) shouldBe null
            listOf(observation.event, observation.state, observation.linked) shouldBe outcomes
            observation
        }
        val application = Properties().apply {
            requireNotNull(EngineBuild::class.java.getResourceAsStream("/application-build.properties")).use(::load)
        }.getProperty("revision")
        val output = TestFiles.root.resolve("generated/event-to-state/observations.json")
        Files.createDirectories(output.parent)
        Files.writeString(output, Json { prettyPrint = true; encodeDefaults = true }
            .encodeToString(EventStateReceipt(application, EngineBuild.revision, cases)) + "\n")
    }
})
