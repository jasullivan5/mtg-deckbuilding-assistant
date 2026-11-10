package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*

class EventOutcomeDiscoveryTest : FunSpec({
    val catalog by lazy { Catalog() }
    val report by lazy { discover(catalog,"Judith, the Scourge Diva") }
    test("source-frozen response cases are interpreted without production name exceptions") {
        val source=java.io.File("src/test/resources/regression/event-outcomes-expectations.json").readText()
        for(c in Json.parseToJsonElement(source).jsonObject.getValue("cases").jsonArray) {
            val obj=c.jsonObject;val name=obj.getValue("card").jsonPrimitive.content
            val x=PathwayExtractor().extract(listOf(catalog.card(name) ?: error("Missing source-reviewed native case $name")))
            val rows=deathResponses(x.eventOutcomes)
            when(obj.getValue("expected").jsonPrimitive.content) {
                "MATCH" -> rows.any { it.verdict==Verdict.MATCH } shouldBe true
                "UNKNOWN" -> rows.any { it.verdict==Verdict.UNKNOWN } shouldBe true
                "NONDEATH" -> {
                    x.eventOutcomes.any { it.kind==OutcomeKind.DRAW_CARDS && it.confidence==Verdict.MATCH } shouldBe true
                    rows.all { it.verdict==Verdict.NO_MATCH } shouldBe true
                }
                "NONDEATH_UNKNOWN_BRANCHES" -> {
                    x.eventOutcomes.any { it.kind==OutcomeKind.CREATE_TOKEN && it.confidence==Verdict.UNKNOWN } shouldBe true
                    rows.all { it.verdict==Verdict.NO_MATCH } shouldBe true
                }
                else -> rows.any { it.verdict==Verdict.MATCH || it.verdict==Verdict.UNKNOWN } shouldBe false
            }
        }
    }
    test("new draw creature token and Treasure responses appear separately from causing deaths") {
        for(name in listOf("Grim Haruspex","Pitiless Plunderer","Doomed Dissenter","Midnight Reaper")) {
            val row=report.results.first { it.card==name && it.branch==Contribution.DEATH_TRIGGERED_OUTCOME && it.verdict==Verdict.MATCH }
            row.eventOutcome!!.actionId shouldBe row.actionId
            row.policyAcceptance shouldBe Verdict.MATCH
        }
        report.results.filter { it.card=="Pitiless Plunderer" && it.branch==Contribution.DIRECT_DEATH_GENERATOR && it.verdict==Verdict.MATCH }.size shouldBe 0
    }
    test("legacy sacrifice and death-enhanced history removal retain their paths") {
        report.results.any { it.card=="Viscera Seer" && it.branch==Contribution.DIRECT_DEATH_GENERATOR && it.verdict==Verdict.MATCH } shouldBe true
        report.results.any { it.card=="Tragic Slip" && it.branch==Contribution.DEATH_ENHANCED_REMOVAL && it.verdict==Verdict.MATCH } shouldBe true
        report.results.any { it.card=="Tragic Slip" && it.branch==Contribution.DEATH_TRIGGERED_OUTCOME } shouldBe false
    }
    test("a new response on a card does not promote its old policy-excluded sacrifice effect") {
        val rows=report.results.filter { it.card=="Liliana, Dreadhorde General" }
        rows.any { it.branch==Contribution.DEATH_TRIGGERED_OUTCOME && it.verdict==Verdict.MATCH } shouldBe true
        rows.any { it.branch==Contribution.DIRECT_DEATH_GENERATOR && it.verdict==Verdict.NO_MATCH } shouldBe true
        (rows.map { it.actionId }.distinct().size > 1) shouldBe true
    }
})
