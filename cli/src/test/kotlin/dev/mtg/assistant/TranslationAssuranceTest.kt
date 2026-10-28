package dev.mtg.assistant

import dev.mtg.assistant.extensions.*
import dev.mtg.assistant.discovery.*
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import io.kotest.assertions.withClue

class TranslationAssuranceTest : FunSpec({
    fun fixture(c: CardDefinition)=TranslationEvidence(c.oracleId!!,definitionDigest(c),ARGENTUM_SHA,"fixture-author",
        fidelity=FidelityState.SOURCE_REVIEWED,execution=ExecutionState.BOUNDED_EVIDENCE_PASSED,
        source=listOf(EvidenceReference("isolated-source","a".repeat(64))),
        requirements=listOf(EvidenceReference("isolated-requirements","b".repeat(64))),
        tests=listOf(EvidenceReference("isolated-execution-receipt","c".repeat(64))))
    fun approve(e: TranslationEvidence)=e.copy(independentApproval=IndependentApproval("fixture-independent-human","2026-10-28","isolated gate fixture, not actual approval",bundleDigest(e),true))
    val c=Tatterkite
    test("text evidence ignores only checkout line endings and detects changed content") {
        val lf="Oracle: α\nCost: {T}\n"
        evidenceTextDigest(lf) shouldBe evidenceTextDigest(lf.replace("\n","\r\n"))
        (evidenceTextDigest(lf)==evidenceTextDigest(lf.replace("{T}","{0}"))) shouldBe false
        (evidenceTextDigest(lf)==evidenceTextDigest(lf+" ")) shouldBe false
        (evidenceTextDigest(lf)==evidenceTextDigest(lf.dropLast(1))) shouldBe false
        val unsupported=fixture(c).copy(source=listOf(EvidenceReference("source","a".repeat(64),"UNKNOWN")))
        assessTranslation(c,approve(unsupported)).qualification shouldBe QualificationState.WITHDRAWN
    }
    test("new definitions and passing author scenarios remain provisional") {
        assessTranslation(c,null).qualification shouldBe QualificationState.PROVISIONAL
        assessTranslation(c,fixture(c)).qualification shouldBe QualificationState.PROVISIONAL
        assessTranslation(c,fixture(c).copy(requirements=emptyList())).qualification shouldBe QualificationState.PROVISIONAL
    }
    test("only explicit separate human approval of the exact evidence bundle qualifies isolated fixture") {
        val e=fixture(c);val approved=approve(e)
        assessTranslation(c,approved).qualification shouldBe QualificationState.QUALIFIED
        assessTranslation(c,approved.copy(independentApproval=approved.independentApproval!!.copy(reviewer=e.author))).qualification shouldBe QualificationState.WITHDRAWN
        assessTranslation(c,approved.copy(independentApproval=approved.independentApproval!!.copy(reviewedAt="not-a-date"))).qualification shouldBe QualificationState.WITHDRAWN
        assessTranslation(c,approved.copy(independentApproval=approved.independentApproval!!.copy(reviewer=" FIXTURE-AUTHOR "))).qualification shouldBe QualificationState.WITHDRAWN
        assessTranslation(c,approved.copy(independentApproval=approved.independentApproval!!.copy(independentHumanReview=false))).qualification shouldBe QualificationState.WITHDRAWN
        assessTranslation(c,approve(e.copy(requirements=emptyList()))).qualification shouldBe QualificationState.WITHDRAWN
    }
    test("definition SDK source requirement and test evidence changes revoke previous approval") {
        val e=approve(fixture(c))
        assessTranslation(c.copy(oracleText="Changed source mechanics"),e).qualification shouldBe QualificationState.WITHDRAWN
        assessTranslation(c,e,"different-sdk").qualification shouldBe QualificationState.WITHDRAWN
        for(changed in listOf(
            e.copy(source=listOf(EvidenceReference("changed-source","d".repeat(64)))),
            e.copy(requirements=listOf(EvidenceReference("changed-requirements","e".repeat(64)))),
            e.copy(tests=listOf(EvidenceReference("changed-tests","f".repeat(64))))
        )) assessTranslation(c,changed).qualification shouldBe QualificationState.WITHDRAWN
    }
    val concern=TranslationConcern("fixture-TK2",ConcernKind.TRANSLATION_STRUCTURE,ConcernState.SUSPECTED,
        "Counter prohibition appears absent","Source TK2 versus deliberately mutated static abilities",
        "ReliabilityChecks.kite; isolated M7 fixture only","Run original and mutated controls")
    test("suspected confirmed disproven and resolved histories remain visible and gate promotion") {
        val base=fixture(c)
        val suspect=base.copy(concerns=listOf(concern))
        assessTranslation(c,suspect).qualification shouldBe QualificationState.BLOCKED
        assessTranslation(c,suspect.copy(concerns=listOf(concern.copy(state=ConcernState.CONFIRMED)))).qualification shouldBe QualificationState.BLOCKED
        assessTranslation(c,suspect.copy(concerns=listOf(concern.copy(state=ConcernState.RESOLVED)))).qualification shouldBe QualificationState.BLOCKED
        val resolved=suspect.copy(concerns=listOf(concern.copy(state=ConcernState.RESOLVED,resolutionEvidence=listOf("Original control passes; mutant fails; TranslationMutationTest M7"))))
        assessTranslation(c,resolved).qualification shouldBe QualificationState.PROVISIONAL
        assessTranslation(c,approve(resolved)).qualification shouldBe QualificationState.QUALIFIED
        assessTranslation(c,approve(base).copy(concerns=listOf(concern))).qualification shouldBe QualificationState.WITHDRAWN
        val extraction=base.copy(concerns=listOf(concern.copy(kind=ConcernKind.DISCOVERY_EXTRACTION)))
        assessTranslation(c,extraction).qualification shouldBe QualificationState.PROVISIONAL
    }
    test("complete representation and positive author tests cannot override discrepancy or failure") {
        for(e in listOf(fixture(c).copy(representation=RepresentationState.PARTIAL),fixture(c).copy(fidelity=FidelityState.CONFIRMED_DISCREPANCY),fixture(c).copy(execution=ExecutionState.FAILURE_OBSERVED))) {
            assessTranslation(c,e).qualification shouldBe QualificationState.BLOCKED
            assessTranslation(c,approve(e)).qualification shouldBe QualificationState.WITHDRAWN
        }
    }
    val catalog by lazy { Catalog() }
    test("historical evidence stays current with one explicit blocked engine concern and upstream unassessed") {
        Extensions.all.size shouldBe 22
        Extensions.all.forEach { ext ->
            val a=catalog.translationAssurance(ext.definition.name)!!
            a.qualification shouldBe if(ext.definition.name==Tatterkite.name) QualificationState.BLOCKED else QualificationState.PROVISIONAL
            a.evidenceCurrent shouldBe true
            a.evidence!!.independentApproval shouldBe null
        }
        catalog.translationAssurance("Forest") shouldBe null
        catalog.card("Predatory Urge") shouldBe PredatoryUrge
    }
    test("recorded references match retained files and physical identity") {
        val root=if(Files.exists(Path.of("reference"))) Path.of(".") else Path.of("..")
        Extensions.all.forEach { ext ->
            val e=ext.review.assuranceEvidence!!
            e.oracleId shouldBe ext.definition.oracleId
            (e.source+e.requirements+e.tests).forEach { ref ->
                withClue(ref.path) {
                    ref.hashFormat shouldBe EVIDENCE_TEXT_SHA256
                    val lf=Files.readString(root.resolve(ref.path)).replace("\r\n","\n")
                    evidenceTextDigest(lf) shouldBe ref.sha256
                    evidenceTextDigest(lf.replace("\n","\r\n")) shouldBe ref.sha256
                }
            }
        }
    }
    test("inspection discovery and coverage consumers retain explicit provisional status") {
        inspectCommander(catalog,"Oviya Pashiri, Sage Lifecrafter").translationAssurance!!.qualification shouldBe QualificationState.PROVISIONAL
        for(objective in listOf("own-nontoken-creature-deaths","reanimate-own-creatures","create-creature-tokens")) {
            val report=discover(catalog,"Oviya Pashiri, Sage Lifecrafter","name:Tatterkite",objective)
            report.translationAssurance.size shouldBe 22
            report.translationAssurance.all {it.qualification==QualificationState.PROVISIONAL || it.card==Tatterkite.name && it.qualification==QualificationState.BLOCKED} shouldBe true
            report.mechanicalInputCards!!.contains(Tatterkite.name) shouldBe true
        }
        // Observable qualified/provisional difference with isolated evidence, without touching registry.
        val e=approve(fixture(OviyaPashiriSageLifecrafter))
        inspectCommanderDefinition(OviyaPashiriSageLifecrafter.name,OviyaPashiriSageLifecrafter,
            provenance=Provenance("APPLICATION_EXTENSION","fixture","2026-10-28"),
            assurance=assessTranslation(OviyaPashiriSageLifecrafter,e)).translationAssurance!!.qualification shouldBe QualificationState.QUALIFIED
    }
})
