package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.audit.*
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Path
import java.nio.file.Files
import kotlinx.serialization.encodeToString

class Phase25ReportingTest : FunSpec({
    val catalog by lazy { Catalog() }
    val root=if(Files.exists(Path.of("benchmark"))) Path.of(".") else Path.of("..")
    fun report(): DiscoveryReport {
        val review=readBenchmark(root.resolve("benchmark/phase25/review-v1.json"))
        val names=review.cases.map { it.card }.distinct()
        val commander=resolveCommander(catalog,"Kenrith, the Returned King")
        val identity=fullIdentity(commander)
        val cards=names.map { requireNotNull(catalog.card(it)) }
        val eligibility=cards.map { EligibilityEvidence(it.name,commanderFormatLegality(it),selectedCommanderEligibility(it,identity),fullIdentity(it).map { x -> x.name },"Actual native Commander filters applied before reviewed subset extraction") }
        val input=cards.filter { c -> eligibility.single { it.card==c.name }.let { it.format==Verdict.MATCH && it.deck==Verdict.MATCH } }
        return factObjectiveReport(catalog,commander.name,identity.map { it.name },null,eligibility,emptyMap(),PathwayExtractor().extract(input),input.map { it.name },"create-creature-tokens")
    }
    test("frozen source-reviewed benchmark evaluates development and held-out examples without mismatches") {
        val r=evaluateBenchmark(report(),root.resolve("benchmark/phase25/review-v1.json"))
        r.cases.filter { it.status=="CONFIRMED_MISMATCH" }.map { it.id to it.issues } shouldBe emptyList()
        r.denominators["reviewed_cases"] shouldBe 16
        val output=root.resolve("generated/phase25/benchmark-all-colors.json")
        Files.createDirectories(output.parent);Files.writeString(output,reportJson.encodeToString(r))
        r.cases.single { it.id=="dreadhorde-invasion" }.status shouldBe "UNRESOLVED"
    }
    test("token diagnostics use the existing independent SDK traversal and root grouping") {
        val r=report();val cards=r.mechanicalInputCards!!.map { catalog.card(it)!! }
        val c=coverage(cards,r);c.objective shouldBe "create-creature-tokens"
        (c.factCounts["CREATE_TOKEN"]!! > 0) shouldBe true
        val e=examine(cards,r)
        e.findings.any { it.card=="Dreadhorde Invasion" && it.check=="A_POTENTIAL_MISS" } shouldBe true
        e.rootIssues.any { it.category==DiagnosticCategory.MISSING_OBJECTIVE_INTERPRETATION && it.representatives.any { r -> r.card=="Rite of Replication" } } shouldBe true
        e.rootIssues.map { it.fingerprint } shouldBe examine(cards,r).rootIssues.map { it.fingerprint }
    }
    test("three objectives share the exact same facts on the same eligible pool") {
        val reports=listOf("own-nontoken-creature-deaths","reanimate-own-creatures","create-creature-tokens").map {
            discover(catalog,"Judith, the Scourge Diva","name:Kuldotha",it)
        }
        reports.map { it.facts }.distinct().size shouldBe 1
        crossObjective(reports[0],reports[2]).identicalFactEvidence shouldBe true
        crossObjective(reports[1],reports[2]).identicalFactEvidence shouldBe true
    }
})
