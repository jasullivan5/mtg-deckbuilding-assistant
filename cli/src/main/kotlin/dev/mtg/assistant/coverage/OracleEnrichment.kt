package dev.mtg.assistant.coverage

import dev.mtg.assistant.discovery.ARGENTUM_SHA
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.security.MessageDigest
import java.util.Locale

const val IDENTITY_ALGORITHM = "physical-identity-v1"
@Serializable enum class IdentityConfidence { CONFIDENT, REVIEW_REQUIRED, AMBIGUOUS, CONFLICT, MISSING }
@Serializable data class EnrichmentContext(val algorithm: String = IDENTITY_ALGORITHM, val argentumCommit: String,
    val snapshotSha256: String, val nativeIdentitySha256: String)
@Serializable data class IdentityAssociation(val native: NativeIdentity, val oracleId: String?, val candidateOracleIds: List<String>,
    val confidence: IdentityConfidence, val method: String, val evidence: List<String>, val metadata: MetadataAgreement? = null)
@Serializable data class MetadataCorrection(val field: String, val nativeValue: List<String>?, val externalValue: List<String>,
    val disposition: String = "REVIEW_ONLY_NOT_APPLIED")
@Serializable data class MetadataAgreement(val nativeOracleId: String?, val nativeIdentity: Set<String>?,
    val derivedNativeIdentity: Set<String>?, val nativeCommanderLegality: String, val externalIdentity: List<String>?,
    val externalCommanderLegality: String?, val issues: List<String>, val candidateCorrections: List<MetadataCorrection>)
@Serializable data class EnrichmentArtifact(val context: EnrichmentContext, val assignments: List<IdentityAssociation>)

private fun key(name: String)=name.lowercase(Locale.ROOT)
private val nonDeckLayouts=setOf("token","double_faced_token","emblem","art_series","vanguard","scheme","planar","phenomenon","front_card")
private val identityIssues=setOf("CONFLICTING_RECORDS_FOR_ORACLE_ID","MISSING_OR_INVALID_ORACLE_ID","MISSING_NAME","MISSING_LAYOUT","MISSING_PHYSICAL_FACES")
private val multiLayouts=setOf("transform","modal_dfc","split","adventure","flip","reversible_card","prototype","prepare")
private fun types(text: String?): Set<String> = text?.substringBefore("—")?.split(Regex("[ /]+"))?.map { it.lowercase(Locale.ROOT) }
    ?.map { if(it=="tribal") "kindred" else it }?.filter { it in setOf("artifact","battle","creature","enchantment","instant","kindred","land","planeswalker","sorcery") }?.toSet().orEmpty()
private fun cost(value: String?)=value?.replace(" ","")?.uppercase(Locale.ROOT)

fun nativeIdentityDigest(cards: List<NativeIdentity>): String = MessageDigest.getInstance("SHA-256")
    .digest(cards.map { snapshotJson.encodeToString(it) }.sorted().joinToString("\n").toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }

/** Exported artifacts are evidence, never unvalidated metadata overlays. Rebuild on any context change. */
fun requireEnrichmentContext(context: EnrichmentContext, cards: List<NativeIdentity>, snapshot: OracleSnapshot, revision: String = ARGENTUM_SHA) {
    require(context==EnrichmentContext(argentumCommit=revision,snapshotSha256=snapshot.manifest.sha256,nativeIdentitySha256=nativeIdentityDigest(cards))) {
        "Enrichment context changed; regenerate and review against the exact snapshot, revision and native identity fields"
    }
}

fun metadataAgreement(native: NativeIdentity, external: OracleIdentity): MetadataAgreement {
    val issues=mutableListOf<String>();val corrections=mutableListOf<MetadataCorrection>()
    if(native.identity==null) issues += "NATIVE_FULL_IDENTITY_MISSING"
    if(external.colorIdentity==null) issues += "EXTERNAL_FULL_IDENTITY_UNRESOLVED"
    else if(native.identity!=null && native.identity!=external.colorIdentity.toSet()) issues += "NATIVE_EXTERNAL_IDENTITY_DISAGREEMENT"
    val externalLegality=external.legalities["commander"]
    if(native.commanderLegality=="UNKNOWN") issues += "NATIVE_LEGALITY_MISSING"
    if(externalLegality !in setOf("legal","not_legal","banned")) issues += "EXTERNAL_COMMANDER_LEGALITY_UNRESOLVED"
    else if(native.commanderLegality!="UNKNOWN" && (native.commanderLegality=="MATCH")!=(externalLegality=="legal")) issues += "NATIVE_EXTERNAL_LEGALITY_DISAGREEMENT"
    if(external.colorIdentity!=null && issues.any { it in setOf("NATIVE_FULL_IDENTITY_MISSING","NATIVE_EXTERNAL_IDENTITY_DISAGREEMENT") })
        corrections += MetadataCorrection("colorIdentityOverride",native.identity?.sorted(),external.colorIdentity)
    if(externalLegality in setOf("legal","not_legal","banned") && issues.any { it in setOf("NATIVE_LEGALITY_MISSING","NATIVE_EXTERNAL_LEGALITY_DISAGREEMENT") })
        corrections += MetadataCorrection("Commander legality",listOf(native.commanderLegality),listOf(externalLegality!!))
    return MetadataAgreement(native.oracleId,native.identity,native.derivedIdentity,native.commanderLegality,external.colorIdentity,
        external.legalities["commander"],issues,corrections)
}

/** Global, one-to-one physical associations. Eligibility and mechanical data are never changed. */
class OracleEnrichment(private val native: List<NativeIdentity>, private val snapshot: OracleSnapshot) {
    val context=EnrichmentContext(argentumCommit=ARGENTUM_SHA,snapshotSha256=snapshot.manifest.sha256,nativeIdentitySha256=nativeIdentityDigest(native))
    private val byId=snapshot.cards.filter { it.oracleId!=null }.associateBy { it.oracleId!! }
    private val deck=snapshot.cards.filter { it.oracleId!=null && it.layout !in nonDeckLayouts }
    private val byPhysicalName=deck.filter { it.name!=null }.groupBy { key(it.name!!) }
    private val bySourceId=deck.filter { it.source.scryfallId!=null }.groupBy { key(it.source.scryfallId!!) }
    private val byAlias=deck.flatMap { c -> c.aliases.map { it to c } }.groupBy({it.first},{it.second})
    val assignments: List<IdentityAssociation>
    private val claims: Map<String,List<IdentityAssociation>>
    init {
        val proposed=native.map(::associate)
        val collisions=proposed.filter { it.oracleId!=null }.groupBy { it.oracleId }.filterValues { it.size>1 }.keys
        val duplicateNames=native.groupBy { it.name }.filterValues { it.size>1 }.keys
        assignments=proposed.map { a -> if(a.oracleId in collisions || a.native.name in duplicateNames)
            a.copy(oracleId=null,confidence=IdentityConfidence.CONFLICT,method="NATIVE_IDENTITY_COLLISION",evidence=a.evidence+"MULTIPLE_NATIVE_PHYSICAL_DEFINITIONS_CLAIM_ONE_IDENTITY") else if(a.oracleId!=null) a.copy(metadata=metadataAgreement(a.native,byId.getValue(a.oracleId))) else a }
        claims=assignments.flatMap { a -> a.candidateOracleIds.map { it to a } }.groupBy({it.first},{it.second})
    }
    fun artifact()=EnrichmentArtifact(context,assignments)
    private fun consistency(n: NativeIdentity,e: OracleIdentity): List<String> {
        val errors=mutableListOf<String>()
        if(e.issues.any { it in identityIssues }) errors += "EXTERNAL_IDENTITY_RECORD_UNRESOLVED"
        val nf=n.faces
        val ef=if(e.faces.isEmpty()) listOf(OracleFace(e.name,e.typeLine,e.oracleText,e.manaCost,null,null)) else e.faces
        if(n.layout==null || n.physicalName==null || nf.isEmpty()) return errors+"NATIVE_IDENTIFYING_FIELDS_MISSING"
        if(key(n.physicalName)!=key(e.name.orEmpty())) errors += "PHYSICAL_NAME_DISAGREEMENT"
        if((e.layout in multiLayouts && n.layout!=e.layout) || (e.layout !in multiLayouts && n.layout!="normal")) errors += "PHYSICAL_LAYOUT_DISAGREEMENT"
        if(nf.map { key(it.name) }!=ef.map { key(it.name.orEmpty()) }) errors += "COMPLETE_ORDERED_FACE_NAMES_DISAGREE"
        if(nf.size==ef.size) nf.zip(ef).forEachIndexed { i,(a,b) ->
            if(types(a.typeLine).isEmpty() || types(a.typeLine)!=types(b.typeLine)) errors += "FACE_${i}_CARD_TYPES_DISAGREE_OR_MISSING"
            // Transform/flip backs have no separate cast cost; native flip backs may retain the front cost.
            if(i==0 || e.layout in setOf("split","adventure","modal_dfc"))
                if(cost(a.manaCost)==null || cost(a.manaCost)!=cost(b.manaCost)) errors += "FACE_${i}_MANA_COST_DISAGREES_OR_MISSING"
        }
        return errors.distinct()
    }
    private fun associate(n: NativeIdentity): IdentityAssociation {
        val physical=byPhysicalName[key(n.physicalName ?: n.name)].orEmpty()
        val source=n.scryfallId?.let { bySourceId[key(it)].orEmpty() }.orEmpty()
        val aliases=n.aliases.flatMap { byAlias[it].orEmpty() }.distinctBy { it.oracleId }
        val candidates=(physical+source+aliases).mapNotNull { it.oracleId }.distinct().sorted()
        fun decision(id: String?,confidence: IdentityConfidence,method: String,evidence: List<String>,ids: List<String> = candidates)=
            IdentityAssociation(n,id,ids,confidence,method,evidence+if(n.scryfallId!=null && source.isEmpty()) listOf("NATIVE_PRINTING_ID_NOT_IN_REPRESENTATIVE_SNAPSHOT; NOT_AN_ORACLE_CONFLICT") else emptyList())
        if(n.declaredOracleId!=null && n.oracleId==null)
            return decision(null,IdentityConfidence.CONFLICT,"INVALID_NATIVE_ORACLE_ID",listOf("MALFORMED_DECLARED_ID_REQUIRES_REVIEW; NOT_REPLACED"))
        val existing=n.oracleId?.let { byId[it] }
        val proposed=when { source.size==1 -> source.single();physical.size==1 -> physical.single();else -> null }
        if(n.oracleId!=null) {
            if(existing==null) return decision(null,IdentityConfidence.CONFLICT,"EXISTING_ID_ABSENT_FROM_SNAPSHOT",listOf("NATIVE_ORACLE_ID_NOT_REPLACED"))
            if(existing.layout in nonDeckLayouts || existing.issues.any { it in identityIssues }) return decision(null,IdentityConfidence.REVIEW_REQUIRED,"EXISTING_ID_REQUIRES_REVIEW",listOf("NON_DECK_OR_UNRESOLVED_EXTERNAL_RECORD"),listOf(n.oracleId))
            if(proposed!=null && proposed.oracleId!=n.oracleId && (source.size==1 || consistency(n,proposed).isEmpty()))
                return decision(null,IdentityConfidence.CONFLICT,"EXISTING_ID_ASSOCIATION_DISAGREEMENT",listOf("NATIVE_ORACLE_ID_NOT_REPLACED","INDEPENDENT_SOURCE_OR_PHYSICAL_EVIDENCE_POINTS_TO_DIFFERENT_ORACLE_ID"),(candidates+listOf(n.oracleId)).distinct().sorted())
            return decision(n.oracleId,IdentityConfidence.CONFIDENT,"ORACLE_ID",listOf("EXISTING_NATIVE_ORACLE_ID_PRESERVED")+consistency(n,existing).map { "IDENTIFYING_FIELD_DIAGNOSTIC:$it" },listOf(n.oracleId))
        }
        if(source.size>1 || physical.size>1) return decision(null,IdentityConfidence.AMBIGUOUS,"MULTIPLE_EXTERNAL_PHYSICAL_IDENTITIES",listOf("FULL_NAME_OR_SOURCE_IDENTIFIER_NOT_UNIQUE"))
        if(source.size==1) {
            val e=source.single()
            if(physical.size==1 && physical.single().oracleId!=e.oracleId) return decision(null,IdentityConfidence.CONFLICT,"SOURCE_ID_PHYSICAL_NAME_CONFLICT",listOf("SOURCE_ID_AND_PHYSICAL_NAME_DISAGREE"))
            if(e.issues.any { it in identityIssues }) return decision(null,IdentityConfidence.REVIEW_REQUIRED,"SOURCE_ID_UNRESOLVED_RECORD",e.issues)
            return decision(e.oracleId,IdentityConfidence.CONFIDENT,"SCRYFALL_PRINTING_ID",listOf("EXACT_SNAPSHOT_PRINTING_ID")+consistency(n,e).map { "IDENTIFYING_FIELD_DIAGNOSTIC:$it" },listOf(e.oracleId!!))
        }
        if(physical.size==1) {
            val e=physical.single();val errors=consistency(n,e)
            if(errors.isEmpty()) return decision(e.oracleId,IdentityConfidence.CONFIDENT,"PHYSICAL_NAME_LAYOUT_COST_TYPES",listOf("UNIQUE_FULL_PHYSICAL_NAME","COMPLETE_ORDERED_FACE_STRUCTURE","MATCHING_CAST_COSTS_AND_CARD_TYPES","NO_ORACLE_TEXT_SIMILARITY_USED"),listOf(e.oracleId!!))
            return decision(null,IdentityConfidence.REVIEW_REQUIRED,"PHYSICAL_CANDIDATE_REQUIRES_REVIEW",errors)
        }
        return decision(null,if(candidates.isEmpty()) IdentityConfidence.MISSING else IdentityConfidence.REVIEW_REQUIRED,
            if(candidates.isEmpty()) "NO_EXTERNAL_IDENTITY_CANDIDATE" else "FACE_ALIAS_CANDIDATE_ONLY",listOf("FACE_ALIAS_IS_NOT_A_PHYSICAL_CARD_IDENTITY"))
    }
    fun match(external: OracleIdentity): NativeMapping {
        require(external.oracleId!=null && byId[external.oracleId]==external) { "External record is outside the bound enrichment snapshot" }
        val rows=claims[external.oracleId].orEmpty().filter { it.oracleId==external.oracleId || it.confidence!=IdentityConfidence.CONFIDENT }
        val confident=rows.filter { it.confidence==IdentityConfidence.CONFIDENT }
        if(confident.size==1) {
            val a=confident.single();val metadata=metadataAgreement(a.native,external)
            return NativeMapping(if(metadata.issues.isEmpty()) CatalogStatus.NATIVE_AVAILABLE else CatalogStatus.NATIVE_METADATA_UNRESOLVED,
                a.method,listOf(a.native),metadata.issues,a,metadata)
        }
        if(rows.isNotEmpty()) {
            val ambiguous=rows.size>1 || rows.any { it.confidence in setOf(IdentityConfidence.AMBIGUOUS,IdentityConfidence.CONFLICT) }
            val a=rows.first().copy(oracleId=null,confidence=if(rows.any { it.confidence==IdentityConfidence.CONFLICT }) IdentityConfidence.CONFLICT else if(ambiguous) IdentityConfidence.AMBIGUOUS else IdentityConfidence.REVIEW_REQUIRED,
                evidence=rows.flatMap { it.evidence }.distinct())
            return NativeMapping(if(ambiguous) CatalogStatus.AMBIGUOUS_MAPPING else CatalogStatus.NATIVE_IDENTITY_REVIEW_REQUIRED,
                if(rows.size>1) "MULTIPLE_UNRESOLVED_NATIVE_CANDIDATES" else a.method,rows.map { it.native },a.evidence,a)
        }
        return NativeMapping(CatalogStatus.NATIVE_DEFINITION_MISSING,"NO_NATIVE_PHYSICAL_ASSOCIATION",emptyList())
    }
}
