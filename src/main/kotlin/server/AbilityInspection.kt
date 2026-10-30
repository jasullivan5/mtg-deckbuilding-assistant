package dev.mtg.assistant.explorer

import dev.mtg.assistant.EngineBuild
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.extensions.TranslationAssurance
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable data class AbilityInspection(
    val id: String, val title: String, val face: String?, val verdict: Verdict,
    val contribution: String, val costs: List<String>, val effects: List<String>,
    val conditions: List<String>, val tradeoffs: List<String>, val uncertainties: List<String>,
    val evidence: List<EvidenceIndicator>, val questions: List<ResearchConcern>,
    val resultIndexes: List<Int>, val actionId: String
)

private fun JsonObject?.items(key: String) = (this?.get(key) as? JsonArray).orEmpty()
private fun JsonObject?.flag(key: String) = (this?.get(key) as? JsonPrimitive)?.booleanOrNull ?: false
private fun JsonObject?.number(key: String) = (this?.get(key) as? JsonPrimitive)?.intOrNull
private fun plainObject(s: String) = s.removePrefix("an ").removePrefix("a ")
private fun fixed(node: JsonObject?): Int? = node?.takeIf { it.text("type") == "Fixed" }?.number("amount")

/** A formatter over the exact authored ability. It never evaluates legality or changes a verdict. */
private class AbilityText(private val action: ActionEvidence?, private val rows: List<ResultEvidence>) {
    val native = action?.native.obj()
    private val targets = native.items("targetRequirements").mapNotNull { it.obj() } + listOfNotNull(native?.get("targetRequirement").obj())
    val costs = mutableListOf<String>()
    val effects = mutableListOf<String>()
    val conditions = mutableListOf<String>()
    val tradeoffs = mutableListOf<String>()
    val gaps = mutableListOf<String>()
    private val activated = rows.any { it.circumstances?.actionKind == "ACTIVATED" }

    private fun targetDescription(t: JsonObject?): String = when (t.text("type")) {
        "TargetObject" -> {
            val filter = t?.get("filter").obj()
            val zone = filter.text("zone")
            filterDescription(filter?.get("baseFilter").obj()) +
                if (zone != null && zone != "Battlefield") " in the recorded ${zone.lowercase()} zone" else ""
        }
        "AnyTarget" -> "any legal damage target"
        "TargetPlayer" -> "a player"
        "TargetOpponent" -> "an opponent"
        else -> "a target with requirements not fully summarized here"
    }
    private fun namedTarget(requirement: JsonObject): String {
        val description = targetDescription(requirement)
        return if (description.startsWith("any ")) description else "target ${plainObject(description)}"
    }
    private fun target(t: JsonObject?): String = when (t.text("type")) {
        "BoundVariable" -> targets.singleOrNull { it.text("id") == t.text("name") }?.let(::namedTarget)
            ?: "an affected object whose target binding needs inspection"
        "ContextTarget" -> targets.getOrNull(t.number("index") ?: 0)?.let(::namedTarget)
            ?: "an affected object whose target binding needs inspection"
        "Self" -> "this permanent"
        "Controller" -> "you"
        "PlayerRef" -> when (t?.get("player").obj().text("type")) {
            "You", "ControllerOfSource" -> "you"; "EachOpponent" -> "each opponent"; "Each" -> "each player"
            "TargetOpponent" -> "target opponent"; "TargetPlayer" -> "target player"; else -> "the recorded player"
        }
        else -> "the affected object (binding not summarized)"
    }
    private fun cost(n: JsonObject?) {
        when (n.text("type")) {
            null -> Unit
            "CostComposite" -> n.items("costs").forEach { cost(it.obj()) }
            "CostAtomWrapper" -> cost(n?.get("atom").obj())
            "AtomMana" -> costs.add("Pay ${n.text("cost") ?: "the recorded mana cost"}.")
            "CostTap" -> costs.add("Tap this permanent; it must be untapped and able to pay a tap cost.")
            "AtomSacrifice" -> {
                val count = n.number("count") ?: 1
                costs += "Sacrifice ${if (count == 1) filterDescription(n?.get("filter").obj()) else "$count permanents matching ${filterDescription(n?.get("filter").obj())}"}${if (n.flag("excludeSelf")) ", other than this source" else ""}. You must control what you sacrifice."
                if (count > 0) tradeoffs += "The sacrifice is paid during activation. It is not refunded if the resulting ability is countered or fails to resolve."
            }
            else -> gaps.add("An additional cost is not summarized. Its exact payment requirements remain in the ability record.")
        }
    }
    private fun effect(n: JsonObject?) {
        when (n.text("type")) {
            "Composite" -> n.items("effects").forEach { effect(it.obj()) }
            "MoveToZone" -> if (n.flag("byDestruction")) effects.add("Destroy ${target(n?.get("target").obj())}.")
                else effects.add("Move ${target(n?.get("target").obj())} to ${n.text("destination")?.lowercase() ?: "the recorded destination"}.")
            "LoseLife" -> fixed(n?.get("amount").obj())?.let { amount -> effects.add("${target(n?.get("target").obj() ?: buildJsonObject { put("type", "PlayerRef"); put("player", buildJsonObject { put("type", "TargetOpponent") }) }).replaceFirstChar { it.uppercase() }} loses $amount life.") }
                ?: gaps.add("The life-loss amount needs inspection; it is not summarized as a fixed number.")
            "GainLife" -> fixed(n?.get("amount").obj())?.let { amount -> effects.add("${target(n?.get("target").obj() ?: buildJsonObject { put("type", "Controller") }).replaceFirstChar { it.uppercase() }} ${if (n?.get("target") == null || n["target"].obj().text("type") == "Controller") "gain" else "gains"} $amount life.") }
                ?: gaps.add("The life-gain amount needs inspection; it is not summarized as a fixed number.")
            "ModifyStats" -> {
                val p = fixed(n?.get("powerModifier").obj()); val t = fixed(n?.get("toughnessModifier").obj())
                if (p != null && t != null) {
                    fun signed(v: Int) = if (v >= 0) "+$v" else v.toString()
                    effects += "${target(n?.get("target").obj()).replaceFirstChar { it.uppercase() }} gets ${signed(p)}/${signed(t)}${if (n?.get("duration") == null || n["duration"].obj().text("type") == "EndOfTurn") " until end of turn" else " for the recorded duration"}."
                } else gaps += "A changing power/toughness amount needs inspection."
            }
            "DealDamage" -> fixed(n?.get("amount").obj())?.let { amount -> effects.add("This source deals $amount damage to ${target(n?.get("target").obj())}.") }
                ?: gaps.add("The damage amount needs inspection; it is not summarized as a fixed number.")
            "Sacrifice" -> effects.add("Sacrifice ${filterDescription(n?.get("filter").obj())}${if (n.flag("excludeSource")) ", other than this source" else ""}.")
            "SacrificeSelf" -> effects.add("Sacrifice this permanent.")
            else -> gaps.add("An additional effect is not summarized; inspect the authored instruction before relying on it.")
        }
    }
    init {
        cost(native?.get("cost").obj())
        for (requirement in targets) {
            val optional = requirement.flag("optional")
            val minimum = requirement.number("minCount") ?: if (optional || requirement.flag("unlimited")) 0 else requirement.number("count") ?: 1
            conditions += "${if (minimum > 0) "Choose" else "If you choose a target, choose"} ${targetDescription(requirement)}${if (minimum > 1) " (at least $minimum targets)" else ""}."
            if (activated && minimum > 0 && requirement["dynamicMinCount"] == null)
                conditions += "A required legal target must exist before you can activate this ability. You cannot activate it merely to pay the sacrifice cost without that target."
            if (requirement["dynamicMinCount"] != null || requirement["dynamicMaxCount"] != null || requirement.flag("sameController") || requirement.flag("sameOwner"))
                gaps += "Additional target-count or relationship restrictions require inspection."
        }
        val trigger = native?.get("trigger").obj()
        if (trigger.text("type") == "ZoneChangeEvent") {
            val ownSource = rows.any { it.circumstances?.timingAndConditions.orEmpty().any { text -> "binding=SELF;" in text } }
            val from = trigger.text("from"); val to = trigger.text("to")
            if (ownSource && to == "Battlefield") conditions += "This ability triggers when this permanent enters the battlefield."
            else if (from == "Battlefield" && to == "Graveyard") conditions += "This ability triggers when ${filterDescription(trigger?.get("filter").obj())} moves from the battlefield to the graveyard."
            else gaps += "This trigger's timing needs inspection."
        }
        effect(native?.get("effect").obj())
        if (activated) conditions += "Each activation needs a new payment of its costs and legal choices; any recorded timing limits still apply."
        if (rows.any { it.operation == "DAMAGE_POTENTIALLY_LETHAL" }) conditions += "Damage contributes a creature death only if it is lethal in that game state. Damage to a player does not itself make a creature die."
        if (rows.any { it.operation == "DESTROY" }) conditions += "Destruction has to succeed to cause a death; indestructibility and replacement effects can change the outcome. The sacrifice cost is separate from that outcome."
        if (rows.any { it.circumstances?.mandatory == Verdict.MATCH }) conditions += "This is a required instruction when the triggered effect resolves, not an optional sacrifice outlet."
        if (rows.any { it.circumstances?.delayedTrigger == true || it.circumstances?.deferredTiming == true }) gaps += "A delayed or timing-dependent instruction needs inspection."
    }
}

private fun unreadInstruction(item: Unsupported, scope: String): ResearchConcern {
    val title = when {
        ".cost" in item.path -> "A payment instruction needs interpretation"
        ".target" in item.path -> "An affected-object restriction needs interpretation"
        ".trigger" in item.path -> "A trigger needs interpretation"
        ".condition" in item.path || ".gate" in item.path -> "A condition needs interpretation"
        else -> "An additional instruction was not analysed"
    }
    val text = when (item.nativeType.substringAfterLast('.')) {
        "LoseLifeEffect" -> "Discovery did not interpret the life-loss instruction for this objective. Its presence in the authored ability is not an engine execution result."
        "GainLifeEffect" -> "Discovery did not interpret the life-gain instruction for this objective. The recognized sacrifice cost remains separately identified."
        else -> "${item.reason} This is a limitation of this discovery record, not an established rules defect or engine failure."
    }
    return ResearchConcern(title, text, "Inspect the retained instruction and affected-object/condition evidence before drawing a conclusion about that part.", EvidenceState.INCOMPLETE, scope)
}

fun abilityInspections(
    results: List<ResultEvidence>, actions: List<ActionEvidence>, unsupported: List<Unsupported>,
    assurance: TranslationAssurance?, scenarios: List<String>, runtimeRevision: String = EngineBuild.revision
): List<AbilityInspection> {
    val byId = actions.associateBy { it.id }
    return results.withIndex().groupBy { it.value.actionId }.entries.mapIndexed { ordinal, (id, entries) ->
        val rows = entries.map { it.value }
        val best = rows.firstOrNull { it.verdict == Verdict.MATCH } ?: rows.firstOrNull { it.verdict == Verdict.UNKNOWN } ?: rows.first()
        val action = byId[id]
        val kind = best.circumstances?.actionKind
        val title = when (kind) { "ACTIVATED" -> "Activated ability"; "TRIGGERED", "STATE_TRIGGERED" -> "Triggered ability"; "SPELL" -> "Spell effect"; else -> "Card ability" } + " ${ordinal + 1}"
        val narration = AbilityText(action, rows)
        val relevantParts = rows.filter { it.verdict != Verdict.NO_MATCH }
        val scope = "$title — ${mechanic(best)} ${if (best.costOrEffect.endsWith("COST")) "cost" else "relationship"}"
        val questions = unsupported.filter { it.actionId == id || it.actionId == action?.associatedActionId }.map { unreadInstruction(it, title) } +
            narration.gaps.distinct().map { ResearchConcern("An authored instruction needs inspection", it, "Read the exact ability record and source requirements.", EvidenceState.INCOMPLETE, title) } +
            rows.filter { it != best && it.decisions.any { d -> d.verdict == Verdict.UNKNOWN } }.map {
                ResearchConcern("Another instruction has unresolved checks",
                    "The authored instruction is described above, but discovery did not resolve all of its affected-object or condition checks. Its objective-policy exclusion is separate from mechanical impossibility.",
                    "Inspect the retained object and objective decisions. An unresolved check is not an established implementation defect.", EvidenceState.INCOMPLETE, "${mechanic(it)} in $title")
            }
        val explanation = explain(best, action)
        val uncertainty = if (best.verdict == Verdict.UNKNOWN) explanation.unknowns.filterNot {
            it.startsWith("This finding") || it.startsWith("Only part") || it.startsWith("The application has not")
        } else emptyList()
        val objectiveConditions = when (best.branch) {
            Contribution.DIRECT_DEATH_GENERATOR -> listOf("For this objective, the affected permanent must be a nontoken creature you control and must reach the graveyard. A token or an ordinary noncreature permanent does not meet that condition.")
            Contribution.REANIMATE_OWN_CREATURE -> listOf("For this objective, the original creature card must come from your graveyard and enter the battlefield under your control. A copy or a return to hand is a different result.")
            Contribution.CREATE_CREATURE_TOKEN -> listOf("For this objective, at least one created token must be a creature under your control when the ability resolves. The actual number still depends on the recorded amount and game state.")
            else -> emptyList()
        }
        AbilityInspection(id, title, best.face.takeUnless { it == "front" }, best.verdict,
            if (best.verdict == Verdict.NO_MATCH) explanation.outcome else explanation.summary,
            narration.costs.distinct(), narration.effects.distinct(), (objectiveConditions + narration.conditions).distinct(), narration.tradeoffs.distinct(), uncertainty,
            evidenceIndicators(relevantParts.ifEmpty { rows }, assurance, scenarios, scope, runtimeRevision), questions.distinct(), entries.map { it.index }, id)
    }.sortedBy { when (it.verdict) { Verdict.MATCH -> 0; Verdict.UNKNOWN -> 1; Verdict.NO_MATCH -> 2 } }
}
