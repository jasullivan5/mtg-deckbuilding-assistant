package dev.mtg.assistant.explorer

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.projectedTypeLine
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import dev.mtg.assistant.EngineBuild
import dev.mtg.assistant.objectives.graveyardCreatureCards
import dev.mtg.assistant.objectives.nontokenCreatureDeath
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.time.Instant

internal val labNativeJson = Json {
    serializersModule = engineSerializersModule
    allowStructuredMapKeys = true
    encodeDefaults = true
}

@Serializable internal data class LabCheckpoint(
    val sourceZone: Zone?, val sourceType: String?, val sourceRef: ObjectRef?, val opponentLife: Int,
    val stackSize: Int, val triggerSource: ObjectRef?, val ownerGraveyard: List<String>,
    val battlefield: List<String>, val creatureCardInGraveyard: Boolean?
)

@Serializable internal data class LabAction(
    val label: String, val outcome: String, val message: String?, val events: List<String>,
    val checkpoint: LabCheckpoint, val nativeAction: JsonElement, val nativeResult: JsonElement
)

@Serializable internal data class LabResult(
    val scenario: LabScenarioInfo, val applicationRevision: String, val argentumCommit: String, val engineMode: String,
    val startedAt: String, val execution: String, val comparison: String, val problem: String?,
    val initial: LabCheckpoint?, val actions: List<LabAction>, val final: LabCheckpoint?,
    val creatureDied: Boolean?, val sourceReturnedAsNewObject: Boolean?, val initialNativeState: JsonElement?
) {
    val scenarioId: String = scenario.id
}

internal data class LabSeed(val card: CardDefinition, val zone: Zone)
internal class LabStopped(message: String) : RuntimeException(message)

/** Small, private position builder. All actions after seeding go through Argentum's transaction boundary. */
internal class LabExecution(
    val sourceCard: CardDefinition, step: Step, support: List<LabSeed>, mana: ManaPoolComponent
) {
    val player = EntityId("lab-player")
    val opponent = EntityId("lab-opponent")
    val source = EntityId("lab-source")
    private val seeds = listOf(LabSeed(sourceCard, Zone.BATTLEFIELD)) + support
    private val ids = seeds.mapIndexed { index, _ -> if (index == 0) source else EntityId("lab-card-$index") }
    private val registry = CardRegistry().apply { seeds.map { it.card }.distinct().forEach(::register) }
    private val processor = ActionProcessor(EngineServices(registry), computeUndo = false)
    private var state = GameState(turnOrder = listOf(player, opponent), activePlayerId = player,
        priorityPlayerId = player, turnNumber = 2, phase = step.phase, step = step)
        .withEntity(player, ComponentContainer.of(PlayerComponent("Player 1"), LifeTotalComponent(20), mana))
        .withEntity(opponent, ComponentContainer.of(PlayerComponent("Opponent"), LifeTotalComponent(20), ManaPoolComponent()))
    private val trace = mutableListOf<LabAction>()
    private val results = mutableListOf<ExecutionResult>()
    val actions: List<LabAction> get() = trace
    val events: List<GameEvent> get() = results.flatMap { it.events }
    val sourceMoves: List<ZoneChangeEvent> get() = events.filterIsInstance<ZoneChangeEvent>().filter { it.entityId == source }
    val sacrifices: List<PermanentsSacrificedEvent> get() =
        events.filterIsInstance<PermanentsSacrificedEvent>().filter { source in it.permanentIds }

    init {
        for ((index, seed) in seeds.withIndex()) {
            val id = ids[index]
            state = state.withEntity(id, CardEntityFactory.create(seed.card, player))
                .addToZone(ZoneKey(player, seed.zone), id)
        }
    }

    fun id(card: CardDefinition): EntityId = ids[seeds.indexOfFirst { it.card == card }]
    fun sourceTypes() = projectedTypeLine(state, source)
    fun rawState(): JsonElement = labNativeJson.encodeToJsonElement(state)
    /** Read-only native state for scoped payment assertions; no application payment implementation. */
    fun nativeState(): GameState = state
    /** Explicit fixture seeding only, before the first real native action. */
    fun seedResearchPosition(transform: (GameState)->GameState) {
        check(trace.isEmpty()) { "A research seed cannot alter an observed execution" }
        state=transform(state)
    }
    private fun name(id: EntityId) = state.getEntity(id)?.get<CardComponent>()?.name ?: id.value

    fun checkpoint() = LabCheckpoint(
        state.logicalZone(source)?.zoneType, sourceTypes()?.toString(), state.objectRef(source),
        requireNotNull(state.getEntity(opponent)?.get<LifeTotalComponent>()).life,
        state.stack.size, state.stack.firstNotNullOfOrNull {
            state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()?.objectReferences?.source
        },
        state.getGraveyard(player).map(::name), state.getBattlefield().map(::name),
        graveyardCreatureCards(state, player).satisfied
    )

    fun creatureDied(): Boolean? {
        val outcomes = sourceMoves.map { nontokenCreatureDeath(it, player).qualifies }
        return if (true in outcomes) true else if (null in outcomes) null else false
    }

    fun sourceReturnedAsNewObject(original: ObjectRef?): Boolean? {
        val returned = sourceMoves.lastOrNull { it.fromZone == Zone.EXILE && it.toZone == Zone.BATTLEFIELD }
            ?: return null
        return returned.newObject?.let { original != null && it.entityId == original.entityId && it != original }
    }

    fun perform(label: String, action: GameAction, allowIllegal: Boolean = false) {
        if (Thread.currentThread().isInterrupted || trace.size >= 24) throw LabStopped("The scenario reached its execution limit.")
        val result = processor.process(state, action).result
        state = result.newState
        results += result
        val outcome = when (val status = result.outcome) {
            Outcome.Done -> "DONE"
            is Outcome.Paused -> "UNSUPPORTED_DECISION"
            is Outcome.Rejected -> when (status.reason) {
                is Rejection.IllegalAction -> "ILLEGAL_ACTION"
                is Rejection.ExecutionFailed -> "EXECUTION_FAILED"
            }
        }
        val message = result.error ?: result.pendingDecision?.let { "No curated response for ${it::class.simpleName}." }
        trace += LabAction(label, outcome, message, result.events.mapNotNull(::describe), checkpoint(),
            labNativeJson.encodeToJsonElement(action), labNativeJson.encodeToJsonElement(result))
        if (outcome != "DONE" && !(outcome == "ILLEGAL_ACTION" && allowIllegal))
            throw LabStopped(message ?: "Argentum could not complete this action.")
    }

    fun passPair(label: String) {
        repeat(2) {
            val priority = state.priorityPlayerId ?: throw LabStopped("The engine did not assign priority.")
            perform(if (it == 0) "Pass priority" else label, PassPriority(priority))
        }
    }

    fun damageToOpponent() = events.filterIsInstance<DamageDealtEvent>().filter {
        it.sourceId == source && it.targetId == opponent
    }.sumOf { it.amount }

    private fun describe(event: GameEvent): String? = when (event) {
        is ZoneChangeEvent -> "${event.entityName} moved from ${event.fromZone?.name?.lowercase()} to ${event.toZone.name.lowercase()}." +
            (event.lastKnown?.typeLine?.let { " At departure it was $it." } ?: "")
        is PermanentsSacrificedEvent -> event.permanentIds.joinToString { name(it) } + " was sacrificed."
        is DamageDealtEvent -> "${event.sourceName ?: sourceCard.name} dealt ${event.amount} damage to ${if (event.targetId == opponent) "the opponent" else event.targetName ?: event.targetId.value}."
        else -> null
    }
}

internal fun executeLab(scenario: LabScenario, revision: String): LabResult {
    val started = Instant.now().toString()
    fun result(
        status: String, comparison: String, problem: String?, game: LabExecution? = null,
        initial: LabCheckpoint? = null, raw: JsonElement? = null
    ) = LabResult(scenario.info, revision, EngineBuild.revision, EngineBuild.mode, started, status,
        comparison, problem, initial, game?.actions.orEmpty(), game?.checkpoint(), game?.creatureDied(),
        game?.sourceReturnedAsNewObject(initial?.sourceRef), raw)

    val game = try { scenario.setup() } catch (e: LabStopped) {
        return result("NOT_STARTED", "UNASSESSED", e.message)
    }
    val initial = game.checkpoint()
    val raw = game.rawState()
    return try {
        val agrees = scenario.run(game)
        result("FRESH_NATIVE", if (agrees) "AGREES" else "DIFFERS", null, game, initial, raw)
    } catch (e: LabStopped) {
        result("FRESH_NATIVE_INCOMPLETE", "UNASSESSED", e.message, game, initial, raw)
    }
}
