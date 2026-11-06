package dev.mtg.assistant.objectives

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import dev.mtg.assistant.discovery.*
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

const val GRAVEYARD_CREATURE_OBJECTIVE = "stock-own-graveyard"

@Serializable data class StateEntryEvidence(
    val factId: String, val route: GraveyardRoute, val count: Int?, val sourcePlayer: String,
    val excludesSource: Boolean = false, val random: Boolean = false,
    val deathComparison: Decision, val deathPolicy: Decision? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val discardsSource: Boolean = false,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val sacrificesSource: Boolean = false
)

/** Compatibility applies to one possible object. Arrival type and persistence remain conditions. */
internal fun graveyardArrivalCompatibility(fact: MechanicalFact, source: Zone, controller: Controller? = null): Verdict =
    if (!fact.movesOriginalCard) Verdict.NO_MATCH else {
        val arrivalType = fact.cardEntry?.sourceCreatureAtArrival
        val movement = fact.compatible(FactConstraints(creature = arrivalType == null, token = TokenStatus.NONTOKEN,
            owner = Controller.YOU, controller = Controller.YOU, from = source, to = Zone.GRAVEYARD,
            sourceController = controller))
        all(listOf(movement, arrivalType ?: Verdict.MATCH))
    }

internal fun simpleCreatureChoice(fact: MechanicalFact, explicit: Boolean): Boolean {
    val filter = fact.objectFilter
    return (!explicit || CardPredicate.IsCreature in filter.cardPredicates) && filter.anyOf.isEmpty() &&
        filter.cardPredicates.all { it in setOf(CardPredicate.IsCreature, CardPredicate.IsNontoken, CardPredicate.IsPermanent) }
}

internal fun evaluateStateEntry(fact: MechanicalFact, route: GraveyardRoute): Decision {
    fun outcome(verdict: Verdict, reason: String) = Decision("own-graveyard-creature-arrival", verdict, "CONDITIONAL", reason)
    val source = when (route) {
        GraveyardRoute.SACRIFICE -> Zone.BATTLEFIELD
        GraveyardRoute.MILL -> Zone.LIBRARY
        GraveyardRoute.DISCARD -> Zone.HAND
    }
    val count = fact.sacrificeChoice?.count ?: fact.cardEntry?.count
    if (count != null && count <= 0) return outcome(Verdict.NO_MATCH, "This instruction moves zero cards.")
    val arrivalType = fact.cardEntry?.sourceCreatureAtArrival
    val compatibility = graveyardArrivalCompatibility(fact, source)
    if (compatibility == Verdict.NO_MATCH) return outcome(compatibility,
        when {
            arrivalType == Verdict.NO_MATCH -> "This instruction sacrifices the source itself. If it reaches the graveyard, it is a noncreature card there."
            fact.cardEntry?.discardsSource == true && CardPredicate.IsNoncreature in fact.objectFilter.cardPredicates ->
                "This cost discards the source card itself, which is a noncreature card."
            else -> "This movement's object restrictions, source zone or destination rule out supplying a creature card to your graveyard."
        })
    if (arrivalType == Verdict.UNKNOWN) return outcome(Verdict.UNKNOWN, "The source's creature-card type in the graveyard requires face-specific interpretation.")
    if (count == null || compatibility == Verdict.UNKNOWN || arrivalType == null && !simpleCreatureChoice(fact, explicit = source == Zone.BATTLEFIELD))
        return outcome(Verdict.UNKNOWN, "The native selection, affected player, quantity or nominal movement leaves the creature-card arrival unresolved.")
    return outcome(Verdict.MATCH, "A creature card you own could reach your graveyard through this movement and supply the state while it remains there.")
}

/** State admission uses only movement facts. The event comparison is attached after evaluation. */
internal fun graveyardEntries(facts: List<MechanicalFact>): List<EvaluatedFact> = facts.mapNotNull { fact ->
    val route = fact.cardEntry?.route ?: GraveyardRoute.SACRIFICE.takeIf { fact.kind == FactKind.SACRIFICE }
        ?: return@mapNotNull null
    EvaluatedFact(fact, listOf(evaluateStateEntry(fact, route)))
}

internal fun stateEntryEvidence(fact: MechanicalFact, death: EvaluatedPathway?): StateEntryEvidence {
    val choice = fact.sacrificeChoice
    val entry = fact.cardEntry
    val mechanical = death?.decisions?.filter { it.basis == "MECHANICAL" }?.let { all(it.map(Decision::verdict)) }
    val comparison = when {
        fact.sourceZone in setOf(Zone.LIBRARY, Zone.HAND) -> Decision("same-movement-death", Verdict.NO_MATCH, "MECHANICAL",
            "This move starts outside the battlefield, so it cannot be a creature death.")
        mechanical == Verdict.MATCH -> Decision("same-movement-death", mechanical, "MECHANICAL",
            "This same sacrifice can cause a death if the creature is nontoken, you control it, and it reaches a graveyard.")
        mechanical == Verdict.NO_MATCH && entry?.sourceCreatureAtArrival == Verdict.NO_MATCH ->
            Decision("same-movement-death", Verdict.UNKNOWN, "MECHANICAL",
                "A noncreature card could still die as a creature if an effect changed its battlefield type. This comparison does not model animation.")
        mechanical == Verdict.NO_MATCH -> Decision("same-movement-death", mechanical, "MECHANICAL",
            "This movement's battlefield restrictions do not meet the event criterion.")
        else -> Decision("same-movement-death", Verdict.UNKNOWN, "MECHANICAL",
            "A qualifying death from this same movement remains unresolved.")
    }
    return StateEntryEvidence(fact.id, entry?.route ?: GraveyardRoute.SACRIFICE, choice?.count ?: entry?.count,
        fact.sourcePlayer, choice?.excludesSource == true, entry?.random == true, comparison,
        death?.decisions?.firstOrNull { it.basis == "POLICY" }, discardsSource = entry?.discardsSource == true,
        sacrificesSource = entry?.sourceCreatureAtArrival != null)
}
