package dev.mtg.assistant.explorer

import com.wingedsheep.sdk.model.CardDefinition
import dev.mtg.assistant.coverage.nativeIdentity
import dev.mtg.assistant.discovery.reportJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Serializable data class ImageFace(val name: String, val url: String, val artist: String?)
@Serializable data class CardImage(
    val index: Int, val card: String, val status: String, val reason: String,
    val printingId: String? = null, val oracleId: String? = null, val set: String? = null,
    val collectorNumber: String? = null, val sourceUrl: String? = null,
    val fetchedAt: String? = null, val metadataSha256: String? = null,
    val algorithm: String = "printing-face-image-v1", val faces: List<ImageFace> = emptyList()
)

private fun uuid(value: String?): String? = value?.lowercase()?.takeIf {
    runCatching { UUID.fromString(it).toString() == it }.getOrDefault(false)
}
private fun sourceImageId(value: String?): String? = runCatching {
    val uri = URI(value ?: return null)
    if (uri.scheme != "https" || uri.host != "cards.scryfall.io") null
    else uuid(uri.path.substringAfterLast('/').substringBefore('.'))
}.getOrNull()
private fun cardTypes(value: String?): Set<String> = value?.substringBefore('—')?.trim()?.lowercase()
    ?.split(Regex("\\s+"))?.map { if (it == "tribal") "kindred" else it }?.toSet().orEmpty()
private fun safeImage(value: String?, printing: String): String? = value?.takeIf {
    runCatching { URI(it).let { u -> u.scheme == "https" && u.host == "cards.scryfall.io" && printing in u.path } }.getOrDefault(false)
}
private fun identifier(card: CardDefinition): JsonObject? {
    if (!card.metadata.scryfallId.isNullOrBlank() && uuid(card.metadata.scryfallId) == null) return null
    val declared = uuid(card.metadata.scryfallId)
    val image = sourceImageId(card.metadata.imageUri)
    if (declared != null && image != null && declared != image) return null
    val id = declared ?: image
    if (id != null) return buildJsonObject { put("id", id) }
    if (!card.setCode.isNullOrBlank() && !card.metadata.collectorNumber.isNullOrBlank())
        return buildJsonObject { put("set", card.setCode!!); put("collector_number", card.metadata.collectorNumber!!) }
    return null // Never fall back to a name lookup.
}

/** Verify physical identity, exact printing and complete ordered faces before exposing any image. */
fun verifiedImage(index: Int, card: CardDefinition, raw: JsonObject, fetchedAt: String): CardImage {
    fun unavailable(reason: String) = CardImage(index, card.name, "UNAVAILABLE", reason)
    val key = identifier(card) ?: return unavailable("No unambiguous printing identifier is available.")
    val printing = uuid(raw.text("id")) ?: return unavailable("The image source supplied no valid printing ID.")
    val oracle = uuid(raw.text("oracle_id")) ?: return unavailable("The image source supplied no valid physical-card ID.")
    if (key.text("id")?.let { it != printing } == true) return unavailable("The printing identifier did not match.")
    if (!card.setCode.isNullOrBlank() && !card.setCode.equals(raw.text("set"), true) ||
        !card.metadata.collectorNumber.isNullOrBlank() && card.metadata.collectorNumber != raw.text("collector_number"))
        return unavailable("The printing set or collector number disagrees with the authored identity.")
    if (!card.oracleId.isNullOrBlank() && uuid(card.oracleId) != oracle)
        return unavailable("The physical-card ID disagrees with the authored identity.")
    val native = nativeIdentity(card, "IMAGE_IDENTITY_CHECK_ONLY")
    val layout = raw.text("layout")
    if (layout !in setOf("normal", "transform", "modal_dfc", "split", "adventure", "flip") || native.layout != layout)
        return unavailable("This physical layout cannot be verified for imagery.")
    if (raw.text("name") != native.physicalName)
        return unavailable("The full physical-card name did not match.")
    val faces = (raw["card_faces"] as? JsonArray)?.map { it.jsonObject } ?: listOf(raw)
    if (faces.size != native.faces.size) return unavailable("The complete set of faces did not match.")
    for ((i, pair) in native.faces.zip(faces).withIndex()) {
        val (a, b) = pair
        if (a.name != b.text("name") || cardTypes(a.typeLine).isEmpty() || cardTypes(a.typeLine) != cardTypes(b.text("type_line")))
            return unavailable("Face ${i + 1} identity or card types did not match.")
        if ((i == 0 || layout in setOf("split", "adventure", "modal_dfc")) && a.manaCost != b.text("mana_cost"))
            return unavailable("Face ${i + 1} mana cost did not match.")
    }
    val images = faces.map { face ->
        val url = safeImage(face["image_uris"].obj().text("normal") ?: raw["image_uris"].obj().text("normal"), printing)
            ?: return unavailable("A verified face image is unavailable.")
        ImageFace(face.text("name")!!, url, face.text("artist") ?: raw.text("artist"))
    }
    val source = raw.text("scryfall_uri")?.takeIf {
        runCatching { URI(it).let { u -> u.scheme == "https" && u.host == "scryfall.com" } }.getOrDefault(false)
    } ?: return unavailable("The printing source link could not be verified.")
    val hash = MessageDigest.getInstance("SHA-256").digest(raw.toString().toByteArray()).joinToString("") { "%02x".format(it) }
    return CardImage(index, card.name, "VERIFIED", "Printing and complete ordered face identities agree; this does not verify card mechanics.",
        printing, oracle, raw.text("set"), raw.text("collector_number"), source, fetchedAt, hash, faces = images)
}

/** Only visible cards are looked up. Collection batches, 600ms spacing, 24h metadata cache, no retries. */
class CardImages(
    private val cache: Path? = null,
    private val fetch: (List<JsonObject>) -> List<JsonObject> = ScryfallImageMetadata()::fetch
) {
    private data class Cached(val at: Instant, val raw: JsonObject)
    private val memory = object : LinkedHashMap<String, Cached>(128, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Cached>) = size > 1000
    }
    private fun key(id: JsonObject) = MessageDigest.getInstance("SHA-256").digest(id.toString().toByteArray()).joinToString("") { "%02x".format(it) }
    private fun matches(id: JsonObject, raw: JsonObject) = if (id.text("id") != null) id.text("id") == raw.text("id")
        else id.text("set").equals(raw.text("set"), true) && id.text("collector_number") == raw.text("collector_number")
    @Synchronized fun resolve(cards: List<Pair<Int, CardDefinition>>): List<CardImage> {
        require(cards.size <= 40)
        val now = Instant.now()
        val identifiers = cards.mapNotNull { identifier(it.second) }.distinct()
        val loaded = identifiers.associateWith { id ->
            val k = key(id)
            val value = memory[k] ?: cache?.resolve("$k.json")?.takeIf(Files::isRegularFile)?.let { path ->
                runCatching {
                    val obj = reportJson.parseToJsonElement(Files.readString(path)).jsonObject
                    Cached(Instant.parse(obj.text("fetchedAt")), obj.getValue("card").jsonObject)
                }.getOrNull()
            }
            value?.takeIf { it.at <= now && Duration.between(it.at, now) < Duration.ofHours(24) && matches(id, it.raw) }
                ?.also { memory[k] = it }
        }.toMutableMap()
        val missing = identifiers.filter { loaded[it] == null }
        var failure = "No metadata was returned for this printing."
        if (missing.isNotEmpty()) {
            runCatching { fetch(missing) }.onSuccess { response ->
                for (id in missing) {
                    val raw = response.filter { matches(id, it) }.singleOrNull() ?: continue
                    val value = Cached(now, raw)
                    memory[key(id)] = value
                    loaded[id] = value
                    cache?.let { directory -> runCatching {
                        Files.createDirectories(directory)
                        Files.writeString(directory.resolve("${key(id)}.json"), buildJsonObject {
                            put("fetchedAt", now.toString()); put("card", raw)
                        }.toString())
                    } }
                }
            }.onFailure { failure = "Image metadata is unavailable. Cached verified images remain usable; try again later." }
        }
        return cards.map { (index, card) ->
            val id = identifier(card)
            val value = id?.let { loaded[it] }
            if (value != null) verifiedImage(index, card, value.raw, value.at.toString())
            else CardImage(index, card.name, "UNAVAILABLE", if (id == null) "No unambiguous printing identifier is available; no name-only image was substituted." else failure)
        }
    }
}

private class ScryfallImageMetadata {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    private var lastRequest = 0L
    private var blockedUntil = Instant.MIN
    @Synchronized fun fetch(ids: List<JsonObject>): List<JsonObject> {
        check(Instant.now() >= blockedUntil) { "Image metadata requests are temporarily paused" }
        val wait = 600 - (System.nanoTime() - lastRequest) / 1_000_000
        if (wait > 0) Thread.sleep(wait)
        val request = HttpRequest.newBuilder(URI("https://api.scryfall.com/cards/collection"))
            .timeout(Duration.ofSeconds(10))
            .header("User-Agent", "MTGDeckbuildingAssistant/0.1 (github.com/jasullivan5/mtg-deckbuilding-assistant)")
            .header("Accept", "application/json").header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(buildJsonObject { put("identifiers", JsonArray(ids)) }.toString())).build()
        lastRequest = System.nanoTime()
        val response = http.send(request, HttpResponse.BodyHandlers.ofInputStream())
        response.body().use { body ->
            if (response.statusCode() != 200) {
                val retry = response.headers().firstValue("Retry-After").orElse("").toLongOrNull() ?: 900L
                blockedUntil = Instant.now().plusSeconds(retry.coerceIn(60, 3600))
                error("Image provider HTTP ${response.statusCode()}; no automatic retries")
            }
            val bytes = body.readNBytes(4_000_001)
            require(bytes.size <= 4_000_000) { "Image metadata response too large" }
            return reportJson.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject.getValue("data").jsonArray.map { it.jsonObject }
        }
    }
}
