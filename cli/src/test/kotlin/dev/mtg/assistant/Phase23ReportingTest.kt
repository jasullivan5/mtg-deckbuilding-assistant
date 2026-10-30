package dev.mtg.assistant

import dev.mtg.assistant.audit.*
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Path

class Phase23ReportingTest : FunSpec({
    val catalog by lazy { Catalog() }
    val report by lazy { discover(catalog,"Judith, the Scourge Diva","name:\"Goblin Bombardment\"") }
    val cost by lazy { report.results.first { it.costOrEffect=="ACTIVATION_COST" } }
    test("rewritten decision reasons are explanation-only not policy or mechanical changes") {
        val changed=report.copy(results=report.results.map { it.copy(decisions=it.decisions.map { d -> d.copy(reason="New presentation: "+d.reason) }) })
        val d=diff(report,changed)
        d.transitions["POLICY_VERDICT_CHANGED"] shouldBe null
        d.transitions["MECHANICAL_STAGE_VERDICT_CHANGED"] shouldBe null
        d.transitions["EXPLANATION_ONLY_CHANGED"] shouldBe report.results.size
        d.changes.filter { it.kind=="PATHWAY_CHANGED" }.all { it.attribution==listOf("EXPLANATION_ONLY") } shouldBe true
    }
    test("constraint and structural evidence changes are independent of verdict changes") {
        val c=cost.copy(prerequisites=cost.prerequisites+"New actual prerequisite",enablingRelationship="different relationship")
        val d=diff(report,report.copy(results=report.results-cost+c))
        d.transitions["CONDITIONS_OR_CONSTRAINTS_CHANGED"] shouldBe 1
        d.transitions["STRUCTURAL_EVIDENCE_CHANGED"] shouldBe 1
        d.transitions["POLICY_VERDICT_CHANGED"] shouldBe null
    }
    test("variant coverage does not claim a whole type from one supported instance") {
        val r=discover(catalog,"Judith, the Scourge Diva","name:\"Arms Race\"")
        val c=coverage(listOf(catalog.card("Arms Race")!!),r)
        c.groups.any { it.nativeType=="ConditionalOnCollection" && it.supportStatus=="PARTIALLY_SUPPORTED" } shouldBe true
        r.results.any { it.verdict==Verdict.MATCH && it.interpretation.contains("PARTIAL") } shouldBe true
        // Haste's unresolved enclosing effect does not erase the supported local sacrifice.
        examine(listOf(catalog.card("Arms Race")!!),r).findings.any { it.check=="B_INCOMPLETE_ACCEPTED_ACTION" } shouldBe true
    }
    test("both frozen reviews remain inspectable without treating unreviewed paths as confirmed errors") {
        val r=discover(catalog,"Judith, the Scourge Diva")
        val old=evaluateBenchmark(r,Path.of("src/test/resources/benchmarks/deaths-initial.json"))
        old.numerators["confirmed_mismatch_cases"] shouldBe 0
        val added=evaluateBenchmark(r,Path.of("src/test/resources/benchmarks/deaths-expanded.json"))
        added.numerators["expected_accepted_recovered"] shouldBe 11
        added.numerators["incorrect_accepted_reviewed_cases"] shouldBe 0
        added.numerators["confirmed_mismatch_cases"] shouldBe 0
    }
    test("recognized sacrifice cost with unresolved filter is partial at both cost and keyword variants") {
        val c=catalog.card("Lava Dart")!!
        val r=discover(catalog,"Judith, the Scourge Diva","name:\"Lava Dart\"")
        r.results.single { it.costOrEffect=="ALTERNATIVE_CASTING_COST" }.localSupport shouldBe SupportStatus.PARTIALLY_SUPPORTED
        val groups=coverage(listOf(c),r).groups
        groups.any { it.nativeType=="AtomSacrifice" && it.supportStatus=="PARTIALLY_SUPPORTED" } shouldBe true
        groups.any { it.nativeType=="Flashback" && it.supportStatus=="PARTIALLY_SUPPORTED" } shouldBe true
    }
    test("scope presentation and new annotation schemas do not inflate structural or policy changes") {
        val c=cost.copy(affectedScope=cost.affectedScope+" (same facts rendered differently)")
        val d=diff(report,report.copy(results=report.results-cost+c))
        d.transitions["EXPLANATION_ONLY_CHANGED"] shouldBe 1
        d.transitions["STRUCTURAL_EVIDENCE_CHANGED"] shouldBe null
        d.transitions["POLICY_VERDICT_CHANGED"] shouldBe null
        val legacy=cost.copy(localSupport=null)
        val a=diff(report.copy(results=report.results-cost+legacy),report)
        a.transitions["SUPPORT_STATUS_ANNOTATION_ADDED_OR_REMOVED"] shouldBe 1
        a.transitions["STRUCTURAL_EVIDENCE_CHANGED"] shouldBe null
    }
    test("accepted keyword cost remains incomplete when its associated spell resolution is unresolved") {
        val c=catalog.card("Dread Return")!!
        val r=discover(catalog,"Judith, the Scourge Diva","name:\"Dread Return\"")
        val cost=r.results.single { it.costOrEffect=="ALTERNATIVE_CASTING_COST" }
        cost.verdict shouldBe Verdict.MATCH
        cost.localSupport shouldBe SupportStatus.SUPPORTED
        cost.interpretation.contains("PARTIAL") shouldBe true
        val e=examine(listOf(c),r)
        e.findings.any { it.check=="B_INCOMPLETE_ACCEPTED_ACTION" && it.path==cost.abilityPath } shouldBe true
    }
})
