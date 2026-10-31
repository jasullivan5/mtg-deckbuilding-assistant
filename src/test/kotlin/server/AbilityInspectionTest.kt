package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.explorer.*
import dev.mtg.assistant.extensions.QualificationState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.time.Instant

class AbilityInspectionTest : FunSpec({
    val catalog by lazy { Catalog() }
    val report by lazy { discover(catalog, "Judith, the Scourge Diva") }
    val stored by lazy { StoredRun(catalog, report, Instant.EPOCH, 1, "isolated-test") }
    fun card(name: String) = stored.card(stored.summary.cards.single { it.card == name }.index)!!

    test("Attrition keeps payment target and destruction in one activation without claiming full verification") {
        val source = reportJson.parseToJsonElement(Files.readString(TestFiles.fixture("cards/attrition-source.json"))).jsonObject
        source.getValue("name").jsonPrimitive.content shouldBe "Attrition"
        source.getValue("oracle_text").jsonPrimitive.content shouldBe "{B}, Sacrifice a creature: Destroy target nonblack creature."
        val evidence = card("Attrition")
        val ability = evidence.abilities.single()
        ability.face shouldBe null
        ability.verdict shouldBe Verdict.MATCH
        ability.costs.any { "{B}" in it } shouldBe true
        ability.costs.any { "Sacrifice a creature" in it } shouldBe true
        ability.effects shouldBe listOf("Destroy target nonblack creature.")
        ability.conditions.any { "nonblack creature" in it } shouldBe true
        ability.conditions.any { "cannot activate" in it && "without that target" in it } shouldBe true
        ability.resultIndexes.map { evidence.results[it].verdict }.toSet() shouldBe setOf(Verdict.MATCH, Verdict.NO_MATCH)
        ability.evidence.single { it.id == "mechanic" }.state shouldBe EvidenceState.ESTABLISHED
        for (id in listOf("source", "execution", "independent")) ability.evidence.single { it.id == id }.state shouldBe EvidenceState.NOT_RECORDED
        evidence.results shouldBe report.results.filter { it.card == "Attrition" }
        ability.questions.any { "nonblack creature" in it.detail } shouldBe true
    }
    test("optional targets do not become mandatory activation dependencies in the formatter") {
        val evidence = card("Attrition")
        val id = evidence.abilities.single().actionId
        val actions = evidence.actions.map { a -> if (a.id != id) a else {
            val node = a.native.jsonObject
            val requirement = node.getValue("targetRequirements").jsonArray.single().jsonObject
            a.copy(native = JsonObject(node + ("targetRequirements" to JsonArray(listOf(JsonObject(requirement + mapOf("optional" to JsonPrimitive(true), "minCount" to JsonPrimitive(0))))))))
        } }
        val ability = abilityInspections(evidence.results, actions, evidence.unsupported, null, emptyList()).single()
        ability.conditions.any { "cannot activate" in it } shouldBe false
        ability.conditions.any { "If you choose a target" in it } shouldBe true
    }
    test("Agent's represented land payment stays an unresolved objective relationship") {
        val ability = card("Agent of Shauku").abilities.single()
        ability.verdict shouldBe Verdict.UNKNOWN
        ability.costs.any { "Sacrifice a land" in it } shouldBe true
        ability.uncertainties.any { "has not established a land becoming a creature" in it } shouldBe true
        ability.evidence.single { it.id == "mechanic" }.state shouldBe EvidenceState.INCOMPLETE
    }
    test("Acolyte's alternatives and other life effects stay together without upgrading verification") {
        val ability = card("Acolyte of Aclazotz").abilities.single()
        ability.costs.any { "(a creature) or (an artifact)" in it && "other than this source" in it } shouldBe true
        ability.effects shouldBe listOf("Each opponent loses 1 life.", "You gain 1 life.")
        ability.evidence.single { it.id == "mechanic" }.state shouldBe EvidenceState.ESTABLISHED
        ability.evidence.single { it.id == "execution" }.state shouldBe EvidenceState.NOT_RECORDED
        ability.questions.isNotEmpty() shouldBe true
    }
    test("Judith's death-trigger connection and historical card evidence do not become independent certification") {
        val evidence = card("Judith, the Scourge Diva")
        val ability = evidence.abilities.first { it.verdict == Verdict.MATCH }
        ability.face shouldBe null
        ability.conditions.any { "nontoken creature you control" in it } shouldBe true
        ability.effects.any { "1 damage" in it } shouldBe true
        ability.contribution.contains("nontoken creature you control") shouldBe true
        ability.contribution.contains("1 damage") shouldBe true
        ability.contribution.contains("how that first creature died") shouldBe true
        val assurance = catalog.translationAssurance("Judith, the Scourge Diva")!!
        assurance.qualification shouldBe QualificationState.PROVISIONAL
        ability.evidence.single { it.id == "source" }.state shouldBe if (assurance.evidenceCurrent) EvidenceState.ESTABLISHED else EvidenceState.OUTDATED
        ability.evidence.single { it.id == "execution" }.historical shouldBe true
        ability.evidence.single { it.id == "independent" }.state shouldBe EvidenceState.NOT_RECORDED
        val view = commanderView(catalog, evidence.card, inspectCommander(catalog, evidence.card))
        view.headline shouldBe "Judith, the Scourge Diva can be your commander · Black and red"
        view.authoredCard!!.text.contains("Whenever a nontoken creature you control dies") shouldBe true
    }
    test("ordinary navigation gives recognized cards priority without losing overlapping diagnostic records") {
        for (finding in stored.summary.cards) {
            if (finding.pathways.getValue("MATCH") > 0) finding.browsingState shouldBe "IDENTIFIED"
            else if (finding.pathways.getValue("UNKNOWN") > 0) finding.browsingState shouldBe "UNRESOLVED"
        }
        val acolyte = stored.summary.cards.single { it.card == "Acolyte of Aclazotz" }
        acolyte.browsingState shouldBe "IDENTIFIED"
        (acolyte.unsupportedRecords > 0) shouldBe true
        stored.summary.cards.single { it.card == "Agent of Shauku" }.browsingState shouldBe "UNRESOLVED"
        stored.summary.cards.size shouldBe (report.results.map { it.card } + report.unsupported.map { it.card }).distinct().size
    }
})
