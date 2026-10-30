package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.explorer.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.time.Instant
import java.security.MessageDigest

class FindingExplanationTest : FunSpec({
    val catalog by lazy { Catalog() }
    val report by lazy { discover(catalog, "Judith, the Scourge Diva") }
    fun source(slug: String) = reportJson.parseToJsonElement(Files.readString(TestFiles.fixture("cards/$slug-source.json"))).jsonObject
    fun row(name: String) = report.results.single { it.card == name && it.operation == "SACRIFICE" }
    fun explanation(name: String) = row(name).let { result -> explain(result, report.actions.first { it.id == result.actionId }) }

    test("source snapshots and ruling receipts retain their recorded retrieval bindings") {
        val manifest = reportJson.parseToJsonElement(Files.readString(TestFiles.fixture("cards/image-and-explanation-sources.json"))).jsonObject
        for (item in manifest.getValue("sources").jsonArray) {
            val entry = item.jsonObject
            val bytes = Files.readAllBytes(TestFiles.fixture("cards/${entry.getValue("file").jsonPrimitive.content}"))
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } shouldBe entry.getValue("sha256").jsonPrimitive.content
        }
        for (slug in listOf("agent-of-shauku", "acolyte-of-aclazotz")) {
            reportJson.parseToJsonElement(Files.readString(TestFiles.fixture("cards/$slug-rulings.json"))).jsonObject.getValue("data").jsonArray.size shouldBe 0
        }
        val rulings = reportJson.parseToJsonElement(Files.readString(TestFiles.fixture("cards/accursed-centaur-rulings.json"))).jsonObject.getValue("data").jsonArray
        rulings.any { "only creature" in it.jsonObject.getValue("comment").jsonPrimitive.content } shouldBe true
    }

    test("land sacrifice is investigated without claiming a represented land-creature interaction") {
        source("agent-of-shauku")["oracle_text"]!!.jsonPrimitive.content.contains("Sacrifice a land") shouldBe true
        val result = row("Agent of Shauku")
        result.verdict shouldBe Verdict.UNKNOWN
        result.nativeOperation.jsonObject["filter"]!!.jsonObject["cardPredicates"]!!.jsonArray.single().jsonObject["type"]!!.jsonPrimitive.content shouldBe "IsLand"
        explanation(result.card).summary.contains("a land as a cost") shouldBe true
        explanation(result.card).unknowns.any { "has not established a land becoming a creature" in it } shouldBe true
        explanation(result.card).requirements.any { "{1}{B}" in it } shouldBe true
    }
    test("creature-or-artifact sacrifice retains the choice and excludes its source") {
        source("acolyte-of-aclazotz")["oracle_text"]!!.jsonPrimitive.content.contains("Sacrifice another creature or artifact") shouldBe true
        val result = row("Acolyte of Aclazotz")
        result.verdict shouldBe Verdict.MATCH
        result.nativeOperation.jsonObject["excludeSelf"]!!.jsonPrimitive.boolean shouldBe true
        val predicates = result.nativeOperation.jsonObject["filter"]!!.jsonObject["cardPredicates"]!!.jsonArray.single().jsonObject["predicates"]!!.jsonArray
        predicates.map { it.jsonObject["type"]!!.jsonPrimitive.content } shouldBe listOf("IsCreature", "IsArtifact")
        val e = explanation(result.card)
        e.summary.contains("(a creature) or (an artifact)") shouldBe true
        e.requirements.any { "Tap this source" in it } shouldBe true
        e.requirements.any { "excluding this source" in it } shouldBe true
        e.requirements.any { "ordinary noncreature permanent does not meet" in it } shouldBe true
    }
    test("mandatory enters sacrifice is distinct from an activation cost and does not exclude self") {
        source("accursed-centaur")["oracle_text"]!!.jsonPrimitive.content.contains("sacrifice a creature") shouldBe true
        val result = row("Accursed Centaur")
        result.verdict shouldBe Verdict.MATCH
        result.costOrEffect shouldBe "EFFECT"
        result.circumstances!!.mandatory shouldBe Verdict.MATCH
        val e = explanation(result.card)
        e.summary.contains("instructs a player to sacrifice a creature") shouldBe true
        e.summary.contains("source itself cannot") shouldBe false
        e.requirements.any { "required sacrifice" in it } shouldBe true
        e.requirements.any { "when this source enters" in it } shouldBe true
    }
    test("presentation index counts are physical-card sets and categories overlap without rewriting evidence") {
        val stored = StoredRun(catalog, report, Instant.EPOCH, 1, "fixture")
        stored.summary.cards.size shouldBe (report.results.map { it.card } + report.unsupported.map { it.card }).distinct().size
        for (verdict in Verdict.entries) {
            stored.summary.cards.count { it.pathways.getValue(verdict.name) > 0 } shouldBe
                report.results.filter { it.verdict == verdict }.map { it.card }.distinct().size
            stored.summary.pathwayCounts.getValue(verdict.name) shouldBe report.results.count { it.verdict == verdict }
        }
        for (name in listOf("Agent of Shauku", "Acolyte of Aclazotz", "Accursed Centaur")) {
            val finding = stored.summary.cards.single { it.card == name }
            stored.card(finding.index)!!.results shouldBe report.results.filter { it.card == name }
            finding.mechanics shouldBe listOf("Sacrifice")
        }
        val excluded = report.results.first { row -> row.decisions.any { it.verdict == Verdict.NO_MATCH && it.basis == "POLICY" } }
        explain(excluded).outcome.contains("selection rules") shouldBe true
    }
})
