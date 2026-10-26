package dev.mtg.assistant

import dev.mtg.assistant.audit.*
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*

/** Injected report/review fixtures test the examiner; the real frozen review is a separate dataset. */
class Phase22DiagnosticTest : FunSpec({
    val catalog by lazy { Catalog() }
    val report by lazy { discover(catalog,"Judith, the Scourge Diva","name:\"Goblin Bombardment\"") }
    val cost by lazy { report.results.first { it.costOrEffect=="ACTIVATION_COST" } }
    fun review()=ReviewedBenchmark("synthetic-review",ARGENTUM_SHA,"isolated diagnostic fixture","test",listOf(
        ReviewCase("cost",cost.card,"synthetic-identity",cost.face,cost.abilityPath,"EXACT",cost.branch,cost.operation,Verdict.MATCH,Verdict.MATCH,
            Contribution.DIRECT_DEATH_GENERATOR,"ACTIVATION_COST","CONFIRMED",listOf("native cost"),emptyMap(),"source fixture",emptyList())))
    test("confirmed missing path is separate from ambiguity and unreviewed acceptance") {
        val r=evaluateBenchmark(report.copy(results=report.results-cost),review())
        r.numerators["expected_accepted_missed"] shouldBe 1
        r.cases.single().issues.any { it.kind=="EXPECTED_PATHWAY_MISSED" && it.certainty=="CONFIRMED_MISMATCH" } shouldBe true
        val ambiguous=evaluateBenchmark(report.copy(results=report.results+cost),review())
        ambiguous.cases.single().mappingStatus shouldBe "AMBIGUOUS_MAPPING"
        ambiguous.numerators["confirmed_mismatch_cases"] shouldBe 0
        ambiguous.numerators["unresolved_cases"] shouldBe 1
        ambiguous.denominators["confirmed_expected_accepted_pathways"] shouldBe 0
    }
    test("policy disagreement preserves recovered mechanical capability and cost-effect mismatch is explicit") {
        val excluded=cost.copy(verdict=Verdict.NO_MATCH,policyAcceptance=Verdict.NO_MATCH,contributionClassification=null,
            decisions=cost.decisions.dropLast(1)+Decision("death-generation-policy",Verdict.NO_MATCH,"POLICY","injected policy"))
        val r=evaluateBenchmark(report.copy(results=report.results-cost+excluded),review())
        r.numerators["confirmed_mechanical_positive_recovered"] shouldBe 1
        r.cases.single().issues.any { it.kind=="POLICY_DISAGREEMENT" } shouldBe true
        val wrong=cost.copy(costOrEffect="EFFECT")
        evaluateBenchmark(report.copy(results=report.results-cost+wrong),review()).cases.single().issues.any { it.kind=="COST_EFFECT_RELATIONSHIP" } shouldBe true
    }
    test("controller and token explanation contradictions are explicitly reported") {
        val wrong=cost.copy(controllerCompatibility=Verdict.NO_MATCH,nontokenCompatibility=Verdict.NO_MATCH)
        val result=evaluateBenchmark(report.copy(results=report.results-cost+wrong),review()).cases.single()
        result.issues.any { it.kind=="INCORRECT_CONTROLLER_COMPATIBILITY" } shouldBe true
        result.issues.any { it.kind=="INCORRECT_TOKEN_COMPATIBILITY" } shouldBe true
    }
    test("root grouping ignores card names and presentation text but retains controller restrictions") {
        val action=ActionEvidence("a","A","front","script.triggeredAbilities[0]",emptyList(),JsonObject(emptyMap()))
        fun f(card: String,controller: String="ControlledByYou",prompt: String="first")=Finding("A_POTENTIAL_MISS",card,"front","script.triggeredAbilities[0].effect.suffer",card,"REVIEW","source fixture",
            Json.parseToJsonElement("""{"type":"Sacrifice","filter":{"cardPredicates":[{"type":"IsCreature"}],"controllerPredicate":{"type":"$controller"}},"prompt":"$prompt"}"""))
        val r=report.copy(actions=listOf(action.copy(id="A"),action.copy(id="B",card="B")))
        val one=rootIssues(listOf(f("A")),r).single()
        val two=rootIssues(listOf(f("A"),f("B",prompt="different")),r).single()
        one.fingerprint shouldBe two.fingerprint
        two.recordCount shouldBe 2;two.distinctCards shouldBe 2;two.distinctActions shouldBe 2
        (rootIssues(listOf(f("A","ControlledByOpponent")),r).single().fingerprint==one.fingerprint) shouldBe false
    }
    test("probable omissions require creature and battlefield-context evidence rather than a type name alone") {
        val source=catalog.card("Ball Lightning")!!.copy(name="A")
        val finding=Finding("A_POTENTIAL_MISS","A","front","script.triggeredAbilities[0].effect.suffer","a","REVIEW","fixture",Json.parseToJsonElement("""{"type":"SacrificeSelf"}"""))
        fun r(native: String)=report.copy(actions=listOf(ActionEvidence("a","A","front","script.triggeredAbilities[0]",emptyList(),Json.parseToJsonElement(native))))
        val alive=r("""{"trigger":{"type":"StepEvent","step":"END"}}""")
        rootIssues(listOf(finding),alive,authoredCards=listOf(source)).single().category shouldBe DiagnosticCategory.PROBABLE_EXTRACTION_OMISSION
        rootIssues(listOf(finding),alive).single().category shouldBe DiagnosticCategory.UNVERIFIED_WARNING
        val left=r("""{"trigger":{"type":"ZoneChangeEvent","from":"Battlefield","to":"Graveyard"},"binding":"SELF"}""")
        rootIssues(listOf(finding),left,authoredCards=listOf(source)).single().category shouldBe DiagnosticCategory.UNVERIFIED_WARNING
        val graveyard=r("""{"activateFromZone":"Graveyard"}""")
        rootIssues(listOf(finding),graveyard,authoredCards=listOf(source)).single().category shouldBe DiagnosticCategory.UNVERIFIED_WARNING
    }
    test("eligibility gaps group by missing data and never become mechanical omission claims") {
        val e=examine(emptyList(),report.copy(results=emptyList(),actions=emptyList(),unsupported=emptyList(),eligibility=listOf(
            EligibilityEvidence("A",Verdict.UNKNOWN,Verdict.MATCH,emptyList(),"missing legality"),
            EligibilityEvidence("B",Verdict.UNKNOWN,Verdict.MATCH,emptyList(),"missing legality"))))
        e.rootIssues.single().category shouldBe DiagnosticCategory.ELIGIBILITY_DATA_PROBLEM
        e.rootIssues.single().distinctCards shouldBe 2
        e.rootIssues.single().recordCount shouldBe 2
    }
    test("Murder's single ordinary removal is not suspicious merely because it contains itself") {
        val r=discover(catalog,"Judith, the Scourge Diva","name:Murder")
        examine(listOf(catalog.card("Murder")!!),r).findings.none { it.card=="Murder" && it.check=="D_SUSPICIOUS_EXCLUSION" } shouldBe true
    }
    test("benchmark diff preserves denominators and discovery diff identifies policy attribution") {
        val before=evaluateBenchmark(report,review(),"frozen")
        val changed=cost.copy(verdict=Verdict.NO_MATCH,policyAcceptance=Verdict.NO_MATCH,contributionClassification=null,
            decisions=cost.decisions.dropLast(1)+Decision("death-generation-policy",Verdict.NO_MATCH,"POLICY","injected"))
        val r=report.copy(results=report.results-cost+changed)
        val after=evaluateBenchmark(r,review(),"frozen")
        benchmarkDiff(before,after).sameFrozenReview shouldBe true
        benchmarkDiff(before,after).beforeDenominators shouldBe benchmarkDiff(before,after).afterDenominators
        diff(report,r).changes.any { it.attribution.contains("DISCOVERY_POLICY") } shouldBe true
    }
})
