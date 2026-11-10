package dev.mtg.assistant.explorer

import dev.mtg.assistant.discovery.*

internal fun stateCheckpointLabel(checkpoint: StateCheckpoint): String = when(checkpoint) {
    StateCheckpoint.RESOLUTION -> "During resolution"
    StateCheckpoint.TRIGGER_AND_RESOLUTION -> "When the ability would trigger and again on resolution"
    StateCheckpoint.ACTIVATION -> "When activating the ability"
}

internal fun stateDependencyExplanation(row: ResultEvidence, d: StateDependency): PathwayExplanation {
    val summary=if(row.verdict==Verdict.MATCH) "This ability checks whether a creature has already died this turn. It does not trigger from that death."
        else "This recorded history check does not establish a supported benefit for the objective."
    val outcome="When true: ${d.whenTrue.description}. Otherwise: ${d.whenFalse?.description ?: "this outcome is unavailable"}."
    return PathwayExplanation(summary,listOf("${stateCheckpointLabel(d.checkpoint)}: ${d.condition.description}.",
        "Native targets, costs, timing and additional requirements still apply."),
        row.decisions.filter { it.verdict==Verdict.UNKNOWN }.map { it.reason },mechanic(row),outcome)
}
