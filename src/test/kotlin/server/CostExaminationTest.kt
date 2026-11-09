package dev.mtg.assistant

import dev.mtg.assistant.explorer.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*

class CostExaminationTest:FunSpec({
    val catalog by lazy { Catalog() }
    val receipt by lazy { CostPaymentBoundary.execute(catalog,"test-native-examination") }
    test("source and native phases preserve raw freeze identities through LF transport without granting qualification") {
        CostExamination.frozenHash("source-examination.json") shouldBe "151853ce0c427845a04c359f1aa5a58e56c92c1a817e43060d726189e581c6b3"
        CostExamination.frozenHash("native-examination.json") shouldBe "07a44c79b23ba32805df78ec3d534bcc19ba4deb996303be53346b45c743e40a"
        CostExamination.frozenArtifactCurrent("source-examination.json") shouldBe true
        CostExamination.frozenArtifactCurrent("source-examination.json","changed requirement content") shouldBe false
        val report=CostExamination.report(catalog,"test")
        report.sourceState shouldBe "CURRENT_SOURCE_BINDINGS"
        report.nativeState shouldBe "CURRENT_NATIVE_BINDINGS"
        report.counts.getValue("examined_source_scopes") shouldBe 10
        report.counts.getValue("examined_native_scopes") shouldBe 10
        report.counts.getValue("independent_approvals") shouldBe 0
        report.native.getValue("independence").jsonObject.getValue("originalMethodResultsExposed").jsonPrimitive.boolean shouldBe false
        report.reviewStatus shouldBe "ISOLATED_AGENT_EXAMINATION_PENDING_EXTERNAL_QUALIFICATION"
        report.additionalChallenge.result shouldBe "DETECTED_CONTRADICTION"
    }
    test("changed source engine definition or harness cannot inherit a current boundary observation") {
        fun current(r:PaymentBoundaryReceipt)=CostExamination.bindingsCurrent(r,receipt.engine,receipt.harnessSha256,receipt.inputs,receipt.definitions)
        current(receipt) shouldBe true
        current(receipt.copy(engine="different-engine")) shouldBe false
        current(receipt.copy(harnessSha256="different-assertions")) shouldBe false
        current(receipt.copy(inputs=receipt.inputs+("source-examination.json" to "changed"))) shouldBe false
        current(receipt.copy(definitions=receipt.definitions-"Lifespark Spellbomb")) shouldBe false
        current(receipt.copy(scenarios=receipt.scenarios+receipt.scenarios.first())) shouldBe false
        val first=receipt.scenarios.first()
        current(receipt.copy(scenarios=listOf(first.copy(steps=emptyList())))) shouldBe false
    }
    test("native selected-creature refusals and decision recovery preserve exact outcomes rather than treating rollback alone as legality") {
        val rows=receipt.scenarios.associateBy { it.id }
        for(id in listOf("SELF","OTHER","BORROWED")) rows.getValue("selected-creature:$id").result shouldBe "BOUNDED_EXPECTATION_SUPPORTED"
        for(id in listOf("NONCREATURE","FOREIGN_CONTROL")) {
            val row=rows.getValue("selected-creature:$id")
            row.result shouldBe "REFUSAL_SUPPORTED_DIAGNOSTIC_LIMIT"
            row.steps.single().validationMessage shouldBe null
            row.steps.single().rejection shouldBe "ExecutionFailed"
            row.steps.single().before shouldBe row.steps.single().after
            row.steps.single().nativeResult.jsonObject.getValue("events").jsonArray.size shouldBe 0
        }
        val choice=rows.getValue("selected-creature:CHOICE")
        choice.steps.map { it.outcome } shouldBe listOf("Paused","Rejected","Done")
        choice.steps[1].rejection shouldBe "IllegalAction"
        choice.checks.values.all { it } shouldBe true
        rows.getValue("activation-boundary:unpayable-mana").steps.single().rejection shouldBe "IllegalAction"
        rows.getValue("activation-boundary:invalid-target").steps.single().rejection shouldBe "IllegalAction"
    }
    test("known Explicit tap-payment failures remain visible despite structurally correct costs and passing research assertions") {
        receipt.scenarios.size shouldBe 14
        receipt.scenarios.sumOf { it.steps.size } shouldBe 16
        receipt.scenarios.count { it.result=="SOURCE_EXPECTATION_CONTRADICTED" } shouldBe 2
        for(scope in listOf("mind-stone:0","evolving-wilds:0")) {
            val rows=receipt.scenarios.associateBy { it.id }
            rows.getValue("tap-boundary:$scope:UNTAPPED_EXPLICIT").result shouldBe "BOUNDED_EXPECTATION_SUPPORTED"
            rows.getValue("tap-boundary:$scope:TAPPED_AUTO").steps.single().rejection shouldBe "IllegalAction"
            val broken=rows.getValue("tap-boundary:$scope:TAPPED_EXPLICIT")
            broken.result shouldBe "SOURCE_EXPECTATION_CONTRADICTED"
            broken.steps.single().validationMessage shouldBe null
            broken.steps.single().outcome shouldBe "Done"
            broken.checks.getValue("unchanged_state") shouldBe false
            broken.steps.single().nativeResult.jsonObject.getValue("events").jsonArray.none { it.jsonObject.getValue("type").jsonPrimitive.content=="TappedEvent" } shouldBe true
        }
    }
})
