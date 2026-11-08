package dev.mtg.assistant

import dev.mtg.assistant.explorer.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SacrificeChallengeTest:FunSpec({
    test("frozen method retains detected rejected missed and execution-dependent distinctions") {
        val report=SacrificeChallenge.report()
        report.methodUnchanged shouldBe true
        val findings=report.adversarial.associateBy { it.id }
        for(id in listOf("missing-target","missing-tap","wrong-arrival-type","wrong-trigger-step")) findings.getValue(id).result shouldBe "DETECTED_CONTRADICTION"
        findings.getValue("wrong-source").result shouldBe "NOT_ACCEPTED_UNRESOLVED"
        findings.getValue("effect-not-cost").result shouldBe "NOT_ACCEPTED_UNRESOLVED"
        findings.getValue("missing-mana").result shouldBe "MISSED_BY_STATIC_METHOD"
        findings.getValue("wrong-target-filter").result shouldBe "MISSED_BY_STATIC_METHOD"
        findings.getValue("generation-guard").result shouldBe "EXECUTION_DEPENDENT"
        findings.getValue("owner-replacement").result shouldBe "EXECUTION_DEPENDENT"
        report.reviews.size shouldBe 5
        report.reviews.count { it.definitionSha256==null } shouldBe 2
        report.reviews.single { it.scope.card=="Caustic Caterpillar" }.scope.heldOut shouldBe true
        report.reviewStatus shouldBe "PENDING_SEPARATE_REVIEW_NOT_INDEPENDENTLY_QUALIFIED"
    }
    test("shared payment pattern executes actual positive and missing-resource positions") {
        val receipt=SacrificeChallenge.execute(Catalog(),"challenge-test")
        receipt.scenarios.size shouldBe 5
        receipt.scenarios.map { it.card }.distinct().size shouldBe 3
        receipt.scenarios.filterNot { it.scenarioId.startsWith("mutant:") }.forEach { it.state shouldBe "AGREES" }
        receipt.scenarios.single { it.scenarioId.startsWith("mutant:") }.state shouldBe "DIFFERS"
        receipt.engine shouldBe EngineBuild.revision
        receipt.definitions.size shouldBe 4
    }
})
