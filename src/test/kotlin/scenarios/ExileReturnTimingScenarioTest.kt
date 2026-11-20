package dev.mtg.assistant

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*

/** Oracle expectations stay the same for the historical failing engine and corrected candidate. */
class ExileReturnTimingScenarioTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun card(name: String)=checkNotNull(catalog.card(name))
    fun GameTestDriver.cast(name: String, target: EntityId?=null): EntityId {
        giveMana(player1,Color.WHITE,5);giveMana(player1,Color.BLUE,5)
        val id=putCardInHand(player1,name)
        submit(CastSpell(player1,id,target?.let { listOf(ChosenTarget.Permanent(it)) } ?: emptyList(),
            paymentStrategy=PaymentStrategy.FromPool)).error shouldBe null
        return id
    }
    fun GameTestDriver.record(label: String, source: EntityId, target: EntityId, events: List<GameEvent> = emptyList()) {
        println("EXILE_TIMING " + buildJsonObject {
            put("engine",EngineBuild.revision);put("checkpoint",label)
            put("source",state.objectRef(source)?.let { reportJson.encodeToJsonElement(com.wingedsheep.engine.state.ObjectRef.serializer(),it) } ?: JsonNull)
            put("target",state.objectRef(target)?.let { reportJson.encodeToJsonElement(com.wingedsheep.engine.state.ObjectRef.serializer(),it) } ?: JsonNull)
            put("sourceZone",state.logicalZone(source)?.zoneType?.name)
            put("targetZone",state.logicalZone(target)?.zoneType?.name)
            put("priority",priorityPlayer?.toString())
            put("stack",JsonArray(state.stack.map { id -> buildJsonObject {
                put("id",id.toString())
                state.getEntity(id)?.get<TriggeredAbilityOnStackComponent>()?.let {
                    put("source",it.sourceId?.toString());put("sourceVisit",it.sourceBattlefieldTimestamp)
                    put("effect",nativeJson.encodeToJsonElement(Effect.serializer(),it.effect))
                }
                put("spell",state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name)
            } }))
            put("legalActions",JsonArray(priorityPlayer?.let { legalActions(it).take(20).map { a -> JsonPrimitive(a.toString()) } }.orEmpty()))
            put("zoneEvents",JsonArray(events.filterIsInstance<ZoneChangeEvent>().map {
                reportJson.encodeToJsonElement(ZoneChangeEvent.serializer(),it)
            }))
        })
    }
    test("Oblivion Ring independently exiles after its earlier empty return resolved") {
        val d=scenarioDriver(card("Oblivion Ring"),card("Hill Giant"),card("Boomerang"))
        val target=d.putCreatureOnBattlefield(d.player2,"Hill Giant")
        val source=d.cast("Oblivion Ring")
        d.bothPass().error shouldBe null
        val decision=d.pendingDecision as ChooseTargetsDecision
        d.submitTargetSelection(decision.playerId,listOf(target)).error shouldBe null
        d.record("ring-entry-trigger-pending",source,target)
        d.cast("Boomerang",source)
        val departure=d.bothPass()
        departure.error shouldBe null
        d.record("ring-departed-both-triggers-pending",source,target,departure.events)
        val emptyReturn=d.bothPass()
        emptyReturn.error shouldBe null
        d.record("ring-empty-return-resolved",source,target,emptyReturn.events)
        d.stackSize shouldBe 1
        val lateExile=d.bothPass()
        lateExile.error shouldBe null
        d.record("ring-late-exile-resolved",source,target,lateExile.events)
        d.getExile(d.player2).contains(target) shouldBe true
        d.stackSize shouldBe 0
    }
    test("Banisher Priest returns before an opponent can respond to an independent return trigger") {
        val d=scenarioDriver(card("Banisher Priest"),card("Hill Giant"),card("Boomerang"),card("Opt"))
        val target=d.putCreatureOnBattlefield(d.player2,"Hill Giant")
        val before=checkNotNull(d.state.objectRef(target))
        val source=d.cast("Banisher Priest")
        d.bothPass().error shouldBe null
        val decision=d.pendingDecision as ChooseTargetsDecision
        d.submitTargetSelection(decision.playerId,listOf(target)).error shouldBe null
        d.bothPass().error shouldBe null
        d.getExile(d.player2).contains(target) shouldBe true
        d.record("priest-duration-established",source,target)
        d.cast("Boomerang",source)
        val departure=d.bothPass()
        departure.error shouldBe null
        d.record("priest-next-priority-after-departure",source,target,departure.events)
        val returnedBeforePriority=d.getPermanents(d.player2).contains(target)
        val stackBeforeResponse=d.stackSize
        // Prove an actual player action is possible at this checkpoint, not only a stack count.
        d.cast("Opt")
        d.record("priest-player-response-announced",source,target)
        returnedBeforePriority shouldBe true
        stackBeforeResponse shouldBe 0
        d.state.isCurrentObject(before) shouldBe false
        d.getController(target) shouldBe d.player2
        departure.events.filterIsInstance<ZoneChangeEvent>().any {
            it.entityId==target && it.transitionCause==ZoneTransitionCause.DURATION_RETURN
        } shouldBe true
    }
})
