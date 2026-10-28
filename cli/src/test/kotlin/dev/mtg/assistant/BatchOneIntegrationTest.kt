package dev.mtg.assistant

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.tooling.CardValidator
import com.wingedsheep.sdk.tooling.LintSeverity
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.extensions.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path

class BatchOneIntegrationTest : FunSpec({
    val cards=listOf(ExperimentTwelve,PlanewideCelebration,MortalWound,Tatterkite,TyriteSanctum,LeyWeaver)
    val names=cards.map { it.name }.toSet()
    val baseline by lazy { Catalog(Extensions.all.filterNot { it.definition.name in names }) }
    val catalog by lazy { Catalog() }
    val root=if(Files.exists(Path.of("reference"))) Path.of(".") else Path.of("..")
    test("every completed physical card was missing and retains verified independent source fields") {
        cards.forEach { c ->
            baseline.card(c.name) shouldBe null
            val source=Json.parseToJsonElement(Files.readString(root.resolve("reference/alignment-batch1/sources/${c.oracleId}.json"))).jsonObject
            c.oracleId shouldBe source.getValue("oracle_id").jsonPrimitive.content
            c.name shouldBe source.getValue("name").jsonPrimitive.content
            c.oracleText shouldBe source.getValue("oracle_text").jsonPrimitive.content
            c.typeLine.toString() shouldBe source.getValue("type_line").jsonPrimitive.content
            c.manaCost.toString() shouldBe source.getValue("mana_cost").jsonPrimitive.content
            c.metadata.scryfallId shouldBe source.getValue("id").jsonPrimitive.content
            c.setCode shouldBe source.getValue("set").jsonPrimitive.content.uppercase()
            c.metadata.collectorNumber shouldBe source.getValue("collector_number").jsonPrimitive.content
            c.colorIdentityOverride!!.map { it.symbol.toString() }.sorted() shouldBe source.getValue("color_identity").jsonArray.map { it.jsonPrimitive.content }.sorted()
            (DeckFormat.COMMANDER in c.legalFormats) shouldBe true
            source.getValue("legalities").jsonObject.getValue("commander").jsonPrimitive.content shouldBe "legal"
            CardValidator.validate(c).filter { it.severity==LintSeverity.ERROR } shouldBe emptyList()
            catalog.card(c.name) shouldBe c
        }
    }
    test("token mode is interpreted without pretending graveyard to hand is reanimation") {
        val token=discover(catalog,"Oviya Pashiri, Sage Lifecrafter","name:Planewide", "create-creature-tokens")
        token.facts.any { it.card==PlanewideCelebration.name && it.kind==FactKind.CREATE_TOKEN } shouldBe true
        val reanimate=discover(catalog,"Oviya Pashiri, Sage Lifecrafter","name:Planewide", "reanimate-own-creatures")
        reanimate.facts.any { it.card==PlanewideCelebration.name && it.destinationZone==Zone.BATTLEFIELD && it.kind==FactKind.MOVE_OBJECT } shouldBe false
    }
    test("retained controlled discovery evidence is unchanged across all supported contexts") {
        for(who in listOf("Judith, the Scourge Diva","Krenko, Mob Boss","Oviya Pashiri, Sage Lifecrafter"))
            for(objective in listOf("own-nontoken-creature-deaths","reanimate-own-creatures","create-creature-tokens")) {
                val before=discover(baseline,who,"name:Kuldotha",objective)
                val after=discover(catalog,who,"name:Kuldotha",objective)
                after.facts shouldBe before.facts;after.actions shouldBe before.actions;after.results shouldBe before.results
                after.unsupported shouldBe before.unsupported;after.structuralSupport shouldBe before.structuralSupport
                after.eligibility.filterNot { it.card in names } shouldBe before.eligibility
            }
    }
})
