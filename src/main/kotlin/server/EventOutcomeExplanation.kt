package dev.mtg.assistant.explorer

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import dev.mtg.assistant.discovery.*

/** Formats existing decisions and native relationships; never evaluates objective admission. */
fun contributionRelationship(row: ResultEvidence): String? = when(row.branch) {
    Contribution.STATE_BENEFICIARY -> if(row.verdict==Verdict.MATCH) "Depends on death history" else null
    Contribution.DIRECT_DEATH_GENERATOR -> "Produces deaths".takeIf { row.verdict==Verdict.MATCH }
    Contribution.DEATH_ENHANCED_REMOVAL -> "Death-enhanced removal".takeIf { row.verdict==Verdict.MATCH }
    Contribution.INTRINSIC_CREATURE_INPUT -> when(row.verdict) {
        Verdict.MATCH -> "Supplies creature inputs"
        Verdict.UNKNOWN -> "Creature input needs investigation"
        else -> null
    }
    Contribution.DEATH_TRIGGERED_OUTCOME -> when(row.verdict) {
        Verdict.MATCH -> "Responds to deaths"
        Verdict.UNKNOWN -> "Death response needs investigation"
        else -> null
    }
    else -> null
}

/** Plain wording for the supported simple filters; complex filters retain the native description. */
private fun eventObject(filter: GameObjectFilter): String {
    val simple=setOf(CardPredicate.IsCreature,CardPredicate.IsNontoken,CardPredicate.IsToken)
    if(filter.anyOf.isNotEmpty() || filter.statePredicates.isNotEmpty() ||
        CardPredicate.IsCreature !in filter.cardPredicates || filter.cardPredicates.any { it !in simple }) return filter.description
    val relation=when(filter.controllerPredicate) {
        null,ControllerPredicate.ControlledByAny -> ""
        ControllerPredicate.ControlledByYou -> " you control"
        ControllerPredicate.ControlledByOpponent -> " an opponent controls"
        ControllerPredicate.OwnedByYou -> " you own"
        ControllerPredicate.OwnedByOpponent -> " an opponent owns"
        else -> return filter.description
    }
    val token=when {
        CardPredicate.IsNontoken in filter.cardPredicates && CardPredicate.IsToken in filter.cardPredicates -> return filter.description
        CardPredicate.IsNontoken in filter.cardPredicates -> "nontoken "
        CardPredicate.IsToken in filter.cardPredicates -> "token "
        else -> ""
    }
    return "${token}creature$relation"
}
internal fun incomingEvent(o: EventOutcome): String {
    val event=o.event.trigger as? EventPattern.ZoneChangeEvent ?: return o.event.trigger.description
    val subject=when(o.event.binding) {
        TriggerBinding.SELF -> if(o.event.sourceIsCreature) "this creature" else "this source"
        TriggerBinding.OTHER -> "another ${eventObject(event.filter).removePrefix("a ").removePrefix("an ")}"
        TriggerBinding.ATTACHED -> "the attached object (${event.filter.description})"
        else -> eventObject(event.filter)
    }
    return if(event.from==Zone.BATTLEFIELD && event.to==Zone.GRAVEYARD) "$subject dies"
        else "$subject moves from ${event.from?.displayName ?: "the recorded source zone"} to ${event.to?.displayName ?: "a permitted destination"}"
}
internal fun producedOutcome(o: EventOutcome): String {
    val nativePlayer=o.native.obj()?.get("target").obj()?.get("player").obj().text("type")
    val receiver=o.playerBinding?.description ?: when(nativePlayer) { "Each"->"each player";"EachOpponent"->"each opponent";else->when(o.recipient) { "YOU"->"you";"OPPONENT"->"an opponent";"ANY"->"a permitted player";else->"the unresolved receiver" } }
    val amount=(o.quantity as? DynamicAmount.Fixed)?.amount?.toString() ?: o.quantity.description
    return when(o.kind) {
        OutcomeKind.DRAW_CARDS -> "$receiver can draw $amount card(s)${if(o.optional) " if the optional choice is made" else ""}"
        OutcomeKind.CREATE_TOKEN -> {
            val template=o.token?.templateReference.orEmpty()
            val output=when {
                template.startsWith("PredefinedTokens:") -> "${template.removePrefix("PredefinedTokens:")} token(s)"
                template.startsWith("Native creature template:") -> "${template.removePrefix("Native creature template:").trim()} creature token(s)"
                else -> "token(s) using the recorded native template or copy"
            }
            "$receiver can create $amount $output${if(o.optional) " if the optional choice is made" else ""}"
        }
        OutcomeKind.FORCED_SACRIFICE -> {
            val instruction = nativeJson.decodeFromJsonElement(Effect.serializer(), o.native) as ForceSacrificeEffect
            val eligible = instruction.filter.description.removePrefix("a ").removePrefix("an ")
            val objects = if (o.quantity == DynamicAmount.Fixed(1)) "one $eligible" else "$amount from the eligible objects ($eligible)"
            val verb = if (receiver == "you") "sacrifice" else "sacrifices"
            "$receiver $verb $objects they control, if able".replace("you sacrifice $objects they control", "you sacrifice $objects you control")
        }
        OutcomeKind.LOSE_LIFE -> "$receiver ${if (receiver == "you") "lose" else "loses"} $amount life"
        OutcomeKind.GAIN_LIFE -> "$receiver ${if (receiver == "you") "gain" else "gains"} $amount life"
        OutcomeKind.DEAL_PLAYER_DAMAGE -> "the recorded source deals $amount damage to $receiver"
    }
}
internal fun eventOutcomeExplanation(row: ResultEvidence, o: EventOutcome): PathwayExplanation {
    val ability=o.event.nativeAbility.obj()
    val caps=buildList {
        if(ability?.get("triggersOnce")?.toString()=="true") add("This ability triggers only once during this source's battlefield lifetime; its allowance must remain.")
        if(ability?.get("effectOncePerTurn")?.toString()=="true") add("The action can be taken only once each turn. Its resolution budget must remain; declining does not spend it.")
        if(ability?.get("oncePerTurn")?.toString()=="true") add("This ability triggers at most once each turn; its trigger allowance must remain.")
        ability.text("triggersPerTurn")?.let { add("This ability triggers at most $it times each turn; its trigger allowance must remain.") }
        if(ability?.get("interveningIf")!=null) add("The source's intervening condition must hold both when it triggers and when it resolves. This condition is not fully interpreted; read its rules paragraph above.")
        if(ability?.get("triggerRestriction")!=null) add("The source's additional trigger-time restriction must hold. It is not fully interpreted and is not a resolution-time recheck.")
        if(o.kind==OutcomeKind.CREATE_TOKEN) {
            addAll(o.token?.characteristics.orEmpty().filterNot { it.contains("retained in native node") }.map { it.removePrefix("Native ").replace("type line=", "Token type: ").replace("template=", "Token name: ") })
            if(o.native.obj()?.get("tapped")?.toString()=="true") add("The created token enters tapped.")
        }
    }
    val requirements=listOf("Required event: ${incomingEvent(o)}.",
        "For this objective, a nontoken creature you control must actually reach the graveyard from the battlefield. The native trigger may also accept other deaths.",
        "This ability must be active in its native zones at the trigger checkpoint: ${o.event.activeZones.joinToString { it.displayName }}. Last-known information can permit a death trigger even though its source has left the battlefield.",
        when(o.event.binding) {
            TriggerBinding.SELF -> "This source itself must be the qualifying dying creature. It need not remain on the battlefield for the ability to resolve."
            TriggerBinding.OTHER -> "The dying creature must be another object, not this source."
            TriggerBinding.ATTACHED -> "The trigger refers to the attached object; the attachment relationship is not fully interpreted."
            else -> "The dying object must meet the native trigger's printed restrictions."
        },
        "Native trigger conditions, turn caps, targets, choices and positive quantity must permit this branch. Replacements, trigger suppression and failed resolution can prevent the outcome.") +
        (if(o.optional) listOf("The operation requires the optional choice to be made.") else emptyList()) +
        (if(o.kind==OutcomeKind.FORCED_SACRIFICE) listOf("Any resulting creature death happens later and depends on the chosen permanent and replacement effects.") else emptyList()) + caps
    val unknowns=buildList {
        if(row.verdict==Verdict.UNKNOWN) add("The application has not established the full event/branch/receiver intersection; retain the exact native conditions below.")
        addAll(o.unresolved)
        if(row.interpretation.startsWith("PARTIAL")) add("Other instructions in this ability remain partial or unsupported. This output does not certify the whole ability.")
        if(o.kind==OutcomeKind.CREATE_TOKEN && o.token?.creature==Verdict.NO_MATCH) add("This is a noncreature token. Treasure is an artifact, not a creature token or mana already produced; using it later requires a separate legal action.")
        add("This is conditional structural discovery, not successful gameplay execution or a judgment of deck value.")
    }
    return PathwayExplanation("${if(row.verdict==Verdict.UNKNOWN) "Potential response" else "Event response"}: when ${incomingEvent(o)}, ${producedOutcome(o)}. This responds to the death; it does not cause the initial death.",
        requirements,unknowns,mechanic(row),when(row.verdict) { Verdict.MATCH->"Can occur if these conditions hold";Verdict.UNKNOWN->"Needs investigation";else->"The event or output does not meet this objective" })
}
