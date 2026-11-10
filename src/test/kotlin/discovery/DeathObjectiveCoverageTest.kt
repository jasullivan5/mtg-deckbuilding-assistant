package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.explorer.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Instant

/** Bounded source comparisons; these are not new translation approvals or gameplay runs. */
class DeathObjectiveCoverageTest : FunSpec({
    val catalog by lazy { Catalog() }
    val report by lazy { discover(catalog, "Judith, the Scourge Diva") }
    val stored by lazy { StoredRun(catalog, report, Instant.EPOCH, 1, "isolated-test") }
    fun rows(name: String) = report.results.filter { it.card == name }
    fun fields(name: String) = stored.card(stored.summary.cards.single { it.card == name }.index)!!.abilities.first().evidence.single { it.id == "mechanic" }.fields
    fun source(slug: String) = reportJson.parseToJsonElement(Files.readString(TestFiles.fixture("cards/$slug-source.json"))).jsonObject

    test("coverage sources bind the exact printing and preserve raw retrieval hashes") {
        val manifest = reportJson.parseToJsonElement(Files.readString(TestFiles.fixture("cards/objective-coverage-sources.json"))).jsonObject
        for (entry in manifest.getValue("sources").jsonArray.map { it.jsonObject }) {
            val bytes = Files.readAllBytes(TestFiles.fixture("cards/${entry.getValue("file").jsonPrimitive.content}"))
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } shouldBe entry.getValue("sha256").jsonPrimitive.content
            val json = reportJson.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject
            json.getValue("id") shouldBe entry.getValue("printingId")
            json.getValue("oracle_id") shouldBe entry.getValue("oracleId")
            json.getValue("name") shouldBe entry.getValue("name")
            val native = catalog.card(entry.getValue("name").jsonPrimitive.content)!!
            native.metadata.imageUri!!.contains(entry.getValue("printingId").jsonPrimitive.content) shouldBe true
        }
    }
    test("Centaur causes a conditional death while Judith responds to the preceding death") {
        rows("Accursed Centaur").single().branch shouldBe Contribution.DIRECT_DEATH_GENERATOR
        rows("Accursed Centaur").single().verdict shouldBe Verdict.MATCH
        val centaur = fields("Accursed Centaur").associate { it.label to it.value }
        centaur.getValue("Mechanic found") shouldBe "Creature sacrifice as an effect"
        centaur.getValue("Why it qualifies").contains("sacrificed and die") shouldBe true
        centaur.getValue("Ability conditions") shouldBe "Source enters the battlefield; sacrifice required"
        centaur.getValue("Death outcome").contains("conditional on reaching the graveyard") shouldBe true
        centaur.getValue("Assumed in play").contains("no replacement") shouldBe true
        val judith = rows("Judith, the Scourge Diva").single { it.verdict == Verdict.MATCH }
        judith.branch shouldBe Contribution.DEATH_ENHANCED_REMOVAL
        val card = stored.card(stored.summary.cards.single { it.card == judith.card }.index)!!
        val why = card.abilities.first { it.verdict == Verdict.MATCH }.evidence.single { it.id == "mechanic" }.fields.single { it.label == "Why it qualifies" }.value
        why.contains("does not cause that first death") shouldBe true
    }
    test("death-triggered draw and treasure now have separately attributed response results") {
        source("grim-haruspex").getValue("oracle_text").jsonPrimitive.content.contains("draw a card") shouldBe true
        source("pitiless-plunderer").getValue("oracle_text").jsonPrimitive.content.contains("Treasure token") shouldBe true
        for (name in listOf("Grim Haruspex", "Pitiless Plunderer")) {
            report.eligibility.single { it.card == name }.deck shouldBe Verdict.MATCH
            report.actions.any { it.card == name && it.path.startsWith("script.triggeredAbilities") } shouldBe true
            rows(name).any { it.branch==Contribution.DEATH_TRIGGERED_OUTCOME && it.verdict==Verdict.MATCH } shouldBe true
        }
        report.actions.filter { it.card == "Grim Haruspex" }.any { "DrawCards" in it.native.toString() } shouldBe true
        report.facts.any { it.card == "Pitiless Plunderer" && it.kind == FactKind.CREATE_TOKEN } shouldBe true
    }
    test("a death-triggered creature token retains token admission and gains a death response") {
        source("doomed-dissenter").getValue("oracle_text").jsonPrimitive.content.contains("dies") shouldBe true
        rows("Doomed Dissenter").any { it.branch==Contribution.DEATH_TRIGGERED_OUTCOME && it.verdict==Verdict.MATCH } shouldBe true
        report.facts.any { it.card == "Doomed Dissenter" && it.kind == FactKind.CREATE_TOKEN } shouldBe true
        val tokens = discover(catalog, report.commander, objective = "create-creature-tokens")
        tokens.results.any { it.card == "Doomed Dissenter" && it.verdict == Verdict.MATCH } shouldBe true
    }
    test("life changes and self-player damage do not become creature removal merely because death triggers them") {
        source("blood-artist").getValue("oracle_text").jsonPrimitive.content.contains("loses 1 life") shouldBe true
        source("midnight-reaper").getValue("oracle_text").jsonPrimitive.content.contains("draw a card") shouldBe true
        rows("Blood Artist").isEmpty() shouldBe true
        report.unsupported.any { it.card == "Blood Artist" && it.nativeType.endsWith("LoseLifeEffect") } shouldBe true
        val reaper = rows("Midnight Reaper").single { it.branch==Contribution.DIRECT_DEATH_GENERATOR }
        reaper.verdict shouldBe Verdict.NO_MATCH
        reaper.branch shouldBe Contribution.DIRECT_DEATH_GENERATOR
        discoveryFields(listOf(reaper), report.actions.first { it.id==reaper.actionId }).single { it.label == "Why not included" }.value shouldBe "This damage affects a player, not a creature."
    }
    test("Tragic Slip is death-enhanced removal whereas Murder is deliberately policy excluded") {
        source("tragic-slip").getValue("oracle_text").jsonPrimitive.content.contains("-13/-13") shouldBe true
        source("murder").getValue("oracle_text").jsonPrimitive.content shouldBe "Destroy target creature."
        rows("Tragic Slip").single { it.verdict == Verdict.MATCH }.branch shouldBe Contribution.DEATH_ENHANCED_REMOVAL
        val murder = rows("Murder").single()
        murder.mechanicalCompatibility shouldBe Verdict.MATCH
        murder.policyAcceptance shouldBe Verdict.NO_MATCH
        fields("Murder").single { it.label == "Why not included" }.value.contains("choosing your own creature") shouldBe true
    }
    test("unresolved land sacrifice is not relabeled as identified creature sacrifice") {
        val agent = fields("Agent of Shauku").associate { it.label to it.value }
        agent.getValue("Mechanic found") shouldBe "Land sacrifice as a cost"
        agent.getValue("Death relationship").contains("unresolved") shouldBe true
        agent.containsKey("Why it qualifies") shouldBe false
    }
    test("death scope help describes bounded responses and the objective menu includes the independent state search") {
        objectives.first().coverageNote!!.contains("Input supply is bounded") shouldBe true
        objectives.map { it.id } shouldBe listOf("own-nontoken-creature-deaths", "reanimate-own-creatures", "create-creature-tokens", "creature-died-this-turn", "stock-own-graveyard")
    }
})
