package dev.mtg.assistant.audit

import dev.mtg.assistant.discovery.*
import kotlinx.serialization.json.*
import java.util.IdentityHashMap

private val snapshotReader = Json { ignoreUnknownKeys = true }

/** Offline comparison reads our recorded decisions, never executes an archived SDK structure. */
internal data class DiscoverySnapshot(
    val report: DiscoveryReport,
    val original: JsonObject,
    val resultNodes: Map<ResultEvidence, JsonObject>
)

private val snapshotReader = Json { ignoreUnknownKeys = true }
private val resultCoreFields = setOf(
    "card", "identifier", "face", "actionId", "abilityPath", "branch", "verdict", "operation",
    "costOrEffect", "affectedScope", "prerequisites", "dependency", "nativeOperation", "decisions",
    "sourceReferences", "commanderLegality", "deckEligibility", "interpretation", "contributionClassification",
    "controllerCompatibility", "nontokenCompatibility", "provenance", "enablingRelationship",
    "mechanicalCompatibility", "policyAcceptance", "mechanicalCapability", "circumstances",
    "strategicDesirability", "localSupport"
)

internal fun recordedResultCore(value: JsonElement): ResultEvidence = snapshotReader.decodeFromJsonElement(
    JsonObject(value.jsonObject.filterKeys { it in resultCoreFields })
)

internal fun recordedPayloadChanged(before: JsonObject, after: JsonObject): Boolean =
    (before.keys + after.keys).any { it !in resultCoreFields && before[it] != after[it] }

internal fun discoverySnapshot(original: JsonObject): DiscoverySnapshot {
    require(!original["argentumCommit"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()) {
        "A historical discovery snapshot must retain its recorded engine revision"
    }
    val nodes = original.getValue("results").jsonArray.map { it.jsonObject }
    val results = nodes.map(::recordedResultCore)
    // Keep identities even when two records have equal core fields but different opaque payloads.
    val raw = IdentityHashMap<ResultEvidence, JsonObject>()
    results.zip(nodes).forEach { (result, node) -> raw[result] = node }
    val eligibility = original.getValue("eligibility").jsonArray.map { value ->
        JsonObject(value.jsonObject.filterKeys { it in setOf("card", "format", "deck", "identity", "reason") })
    }
    val coreFields = setOf("argentumCommit", "objective", "commander", "allowedIdentity", "conventionalQuery",
        "counts", "unsupported", "limitations", "structuralSupport", "mechanicalInputCards")
    val core = JsonObject(original.filterKeys { it in coreFields } + mapOf(
        "results" to JsonArray(emptyList()),
        "eligibility" to JsonArray(eligibility), "actions" to JsonArray(emptyList())
    ))
    val report = snapshotReader.decodeFromJsonElement<DiscoveryReport>(core)
    // Use the same result instances in the report and identity map.
    return DiscoverySnapshot(report.copy(results = results), original, raw)
}

internal fun diffSnapshots(before: JsonObject, after: JsonObject,
    oldCoverage: CoverageReport? = null, newCoverage: CoverageReport? = null): ChangeReport {
    val old = discoverySnapshot(before)
    val current = discoverySnapshot(after)
    return diffRecorded(old.report, current.report, oldCoverage, newCoverage, old.resultNodes, current.resultNodes)
}
