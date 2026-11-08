package dev.mtg.assistant

import dev.mtg.assistant.coverage.*
import dev.mtg.assistant.explorer.*
import dev.mtg.assistant.extensions.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow

class VerificationCoverageTest:FunSpec({
    fun record(id:String="observation",scope:String="ability-1")=VerificationRecord(id,"physical-oracle","Front // Back","EXECUTION",scope,"One declared ability",face="Back",definitionSha256="definition-a",engine=EngineBuild.revision,
        author="author",result="PASS",expected="Source requirement",observed="Recorded native state",scenarioId="scenario-a",sourceValidated=true,reference="retained/receipt.json")
    test("exact identity face definition engine and scenario are required independently") {
        val r=record()
        indexVerification(r,"definition-a",EngineBuild.revision,setOf("Front","Back")).freshness shouldBe "CURRENT"
        indexVerification(r,"different-definition",EngineBuild.revision,setOf("Front","Back")).freshness shouldBe "STALE"
        indexVerification(r,null,EngineBuild.revision,setOf("Front","Back")).freshness shouldBe "UNRESOLVED"
        indexVerification(r.copy(face="Unrelated"),"definition-a",EngineBuild.revision,setOf("Front","Back")).freshness shouldBe "UNRESOLVED"
        indexVerification(r.copy(scenarioId=null),"definition-a",EngineBuild.revision,setOf("Back")).freshness shouldBe "UNRESOLVED"
        indexVerification(r.copy(engine="old-engine"),"definition-a",EngineBuild.revision,setOf("Back")).freshness shouldBe "STALE"
        indexVerification(r.copy(engine="old-engine",historical=true,result="FAIL",definitionSha256=null),"definition-a",EngineBuild.revision,setOf("Back")).freshness shouldBe "HISTORICAL"
        indexVerification(r.copy(kind="SOURCE_REVIEW",sourceValidated=false),"definition-a",EngineBuild.revision,setOf("Back")).freshness shouldBe "STALE"
    }
    test("duplicate observations do not multiply scenarios abilities or physical cards and conflicts fail closed") {
        val a=record();val anotherScenario=record("scenario-b");val anotherAbility=record("ability-b","ability-2")
        val records=uniqueVerificationRecords(listOf(a,a,anotherScenario,anotherAbility))
        records.size shouldBe 3
        records.map { it.oracleId }.distinct().size shouldBe 1
        records.map { it.scopeId }.distinct().size shouldBe 2
        shouldThrow<IllegalArgumentException> { uniqueVerificationRecords(listOf(a,a.copy(result="FAIL"))) }
        shouldThrow<IllegalArgumentException> { uniqueVerificationRecords(listOf(a.copy(oracleId=""))) }
    }
    test("passing partial observations and open concerns never manufacture complete-card qualification") {
        val a=record().copy(materialConcerns=listOf("Unreviewed trigger condition"))
        indexVerification(a,"definition-a",EngineBuild.revision,setOf("Back")).record.materialConcerns.size shouldBe 1
        // A fresh PASS is intentionally compatible with a concern on another scope; it does not issue an approval.
        val c=Catalog();val native=c.card("Mogg Fanatic")!!
        assessTranslation(native,null).qualification shouldBe QualificationState.PROVISIONAL
        assessTranslation(native,null).evidenceCurrent shouldBe false
    }
    test("reference global baseline remains immutable and source scopes cannot silently qualify cards") {
        val baseline=retainedGlobalBaseline()
        baseline.counts.getValue("eligible_oracle_cards") shouldBe 32109
        baseline.counts.getValue("confident_native_identity_associations") shouldBe 16927
        compareGlobalBaselines(baseline,baseline).definitionChanges shouldBe emptyList()
        val service=ResearchProgressService(Catalog(),"test")
        val report=service.verification
        report.completeness shouldBe "NOT_ESTABLISHED_AT_CATALOG_SCALE"
        report.qualification.getValue("QUALIFIED") shouldBe 0
        report.qualifiedScopes shouldBe emptyList()
        report.population.size shouldBe 32109
        report.evidence.map { it.record.id }.distinct().size shouldBe report.evidence.size
        report.evidence.any { it.freshness=="HISTORICAL" && it.record.result=="FAIL" } shouldBe true
        report.evidence.filter { it.record.kind=="EXECUTION" && it.freshness=="CURRENT" }.all { it.record.scenarioId!=null && it.record.definitionSha256!=null && it.record.engine==EngineBuild.revision } shouldBe true
        report.evidence.any { it.record.materialConcerns.isNotEmpty() } shouldBe true
        val changed=compareVerification(report,report.copy(measurementDefinition="different-v2"))
        changed.comparable shouldBe false;changed.newSourceScopes shouldBe emptyList()
        shouldThrow<IllegalArgumentException> { compareVerification(report,report.copy(population=report.population+report.population.first())) }
        val old=report.copy(evidence=report.evidence.map { if(it.freshness=="CURRENT") it.copy(freshness="STALE") else it })
        compareVerification(report,old).becameStale.isNotEmpty() shouldBe true
    }
})
