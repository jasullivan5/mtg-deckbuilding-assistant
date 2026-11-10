package dev.mtg.assistant.explorer

import dev.mtg.assistant.discovery.*

/** Describes the recorded relationship; admission is decided in the shared query. */
internal fun selfReturnExplanation(row: ResultEvidence,link: SelfReturnLink): PathwayExplanation {
    val movement=row.nativeOperation.obj()
    val placement=movement.text("placement")
    val requirements=buildList {
        add("The supplied resource must be this card's own original nontoken creature, not a created token or a generic reanimation target.")
        add("Recorded movement: ${link.sourceZone?.displayName ?: "an unresolved source zone"} → ${link.destinationZone?.displayName ?: "an unresolved destination zone"}. Native returning controller: ${when(link.destinationController) { "OWNER"->"the card's owner";"YOU"->"you";"OPPONENT"->"an opponent";"ANY"->"a permitted player";else->"unresolved" }}.")
        add("For this objective, you must own the original card in your graveyard and control the returning creature. The card must still be available when the return resolves.")
        if(link.context.kind=="ACTIVATED") add("Activate the source's ability from ${link.context.activeZones.joinToString { it.displayName }.ifEmpty { "an unresolved zone" }} and pay ${link.context.activationCost ?: "its full native costs"}. Any timing and activation restrictions still apply.")
        else add("Return opportunity: ${link.context.opportunity}")
        if(link.optional) add("The return or its resolution payment is optional; the relevant choice and payment must be made.")
        if(placement=="Tapped") add("The creature returns tapped.")
        if(placement=="TappedAndAttacking") add("The creature returns tapped and attacking; combat and defender requirements still apply.")
        if(movement?.get("entryCounters")?.toString()?.contains("PLUS_ONE_PLUS_ONE")==true) add("The return includes a +1/+1 entry counter, subject to applicable counter rules.")
        add("A separate legal mechanism would have to make the returned creature die. Returning it does not itself produce a death.")
        addAll(link.context.limits.map { it.explanation })
        addAll(link.limits.map { it.explanation })
    }
    val unknowns=buildList {
        if(row.verdict==Verdict.UNKNOWN) add("The return is not established as a usable input for this death objective. Its source, branch, condition or later-death restriction needs investigation; no missing supporting effect is assumed.")
        if(row.verdict==Verdict.NO_MATCH) add("This recorded movement does not meet the bounded intrinsic original-creature return criterion; it is not a whole-card rejection.")
        add("Continued availability is conditional: the original card would need to reach your graveyard again, the source's native opportunity and costs would need to be available, and no one-use or replacement limit could prevent the route.")
        add("No complete sacrifice/return cycle, unlimited recursion or guaranteed creature each turn has been demonstrated. Other instructions in this card may remain unsupported.")
    }
    val summary=when(row.verdict) {
        Verdict.MATCH -> "This creature can replenish itself by returning its own original card from your graveyard to the battlefield under your control. It supplies possible creature material for a later death; the return is not a death."
        Verdict.UNKNOWN -> "A native return route is visible, but its use as a creature input for a later qualifying death remains unresolved."
        Verdict.NO_MATCH -> "This movement does not establish intrinsic creature-input replenishment for the death objective."
    }
    return PathwayExplanation(summary,requirements,unknowns,mechanic(row),when(row.verdict) {
        Verdict.MATCH->"Supported self-return route; continued availability is conditional"
        Verdict.UNKNOWN->"Input replenishment needs investigation"
        Verdict.NO_MATCH->"Not an intrinsic creature-input return"
    })
}
