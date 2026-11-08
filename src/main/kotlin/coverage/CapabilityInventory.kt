package dev.mtg.assistant.coverage

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CommanderEligibility
import com.wingedsheep.sdk.model.CreatureStats
import dev.mtg.assistant.Catalog
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.explorer.Objective
import dev.mtg.assistant.explorer.objectives
import kotlinx.serialization.Serializable

@Serializable data class CommanderCandidate(val oracleId: String?, val card: String, val sourceClass: String,
    val nativeName: String?, val association: String, val selection: String, val blockers: List<String>, val configurationFamilies: List<String>)
@Serializable data class CommanderInventory(val snapshotSha256: String, val engine: String, val counts: Map<String, Int>,
    val records: List<CommanderCandidate>, val unsupportedFamilies: List<String>,
    val eligibilityScope: String = "SDK-identifiable legendary creature or explicit commander-clause candidates in the legal physical source pool. This is a bounded candidate denominator, not all legal commander configurations.",
    val operationalScope: String = "Selection predicates measured for each identified candidate using the real application inspection gate. Full discovery is tested only on representative controls; no per-candidate operational-success count is claimed.",
    val limits: List<String> = listOf("Existing SDK eligibility is reused on a source type/text projection, not another rules parser. Unrecognized source forms are not proven ineligible.",
        "Current rules allow additional legendary Vehicle/Spacecraft forms; these are separately flagged for review and unsupported by the application's existing selection model.",
        "Characteristic-dependent exceptions such as Grist and future pre-game forms are not exhaustively classified. The full individual-commander denominator is not measured.",
        "Partner-bearing legendary creatures can be selectable alone; pair configurations remain unsupported. Family card counts overlap and are not configuration counts."))

/** Reuse the SDK's existing eligibility rule on source fields; never feed this projection into discovery. */
internal fun sourceCommanderClass(source: OracleIdentity): Pair<String, CardDefinition?> {
    return runCatching {
        val front = source.faces.firstOrNull()
        val type = TypeLine.parse(front?.typeLine ?: source.typeLine ?: error("Missing source type"))
        val projected = CardDefinition(source.name ?: error("Missing source name"), ManaCost.ZERO,
            type, oracleText=front?.oracleText ?: source.oracleText.orEmpty(),
            creatureStats=if(type.isCreature) CreatureStats.parse(front?.power ?: source.power ?: error("Missing source power"),front?.toughness ?: source.toughness ?: error("Missing source toughness")) else null)
        when {
            CommanderEligibility.isLegalCommander(projected) -> "SDK_IDENTIFIED_SINGLE_CANDIDATE" to projected
            CommanderEligibility.isBackground(projected) -> "BACKGROUND_COMPONENT" to projected
            projected.typeLine.supertypes.any { it.name == "LEGENDARY" } && projected.typeLine.subtypes.any { it.value in setOf("Vehicle","Spacecraft") } ->
                "SPECIAL_NONCREATURE_FORM_REVIEW" to projected
            else -> "NOT_IDENTIFIED_BY_BOUNDED_PREDICATE" to projected
        }
    }.getOrElse { "SOURCE_UNRESOLVED" to null }
}

fun commanderInventory(catalog: Catalog, global: CatalogCoverageReport): CommanderInventory {
    require(global.scope == "ALL_COMMANDER_LEGAL")
    val records = global.records.mapNotNull { row ->
        val source = row.external
        val (sourceClass, _) = sourceCommanderClass(source)
        if (sourceClass == "NOT_IDENTIFIED_BY_BOUNDED_PREDICATE") return@mapNotNull null
        val native = row.mapping.association?.takeIf { it.confidence == IdentityConfidence.CONFIDENT }?.native?.name?.let(catalog::card)
        val inspection = native?.let { inspectCommanderDefinition(it.name,it,catalog.provenance(it.name),true,
            catalog.definitionReview(it.name),catalog.eligibility.resolve(it),catalog.translationAssurance(it.name)) }
        val families = source.keywords.filter { it.lowercase() in setOf("partner","partner with","friends forever","choose a background","doctor's companion") }.toMutableList()
        if (sourceClass == "BACKGROUND_COMPONENT") families += "Background component"
        if (sourceClass == "SPECIAL_NONCREATURE_FORM_REVIEW") families += "Vehicle or Spacecraft form"
        if (inspection?.blockers?.any { it.code == CommanderBlocker.UNSUPPORTED_CONSTRUCTION } == true) families += "Unsupported pre-game or construction requirement"
        val selection = when {
            sourceClass != "SDK_IDENTIFIED_SINGLE_CANDIDATE" -> "OUTSIDE_IDENTIFIED_SINGLE_SCOPE"
            native == null -> if(row.status == CatalogStatus.NATIVE_DEFINITION_MISSING) "NATIVE_MISSING" else "ASSOCIATION_UNCERTAIN"
            inspection?.selectable == true && row.mapping.effectiveAgreement?.issues?.isEmpty() == true -> "SELECTION_GATE_PASSED"
            inspection?.blockers?.any { it.code == CommanderBlocker.UNSUPPORTED_CONSTRUCTION } == true -> "UNSUPPORTED_CONFIGURATION"
            else -> "UNRESOLVED_OR_BLOCKED"
        }
        CommanderCandidate(source.oracleId,source.name.orEmpty(),sourceClass,native?.name,
            row.mapping.association?.confidence?.name ?: row.status.name,selection,
            inspection?.blockers.orEmpty().map { "${it.code}: ${it.reason}" } + row.mapping.effectiveAgreement?.issues.orEmpty(),families.distinct())
    }
    val single = records.filter { it.sourceClass == "SDK_IDENTIFIED_SINGLE_CANDIDATE" }
    val counts = linkedMapOf("identified_single_candidates" to single.size,
        "confident_native_candidates" to single.count { it.nativeName != null },
        "selection_gate_passed" to single.count { it.selection == "SELECTION_GATE_PASSED" },
        "native_missing" to single.count { it.selection == "NATIVE_MISSING" },
        "association_uncertain" to single.count { it.selection == "ASSOCIATION_UNCERTAIN" },
        "unsupported_configuration" to single.count { it.selection == "UNSUPPORTED_CONFIGURATION" },
        "unresolved_or_blocked" to single.count { it.selection == "UNRESOLVED_OR_BLOCKED" },
        "special_noncreature_forms_for_review" to records.count { it.sourceClass == "SPECIAL_NONCREATURE_FORM_REVIEW" },
        "background_components" to records.count { it.sourceClass == "BACKGROUND_COMPONENT" },
        "source_unresolved" to records.count { it.sourceClass == "SOURCE_UNRESOLVED" })
    check(listOf("selection_gate_passed","native_missing","association_uncertain","unsupported_configuration","unresolved_or_blocked").sumOf { counts.getValue(it) } == single.size)
    return CommanderInventory(global.snapshot.sha256,global.argentumCommit,counts,records,
        listOf("Partner / Partner with / Friends forever pair configurations", "Choose a Background plus Background configurations",
            "Doctor's companion and other paired commanders", "Pre-game chosen identity", "Special noncreature forms not admitted by the current selection gate"))
}

@Serializable data class ObjectiveCardReach(val card: String, val matches: Int, val unknowns: Int, val nonmatches: Int,
    val unsupportedRecords: Int, val relevantLocations: List<String>)
@Serializable data class ObjectiveReach(val definition: Objective, val counts: Map<String, Int>, val records: List<ObjectiveCardReach>, val implementation: String)
@Serializable data class ObjectiveInventory(val commander: String, val engine: String, val population: List<String>,
    val objectives: List<ObjectiveReach>, val correctness: String = "Not measured at catalog scale. Existing source-led regression examples are bounded and author-led; operational reach is not independent precision or recall.",
    val overlap: String = "Distinct card categories overlap. Pathways and unsupported records are different units. No emitted relevant pathway is not NO_MATCH.",
    val expressionLimits: List<String> = listOf("General death-response payoffs", "Cross-card dependencies and complementary packages", "Affordability and timing satisfiability",
        "Repeated or renewable capabilities as general queries", "Substitutions and displaced deck slots", "Competitive-position relevance"))

fun objectiveReach(report: DiscoveryReport, definition: Objective): ObjectiveReach {
    val input = requireNotNull(report.mechanicalInputCards)
    val results = report.results.groupBy { it.card }; val unsupported = report.unsupported.groupBy { it.card }
    val records = input.map { name ->
        val rows = results[name].orEmpty()
        ObjectiveCardReach(name,rows.count { it.verdict == Verdict.MATCH },rows.count { it.verdict == Verdict.UNKNOWN },
            rows.count { it.verdict == Verdict.NO_MATCH },unsupported[name].orEmpty().size,
            (rows.map { "${it.face}|${it.actionId}|${it.abilityPath}" } + unsupported[name].orEmpty().map { "${it.face}|${it.actionId}|${it.path}" }).distinct())
    }
    val counts = linkedMapOf("eligible_physical_cards_examined" to input.size,
        "distinct_matching_cards" to records.count { it.matches > 0 }, "distinct_unknown_cards" to records.count { it.unknowns > 0 },
        "distinct_nonmatch_cards" to records.count { it.nonmatches > 0 }, "distinct_cards_with_unsupported_records" to records.count { it.unsupportedRecords > 0 },
        "cards_without_emitted_relevant_pathway" to records.count { it.matches + it.unknowns + it.nonmatches == 0 },
        "matching_pathways" to report.results.count { it.verdict == Verdict.MATCH }, "unknown_pathways" to report.results.count { it.verdict == Verdict.UNKNOWN },
        "nonmatch_pathways" to report.results.count { it.verdict == Verdict.NO_MATCH }, "unsupported_records" to report.unsupported.size)
    val implementation = when (report.objective) {
        "own-nontoken-creature-deaths" -> "Legacy pathway projection and MechanicalQuery; shares the native walker, not fully fact-driven. discovery/Discovery.kt"
        "reanimate-own-creatures" -> "Consumes shared MechanicalFact records through reanimationFacts and FactQuery. discovery/MechanicalFacts.kt and Reanimation.kt"
        "create-creature-tokens" -> "Consumes shared MechanicalFact records through token predicates. discovery/TokenFacts.kt and Reanimation.kt"
        else -> "Consumes shared MechanicalFact records for source-bound sacrifice, mill and discard. objectives/GraveyardDiscovery.kt"
    }
    return ObjectiveReach(definition,counts,records,implementation)
}

fun objectiveInventory(catalog: Catalog, commander: String = "Judith, the Scourge Diva"): ObjectiveInventory {
    var population: List<String>? = null
    val reports = objectives.map { definition ->
        val report = discover(catalog,commander,objective=definition.id)
        if(population == null) population = report.mechanicalInputCards else check(population == report.mechanicalInputCards)
        objectiveReach(report,definition)
    }
    return ObjectiveInventory(commander,ARGENTUM_SHA,requireNotNull(population),reports)
}
