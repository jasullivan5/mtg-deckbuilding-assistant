package dev.mtg.assistant

import dev.mtg.assistant.explorer.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.withClue

class ActivationCostFidelityTest:FunSpec({
    val catalog by lazy { Catalog() }
    test("source-first bound complete costs support held-outs without whole-ability approval") {
        val report=ActivationCostFidelity.report(catalog,"test")
        report.reviews.size shouldBe 10
        report.reviews.forEach { withClue("${it.scope.id}: ${it.reasons} ${it.comparison}") { it.comparison.state shouldBe "SUPPORTED" } }
        report.reviews.map { it.scope.oracleId }.distinct().size shouldBe 9
        report.reviews.count { it.scope.heldOut } shouldBe 5
        report.reviews.all { it.actionId!=null && it.nativeAbilityId!=null && it.declarationAndResolution.toString().contains("effect") } shouldBe true
        report.qualification shouldBe "PENDING_SEPARATE_REVIEW_NOT_WHOLE_ABILITY_OR_CARD_QUALIFICATION"
        report.reviews.single { it.scope.card=="Sakura-Tribe Elder" }.reasons.any { it.contains("Earlier exact-printing concern retained") } shouldBe true
        val scope=ActivationCostFidelity.scopes.first()
        ActivationCostFidelity.review(catalog,scope.copy(payer="OPPONENT")).comparison.state shouldBe "UNRESOLVED"
    }
    test("expanded method detects missing mana and other payments while keeping historical target and alternative gaps") {
        val mutations=ActivationCostFidelity.mutations(catalog).associateBy { it.id }
        for(id in listOf("missing-mana","wrong-mana-amount","wrong-mana-color","generic-not-colorless","missing-sacrifice","missing-tap","wrong-sacrifice-source","wrong-sacrifice-selection","cost-to-effect","wrong-life"))
            withClue(id) { mutations.getValue(id).result shouldBe "DETECTED_CONTRADICTION" }
        mutations.getValue("alternative-not-combined").result shouldBe "REJECTED_UNRESOLVED"
        mutations.getValue("wrong-target-effect").result shouldBe "OUTSIDE_PAYMENT_SCOPE"
        mutations.getValue("wrong-target-effect").comparison.state shouldBe "SUPPORTED"
        SacrificeChallenge.report().adversarial.single { it.id=="missing-mana" }.result shouldBe "MISSED_BY_STATIC_METHOD"
    }
    test("real native payment checks retain positive negative borrowed choice and deliberately incorrect observations") {
        val receipt=ActivationCostExecution.execute(catalog,"test")
        val ordinary=receipt.observations.filter { !it.mutant && it.result!="UNRESOLVED" && !it.scenarioId.endsWith(":ILLEGAL_SELECTION") }
        ordinary.forEach { withClue("${it.scenarioId}: ${it.checks}: ${it.problem}") { it.result shouldBe "AGREES" } }
        receipt.observations.count { !it.mutant && it.result=="UNRESOLVED" } shouldBe 1
        // Wrong-type selection leaves state unchanged but the engine reports execution failure,
        // not the expected clean illegal-action rejection. Preserve this incomplete observation.
        val illegal=receipt.observations.single { it.scenarioId.endsWith(":ILLEGAL_SELECTION") }
        illegal.result shouldBe "INCOMPLETE"
        illegal.checks["no_state_or_payment_changes"] shouldBe true
        illegal.checks["native_rejection"] shouldBe false
        receipt.observations.filter { it.mutant }.forEach { withClue(it.scenarioId) { it.result shouldBe "DIFFERS" } }
        receipt.observations.filter { it.mutant }.size shouldBe 2
        receipt.observations.all { it.executed } shouldBe true
        receipt.engine shouldBe EngineBuild.revision
        receipt.observations.any { it.scenarioId.contains("BORROWED_SELECTION") && it.result=="AGREES" } shouldBe true
    }
})
