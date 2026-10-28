package dev.mtg.assistant

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.*
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.extensions.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.assertions.throwables.shouldThrow
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path

class CommanderIntegrationTest : FunSpec({
    val catalog by lazy { Catalog() }
    val upstreamOnly by lazy { Catalog(emptyList()) }
    fun fixture()=card("Synthetic Commander") { manaCost="{G}";colorIdentity="G";typeLine="Legendary Creature — Human";power=1;toughness=1 }
        .copy(oracleId="11111111-1111-1111-1111-111111111111",legalFormats=setOf(DeckFormat.COMMANDER))
    fun inspected(c: CardDefinition)=inspectCommanderDefinition(c.name,c)
    fun extension(c: CardDefinition)=ExtensionDefinition(c,Provenance("APPLICATION_EXTENSION","synthetic-native-fixture","2026-10-26"))

    test("native upstream commander is selectable without claiming independent mechanical certification") {
        val r=inspectCommander(catalog,"Krenko, Mob Boss")
        r.availability shouldBe DefinitionAvailability.UPSTREAM_NATIVE
        r.selectable shouldBe true
        r.mechanicalReview shouldBe MechanicalReviewStatus.NOT_INDEPENDENTLY_VERIFIED
    }
    test("missing pinned definition is distinct from Magic card nonexistence and retains exact lookup") {
        upstreamOnly.card(OviyaPashiriSageLifecrafter.name) shouldBe null
        val r=inspectCommander(upstreamOnly,OviyaPashiriSageLifecrafter.name)
        r.availability shouldBe DefinitionAvailability.MISSING_FROM_PINNED_CATALOG
        r.blockers.single().code shouldBe CommanderBlocker.MISSING_DEFINITION
        r.blockers.single().reason shouldContain "does not establish"
        inspectCommander(catalog,"oviya pashiri, sage lifecrafter").selectable shouldBe false
    }
    test("application commander exposes verified provenance and bounded scenario record") {
        val r=inspectCommander(catalog,OviyaPashiriSageLifecrafter.name)
        r.availability shouldBe DefinitionAvailability.APPLICATION_EXTENSION
        r.provenance!!.verifiedAt shouldBe "2026-10-26"
        r.mechanicalReview shouldBe MechanicalReviewStatus.ENGINE_SCENARIOS_RECORDED
        r.scenarioReferences.size shouldBe 1
        r.selectable shouldBe true
        fullIdentity(resolveCommander(catalog,r.requestedName)) shouldBe setOf(Color.GREEN)
    }
    test("unresolved legality and authoritative identity remain independent structured blockers") {
        val r=inspected(fixture().copy(legalFormats=emptySet(),colorIdentityOverride=null))
        r.blockers.map { it.code }.toSet() shouldBe setOf(CommanderBlocker.LEGALITY_UNRESOLVED,CommanderBlocker.IDENTITY_UNRESOLVED)
        r.mechanicalReview shouldBe MechanicalReviewStatus.NOT_INDEPENDENTLY_VERIFIED
    }
    test("known non-Commander legality differs from unresolved legality") {
        inspected(fixture().copy(legalFormats=setOf(DeckFormat.MODERN))).blockers.single().code shouldBe CommanderBlocker.NOT_COMMANDER_LEGAL
    }
    test("ordinary ineligible card differs from unsupported commander construction") {
        inspectCommander(catalog,"Murder").blockers.map { it.code } shouldBe listOf(CommanderBlocker.INELIGIBLE_COMMANDER)
        val chosen=fixture().copy(oracleText="Before the game begins, choose a color.")
        inspected(chosen).blockers.single().code shouldBe CommanderBlocker.UNSUPPORTED_CONSTRUCTION
        inspected(fixture().copy(typeLine=TypeLine.parse("Legendary Enchantment — Background"),creatureStats=null)).blockers.single().code shouldBe CommanderBlocker.UNSUPPORTED_CONSTRUCTION
    }
    test("back face aliases are reported as unavailable physical commander selections") {
        inspectCommanderDefinition("Back",fixture().copy(name="Back"),physicalFront=false).blockers.single().code shouldBe CommanderBlocker.NOT_PHYSICAL_FRONT
    }
    test("native structural failures and declared limitations differ from unverified mechanics") {
        inspected(fixture().copy(equipCost=ManaCost.parse("{1}"))).mechanicalReview shouldBe MechanicalReviewStatus.KNOWN_INCOMPLETE
        val r=inspectCommanderDefinition("Synthetic Commander",fixture(),review=DefinitionReview(knownLimitations=listOf("Synthetic unimplemented ability")))
        r.mechanicalReview shouldBe MechanicalReviewStatus.KNOWN_INCOMPLETE
        r.knownLimitations.size shouldBe 1
    }
    test("inspection CLI reports structured selection separately from successful inspection") {
        val output=mutableListOf<String>()
        runCli(arrayOf("commander","inspect",OviyaPashiriSageLifecrafter.name,"--json"),{catalog},output::add) shouldBe 0
        val json=Json.parseToJsonElement(output.single()).jsonObject
        json["selectable"]!!.jsonPrimitive.boolean shouldBe true
        output.clear()
        runCli(arrayOf("commander","inspect","No Such Native Card"),{catalog},output::add) shouldBe 0
        output.single() shouldContain "MISSING_DEFINITION"
        runCli(arrayOf("commander","inspect"),{catalog},{},{}) shouldBe 2
    }
    test("duplicates are rejected within additions and against upstream after future pin updates") {
        val e=extension(fixture())
        shouldThrow<IllegalArgumentException> { Extensions.validate(emptyList(),listOf(e,e)) }
        shouldThrow<IllegalArgumentException> { Extensions.validate(listOf(e.definition),listOf(e)) }
        shouldThrow<IllegalArgumentException> { Extensions.validate(listOf(e.definition.copy(name="Upstream Rename")),listOf(e)) }.message!! shouldContain "Oracle identity collision"
    }
    test("nested upstream and extension face aliases collide case insensitively") {
        val e=extension(fixture())
        val back=fixture().copy(name="Back Alias",oracleId="22222222-2222-2222-2222-222222222222")
        val upstream=fixture().copy(name="Upstream Front",oracleId="33333333-3333-3333-3333-333333333333",backFace=back)
        shouldThrow<IllegalArgumentException> { Extensions.validate(listOf(upstream),listOf(e.copy(definition=e.definition.copy(name="BACK ALIAS")))) }
        shouldThrow<IllegalArgumentException> { Extensions.validate(listOf(back),listOf(e.copy(definition=e.definition.copy(backFace=back)))) }
        val splitFace=CardFace("Back Alias",ManaCost.parse("{G}"),TypeLine.parse("Sorcery"))
        Extensions.validate(listOf(upstream.copy(backFace=null,cardFaces=listOf(splitFace))),listOf(e.copy(definition=back))) shouldBe emptyList()
    }
    test("nested Oracle identities collide even with distinct names") {
        val e=extension(fixture())
        val nested=fixture().copy(name="Nested Upstream")
        shouldThrow<IllegalArgumentException> { Extensions.validate(listOf(fixture().copy(name="Outer",oracleId="44444444-4444-4444-4444-444444444444",backFace=nested)),listOf(e)) }
    }
    test("extension registration refuses incomplete metadata provenance native structures and known omissions") {
        val e=extension(fixture())
        for(c in listOf(e.definition.copy(colorIdentityOverride=null),e.definition.copy(legalFormats=emptySet()),e.definition.copy(oracleId=null),e.definition.copy(equipCost=ManaCost.parse("{1}"))))
            shouldThrow<IllegalArgumentException> { Extensions.validate(emptyList(),listOf(e.copy(definition=c))) }
        shouldThrow<IllegalArgumentException> { Extensions.validate(emptyList(),listOf(e.copy(provenance=e.provenance.copy(verifiedAt=null)))) }
        shouldThrow<IllegalArgumentException> { Extensions.validate(emptyList(),listOf(e.copy(provenance=e.provenance.copy(source="")))) }
        shouldThrow<IllegalArgumentException> { Extensions.validate(emptyList(),listOf(e.copy(review=DefinitionReview(knownLimitations=listOf("Missing ability"))))) }
    }
    test("native Oviya metadata and both activation structures match source snapshot") {
        val c=OviyaPashiriSageLifecrafter
        c.manaCost.toString() shouldBe "{G}"; c.setCode shouldBe "KLD";c.metadata.collectorNumber shouldBe "165"
        c.creatureStats!!.power.description shouldBe "1"; c.creatureStats!!.toughness.description shouldBe "2"
        c.metadata.rulings.size shouldBe 2;c.activatedAbilities.size shouldBe 2
        c.activatedAbilities.all { it.targetRequirements.isEmpty() } shouldBe true
        catalog.card(c.name)!!.script shouldBe c.script
        catalog.search("name:Oviya").any { it.name==c.name } shouldBe true
    }
    test("Oviya metadata and Oracle text match archived independently retrieved source") {
        val root=if(Files.exists(Path.of("reference"))) Path.of(".") else Path.of("..")
        val source=Json.parseToJsonElement(Files.readString(root.resolve("reference/oviya-kld-165-scryfall.json"))).jsonObject
        val c=OviyaPashiriSageLifecrafter
        c.name shouldBe source.getValue("name").jsonPrimitive.content
        c.oracleText shouldBe source.getValue("oracle_text").jsonPrimitive.content
        c.oracleId shouldBe source.getValue("oracle_id").jsonPrimitive.content
        c.metadata.scryfallId shouldBe source.getValue("id").jsonPrimitive.content
        c.metadata.artist shouldBe source.getValue("artist").jsonPrimitive.content
        c.metadata.rarity.name.lowercase() shouldBe source.getValue("rarity").jsonPrimitive.content
        c.legalFormats shouldBe DeckFormat.entries.filter { source.getValue("legalities").jsonObject[it.scryfallKey]?.jsonPrimitive?.content=="legal" }.toSet()
        fullIdentity(c).map { it.symbol.toString() }.toSet() shouldBe source.getValue("color_identity").jsonArray.map { it.jsonPrimitive.content }.toSet()
        val rulings=Json.parseToJsonElement(Files.readString(root.resolve("reference/oviya-rulings-scryfall.json"))).jsonObject.getValue("data").jsonArray
        c.metadata.rulings.map { it.date to it.text } shouldBe rulings.map { it.jsonObject.getValue("published_at").jsonPrimitive.content to it.jsonObject.getValue("comment").jsonPrimitive.content }
    }
    test("mono-green commander filters full identities and all objectives share identical facts") {
        val commander=resolveCommander(catalog,OviyaPashiriSageLifecrafter.name)
        selectedCommanderEligibility(commander,setOf(Color.GREEN)) shouldBe Verdict.MATCH
        selectedCommanderEligibility(JudithTheScourgeDiva,setOf(Color.GREEN)) shouldBe Verdict.NO_MATCH
        selectedCommanderEligibility(commander,setOf(Color.RED)) shouldBe Verdict.NO_MATCH
        selectedCommanderEligibility(catalog.card("Sol Ring")!!,setOf(Color.GREEN)) shouldBe Verdict.MATCH
        val reports=listOf("own-nontoken-creature-deaths","reanimate-own-creatures","create-creature-tokens").map { discover(catalog,commander.name,"name:Oviya",it) }
        reports.map { it.facts }.distinct().size shouldBe 1
        reports.last().results.count { it.verdict==Verdict.MATCH && it.card==commander.name } shouldBe 2
        reports.take(2).all { it.results.none { r -> r.verdict==Verdict.MATCH && r.card==commander.name } } shouldBe true
    }
    test("Judith and Krenko retain complete eligible pools and controlled A B C evidence") {
        val previous=Catalog(Extensions.all.filter { it.definition.name!=OviyaPashiriSageLifecrafter.name })
        for(who in listOf("Judith, the Scourge Diva","Krenko, Mob Boss")) for(objective in listOf("own-nontoken-creature-deaths","reanimate-own-creatures","create-creature-tokens")) {
            val before=discover(previous,who,"name:Kuldotha",objective)
            val after=discover(catalog,who,"name:Kuldotha",objective)
            after.eligibility.filterNot { it.card==OviyaPashiriSageLifecrafter.name } shouldBe before.eligibility
            after.counts["commander_eligible_cards"] shouldBe before.counts["commander_eligible_cards"]
            after.mechanicalInputCards shouldBe before.mechanicalInputCards
            after.facts shouldBe before.facts;after.actions shouldBe before.actions;after.results shouldBe before.results
            after.unsupported shouldBe before.unsupported
            after.counts["authored_physical_cards"] shouldBe before.counts["authored_physical_cards"]!!+1
        }
    }
})
