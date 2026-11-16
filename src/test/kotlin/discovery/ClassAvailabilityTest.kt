package dev.mtg.assistant

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.TimingRule
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ClassAvailabilityTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun extract(name: String) = PathwayExtractor(inspectCounters = true).extract(listOf(checkNotNull(catalog.card(name))))

    test("Innkeeper's Talent retains native progression on its exact replacement path") {
        val extraction = extract("Innkeeper's Talent")
        val modifier = extraction.counterModifiers.single()
        val action = extraction.actions.single { it.id == modifier.action.actionId }
        val availability = checkNotNull(action.classAvailability)
        modifier.action.classAvailability shouldBe availability
        modifier.transformation shouldBe CounterTransformation.Multiplicative(2)
        modifier.placedByYou shouldBe true
        action.path shouldBe "script.classLevels[1].replacementEffects[0]"
        availability.minimumLevel shouldBe 3
        availability.progression.map { it.level } shouldBe listOf(2, 3)
        availability.progression.map { it.manaCost.toString() } shouldBe listOf("{G}", "{3}{G}")
        availability.levelUpTiming shouldBe TimingRule.SorcerySpeed
        availability.sourceZone shouldBe Zone.BATTLEFIELD
        val asked = CounterQuestion(CounterType.PLUS_ONE_PLUS_ONE, Requirements(controller = Controller.YOU),
            amount = 2, placer = Controller.YOU, sourceZone = Zone.BATTLEFIELD)
        all(counterModifierDecisions(modifier, asked).map { it.verdict }) shouldBe Verdict.MATCH
        for (level in listOf(null, 1, 2)) {
            isolatedCounterModification(modifier, asked.copy(currentClassLevel = level)) shouldBe null
        }
        isolatedCounterModification(modifier, asked.copy(currentClassLevel = 3)) shouldBe 4
        isolatedCounterModification(modifier, asked.copy(currentClassLevel = 3, placer = Controller.OPPONENT)) shouldBe null
        isolatedCounterModification(modifier, asked.copy(currentClassLevel = 3, sourceZone = Zone.GRAVEYARD)) shouldBe null
        extraction.actions.single { it.path == "script.triggeredAbilities[0]" }.classAvailability shouldBe null
        extraction.actions.single { it.path == "script.classLevels[0].staticAbilities[0]" }.classAvailability?.minimumLevel shouldBe 2
        extract("Hardened Scales").counterModifiers.single().action.classAvailability shouldBe null
    }

    test("Stormchaser's Talent joins the same availability to a supported event outcome") {
        val extraction = extract("Stormchaser's Talent")
        val outcomes = extraction.eventOutcomes.filter { it.kind == OutcomeKind.CREATE_TOKEN }
        outcomes.size shouldBe 2
        val gated = outcomes.single { ".classLevels" in it.actionId }
        val availability = checkNotNull(extraction.actions.single { it.id == gated.actionId }.classAvailability)
        availability.atLevel(2) shouldBe Verdict.NO_MATCH
        availability.atLevel(3) shouldBe Verdict.MATCH
        gated.confidence shouldBe Verdict.MATCH
        extraction.actions.single { it.id == outcomes.single { it != gated }.actionId }.classAvailability shouldBe null
        extraction.unsupported.any { it.path == "script.classLevels[0].triggeredAbilities[0].trigger" } shouldBe true
    }
})
