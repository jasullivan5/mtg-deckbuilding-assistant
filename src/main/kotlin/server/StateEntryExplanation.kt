package dev.mtg.assistant.explorer

import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.objectives.StateEntryEvidence

internal fun stateEntryExplanation(row: ResultEvidence, action: ActionEvidence?, entry: StateEntryEvidence): PathwayExplanation {
    val count = entry.count
    val cards = if (count == 1) "one card" else "$count cards"
    val trigger = action?.native.obj()?.get("trigger").obj()
    val triggerNote = if (trigger.text("type") == "ZoneChangeEvent" &&
        action?.prerequisites.orEmpty().any { "binding=SELF;" in it }) when {
        trigger.text("from") == "Battlefield" && trigger.text("to") == "Graveyard" -> "When this permanent dies, "
        trigger.text("to") == "Battlefield" -> "When this permanent enters, "
        else -> ""
    } else ""
    val movement = when (entry.route) {
        GraveyardRoute.MILL -> "mill $cards from your library"
        GraveyardRoute.DISCARD -> if (entry.discardsSource) "discard this creature card from your hand"
            else "discard $cards from your hand" + if (entry.random) " at random" else ""
        GraveyardRoute.SACRIFICE -> (if (entry.sourcePlayer == "OPPONENT") "an opponent sacrifices " else "sacrifice ") + when {
            entry.excludesSource && count == 1 -> "another creature"
            entry.excludesSource -> "$count creatures other than the source"
            count == 1 -> "a creature"
            else -> "$count creatures"
        }
    }
    val timing = when (row.costOrEffect) {
        "ACTIVATION_COST" -> " to activate this ability"
        "CASTING_COST" -> " as part of casting this spell"
        "OPTIONAL_CASTING_COST", "ALTERNATIVE_CASTING_COST" -> " if you use this casting payment"
        "RESOLUTION_COST" -> " as a payment during resolution"
        else -> ""
    }
    val conditional = row.verdict == Verdict.MATCH
    val summary = if (conditional)
        "${(triggerNote + movement).replaceFirstChar { it.uppercase() }}$timing."
    else row.decisions.first().reason
    val requirements = if (conditional) buildList {
        if (entry.sourcePlayer == "ANY" && entry.route != GraveyardRoute.SACRIFICE)
            add("This route must affect you. If it targets a player, you must be a legal target.")
        add(when (entry.route) {
            GraveyardRoute.MILL -> "At least one card milled must be a creature card in your graveyard."
            GraveyardRoute.DISCARD -> if (entry.discardsSource)
                "Meet the timing, mana costs and any target requirements. You pay by discarding this exact card before the ability resolves."
            else "An eligible creature card must be among the cards actually discarded."
            GraveyardRoute.SACRIFICE -> "An eligible creature you own must reach your graveyard as a creature card."
        })
        add("The card must stay in your graveyard through the checkpoint. Replacement effects can redirect the move.")
    } else emptyList()
    return PathwayExplanation(summary, requirements, emptyList(), mechanic(row),
        if (conditional) "Could supply the state" else if (row.verdict == Verdict.UNKNOWN) "Unresolved" else "Movement cannot supply the state")
}

internal fun stateEntryEventNote(entry: StateEntryEvidence): String =
    if (entry.deathPolicy?.verdict == Verdict.NO_MATCH && entry.deathComparison.verdict == Verdict.MATCH)
        "Death objective · This sacrifice is mechanically possible and excluded by the existing death policy."
    else if (entry.deathPolicy?.verdict == Verdict.UNKNOWN && entry.deathComparison.verdict == Verdict.MATCH)
        "Death objective · A qualifying death is mechanically possible. Admission under the death policy is unresolved."
    else "Death objective · ${entry.deathComparison.reason}"
