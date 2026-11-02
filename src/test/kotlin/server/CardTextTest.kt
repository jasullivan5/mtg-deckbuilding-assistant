package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.explorer.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

class CardTextTest : FunSpec({
    val catalog by lazy { Catalog() }
    val stored by lazy { StoredRun(catalog, discover(catalog, "Judith, the Scourge Diva"), Instant.EPOCH, 1, "isolated-test") }
    fun card(name: String) = stored.card(stored.summary.cards.single { it.card == name }.index)!!

    test("Judith marks only her corresponding death trigger and does not repeat it in the connection") {
        val native = catalog.card("Judith, the Scourge Diva")!!
        val lines = commanderRulesLines(native, objectives.first().id)
        lines.map { it.text } shouldBe native.oracleText.lines()
        lines.count { it.relevance == Verdict.MATCH } shouldBe 1
        lines.first().actionId shouldBe null
        lines.last().text.startsWith("Whenever a nontoken creature") shouldBe true
        commanderObjectiveConnection(native, objectives.first().id) shouldBe "The trigger does not depend on what caused the death."
        commanderRulesLines(native, "create-creature-tokens").all { it.actionId == null } shouldBe true
    }
    test("Attrition links its single complete activation while preserving both original decisions") {
        val evidence = card("Attrition")
        evidence.rules.single().actionId shouldBe evidence.abilities.single().actionId
        evidence.rules.single().relevance shouldBe Verdict.MATCH
        evidence.results.map { it.verdict } shouldBe listOf(Verdict.MATCH, Verdict.NO_MATCH)
        evidence.abilities.single().objectiveNotes.any { "legal target before activation" in it } shouldBe true
    }
    test("Centaur adds the nontoken and mandatory requirements without restating its trigger") {
        val evidence = card("Accursed Centaur")
        evidence.rules.single().relevance shouldBe Verdict.MATCH
        val notes = evidence.abilities.single().objectiveNotes
        notes shouldBe listOf("For this objective, sacrifice a nontoken creature you control. The sacrifice is mandatory when the ability resolves.")
        notes.none { "enters the battlefield" in it } shouldBe true
    }
    test("Acolyte explains the choice's relevance and Agent stays unresolved") {
        val acolyte = card("Acolyte of Aclazotz")
        acolyte.abilities.single().objectiveNotes.single().contains("noncreature artifact or token") shouldBe true
        val agent = card("Agent of Shauku")
        agent.rules.single().relevance shouldBe Verdict.UNKNOWN
        agent.abilities.single().unresolvedNotes.single().contains("land would also have to be a nontoken creature") shouldBe true
    }
    test("ambiguous duplicated or changed text never receives an invented precise association") {
        val evidence = card("Attrition")
        val line = evidence.authoredCard!!.text
        rulesLines(evidence.card, "$line\n$line", evidence.abilities).all { it.actionId == null } shouldBe true
        rulesLines(evidence.card, "$line Draw a card.", evidence.abilities).single().actionId shouldBe null
        rulesLines(evidence.card, line, evidence.abilities.map { it.copy(face = "back") }).single().actionId shouldBe null
    }
    test("review fields name the bound Scryfall data without inventing a reviewer date or ability coverage") {
        val assurance = catalog.translationAssurance("Judith, the Scourge Diva")!!
        val fields = evidenceIndicators(emptyList(), assurance, emptyList(), "Commander").single { it.id == "source" }.fields
        fields.single { it.label == "Rules source" }.value shouldBe "Scryfall Oracle text · retained snapshot"
        fields.single { it.label == "Recorded author" }.value shouldBe "AI agent · name not recorded"
        fields.single { it.label == "Comparison reviewer" }.value shouldBe "Not captured separately by the card-review format"
        fields.single { it.label == "Comparison date" }.value shouldBe "Not captured by the card-review format"
        fields.single { it.label == "Abilities checked" }.value shouldBe "The card-review format has no ability-by-ability links"
        val source = assurance.evidence!!.source.single()
        recordedSource(source.copy(sha256 = "0".repeat(64)), assurance.evidence!!.oracleId).value shouldBe "Retained file; provider not identified"
        recordedSource(source, "wrong-identity").value shouldBe "Retained file; provider not identified"
    }
})
