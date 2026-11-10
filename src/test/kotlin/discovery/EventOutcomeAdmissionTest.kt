package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Uses only the pre-existing API, so the identical checks can show the old omissions. */
class EventOutcomeAdmissionTest : FunSpec({
    val report by lazy { discover(Catalog(),"Judith, the Scourge Diva") }
    for(name in listOf("Grim Haruspex","Pitiless Plunderer","Doomed Dissenter")) test("$name has an admitted death-triggered outcome") {
        report.results.any { it.card==name && it.branch.name=="DEATH_TRIGGERED_OUTCOME" && it.verdict==Verdict.MATCH } shouldBe true
    }
})
