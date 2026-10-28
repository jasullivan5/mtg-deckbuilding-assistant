package dev.mtg.assistant.discovery

import com.wingedsheep.sdk.model.CardDefinition
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable data class Circumstances(val actionKind: String, val mandatory: Verdict,
    val deferredTiming: Boolean, val delayedTrigger: Boolean, val timingAndConditions: List<String>,
    val sourceMustRemainOnBattlefield: Boolean, val objectRestrictions: List<String>,
    val repeatability: String, val resourceAssumptions: String = "Legal choices/resources must exist; no gameplay outcome guaranteed")

const val ARGENTUM_SHA = "ebf5bf2e9fbb80d13a556d75491dd462e10c206f"
val reportJson = Json { encodeDefaults = true }
val nativeJson = Json { encodeDefaults = false; prettyPrint = false }
@Serializable enum class SupportStatus { SUPPORTED, PARTIALLY_SUPPORTED, UNSUPPORTED }
@Serializable data class StructuralSupport(val card: String,val face: String,val path: String,val nativeType: String,
    val status: SupportStatus,val reason: String,val actionId: String)
@Serializable enum class Contribution { DIRECT_DEATH_GENERATOR, DEATH_ENHANCED_REMOVAL, REANIMATE_OWN_CREATURE, CREATE_CREATURE_TOKEN }
@Serializable data class ActionEvidence(val id: String, val card: String, val face: String, val path: String,
    val prerequisites: List<String>, val native: JsonElement, val associatedActionId: String? = null)
@Serializable data class Unsupported(val card: String, val face: String, val path: String, val nativeType: String,
    val reason: String, val actionId: String)
data class Pathway(val card: CardDefinition, val face: String, val actionId: String, val path: String,
    val operation: String, val origin: String, val scope: ObjectScope,
    val branch: Contribution = Contribution.DIRECT_DEATH_GENERATOR,
    val prerequisites: List<String> = emptyList(), val native: JsonElement,
    val incidentalRemoval: Boolean = false, val dependency: String? = null,
    val dependencyVerdict: Verdict = Verdict.UNKNOWN, val relationship: String = "UNRESOLVED", val operationCompatibility: Verdict = Verdict.MATCH,
    val circumstances: Circumstances? = null, val opposingRemovalAssociation: Verdict = Verdict.NO_MATCH, val factId: String? = null)
@Serializable data class EligibilityEvidence(val card: String, val format: Verdict, val deck: Verdict,
    val identity: List<String>, val reason: String, val provenance: dev.mtg.assistant.extensions.Provenance? = null,
    val effectiveMetadata: dev.mtg.assistant.eligibility.EffectiveEligibility? = null)
@Serializable data class ResultEvidence(val card: String, val identifier: String, val face: String,
    val actionId: String, val abilityPath: String, val branch: Contribution, val verdict: Verdict,
    val operation: String, val costOrEffect: String, val affectedScope: String,
    val prerequisites: List<String>, val dependency: String?, val nativeOperation: JsonElement,
    val decisions: List<Decision>, val sourceReferences: List<String>,
    val commanderLegality: Verdict, val deckEligibility: Verdict,
    val interpretation: String = "STRUCTURAL_POTENTIAL_NOT_GAMEPLAY_VERIFIED",
    val contributionClassification: Contribution? = null,
    val controllerCompatibility: Verdict = Verdict.UNKNOWN,
    val nontokenCompatibility: Verdict = Verdict.UNKNOWN,
    val provenance: dev.mtg.assistant.extensions.Provenance? = null,
    val enablingRelationship: String = "UNRESOLVED",
    val mechanicalCompatibility: Verdict? = null, val policyAcceptance: Verdict? = null,
    val mechanicalCapability: String? = null, val circumstances: Circumstances? = null,
    val strategicDesirability: String = "NOT_EVALUATED", val localSupport: SupportStatus? = null)
@Serializable data class DiscoveryReport(val argentumCommit: String = ARGENTUM_SHA, val objective: String,
    val commander: String, val allowedIdentity: List<String>, val conventionalQuery: String?,
    val counts: Map<String, Int>, val eligibility: List<EligibilityEvidence>, val results: List<ResultEvidence>,
    val actions: List<ActionEvidence>, val unsupported: List<Unsupported>, val limitations: List<String>,
    val structuralSupport: List<StructuralSupport> = emptyList(),
    val facts: List<MechanicalFact> = emptyList(), val mechanicalInputCards: List<String>? = null,
    val translationAssurance: List<dev.mtg.assistant.extensions.TranslationAssurance> = emptyList())

/** Keyword/self alternative payments link to the card script, whose spell action owns resolution. */
fun associatedSpellAction(actions: Map<String,ActionEvidence>,actionId: String): String? =
    actions[actionId]?.associatedActionId?.let { if(it.endsWith("|script")) "$it.spell" else it }
