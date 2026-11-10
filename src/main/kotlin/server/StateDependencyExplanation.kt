package dev.mtg.assistant.explorer

import dev.mtg.assistant.discovery.*

internal fun stateCheckpointLabel(checkpoint: StateCheckpoint): String = when(checkpoint) {
    StateCheckpoint.RESOLUTION -> "During resolution"
    StateCheckpoint.TRIGGER_AND_RESOLUTION -> "When the ability would trigger and again on resolution"
    StateCheckpoint.ACTIVATION -> "When activating the ability"
}

internal fun stateDependencyExplanation(row: ResultEvidence, d: StateDependency): PathwayExplanation {
    val summary=when {
        row.verdict!=Verdict.MATCH -> "This recorded history check does not establish a supported benefit for the objective."
        d.incomingEvent==null -> "This ability checks whether a creature has already died this turn. It does not trigger from that death."
        else -> "This ability checks whether a creature has already died this turn. The history check is separate from the ability's trigger."
    }
    val outcome="When true: ${d.whenTrue.description}. Otherwise: ${d.whenFalse?.description ?: "this outcome is unavailable"}."
    val requirements=buildList {
        add("${stateCheckpointLabel(d.checkpoint)}: ${d.condition.description}.")
        d.incomingEvent?.let { add("Incoming trigger: ${it.trigger.description}.") }
        add("Native targets, costs, timing and additional requirements still apply.")
    }
    return PathwayExplanation(summary,requirements,
        row.decisions.filter { it.verdict==Verdict.UNKNOWN }.map { it.reason },mechanic(row),outcome)
}
