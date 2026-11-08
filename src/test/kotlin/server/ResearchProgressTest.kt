package dev.mtg.assistant

import dev.mtg.assistant.audit.*
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.explorer.*
import dev.mtg.assistant.extensions.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import kotlinx.serialization.json.*

class ResearchProgressTest : FunSpec({
    fun group(state: String, count: Int) = CoverageGroup(state, "FixtureFamily", "script", JsonNull,
        count, 1, 1, state, emptyList(), "UNRESOLVED", 0, emptyList(), "Review")
    test("typed-node distribution is disjoint while cards and type families can overlap") {
        val audit = CoverageReport(commander="fixture", inputCards=1, encounteredTypedNodes=6,
            groups=listOf(group("SUPPORTED",1),group("PARTIALLY_SUPPORTED",2),group("UNSUPPORTED",1),group("OBSERVED_WITHOUT_LOCAL_PATHWAY",2)))
        val measured = progressDistribution(audit)
        measured.denominator shouldBe 6
        measured.counts["SUPPORTED"] shouldBe 1
        measured.counts.values.sum() shouldBe 6
        // One supported node alongside five other nodes must not imply one fully interpreted card.
        measured.unit shouldBe "serialized native typed-node occurrences"
        shouldThrow<IllegalStateException> { progressDistribution(audit.copy(encounteredTypedNodes=7)) }
    }
    test("missing and stale scopes cannot inflate source review execution or qualification") {
        val card = Extensions.all.first { it.definition.name != "Tatterkite" }.definition
        val missing = assessTranslation(card, null)
        val recorded = Catalog().translationAssurance("Tatterkite")!!
        val stale = assessTranslation(card, recorded.evidence, pin="0000000000000000000000000000000000000000")
        stale.evidenceCurrent shouldBe false
        stale.sourceFidelity shouldBe FidelityState.UNREVIEWED
        stale.engineExecution shouldBe ExecutionState.NOT_TESTED
        val a = progressAssurance(20, listOf(missing, stale.copy(card="stale-fixture")))
        a.notMeasured shouldBe 18
        a.current shouldBe 0
        a.stale shouldBe 1
        a.missing shouldBe 1
        a.fidelity.values.sum() shouldBe 0
        a.qualification["QUALIFIED"] shouldBe 0
        a.qualification["BLOCKED"] shouldBe 1
        shouldThrow<IllegalArgumentException> { progressAssurance(20, listOf(missing,missing)) }
    }
    test("native audit retains unsupported enclosing branches and absence is not NO_MATCH") {
        val catalog = Catalog()
        val service = ResearchProgressService(catalog, "progress-test")
        val report = service.report
        report.external.state shouldBe "NOT_MEASURED"
        report.external.counts.isEmpty() shouldBe true
        report.population.size shouldBe physicalCards(catalog.cards).size
        report.definitionOrigins.values.sum() shouldBe report.population.size
        report.mechanics.counts.values.sum() shouldBe report.mechanics.denominator
        report.mechanics.counts.getValue("OBSERVED_WITHOUT_LOCAL_PATHWAY").let { it > 0 } shouldBe true
        report.mechanics.counts.getValue("UNSUPPORTED").let { it > 0 } shouldBe true
        report.assurance.current + report.assurance.stale + report.assurance.missing shouldBe report.assurance.records.size
        report.assurance.notMeasured + report.assurance.records.size shouldBe report.population.size
        service.nativeCoverage.groups.any { it.representatives.any { r -> r.card == "Pit Raptor" } && it.supportStatus != "SUPPORTED" } shouldBe true
        report.applicationRevision shouldBe "progress-test"
        report.argentumCommit shouldBe EngineBuild.revision
        report.families.sumOf { it.occurrences } shouldBe report.mechanics.denominator
        report.families.sumOf { it.supported } shouldBe (report.mechanics.counts["SUPPORTED"] ?: 0)
        report.assurance.records.single { it.card=="Tatterkite" }.qualification shouldBe QualificationState.BLOCKED
    }
    test("unavailable explicit source cannot become zero missing cards") {
        val service = ResearchProgressService(Catalog(), "fixture", java.nio.file.Path.of("absent-progress-snapshot"))
        service.externalRecords shouldBe null
        service.report.external.state shouldBe "UNAVAILABLE"
        service.report.external.counts.isEmpty() shouldBe true
    }
    test("historical comparison retains exact pool revisions negative cases and controlled gains") {
        val h = reportJson.parseToJsonElement(javaClass.getResource("/research-history.json")!!.readText()).jsonObject
        h["observation"]!!.jsonPrimitive.content shouldBe "HISTORICAL_RECORDED_NOT_CURRENT_EXECUTION"
        h["engine"]!!.jsonPrimitive.content shouldBe "03869c08665bf476a8b40dacbfbcd501c54a33bd"
        h["population"]!!.jsonArray.size shouldBe 6111
        val comparisons = h["comparisons"]!!.jsonArray.map { it.jsonObject }
        comparisons.map { it["comparable"]!!.jsonPrimitive.boolean } shouldBe listOf(true,true)
        comparisons.map { it["newMatchingCards"]!!.jsonArray.size } shouldBe listOf(8,88)
        comparisons.map { it["newMatchingPathways"]!!.jsonPrimitive.int } shouldBe listOf(9,93)
        comparisons[1]["resolvedNonmatches"]!!.jsonPrimitive.int shouldBe 223
        comparisons.forEach { it["lostMatches"]!!.jsonArray.size shouldBe 0; it["removedPathways"]!!.jsonArray.size shouldBe 0 }
        comparisons[1]["changes"]!!.jsonArray.any { it.jsonObject["card"]!!.jsonPrimitive.content=="Seal of Fire" && it.jsonObject["after"]!!.jsonPrimitive.content=="NO_MATCH" } shouldBe true
    }
})
