package dev.mtg.assistant.explorer

import dev.mtg.assistant.EngineBuild
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.extensions.TranslationAssurance
import com.wingedsheep.sdk.model.CardDefinition
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable data class AbilityInspection(
    val id: String, val title: String, val face: String?, val verdict: Verdict,
    val contribution: String, val costs: List<String>, val effects: List<String>,
    val conditions: List<String>, val tradeoffs: List<String>, val uncertainties: List<String>,
    val evidence: List<EvidenceIndicator>, val questions: List<ResearchConcern>,
    val resultIndexes: List<Int>, val actionId: String,
    val objectiveNotes: List<String> = emptyList(), val unresolvedNotes: List<String> = emptyList(),
    val textCandidates: List<String> = emptyList()
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
    var triggerDescription: String? = null
        private set
    private val costExcerpt = mutableListOf<String>()
    var triggerExcerpt: String? = null
        private set
    fun textCandidates(): List<String> {
        if (gaps.isNotEmpty() || native?.get("condition") != null) return listOfNotNull(native.text("descriptionOverride"))
        val effect = effects.joinToString(" ")
        return listOfNotNull(native.text("descriptionOverride"), when {
            triggerExcerpt != null -> "$triggerExcerpt, $effect"
            costExcerpt.isNotEmpty() -> "${costExcerpt.joinToString(", ")}: $effect"
            else -> effect.takeIf { it.isNotBlank() }
        })
    }
    fun title(fallback: String): String {
        val effect = effects.joinToString(" ").removeSuffix(".").replace("This source deals", "deal")
        val lead = triggerExcerpt ?: costExcerpt.joinToString(", ").takeIf { it.isNotBlank() }
        // This is an excerpt of the corresponding stored action, never a guessed Oracle-line mapping.
        val text = when {
            lead != null && effect.isNotBlank() -> "$lead — ${effect.replaceFirstChar { it.lowercase() }}"
            lead != null -> lead
            effect.isNotBlank() -> effect
            else -> fallback
        }
        val incomplete = if (gaps.isNotEmpty() || native?.get("condition") != null) " · more details below" else ""
        return text.take(180) + (if (text.length > 180) "…" else "") + incomplete
    }
    private val activated = rows.any { it.circumstances?.actionKind == "ACTIVATED" }

    private fun targetDescription(t: JsonObject?): String = when (t.text("type")) {
        "TargetObject" -> {
            val filter = t?.get("filter").obj()
            val zone = filter.text("zone")
            filterDescription(filter?.get("baseFilter").obj()) +
                if (zone != null && zone != "Battlefield") " in the recorded ${zone.lowercase()} zone" else ""
        }
        "AnyTarget" -> "any target"
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
            "AtomMana" -> { costs.add("Pay ${n.text("cost") ?: "the recorded mana cost"}."); costExcerpt += n.text("cost") ?: "Mana payment" }
            "CostTap" -> { costs.add("Tap this permanent; it must be untapped and able to pay a tap cost."); costExcerpt += "{T}" }
            "AtomSacrifice" -> {
                val count = n.number("count") ?: 1
                costs += "Sacrifice ${if (count == 1) filterDescription(n?.get("filter").obj()) else "$count permanents matching ${filterDescription(n?.get("filter").obj())}"}${if (n.flag("excludeSelf")) ", other than this source" else ""}. You must control what you sacrifice."
                costExcerpt += costs.last().substringBefore(". You")
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
            if (activated && minimum > 0 && requirement["dynamicMinCount"] == null)
                conditions += "Choose ${targetDescription(requirement)}${if (minimum > 1) " (at least $minimum targets)" else ""} as a legal target before activation${if (costExcerpt.any { it.startsWith("Sacrifice") }) ", even if you only want to sacrifice" else ""}."
            else conditions += "${if (minimum > 0) "Choose" else "If you choose a target, choose"} ${targetDescription(requirement)}${if (minimum > 1) " (at least $minimum targets)" else ""}."
            if (requirement["dynamicMinCount"] != null || requirement["dynamicMaxCount"] != null || requirement.flag("sameController") || requirement.flag("sameOwner"))
                gaps += "Additional target-count or relationship restrictions require inspection."
        }
        val trigger = native?.get("trigger").obj()
        if (trigger.text("type") == "ZoneChangeEvent") {
            val ownSource = rows.any { it.circumstances?.timingAndConditions.orEmpty().any { text -> "binding=SELF;" in text } }
            val from = trigger.text("from"); val to = trigger.text("to")
            if (ownSource && to == "Battlefield") {
                triggerExcerpt = "When this permanent enters the battlefield"
                triggerDescription = "$triggerExcerpt."
            }
            else if (from == "Battlefield" && to == "Graveyard") {
                val subject = if (ownSource) "this permanent" else filterDescription(trigger?.get("filter").obj())
                val creature = trigger?.get("filter").obj().items("cardPredicates").any { it.obj().text("type") == "IsCreature" }
                triggerExcerpt = "Whenever $subject ${if (creature) "dies" else "is put into a graveyard from the battlefield"}"
                triggerDescription = "$triggerExcerpt."
            }
            else gaps += "This trigger's timing needs inspection."
        }
        triggerDescription?.let { conditions += it }
        effect(native?.get("effect").obj())
        if (activated) conditions += "You can activate it again while you have the resources and legal choices; timing limits still apply."
        if (rows.any { it.operation == "DAMAGE_POTENTIALLY_LETHAL" }) conditions += "Damage contributes a creature death only if it is lethal in that game state. Damage to a player does not itself make a creature die."
        if (rows.any { it.operation == "DESTROY" }) tradeoffs += "Indestructibility or replacement effects can prevent the destruction from causing a death. They do not undo the sacrifice payment."
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
    assurance: TranslationAssurance?, scenarios: List<String>, runtimeRevision: String = EngineBuild.revision,
    deathPayoffCommander: String? = null
): List<AbilityInspection> {
    val byId = actions.associateBy { it.id }
    return results.withIndex().groupBy { it.value.actionId }.entries.mapIndexed { ordinal, (id, entries) ->
        val rows = entries.map { it.value }
        val best = rows.firstOrNull { it.verdict == Verdict.MATCH } ?: rows.firstOrNull { it.verdict == Verdict.UNKNOWN } ?: rows.first()
        val action = byId[id]
        val kind = best.circumstances?.actionKind
        val narration = AbilityText(action, rows)
        val title = narration.title(when (kind) { "ACTIVATED" -> "Ability with an activation cost"; "TRIGGERED", "STATE_TRIGGERED" -> "Ability triggered by an event"; "SPELL" -> "Spell instructions"; else -> "Ability details" } + " (${ordinal + 1})")
        val relevantParts = rows.filter { it.verdict != Verdict.NO_MATCH }
        val scope = "$title — ${mechanic(best)} ${if (best.costOrEffect.endsWith("COST")) "cost" else "relationship"}"
        val questions = unsupported.filter { it.actionId == id || it.actionId == action?.associatedActionId }.map { unreadInstruction(it, title) } +
            narration.gaps.distinct().map { ResearchConcern("An authored instruction needs inspection", it, "Read the exact ability record and source requirements.", EvidenceState.INCOMPLETE, title) } +
            rows.filter { it != best && it.decisions.any { d -> d.verdict == Verdict.UNKNOWN } }.map {
                ResearchConcern("Another instruction has unresolved checks",
                    "The affected-object restriction '${it.affectedScope.substringBefore(';')}' has unresolved discovery checks. The authored instruction is described above; its objective-policy exclusion is separate from mechanical impossibility.",
                    "Inspect the retained object and objective decisions. An unresolved check is not an established implementation defect.", EvidenceState.INCOMPLETE, "${mechanic(it)} in $title")
            }
        val explanation = explain(best, action)
        val landSacrifice = best.operation == "SACRIFICE" && best.nativeOperation.obj()?.get("filter").obj()
            .items("cardPredicates").any { it.obj().text("type") == "IsLand" }
        val uncertainty = if (best.verdict != Verdict.UNKNOWN) emptyList() else if (landSacrifice)
            listOf("Discovery has not resolved the land/creature type overlap.") +
                explanation.unknowns.filter { it.startsWith("An additional requirement") }
        else explanation.unknowns.filterNot {
            it.startsWith("This finding") || it.startsWith("Only part") || it.startsWith("The application has not")
        }
        val secondaryUnknowns = rows.filter { it != best && it.decisions.any { d -> d.verdict == Verdict.UNKNOWN } }.map {
            "Discovery has not fully interpreted the '${it.affectedScope.substringBefore(';')}' restriction."
        }.distinct()
        val objectiveConditions = when (best.branch) {
            Contribution.DIRECT_DEATH_GENERATOR -> listOf("For this objective, use a nontoken creature you control that reaches the graveyard. A token or noncreature permanent does not advance it.")
            Contribution.REANIMATE_OWN_CREATURE -> listOf("For this objective, the original creature card must come from your graveyard and enter the battlefield under your control. A copy or a return to hand is a different result.")
            Contribution.CREATE_CREATURE_TOKEN -> listOf("For this objective, at least one created token must be a creature under your control when the ability resolves. The actual number still depends on the recorded amount and game state.")
            else -> emptyList()
        }
        val notes = buildList {
            if (best.branch == Contribution.DIRECT_DEATH_GENERATOR && best.operation == "SACRIFICE" && best.verdict == Verdict.MATCH) {
                val filter = best.nativeOperation.obj()?.get("filter").obj()
                val choice = filter.items("cardPredicates").singleOrNull().obj()
                val creatureOrArtifact = choice.text("type") == "Or" && choice.items("predicates").map { it.obj().text("type") }.toSet() == setOf("IsCreature", "IsArtifact")
                add(if (sacrificeObject(best) == "Artifact or land") "A nontoken artifact creature you control can pay this cost without animation. A noncreature artifact or land does not cause a creature death."
                    else if (sacrificeObject(best) == "Artifact") "For this objective, use a nontoken artifact creature you control; a noncreature artifact does not cause a creature death."
                    else if (creatureOrArtifact) "Only a nontoken creature choice advances this objective; a noncreature artifact or token does not."
                    else "For this objective, sacrifice a nontoken creature you control.${if (best.circumstances?.mandatory == Verdict.MATCH) " The sacrifice is mandatory when the ability resolves." else ""}")
            } else if (best.verdict == Verdict.MATCH && best.branch == Contribution.DIRECT_DEATH_GENERATOR)
                add(if (best.operation == "DAMAGE_POTENTIALLY_LETHAL") "The damage must be lethal to a nontoken creature you control."
                    else "This contributes when a nontoken creature you control dies.")
            addAll(narration.conditions.filter { "even if you only want to sacrifice" in it })
        }
        val unresolvedNotes = if (landSacrifice && best.verdict == Verdict.UNKNOWN)
            listOf("A sacrificed land must also be a nontoken creature you control, either inherently or through a type-changing effect. Discovery has not resolved that overlap.")
        else uncertainty + secondaryUnknowns.map { it.substringBefore(" The contribution above") }
        AbilityInspection(id, title, best.face.takeUnless { it == "front" }, best.verdict,
            when {
                best.verdict == Verdict.NO_MATCH -> explanation.outcome
                best.branch == Contribution.DEATH_ENHANCED_REMOVAL && narration.triggerDescription != null ->
                    "${narration.triggerDescription} ${narration.effects.joinToString(" ")} This ability benefits from a creature dying; it does not cause the initial death."
                best.branch == Contribution.DIRECT_DEATH_GENERATOR && best.operation == "SACRIFICE" && best.costOrEffect.endsWith("COST") && best.verdict == Verdict.MATCH ->
                    if (deathPayoffCommander != null) "Sacrificing a nontoken creature you control to pay this ability's cost can trigger $deathPayoffCommander's death ability."
                    else "Paying this ability's sacrifice cost with a nontoken creature you control can trigger abilities that care about your creatures dying."
                else -> explanation.summary
            },
            narration.costs.distinct(), narration.effects.distinct(), (objectiveConditions + narration.conditions).distinct(), narration.tradeoffs.distinct(), (uncertainty + secondaryUnknowns).distinct(),
            evidenceIndicators(relevantParts.ifEmpty { rows }, assurance, scenarios, scope, runtimeRevision, action), questions.distinct(), entries.map { it.index }, id,
            notes.distinct(), unresolvedNotes.distinct(), narration.textCandidates())
    }.sortedBy { when (it.verdict) { Verdict.MATCH -> 0; Verdict.UNKNOWN -> 1; Verdict.NO_MATCH -> 2 } }
}

/** A commander connection uses the same single-card native walker; it is not a second objective policy. */
private fun commanderConnections(card: CardDefinition, objective: String): List<ActionEvidence> {
    if (objective != "own-nontoken-creature-deaths") return emptyList()
    val extracted = PathwayExtractor().extract(listOf(card))
    val actionIds = extracted.pathways.filter {
        it.branch == Contribution.DEATH_ENHANCED_REMOVAL &&
            it.scope.compatible(Requirements(creature = true, controller = Controller.YOU, token = TokenStatus.NONTOKEN)) == Verdict.MATCH
    }.map { it.actionId }.toSet()
    return extracted.actions.filter { it.id in actionIds }.filter { action ->
        val text = AbilityText(action, emptyList())
        // Only this fully described trigger supports the broad connection. Other forms need a truthful fallback.
        text.triggerExcerpt == "Whenever a nontoken creature you control dies" && text.gaps.isEmpty() &&
            action.native.obj()?.get("condition") == null
    }
}
fun commanderObjectiveConnection(card: CardDefinition, objective: String): String? =
    if (commanderConnections(card, objective).isEmpty()) null else "The trigger does not depend on what caused the death."

fun commanderRulesLines(card: CardDefinition, objective: String): List<RulesLine> = linkedRulesLines(card.name, card.oracleText,
    commanderConnections(card, objective).map { RulesLink(it.id, it.face.takeUnless { face -> face == "front" }, Verdict.MATCH, AbilityText(it, emptyList()).textCandidates()) })
