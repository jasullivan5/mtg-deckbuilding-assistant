package dev.mtg.assistant.explorer

import dev.mtg.assistant.EngineBuild
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.extensions.*
import kotlinx.serialization.Serializable

/** Presentation of existing evidence gates, not a new qualification or acceptance system. */
@Serializable enum class EvidenceState { ESTABLISHED, NOT_RECORDED, INCOMPLETE, KNOWN_PROBLEM, OUTDATED }
@Serializable data class RecordReference(val label: String, val path: String, val sha256: String? = null)
@Serializable data class EvidenceIndicator(
    val id: String, val label: String, val state: EvidenceState, val status: String,
    val scope: String, val explanation: String,
    val established: List<String> = emptyList(), val limits: List<String> = emptyList(),
    val nextSteps: List<String> = emptyList(), val references: List<RecordReference> = emptyList(),
    val recordedEngine: String? = null, val currentEngine: String = EngineBuild.revision,
    val historical: Boolean = false
)
@Serializable data class ResearchConcern(
    val title: String, val detail: String, val nextStep: String,
    val state: EvidenceState, val scope: String
)

fun evidenceIndicators(
    results: List<ResultEvidence>, assurance: TranslationAssurance?, scenarioReferences: List<String>,
    scope: String, runtimeRevision: String = EngineBuild.revision
): List<EvidenceIndicator> {
    val record = assurance?.evidence
    val current = assurance?.evidenceCurrent == true
    val stale = record != null && !current
    val historical = record != null && record.argentumCommit != runtimeRevision
    fun refs(items: List<EvidenceReference>, label: String) = items.map { RecordReference(label, it.path, it.sha256) }
    val recordedScope = "This record concerns the card's implementation. It does not identify which individual instructions in $scope were checked."
    val bindings = if (stale) listOf("The recorded identity, definition or engine binding is no longer applicable. A historical result is retained, not transferred to this build.") else emptyList()
    fun indicator(id: String, label: String, state: EvidenceState, status: String, explanation: String,
                  established: List<String> = emptyList(), limits: List<String> = emptyList(), next: List<String> = emptyList(),
                  references: List<RecordReference> = emptyList()) = EvidenceIndicator(
        id, label, state, status, recordedScope, explanation, established, limits + bindings, next, references,
        record?.argentumCommit, runtimeRevision, historical
    )
    val matched = results.filter { it.verdict == Verdict.MATCH }
    val unknown = results.filter { it.verdict == Verdict.UNKNOWN }
    val mechanic = when {
        matched.isNotEmpty() -> EvidenceIndicator("mechanic", "Mechanic identified", EvidenceState.ESTABLISHED, "Relevant part identified", scope,
            "Discovery identified a part of this ability that meets the objective's recorded checks.",
            established = matched.map { "${dev.mtg.assistant.explorer.mechanic(it)}: ${if (it.costOrEffect.endsWith("COST")) "a cost" else "an effect or dependency"} was accepted for this objective." }.distinct(),
            limits = listOf("This check covers those recorded parts, not every effect of the ability, rules fidelity, engine execution or deck value."),
            references = matched.map { RecordReference("Discovery check", it.abilityPath) } + matched.flatMap { it.sourceReferences }.distinct()
                .filter { it.startsWith("https://github.com/") || it.startsWith("src/main/") }
                .map { RecordReference("Authored source (representation, not a rules review)", it) }, currentEngine = runtimeRevision)
        unknown.isNotEmpty() -> EvidenceIndicator("mechanic", "Mechanic identified", EvidenceState.INCOMPLETE, "Relationship unresolved", scope,
            "An operation is represented, but discovery could not establish its relationship to this objective.",
            limits = listOf("An unresolved relationship is not an observed rules error or a failed engine scenario."),
            nextSteps = listOf("Inspect the specific affected-object, cost or condition questions below; retain unresolved checks until there is evidence."),
            references = unknown.map { RecordReference("Unresolved discovery check", it.abilityPath) }, currentEngine = runtimeRevision)
        results.isNotEmpty() -> EvidenceIndicator("mechanic", "Mechanic identified", EvidenceState.INCOMPLETE, "Does not qualify here", scope,
            "The recorded checks do not qualify this ability for this objective. Policy exclusions are separate from mechanical impossibility.",
            references = results.map { RecordReference("Objective decision", it.abilityPath) }, currentEngine = runtimeRevision)
        else -> EvidenceIndicator("mechanic", "Mechanic identified", EvidenceState.NOT_RECORDED, "No objective check", scope,
            "No objective-specific discovery result is available in this inspection.",
            nextSteps = listOf("Run discovery to inspect the represented relationships; absence of a result is not a negative finding."), currentEngine = runtimeRevision)
    }
    val source = when {
        stale -> indicator("source", "Source review", EvidenceState.OUTDATED, "Prior review not applicable",
            "An earlier source review exists, but it no longer applies to this card and build.",
            next = listOf("Review changed bindings and record a new source comparison."), references = refs(record!!.source, "Recorded source"))
        assurance?.sourceFidelity == FidelityState.CONFIRMED_DISCREPANCY -> indicator("source", "Source review", EvidenceState.KNOWN_PROBLEM, "Recorded disagreement",
            "The existing record identifies a confirmed disagreement between source rules and implementation.", next = listOf("Inspect the concern and its reproduction before relying on the affected behavior."), references = refs(record?.source.orEmpty(), "Recorded source"))
        assurance?.sourceFidelity == FidelityState.SUSPECTED_DISCREPANCY -> indicator("source", "Source review", EvidenceState.INCOMPLETE, "Review question open",
            "The record suspects a source disagreement; that is not yet a confirmed defect.", next = listOf("Compare the affected instruction against the retained source and record a scoped finding."), references = refs(record?.source.orEmpty(), "Recorded source"))
        current && assurance?.sourceFidelity == FidelityState.SOURCE_REVIEWED && record!!.source.isNotEmpty() -> indicator(
            "source", "Source review", EvidenceState.ESTABLISHED, "Author review recorded",
            "A developer's comparison against retained card sources is recorded for this implementation.",
            established = listOf("Source fidelity is recorded as source-reviewed by ${record.author}."),
            limits = listOf("This is author/source-led evidence, not an independent rules certification or an ability-specific approval.") +
                if (record.requirements.isEmpty()) listOf("No separately bound requirements record is supplied for this card.") else emptyList(),
            references = refs(record.source, "Recorded source") + refs(record.requirements, "Review requirements"))
        else -> indicator("source", "Source review", EvidenceState.NOT_RECORDED, "No comparison recorded",
            "No applicable registered source comparison establishes this interpretation's rules fidelity. Seeing authored text or verified artwork does not fill that gap.",
            next = listOf("Compare the exact ability with dated Oracle/ruling requirements and retain the review against this definition."))
    }
    val execution = when {
        stale -> indicator("execution", "Engine scenarios", EvidenceState.OUTDATED, "Prior evidence not applicable",
            "Historical execution evidence exists but does not pass the current assurance binding check.",
            limits = listOf("This is neither a fresh pass nor a fresh failure on this engine."), next = listOf("Revalidate the changed definition/engine with the relevant scenarios."), references = refs(record!!.tests, "Recorded execution evidence"))
        current && assurance?.engineExecution == ExecutionState.FAILURE_OBSERVED -> indicator("execution", "Engine scenarios", EvidenceState.KNOWN_PROBLEM, "Failure recorded",
            "A failure is recorded for the bound scenario scope.", limits = listOf("The recorded failure must be interpreted at its recorded engine revision; this inspection did not execute a new scenario."),
            next = listOf("Inspect the failing scenario, actual outcome and reproduction; record a scoped resolution if corrected."), references = refs(record?.tests.orEmpty(), "Execution record"))
        current && assurance?.engineExecution == ExecutionState.BOUNDED_EVIDENCE_PASSED && record!!.tests.isNotEmpty() -> indicator(
            "execution", "Engine scenarios", EvidenceState.ESTABLISHED, if (historical) "Historical pass recorded" else "Bounded pass recorded",
            "The recorded tests passed in the situations they covered. Opening this card has not played a new game.",
            established = listOf("Recorded execution: ${record.argentumCommit}."),
            limits = listOf("The record reports bounded passes, not whole-card correctness or proof of every interaction in this ability. This view does not identify the outcome of each test for this selected ability; a test-source link alone does not prove it ran.") +
                if (historical) listOf("The current engine has a different revision. The existing compatibility gate accepts the binding, but this remains historical execution, not a fresh run.") else emptyList(),
            references = refs(record.tests, "Execution record") + scenarioReferences.map { RecordReference("Scenario source (not an execution receipt)", it) })
        scenarioReferences.isNotEmpty() -> indicator("execution", "Engine scenarios", EvidenceState.INCOMPLETE, "Scenario files listed",
            "Scenario source references exist, but there is no applicable recorded passed/failed execution for this scope.",
            limits = listOf("A test filename does not prove the test ran or passed; missing execution evidence does not prove engine incapability."),
            next = listOf("Run the scoped cases and retain actual outcomes, definition and engine bindings."),
            references = scenarioReferences.map { RecordReference("Scenario source only", it) })
        else -> indicator("execution", "Engine scenarios", EvidenceState.NOT_RECORDED, "No run record",
            "No card-specific execution record is supplied here. This does not mean the engine cannot execute the ability.",
            next = listOf("Exercise the relevant costs, legal targets, resolution and negative controls, then retain a revision-bound receipt."))
    }
    val independent = when {
        assurance?.qualification == QualificationState.QUALIFIED && current && record?.independentApproval != null -> indicator(
            "independent", "Independent review", EvidenceState.ESTABLISHED, "Scoped approval recorded",
            "The existing assurance gate accepted an independent review of the exact evidence bundle.",
            established = listOf("Reviewer: ${record.independentApproval.reviewer}; recorded ${record.independentApproval.reviewedAt}."),
            limits = listOf("The approval covers its bound requirements and cases; it is not a universal card or strategic-value certificate."),
            references = listOf(RecordReference("Independent review", record.independentApproval.reviewReference)))
        assurance?.qualification == QualificationState.WITHDRAWN || record?.independentApproval != null -> indicator(
            "independent", "Independent review", EvidenceState.OUTDATED, "Approval not applicable",
            "A prior approval is present but is not accepted by the current qualification gate.", next = listOf("Inspect the changed or invalid approval/bundle bindings; obtain a new scoped independent review if appropriate."))
        else -> indicator("independent", "Independent review", EvidenceState.NOT_RECORDED, "No approval recorded",
            "No current independent approval is recorded. Author tests and source comparison remain useful evidence without becoming independent qualification.",
            next = listOf("An independent reviewer must assess the scoped source, requirements, implementation and execution evidence and approve the exact bundle."))
    }
    return listOf(mechanic, source, execution, independent)
}

fun materialConcerns(assurance: TranslationAssurance?): List<ResearchConcern> = assurance?.evidence?.concerns.orEmpty()
    .filter { it.state in setOf(ConcernState.CONFIRMED, ConcernState.SUSPECTED, ConcernState.UNRESOLVED) }
    .map { concern -> ResearchConcern(concern.claim, concern.reason,
        "Review the retained reproduction at its recorded engine version. Closing this concern requires reviewed, revision-bound resolution evidence; the original suggested action remains in the detailed record.",
        if (concern.state == ConcernState.CONFIRMED) EvidenceState.KNOWN_PROBLEM else EvidenceState.INCOMPLETE,
        "Card-level concern recorded on ${assurance!!.evidence!!.argentumCommit}; it is not automatically assigned to this ability or a fresh failure on this build.") }
