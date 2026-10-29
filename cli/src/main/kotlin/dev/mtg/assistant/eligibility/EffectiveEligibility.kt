package dev.mtg.assistant.eligibility

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition
import dev.mtg.assistant.coverage.*
import dev.mtg.assistant.discovery.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.security.MessageDigest

@Serializable enum class CorrectionField { FULL_COLOR_IDENTITY, COMMANDER_LEGALITY }
@Serializable enum class ReviewDisposition { APPROVED, PROPOSED }
@Serializable data class EligibilityCorrection(
    val id: String, val disposition: ReviewDisposition, val oracleId: String, val native: NativeIdentity,
    val definitionSha256: String, val field: CorrectionField, val expected: List<String>?, val approved: List<String>,
    val sourceUri: String, val verifiedAt: String, val reviewEvidence: List<String>
)
@Serializable data class CorrectionPlan(
    val schemaVersion: Int = 1, val context: EnrichmentContext, val snapshot: SnapshotManifest,
    val sources: List<OracleIdentity>, val currentSources: List<OracleIdentity>, val corrections: List<EligibilityCorrection>
)
@Serializable data class CorrectionDecision(val record: EligibilityCorrection, val state: String)
@Serializable data class EffectiveEligibility(
    val nativeName: String, val oracleId: String?, val originalIdentity: List<String>?,
    val originalCommanderLegality: Verdict, val identity: List<String>?, val commanderLegality: Verdict,
    val corrections: List<CorrectionDecision> = emptyList()
) {
    val authoritativeIdentity: Boolean get() = identity != null
    val colors: Set<Color> get() = identity.orEmpty().map { symbol -> Color.entries.single { it.symbol.toString() == symbol } }.toSet()
}

fun nativeEligibility(card: CardDefinition) = EffectiveEligibility(card.name,card.oracleId,
    if(card.colorIdentityOverride==null) null else fullIdentity(card).map { it.symbol.toString() }.sorted(),
    commanderFormatLegality(card),if(card.colorIdentityOverride==null) null else fullIdentity(card).map { it.symbol.toString() }.sorted(),
    commanderFormatLegality(card))

/** Root eligibility fields may change independently; all other native content remains guarded. */
fun eligibilityDefinitionDigest(card: CardDefinition): String {
    fun ordered(e: JsonElement): JsonElement = when(e) {
        is JsonObject -> JsonObject(e.filterKeys { it !in setOf("colorIdentityOverride","legalFormats") }.toSortedMap().mapValues { ordered(it.value) })
        is JsonArray -> JsonArray(e.map(::ordered))
        else -> e
    }
    val value=ordered(nativeJson.encodeToJsonElement(CardDefinition.serializer(),card))
    return MessageDigest.getInstance("SHA-256").digest(value.toString().toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

/** Reviewed data ships with the CLI. No runtime network, registry mutation or native card copies. */
object ApprovedEligibility {
    val plan: CorrectionPlan by lazy {
        val stream=requireNotNull(javaClass.getResourceAsStream("/eligibility/phase29.json")) { "Missing reviewed eligibility resource" }
        stream.bufferedReader(Charsets.UTF_8).use { snapshotJson.decodeFromString<CorrectionPlan>(it.readText()) }
    }
}

/** Validate the complete plan before publishing any effective values. Invalid approved data fails clearly. */
class EligibilityResolver(cards: List<CardDefinition>, val plan: CorrectionPlan?, revision: String = ARGENTUM_SHA) {
    private val targets=cards.groupBy { it.name }
    private val effective: Map<String,EffectiveEligibility>
    val validationMillis: Long
    init {
        val start=System.nanoTime()
        val output=mutableMapOf<String,EffectiveEligibility>()
        if(plan!=null) {
            require(plan.schemaVersion==1 && plan.context.algorithm==IDENTITY_ALGORITHM) { "Unsupported correction plan/identity algorithm" }
            // Revision is provenance. Reusable source metadata is guarded below by
            // physical identity, native definition, field values and both snapshots.
            // An unrelated engine correction must not turn reviewed legality into a pin gate.
            require(plan.context.argentumCommit.matches(Regex("[0-9a-f]{40}")) &&
                revision.matches(Regex("[0-9a-f]{40}")) &&
                plan.context.snapshotSha256==plan.snapshot.sha256) { "Correction source snapshot or revision provenance invalid; review required" }
            require(plan.snapshot.updatedAt.isNotBlank() && plan.snapshot.sourceUri.startsWith("https://api.scryfall.com/bulk-data")) { "Missing correction source provenance" }
            require(plan.sources.map { it.oracleId }.distinct().size==plan.sources.size && plan.currentSources.map { it.oracleId }.distinct().size==plan.currentSources.size) { "Duplicate correction source identities" }
            require(plan.corrections.map { it.id }.distinct().size==plan.corrections.size &&
                plan.corrections.map { it.native.name to it.field }.distinct().size==plan.corrections.size) { "Duplicate or conflicting eligibility corrections" }
            val projections=cards.map { c -> nativeIdentity(c,plan.corrections.firstOrNull { it.native.name==c.name }?.native?.origin ?: "UPSTREAM_ARGENTUM") }
            // The source subset contains reviewed complete physical records from the bound full snapshot.
            // Reuse the frozen Phase 2.8 matcher globally over native targets, including duplicate claims.
            val snapshot=OracleSnapshot(plan.snapshot,plan.sources,plan.sources.size,0,0)
            val associations=OracleEnrichment(projections,snapshot).assignments.groupBy { it.native.name }
            for(record in plan.corrections) {
                require(record.id.isNotBlank() && oracleId(record.oracleId)!=null && record.sourceUri.startsWith("https://scryfall.com/card/") &&
                    runCatching { java.time.OffsetDateTime.parse(record.verifiedAt) }.isSuccess && record.reviewEvidence.isNotEmpty()) { "Invalid correction provenance: ${record.id}" }
                if(record.disposition==ReviewDisposition.PROPOSED) continue
                val card=cards.singleOrNull { it.name==record.native.name }
                    ?: throw IllegalArgumentException("Correction target missing or ambiguous: ${record.id}")
                val association=associations[card.name]?.singleOrNull()
                require(association?.confidence==IdentityConfidence.CONFIDENT && association.oracleId==record.oracleId) { "Correction identity is not confident or disagrees: ${record.id}" }
                val original=record.native
                val currentNative=association.native
                val allowedOtherField=plan.corrections.filter { it.native.name==card.name && it.field!=record.field && it.disposition==ReviewDisposition.APPROVED }
                require(if(record.field==CorrectionField.FULL_COLOR_IDENTITY)
                    currentNative.commanderLegality==original.commanderLegality || allowedOtherField.any { it.approved==listOf(currentNative.commanderLegality) }
                else currentNative.identity==original.identity || allowedOtherField.any { it.approved==currentNative.identity?.sorted() }) {
                    "Unreviewed change to another native eligibility field: ${record.id}"
                }
                // Permit only eligibility metadata to evolve; identity/printing evidence remains source bound.
                require(association.native.copy(identity=original.identity,commanderLegality=original.commanderLegality,derivedIdentity=original.derivedIdentity)==original &&
                    eligibilityDefinitionDigest(card)==record.definitionSha256) { "Native definition/identity changed; review correction ${record.id}" }
                val source=plan.sources.singleOrNull { it.oracleId==record.oracleId }
                    ?: throw IllegalArgumentException("Missing archived correction source")
                val current=plan.currentSources.singleOrNull { it.oracleId==record.oracleId }
                    ?: throw IllegalArgumentException("Missing independent current correction source")
                require(source.issues.isEmpty() && current.issues.isEmpty() && source.colorIdentity==current.colorIdentity &&
                    source.legalities["commander"]==current.legalities["commander"] && current.source.uri==record.sourceUri) { "Unresolved or disagreeing correction sources: ${record.id}" }
                val base=output[card.name] ?: nativeEligibility(card).copy(oracleId=record.oracleId)
                val nativeValue=when(record.field) {
                    CorrectionField.FULL_COLOR_IDENTITY -> nativeEligibility(card).originalIdentity
                    CorrectionField.COMMANDER_LEGALITY -> listOf(nativeEligibility(card).originalCommanderLegality.name)
                }
                val expectedSource=when(record.field) {
                    CorrectionField.FULL_COLOR_IDENTITY -> {
                        require(record.approved.distinct().size==record.approved.size && record.approved.all { it in setOf("W","U","B","R","G") }) { "Invalid corrected color identity" }
                        require(record.approved.containsAll((identityRestrictions(card)+card.manaCost.colors+card.colorIndicator.orEmpty()).map { it.symbol.toString() })) { "Correction would erase native face/land restrictions" }
                        source.colorIdentity
                    }
                    CorrectionField.COMMANDER_LEGALITY -> when(source.legalities["commander"]) {
                        "legal" -> listOf(Verdict.MATCH.name)
                        "banned","not_legal" -> listOf(Verdict.NO_MATCH.name)
                        else -> null
                    }
                }
                require(expectedSource!=null && record.approved==expectedSource.sorted()) { "Corrected value lacks reviewed source support: ${record.id}" }
                require(nativeValue==record.expected || nativeValue==record.approved) { "Stale native value; review correction ${record.id}" }
                val decision=CorrectionDecision(record,if(nativeValue==record.approved) "UPSTREAM_ALREADY_CORRECT" else "APPLIED")
                output[card.name]=when(record.field) {
                    CorrectionField.FULL_COLOR_IDENTITY -> base.copy(identity=record.approved,corrections=base.corrections+decision)
                    CorrectionField.COMMANDER_LEGALITY -> base.copy(commanderLegality=Verdict.valueOf(record.approved.single()),corrections=base.corrections+decision)
                }
            }
        }
        effective=output
        validationMillis=(System.nanoTime()-start)/1_000_000
    }
    fun resolve(card: CardDefinition): EffectiveEligibility = effective[card.name]?.also {
        require(targets[card.name]?.singleOrNull()==card) { "Effective metadata requested for a changed native definition" }
    } ?: nativeEligibility(card)
    fun requireSource(snapshotSha256: String, nativeNames: Collection<String>) {
        require(plan==null || plan.context.snapshotSha256==snapshotSha256 || nativeNames.none { it in effective }) {
            "Coverage source differs from approved correction snapshot; review corrections before using another source version"
        }
    }
    fun reviewed(): List<EffectiveEligibility> = effective.values.sortedBy { it.nativeName }
}
