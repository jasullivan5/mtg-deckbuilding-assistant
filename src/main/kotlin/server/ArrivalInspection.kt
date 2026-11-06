package dev.mtg.assistant.explorer

import dev.mtg.assistant.discovery.MechanicalFact
import dev.mtg.assistant.discovery.ResultEvidence
import dev.mtg.assistant.discovery.Verdict
import dev.mtg.assistant.objectives.ConditionalArrival

internal fun inspectArrival(arrival: ConditionalArrival, fact: MechanicalFact?, row: ResultEvidence?): ArrivalInspection {
    val sources = row?.sourceReferences.orEmpty()
    val choice = fact?.sacrificeChoice
    if (choice == null || arrival.decision.verdict != Verdict.MATCH) return ArrivalInspection(arrival, sources = sources)
    val creatures = if (choice.count == 1) "a creature" else "${choice.count} creatures"
    val mechanism = when (fact.costOrEffect) {
        "ACTIVATION_COST" -> "Sacrifice $creatures you control to activate the ability."
        "CASTING_COST" -> "Sacrifice $creatures you control as part of casting the spell."
        "ALTERNATIVE_CASTING_COST", "OPTIONAL_CASTING_COST" ->
            "If you choose this casting route, sacrifice $creatures you control as part of casting the spell."
        "RESOLUTION_COST" -> "Sacrifice $creatures you control as a payment during resolution."
        else -> if (fact.circumstances?.mandatory == Verdict.MATCH)
            "The ability requires you to sacrifice $creatures you control as it resolves."
        else "Sacrifice $creatures you control during resolution when this instruction applies."
    }
    val prerequisites = buildList {
        add("You need the required creatures and must meet the ability’s timing and other costs or conditions. The sacrifice instruction does not create that material.")
        if (choice.excludesSource) add("The source is excluded from the sacrifice choices.")
        else if (fact.circumstances?.actionKind != "SPELL")
            add("The source may be chosen if it is on the battlefield and meets the sacrifice requirements.")
        add("A borrowed creature goes to its owner’s graveyard. A creature token is outside this objective, and an animated land may arrive as a land card.")
        add("A replacement such as a finality counter can send the creature elsewhere. The card must still be in your graveyard at the checkpoint.")
    }
    return ArrivalInspection(arrival, mechanism = mechanism, prerequisites = prerequisites, sources = sources)
}
