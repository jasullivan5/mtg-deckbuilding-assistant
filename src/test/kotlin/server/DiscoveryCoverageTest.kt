package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.explorer.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Instant

private const val REVIEWED_ENGINE = "03869c08665bf476a8b40dacbfbcd501c54a33bd"

class DiscoveryCoverageTest : FunSpec({
    val catalog by lazy { Catalog() }
    val report by lazy { discover(catalog, "Judith, the Scourge Diva") }
    fun store(input: DiscoveryReport) = StoredRun(catalog, input, Instant.EPOCH, 1, "coverage-test")

    test("retained sources bind exact native printings and unchanged Oracle snapshots") {
        coverageSpec.reviewedEngine shouldBe REVIEWED_ENGINE
        for (source in coverageSpec.cards) {
            val bytes = Files.readAllBytes(TestFiles.fixture("cards/${TestFiles.slug(source.card)}-source.json"))
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } shouldBe source.snapshotSha256
            val json = reportJson.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject
            for ((field, value) in mapOf("name" to source.card, "oracle_id" to source.oracleId,
                "id" to source.printingId, "oracle_text" to source.oracleText, "uri" to source.sourceUrl)) {
                json.getValue(field).jsonPrimitive.content shouldBe value
            }
            printingIdentity(catalog.card(source.card)!!) shouldBe source.printingId
        }
    }
    test("cohort partitions distinct physical cards and preserves unmodified results and evidence") {
        val stored = store(report)
        val comparison = stored.coverage!!
        val reviewed = report.argentumCommit == REVIEWED_ENGINE
        comparison.cards.groupingBy { it.category }.eachCount() shouldBe mapOf(
            CoverageCategory.RECOGNIZED to 4, CoverageCategory.UNRESOLVED to 1,
            CoverageCategory.POLICY_EXCLUDED to 1, CoverageCategory.INELIGIBLE to 1
        ) + if (reviewed) {
            mapOf(CoverageCategory.OBJECTIVE_GAP to 4, CoverageCategory.ORDINARY_BODY to 1)
        } else {
            mapOf(CoverageCategory.UNEXPLAINED to 5)
        }
        if (!reviewed) {
            comparison.cards.filter { it.category == CoverageCategory.UNEXPLAINED }.map { it.source.card }.toSet() shouldBe
                setOf("Grim Haruspex", "Doomed Dissenter", "Pitiless Plunderer", "Reassembling Skeleton", "Walking Corpse")
            comparison.cards.single { it.source.card == "Agent of Shauku" }.explanation shouldBe
                "Inspect the current decision records. The retained diagnosis used a different engine revision."
        }
        stored.export.report shouldBe report
        stored.summary.cards.map { it.card } shouldBe (report.results.map { it.card } + report.unsupported.map { it.card }).distinct().sorted()
        for (entry in comparison.cards) {
            val evidence = stored.card(entry.index!!)!!
            evidence.card shouldBe entry.source.card
            evidence.results shouldBe report.results.filter { it.card == evidence.card }
            evidence.actions shouldBe report.actions.filter { it.card == evidence.card }
            evidence.facts shouldBe report.facts.filter { it.card == evidence.card }
            entry.inFindings shouldBe stored.summary.cards.any { it.card == evidence.card }
        }
        val skeleton = comparison.cards.single { it.source.card == "Reassembling Skeleton" }
        skeleton.inFindings shouldBe true // A diagnostic can be indexed without a pathway.
        val plunderer = comparison.cards.single { it.source.card == "Pitiless Plunderer" }
        plunderer.inFindings shouldBe false
        stored.card(plunderer.index!!)!!.results.isEmpty() shouldBe true
        stored.card(skeleton.index!!)!!.results.isEmpty() shouldBe true
        val arbor = comparison.cards.single { it.source.card == "Dryad Arbor" }
        arbor.eligibility!!.deck shouldBe Verdict.NO_MATCH
        arbor.eligibility.identity shouldBe listOf("GREEN")
    }
    test("only the reviewed revision can use historical gap and ordinary body diagnoses") {
        // Synthetic inputs isolate classification. Neither report is execution evidence.
        val draw = "Grim Haruspex"
        val body = "Walking Corpse"
        fun fixture(revision: String) = DiscoveryReport(
            argentumCommit = revision, objective = "own-nontoken-creature-deaths",
            commander = "Judith, the Scourge Diva", allowedIdentity = listOf("BLACK", "RED"),
            conventionalQuery = null, counts = emptyMap(),
            eligibility = listOf(draw, body).map {
                EligibilityEvidence(it, Verdict.MATCH, Verdict.MATCH, listOf("BLACK"), "Test fixture")
            },
            results = emptyList(),
            actions = listOf(ActionEvidence("draw", draw, "front", "fixture", emptyList(), JsonPrimitive("DrawCards"))),
            unsupported = emptyList(), limitations = emptyList(), mechanicalInputCards = listOf(draw, body)
        )
        val historical = store(fixture(REVIEWED_ENGINE)).coverage!!.cards.associateBy { it.source.card }
        historical.getValue(draw).category shouldBe CoverageCategory.OBJECTIVE_GAP
        historical.getValue(body).category shouldBe CoverageCategory.ORDINARY_BODY
        for (card in listOf(draw, body)) {
            historical.getValue(card).explanation shouldBe historical.getValue(card).source.diagnosis
        }
        val different = store(fixture("different-engine")).coverage!!.cards.associateBy { it.source.card }
        for (card in listOf(draw, body)) {
            different.getValue(card).category shouldBe CoverageCategory.UNEXPLAINED
            different.getValue(card).explanation shouldBe "The available records do not establish the outcome for this relationship."
        }
    }
    test("scope, missing eligibility and missing definition are kept distinct") {
        store(report.copy(objective = "create-creature-tokens")).coverage shouldBe null
        store(report.copy(conventionalQuery = "Viscera")).coverage shouldBe null
        val withoutEligibility = store(report.copy(eligibility = emptyList())).coverage!!
        withoutEligibility.cards.all { it.category == CoverageCategory.UNEXPLAINED } shouldBe true
        val emptyCatalog = Catalog().also { it.registry.clear() }
        val missing = StoredRun(emptyCatalog, report, Instant.EPOCH, 1, "missing").coverage!!
        missing.cards.all { it.category == CoverageCategory.MISSING_DEFINITION && it.index == null } shouldBe true
    }
})
