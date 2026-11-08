package dev.mtg.assistant.explorer

import dev.mtg.assistant.Catalog
import dev.mtg.assistant.EngineBuild
import dev.mtg.assistant.audit.CoverageReport
import dev.mtg.assistant.audit.coverage
import dev.mtg.assistant.coverage.*
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.extensions.*
import kotlinx.serialization.Serializable
import java.nio.file.Path
import java.time.Instant

@Serializable data class ProgressDistribution(val unit: String, val denominator: Int, val counts: Map<String, Int>)
@Serializable data class ProgressFamily(val nativeType: String, val occurrences: Int, val supported: Int)
@Serializable data class ProgressAssurance(val population: Int, val notMeasured: Int,
    val current: Int, val stale: Int, val missing: Int, val fidelity: Map<String, Int>, val qualification: Map<String, Int>,
    val records: List<TranslationAssurance>)
@Serializable data class ProgressExternal(val state: String, val reason: String, val snapshot: SnapshotManifest? = null,
    val commander: String = "Judith, the Scourge Diva", val counts: Map<String, Int> = emptyMap(),
    val exclusions: Map<String, Int> = emptyMap())
@Serializable data class ProgressObstacle(val id: String, val problem: String, val impact: String,
    val why: String, val examples: String, val investigation: String, val evidenceStatus: String, val records: String)
@Serializable data class ProgressCapability(val capability: String, val state: String, val limit: String)
@Serializable data class ResearchProgress(val schemaVersion: Int = 1, val applicationRevision: String,
    val argentumCommit: String = EngineBuild.revision, val measuredAt: String,
    val population: List<String>, val registeredNames: Int, val definitionOrigins: Map<String, Int>,
    val external: ProgressExternal, val mechanics: ProgressDistribution, val families: List<ProgressFamily>,
    val factKinds: Map<String, Int>, val factConfidence: Map<String, Int>,
    val assurance: ProgressAssurance, val execution: String,
    val obstacles: List<ProgressObstacle>, val capabilities: List<ProgressCapability>)

/** Keep every state disjoint. No objective match or card-level certification is inferred. */
internal fun progressDistribution(report: CoverageReport): ProgressDistribution {
    val counts = report.groups.groupBy { it.supportStatus }.mapValues { (_, rows) -> rows.sumOf { it.occurrences } }.toSortedMap()
    check(counts.values.sum() == report.encounteredTypedNodes)
    return ProgressDistribution("serialized native typed-node occurrences", report.encounteredTypedNodes, counts)
}

internal fun progressAssurance(population: Int, records: List<TranslationAssurance>): ProgressAssurance {
    require(records.map { it.card }.distinct().size == records.size && population >= records.size)
    val current = records.filter { it.evidenceCurrent }
    // Gate output is authoritative; stale original evidence is retained only for inspection.
    return ProgressAssurance(population, population - records.size, current.size,
        records.count { !it.evidenceCurrent && it.evidence != null }, records.count { it.evidence == null },
        FidelityState.entries.associate { state -> state.name to current.count { it.sourceFidelity == state } },
        QualificationState.entries.associate { state -> state.name to records.count { it.qualification == state } }, records)
}

/** Cached per server/build. Uses the existing walker and independent catalog audit, never another interpreter. */
internal class ResearchProgressService(private val catalog: Catalog, private val revision: String,
    private val snapshotPath: Path? = null) {
    private val physical by lazy { physicalCards(catalog.cards) }
    private val extracted by lazy { PathwayExtractor(inspectCardEntries = true).extract(physical) }
    val instructionRecords get() = extracted.structuralSupport.toList()
    val factRecords get() = extracted.facts.toList()
    val nativeCoverage: CoverageReport by lazy {
        val e = extracted
        val report = DiscoveryReport(objective = "catalog-structural-observation", commander = "No commander filter",
            allowedIdentity = emptyList(), conventionalQuery = null, counts = emptyMap(), eligibility = emptyList(),
            results = emptyList(), actions = e.actions, unsupported = e.unsupported, limitations = emptyList(),
            structuralSupport = e.structuralSupport, facts = e.facts, mechanicalInputCards = physical.map { it.name })
        coverage(physical, report)
    }
    // An explicit snapshot is optional. Failure is visible and never becomes a zero missing-card count.
    private val externalAttempt by lazy { snapshotPath?.let { runCatching {
        catalogCoverage(catalog, loadOracleSnapshot(it), "Judith, the Scourge Diva")
    } } }
    val externalRecords: CatalogCoverageReport? get() = externalAttempt?.getOrNull()
    val report: ResearchProgress by lazy {
        val audit = nativeCoverage
        val records = physical.mapNotNull { catalog.translationAssurance(it.name) }
        val assurance = progressAssurance(physical.size, records)
        val external = externalRecords?.let { ProgressExternal("MEASURED", "Explicit local snapshot; ordinary Judith-eligible physical identities. Association is not fidelity.",
            it.snapshot, it.commander, it.counts, it.poolExclusions) } ?: ProgressExternal(
            if (snapshotPath == null) "NOT_MEASURED" else "UNAVAILABLE",
            if (snapshotPath == null) "No external snapshot selected. Native inventory is measurable; a percentage of all Magic cards is not. Use the explicit snapshot workflow and launch with -OracleSnapshot."
            else "The selected snapshot could not be measured: ${externalAttempt?.exceptionOrNull()?.message}")
        val incomplete = audit.groups.filter { it.supportStatus != "SUPPORTED" }.sumOf { it.occurrences }
        ResearchProgress(applicationRevision = revision, measuredAt = Instant.now().toString(),
            population = physical.map { it.name }, registeredNames = catalog.cards.size,
            definitionOrigins = physical.groupingBy { catalog.provenance(it.name).origin }.eachCount().toSortedMap(),
            external = external, mechanics = progressDistribution(audit),
            families = audit.groups.groupBy { it.nativeType }.map { (type, groups) ->
                ProgressFamily(type, groups.sumOf { it.occurrences }, groups.filter { it.supportStatus == "SUPPORTED" }.sumOf { it.occurrences })
            }.sortedWith(compareByDescending<ProgressFamily> { it.supported }.thenBy { it.nativeType }),
            factKinds = extracted.facts.groupingBy { it.kind.name }.eachCount().toSortedMap(),
            factConfidence = Verdict.entries.associate { it.name to extracted.facts.count { f -> f.confidence == it } },
            assurance = assurance,
            execution = "Not measured at catalog scale. The extension ledger records card-level historical execution, without a scenario/ability outcome index. No scenario pass total is inferred. Curated Lab runs are on demand; retained observations keep their original engine revision. Missing evidence is not a failure. See the research index and source-sacrifice investigation for the retained Spark Elemental failure.",
            obstacles = listOf(
                ProgressObstacle("fidelity", "Inherited source fidelity is not established", "${assurance.notMeasured} native physical definitions outside the application assurance ledger; broader fidelity not measured.",
                    "Discovery can faithfully read a native definition that mistranslates Oracle requirements.", "Tatterkite retains a material concern in the extension ledger; inherited definitions receive no implied review.",
                    "Review one recurring native mechanic against a small source-bound cohort with positive, negative and held-out controls.", "Measured ledger scope; corpus fidelity unquantified", "assurance"),
                ProgressObstacle("mechanics", "Native structures remain partly read or unclassified", "$incomplete of ${audit.encounteredTypedNodes} typed nodes are not explicitly SUPPORTED. This includes unclassified metadata, not just missing mechanics.",
                    "Unresolved enclosing branches can limit an otherwise recognized operation. No emitted pathway is not a negative.", "Pit Raptor's nested pay-or-sacrifice branch; specialized token operations such as Amass/populate.",
                    "Inspect partial and unsupported groups, then source-review one recurring branch shape before extending extraction.", "Measured native audit; no Oracle recall claim", "mechanics"),
                ProgressObstacle("availability", "External cards may lack native definitions or confident identities",
                    if (external.state == "MEASURED") "${external.counts["native_definition_missing"]} missing definitions in ${external.counts["eligible_oracle_cards"]} Judith-eligible external identities; identity uncertainty is separate."
                    else "External missing/ambiguous population not measured without an explicit snapshot.",
                    "Absent definitions never enter mechanical discovery; uncertain associations cannot establish source coverage.", "Back faces and meld results are not additional physical cards. Missing and competing identity claims remain quarantined by the coverage audit.",
                    "Run the existing catalog-coverage audit against an explicit hash-checked snapshot; inspect missing and ambiguous records separately.", if (external.state == "MEASURED") "Measured within selected snapshot" else "Unquantified", "external"),
                ProgressObstacle("execution", "Reusable execution evidence has narrow reach", "Scenario-to-ability coverage at catalog scale is not measured.",
                    "Structural potential cannot establish behavior after object identity, targets or zones change.", "The source-sacrifice investigation retained Spark Elemental's old-trigger/new-object failure. Later Lab observations remain separately revision-bound.",
                    "Index existing scenario identities, mechanics, engine revisions and outcomes before attempting a catalog-wide count.", "Historically observed failure; breadth unquantified", "execution"),
                ProgressObstacle("context", "Discovery does not yet evaluate a deck slot in context", "System capability gap; affected-card count is not a meaningful unit.",
                    "A useful mechanical contribution can still be unaffordable or displace a better complementary card.", "Reassembling Skeleton plus Viscera Seer needs mana, timing and another card; discovery does not evaluate the package or its opportunity cost.",
                    "Specify one bounded cross-card dependency question using existing facts, with explicit resource and timing requirements.", "Documented capability limit", "capabilities")
            ), capabilities = listOf(
                ProgressCapability("Commander eligibility and source identity", "Implemented with limits", "Single fixed-identity commander; unknown metadata and physical associations remain explicit."),
                ProgressCapability("Mechanical representation and discovery", "Implemented with limits", "Four bounded objectives over authored native definitions; unsupported structures remain."),
                ProgressCapability("Source fidelity and engine verification", "Early research capability", "Extension ledger and bounded scenarios; inherited catalog fidelity not measured."),
                ProgressCapability("Composable objectives and cross-card dependencies", "Early research capability", "Shared facts and atomic queries; no general cross-card package analysis."),
                ProgressCapability("Resources, timing and complementary packages", "Not yet implemented", "Prerequisites are reported; affordability and whole-package feasibility are not solved."),
                ProgressCapability("Substitutions and displaced deck slots", "Not yet implemented", "No explicit opportunity-cost comparison."),
                ProgressCapability("Contextual deck value", "Not yet implemented", "Structural MATCH is not an inclusion recommendation."),
                ProgressCapability("Plan progress versus competitive position", "Not evaluated", "No opponent-relative evaluation."),
                ProgressCapability("Representative gameplay validation", "Not evaluated", "Bounded engine observations do not validate deck performance.")
            ))
    }
}
