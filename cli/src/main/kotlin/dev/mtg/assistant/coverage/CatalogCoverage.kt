package dev.mtg.assistant.coverage

import com.wingedsheep.sdk.model.CardDefinition
import dev.mtg.assistant.Catalog
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.eligibility.*
import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

val coverageObjectives = listOf("own-nontoken-creature-deaths","reanimate-own-creatures","create-creature-tokens")
@Serializable enum class CatalogStatus { NATIVE_AVAILABLE, NATIVE_METADATA_UNRESOLVED, NATIVE_MECHANICALLY_UNRESOLVED, NATIVE_DEFINITION_MISSING, AMBIGUOUS_MAPPING, NATIVE_IDENTITY_REVIEW_REQUIRED }
@Serializable data class NativeFaceEvidence(val name: String, val manaCost: String?, val typeLine: String?)
@Serializable data class NativeIdentity(val name: String, val oracleId: String?, val aliases: Set<String>,
    val identity: Set<String>?, val commanderLegality: String, val origin: String,
    val layout: String? = null, val faces: List<NativeFaceEvidence> = emptyList(), val physicalName: String? = null,
    val scryfallId: String? = null, val setCode: String? = null, val collectorNumber: String? = null,
    val derivedIdentity: Set<String>? = null, val declaredOracleId: String? = null)
@Serializable data class NativeMapping(val status: CatalogStatus, val method: String,
    val candidates: List<NativeIdentity>, val reasons: List<String> = emptyList(),
    val association: IdentityAssociation? = null, val metadata: MetadataAgreement? = null,
    val effectiveMetadata: EffectiveEligibility? = null, val effectiveAgreement: MetadataAgreement? = null)
@Serializable data class ObjectiveEvaluation(val state: String, val matchedPathways: Int = 0,
    val unknownPathways: Int = 0, val unsupportedNodes: Int = 0,
    val conditionalMatchedPathways: Int = 0, val partialMatchedPathways: Int = 0,
    val mechanicalNonmatchPathways: Int = 0, val policyExcludedPathways: Int = 0)
@Serializable data class TranslationEntry(val oracleId: String, val status: String)
@Serializable data class TranslationLedger(val snapshotSha256: String, val cards: List<TranslationEntry>)
@Serializable data class CoverageCard(val external: OracleIdentity, val status: CatalogStatus,
    val mapping: NativeMapping, val constructionNotes: List<String>,
    val objectives: Map<String,ObjectiveEvaluation>, val translationStatus: String)
@Serializable data class PoolIssue(val external: OracleIdentity, val decision: PoolDecision)
@Serializable data class CorrectionEligibilityImpact(val metadata: EffectiveEligibility, val beforeFormat: Verdict, val beforeDeck: Verdict,
    val afterFormat: Verdict, val afterDeck: Verdict)
@Serializable data class CatalogCoverageReport(val schemaVersion: Int = 3, val snapshot: SnapshotManifest,
    val argentumCommit: String = ARGENTUM_SHA, val commander: String, val allowedIdentity: List<String>,
    val counts: Map<String,Int>, val poolExclusions: Map<String,Int>, val records: List<CoverageCard>,
    val unresolvedPoolRecords: List<PoolIssue>, val nativeWithoutExternalMatch: List<NativeIdentity>,
    val timingsMillis: Map<String,Long>, val enrichment: EnrichmentContext? = null, val eligibilityCorrections: List<CorrectionEligibilityImpact> = emptyList(),
    val correctionValidationMillis: Long = 0, val limitations: List<String> = listOf(
        "Snapshot legality/identity and pinned native eligibility are distinct sources; original disagreement remains visible; only explicitly reviewed corrections affect effective native eligibility.",
        "Native availability is authored coverage, not independent verification of all card behavior.",
        "Missing or uncertain associations are not mechanical NO_MATCH. Confident associations may attach existing native evaluations even when metadata disagrees; only explicitly reviewed corrections can change native eligibility.",
        "An evaluated native definition with no supported match is not proof that the Oracle card cannot contribute; unsupported native mechanics remain unresolved.",
        "Ordinary inclusion only. Pairing, companions as companions, singleton/quantity exceptions, outside-game and whole-deck construction are not validated.",
        "Unknown and conflicting external records remain outside the confirmed eligible denominator and are listed separately.",
        "The manifest and SHA256 identify local input integrity and declared provenance; they are not a Scryfall signature. No network is used by this command."
    ))

fun nativeIdentity(card: CardDefinition, origin: String): NativeIdentity {
    fun names(c: CardDefinition): List<String> = listOf(c.name)+c.cardFaces.map { it.name }+
        (c.backFace?.let(::names) ?: emptyList())+(c.flipSide?.let(::names) ?: emptyList())
    fun face(c: CardDefinition)=NativeFaceEvidence(c.name,if(c.hasNoManaCost) "" else c.manaCost.toString(),c.typeLine.toString())
    val faces=when {
        card.layout.name=="SPLIT" -> card.cardFaces.map { NativeFaceEvidence(it.name,it.manaCost.toString(),it.typeLine.toString()) }
        card.backFace!=null -> listOf(face(card),face(card.backFace!!))
        card.flipSide!=null -> listOf(face(card),face(card.flipSide!!))
        card.cardFaces.isNotEmpty() -> listOf(face(card))+card.cardFaces.map { NativeFaceEvidence(it.name,it.manaCost.toString(),it.typeLine.toString()) }
        else -> listOf(face(card))
    }
    val layout=when {
        card.flipSide!=null -> "flip"
        card.backFace!=null -> if(card.layout.name=="MODAL_DFC") "modal_dfc" else "transform"
        else -> card.layout.name.lowercase(Locale.ROOT)
    }
    return NativeIdentity(card.name,oracleId(card.oracleId),names(card).map { it.lowercase(Locale.ROOT) }.toSet(),
        if(card.colorIdentityOverride==null) null else fullIdentity(card).map { it.symbol.toString() }.toSet(),
        commanderFormatLegality(card).name,origin,layout,faces,faces.joinToString(" // ") { it.name },
        oracleId(card.metadata.scryfallId),card.setCode,card.metadata.collectorNumber,fullIdentity(card.copy(colorIdentityOverride=null)).map { it.symbol.toString() }.toSet(),card.oracleId)
}

/** Legacy diagnostics without a snapshot; source-bound enrichment when the complete snapshot is supplied. */
class NativeIndex(val cards: List<NativeIdentity>, snapshot: OracleSnapshot? = null) {
    val enrichment: OracleEnrichment? = snapshot?.let { OracleEnrichment(cards,it) }
    private val byId = cards.filter { it.oracleId!=null }.groupBy { it.oracleId }
    private val byAlias = cards.flatMap { c -> c.aliases.map { it to c } }.groupBy({it.first},{it.second})
    fun match(external: OracleIdentity): NativeMapping {
        enrichment?.let { return it.match(external) }
        val ids = byId[external.oracleId].orEmpty()
        if(ids.size>1) return NativeMapping(CatalogStatus.AMBIGUOUS_MAPPING,"ORACLE_ID_COLLISION",ids,listOf("MULTIPLE_NATIVE_PHYSICAL_DEFINITIONS"))
        if(ids.size==1) {
            val native = ids.single()
            val reasons = buildList {
                if(native.identity==null) add("NATIVE_FULL_IDENTITY_MISSING")
                else if(native.identity!=external.colorIdentity.orEmpty().toSet()) add("NATIVE_EXTERNAL_IDENTITY_DISAGREEMENT")
                if(native.commanderLegality==Verdict.UNKNOWN.name) add("NATIVE_LEGALITY_MISSING")
            }
            return NativeMapping(if(reasons.isEmpty()) CatalogStatus.NATIVE_AVAILABLE else CatalogStatus.NATIVE_METADATA_UNRESOLVED,"ORACLE_ID",ids,reasons)
        }
        val aliases = external.aliases.flatMap { byAlias[it].orEmpty() }.distinct()
        if(aliases.isNotEmpty()) return NativeMapping(
            if(aliases.size==1 && aliases.single().oracleId==null && external.mappingNotes.isEmpty()) CatalogStatus.NATIVE_METADATA_UNRESOLVED else CatalogStatus.AMBIGUOUS_MAPPING,
            "NAME_OR_FACE_CANDIDATE_ONLY",aliases,listOf("NO_AUTHORITATIVE_ORACLE_ID_MATCH")+external.mappingNotes)
        return NativeMapping(CatalogStatus.NATIVE_DEFINITION_MISSING,"NO_ID_OR_ALIAS_CANDIDATE",emptyList())
    }
}

fun catalogCoverage(catalog: Catalog, snapshot: OracleSnapshot, commanderName: String,
    objectives: List<String> = emptyList(), ledger: TranslationLedger? = null, catalogLoadMillis: Long = 0): CatalogCoverageReport {
    require(objectives.distinct().size==objectives.size && objectives.all { it in coverageObjectives }) { "Unsupported/duplicate objective" }
    val commander = resolveCommander(catalog,commanderName)
    val resolver=catalog.eligibility
    resolver.requireSource(snapshot.manifest.sha256,listOf(commander.name))
    val allowed = (resolver.resolve(commander).colors + identityRestrictions(commander)).map { it.symbol.toString() }.toSet()
    if(ledger!=null) {
        require(ledger.snapshotSha256==snapshot.manifest.sha256) { "Translation ledger snapshot mismatch" }
        require(ledger.cards.map { it.oracleId }.distinct().size==ledger.cards.size && ledger.cards.all { oracleId(it.oracleId)!=null }) { "Translation ledger identity collision/invalid ID" }
        require(ledger.cards.all { it.status in setOf("INVESTIGATED_ASSISTED_AUTHORING","INVESTIGATED_SUPPORT_REQUIRED","INVESTIGATED_UNRESOLVED","VERIFIED_NATIVE_EXTENSION") }) { "Unknown translation status" }
    }
    val started = System.nanoTime()
    val native = physicalCards(catalog.cards).map { nativeIdentity(it,catalog.provenance(it.name).origin) }
    val index = NativeIndex(native,snapshot)
    val pool = snapshot.cards.map { it to externalEligibility(it,allowed) }
    val nativeCards=physicalCards(catalog.cards).associateBy { it.name }
    val mappings = pool.filter { it.second.state==PoolState.ELIGIBLE }.map { (external,decision) ->
        val mapping=index.match(external)
        val associated=mapping.association?.takeIf { it.confidence==IdentityConfidence.CONFIDENT }
        val effective=associated?.let {
            resolver.requireSource(snapshot.manifest.sha256,listOf(it.native.name))
            resolver.resolve(nativeCards.getValue(it.native.name))
        }
        val agreement=if(associated!=null && effective!=null) metadataAgreement(associated.native.copy(
            identity=effective.identity?.toSet(),commanderLegality=effective.commanderLegality.name),external) else null
        Triple(external,decision,if(agreement==null) mapping else mapping.copy(
            status=if(agreement.issues.isEmpty()) CatalogStatus.NATIVE_AVAILABLE else CatalogStatus.NATIVE_METADATA_UNRESOLVED,
            effectiveMetadata=effective, effectiveAgreement=agreement))
    }
    val matchingMillis = (System.nanoTime()-started)/1_000_000
    val evaluationStart = System.nanoTime()
    val reports = objectives.associateWith { discover(catalog,commanderName,objective=it) }
    // Group once, rather than rescanning the entire discovery report for every external card.
    val evaluations = reports.mapValues { (_,report) ->
        val results=report.results.groupBy { it.card }; val unsupported=report.unsupported.groupBy { it.card }
        val support=report.structuralSupport.groupBy { it.card }
        report.mechanicalInputCards.orEmpty().associateWith { name ->
            val unknown=results[name].orEmpty().count { it.verdict==Verdict.UNKNOWN }
            fun node(action: String,face: String,path: String,type: String) = listOf(action,face,path,type.substringAfterLast('.').substringAfterLast('$'))
            val missing=(unsupported[name].orEmpty().map { node(it.actionId,it.face,it.path,it.nativeType) }+
                support[name].orEmpty().filter { it.status!=SupportStatus.SUPPORTED }.map { node(it.actionId,it.face,it.path,it.nativeType) }).distinct().size
            val matches=results[name].orEmpty().count { it.verdict==Verdict.MATCH }
            ObjectiveEvaluation(if(unknown>0 || missing>0) "NATIVE_MECHANICALLY_UNRESOLVED" else if(matches>0) "EVALUATED_STRUCTURAL_MATCH" else "EVALUATED_NO_SUPPORTED_MATCH",matches,unknown,missing,
                conditionalMatchedPathways=results[name].orEmpty().count { it.verdict==Verdict.MATCH && it.prerequisites.isNotEmpty() },
                partialMatchedPathways=results[name].orEmpty().count { it.verdict==Verdict.MATCH && (it.interpretation.startsWith("PARTIAL") || it.localSupport!=SupportStatus.SUPPORTED) },
                mechanicalNonmatchPathways=results[name].orEmpty().count { it.verdict==Verdict.NO_MATCH && it.mechanicalCompatibility==Verdict.NO_MATCH },
                policyExcludedPathways=results[name].orEmpty().count { it.verdict==Verdict.NO_MATCH && it.mechanicalCompatibility==Verdict.MATCH && it.policyAcceptance==Verdict.NO_MATCH })
        }
    }
    val translation = ledger?.cards.orEmpty().associate { it.oracleId to it.status }
    val records = mappings.map { (external,decision,mapping) ->
        val nativeName = mapping.candidates.singleOrNull()?.name
        val outcomes = objectives.associateWith { objective ->
            if(mapping.association?.confidence!=IdentityConfidence.CONFIDENT) ObjectiveEvaluation("NOT_EVALUATED_${mapping.status}")
            else evaluations[objective]?.get(nativeName) ?: ObjectiveEvaluation("NOT_EVALUATED_NATIVE_ELIGIBILITY_DISAGREEMENT")
        }
        val status = if(mapping.status==CatalogStatus.NATIVE_AVAILABLE && outcomes.values.any { it.state=="NATIVE_MECHANICALLY_UNRESOLVED" }) CatalogStatus.NATIVE_MECHANICALLY_UNRESOLVED else mapping.status
        CoverageCard(external,status,mapping,decision.constructionNotes,outcomes,translation[external.oracleId] ?: "NOT_INVESTIGATED")
    }
    val unresolved = pool.filter { it.second.state==PoolState.UNRESOLVED }.map { PoolIssue(it.first,it.second) }
    val exclusions = pool.filter { it.second.state==PoolState.EXCLUDED }.flatMap { it.second.reasons }.groupingBy { it }.eachCount().toSortedMap()
    val confident = records.filter { it.mapping.association?.confidence==IdentityConfidence.CONFIDENT && it.mapping.candidates.size==1 }
    val counts = linkedMapOf("raw_snapshot_records" to snapshot.rawRecords,"duplicate_records_collapsed" to snapshot.duplicateRecords,
        "distinct_snapshot_records" to snapshot.cards.size,"eligible_oracle_cards" to records.size,
        "excluded_records" to pool.count { it.second.state==PoolState.EXCLUDED },"unresolved_pool_records" to unresolved.size,
        "confident_oracle_id_matches" to confident.size,"confident_native_identity_associations" to confident.size,
        "existing_native_oracle_id_matches" to confident.count { it.mapping.method=="ORACLE_ID" },
        "enriched_identity_associations" to confident.count { it.mapping.method!="ORACLE_ID" },"native_physical_definitions" to native.size,
        "matched_upstream_definitions" to confident.count { it.mapping.candidates.single().origin=="UPSTREAM_ARGENTUM" },
        "matched_application_extensions" to confident.count { it.mapping.candidates.single().origin=="APPLICATION_EXTENSION" },
        "identity_review_required" to records.count { it.mapping.association?.confidence==IdentityConfidence.REVIEW_REQUIRED },
        "identity_conflicts" to records.count { it.mapping.association?.confidence==IdentityConfidence.CONFLICT },
        "native_metadata_discrepancies" to confident.count { it.mapping.metadata?.issues?.isNotEmpty()==true },
        "effective_metadata_discrepancies" to confident.count { it.mapping.effectiveAgreement?.issues?.isNotEmpty()==true },
        "approved_corrections_in_external_pool" to confident.count { it.mapping.effectiveMetadata?.corrections?.any { c -> c.state=="APPLIED" }==true },
        "native_not_evaluated" to records.count { it.objectives.isEmpty() || it.objectives.values.all { o -> o.state.startsWith("NOT_EVALUATED") } },
        "native_mechanical_unresolved_any_objective" to records.count { it.objectives.values.any { o -> o.state=="NATIVE_MECHANICALLY_UNRESOLVED" } },
        "native_eligibility_disagreements" to confident.count { it.mapping.candidates.single().commanderLegality!=Verdict.MATCH.name || it.mapping.reasons.isNotEmpty() },
        "missing_investigated" to records.count { it.status==CatalogStatus.NATIVE_DEFINITION_MISSING && it.translationStatus!="NOT_INVESTIGATED" },
        "missing_uninvestigated" to records.count { it.status==CatalogStatus.NATIVE_DEFINITION_MISSING && it.translationStatus=="NOT_INVESTIGATED" })
    CatalogStatus.entries.forEach { status -> counts[status.name.lowercase(Locale.ROOT)] = records.count { it.status==status } }
    check(CatalogStatus.entries.sumOf { counts.getValue(it.name.lowercase(Locale.ROOT)) }==records.size)
    check(records.size+counts.getValue("excluded_records")+unresolved.size==snapshot.cards.size)
    return CatalogCoverageReport(snapshot=snapshot.manifest,commander=commanderName,allowedIdentity=allowed.sorted(),counts=counts,
        poolExclusions=exclusions,records=records,unresolvedPoolRecords=unresolved,
        nativeWithoutExternalMatch=index.enrichment!!.assignments.filter { it.confidence!=IdentityConfidence.CONFIDENT }.map { it.native },
        enrichment=index.enrichment!!.context,
        eligibilityCorrections=resolver.reviewed().map { effective ->
            val card=nativeCards.getValue(effective.nativeName)
            val colors=allowed.map { symbol -> com.wingedsheep.sdk.core.Color.entries.single { it.symbol.toString()==symbol } }.toSet()
            CorrectionEligibilityImpact(effective,commanderFormatLegality(card),selectedCommanderEligibility(card,colors),
                effective.commanderLegality,selectedCommanderEligibility(card,colors,effective))
        },correctionValidationMillis=resolver.validationMillis,
        timingsMillis=mapOf("snapshotLoadAndHash" to snapshot.loadMillis,"catalogLoad" to catalogLoadMillis,
            "poolAndMapping" to matchingMillis,"objectiveEvaluation" to (System.nanoTime()-evaluationStart)/1_000_000))
}

@Serializable data class MissingInventory(val snapshotSha256: String, val commander: String, val status: CatalogStatus,
    val total: Int, val offset: Int, val limit: Int, val items: List<CoverageCard>)

fun catalogCoverageCommand(args: List<String>, catalog: () -> Catalog, out: (String)->Unit) {
    val options=linkedMapOf<String,String>(); var json=false; var i=1
    while(i<args.size) {
        val key=args[i++]
        if(key=="--json") { require(!json) { "Duplicate --json" }; json=true }
        else { require(key.startsWith("--") && i<args.size && !args[i].startsWith("--") && key !in options) { "Missing/duplicate option: $key" }; options[key]=args[i++] }
    }
    fun required(key: String)=requireNotNull(options[key]) { "$key required" }
    val inventory=args.first()=="catalog-missing"
    val allowed=if(inventory) setOf("--report","--limit","--offset","--name","--status","--output") else setOf("--commander","--oracle-cards","--manifest","--objective","--translation-review","--output","--enrichment-output")
    require(options.keys.all { it in allowed }) { "Unknown coverage option: ${options.keys-allowed}" }
    val text = if(inventory) {
        val report=snapshotJson.decodeFromString<CatalogCoverageReport>(Files.readString(Path.of(required("--report"))))
        val limit=options["--limit"]?.toIntOrNull() ?: if("--limit" in options) throw IllegalArgumentException("Invalid limit") else 20
        val offset=options["--offset"]?.toIntOrNull() ?: if("--offset" in options) throw IllegalArgumentException("Invalid offset") else 0
        require(limit in 1..200 && offset>=0) { "limit must be 1..200 and offset nonnegative" }
        val status=options["--status"]?.let { value -> CatalogStatus.entries.firstOrNull { it.name==value } ?: throw IllegalArgumentException("Unknown status: $value") } ?: CatalogStatus.NATIVE_DEFINITION_MISSING
        val filtered=report.records.filter { it.status==status && (options["--name"]==null || it.external.name.orEmpty().contains(options.getValue("--name"),true)) }
        val page=MissingInventory(report.snapshot.sha256,report.commander,status,filtered.size,offset,limit,filtered.drop(offset).take(limit))
        if(json) snapshotJson.encodeToString(page) else "${page.commander}: ${page.total} $status; offset $offset; limit $limit\n"+page.items.joinToString("\n") { "${it.external.oracleId}\t${it.external.name}\t${it.translationStatus}" }
    } else {
        val path=Path.of(required("--oracle-cards"))
        val snapshot=loadOracleSnapshot(path,options["--manifest"]?.let(Path::of) ?: Path.of("$path.manifest.json"))
        val objectives=options["--objective"]?.let { if(it=="all") coverageObjectives else listOf(it) }.orEmpty()
        val ledger=options["--translation-review"]?.let { snapshotJson.decodeFromString<TranslationLedger>(Files.readString(Path.of(it))) }
        val start=System.nanoTime();val data=catalog();val elapsed=(System.nanoTime()-start)/1_000_000
        val report=catalogCoverage(data,snapshot,required("--commander"),objectives,ledger,elapsed)
        options["--enrichment-output"]?.let { destination ->
            val artifact=OracleEnrichment(physicalCards(data.cards).map { nativeIdentity(it,data.provenance(it.name).origin) },snapshot).artifact()
            val output=Path.of(destination).toAbsolutePath();Files.createDirectories(output.parent)
            Files.writeString(output,snapshotJson.encodeToString(artifact))
        }
        if(json) snapshotJson.encodeToString(report) else "${report.commander}; Scryfall ${report.snapshot.updatedAt}; SHA256 ${report.snapshot.sha256}\nArgentum ${report.argentumCommit}\n${report.counts}\nTimings ms: ${report.timingsMillis}\nUse audit catalog-missing --report <JSON report> --limit 20 for the inventory."
    }
    options["--output"]?.let { val p=Path.of(it).toAbsolutePath(); Files.createDirectories(p.parent); Files.writeString(p,text); out("Wrote $p") } ?: out(text)
}
