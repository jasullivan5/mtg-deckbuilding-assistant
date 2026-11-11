package dev.mtg.assistant.explorer

import dev.mtg.assistant.discovery.*
import kotlinx.serialization.json.*

/** Display vocabulary for the represented sacrifice filter; never an admission predicate. */
internal fun sacrificeObject(row: ResultEvidence): String {
    val node = row.nativeOperation.obj()
    if (node.text("type") in setOf("AtomSacrificeSelf", "SacrificeSelf")) return if (row.mechanicalCompatibility == Verdict.MATCH) "Source creature" else "Source permanent"
    val predicates = (node?.get("filter").obj()?.get("cardPredicates") as? JsonArray).orEmpty()
    val predicate = predicates.singleOrNull().obj()
    return when (predicate.text("type")) {
        "IsCreature" -> "Creature"
        "IsArtifact" -> "Artifact"
        "IsLand" -> "Land"
        "IsPermanent" -> "Permanent"
        "Or" -> when ((predicate?.get("predicates") as? JsonArray).orEmpty().map { it.obj().text("type") }.toSet()) {
            setOf("IsArtifact", "IsLand") -> "Artifact or land"
            setOf("IsCreature", "IsArtifact") -> "Creature or artifact"
            else -> "Object"
        }
        else -> "Object"
    }
}

/** Explains the recorded query, not a second acceptance test or a game-state evaluator. */
internal fun discoveryFields(results: List<ResultEvidence>, action: ActionEvidence?): List<EvidenceField> = buildList {
    val row = results.firstOrNull { it.verdict == Verdict.MATCH } ?: results.firstOrNull { it.verdict == Verdict.UNKNOWN } ?: results.firstOrNull() ?: return@buildList
    row.stateDependency?.let { d ->
        val explanation=stateDependencyExplanation(row,d)
        add(EvidenceField("Relationship",explanation.summary))
        add(EvidenceField("Required history",d.condition.description))
        add(EvidenceField("When checked",stateCheckpointLabel(d.checkpoint)))
        add(EvidenceField("When true",conditionalOutcomeDescription(d.whenTrue)))
        add(EvidenceField("Otherwise",d.whenFalse?.let(::conditionalOutcomeDescription) ?: "This outcome is unavailable."))
        if(row.verdict==Verdict.UNKNOWN) add(EvidenceField("Unresolved","The predicate, branch context or effect change is not fully interpreted."))
        return@buildList
    }
    row.selfReturn?.let { link ->
        add(EvidenceField("Relationship",contributionRelationship(row) ?: "Nonqualifying intrinsic return"))
        add(EvidenceField("Resource criterion","Must return the originating card's own original nontoken creature; not a token copy or a generic reanimation target"))
        add(EvidenceField("Return route",link.context.opportunity))
        add(EvidenceField("Zones and control","Original card in its owner's graveyard â†’ battlefield under our control; owner must be us"))
        add(EvidenceField("Continued availability","Conditional on a later graveyard arrival, native costs/opportunity and retained limits. No sacrifice/return cycle is proven."))
        if(link.limits.isNotEmpty()) add(EvidenceField("Limits",link.limits.joinToString { it.explanation }))
        if(row.verdict==Verdict.UNKNOWN) add(EvidenceField("Unresolved","Return association, conditions or future-death barriers are not fully established"))
        return@buildList
    }
    row.eventOutcome?.let { o ->
        add(EvidenceField("Relationship",contributionRelationship(row) ?: "Nonqualifying event response"))
        add(EvidenceField("Required event",incomingEvent(o)))
        add(EvidenceField("Possible outcome",producedOutcome(o)))
        add(EvidenceField("Scope","Same native ability and branch; not the cause of the initial death or a gameplay guarantee"))
        if(row.verdict==Verdict.UNKNOWN) add(EvidenceField("Unresolved","Event restrictions, branch or receiver need further interpretation"))
        return@buildList
    }
    row.stateEntry?.let {
        val explanation = stateEntryExplanation(row, action, it)
        add(EvidenceField("Movement", explanation.summary))
        add(EvidenceField("State contribution", explanation.outcome))
        add(EvidenceField("Event comparison", stateEntryEventNote(it)))
        return@buildList
    }
    val payoff = row.branch == Contribution.DEATH_ENHANCED_REMOVAL
    val mechanism = when (row.operation) {
        "SACRIFICE" -> "${sacrificeObject(row)} sacrifice${if (row.costOrEffect.endsWith("COST")) " as a cost" else " as an effect"}"
        "DESTROY" -> "Destruction"
        "DAMAGE_POTENTIALLY_LETHAL" -> "${if (payoff) "Death-related " else ""}damage"
        "TOUGHNESS_REDUCTION" -> "${if (payoff) "Death-enhanced " else ""}toughness reduction"
        "MOVE_TO_GRAVEYARD" -> "Battlefield-to-graveyard movement"
        "EXILE_REMOVAL" -> "Death-enabled exile"
        else -> mechanic(row)
    }
    add(EvidenceField("Mechanic found", mechanism))
    if (row.verdict == Verdict.MATCH) {
        add(EvidenceField("Why it qualifies", when {
            payoff && row.abilityPath.endsWith(".trigger") -> "Your nontoken creature dying can trigger this removal effect; the trigger does not cause that first death."
            payoff -> "A death can enable or improve this removal; your nontoken creature can supply that death."
            row.branch == Contribution.REANIMATE_OWN_CREATURE -> "An original creature card can return from your graveyard to your battlefield."
            row.branch == Contribution.CREATE_CREATURE_TOKEN -> "The effect can create at least one creature token under your control."
            row.operation == "SACRIFICE" -> "A nontoken creature you control can be sacrificed and die."
            row.operation == "DAMAGE_POTENTIALLY_LETHAL" -> "Lethal damage can cause a nontoken creature you control to die."
            row.operation == "TOUGHNESS_REDUCTION" -> "Reducing a nontoken creature you control to zero toughness can cause it to die."
            else -> "The effect can cause a nontoken creature you control to reach the graveyard."
        }))
        if (row.branch == Contribution.DIRECT_DEATH_GENERATOR)
            add(EvidenceField("Death outcome", "Structurally possible; conditional on reaching the graveyard"))
        if (payoff) add(EvidenceField("Relationship checked", "A matching creature death and a removal effect with a positive death dependency"))
        if (row.costOrEffect.endsWith("COST")) add(EvidenceField("Part checked", "Sacrifice payment; other effects are separate"))
    } else if (row.verdict == Verdict.UNKNOWN) {
        add(EvidenceField("Death relationship", if (payoff) "The death/removal dependency remains unresolved"
            else "A complete link from your nontoken creature, through this action, to a death is unresolved"))
    } else if (row.verdict == Verdict.NO_MATCH) {
        val ordinaryRemoval = row.decisions.any { it.basis == "POLICY" && it.verdict == Verdict.NO_MATCH && it.reason.startsWith("Policy: ordinary friendly-target removal") }
        add(EvidenceField("Why not included", if (row.operation == "DAMAGE_POTENTIALLY_LETHAL" && row.nativeOperation.obj()?.get("target").obj().text("type") in setOf("Controller", "PlayerRef"))
            "This damage affects a player, not a creature."
        else if (ordinaryRemoval && row.mechanicalCompatibility == Verdict.MATCH)
            "Ordinary removal is excluded when its only connection is choosing your own creature."
        else "The recorded object restrictions or objective policy did not qualify this effect."))
        add(EvidenceField("Decision scope", "This effect only; other payoffs may be outside the objective"))
    }
    if (row.operation == "SACRIFICE" && row.verdict == Verdict.MATCH && sacrificeObject(row) == "Artifact or land")
        add(EvidenceField("Accepted route", "Artifact-creature choice; land/creature overlap remains unresolved"))
    val context = row.circumstances
    if (row.operation == "SACRIFICE" && row.costOrEffect.endsWith("COST") && context?.actionKind == "ACTIVATED" && row.verdict != Verdict.NO_MATCH)
        add(EvidenceField("Repeat use", "Optional activation; repeat only when the source is available and costs, timing and limits permit."))
    val trigger = action?.native.obj()?.get("trigger").obj()
    val selfEntry = trigger.text("type") == "ZoneChangeEvent" && trigger.text("to") == "Battlefield" &&
        context?.timingAndConditions.orEmpty().any { "binding=SELF;" in it }
    if (selfEntry) add(EvidenceField("Ability conditions", "Source enters the battlefield${if (context?.mandatory == Verdict.MATCH) "; sacrifice required" else ""}"))
    else if (payoff && row.abilityPath.endsWith(".trigger") && trigger.text("from") == "Battlefield" && trigger.text("to") == "Graveyard")
        add(EvidenceField("Ability conditions", "A matching battlefield-to-graveyard death event triggers the removal effect"))
    if (row.verdict != Verdict.NO_MATCH && row.branch == Contribution.DIRECT_DEATH_GENERATOR && row.prerequisites.any { "no death-replacing effects are assumed" in it })
        add(EvidenceField("Assumed in play", "Legal resources and choices; no replacement that prevents the death"))
    val unresolved = results.filter { it.verdict != Verdict.NO_MATCH }.flatMap { it.decisions }.filter { it.verdict == Verdict.UNKNOWN }.map { when (it.stage) {
        "potential-creature-death" -> "Affected creature type"
        "controller-YOU" -> "Control of the affected creature"
        "token-NONTOKEN" -> "Whether the creature is nontoken"
        else -> "Additional conditions"
    } }.distinct()
    if (unresolved.isNotEmpty()) add(EvidenceField("Unresolved", unresolved.joinToString("; ")))
}
