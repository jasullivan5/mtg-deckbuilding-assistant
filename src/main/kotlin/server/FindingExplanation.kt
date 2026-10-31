package dev.mtg.assistant.explorer

import dev.mtg.assistant.discovery.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

internal fun JsonElement?.obj() = this as? JsonObject
internal fun JsonObject?.text(key: String) = (this?.get(key) as? JsonPrimitive)?.contentOrNull
private fun JsonObject?.array(key: String) = (this?.get(key) as? JsonArray).orEmpty()

@Serializable data class PathwayExplanation(
    val summary: String, val requirements: List<String>, val unknowns: List<String> = emptyList(),
    val mechanic: String = "Other relationships", val outcome: String = ""
)

/** A bounded description of represented SDK records. This never accepts/rejects a pathway. */
private fun predicate(p: JsonObject): String = when (p.text("type")) {
    "IsCreature" -> "a creature"
    "IsArtifact" -> "an artifact"
    "IsLand" -> "a land"
    "IsEnchantment" -> "an enchantment"
    "IsPlaneswalker" -> "a planeswalker"
    "IsPermanent" -> "a permanent"
    "IsNontoken" -> "a nontoken permanent"
    "IsToken" -> "a token"
    "IsNoncreature" -> "a noncreature permanent"
    "Or", "And" -> p.array("predicates").joinToString(if (p.text("type") == "Or") " or " else " and ") {
        "(${it.obj()?.let(::predicate) ?: "an unresolved object"})"
    }
    "Not" -> "an object other than ${p["predicate"].obj()?.let(::predicate) ?: "an unresolved object"}"
    else -> "an object with an additional requirement the explanation cannot translate"
}

internal fun filterDescription(filter: JsonObject?): String {
    if (filter == null) return "an affected object whose type is unresolved"
    val predicates = filter.array("cardPredicates").mapNotNull { it.obj() }
    val simple = setOf("IsCreature", "IsArtifact", "IsLand", "IsEnchantment", "IsPlaneswalker", "IsPermanent", "IsNontoken", "NotColor")
    if (predicates.isNotEmpty() && predicates.all { it.text("type") in simple } && filter.array("anyOf").isEmpty()) {
        val kinds = predicates.mapNotNull { when (it.text("type")) {
            "IsCreature" -> "creature"; "IsArtifact" -> "artifact"; "IsLand" -> "land"
            "IsEnchantment" -> "enchantment"; "IsPlaneswalker" -> "planeswalker"; "IsPermanent" -> "permanent"; else -> null
        } }
        val modifiers = predicates.mapNotNull { when (it.text("type")) {
            "IsNontoken" -> "nontoken"
            "NotColor" -> it.text("color")?.lowercase()?.let { color -> "non$color" }
            else -> null
        } }
        val controller = when (filter["controllerPredicate"].obj().text("type")) {
            "ControlledByYou" -> " you control"; "ControlledByOpponent" -> " an opponent controls"
            null -> ""; else -> " subject to an additional controller restriction"
        }
        val phrase = (modifiers + kinds.ifEmpty { listOf("permanent") }).joinToString(" ")
        val state = if (filter.array("statePredicates").isNotEmpty()) " with additional state restrictions" else ""
        return "${if (phrase.firstOrNull() in listOf('a','e','i','o','u')) "an" else "a"} $phrase$controller$state"
    }
    val clauses = filter.array("cardPredicates").map { it.obj()?.let(::predicate) ?: "an unresolved object" } +
        filter.array("anyOf").takeIf { it.isNotEmpty() }?.let { branches ->
            listOf(branches.joinToString(" or ") { "(${filterDescription(it.obj())})" }) }.orEmpty()
    return clauses.joinToString(" and ").ifBlank { "a permanent" }
}

private fun costDescriptions(node: JsonObject?): List<String> = when (node.text("type")) {
    null -> emptyList()
    "CostComposite" -> node.array("costs").flatMap { costDescriptions(it.obj()) }
    "CostAtomWrapper" -> costDescriptions(node?.get("atom").obj())
    "AtomMana" -> listOf("Pay ${node.text("cost") ?: "the represented mana cost"}.")
    "CostTap" -> listOf("Tap this source; it must be untapped. A creature normally needs haste or to have been under your control since your turn began.")
    "AtomSacrifice", "Sacrifice", "AtomSacrificeAll" -> emptyList() // Described by the exact result, not a second cost evaluator.
    else -> listOf("Additional costs are retained in the detailed ability record; this explanation does not translate them.")
}

fun mechanic(result: ResultEvidence): String = when {
    result.branch == Contribution.DEATH_ENHANCED_REMOVAL -> "Removal enabled by a death"
    result.branch == Contribution.REANIMATE_OWN_CREATURE -> "Return from the graveyard"
    result.branch == Contribution.CREATE_CREATURE_TOKEN -> "Create creature tokens"
    result.operation == "SACRIFICE" -> "Sacrifice"
    result.operation == "DESTROY" -> "Destruction"
    result.operation == "DAMAGE_POTENTIALLY_LETHAL" -> "Damage"
    result.operation == "TOUGHNESS_REDUCTION" -> "Reduce toughness"
    result.operation == "MOVE_TO_GRAVEYARD" -> "Move to the graveyard"
    else -> "Other relationships"
}

fun explain(result: ResultEvidence, action: ActionEvidence? = null): PathwayExplanation {
    val native = result.nativeOperation.obj()
    val filter = native?.get("filter").obj()
    val affected = filterDescription(filter)
    val isSacrifice = result.operation == "SACRIFICE"
    val sacrificeSelf = native.text("type") == "SacrificeSelf"
    val excludesSource = native?.get("excludeSelf")?.jsonPrimitive?.booleanOrNull == true ||
        native?.get("excludeSource")?.jsonPrimitive?.booleanOrNull == true ||
        result.circumstances?.objectRestrictions?.contains("EXCLUDES_SOURCE") == true
    val relationship = when {
        result.branch == Contribution.DEATH_ENHANCED_REMOVAL -> "A death event is linked to this card's removal ability. The details retain which event and which removal are connected."
        isSacrifice -> "This ability ${if (result.costOrEffect.endsWith("COST")) "sacrifices ${if (sacrificeSelf) "this source" else affected} as a cost" else "instructs a player to sacrifice ${if (sacrificeSelf) "this source" else affected}"}. ${if (excludesSource) "The source itself cannot be sacrificed for this instruction. " else ""}A sacrificed nontoken creature you control can contribute to your creature-dies objective if it reaches the graveyard."
        result.branch == Contribution.REANIMATE_OWN_CREATURE -> "This ability moves a card from your graveyard onto your battlefield. This is being checked for returning an original creature card."
        result.branch == Contribution.CREATE_CREATURE_TOKEN -> "This ability creates tokens. This is being checked for creating at least one creature token under your control."
        result.operation == "DESTROY" -> "This ability destroys an affected permanent. This is being checked for causing a nontoken creature you control to die."
        result.operation == "DAMAGE_POTENTIALLY_LETHAL" -> "This ability deals damage. Damage could cause a creature to die only if it is lethal in the actual game."
        result.operation == "TOUGHNESS_REDUCTION" -> "This ability reduces toughness. A creature could die if its toughness becomes zero or less in the actual game."
        result.operation == "MOVE_TO_GRAVEYARD" -> "This ability moves an object to the graveyard. It contributes a creature death only if that object is a creature on the battlefield immediately beforehand."
        else -> "An operation was retained for this objective, but this explanation cannot describe its precise relationship. Inspect the evidence before drawing a conclusion."
    }
    val requirements = mutableListOf<String>()
    requirements += costDescriptions(action?.native.obj()?.get("cost").obj())
    if (isSacrifice) {
        requirements += if (sacrificeSelf) "The source must be available to sacrifice." else
            "The sacrifice must use: $affected${if (excludesSource) ", excluding this source" else ""}. Other filter restrictions remain in the evidence."
        if (result.branch == Contribution.DIRECT_DEATH_GENERATOR) requirements +=
            "For your objective, the sacrificed object would have to be a nontoken creature you control. A token or an ordinary noncreature permanent does not meet it."
        if (!excludesSource && native.text("type") in setOf("Sacrifice", "AtomSacrifice"))
            requirements += "The represented sacrifice does not exclude this source; if it satisfies all restrictions, it can be chosen."
        if (result.circumstances?.mandatory == Verdict.MATCH) requirements += "This is a required sacrifice when the represented effect resolves, rather than a repeatable sacrifice cost."
    }
    val timing = result.circumstances?.timingAndConditions.orEmpty()
    if (action?.native.obj()?.get("trigger").obj().text("type") == "ZoneChangeEvent" &&
        action?.native.obj()?.get("trigger").obj().text("to") == "Battlefield" && timing.any { "binding=SELF;" in it })
        requirements += "The ability triggers when this source enters the battlefield."
    requirements += "All timing, targets, choices and resources for this particular ability must be legal. Exact restrictions are retained in the ability evidence."
    if (result.branch == Contribution.DIRECT_DEATH_GENERATOR || result.branch == Contribution.DEATH_ENHANCED_REMOVAL)
        requirements += "The creature must reach the graveyard to die. Replacement effects or other game-state rules can change the outcome."
    if (result.branch == Contribution.REANIMATE_OWN_CREATURE)
        requirements += "The original object must be a creature card in your graveyard and the represented movement must put it onto your battlefield."
    if (result.branch == Contribution.CREATE_CREATURE_TOKEN)
        requirements += "The created objects must include at least one creature token under your control when the ability resolves."
    val unknowns = mutableListOf<String>()
    if (result.verdict == Verdict.UNKNOWN) {
        unknowns += "The application has not established that this ability meets the objective."
        if (isSacrifice && (filter?.get("cardPredicates") as? JsonArray).orEmpty().any { it.obj().text("type") == "IsLand" })
            unknowns += "An ordinary land is not a creature. A land would also need to be a nontoken creature you control when sacrificed. This run has not established a land becoming a creature or verified that interaction."
        unknowns += result.decisions.filter { it.verdict == Verdict.UNKNOWN }.map { decision ->
            when (decision.stage) {
                "potential-creature-death" -> "Whether the affected object can be a creature under the recorded restrictions remains unresolved."
                "controller-YOU" -> "Whether the affected object can be under your control remains unresolved."
                "token-NONTOKEN" -> "Whether the affected object can be nontoken remains unresolved."
                else -> "An additional requirement could not be established. Its exact decision is retained in the ability evidence."
            }
        }.distinct()
    }
    if (result.localSupport == SupportStatus.PARTIALLY_SUPPORTED || result.interpretation.startsWith("PARTIAL"))
        unknowns += "Only part of this ability has been understood. Other instructions may matter; inspect what the application could not read."
    unknowns += "This finding is not a played-out game, a full card verification, or a judgment that the card belongs in your deck."
    val outcome = when (result.verdict) {
        Verdict.MATCH -> "Can contribute if these conditions hold"
        Verdict.UNKNOWN -> "Needs investigation"
        Verdict.NO_MATCH -> listOfNotNull(
            "This ability was excluded by this objective's selection rules. This does not mean the card cannot contribute in a game."
                .takeIf { result.decisions.any { it.verdict == Verdict.NO_MATCH && it.basis == "POLICY" } },
            "The affected objects or other recorded requirements do not satisfy this objective for this ability."
                .takeIf { result.decisions.any { it.verdict == Verdict.NO_MATCH && it.basis != "POLICY" } }
        ).joinToString(" ").ifBlank { "This ability did not meet the objective." } + " Other abilities on this card may differ."
    }
    return PathwayExplanation(relationship, requirements.distinct(), unknowns, mechanic(result), outcome)
}
