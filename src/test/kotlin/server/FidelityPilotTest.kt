package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.explorer.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.withClue
import kotlinx.serialization.json.*

class FidelityPilotTest : FunSpec({
    val catalog by lazy { Catalog() }
    test("six source-bound ability scopes use the same checks without inherited qualification") {
        val reviews=SacrificePilot.scopes.map { SacrificePilot.review(catalog,it) }
        reviews.size shouldBe 6
        reviews.count { it.scope.heldOut } shouldBe 2
        reviews.map { it.checks.map { c -> c.requirement } }.distinct().size shouldBe 1
        reviews.forEach { review -> review.checks.filter { it.requirement != "OBJECT_GENERATION_IN_EXECUTION" }.forEach { check ->
            withClue("${review.scope.card}: ${check.requirement}: ${check.observation}") { check.state shouldBe "SUPPORTED" }
        } }
        reviews.all { it.checks.single { c -> c.requirement == "OBJECT_GENERATION_IN_EXECUTION" }.state == "UNTESTED" } shouldBe true
        reviews.forEach { catalog.translationAssurance(it.scope.card) shouldBe null }
    }
    test("deceptive expected creature status and wrong timing are contradicted rather than promoted") {
        val negative=SacrificePilot.scopes.single { it.card == "Seal of Fire" }
        SacrificePilot.review(catalog,negative.copy(creatureAtArrival=true)).checks.single { it.requirement == "ARRIVING_CARD_CHARACTERISTICS" }.state shouldBe "CONTRADICTED"
        val spark=SacrificePilot.scopes.single { it.card == "Spark Elemental" }
        SacrificePilot.review(catalog,spark.copy(role="COST")).checks.single { it.requirement == "COST_OR_RESOLUTION_ROLE" }.state shouldBe "CONTRADICTED"
    }
    test("fresh bounded native executions retain exact engine scenario identities negative controls and generation changes") {
        val result=SacrificePilot.execute(catalog,"pilot-test")
        result.engine shouldBe ARGENTUM_SHA
        result.scenarios.size shouldBe 11
        result.scenarios.forEach { it.state shouldBe "AGREES" }
        result.scenarios.map { it.card }.distinct().size shouldBe 6
        result.definitions.size shouldBe 6
        val blink=result.scenarios.single { it.scenarioId == "spark-blink" }.observations.jsonObject
        blink["sourceReturnedAsNewObject"]!!.jsonPrimitive.boolean shouldBe true
        reportJson.decodeFromJsonElement<com.wingedsheep.sdk.core.Zone>(blink["final"]!!.jsonObject["sourceZone"]!!) shouldBe com.wingedsheep.sdk.core.Zone.BATTLEFIELD
    }
})
