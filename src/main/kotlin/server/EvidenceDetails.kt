package dev.mtg.assistant.explorer

import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.extensions.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.net.URI

@Serializable data class EvidenceField(val label: String, val value: String, val reference: RecordReference? = null)

/** Describes an existing hash-bound snapshot, never a newly performed comparison. */
internal fun recordedSource(reference: EvidenceReference, oracleId: String): EvidenceField {
    val ref = RecordReference("Retained source", reference.path, reference.sha256)
    fun fallback() = EvidenceField("Rules source", "Retained file; provider not identified", ref)
    val relative = reference.path.removePrefix("src/test/resources/")
    if (!relative.matches(Regex("cards/[a-z0-9-]+-(source|rulings)\\.json")) || reference.hashFormat != EVIDENCE_TEXT_SHA256) return fallback()
    val text = EvidenceField::class.java.getResourceAsStream("/retained-evidence/$relative")
        ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: return fallback()
    if (evidenceTextDigest(text) != reference.sha256) return fallback()
    val source = runCatching { reportJson.parseToJsonElement(text).jsonObject }.getOrNull() ?: return fallback()
    val uri = runCatching { URI(source.text("uri")) }.getOrNull()
    if (source.text("object") == "card" && source.text("oracle_id") == oracleId &&
        uri?.scheme == "https" && uri.host == "api.scryfall.com" && source["oracle_text"] is JsonPrimitive)
        return EvidenceField("Rules source", "Scryfall Oracle text · retained snapshot", ref)
    val rulings = (source["data"] as? JsonArray).orEmpty()
    if (source.text("object") == "list" && rulings.isNotEmpty() && rulings.all {
        it.obj().text("oracle_id") == oracleId && it.obj().text("source") == "wotc"
    }) return EvidenceField("Rules source", "Wizards rulings · retained snapshot", ref)
    return fallback()
}

internal fun evidenceFields(item: EvidenceIndicator, record: TranslationEvidence?, results: List<ResultEvidence>, action: ActionEvidence? = null): List<EvidenceField> = when (item.id) {
    "mechanic" -> discoveryFields(results, action)
    "source" -> if (record == null || item.state == EvidenceState.NOT_RECORDED) emptyList() else buildList {
        add(EvidenceField("Recorded author", if (record.author == "implementing-agent") "AI agent · name not recorded" else "Not identified"))
        // The contract has an implementation author, not a separate comparison-reviewer field.
        add(EvidenceField("Comparison reviewer", "Not captured separately by the card-review format"))
        addAll(record.source.map { recordedSource(it, record.oracleId) })
        add(EvidenceField("Abilities checked", "The card-review format has no ability-by-ability links"))
        add(EvidenceField("Comparison date", "Not captured by the card-review format"))
        if (item.state == EvidenceState.OUTDATED) add(EvidenceField("Applies to this build", "No · card or engine binding changed"))
    }
    "execution" -> if (item.state == EvidenceState.NOT_RECORDED) emptyList() else buildList {
        add(EvidenceField("Engine version", if (item.historical) "Different from the current version" else "Same as the current version"))
        add(EvidenceField("Ability coverage", "Individual interactions not linked in this card record"))
        if (item.state == EvidenceState.OUTDATED) {
            add(EvidenceField("Earlier outcome", when (record?.execution) {
                ExecutionState.BOUNDED_EVIDENCE_PASSED -> "Selected scenarios passed"
                ExecutionState.FAILURE_OBSERVED -> "Failure recorded"
                else -> "No result recorded"
            }))
            add(EvidenceField("Current outcome", "Not established by this evidence"))
        }
    }
    "independent" -> if (item.state == EvidenceState.ESTABLISHED && record?.independentApproval != null) listOf(
        EvidenceField("Reviewer", record.independentApproval.reviewer),
        EvidenceField("Reviewed on", record.independentApproval.reviewedAt),
        EvidenceField("Coverage", "Recorded requirements and cases")
    ) else emptyList()
    else -> emptyList()
}

internal fun discoveryStatus(results: List<ResultEvidence>): String {
    val match = results.firstOrNull { it.verdict == Verdict.MATCH }
    if (match != null) return when (match.operation) {
        "SACRIFICE" -> if (match.costOrEffect.endsWith("COST")) "${sacrificeObject(match)} sacrifice cost identified" else "${sacrificeObject(match)} sacrifice effect identified"
        "DESTROY" -> "Destruction identified"
        "DAMAGE_POTENTIALLY_LETHAL" -> "Death-related damage identified"
        else -> "${mechanic(match)} identified"
    }
    val unknown = results.filter { it.verdict == Verdict.UNKNOWN }.flatMap { it.decisions }.filter { it.verdict == Verdict.UNKNOWN }
    return when {
        unknown.any { it.stage == "potential-creature-death" } -> "Affected creature type unresolved"
        unknown.any { it.stage == "controller-YOU" } -> "Creature control unresolved"
        unknown.any { it.stage == "token-NONTOKEN" } -> "Nontoken status unresolved"
        unknown.isNotEmpty() -> "Conditions unresolved"
        results.any { it.verdict == Verdict.UNKNOWN } -> "Relationship unresolved"
        results.isEmpty() -> "No discovery result"
        else -> "No qualifying relationship"
    }
}
