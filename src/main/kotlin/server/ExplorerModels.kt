package dev.mtg.assistant.explorer

import dev.mtg.assistant.Catalog
import dev.mtg.assistant.EngineBuild
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.extensions.TranslationAssurance
import dev.mtg.assistant.objectives.ConditionalArrival
import dev.mtg.assistant.objectives.GRAVEYARD_CREATURE_CRITERION
import dev.mtg.assistant.objectives.GRAVEYARD_CREATURE_OBJECTIVE
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable data class Objective(val id: String, val label: String, val explanation: String, val capabilities: List<String> = emptyList(), val coverageNote: String? = null)
val objectives = listOf(
    Objective("own-nontoken-creature-deaths", "My nontoken creatures die", "Find ways to produce your nontoken creatures' deaths, intrinsic creature-input replenishment, supported draw/token responses that benefit from deaths, and separately identified death-enhanced removal.",
        listOf("Produces deaths: supported sacrifice and own-source death operations.", "Supplies creature inputs: supported intrinsic original-creature self-return; continued availability is conditional, not a proven loop.", "Benefits from deaths: supported triggered card draw and creature or noncreature token creation, including Treasure.", "Death-enhanced removal: retained removal that a death triggers, enables or improves."),
        "Incoming death restrictions and same-ability branches must qualify. Conditions can remain uncertain. Input supply is bounded to original-creature self-return; ordinary bodies and generic reanimation are not intrinsic self-return suppliers. Other resources, life-change/mana payoffs, general replacement effects and composition are not implemented. Ordinary removal is still not admitted merely because it can target your creature."),
    Objective("reanimate-own-creatures", "Return our creatures", "Explore ways to bring creature cards from your graveyard onto your battlefield.", listOf("Return original creature cards from your graveyard to your battlefield.")),
    Objective("create-creature-tokens", "Create creature tokens", "Explore ways to create creature tokens you control, and the conditions they need.", listOf("Create one or more creature tokens under your control.", "Recognized variable amounts, with conditions for creating at least one token.")),
    Objective(DEATH_HISTORY_OBJECTIVE, "A creature died this turn", "Find abilities that check for a creature death earlier this turn and gain access to an effect or change its magnitude.",
        listOf("Benefits from death history: supported checks at resolution, activation or both triggering and resolution."),
        "A token or an opponent's creature can establish the broad history. Some abilities narrow whose creature counts. Direct dies triggers and ways to cause deaths are separate."),
    Objective(GRAVEYARD_CREATURE_OBJECTIVE, GRAVEYARD_CREATURE_CRITERION, "Find ways to put a creature card into your graveyard through supported native mechanics.",
        listOf("Sacrifice a creature.", "Mill cards from your library.", "Discard cards from your hand."),
        "The current search covers supported fixed-count routes. Actual card choices, replacement effects and the graveyard at the checkpoint depend on play.")
)
@Serializable data class ResearchRequest(val commander: String, val objective: String? = null)
@Serializable data class ApiError(val code: String, val message: String, val inspection: CommanderInspection? = null)
@Serializable data class CardFinding(
    val index: Int, val card: String, val pathways: Map<String, Int>, val branches: List<Contribution>,
    val unsupportedRecords: Int, val partialPathways: Int, val reason: String?, val mechanics: List<String> = emptyList(),
    val browsingState: String = "UNINTERPRETED", val recordedConcern: Boolean = false,
    val conditionalGraveyardContribution: Boolean = false
)
@Serializable data class RunSummary(
    val schemaVersion: Int = 1, val execution: String = "FRESH_STRUCTURAL_DISCOVERY",
    val runId: String, val startedAt: String, val durationMs: Long,
    val applicationRevision: String, val argentumCommit: String, val engineMode: String,
    val commander: String, val objective: String, val allowedIdentity: List<String>,
    val counts: Map<String, Int>, val pathwayCounts: Map<String, Int>, val limitations: List<String>,
    val cards: List<CardFinding>
)
@Serializable data class AuthoredCardText(val manaCost: String, val typeLine: String, val text: String)
@Serializable data class ArrivalInspection(
    val relationship: ConditionalArrival, val stateCriterion: String = GRAVEYARD_CREATURE_CRITERION,
    val mechanism: String? = null, val prerequisites: List<String> = emptyList(),
    val sources: List<String> = emptyList()
)
@Serializable data class CardEvidence(
    val runId: String, val card: String, val authoredCard: AuthoredCardText?,
    val results: List<ResultEvidence>, val actions: List<ActionEvidence>,
    val facts: List<MechanicalFact>, val unsupported: List<Unsupported>,
    val structuralSupport: List<StructuralSupport>, val eligibility: List<EligibilityEvidence>,
    val translationAssurance: List<TranslationAssurance>, val explanations: List<PathwayExplanation>,
    val abilities: List<AbilityInspection> = emptyList(), val evidenceSummary: List<EvidenceIndicator> = emptyList(),
    val concerns: List<ResearchConcern> = emptyList(), val rules: List<RulesLine> = emptyList(),
    val graveyardContributions: List<ArrivalInspection> = emptyList(),
    val labScenarioIds: List<String> = emptyList()
)
@Serializable data class CommanderView(
    val inspection: CommanderInspection, val authoredCard: AuthoredCardText?, val headline: String,
    val evidenceSummary: List<EvidenceIndicator>, val concerns: List<ResearchConcern>,
    val objectiveConnection: String? = null, val image: CardImage? = null, val rules: List<RulesLine> = emptyList()
)
fun commanderView(catalog: Catalog, name: String, inspection: CommanderInspection, objective: String = objectives.first().id,
                  image: CardImage? = null): CommanderView {
    val card = catalog.card(name)
    val colors = inspection.colorIdentity.map { it.lowercase() }
    val identity = if (!inspection.authoritativeIdentity) "Color identity unresolved" else if (colors.isEmpty()) "Colorless" else
        colors.joinToString(" and ").replaceFirstChar { it.uppercase() }
    return CommanderView(inspection, card?.let { AuthoredCardText(it.manaCost.toString(), it.typeLine.toString(), it.oracleText) },
        if (inspection.selectable) identity else "Selection blocked · $identity",
        evidenceIndicators(emptyList(), inspection.translationAssurance, inspection.scenarioReferences, "Commander card record")
            .filterNot { it.id == "mechanic" },
        materialConcerns(inspection.translationAssurance),
        card?.takeIf { inspection.selectable }?.let { commanderObjectiveConnection(it, objective) }, image,
        card?.let { commanderRulesLines(it, objective) }.orEmpty())
}
@Serializable data class LiveDiscovery(
    val schemaVersion: Int = 1, val execution: String = "FRESH_STRUCTURAL_DISCOVERY",
    val runId: String, val startedAt: String, val durationMs: Long,
    val applicationRevision: String, val engineMode: String, val report: DiscoveryReport
)

/** Presentation indexes refer to the unchanged report; they do not evaluate mechanics. */
class StoredRun(
    private val catalog: Catalog, val report: DiscoveryReport, started: Instant,
    durationMs: Long, revision: String
) {
    private val results = report.results.groupBy { it.card }
    private val unsupported = report.unsupported.groupBy { it.card }
    private val actions = report.actions.groupBy { it.card }
    private val facts = report.facts.groupBy { it.card }
    private val support = report.structuralSupport.groupBy { it.card }
    private val eligibility = report.eligibility.groupBy { it.card }
    private val assurance = report.translationAssurance.groupBy { it.card }
    private val arrivals = report.conditionalArrivals.groupBy { it.card }
    private val deathPayoffCommander = catalog.card(report.commander)?.takeIf {
        commanderObjectiveConnection(it, report.objective) != null
    }?.name
    val id: String = UUID.randomUUID().toString()
    val summary = RunSummary(
        runId = id, startedAt = started.toString(), durationMs = durationMs,
        applicationRevision = revision, argentumCommit = report.argentumCommit, engineMode = EngineBuild.mode,
        commander = report.commander, objective = report.objective, allowedIdentity = report.allowedIdentity,
        counts = report.counts, pathwayCounts = Verdict.entries.associate { v -> v.name to report.results.count { it.verdict == v } },
        limitations = report.limitations,
        cards = (results.keys + unsupported.keys).sorted().mapIndexed { index, card ->
            val rows = results[card].orEmpty()
            CardFinding(index, card, Verdict.entries.associate { v -> v.name to rows.count { it.verdict == v } },
                rows.map { it.branch }.distinct(), unsupported[card].orEmpty().size,
                rows.count { it.interpretation.startsWith("PARTIAL") || it.localSupport == SupportStatus.PARTIALLY_SUPPORTED },
                (rows.firstOrNull { it.verdict == Verdict.MATCH } ?: rows.firstOrNull { it.verdict == Verdict.UNKNOWN } ?: rows.firstOrNull())
                    ?.let { explain(it, actions[card].orEmpty().firstOrNull { a -> a.id == it.actionId }).summary },
                (if(report.objective == GRAVEYARD_CREATURE_OBJECTIVE && rows.any { it.verdict == Verdict.MATCH })
                    rows.filter { it.verdict == Verdict.MATCH } else rows).let { selected -> (selected.map(::mechanic)+selected.mapNotNull(::contributionRelationship)).distinct() },
                when { rows.any { it.verdict == Verdict.MATCH } -> "IDENTIFIED"
                    rows.any { it.verdict == Verdict.UNKNOWN } -> "UNRESOLVED"
                    unsupported[card].orEmpty().isNotEmpty() -> "UNINTERPRETED"
                    else -> "NOT_QUALIFYING" },
                assurance[card].orEmpty().any { materialConcerns(it).any { c -> c.state == EvidenceState.KNOWN_PROBLEM } },
                arrivals[card].orEmpty().any { it.decision.verdict == Verdict.MATCH })
        }
    )
    val coverage = compareCoverage(catalog, report, summary.cards, results, actions, facts, eligibility)
    private val coverageNames = coverage?.cards.orEmpty().mapNotNull { row -> row.index?.let { it to row.source.card } }.toMap()
    val export = LiveDiscovery(runId = id, startedAt = started.toString(), durationMs = durationMs,
        applicationRevision = revision, engineMode = EngineBuild.mode, report = report)

    private fun cardName(index: Int) = summary.cards.getOrNull(index)?.card ?: coverageNames[index]

    fun definition(index: Int) = cardName(index)?.let(catalog::card)

    fun card(index: Int): CardEvidence? {
        val name = cardName(index) ?: return null
        val native = catalog.card(name)
        val scenarios = catalog.definitionReview(name).scenarioReferences
        val review = catalog.translationAssurance(name)
        val abilities = abilityInspections(results[name].orEmpty(), actions[name].orEmpty(), unsupported[name].orEmpty(), review, scenarios, deathPayoffCommander = deathPayoffCommander)
        return CardEvidence(id, name, native?.let { AuthoredCardText(it.manaCost.toString(), it.typeLine.toString(), it.oracleText) },
            results[name].orEmpty(), actions[name].orEmpty(), facts[name].orEmpty(), unsupported[name].orEmpty(),
            support[name].orEmpty(), eligibility[name].orEmpty(), assurance[name].orEmpty(), results[name].orEmpty().map { result -> explain(result, actions[name].orEmpty().firstOrNull { it.id == result.actionId }) },
            abilities,
            evidenceIndicators(results[name].orEmpty(), review, scenarios, "Card-level discovery findings")
                .filterNot { it.id == "mechanic" && results[name].isNullOrEmpty() }, materialConcerns(review),
            rulesLines(name, native?.oracleText.orEmpty(), abilities),
            arrivals[name].orEmpty().map { arrival ->
                val fact = facts[name].orEmpty().firstOrNull { it.id == arrival.factId }
                val row = results[name].orEmpty().firstOrNull { it.actionId == arrival.actionId && it.abilityPath == arrival.path }
                inspectArrival(arrival, fact, row)
            }, labScenariosFor(native))
    }
}
