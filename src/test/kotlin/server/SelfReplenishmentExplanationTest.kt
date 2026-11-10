package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.explorer.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SelfReplenishmentExplanationTest : FunSpec({
    val catalog by lazy { Catalog() }
    val report by lazy { discover(catalog,"Judith, the Scourge Diva") }
    fun row(name:String)=report.results.single { it.card==name && it.branch==Contribution.INTRINSIC_CREATURE_INPUT }
    test("activated self-return explains its full cost tapped entry and absence of a proven cycle") {
        val e=explain(row("Reassembling Skeleton"))
        e.requirements.any { "{1}{B}" in it } shouldBe true
        e.requirements.any { "returns tapped" in it } shouldBe true
        e.unknowns.any { "No complete sacrifice/return cycle" in it } shouldBe true
    }
    test("graveyard-triggered return preserves land entry and optionality") {
        val e=explain(row("Bloodghast"))
        e.requirements.any { "land" in it.lowercase() && "enters" in it.lowercase() } shouldBe true
        e.requirements.any { "optional" in it } shouldBe true
    }
    test("finality is visible as a barrier to a qualifying death") {
        val e=explain(row("Wolfbat"))
        e.outcome shouldBe "Input replenishment needs investigation"
        e.requirements.any { "finality" in it.lowercase() && "instead" in it.lowercase() } shouldBe true
    }
    test("return-to-hand nonmatches do not pretend that the recorded movement returns to battlefield") {
        val e=explain(row("Tymaret, the Murder King"))
        e.requirements.any { it.startsWith("Recorded movement:") && "hand" in it.lowercase() } shouldBe true
    }
})
