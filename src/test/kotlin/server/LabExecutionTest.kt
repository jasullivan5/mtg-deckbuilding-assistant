package dev.mtg.assistant.explorer

import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.sdk.core.Zone
import dev.mtg.assistant.EngineBuild
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LabExecutionTest : FunSpec({
    fun run(id: String) = executeLab(LabScenarios.all.getValue(id), "lab-test")
    fun LabAction.events() = labNativeJson.decodeFromJsonElement(ExecutionResult.serializer(), nativeResult).events
    fun LabResult.moves() = actions.flatMap { it.events() }.filterIsInstance<ZoneChangeEvent>()
        .filter { it.entityId == initial!!.sourceRef!!.entityId }

    for (id in LabScenarios.all.keys) {
        test("$id executes freshly and repeats its observed checkpoints") {
            val first = run(id)
            first.execution shouldBe "FRESH_NATIVE"
            first.comparison shouldBe "AGREES"
            first.scenarioId shouldBe id
            first.applicationRevision shouldBe "lab-test"
            first.argentumCommit shouldBe EngineBuild.revision
            val second = run(id)
            second.initial shouldBe first.initial
            second.final shouldBe first.final
            second.actions.map { it.checkpoint } shouldBe first.actions.map { it.checkpoint }
        }
    }

    test("Mogg Fanatic pays before damage and needs a target") {
        val legal = run("mogg-pay")
        val payment = legal.actions.first()
        payment.checkpoint.opponentLife shouldBe 20
        payment.checkpoint.stackSize shouldBe 1
        payment.events().filterIsInstance<PermanentsSacrificedEvent>().size shouldBe 1
        payment.events().filterIsInstance<DamageDealtEvent>() shouldBe emptyList()
        val move = legal.moves().single()
        move.oldObject shouldBe legal.initial!!.sourceRef
        move.newObject shouldBe legal.final!!.sourceRef
        move.lastKnown!!.typeLine!!.isCreature shouldBe true
        move.toZone shouldBe Zone.GRAVEYARD
        legal.final!!.ownerGraveyard shouldBe listOf("Mogg Fanatic")
        legal.actions.last().events().filterIsInstance<DamageDealtEvent>().single().amount shouldBe 1
        legal.final!!.opponentLife shouldBe 19

        val illegal = run("mogg-no-target")
        illegal.actions.single().outcome shouldBe "ILLEGAL_ACTION"
        illegal.actions.single().events() shouldBe emptyList()
        illegal.final shouldBe illegal.initial
        illegal.creatureDied shouldBe false
    }

    test("animated Wilds dies but the arriving land is not a creature-card witness") {
        val result = run("wilds-animated")
        result.initial!!.sourceType shouldBe "Land"
        val move = result.moves().single()
        move.lastKnown!!.typeLine!!.isCreature shouldBe true
        move.wasSacrificed shouldBe true
        move.ownerId shouldBe move.lastKnown!!.controllerId
        move.requestedDestination shouldBe Zone.GRAVEYARD
        move.toZone shouldBe Zone.GRAVEYARD
        result.creatureDied shouldBe true
        result.final!!.sourceType shouldBe "Land"
        result.final!!.creatureCardInGraveyard shouldBe false
        result.final!!.stackSize shouldBe 1
    }

    test("Spark's old trigger has the original reference even after Blink") {
        val normal = run("spark-ordinary")
        normal.moves().single().oldObject shouldBe normal.initial!!.sourceRef
        normal.final!!.ownerGraveyard shouldBe listOf("Spark Elemental")

        val blink = run("spark-blink")
        val original = blink.initial!!.sourceRef!!
        val returned = blink.final!!.sourceRef!!
        returned.entityId shouldBe original.entityId
        (returned.generation != original.generation) shouldBe true
        blink.actions.dropLast(1).last().checkpoint.triggerSource shouldBe original
        blink.actions.last().events().filterIsInstance<PermanentsSacrificedEvent>() shouldBe emptyList()
        blink.moves().map { it.toZone } shouldBe listOf(Zone.EXILE, Zone.BATTLEFIELD)
        blink.final!!.battlefield.contains("Spark Elemental") shouldBe true
        blink.final!!.ownerGraveyard shouldBe listOf("Momentary Blink")
    }

    test("unsupported setup and interrupted scripts preserve their distinct outcomes") {
        val original = LabScenarios.all.getValue("mogg-pay")
        val unavailable = LabScenario(original.info, original.card, { throw LabStopped("fixture unavailable") }, original.run)
        executeLab(unavailable, "test").let {
            it.execution shouldBe "NOT_STARTED"
            it.comparison shouldBe "UNASSESSED"
            it.actions shouldBe emptyList()
        }
        val interrupted = LabScenario(original.info, original.card, original.setup) { game ->
            original.run(game)
            throw LabStopped("unsupported next decision")
        }
        executeLab(interrupted, "test").let {
            it.execution shouldBe "FRESH_NATIVE_INCOMPLETE"
            it.comparison shouldBe "UNASSESSED"
            it.final!!.sourceZone shouldBe Zone.GRAVEYARD
            it.problem shouldBe "unsupported next decision"
        }
        val disagreement = LabScenario(original.info, original.card, original.setup) { game ->
            original.run(game)
            false
        }
        executeLab(disagreement, "test").let {
            it.execution shouldBe "FRESH_NATIVE"
            it.comparison shouldBe "DIFFERS"
            it.final!!.sourceZone shouldBe Zone.GRAVEYARD
        }
    }
})
