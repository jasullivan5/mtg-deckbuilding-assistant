package dev.mtg.assistant

import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.tooling.CardValidator
import dev.mtg.assistant.coverage.*
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.extensions.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path

class TokenCardIntegrationTest : FunSpec({
    val catalog by lazy { Catalog() }
    val additions=listOf(KrenkosCommand,Bitterblossom)
    val root = TestFiles.root
    test("translated definitions retain independent printing identity legality Oracle text and native validation") {
        for((c,file) in listOf(KrenkosCommand to "krenkos-command",Bitterblossom to "bitterblossom")) {
            val source=Json.parseToJsonElement(Files.readString(root.resolve("src/test/resources/cards/$file-source.json"))).jsonObject
            c.name shouldBe source.getValue("name").jsonPrimitive.content
            c.oracleText shouldBe source.getValue("oracle_text").jsonPrimitive.content
            c.oracleId shouldBe source.getValue("oracle_id").jsonPrimitive.content
            c.metadata.scryfallId shouldBe source.getValue("id").jsonPrimitive.content
            c.metadata.collectorNumber shouldBe source.getValue("collector_number").jsonPrimitive.content
            c.metadata.rarity.name.lowercase() shouldBe source.getValue("rarity").jsonPrimitive.content
            c.metadata.artist shouldBe source.getValue("artist").jsonPrimitive.content
            c.setCode!!.lowercase() shouldBe source.getValue("set").jsonPrimitive.content
            c.legalFormats shouldBe DeckFormat.entries.filter { source.getValue("legalities").jsonObject[it.scryfallKey]?.jsonPrimitive?.content=="legal" }.toSet()
            fullIdentity(c).map { it.symbol.toString() }.toSet() shouldBe source.getValue("color_identity").jsonArray.map { it.jsonPrimitive.content }.toSet()
            c.cardFaces.isEmpty() shouldBe true;c.backFace shouldBe null;c.flipSide shouldBe null
            CardValidator.validate(c).isEmpty() shouldBe true
            catalog.provenance(c.name).origin shouldBe "APPLICATION_EXTENSION"
            catalog.definitionReview(c.name).scenarioReferences.isNotEmpty() shouldBe true
        }
        val rulings=Json.parseToJsonElement(Files.readString(root.resolve("src/test/resources/cards/bitterblossom-rulings.json"))).jsonObject.getValue("data").jsonArray
        Bitterblossom.metadata.rulings.map { it.date to it.text } shouldBe rulings.map { it.jsonObject.getValue("published_at").jsonPrimitive.content to it.jsonObject.getValue("comment").jsonPrimitive.content }
    }
    test("new definitions are reused for multiple commanders with unchanged objective predicates") {
        for((name,commanders) in listOf(KrenkosCommand.name to listOf("Judith, the Scourge Diva","Krenko, Mob Boss"),
            Bitterblossom.name to listOf("Judith, the Scourge Diva","Marrow-Gnawer"))) {
            for(who in commanders) {
                val reports=coverageObjectives.map { discover(catalog,who,"!\"$name\"",it) }
                reports.all { it.mechanicalInputCards==listOf(name) } shouldBe true
                reports.map { it.facts }.distinct().size shouldBe 1
                reports.last().results.count { it.verdict==Verdict.MATCH } shouldBe 1
                reports.take(2).all { it.results.none { r -> r.verdict==Verdict.MATCH } } shouldBe true
            }
        }
        val excluded=discover(catalog,"Oviya Pashiri, Sage Lifecrafter","name:Bitterblossom OR name:Krenko",objective="create-creature-tokens")
        excluded.mechanicalInputCards shouldBe emptyList()
    }
    test("Bitterblossom preserves upkeep timing and has no fabricated life payment or creature source") {
        val extracted=PathwayExtractor().extract(listOf(Bitterblossom))
        val token=tokenCreationFacts(extracted.facts).execute().single()
        token.verdict shouldBe Verdict.MATCH
        token.fact.circumstances!!.actionKind shouldBe "TRIGGERED"
        (token.fact.prerequisites+token.fact.circumstances!!.timingAndConditions).any { it.contains("UPKEEP") } shouldBe true
        extracted.facts.none { it.costOrEffect in setOf("CASTING_COST","ACTIVATION_COST") } shouldBe true
        Bitterblossom.typeLine.isCreature shouldBe false
    }
    test("new verified identities become authoritative coverage matches and future upstream duplicates fail") {
        for(c in additions) {
            val native=nativeIdentity(c,"APPLICATION_EXTENSION")
            val source=OracleIdentity(c.oracleId,c.name,"normal",fullIdentity(c).map { it.symbol.toString() },
                c.typeLine.toString(),c.oracleText,c.manaCost.toString(),emptyList(),mapOf("commander" to "legal"),emptyList(),emptyList(),
                OracleSource(c.metadata.scryfallId,null,null,c.setCode,c.metadata.collectorNumber,c.metadata.releaseDate))
            NativeIndex(listOf(native)).match(source).status shouldBe CatalogStatus.NATIVE_AVAILABLE
            val extension=Extensions.all.single { it.definition.name==c.name }
            shouldThrow<IllegalArgumentException> { Extensions.validate(listOf(c.copy(name="Future Upstream Name")),listOf(extension)) }
        }
    }
    test("preexisting controlled input facts results actions and uncertainty remain byte equivalent") {
        val previous=Catalog(Extensions.all.filterNot { e -> additions.any { it.name==e.definition.name } })
        for(who in listOf("Judith, the Scourge Diva","Krenko, Mob Boss")) for(objective in coverageObjectives) {
            val before=discover(previous,who,"name:Kuldotha",objective)
            val after=discover(catalog,who,"name:Kuldotha",objective)
            after.facts shouldBe before.facts;after.actions shouldBe before.actions;after.results shouldBe before.results
            after.unsupported shouldBe before.unsupported;after.structuralSupport shouldBe before.structuralSupport
            after.eligibility.filterNot { e -> additions.any { it.name==e.card } } shouldBe before.eligibility
        }
    }
})
