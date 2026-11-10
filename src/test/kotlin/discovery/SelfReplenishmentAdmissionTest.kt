package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Identical API assertions run against unmodified merged main to demonstrate the omission. */
class SelfReplenishmentAdmissionTest : FunSpec({
    val report by lazy { discover(Catalog(),"Judith, the Scourge Diva") }
    for(name in listOf("Reassembling Skeleton","Bloodghast","Bladewing's Thrall")) test("$name supplies intrinsic creature inputs") {
        report.results.any { it.card==name && it.branch.name=="INTRINSIC_CREATURE_INPUT" && it.verdict==Verdict.MATCH } shouldBe true
    }
})
