package dev.mtg.assistant

import dev.mtg.assistant.discovery.reportJson
import dev.mtg.assistant.explorer.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Files

class CardImagesTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun source(slug: String) = reportJson.parseToJsonElement(Files.readString(TestFiles.fixture("cards/$slug-source.json"))).jsonObject
    val at = "2026-10-30T00:00:00Z"
    test("an exact printing and physical identity are required, rather than a name-only image match") {
        val card = catalog.card("Agent of Shauku")!!
        val raw = source("agent-of-shauku")
        val image = verifiedImage(0, card, raw, at)
        image.status shouldBe "VERIFIED"
        image.printingId shouldBe "d8316804-6f8b-423e-a2c3-fa476c095544"
        image.oracleId shouldBe "d2e0d739-b441-4173-a8f1-b831a19ea98e"
        image.faces.single().name shouldBe card.name
        for ((field, value) in mapOf("id" to "00000000-0000-0000-0000-000000000001", "name" to "Some other card", "collector_number" to "999", "mana_cost" to "{G}", "type_line" to "Instant")) {
            verifiedImage(0, card, JsonObject(raw + (field to JsonPrimitive(value))), at).status shouldBe "UNAVAILABLE"
        }
        verifiedImage(0, card.copy(oracleId = "00000000-0000-0000-0000-000000000001"), raw, at).status shouldBe "UNAVAILABLE"
        verifiedImage(0, card, JsonObject(raw + ("image_uris" to buildJsonObject { put("normal", "https://untrusted.invalid/image.jpg") })), at).status shouldBe "UNAVAILABLE"
    }
    test("multi-face artwork requires the complete ordered physical faces") {
        val card = catalog.card("Delver of Secrets")!!
        val raw = source("delver-of-secrets")
        val image = verifiedImage(0, card, raw, at)
        image.status shouldBe "VERIFIED"
        image.faces.map { it.name } shouldBe listOf("Delver of Secrets", "Insectile Aberration")
        val faces = raw.getValue("card_faces").jsonArray
        verifiedImage(0, card, JsonObject(raw + ("card_faces" to JsonArray(faces.reversed()))), at).status shouldBe "UNAVAILABLE"
        verifiedImage(0, card, JsonObject(raw + ("card_faces" to JsonArray(faces.take(1)))), at).status shouldBe "UNAVAILABLE"
    }
    test("cached metadata avoids another lookup and still rechecks identity against the current definition") {
        val directory = Files.createTempDirectory("card-image-cache-")
        var calls = 0
        val card = catalog.card("Agent of Shauku")!!
        val service = CardImages(directory) { ids -> calls++; ids.size shouldBe 1; listOf(source("agent-of-shauku")) }
        try {
            service.resolve(listOf(0 to card)).single().status shouldBe "VERIFIED"
            service.resolve(listOf(1 to card)).single().index shouldBe 1
            calls shouldBe 1
            val offline = CardImages(directory) { error("No network should be needed") }
            offline.resolve(listOf(0 to card)).single().status shouldBe "VERIFIED"
            offline.resolve(listOf(0 to card.copy(name = "Conflicting native identity"))).single().status shouldBe "UNAVAILABLE"
        } finally {
            Files.list(directory).use { files -> files.forEach { Files.delete(it) } }
            Files.delete(directory)
        }
    }
    test("an unavailable provider does not invent an image or break research") {
        val service = CardImages { error("offline") }
        service.resolve(listOf(0 to catalog.card("Agent of Shauku")!!)).single().let {
            it.status shouldBe "UNAVAILABLE"
            it.faces shouldBe emptyList()
        }
    }
})
