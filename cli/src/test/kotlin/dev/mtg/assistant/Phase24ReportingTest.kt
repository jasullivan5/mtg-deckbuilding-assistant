package dev.mtg.assistant

import dev.mtg.assistant.audit.*
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Path

class Phase24ReportingTest : FunSpec({
    val c by lazy { Catalog() }
    val a by lazy { discover(c,"Judith, the Scourge Diva","name:Zombify") }
    val b by lazy { discover(c,"Judith, the Scourge Diva","name:Zombify","reanimate-own-creatures") }
    test("both objectives export identical facts and use one eligibility pipeline") {
        a.eligibility shouldBe b.eligibility;a.facts shouldBe b.facts
        val report=crossObjective(a,b);report.identicalFactEvidence shouldBe true;report.sameEligiblePool shouldBe true
        report.sharedFactIdentities shouldBe a.facts.size
    }
    test("objective B normal result is fully inspectable by existing reports") {
        b.results.single().verdict shouldBe Verdict.MATCH
        val cov=coverage(listOf(c.card("Zombify")!!),b);cov.objective shouldBe "reanimate-own-creatures"
        cov.factCounts.getValue("MOVE_OBJECT") shouldBe 1
        cov.groups.single { it.nativeType=="MoveToZone" }.supportStatus shouldBe "SUPPORTED"
        val audit=examine(listOf(c.card("Zombify")!!),b);audit.objective shouldBe b.objective
        audit.findings.none { it.check=="D_SUSPICIOUS_EXCLUSION" } shouldBe true
    }
    test("different objective snapshots cannot appear as a controlled same-objective regression") {
        diff(a,b).comparablePool shouldBe false
    }
    test("reason-only differences remain separate from mechanical verdict changes for B") {
        val r=b.results.single();val altered=b.copy(results=listOf(r.copy(decisions=r.decisions.map { it.copy(reason="presentation only") })))
        val d=diff(b,altered)
        (d.counts["POLICY_VERDICT_CHANGED"] ?: 0) shouldBe 0
        (d.counts["MECHANICAL_VERDICT_CHANGED"] ?: 0) shouldBe 0
        d.changes.any { it.attribution==listOf("EXPLANATION_ONLY") } shouldBe true
    }
    test("ownership binding uncertainty is not reported as a confirmed omission") {
        val rows=discover(c,"Judith, the Scourge Diva","name:Unearth","reanimate-own-creatures")
        val audit=examine(listOf(c.card("Unearth")!!),rows)
        audit.findings.any { it.check=="G_UNRESOLVED_OBJECTIVE_CONSTRAINT" } shouldBe true
        audit.rootIssues.any { it.category==DiagnosticCategory.MISSING_OBJECTIVE_INTERPRETATION } shouldBe true
        audit.rootIssues.none { it.category==DiagnosticCategory.CONFIRMED_BENCHMARK_MISMATCH } shouldBe true
    }
    test("frozen B review maps expected movement while absent casting remains distinct") {
        val root=if(java.nio.file.Files.exists(Path.of("benchmark"))) Path.of(".") else Path.of("..")
        val review=readBenchmark(root.resolve("benchmark/phase24/review-v1.json"))
        val evaluated=evaluateBenchmark(b,review)
        evaluated.cases.single { it.card=="Zombify" }.status shouldBe "MATCH"
        // --search restricted samples do not establish recall for omitted cards.
        evaluated.cases.single { it.card=="Reanimate" }.mappingStatus shouldBe "OUTSIDE_CONVENTIONAL_SEARCH"
    }
})
