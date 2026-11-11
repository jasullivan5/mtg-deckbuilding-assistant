package dev.mtg.assistant.explorer

import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import dev.mtg.assistant.discovery.*

/** The controller reference is rebound to the iterated player by the native executor. */
internal fun conditionalOutcomeDescription(effect: Effect): String {
    val each = effect as? ForEachEffect ?: return effect.description
    val players = (each.space as? IterationSpace.Players)?.players
    val sacrifice = each.body as? ForceSacrificeEffect
    return if (players in setOf(Player.Each, Player.EachOpponent) && sacrifice?.target == EffectTarget.Controller)
        sacrifice.copy(target = EffectTarget.PlayerRef(checkNotNull(players))).description
    else effect.description
}

internal fun stateCheckpointLabel(checkpoint: StateCheckpoint): String = when(checkpoint) {
    StateCheckpoint.RESOLUTION -> "During resolution"
    StateCheckpoint.TRIGGER_AND_RESOLUTION -> "When the ability would trigger and again on resolution"
    StateCheckpoint.ACTIVATION -> "When activating the ability"
}

internal fun stateDependencyExplanation(row: ResultEvidence, d: StateDependency): PathwayExplanation {
    val summary=when {
        row.verdict!=Verdict.MATCH -> "The effect of this history check is unresolved or outside the search."
        d.incomingEvent==null -> "This ability checks whether a creature has already died this turn. It does not trigger from that death."
        else -> "This ability checks whether a creature has already died this turn. The history check is separate from the ability's trigger."
    }
    val outcome="When true: ${conditionalOutcomeDescription(d.whenTrue)}. Otherwise: ${d.whenFalse?.let(::conditionalOutcomeDescription) ?: "this outcome is unavailable"}."
    val requirements=buildList {
        add("${stateCheckpointLabel(d.checkpoint)}: ${d.condition.description}.")
        d.incomingEvent?.let { add("Incoming trigger: ${it.trigger.description}.") }
        add("Native targets, costs, timing and additional requirements still apply.")
    }
    return PathwayExplanation(summary,requirements,
        row.decisions.filter { it.verdict==Verdict.UNKNOWN }.map { it.reason },mechanic(row),outcome)
}
