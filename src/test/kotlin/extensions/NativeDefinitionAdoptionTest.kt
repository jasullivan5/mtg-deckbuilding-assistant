package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.eligibility.*
import dev.mtg.assistant.extensions.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Files

class NativeDefinitionAdoptionTest : FunSpec({
    val catalog by lazy { Catalog() }
    val observations by lazy {
        Json.parseToJsonElement(Files.readString(TestFiles.root.resolve("src/test/resources/cards/native-adoption-sources.json")))
            .jsonObject.getValue("cards").jsonArray.associateBy { it.jsonObject.getValue("name").jsonPrimitive.content }
    }
    val adopted=listOf("Bitterblossom","Galadhrim Ambush","Hour of Promise","Judith, the Scourge Diva",
        "Krenko's Command","Labyrinth of Skophos","Might of Murasa","Oviya Pashiri, Sage Lifecrafter",
        "Planewide Celebration","Spined Tyrranax","Throne of Makindi","Tyrite Sanctum")
    for(name in adopted+"Sunforger") test("native identity, text and effective metadata agree with dated physical source: $name") {
        val card=catalog.card(name)!!
        val source=observations.getValue(name).jsonObject.getValue("physicalSource").jsonObject
        card.oracleId shouldBe source.getValue("oracle_id").jsonPrimitive.content
        card.metadata.scryfallId shouldBe source.getValue("id").jsonPrimitive.content
        card.setCode!!.lowercase() shouldBe source.getValue("set").jsonPrimitive.content
        card.metadata.collectorNumber shouldBe source.getValue("collector_number").jsonPrimitive.content
        card.metadata.releaseDate shouldBe source.getValue("released_at").jsonPrimitive.content
        card.oracleText shouldBe source.getValue("oracle_text").jsonPrimitive.content
        card.typeLine.toString() shouldBe source.getValue("type_line").jsonPrimitive.content
        card.manaCost.toString() shouldBe source.getValue("mana_cost").jsonPrimitive.content
        val expectedIdentity=source.getValue("color_identity").jsonArray.map { it.jsonPrimitive.content }.sorted()
        catalog.eligibility.resolve(card).identity shouldBe expectedIdentity
        catalog.eligibility.resolve(card).commanderLegality shouldBe Verdict.MATCH
        catalog.cards.count { it.oracleId==card.oracleId } shouldBe 1
        catalog.provenance(name).origin shouldBe "UPSTREAM_ARGENTUM"
        Extensions.all.none { it.definition.name==name || it.definition.oracleId==card.oracleId } shouldBe true
    }
    test("native adoption preserves historical assurance without transferring its definition or execution claims") {
        adopted.forEach { name ->
            val assurance=catalog.translationAssurance(name)!!
            assurance.evidence!!.oracleId shouldBe catalog.card(name)!!.oracleId
            assurance.evidenceCurrent shouldBe false
            assurance.sourceFidelity shouldBe FidelityState.UNREVIEWED
            assurance.engineExecution shouldBe ExecutionState.NOT_TESTED
            assurance.qualification shouldBe QualificationState.PROVISIONAL
        }
        catalog.translationAssurance("Tatterkite")!!.qualification shouldBe QualificationState.BLOCKED
    }
    test("Sunforger uses corrected native identity while its old reviewed correction remains archived") {
        val historical=ApprovedEligibility.historicalPlan.corrections.single { it.oracleId==catalog.card("Sunforger")!!.oracleId }
        historical.native.identity shouldBe emptyList()
        ApprovedEligibility.plan.corrections.none { it.id==historical.id } shouldBe true
        catalog.eligibility.resolve(catalog.card("Sunforger")!!).corrections shouldBe emptyList()
        catalog.search("name:Sunforger id<=r") shouldBe emptyList()
        catalog.search("name:Sunforger id:rw").map { it.name } shouldBe listOf("Sunforger")
    }
})
