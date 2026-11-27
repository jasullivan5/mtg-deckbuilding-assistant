package dev.mtg.assistant

import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.GrantActivatedAbilityEffect
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

class TemporaryGrantTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun extract(name: String) = PathwayExtractor().extract(listOf(checkNotNull(catalog.card(name))))

    test("source-frozen cards bind their native instruction, source hash and nested body") {
        val root = Path.of(System.getProperty("assistant.projectRoot"))
        val cases = Json.parseToJsonElement(Files.readString(root.resolve(
            "src/test/resources/regression/temporary-grant-sources.json"))).jsonObject.getValue("cases").jsonArray
        cases.map { it.jsonObject.getValue("printingId") }.distinct().size shouldBe 5
        for (row in cases.map { it.jsonObject }) {
            val native = root.parent.resolve("argentum-engine").resolve(row.getValue("nativePath").jsonPrimitive.content)
            val bytes = Files.readString(native).replace("\r\n", "\n").toByteArray()
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } shouldBe
                row.getValue("nativeLfSha256").jsonPrimitive.content
            val extracted = extract(row.getValue("name").jsonPrimitive.content)
            val parent = extracted.actions.single { it.temporaryGrants.isNotEmpty() }
            val grant = parent.temporaryGrants.single()
            grant.path shouldBe row.getValue("declarationPath").jsonPrimitive.content
            grant.actionId shouldBe parent.id
            grant.body.action.path shouldBe grant.path + ".ability"
            val expected = when (grant.effect) {
                is GrantActivatedAbilityEffect -> "ACTIVATOR"
                is GrantTriggeredAbilityEffect -> "TRIGGER_CONTROLLER"
                else -> error("Unexpected family")
            }
            grant.body.controller shouldBe expected
            extracted.facts.none { it.actionId == grant.body.action.id } shouldBe true
            reportJson.decodeFromString(ActionEvidence.serializer(),
                reportJson.encodeToString(ActionEvidence.serializer(), parent)) shouldBe parent
            if (grant.card == "Quicksmith Spy") {
                (grant.duration is Duration.WhileYouControlSource) shouldBe true
                grant.expiryCheckpoint shouldBe null
                grant.support shouldBe Verdict.UNKNOWN
            } else {
                grant.duration shouldBe Duration.EndOfTurn
                grant.expiryCheckpoint shouldBe "NATIVE_TURN_CLEANUP"
                grant.support shouldBe Verdict.MATCH
            }
        }
    }

    test("old action JSON needs no temporary grant field and new modal bodies stay nested") {
        val origin = extract("Glorious Sunrise").actions.single { it.temporaryGrants.isNotEmpty() }
        val grant = origin.temporaryGrants.single()
        grant.mode shouldBe 1
        grant.body.facts.any { it.operation == "MANA_PRODUCTION" } shouldBe true
        val old = reportJson.encodeToJsonElement(ActionEvidence.serializer(), origin.copy(temporaryGrants = emptyList())).jsonObject
        ("temporaryGrants" in old) shouldBe false
        reportJson.decodeFromJsonElement(ActionEvidence.serializer(), old).temporaryGrants shouldBe emptyList()
        val trash = extract("Trash the Town").actions.flatMap { it.temporaryGrants }.single()
        trash.mode shouldBe 2
        trash.body.facts.any { it.operation == "DRAW_CARDS" } shouldBe true
    }
})
