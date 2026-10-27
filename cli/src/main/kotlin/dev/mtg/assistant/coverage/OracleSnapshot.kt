package dev.mtg.assistant.coverage

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.util.Locale
import java.util.UUID
import java.util.zip.GZIPInputStream

internal val snapshotJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
internal fun JsonObject.text(key: String) = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
internal fun JsonObject.strings(key: String): List<String>? = (this[key] as? JsonArray)?.let { array ->
    if (array.any { it !is JsonPrimitive || !it.isString }) null else array.map { it.jsonPrimitive.content }
}
internal fun oracleId(value: String?): String? = value?.lowercase(Locale.ROOT)?.let {
    if (runCatching { UUID.fromString(it).toString() == it }.getOrDefault(false)) it else null
}

@Serializable data class SnapshotManifest(
    val schemaVersion: Int = 1, val bulkType: String, val bulkId: String,
    val updatedAt: String, val downloadedAt: String, val sourceUri: String, val downloadUri: String,
    val format: String, val compression: String, val sha256: String, val bytes: Long
)
@Serializable data class OracleFace(val name: String?, val typeLine: String?, val oracleText: String?,
    val manaCost: String?, val colorIndicator: List<String>?, val oracleId: String?)
@Serializable data class OracleSource(val scryfallId: String?, val uri: String?, val rulingsUri: String?,
    val set: String?, val collectorNumber: String?, val releasedAt: String?)
@Serializable data class OracleIdentity(
    val oracleId: String?, val name: String?, val layout: String?, val colorIdentity: List<String>?,
    val typeLine: String?, val oracleText: String?, val manaCost: String?,
    val faces: List<OracleFace>, val legalities: Map<String,String>, val keywords: List<String>,
    val relatedParts: List<String>, val source: OracleSource, val issues: List<String> = emptyList(),
    val mappingNotes: List<String> = emptyList()
) {
    val aliases: Set<String> get() = (listOfNotNull(name) + faces.mapNotNull { it.name }).map { it.lowercase(Locale.ROOT) }.toSet()
    /** Printing metadata may differ; Oracle content and physical layout may not. */
    internal fun identityContent() = copy(source = OracleSource(null,null,null,null,null,null))
}
data class OracleSnapshot(val manifest: SnapshotManifest, val cards: List<OracleIdentity>, val rawRecords: Int,
    val duplicateRecords: Int, val loadMillis: Long)

private fun readIdentity(o: JsonObject): OracleIdentity {
    val issues = mutableListOf<String>()
    val id = oracleId(o.text("oracle_id"))
    if (id == null) issues += "MISSING_OR_INVALID_ORACLE_ID"
    val name = o.text("name"); if (name.isNullOrBlank()) issues += "MISSING_NAME"
    val identity = o.strings("color_identity")
    if (identity == null || identity.any { it !in setOf("W","U","B","R","G") } || identity.distinct().size != identity.size)
        issues += "MISSING_OR_INVALID_FULL_COLOR_IDENTITY"
    val layout = o.text("layout"); if (layout.isNullOrBlank()) issues += "MISSING_LAYOUT"
    val faces = (o["card_faces"] as? JsonArray).orEmpty().map { face ->
        val f = face.jsonObject
        OracleFace(f.text("name"),f.text("type_line"),f.text("oracle_text"),f.text("mana_cost"),f.strings("color_indicator"),oracleId(f.text("oracle_id")))
    }
    if (faces.any { it.name.isNullOrBlank() || it.typeLine.isNullOrBlank() || it.oracleText == null }) issues += "INCOMPLETE_FACE_METADATA"
    if (layout in setOf("transform","modal_dfc","split","adventure","flip","reversible_card") && faces.size < 2)
        issues += "MISSING_PHYSICAL_FACES"
    if (faces.isEmpty() && (o.text("type_line").isNullOrBlank() || o.text("oracle_text") == null)) issues += "INCOMPLETE_ORACLE_CONTENT"
    val legality = (o["legalities"] as? JsonObject).orEmpty().mapNotNull { (k,v) ->
        (v as? JsonPrimitive)?.takeIf { it.isString }?.content?.let { k to it }
    }.toMap()
    if (legality["commander"] !in setOf("legal","not_legal","banned","restricted")) issues += "MISSING_OR_UNKNOWN_COMMANDER_LEGALITY"
    return OracleIdentity(id,name,layout,identity?.sorted(),o.text("type_line"),o.text("oracle_text"),o.text("mana_cost"),
        faces,legality,o.strings("keywords").orEmpty(),(o["all_parts"] as? JsonArray).orEmpty().map { it.toString() },
        OracleSource(o.text("id"),o.text("scryfall_uri"),o.text("rulings_uri"),o.text("set"),o.text("collector_number"),o.text("released_at")),issues)
}

fun sha256(path: Path): String {
    val digest = MessageDigest.getInstance("SHA-256")
    Files.newInputStream(path).use { input ->
        val buffer = ByteArray(1024*1024)
        while (true) { val n=input.read(buffer); if(n<0) break; digest.update(buffer,0,n) }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

/** Offline, integrity-checked input. Missing manifests and corrupt files fail explicitly. */
@OptIn(ExperimentalSerializationApi::class)
fun loadOracleSnapshot(path: Path, manifestPath: Path = Path.of("$path.manifest.json")): OracleSnapshot {
    val started = System.nanoTime()
    require(Files.isRegularFile(path) && Files.isRegularFile(manifestPath)) { "Snapshot and adjacent .manifest.json are required; use scripts/oracle-snapshot.py download" }
    val manifest = snapshotJson.decodeFromString<SnapshotManifest>(Files.readString(manifestPath))
    require(manifest.schemaVersion == 1 && manifest.bulkType == "oracle_cards") { "Unsupported snapshot manifest" }
    require(runCatching { OffsetDateTime.parse(manifest.updatedAt); OffsetDateTime.parse(manifest.downloadedAt) }.isSuccess) { "Snapshot timestamps must include an offset" }
    require(manifest.sourceUri.startsWith("https://api.scryfall.com/bulk-data") && manifest.bulkId.isNotBlank()) { "Manifest must identify the Scryfall Oracle bulk source" }
    require(manifest.format in setOf("jsonl","json-array") && manifest.compression in setOf("none","gzip")) { "Unsupported snapshot encoding" }
    require(Files.size(path) == manifest.bytes && sha256(path) == manifest.sha256.lowercase(Locale.ROOT)) { "Snapshot size/SHA256 does not match manifest" }
    val parsed = mutableListOf<OracleIdentity>()
    fun stream(): InputStream = Files.newInputStream(path).let { if(manifest.compression=="gzip") GZIPInputStream(it) else it }
    stream().use { input ->
        if (manifest.format=="jsonl") input.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.filter { it.isNotBlank() }.forEach { parsed += readIdentity(snapshotJson.parseToJsonElement(it).jsonObject) }
        } else snapshotJson.decodeToSequence<JsonObject>(input, DecodeSequenceMode.ARRAY_WRAPPED).forEach { parsed += readIdentity(it) }
    }
    require(parsed.isNotEmpty()) { "Empty Oracle snapshot" }
    val noIds = parsed.filter { it.oracleId==null }
    val grouped = parsed.filter { it.oracleId!=null }.groupBy { it.oracleId }
    val distinctCards = grouped.values.map { group ->
        val first = group.sortedBy { it.source.scryfallId }.first()
        if(group.map { it.identityContent() }.distinct().size>1)
            first.copy(issues=(first.issues+"CONFLICTING_RECORDS_FOR_ORACLE_ID").distinct()) else first
    } + noIds
    val aliasOwners = distinctCards.filter { it.legalities["commander"]=="legal" && it.oracleId!=null }
        .flatMap { c -> c.aliases.map { it to c.oracleId } }.groupBy({it.first},{it.second})
    val collisions = aliasOwners.filterValues { it.distinct().size>1 }.keys
    val cards = distinctCards.map { c ->
        if(c.legalities["commander"]=="legal" && c.aliases.any { it in collisions })
            c.copy(mappingNotes=(c.mappingNotes+"EXTERNAL_NAME_OR_FACE_COLLISION").distinct()) else c
    }
    return OracleSnapshot(manifest,cards.sortedWith(compareBy({it.name ?: ""},{it.oracleId ?: ""})),parsed.size,
        parsed.size-cards.size,(System.nanoTime()-started)/1_000_000)
}

@Serializable enum class PoolState { ELIGIBLE, EXCLUDED, UNRESOLVED }
@Serializable data class PoolDecision(val state: PoolState, val reasons: List<String>, val constructionNotes: List<String> = emptyList())

/** Ordinary inclusion only; Scryfall's full identity is authoritative, never mana-cost color search. */
fun externalEligibility(card: OracleIdentity, allowed: Set<String>): PoolDecision {
    val notes = buildList {
        if(card.keywords.any { it.equals("Companion",true) }) add("COMPANION_ROLE_NOT_EVALUATED; ordinary inclusion only")
        if(card.keywords.any { it.lowercase(Locale.ROOT) in setOf("partner","choose a background","friends forever","doctor's companion") }) add("PAIRING_NOT_EVALUATED; selected commander remains unchanged")
        if((listOfNotNull(card.oracleText)+card.faces.mapNotNull { it.oracleText }).any { it.contains("before the game begins",true) && it.contains("choose a color",true) }) add("PRE_GAME_CHOSEN_COLOR_COMMANDER_ROLE_NOT_EVALUATED; ordinary fixed-identity inclusion only")
        if((listOfNotNull(card.oracleText)+card.faces.mapNotNull { it.oracleText }).any { it.contains("A deck can have",true) }) add("DECK_QUANTITY_EXCEPTION_NOT_EVALUATED; singleton/deck validation outside scope")
    }
    if(card.legalities["commander"] in setOf("not_legal","banned")) return PoolDecision(PoolState.EXCLUDED,listOf("NOT_COMMANDER_LEGAL"),notes)
    if(card.issues.isNotEmpty()) return PoolDecision(PoolState.UNRESOLVED,card.issues,notes)
    if(card.legalities["commander"]!="legal") return PoolDecision(PoolState.UNRESOLVED,listOf("UNSUPPORTED_LEGALITY"),notes)
    if(!allowed.containsAll(card.colorIdentity.orEmpty())) return PoolDecision(PoolState.EXCLUDED,listOf("COLOR_IDENTITY_OUTSIDE_COMMANDER"),notes)
    // CR 903.5d: basic land types impose a restriction independently of color identity.
    val basic = mapOf("Plains" to "W","Island" to "U","Swamp" to "B","Mountain" to "R","Forest" to "G")
    val types = (listOfNotNull(card.typeLine)+card.faces.mapNotNull { it.typeLine }).flatMap { it.split(' ','—') }.toSet()
    if(basic.any { (type,color) -> type in types && color !in allowed }) return PoolDecision(PoolState.EXCLUDED,listOf("BASIC_LAND_TYPE_RESTRICTION_903_5D"),notes)
    return PoolDecision(PoolState.ELIGIBLE,emptyList(),notes)
}
