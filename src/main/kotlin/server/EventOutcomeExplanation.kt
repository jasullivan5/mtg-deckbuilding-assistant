package dev.mtg.assistant.explorer

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import dev.mtg.assistant.discovery.*

/** Formats existing decisions and native relationships; never evaluates objective admission. */
fun contributionRelationship(row: ResultEvidence): String? = when(row.branch) {
    Contribution.DIRECT_DEATH_GENERATOR -> "Produces deaths".takeIf { row.verdict==Verdict.MATCH }
    Contribution.DEATH_ENHANCED_REMOVAL -> "Death-enhanced removal".takeIf { row.verdict==Verdict.MATCH }
    Contribution.DEATH_TRIGGERED_OUTCOME -> when(row.verdict) {
        Verdict.MATCH -> "Benefits from deaths"
        Verdict.UNKNOWN -> "Death response needs investigation"
        else -> null
    }
    else -> null
}

internal fun incomingEvent(o: EventOutcome): String {
    val event=o.event.trigger as? EventPattern.ZoneChangeEvent ?: return o.event.trigger.description
    val subject=when(o.event.binding) {
        TriggerBinding.SELF -> if(o.event.sourceIsCreature) "this creature" else "this source"
        TriggerBinding.OTHER -> "another ${event.filter.description.removePrefix("a ").removePrefix("an ")}"
        TriggerBinding.ATTACHED -> "the attached object (${event.filter.description})"
        else -> event.filter.description
    }
    return if(event.from==Zone.BATTLEFIELD && event.to==Zone.GRAVEYARD) "$subject dies"
        else "$subject moves from ${event.from?.displayName ?: "the recorded source zone"} to ${event.to?.displayName ?: "a permitted destination"}"
}
internal fun producedOutcome(o: EventOutcome): String {
    val receiver=when(o.recipient) { "YOU"->"you";"OPPONENT"->"an opponent";"ANY"->"a permitted player";else->"the unresolved receiver" }
    val amount=(o.quantity as? DynamicAmount.Fixed)?.amount?.toString() ?: o.quantity.description
    return when(o.kind) {
        OutcomeKind.DRAW_CARDS -> "$receiver can draw $amount card(s)${if(o.optional) " if the optional choice is made" else ""}"
        OutcomeKind.CREATE_TOKEN -> "$receiver can create $amount token(s): ${o.token?.templateReference ?: "native template"}${if(o.optional) " if the optional choice is made" else ""}"
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
            addAll(o.token?.characteristics.orEmpty().filterNot { it.contains("retained in native node") })
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
        (if(o.optional) listOf("The output is optional; it is not an automatic resource gain.") else emptyList()) + caps
    val unknowns=buildList {
        if(row.verdict==Verdict.UNKNOWN) add("The application has not established the full event/branch/receiver intersection; retain the exact native conditions below.")
        addAll(o.unresolved)
        if(row.interpretation.startsWith("PARTIAL")) add("Other instructions in this ability remain partial or unsupported. This output does not certify the whole ability.")
        if(o.kind==OutcomeKind.CREATE_TOKEN && o.token?.creature==Verdict.NO_MATCH) add("This is a noncreature token. Treasure is an artifact, not a creature token or mana already produced; using it later requires a separate legal action.")
        add("This is conditional structural discovery, not successful gameplay execution or a judgment of deck value.")
    }
    return PathwayExplanation("${if(row.verdict==Verdict.UNKNOWN) "Potential response" else "Event response"}: when ${incomingEvent(o)}, ${producedOutcome(o)}. This responds to the death; it does not cause the initial death.",
        requirements,unknowns,mechanic(row),when(row.verdict) { Verdict.MATCH->"Can benefit if these conditions hold";Verdict.UNKNOWN->"Needs investigation";else->"The event or output does not meet this objective" })
}
