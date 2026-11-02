package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.explorer.*
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Instant

/** Bounded source/representation comparisons, not gameplay receipts or independent approval. */
class SacrificeBoundaryTest : FunSpec({
    val catalog by lazy { Catalog() }
    val report by lazy { discover(catalog, "Judith, the Scourge Diva") }
    val stored by lazy { StoredRun(catalog, report, Instant.EPOCH, 1, "boundary-test") }
    fun row(name: String) = report.results.single { it.card == name && it.operation == "SACRIFICE" }
    fun fields(name: String) = stored.card(stored.summary.cards.single { it.card == name }.index)!!.abilities
        .first { it.verdict != Verdict.NO_MATCH }.evidence.single { it.id == "mechanic" }.fields.associate { it.label to it.value }

    test("source snapshots retain exact physical identities hashes and actual native printing bindings") {
        val manifest = reportJson.parseToJsonElement(Files.readString(TestFiles.fixture("cards/sacrifice-boundary-sources.json"))).jsonObject
        for (entry in manifest.getValue("sources").jsonArray.map { it.jsonObject }) {
            val bytes = Files.readAllBytes(TestFiles.fixture("cards/${entry.getValue("file").jsonPrimitive.content}"))
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } shouldBe entry.getValue("sha256").jsonPrimitive.content
            val source = reportJson.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject
            source.getValue("name") shouldBe entry.getValue("name")
            source.getValue("id") shouldBe entry.getValue("printingId")
            source.getValue("oracle_id") shouldBe entry.getValue("oracleId")
            catalog.card(source.getValue("name").jsonPrimitive.content)!!.metadata.imageUri!!.contains(entry.getValue("printingId").jsonPrimitive.content) shouldBe true
        }
    }
    test("artifact land and broad permanent sacrifices preserve their different compatibility decisions") {
        for (name in listOf("Viscera Seer", "Blood Bairn", "Atog", "Akki Scrapchomper", "Claws of Gix", "Mogg Fanatic")) {
            report.eligibility.single { it.card == name }.deck shouldBe Verdict.MATCH
            row(name).mechanicalCompatibility shouldBe Verdict.MATCH
            row(name).policyAcceptance shouldBe Verdict.MATCH
            row(name).verdict shouldBe Verdict.MATCH
        }
        row("Agent of Shauku").mechanicalCompatibility shouldBe Verdict.UNKNOWN
        row("Agent of Shauku").policyAcceptance shouldBe Verdict.MATCH
        row("Agent of Shauku").verdict shouldBe Verdict.UNKNOWN
        fields("Akki Scrapchomper").getValue("Mechanic found") shouldBe "Artifact or land sacrifice as a cost"
        fields("Akki Scrapchomper").getValue("Creature overlap").contains("without a type-changing effect") shouldBe true
        fields("Atog").getValue("Mechanic found") shouldBe "Artifact sacrifice as a cost"
        fields("Claws of Gix").getValue("Mechanic found") shouldBe "Permanent sacrifice as a cost"
        fields("Agent of Shauku").containsKey("Why it qualifies") shouldBe false
    }
    test("mandatory optional other-creature and self costs are retained without inferring a free repeated death") {
        row("Accursed Centaur").circumstances!!.mandatory shouldBe Verdict.MATCH
        row("Viscera Seer").circumstances!!.mandatory shouldBe Verdict.NO_MATCH
        fields("Viscera Seer").getValue("Repeat use").contains("source is available") shouldBe true
        row("Blood Bairn").nativeOperation.jsonObject.getValue("excludeSelf").jsonPrimitive.boolean shouldBe true
        row("Mogg Fanatic").affectedScope.contains("selfCreature=true") shouldBe true
        val centaur = stored.card(stored.summary.cards.single { it.card == "Accursed Centaur" }.index)!!.abilities.single()
        centaur.conditions.any { "required instruction" in it } shouldBe true
        val attrition = stored.card(stored.summary.cards.single { it.card == "Attrition" }.index)!!.abilities.single()
        attrition.objectiveNotes.any { "legal target before activation" in it } shouldBe true
    }
    test("inherent overlap witnesses are distinct from animation and commander eligibility") {
        val artifactCreature = catalog.card("Ornithopter")!!
        artifactCreature.typeLine.isCreature shouldBe true
        report.eligibility.single { it.card == artifactCreature.name }.deck shouldBe Verdict.MATCH
        val landCreature = catalog.card("Dryad Arbor")!!
        landCreature.typeLine.isCreature shouldBe true
        report.eligibility.single { it.card == landCreature.name }.deck shouldBe Verdict.NO_MATCH // Green identity, not Judith-legal.
        ObjectScope(com.wingedsheep.sdk.scripting.GameObjectFilter.Artifact, "YOU").compatible(Requirements(controller = Controller.YOU, token = TokenStatus.NONTOKEN)) shouldBe Verdict.MATCH
        ObjectScope(com.wingedsheep.sdk.scripting.GameObjectFilter.Land, "YOU").compatible(Requirements(controller = Controller.YOU, token = TokenStatus.NONTOKEN)) shouldBe Verdict.UNKNOWN // Unsupported overlap, not impossibility.
    }
    test("renewable self creatures are represented without being silently added as death contributors") {
        val returns = discover(catalog, report.commander, objective = "reanimate-own-creatures")
        for (name in listOf("Reassembling Skeleton", "Bloodghast")) {
            report.eligibility.single { it.card == name }.deck shouldBe Verdict.MATCH
            report.results.none { it.card == name } shouldBe true
            report.facts.any { it.card == name && it.kind == FactKind.MOVE_OBJECT && it.sourceZone == com.wingedsheep.sdk.core.Zone.GRAVEYARD && it.destinationZone == com.wingedsheep.sdk.core.Zone.BATTLEFIELD } shouldBe true
            returns.results.any { it.card == name && it.verdict == Verdict.MATCH } shouldBe true
        }
        report.actions.any { it.card == "Bloodghast" && "ZoneChangeEvent" in it.native.toString() && "IsLand" in it.native.toString() } shouldBe true
        report.actions.any { it.card == "Reassembling Skeleton" && "activateFromZone" in it.native.toString() } shouldBe true
    }
    test("non-battlefield trigger sources respect their declared zone without overriding incompatible movements") {
        fun fixture(active: Zone, from: Zone) = card("Synthetic zone-bound observer") {
            typeLine = "Creature — Spirit"
            power = 1; toughness = 1
            triggeredAbility {
                trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
                triggerZone = active
                effect = Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD, fromZone = from)
            }
        }
        for (zone in listOf(Zone.GRAVEYARD, Zone.EXILE, Zone.HAND)) {
            val fact = PathwayExtractor().extract(listOf(fixture(zone, zone))).facts.single { it.kind == FactKind.MOVE_OBJECT }
            fact.sourceZone shouldBe zone
            fact.confidence shouldBe Verdict.MATCH
            fact.circumstances!!.sourceMustRemainOnBattlefield shouldBe false
        }
        val incompatible = PathwayExtractor().extract(listOf(fixture(Zone.EXILE, Zone.GRAVEYARD))).facts.single { it.kind == FactKind.MOVE_OBJECT }
        incompatible.confidence shouldBe Verdict.NO_MATCH
        reanimationFacts(listOf(incompatible)).execute().single().verdict shouldBe Verdict.NO_MATCH
    }
    test("ordinary bodies and unqueried observers are not rejected as irrelevant strategic resources") {
        report.eligibility.single { it.card == "Walking Corpse" }.deck shouldBe Verdict.MATCH
        catalog.card("Walking Corpse")!!.script.hasBehavior shouldBe false
        report.results.none { it.card == "Walking Corpse" } shouldBe true
        report.results.none { it.card == "Grim Haruspex" } shouldBe true
        report.actions.any { it.card == "Grim Haruspex" && "DrawCards" in it.native.toString() } shouldBe true
        report.results.any { it.card == "Judith, the Scourge Diva" && it.branch == Contribution.DEATH_ENHANCED_REMOVAL && it.verdict == Verdict.MATCH } shouldBe true
    }
})
