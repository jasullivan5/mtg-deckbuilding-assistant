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
    Objective("own-nontoken-creature-deaths", "Our nontoken creatures die", "Find direct death generation and separately classified death-enhanced removal."),
    Objective("reanimate-own-creatures", "Return our creatures", "Find direct movement of original creature cards from our graveyard to our battlefield."),
    Objective("create-creature-tokens", "Create creature tokens", "Find creature-token creation under our control, subject to type, quantity and controller prerequisites.")
)
@Serializable data class ResearchRequest(val commander: String, val objective: String? = null)
@Serializable data class ApiError(val code: String, val message: String, val inspection: CommanderInspection? = null)
@Serializable data class CardFinding(
    val index: Int, val card: String, val pathways: Map<String, Int>, val branches: List<Contribution>,
    val unsupportedRecords: Int, val partialPathways: Int, val reason: String?
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
@Serializable data class PathwayExplanation(val summary: String, val requirements: List<String>)

/** Readable restatement of the existing objective/verdict, not another mechanical evaluator. */
fun explain(result: ResultEvidence): PathwayExplanation {
    val potential = when (result.branch) {
        Contribution.DIRECT_DEATH_GENERATOR -> "This pathway can contribute to a nontoken creature you control dying, under its retained conditions."
        Contribution.DEATH_ENHANCED_REMOVAL -> "A retained death event may enable or improve this removal pathway."
        Contribution.REANIMATE_OWN_CREATURE -> "This pathway can move an original creature card from your graveyard to your battlefield, under its retained conditions."
        Contribution.CREATE_CREATURE_TOKEN -> "This pathway can create a creature token under your control, under its retained conditions."
    }
    val summary = when (result.verdict) {
        Verdict.MATCH -> potential
        Verdict.UNKNOWN -> "At least one requirement is unresolved. The application has not established that this pathway satisfies the objective."
        Verdict.NO_MATCH -> "This represented pathway does not meet at least one requirement of the objective. Other pathways on the card may differ."
    }
    val objectiveRequirements = when (result.branch) {
        Contribution.DIRECT_DEATH_GENERATOR -> listOf("A nontoken creature you control must be affected in a way that can cause its death.")
        Contribution.DEATH_ENHANCED_REMOVAL -> listOf("The retained death-event dependency must hold for the represented removal improvement.")
        Contribution.REANIMATE_OWN_CREATURE -> listOf("The original card must be a creature card in your graveyard.", "The represented movement must put that card onto your battlefield.")
        Contribution.CREATE_CREATURE_TOKEN -> listOf("The created object must be a creature token under your control.", "The native quantity must evaluate to a positive number when the action resolves.")
    }
    return PathwayExplanation(summary, objectiveRequirements + listOf(
        "The action's exact timing, costs, targets, choices and branch restrictions must hold; inspect the native prerequisites below.",
        "Resources and legal choices must exist. Replacements and other game-state effects can change the actual outcome."))
}
@Serializable data class CardEvidence(
    val runId: String, val card: String, val authoredCard: AuthoredCardText?,
    val results: List<ResultEvidence>, val actions: List<ActionEvidence>,
    val facts: List<MechanicalFact>, val unsupported: List<Unsupported>,
    val structuralSupport: List<StructuralSupport>, val eligibility: List<EligibilityEvidence>,
    val translationAssurance: List<TranslationAssurance>, val explanations: List<PathwayExplanation>
)
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
                    ?.let { explain(it).summary })
        }
    )
    val export = LiveDiscovery(runId = id, startedAt = started.toString(), durationMs = durationMs,
        applicationRevision = revision, engineMode = EngineBuild.mode, report = report)

    fun card(index: Int): CardEvidence? {
        val name = summary.cards.getOrNull(index)?.card ?: return null
        val native = catalog.card(name)
        return CardEvidence(id, name, native?.let { AuthoredCardText(it.manaCost.toString(), it.typeLine.toString(), it.oracleText) },
            results[name].orEmpty(), actions[name].orEmpty(), facts[name].orEmpty(), unsupported[name].orEmpty(),
            support[name].orEmpty(), eligibility[name].orEmpty(), assurance[name].orEmpty(), results[name].orEmpty().map(::explain))
    }
}
