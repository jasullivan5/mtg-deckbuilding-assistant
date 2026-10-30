package dev.mtg.assistant.explorer

import dev.mtg.assistant.Catalog
import dev.mtg.assistant.EngineBuild
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.extensions.TranslationAssurance
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable data class Objective(val id: String, val label: String, val explanation: String)
val objectives = listOf(
    Objective("own-nontoken-creature-deaths", "My nontoken creatures die", "Explore sacrifice and other ways your nontoken creatures could die, plus removal enabled by a death."),
    Objective("reanimate-own-creatures", "Return our creatures", "Explore ways to bring creature cards from your graveyard onto your battlefield."),
    Objective("create-creature-tokens", "Create creature tokens", "Explore ways to create creature tokens you control, and the conditions they need.")
)
@Serializable data class ResearchRequest(val commander: String, val objective: String? = null)
@Serializable data class ApiError(val code: String, val message: String, val inspection: CommanderInspection? = null)
@Serializable data class CardFinding(
    val index: Int, val card: String, val pathways: Map<String, Int>, val branches: List<Contribution>,
    val unsupportedRecords: Int, val partialPathways: Int, val reason: String?, val mechanics: List<String> = emptyList(),
    val browsingState: String = "UNINTERPRETED", val recordedConcern: Boolean = false
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
@Serializable data class CardEvidence(
    val runId: String, val card: String, val authoredCard: AuthoredCardText?,
    val results: List<ResultEvidence>, val actions: List<ActionEvidence>,
    val facts: List<MechanicalFact>, val unsupported: List<Unsupported>,
    val structuralSupport: List<StructuralSupport>, val eligibility: List<EligibilityEvidence>,
    val translationAssurance: List<TranslationAssurance>, val explanations: List<PathwayExplanation>,
    val abilities: List<AbilityInspection> = emptyList(), val evidenceSummary: List<EvidenceIndicator> = emptyList(),
    val concerns: List<ResearchConcern> = emptyList()
)
@Serializable data class CommanderView(
    val inspection: CommanderInspection, val authoredCard: AuthoredCardText?, val headline: String,
    val evidenceSummary: List<EvidenceIndicator>, val concerns: List<ResearchConcern>
)
fun commanderView(catalog: Catalog, name: String, inspection: CommanderInspection): CommanderView {
    val card = catalog.card(name)
    val colors = inspection.colorIdentity.map { it.lowercase() }
    val identity = if (!inspection.authoritativeIdentity) "Color identity unresolved" else if (colors.isEmpty()) "Colorless" else
        colors.joinToString(" and ").replaceFirstChar { it.uppercase() }
    return CommanderView(inspection, card?.let { AuthoredCardText(it.manaCost.toString(), it.typeLine.toString(), it.oracleText) },
        if (inspection.selectable) "${inspection.requestedName} can be your commander · $identity" else "Selection blocked · $identity",
        evidenceIndicators(emptyList(), inspection.translationAssurance, inspection.scenarioReferences, "Commander card record; no objective-specific run in this inspection"),
        materialConcerns(inspection.translationAssurance))
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
                    ?.let { explain(it, actions[card].orEmpty().firstOrNull { a -> a.id == it.actionId }).summary }, rows.map(::mechanic).distinct(),
                when { rows.any { it.verdict == Verdict.MATCH } -> "IDENTIFIED"
                    rows.any { it.verdict == Verdict.UNKNOWN } -> "UNRESOLVED"
                    unsupported[card].orEmpty().isNotEmpty() -> "UNINTERPRETED"
                    else -> "NOT_QUALIFYING" },
                assurance[card].orEmpty().any { materialConcerns(it).any { c -> c.state == EvidenceState.KNOWN_PROBLEM } })
        }
    )
    val export = LiveDiscovery(runId = id, startedAt = started.toString(), durationMs = durationMs,
        applicationRevision = revision, engineMode = EngineBuild.mode, report = report)

    fun definition(index: Int) = summary.cards.getOrNull(index)?.card?.let(catalog::card)

    fun card(index: Int): CardEvidence? {
        val name = summary.cards.getOrNull(index)?.card ?: return null
        val native = catalog.card(name)
        val scenarios = catalog.definitionReview(name).scenarioReferences
        val review = catalog.translationAssurance(name)
        return CardEvidence(id, name, native?.let { AuthoredCardText(it.manaCost.toString(), it.typeLine.toString(), it.oracleText) },
            results[name].orEmpty(), actions[name].orEmpty(), facts[name].orEmpty(), unsupported[name].orEmpty(),
            support[name].orEmpty(), eligibility[name].orEmpty(), assurance[name].orEmpty(), results[name].orEmpty().map { result -> explain(result, actions[name].orEmpty().firstOrNull { it.id == result.actionId }) },
            abilityInspections(results[name].orEmpty(), actions[name].orEmpty(), unsupported[name].orEmpty(), review, scenarios),
            evidenceIndicators(results[name].orEmpty(), review, scenarios, "Card-level discovery findings"), materialConcerns(review))
    }
}
