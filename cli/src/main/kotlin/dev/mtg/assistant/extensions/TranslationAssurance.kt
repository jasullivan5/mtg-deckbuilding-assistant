package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.tooling.CardExporter
import dev.mtg.assistant.discovery.ARGENTUM_SHA
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.security.MessageDigest

@Serializable enum class RepresentationState { COMPLETE, PARTIAL, UNRESOLVED }
@Serializable enum class FidelityState { UNREVIEWED, SOURCE_REVIEWED, SUSPECTED_DISCREPANCY, CONFIRMED_DISCREPANCY }
@Serializable enum class ExecutionState { NOT_TESTED, BOUNDED_EVIDENCE_PASSED, FAILURE_OBSERVED }
@Serializable enum class QualificationState { PROVISIONAL, QUALIFIED, BLOCKED, WITHDRAWN }
@Serializable enum class ConcernKind { SOURCE_INTERPRETATION, TRANSLATION_STRUCTURE, ENGINE_EXECUTION, DISCOVERY_EXTRACTION }
@Serializable enum class ConcernState { SUSPECTED, CONFIRMED, DISPROVEN, RESOLVED, UNRESOLVED }
@Serializable data class EvidenceReference(val path: String, val sha256: String)
@Serializable data class TranslationConcern(val id: String, val kind: ConcernKind, val state: ConcernState,
    val claim: String, val reason: String, val reproduction: String, val nextAction: String,
    val resolutionEvidence: List<String> = emptyList())
/** Approval is explicit review evidence, not a signature or an automated certification service. */
@Serializable data class IndependentApproval(val reviewer: String, val reviewedAt: String,
    val reviewReference: String, val bundleSha256: String, val independentHumanReview: Boolean)
@Serializable data class TranslationEvidence(val oracleId: String, val definitionSha256: String,
    val argentumCommit: String, val author: String,
    val representation: RepresentationState = RepresentationState.COMPLETE,
    val fidelity: FidelityState = FidelityState.UNREVIEWED,
    val execution: ExecutionState = ExecutionState.NOT_TESTED,
    val source: List<EvidenceReference> = emptyList(), val requirements: List<EvidenceReference> = emptyList(),
    val tests: List<EvidenceReference> = emptyList(), val limitations: List<String> = emptyList(),
    val concerns: List<TranslationConcern> = emptyList(), val independentApproval: IndependentApproval? = null)
@Serializable data class TranslationAssurance(val card: String, val oracleId: String?,
    val representation: RepresentationState, val sourceFidelity: FidelityState, val engineExecution: ExecutionState,
    val qualification: QualificationState, val evidenceCurrent: Boolean, val reasons: List<String>,
    val evidence: TranslationEvidence?,
    val discoveryInterpretation: String = "SEPARATE_PER_OBJECTIVE_RESULTS_AND_UNSUPPORTED_RECORDS",
    val reviewKind: String = "SOURCE_FIRST_OR_AUTHOR_AGENT_REVIEW_NOT_INDEPENDENT_CERTIFICATION")

private val assuranceJson = Json { encodeDefaults = true }
private fun canonical(value: JsonElement): JsonElement = when(value) {
    is JsonObject -> JsonObject(value.toSortedMap().mapValues { canonical(it.value) })
    is JsonArray -> JsonArray(value.map(::canonical))
    else -> value
}
fun evidenceDigest(text: String): String = MessageDigest.getInstance("SHA-256")
    .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
fun definitionDigest(card: CardDefinition): String = evidenceDigest(canonical(Json.parseToJsonElement(CardExporter.exportToJson(card))).toString())
fun bundleDigest(evidence: TranslationEvidence): String = evidenceDigest(canonical(
    assuranceJson.encodeToJsonElement(evidence.copy(independentApproval=null))).toString())

/** Pure gate; reports research access without changing eligibility or mechanical predicates. */
fun assessTranslation(card: CardDefinition, evidence: TranslationEvidence?, pin: String = ARGENTUM_SHA): TranslationAssurance {
    val current = evidence != null && evidence.oracleId==card.oracleId && evidence.definitionSha256==definitionDigest(card) && evidence.argentumCommit==pin
    val concerns = evidence?.concerns.orEmpty().any { it.kind!=ConcernKind.DISCOVERY_EXTRACTION && it.state in setOf(ConcernState.SUSPECTED,ConcernState.CONFIRMED,ConcernState.UNRESOLVED) }
    val blocked = evidence?.representation?.let { it!=RepresentationState.COMPLETE }==true || concerns || evidence?.fidelity in setOf(FidelityState.SUSPECTED_DISCREPANCY,FidelityState.CONFIRMED_DISCREPANCY) || evidence?.execution==ExecutionState.FAILURE_OBSERVED
    val ready = current && !blocked && evidence!!.fidelity==FidelityState.SOURCE_REVIEWED && evidence.execution==ExecutionState.BOUNDED_EVIDENCE_PASSED &&
        listOf(evidence.source,evidence.requirements,evidence.tests).all { refs -> refs.isNotEmpty() && refs.all { it.path.isNotBlank() && it.sha256.matches(Regex("[0-9a-f]{64}")) } }
    val approval = evidence?.independentApproval
    val approved = ready && approval!=null && approval.independentHumanReview && approval.reviewer.isNotBlank() && approval.reviewer!=evidence!!.author &&
        approval.reviewedAt.isNotBlank() && approval.reviewReference.isNotBlank() && approval.bundleSha256==bundleDigest(evidence!!)
    val state = when {
        approval!=null && !approved -> QualificationState.WITHDRAWN
        blocked -> QualificationState.BLOCKED
        approved -> QualificationState.QUALIFIED
        else -> QualificationState.PROVISIONAL
    }
    return TranslationAssurance(card.name,card.oracleId,evidence?.representation ?: RepresentationState.COMPLETE,
        if(current) evidence!!.fidelity else FidelityState.UNREVIEWED,
        if(current) evidence!!.execution else ExecutionState.NOT_TESTED,state,current,
        buildList {
            if(!current) add("Evidence absent or stale for physical identity, definition or SDK pin")
            if(blocked) add("Open material discrepancy, incomplete representation or execution failure; inspect concern history")
            if(!approved) add("No current independent human approval; passing author scenarios cannot qualify a translation")
            if(approved) add("Explicit independent review recorded for this exact evidence bundle; bounded claims only")
        },evidence,reviewKind=if(approved) "RECORDED_INDEPENDENT_HUMAN_REVIEW" else "SOURCE_FIRST_OR_AUTHOR_AGENT_REVIEW_NOT_INDEPENDENT_CERTIFICATION")
}

internal object RecordedTranslationEvidence {
    private val records by lazy {
        val text=checkNotNull(javaClass.getResourceAsStream("/translation-assurance.json")) { "Missing translation assurance ledger" }.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val rows=assuranceJson.decodeFromString<List<TranslationEvidence>>(text)
        require(rows.map { it.oracleId }.distinct().size==rows.size) { "Duplicate assurance identities" }
        rows.associateBy { it.oracleId }
    }
    fun find(id: String?) = records[id]
}
