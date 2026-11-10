package dev.mtg.assistant.explorer

import dev.mtg.assistant.Catalog
import dev.mtg.assistant.discovery.*
import kotlinx.serialization.Serializable

private const val COVERAGE_COMMANDER = "Judith, the Scourge Diva"
private const val COVERAGE_OBJECTIVE = "own-nontoken-creature-deaths"

@Serializable
data class CoverageSource(
    val card: String, val probe: String, val expectation: String, val diagnosis: String,
    val printingId: String, val oracleId: String, val oracleText: String,
    val sourceUrl: String, val snapshotSha256: String
)

@Serializable
internal data class CoverageSpec(val reviewedOn: String, val reviewedEngine: String, val cards: List<CoverageSource>)

internal val coverageSpec by lazy {
    reportJson.decodeFromString<CoverageSpec>(
        checkNotNull(CoverageSpec::class.java.getResource("/discovery-coverage-v1.json")).readText()
    ).also { spec -> check(spec.cards.distinctBy { it.oracleId }.size == spec.cards.size) }
}

@Serializable
enum class CoverageCategory {
    RECOGNIZED, UNRESOLVED, POLICY_EXCLUDED, OBJECTIVE_GAP, ORDINARY_BODY,
    INELIGIBLE, MISSING_DEFINITION, IDENTITY_UNRESOLVED, UNEXPLAINED
}

@Serializable
data class CoverageCard(
    val source: CoverageSource, val category: CoverageCategory, val explanation: String,
    val definitionState: String, val eligibility: EligibilityEvidence?, val index: Int?,
    val inFindings: Boolean, val relationshipChecks: Int, val cardChecks: Int, val unreadInstructions: Int
)

@Serializable
data class CoverageComparison(val reviewedOn: String, val reviewedEngine: String, val cards: List<CoverageCard>)

internal fun compareCoverage(
    catalog: Catalog, report: DiscoveryReport, findings: List<CardFinding>,
    results: Map<String, List<ResultEvidence>>, actions: Map<String, List<ActionEvidence>>,
    facts: Map<String, List<MechanicalFact>>, eligibility: Map<String, List<EligibilityEvidence>>
): CoverageComparison? {
    if (report.commander != COVERAGE_COMMANDER || report.objective != COVERAGE_OBJECTIVE ||
        report.conventionalQuery != null) return null

    val findingByName = findings.associateBy { it.card }
    val input = report.mechanicalInputCards.orEmpty().toSet()
    return CoverageComparison(coverageSpec.reviewedOn, coverageSpec.reviewedEngine, coverageSpec.cards.mapIndexed { offset, source ->
        val name = source.card
        val native = catalog.card(name)
        val bound = native != null && printingIdentity(native) == source.printingId
        val finding = findingByName[name].takeIf { bound }
        val rows = results[name].orEmpty().takeIf { bound }.orEmpty()
        val checked = rows.filter {
            (it.operation == source.probe && it.branch == Contribution.DIRECT_DEATH_GENERATOR) ||
                (it.branch==Contribution.INTRINSIC_CREATURE_INPUT && source.probe=="MOVE_OBJECT") ||
                (it.branch == Contribution.DEATH_TRIGGERED_OUTCOME && it.operation == when(source.probe) {
                    "DrawCards" -> "DRAW_CARDS"; "CREATE_TOKEN" -> "CREATE_TOKEN"; else -> ""
                })
        }
        val eligible = eligibility[name]?.singleOrNull().takeIf { bound }
        val reviewedEngine = report.argentumCommit == coverageSpec.reviewedEngine
        val category = when {
            native == null -> CoverageCategory.MISSING_DEFINITION
            !bound -> CoverageCategory.IDENTITY_UNRESOLVED
            eligible?.format == Verdict.NO_MATCH || eligible?.deck == Verdict.NO_MATCH -> CoverageCategory.INELIGIBLE
            eligible?.format != Verdict.MATCH || eligible.deck != Verdict.MATCH || name !in input -> CoverageCategory.UNEXPLAINED
            checked.any { it.verdict == Verdict.MATCH } -> CoverageCategory.RECOGNIZED
            checked.any { it.verdict == Verdict.UNKNOWN } -> CoverageCategory.UNRESOLVED
            checked.any { it.mechanicalCompatibility == Verdict.MATCH && it.policyAcceptance == Verdict.NO_MATCH } ->
                CoverageCategory.POLICY_EXCLUDED
            reviewedEngine && rows.isEmpty() && source.probe == "BODY" && !native.script.hasBehavior ->
                CoverageCategory.ORDINARY_BODY
            reviewedEngine && rows.isEmpty() && when (source.probe) {
                "DrawCards" -> actions[name].orEmpty().any { "DrawCards" in it.native.toString() }
                "CREATE_TOKEN", "MOVE_OBJECT" -> facts[name].orEmpty().any { it.kind.name == source.probe }
                else -> false
            } -> CoverageCategory.OBJECTIVE_GAP
            else -> CoverageCategory.UNEXPLAINED
        }
        val explanation = when (category) {
            CoverageCategory.RECOGNIZED -> if (checked.any { it.branch==Contribution.INTRINSIC_CREATURE_INPUT && it.verdict==Verdict.MATCH })
                "Current discovery recognizes intrinsic original-creature self-return as input supply. This return does not cause a death or prove a repeatable cycle; the retained diagnosis describes the earlier query."
                else if (checked.any { it.branch==Contribution.DEATH_TRIGGERED_OUTCOME && it.verdict==Verdict.MATCH })
                "Current discovery recognizes a qualifying death response and its same-ability outcome. The retained source diagnosis describes the earlier query."
                else if(reviewedEngine) source.diagnosis else "Inspect the current decision records. The retained diagnosis used a different engine revision."
            CoverageCategory.MISSING_DEFINITION -> "The catalog has no native definition under the reviewed name."
            CoverageCategory.IDENTITY_UNRESOLVED -> "The catalog printing could not be bound to the reviewed snapshot."
            CoverageCategory.UNEXPLAINED -> "The available records do not establish the outcome for this relationship."
            CoverageCategory.INELIGIBLE -> if (reviewedEngine && source.probe == "ELIGIBILITY") source.diagnosis else eligible!!.reason
            else -> if (reviewedEngine) source.diagnosis else "Inspect the current decision records. The retained diagnosis used a different engine revision."
        }
        CoverageCard(source, category, explanation, when {
            bound -> "Reviewed printing present"
            native == null -> "Definition missing"
            else -> "Printing association unresolved"
        }, eligible, if (bound) finding?.index ?: findings.size + offset else null,
            finding != null, checked.size, rows.size, finding?.unsupportedRecords ?: 0)
    })
}
