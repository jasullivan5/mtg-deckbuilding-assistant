package dev.mtg.assistant.coverage

import dev.mtg.assistant.Catalog
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.explorer.SacrificePilot
import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@Serializable data class GlobalBaselineEntry(val oracleId: String, val card: String, val legality: String,
    val poolState: String, val association: String, val nativeName: String?, val origin: String?, val metadataIssues: List<String>)
@Serializable data class GlobalBaseline(val schemaVersion: Int = 1, val measurementDefinition: String = "global-commander-baseline-v1",
    val applicationRevision: String, val engine: String, val snapshot: SnapshotManifest, val identityAlgorithm: String,
    val nativeIdentitySha256: String, val populationPolicy: String, val counts: Map<String,Int>, val exclusions: Map<String,Int>,
    val records: List<GlobalBaselineEntry>, val unresolvedWithoutOracleId: Int, val commanders: CommanderInventory,
    val objectiveScope: ObjectiveInventory, val pilotRequirementsSha256: String,
    val limits: List<String> = listOf("Availability only, not source-fidelity, execution or independent qualification.",
        "Single-candidate inventory uses the existing bounded SDK predicate; full legal commander/configuration denominator is not measured.",
        "Objective reach is a separate Judith-native population with overlapping categories; correctness is not measured at catalog scale.",
        "Historical Judith comparisons use different revisions and remain separate from this first global baseline."))

fun globalBaseline(catalog: Catalog, snapshot: OracleSnapshot, applicationRevision: String): GlobalBaseline {
    val report = catalogCoverage(catalog,snapshot,null)
    val mapped = report.records.associateBy { it.external.oracleId }
    val entries = snapshot.cards.filter { it.oracleId != null }.map { source ->
        val pool=externalEligibility(source,null); val row=mapped[source.oracleId]
        val association=row?.mapping?.association
        GlobalBaselineEntry(source.oracleId!!,source.name.orEmpty(),source.legalities["commander"] ?: "UNKNOWN",pool.state.name,
            association?.confidence?.name ?: row?.status?.name ?: "OUTSIDE_LEGAL_POOL",
            association?.takeIf { it.confidence == IdentityConfidence.CONFIDENT }?.native?.name,
            association?.takeIf { it.confidence == IdentityConfidence.CONFIDENT }?.native?.origin,
            row?.mapping?.effectiveAgreement?.issues.orEmpty())
    }.sortedBy { it.oracleId }
    val objectives=objectiveInventory(catalog).let { it.copy(objectives=it.objectives.map { objective -> objective.copy(records=emptyList()) }) }
    return GlobalBaseline(applicationRevision=applicationRevision,engine=ARGENTUM_SHA,snapshot=snapshot.manifest,
        identityAlgorithm=report.enrichment!!.algorithm,nativeIdentitySha256=report.enrichment.nativeIdentitySha256,
        populationPolicy=report.populationPolicy,counts=report.counts,exclusions=report.poolExclusions,records=entries,
        unresolvedWithoutOracleId=snapshot.cards.count { it.oracleId == null },commanders=commanderInventory(catalog,report),
        objectiveScope=objectives,pilotRequirementsSha256=SacrificePilot.requirementsHash)
}

@Serializable data class GlobalCoverageChange(val beforeSnapshot: String, val afterSnapshot: String,
    val beforeEngine: String, val afterEngine: String, val comparableDefinitions: Boolean, val definitionChanges: List<String>,
    val sameSourceUniverse: Boolean, val newlyAddedLegalCards: List<String>, val existingCardsNewlyLegal: List<String>,
    val noLongerLegal: List<String>, val newlyIntegratedExistingCards: List<String>, val resolvedUncertainAssociations: List<String>,
    val lostCoverageWithinStillLegalPopulation: List<String>, val absentSourceRecords: List<String>,
    val limits: String = "Universe growth and legality changes are separate from availability gains. Revision differences are retained, not normalized away. No engine correctness or fidelity gain is inferred.")

fun compareGlobalBaselines(before: GlobalBaseline, after: GlobalBaseline): GlobalCoverageChange {
    fun valid(b: GlobalBaseline) {
        require(b.records.map { it.oracleId }.distinct().size == b.records.size) { "Duplicate baseline physical identity" }
        require(b.records.count { it.poolState == "ELIGIBLE" } == b.counts.getValue("eligible_oracle_cards")) { "Baseline legal denominator disagrees with records" }
        require(b.records.count { it.poolState == "ELIGIBLE" && it.association == "CONFIDENT" } == b.counts.getValue("confident_native_identity_associations")) { "Baseline association numerator disagrees with records" }
    }
    valid(before); valid(after)
    val old=before.records.associateBy { it.oracleId }; val now=after.records.associateBy { it.oracleId }
    val changes=buildList {
        if(before.schemaVersion != after.schemaVersion) add("Baseline schema changed")
        if(before.measurementDefinition != after.measurementDefinition) add("Measurement definition changed")
        if(before.populationPolicy != after.populationPolicy) add("Physical population policy changed")
        if(before.identityAlgorithm != after.identityAlgorithm) add("Identity matching algorithm changed")
        if(before.snapshot.sha256 == after.snapshot.sha256 && (old.keys != now.keys || before.records.any { prior -> now[prior.oracleId]?.let { it.legality != prior.legality || it.poolState != prior.poolState } == true }))
            add("Same source hash but different source identity population, legality or physical roles")
    }
    fun names(filter: (GlobalBaselineEntry)->Boolean)=after.records.filter(filter).map { it.card }.sorted()
    val comparable=changes.isEmpty()
    return GlobalCoverageChange(before.snapshot.sha256,after.snapshot.sha256,before.engine,after.engine,comparable,changes,
        before.snapshot.sha256 == after.snapshot.sha256,
        names { it.poolState == "ELIGIBLE" && it.oracleId !in old },
        names { it.poolState == "ELIGIBLE" && old[it.oracleId]?.poolState?.let { p -> p != "ELIGIBLE" } == true },
        before.records.filter { it.poolState == "ELIGIBLE" && now[it.oracleId]?.poolState?.let { p -> p != "ELIGIBLE" } == true }.map { it.card }.sorted(),
        if(comparable) names { it.poolState == "ELIGIBLE" && it.association == "CONFIDENT" && old[it.oracleId]?.let { prior -> prior.poolState == "ELIGIBLE" && prior.association in setOf("MISSING","NATIVE_DEFINITION_MISSING") } == true } else emptyList(),
        if(comparable) names { it.poolState == "ELIGIBLE" && it.association == "CONFIDENT" && old[it.oracleId]?.association in setOf("AMBIGUOUS","CONFLICT","REVIEW_REQUIRED") } else emptyList(),
        if(comparable) names { it.poolState == "ELIGIBLE" && it.association != "CONFIDENT" && old[it.oracleId]?.let { prior -> prior.poolState == "ELIGIBLE" && prior.association == "CONFIDENT" } == true } else emptyList(),
        before.records.filter { it.oracleId !in now }.map { it.card }.sorted())
}

fun readGlobalBaseline(path: Path): GlobalBaseline = Files.newInputStream(path).let { input ->
    (if(path.toString().endsWith(".gz")) GZIPInputStream(input) else input).bufferedReader(Charsets.UTF_8).use { reportJson.decodeFromString(it.readText()) }
}

/** Explicit immutable-baseline and bounded-pilot CLI. No automatic source fetch or overwrite. */
fun researchCommand(args: List<String>, catalog: () -> Catalog, out: (String)->Unit) {
    val command=args.firstOrNull(); val options=args.drop(1).chunked(2).associate { pair ->
        require(pair.size == 2 && pair[0].startsWith("--")); pair[0] to pair[1]
    }
    require(options.size*2 == args.size-1) { "Duplicate research option" }
    fun required(key: String)=requireNotNull(options[key]) { "$key required" }
    val output=Path.of(required("--output")); require(!Files.exists(output)) { "Research records are not silently replaced; choose a new output path" }
    val revision=Properties().run { GlobalBaseline::class.java.getResourceAsStream("/application-build.properties")!!.use(::load);getProperty("revision") }
    val text = when(command) {
        "baseline" -> { require(options.keys == setOf("--oracle-cards","--output")); reportJson.encodeToString(GlobalBaseline.serializer(),globalBaseline(catalog(),loadOracleSnapshot(Path.of(required("--oracle-cards"))),revision)) }
        "compare" -> { require(options.keys == setOf("--before","--after","--output")); reportJson.encodeToString(GlobalCoverageChange.serializer(),compareGlobalBaselines(readGlobalBaseline(Path.of(required("--before"))),readGlobalBaseline(Path.of(required("--after"))))) }
        "fidelity-pilot" -> { require(options.keys == setOf("--output")); reportJson.encodeToString(dev.mtg.assistant.explorer.PilotReceipt.serializer(),SacrificePilot.execute(catalog(),revision)) }
        "fidelity-challenge" -> { require(options.keys == setOf("--output")); reportJson.encodeToString(dev.mtg.assistant.explorer.PilotReceipt.serializer(),dev.mtg.assistant.explorer.SacrificeChallenge.execute(catalog(),revision)) }
        "verification-baseline" -> { require(options.keys == setOf("--output")); reportJson.encodeToString(VerificationCoverage.serializer(),dev.mtg.assistant.explorer.ResearchProgressService(catalog(),revision).verification) }
        "verification-compare" -> {
            require(options.keys == setOf("--before","--after","--output"))
            fun read(key:String):VerificationCoverage = Files.newInputStream(Path.of(required(key))).let { input ->
                (if(required(key).endsWith(".gz")) GZIPInputStream(input) else input).bufferedReader().use { reportJson.decodeFromString<VerificationCoverage>(it.readText()) }
            }
            reportJson.encodeToString(VerificationChange.serializer(),compareVerification(read("--before"),read("--after")))
        }
        else -> error("Usage: research {baseline --oracle-cards <snapshot>|compare --before <baseline> --after <baseline>|fidelity-pilot} --output <new path>")
    }
    output.toAbsolutePath().parent?.let(Files::createDirectories)
    Files.newOutputStream(output,java.nio.file.StandardOpenOption.CREATE_NEW,java.nio.file.StandardOpenOption.WRITE).let { if(output.toString().endsWith(".gz")) GZIPOutputStream(it) else it }.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
    out("Wrote $output")
}
