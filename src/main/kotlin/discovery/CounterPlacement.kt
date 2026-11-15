package dev.mtg.assistant.discovery

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

const val COUNTER_OBJECTIVE = "place-plus-one-counters"

@Serializable enum class PlacementTiming { RESOLUTION, ENTRY }

@Serializable data class CounterAction(
    val card: String, val face: String, val actionId: String, val path: String,
    val native: JsonElement, val description: String, val prerequisites: List<String>
)

@Serializable data class CounterRecipient(
    val filter: GameObjectFilter = GameObjectFilter.Any,
    val controller: String = "ANY",
    val sourceCreature: Boolean? = null,
    val battlefield: Boolean = true,
    val unresolved: String? = null
) {
    fun compatible(requirement: Requirements) = ObjectScope(
        filter, actor = controller, printedSelfCreature = sourceCreature,
        battlefield = battlefield, unresolved = unresolved
    ).compatible(requirement)
}

@Serializable data class CounterPlacement(
    val action: CounterAction, val type: CounterType, val amount: DynamicAmount,
    val timing: PlacementTiming, val recipient: CounterRecipient, val target: EffectTarget?,
    val targets: List<TargetRequirement>, val optional: Boolean,
    val quantity: Verdict, val context: Verdict
)

@Serializable data class CounterPlacementModifier(
    val action: CounterAction, val pattern: EventPattern, val delta: Int?,
    val placedByYou: Boolean, val activeZones: Set<Zone>, val sourceCreature: Boolean
) {
    fun recipientCompatibility(requirement: Requirements): Verdict {
        fun compatible(recipient: Recipient): Verdict = when (recipient) {
            Recipient.Self -> CounterRecipient(controller = "YOU", sourceCreature = sourceCreature).compatible(requirement)
            is Recipient.Object -> CounterRecipient(filter = recipient.filter).compatible(requirement)
            is Recipient.AnyOf -> any(recipient.options.map(::compatible))
            is Recipient.Player, Recipient.AnotherPlayer -> Verdict.NO_MATCH
        }
        return (pattern as? EventPattern.CounterPlacementEvent)?.recipient?.let(::compatible) ?: Verdict.UNKNOWN
    }
}

/** Structural questions can use a different counter type or recipient without rereading native scripts. */
data class CounterQuestion(
    val type: CounterType,
    val recipient: Requirements,
    val amount: Int? = null,
    val placer: Controller? = null,
    val sourceZone: Zone? = null,
    val sourceIsEntering: Boolean = false
)

fun counterPlacementDecisions(fact: CounterPlacement, type: CounterType, recipient: Requirements): List<Decision> =
    listOf(
        Decision("counter-type", if (fact.type == type) Verdict.MATCH else Verdict.NO_MATCH, "MECHANICAL", fact.type.printed),
        Decision("counter-recipient", fact.recipient.compatible(recipient), "MECHANICAL", fact.recipient.filter.description),
        Decision("positive-placement", fact.quantity, "MECHANICAL", "Quantity per eligible recipient: ${fact.amount.description}"),
        Decision("placement-context", fact.context, "MECHANICAL", "Native ${fact.timing} and branch requirements apply")
    )

fun counterModifierDecisions(fact: CounterPlacementModifier, question: CounterQuestion): List<Decision> {
    val event = fact.pattern as? EventPattern.CounterPlacementEvent
    val counterType = when {
        event == null -> Verdict.UNKNOWN
        event.counterType == null || event.counterType == question.type -> Verdict.MATCH
        else -> Verdict.NO_MATCH
    }
    val active = when {
        Zone.BATTLEFIELD !in fact.activeZones -> Verdict.NO_MATCH
        question.sourceIsEntering -> if (event?.recipient == Recipient.Self) Verdict.MATCH else Verdict.NO_MATCH
        question.sourceZone != null && question.sourceZone !in fact.activeZones -> Verdict.NO_MATCH
        else -> Verdict.MATCH
    }
    return listOf(
        Decision("modified-counter-type", counterType, "MECHANICAL", fact.pattern.description),
        Decision("modified-recipient", fact.recipientCompatibility(question.recipient), "MECHANICAL",
            event?.recipient?.description ?: "Uninterpreted placement pattern"),
        Decision("positive-original-placement", if (question.amount != null && question.amount <= 0) Verdict.NO_MATCH else Verdict.MATCH,
            "MECHANICAL", "Requires a separate placement of one or more counters"),
        Decision("modifier-source", active, "MECHANICAL", "Source active zones: ${fact.activeZones}"),
        Decision("placing-player", if (fact.placedByYou && question.placer == Controller.OPPONENT) Verdict.NO_MATCH else Verdict.MATCH,
            "MECHANICAL", if (fact.placedByYou) "The modifier's controller must place the counters" else "Any player may place the counters"),
        Decision("placement-change", when (fact.delta) { null -> Verdict.UNKNOWN; 0 -> Verdict.NO_MATCH; else -> Verdict.MATCH },
            "MECHANICAL", fact.action.description)
    )
}

/** One isolated additive replacement, with no claim about ordering or other applicable replacements. */
fun isolatedCounterModification(fact: CounterPlacementModifier, question: CounterQuestion): Int? {
    val count = question.amount ?: return null
    if (all(counterModifierDecisions(fact, question).map { it.verdict }) != Verdict.MATCH) return null
    return (count.toLong() + checkNotNull(fact.delta)).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
}
