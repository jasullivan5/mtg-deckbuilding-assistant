package dev.mtg.assistant.coverage

import dev.mtg.assistant.Catalog
import dev.mtg.assistant.EngineBuild
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.explorer.*
import dev.mtg.assistant.extensions.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import com.wingedsheep.sdk.tooling.CardExporter
import java.time.Instant
import java.util.zip.GZIPInputStream

@Serializable data class VerificationRecord(val id:String, val oracleId:String, val card:String, val kind:String,
    val scopeId:String, val scope:String, val face:String?=null, val printingId:String?=null,
    val definitionSha256:String?=null, val engine:String?=null, val applicationRevision:String?=null,
    val author:String, val result:String, val expected:String, val observed:String,
    val source:List<EvidenceReference> = emptyList(), val requirements:List<String> = emptyList(),
    val requirementResults:Map<String,String> = emptyMap(), val scenarioId:String?=null,
    val observedAt:String?=null, val historical:Boolean=false, val sourceValidated:Boolean=false,
    val reference:String, val materialConcerns:List<String> = emptyList(), val limits:List<String> = emptyList())
@Serializable data class IndexedVerification(val record:VerificationRecord,val freshness:String,val reasons:List<String>)
@Serializable data class InterpretedCard(val oracleId:String,val card:String,val definitionSha256:String,
    val faces:List<String>,val actionIds:List<String>,val counts:Map<String,Int>,
    val scope:String="Native walker records only; full Oracle mechanic/ability completeness is not established")
@Serializable data class VerificationCoverage(val schemaVersion:Int=1,val measurementDefinition:String="scope-aware-verification-v1",
    val measuredAt:String,val applicationRevision:String,val engine:String,val snapshot:SnapshotManifest,
    val populationPolicy:String,val populationSha256:String,val population:List<String>,
    val counts:Map<String,Int>,val interpretation:List<InterpretedCard>,val evidence:List<IndexedVerification>,
    val qualification:Map<String,Int>,val unindexed:List<String>,val challenge:SacrificeChallengeReport,
    val nativeReviewDefinitions:Map<String,JsonElement> = emptyMap(),val qualifiedScopes:List<String> = emptyList(),
    val completeness:String="NOT_ESTABLISHED_AT_CATALOG_SCALE",
    val completenessRequirements:List<String> = listOf("A source-derived inventory accounts for all relevant faces, printed abilities, keywords, alternative and intrinsic mechanics under a declared bounded review scope.",
        "Every inventoried material scope has current identity/source/definition-bound fidelity requirements and comparisons; supported facts alone are insufficient.",
        "Appropriate bounded positive, negative and consequential execution observations cover the declared requirements on the actual engine, with material unknowns and concerns resolved or explicitly outside scope.",
        "Independent qualification additionally requires the existing exact-bundle independent approval gate. Author challenges and tests cannot approve their own evidence.",
        "No current corpus source-to-native scope inventory proves completeness. Therefore no complete-card count or percentage is published."),
    val limits:List<String> = listOf("Evidence categories overlap and are not a cumulative funnel. Distinct physical cards, native records, scopes, requirement applications and scenarios are different units.",
        "A current observation means its recorded bindings still agree, not that it executed on this page load. Native execution is explicit and receipts retain their original date.",
        "The denominator is the immutable PR39 source population, not a silently refreshed external source. Interpretation is an application walk of native structures, not an Oracle completeness audit.",
        "Inherited definitions are not implicitly reviewed. Test file references and card-level execution claims without attributable scenario observations are retained as references, not execution passes."))

/** Observation ID includes its original receipt identity; repeated ingestion is idempotent, conflicting IDs fail closed. */
fun uniqueVerificationRecords(records:List<VerificationRecord>):List<VerificationRecord> = records.groupBy { it.id }.map { (id,rows) ->
    require(id.isNotBlank() && rows.all { it.oracleId.isNotBlank() && it.scopeId.isNotBlank() && it.reference.isNotBlank() }) { "Evidence identity, scope and reference are required" }
    require(rows.distinct().size==1) { "Conflicting evidence observation identity: $id" };rows.first()
}.sortedBy { it.id }

fun indexVerification(record:VerificationRecord,definitionSha256:String?,engine:String,faces:Set<String>):IndexedVerification {
    val reasons=buildList {
        if(definitionSha256==null) add("Confident current native definition unavailable")
        if(record.definitionSha256==null) add("Recorded native definition fingerprint missing")
        else if(record.definitionSha256!=definitionSha256) add("Native definition fingerprint changed")
        if(record.engine==null) add("Engine revision missing") else if(record.engine!=engine && !(record.kind=="SOURCE_REVIEW" && EngineBuild.evidenceCompatible(record.engine,engine))) add("Recorded engine differs from current engine")
        if(record.face!=null && record.face !in faces) add("Face not bound to the associated native physical card")
        if(record.kind=="SOURCE_REVIEW" && !record.sourceValidated) add("Source/requirements provenance is not fully validated for this projection")
        if(record.kind=="EXECUTION" && record.scenarioId.isNullOrBlank()) add("Scenario identity missing")
    }
    val freshness=when { record.historical->"HISTORICAL";definitionSha256==null || record.definitionSha256==null || record.engine==null || reasons.any { it.startsWith("Face") || it.startsWith("Scenario") }->"UNRESOLVED";reasons.isNotEmpty()->"STALE";else->"CURRENT" }
    return IndexedVerification(record,freshness,reasons)
}

fun retainedGlobalBaseline():GlobalBaseline = GlobalBaseline::class.java.getResourceAsStream("/global-commander-baseline.json.gz")!!.let {
    GZIPInputStream(it).bufferedReader().use { reader->reportJson.decodeFromString(reader.readText()) }
}

fun requireVerificationIdentityBinding(baseline:GlobalBaseline,identities:List<NativeIdentity>) {
    require(baseline.identityAlgorithm==IDENTITY_ALGORITHM && baseline.populationPolicy=="physical-format-population-v1") { "Verification reference identity/population policy changed; select an explicitly reviewed compatible baseline" }
    require(baseline.nativeIdentitySha256==nativeIdentityDigest(identities)) { "Native identity inventory changed; re-establish source associations explicitly before measuring verification coverage" }
}

/** A reference, setup or test name is not an observed native transaction. */
fun hasNativeExecutionTrace(value:JsonElement):Boolean = when(value) {
    is JsonObject -> (value["nativeAction"] != null && value["nativeAction"] != JsonNull && value["nativeResult"] != null && value["nativeResult"] != JsonNull) || value.values.any(::hasNativeExecutionTrace)
    is JsonArray -> value.any(::hasNativeExecutionTrace)
    else -> false
}
fun ledgerVerificationKind(state:FidelityState)=if(state==FidelityState.UNREVIEWED) "LEDGER_REFERENCE" else "SOURCE_REVIEW"

internal fun verificationCoverage(catalog:Catalog,revision:String,structural:List<StructuralSupport>,pilot:FidelityPilot,
    challenge:SacrificeChallengeReport,baseline:GlobalBaseline=retainedGlobalBaseline()):VerificationCoverage {
    // Validate the immutable population through the existing comparison contract.
    compareGlobalBaselines(baseline,baseline)
    requireVerificationIdentityBinding(baseline,physicalCards(catalog.cards).map { nativeIdentity(it,catalog.provenance(it.name).origin) })
    val legal=baseline.records.filter { it.poolState=="ELIGIBLE" }.associateBy { it.oracleId }
    val native=legal.values.filter { it.association=="CONFIDENT" }.mapNotNull { row->row.nativeName?.let(catalog::card)?.let { row.oracleId to it } }.toMap()
    val byName=native.entries.associate { it.value.name to it.key }
    val interpreted=structural.groupBy { it.card }.mapNotNull { (name,rows)->byName[name]?.let { id->InterpretedCard(id,name,definitionDigest(native.getValue(id)),rows.map { it.face }.distinct().sorted(),rows.map { it.actionId }.distinct().sorted(),
        SupportStatus.entries.associate { status->status.name to rows.count { it.status==status } }) } }.sortedBy { it.oracleId }
    val records=mutableListOf<VerificationRecord>()
    val assurances=catalog.cards.mapNotNull { catalog.translationAssurance(it.name) }.distinctBy { it.oracleId }
    assurances.forEach { a->
        val e=a.evidence ?: return@forEach
        records+=VerificationRecord("ledger:${e.oracleId}:${bundleDigest(e)}",e.oracleId,a.card,ledgerVerificationKind(e.fidelity),"ledger-unspecified","Retained extension card-level review; per-ability allocation not established",
            definitionSha256=e.definitionSha256,engine=e.argentumCommit,author=e.author,result=e.fidelity.name,
            expected="Retained source-derived requirements in the existing translation ledger",observed="${e.fidelity}; recorded execution claim ${e.execution}; current gate ${a.qualification}",
            source=e.source+e.requirements+e.tests,requirements=e.requirements.map { it.path },sourceValidated=a.evidenceCurrent,
            reference="src/main/resources/translation-assurance.json",materialConcerns=e.concerns.filter { it.state in setOf(ConcernState.SUSPECTED,ConcernState.CONFIRMED,ConcernState.UNRESOLVED) }.map { "${it.id}: ${it.claim}: ${it.reason}" },
            limits=e.limitations+"Legacy card-level execution labels/test references do not supply scenario-level outcomes to this index. Ability allocation remains unknown.")
    }
    fun review(review:PilotReview,hash:String,origin:String) {
        val scope=review.scope
        records+=VerificationRecord("$origin:review:${scope.oracleId}:$hash:${review.definitionSha256}",scope.oracleId,scope.card,"SOURCE_REVIEW",review.fact?.actionId ?: "$origin:unresolved-ability","One source-bound sacrifice ability; all other card behaviors outside review",face=scope.card,printingId=scope.printingId,
            definitionSha256=review.definitionSha256,engine=EngineBuild.revision,applicationRevision=revision,author="Codex Build (author-led, not independent)",result=when { review.checks.any { it.state=="CONTRADICTED" }->"CONTRADICTED_REQUIREMENTS";review.checks.any { it.state=="UNRESOLVED" }->"UNRESOLVED";else->"PARTIAL_SUPPORTED" },
            expected="Exact source-derived sacrifice requirements; inspect each check and source-specific additional requirements",observed=review.checks.joinToString { "${it.requirement}: ${it.state}" },
            source=listOf(EvidenceReference(scope.sourceFile,scope.sourceSha256)),requirements=review.checks.map { "$origin:$hash:${it.requirement}" },requirementResults=review.checks.associate { it.requirement to it.state },sourceValidated=true,
            reference="src/test/resources/fidelity/${if(origin=="pilot") "source-sacrifice" else "sacrifice-challenge"}/requirements.json",
            limits=listOf("Physical Oracle identity comes from the existing global association; a failed exact-printing check is a method binding concern, not automatically an Oracle mistranslation.","Static evidence does not establish object generation, full activation restrictions, all costs, target legality or remaining card behavior."))
    }
    pilot.reviews.forEach { review(it,pilot.requirementsSha256,"pilot") };challenge.reviews.forEach { review(it,challenge.requirementsSha256,"challenge") }
    fun receipt(receipt:PilotReceipt?,scopes:List<PilotScope>,current:Boolean,origin:String) {
        receipt ?: return
        receipt.scenarios.filterNot { it.scenarioId.startsWith("mutant:") }.forEach { s->
            val scope=scopes.singleOrNull { it.card==s.card } ?: return@forEach
            val reviewedAbility=(pilot.reviews+challenge.reviews).singleOrNull { it.scope.oracleId==scope.oracleId }?.fact?.actionId
            val boundScope=if(current && reviewedAbility!=null) reviewedAbility else "unallocated-historical-sacrifice:${scope.oracleId}"
            val kind=if(hasNativeExecutionTrace(s.observations)) "EXECUTION" else "EXECUTION_ATTEMPT"
            records+=VerificationRecord("execution:${s.scenarioId}:${receipt.engine}:${receipt.executedAt}",scope.oracleId,s.card,kind,boundScope,s.mechanic,face=scope.card,printingId=scope.printingId,
                definitionSha256=receipt.definitions[s.card],engine=receipt.engine,applicationRevision=receipt.applicationRevision,author="Codex Build native harness",
                result=when(s.state) { "AGREES"->"PASS";"DIFFERS"->"FAIL";else->"UNKNOWN" },expected=s.mechanic,observed="${s.state}; ${s.problem.orEmpty()}",source=listOf(EvidenceReference(scope.sourceFile,scope.sourceSha256)),
                requirements=listOf(receipt.requirementsSha256),scenarioId=s.scenarioId,observedAt=receipt.executedAt,sourceValidated=current,
                reference="src/main/resources/${if(origin=="pilot") "pilot-execution.json" else "sacrifice-challenge-execution.json"}",
                limits=listOf("Passing observation applies only to the recorded scenario. Whole-card and untested interaction claims are excluded.") + if(current) emptyList() else listOf("Receipt source/definition/harness bindings no longer agree; execution must not be promoted to current."))
        }
    }
    receipt(pilot.recordedExecution,pilot.reviews.map { it.scope },pilot.executionState=="RECORDED_CURRENT_BINDINGS","pilot")
    receipt(challenge.execution,challenge.reviews.map { it.scope },challenge.executionState=="RECORDED_CURRENT_BINDINGS","challenge")
    val spark=pilot.reviews.single { it.scope.card=="Spark Elemental" }.scope
    records+=VerificationRecord("historical:spark-blink:03869c0",spark.oracleId,spark.card,"EXECUTION","sacrifice:${spark.oracleId}","Old end-step source trigger after exile/return",engine="03869c08665bf476a8b40dacbfbcd501c54a33bd",author="Recorded source-sacrifice investigation; no independent approval",
        result="FAIL",expected="New object stays on battlefield after old trigger resolves",observed="Generation 100 was incorrectly sacrificed to generation 102",scenarioId="spark-blink",historical=true,
        reference="docs/SOURCE_SACRIFICE_DISCOVERY.md#confirmed-native-limitation--trigger-after-a-zone-round-trip",limits=listOf("Documentation-bound historical observation; exact old definition fingerprint and raw receipt are not indexed. Never current-engine evidence."))
    fun faces(card:com.wingedsheep.sdk.model.CardDefinition):Set<String> = setOf(card.name)+card.cardFaces.map { it.name }+(card.backFace?.let(::faces) ?: emptySet())+(card.flipSide?.let(::faces) ?: emptySet())
    val indexed=uniqueVerificationRecords(records).map { r->
        val n=native[r.oracleId]
        val base=indexVerification(r,n?.let(::definitionDigest),EngineBuild.revision,n?.let(::faces).orEmpty())
        // Harness/source mismatch must stale execution even if engine and definition happen to agree.
        if(r.kind=="EXECUTION" && !r.historical && !r.sourceValidated && base.freshness=="CURRENT") base.copy(freshness="STALE",reasons=base.reasons+"Receipt requirements or harness changed") else base
    }
    val inPopulation=indexed.filter { it.record.oracleId in legal }
    fun cards(kind:String,freshness:String?=null,result:String?=null)=inPopulation.filter { it.record.kind==kind && (freshness==null || it.freshness==freshness) && (result==null || it.record.result==result) }.map { it.record.oracleId }.distinct().size
    val counts=linkedMapOf("commander_legal_physical_cards" to legal.size,"definition_available_cards" to native.size,
        "interpretation_evidenced_cards" to interpreted.size,"interpretation_records" to interpreted.sumOf { it.counts.values.sum() },
        "interpretation_cards_with_partial_or_unsupported" to interpreted.count { it.counts.filterKeys { k->k!="SUPPORTED" }.values.sum()>0 },
        "source_review_current_cards" to cards("SOURCE_REVIEW","CURRENT"),"source_review_stale_cards" to cards("SOURCE_REVIEW","STALE"),
        "source_review_unresolved_cards" to cards("SOURCE_REVIEW","UNRESOLVED"),"execution_current_cards" to cards("EXECUTION","CURRENT"),
        "execution_current_pass_cards" to cards("EXECUTION","CURRENT","PASS"),"execution_current_fail_cards" to cards("EXECUTION","CURRENT","FAIL"),
        "execution_current_unknown_cards" to cards("EXECUTION","CURRENT","UNKNOWN"),"execution_historical_cards" to cards("EXECUTION","HISTORICAL"),
        "execution_stale_cards" to cards("EXECUTION","STALE"),"cards_with_indexed_source_or_execution" to inPopulation.map { it.record.oracleId }.distinct().size,
        "cards_with_any_indexed_evidence" to (interpreted.map { it.oracleId }+inPopulation.map { it.record.oracleId }).distinct().size,
        "source_review_scopes_current" to inPopulation.filter { it.record.kind=="SOURCE_REVIEW" && it.freshness=="CURRENT" }.map { it.record.oracleId to it.record.scopeId }.distinct().size,
        "requirement_applications_current" to inPopulation.filter { it.record.kind=="SOURCE_REVIEW" && it.freshness=="CURRENT" }.sumOf { it.record.requirementResults.size },
        "execution_observations_current" to inPopulation.count { it.record.kind=="EXECUTION" && it.freshness=="CURRENT" },
        "execution_observations_historical" to inPopulation.count { it.record.kind=="EXECUTION" && it.freshness=="HISTORICAL" },
        "evidence_records_outside_reference_population" to indexed.count { it.record.oracleId !in legal },
        "current_records_with_contradictions" to inPopulation.count { it.freshness=="CURRENT" && (it.record.result=="FAIL" || it.record.requirementResults.values.any { state->state=="CONTRADICTED" }) })
    return VerificationCoverage(measuredAt=Instant.now().toString(),applicationRevision=revision,engine=EngineBuild.revision,snapshot=baseline.snapshot,populationPolicy=baseline.populationPolicy,
        populationSha256=evidenceDigest(legal.keys.sorted().joinToString("\n",postfix="\n")),population=legal.keys.sorted(),counts=counts,interpretation=interpreted,evidence=indexed,
        qualification=QualificationState.entries.associate { q->q.name to assurances.count { it.qualification==q && it.oracleId in legal } } + mapOf("qualified_scope_cards" to assurances.filter { it.evidenceCurrent && it.qualification==QualificationState.QUALIFIED && it.oracleId in legal }.map { it.oracleId }.distinct().size,
            "not_measured_under_qualification_process" to legal.size-assurances.filter { it.oracleId in legal }.map { it.oracleId }.distinct().size),
        unindexed=listOf("Historical native test files without an executed, identity/definition/engine-bound observation receipt are unindexed, not passes or failures. Their total card/scenario coverage is not measured.",
            "22 extension ledger scopes are ingested at their recorded broad scope; references alone cannot be allocated to exact abilities or counted as scenario execution.",
            "The five Lab positions are already represented by their pilot receipt observations; reading the registry again does not create five more observations.",
            "Adversarial mutant definitions are method-challenge evidence only. They are not production-card failures or newly available cards."),challenge=challenge,
        nativeReviewDefinitions=native.filterKeys { id->inPopulation.any { it.record.oracleId==id } }.values.associate { it.name to reportJson.parseToJsonElement(CardExporter.exportToJson(it)) },
        qualifiedScopes=assurances.filter { it.evidenceCurrent && it.qualification==QualificationState.QUALIFIED && it.oracleId in legal }.map { "${it.oracleId}:ledger-unspecified:${bundleDigest(it.evidence!!)}" }.sorted())
}

@Serializable data class VerificationChange(val comparable:Boolean,val definitionChanges:List<String>,val newSourceScopes:List<String>,
    val newExecutionObservations:List<String>,val becameStale:List<String>,val newlyContradictory:List<String>,val resolvedConcerns:List<String>,
    val qualificationChanged:Boolean,val sourcePopulationChanged:Boolean,
    val addedPopulation:List<String> = emptyList(),val removedPopulation:List<String> = emptyList(),val sourceScopesInAddedPopulation:List<String> = emptyList(),
    val limits:String="New records and reclassifications are separate; changed measurement definitions suppress gain claims. Current-binding evidence is not necessarily newly executed.")
fun compareVerification(before:VerificationCoverage,after:VerificationCoverage):VerificationChange {
    fun valid(v:VerificationCoverage) {
        require(v.population.distinct().size==v.population.size && v.population.size==v.counts.getValue("commander_legal_physical_cards"))
        require(v.populationSha256==evidenceDigest(v.population.sorted().joinToString("\n",postfix="\n"))) { "Reference population fingerprint disagrees with records" }
        uniqueVerificationRecords(v.evidence.map { it.record })
    }
    valid(before);valid(after)
    val changes=buildList {
        if(before.schemaVersion!=after.schemaVersion || before.measurementDefinition!=after.measurementDefinition) add("Evidence schema/measurement changed")
        if(before.populationPolicy!=after.populationPolicy) add("Population policy changed")
        if(before.snapshot.sha256==after.snapshot.sha256 && before.populationSha256!=after.populationSha256) add("Same source snapshot but changed reference population")
    }
    val old=before.evidence.associateBy { it.record.id };val comparable=changes.isEmpty()
    fun scopeKey(e:IndexedVerification)="${e.record.oracleId}:${e.record.scopeId}"
    val priorSourceScopes=before.evidence.filter { it.record.kind=="SOURCE_REVIEW" && it.freshness=="CURRENT" }.map(::scopeKey).toSet()
    val newScopes=if(comparable) after.evidence.filter { it.record.kind=="SOURCE_REVIEW" && it.freshness=="CURRENT" && it.record.oracleId in before.population && scopeKey(it) !in priorSourceScopes }.map(::scopeKey).distinct() else emptyList()
    val newExecutions=if(comparable) after.evidence.filter { it.record.kind=="EXECUTION" && it.freshness=="CURRENT" && it.record.id !in old }.map { it.record.id } else emptyList()
    return VerificationChange(comparable,changes,newScopes,newExecutions,after.evidence.filter { it.freshness=="STALE" && old[it.record.id]?.freshness=="CURRENT" }.map { it.record.id },
        after.evidence.filter { it.record.result in setOf("FAIL","CONTRADICTED_REQUIREMENTS") && old[it.record.id]?.record?.result !in setOf("FAIL","CONTRADICTED_REQUIREMENTS") }.map { it.record.id },
        after.evidence.filter { old[it.record.id]?.record?.materialConcerns?.isNotEmpty()==true && it.record.materialConcerns.isEmpty() }.map { it.record.id },
        before.qualification!=after.qualification || before.qualifiedScopes!=after.qualifiedScopes,before.snapshot.sha256!=after.snapshot.sha256 || before.populationSha256!=after.populationSha256,
        (after.population-before.population.toSet()).sorted(),(before.population-after.population.toSet()).sorted(),
        if(comparable) after.evidence.filter { it.record.kind=="SOURCE_REVIEW" && it.record.oracleId !in before.population }.map(::scopeKey).distinct() else emptyList())
}
