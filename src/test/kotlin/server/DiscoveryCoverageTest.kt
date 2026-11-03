package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.explorer.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Instant

class DiscoveryCoverageTest : FunSpec({
    val catalog by lazy { Catalog() }
    val report by lazy { discover(catalog, "Judith, the Scourge Diva") }
    fun store(input: DiscoveryReport) = StoredRun(catalog, input, Instant.EPOCH, 1, "coverage-test")

    test("retained sources bind exact native printings and unchanged Oracle snapshots") {
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
        comparison.cards.groupingBy { it.category }.eachCount() shouldBe mapOf(
            CoverageCategory.RECOGNIZED to 4, CoverageCategory.UNRESOLVED to 1,
            CoverageCategory.OBJECTIVE_GAP to 4, CoverageCategory.ORDINARY_BODY to 1,
            CoverageCategory.POLICY_EXCLUDED to 1, CoverageCategory.INELIGIBLE to 1
        )
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
    test("scope, missing eligibility, missing definition and changed engine evidence are kept distinct") {
        store(report.copy(objective = "create-creature-tokens")).coverage shouldBe null
        store(report.copy(conventionalQuery = "Viscera")).coverage shouldBe null
        val withoutEligibility = store(report.copy(eligibility = emptyList())).coverage!!
        withoutEligibility.cards.all { it.category == CoverageCategory.UNEXPLAINED } shouldBe true
        val otherEngine = store(report.copy(argentumCommit = "other")).coverage!!
        otherEngine.cards.single { it.source.card == "Grim Haruspex" }.category shouldBe CoverageCategory.UNEXPLAINED
        val emptyCatalog = Catalog().also { it.registry.clear() }
        val missing = StoredRun(emptyCatalog, report, Instant.EPOCH, 1, "missing").coverage!!
        missing.cards.all { it.category == CoverageCategory.MISSING_DEFINITION && it.index == null } shouldBe true
    }
})
