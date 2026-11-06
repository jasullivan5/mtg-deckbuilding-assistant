package dev.mtg.assistant.objectives

import com.wingedsheep.sdk.core.Zone
import dev.mtg.assistant.discovery.*
import kotlinx.serialization.Serializable

const val GRAVEYARD_CREATURE_CRITERION = "I have at least one creature card in my graveyard"

@Serializable data class ConditionalArrival(
    val card: String, val actionId: String, val path: String, val factId: String?,
    val decision: Decision
)

/**
 * Interpret one accepted event route's nominal movement against the independent checkpoint
 * criterion. The compatibility query applies to the departing object. The card type and
 * persistence of the arrival remain gameplay prerequisites.
 */
internal fun creatureCardArrival(fact: MechanicalFact?): Decision {
    fun decision(verdict: Verdict, reason: String) =
        Decision("owned-creature-card-at-graveyard-checkpoint", verdict, "CONDITIONAL", reason)
    if (fact?.sacrificeChoice == null) return decision(Verdict.UNKNOWN,
        "This sacrifice selection or destination is outside the supported fixed-count choice.")
    if (fact.sourceZone == null || fact.destinationZone == null) return decision(Verdict.UNKNOWN,
        "The source or nominal destination of the sacrifice is missing from the native fact.")
    val compatibility = graveyardArrivalCompatibility(fact, Zone.BATTLEFIELD, Controller.YOU)
    if (compatibility == Verdict.NO_MATCH) return decision(compatibility,
        "The represented selection or destination rules out a nontoken creature you control and own reaching your graveyard.")

    if (compatibility == Verdict.UNKNOWN || !simpleCreatureChoice(fact, explicit = true)) return decision(Verdict.UNKNOWN,
        "The native selection does not establish a supported choice of a creature that could also be a creature card in the graveyard.")
    return decision(Verdict.MATCH,
        "A nontoken creature you control can die through this sacrifice. If you also own it, it reaches your graveyard as a creature card, and it stays there through the checkpoint, the arrival can supply the graveyard criterion.")
}

internal fun conditionalSacrificeArrivals(
    deathPaths: List<EvaluatedPathway>, facts: List<MechanicalFact>
): List<ConditionalArrival> {
    val byId = facts.associateBy { it.id }
    return deathPaths.mapNotNull { evaluated ->
        val path = evaluated.pathway
        if (path.operation != "SACRIFICE" || evaluated.verdict == Verdict.NO_MATCH) return@mapNotNull null
        val decision = if (evaluated.verdict == Verdict.UNKNOWN)
            Decision("death-prerequisite", Verdict.UNKNOWN, "CONDITIONAL",
                "The death pathway or its admission under the objective policy remains unresolved.")
        else creatureCardArrival(path.factId?.let(byId::get))
        ConditionalArrival(path.card.name, path.actionId, path.path, path.factId, decision)
    }
}
