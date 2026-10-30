package dev.mtg.assistant.audit

import dev.mtg.assistant.discovery.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.security.MessageDigest

@Serializable enum class DiagnosticCategory { PROBABLE_EXTRACTION_OMISSION, POSSIBLE_POLICY_FALSE_NEGATIVE,
    INCOMPLETE_INTERPRETATION, UNSUPPORTED_BUT_LIKELY_UNRELATED, ELIGIBILITY_DATA_PROBLEM,
    UNVERIFIED_WARNING, EXPECTED_POLICY_EXCLUSION, CONFIRMED_BENCHMARK_MISMATCH,
    MISSING_MECHANICAL_FACT, MISSING_OBJECTIVE_INTERPRETATION, UNRESOLVED_OWNERSHIP_OR_CONTROL }
@Serializable data class DiagnosticRef(val findingIndex: Int,val card: String,val face: String,val path: String,val actionId: String?,val sourceReferences: List<String> = emptyList())
@Serializable data class PriorityEvidence(val confirmedBenchmarkCases: List<String>,val benchmarkCases: List<String>,
    val acceptedActions: Int,val mechanicallyCompatibleRejectedActions: Int,val relevantNativeContext: Boolean,
    val genericInterpretationFeasibility: String,val rankingRule: String)
@Serializable data class RootIssue(val fingerprint: String,val category: DiagnosticCategory,val nativeType: String,
    val context: String,val stage: String,val structuralPattern: JsonElement,val reason: String,
    val recordCount: Int,val distinctCards: Int,val distinctActions: Int,val priorityBand: String,
    val evidence: PriorityEvidence,val proposedFix: String,val uncertainty: String,
    val representatives: List<DiagnosticRef>,val affected: List<DiagnosticRef>)
private fun rootHash(value: String)=MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }.take(20)
private fun type(o: JsonElement?): String?=(o as? JsonObject)?.get("type")?.jsonPrimitive?.contentOrNull
private fun hasDeathContext(a: ActionEvidence?): Boolean {
    if(a==null) return false
    fun walk(v: JsonElement): Boolean = when(v) {
        is JsonObject -> type(v) in setOf("CreatureDiedThisTurn","ControlledCreatureDiedThisTurn","CreatureDiedThisTurnMatching") ||
            (type(v)=="ZoneChangeEvent" && v["from"]?.jsonPrimitive?.contentOrNull=="Battlefield" && v["to"]?.jsonPrimitive?.contentOrNull=="Graveyard") || v.values.any(::walk)
        is JsonArray -> v.any(::walk)
        else -> false
    }
    return walk(a.native)
}
/** Diagnostic proof is deliberately narrower than the extractor; unknown is never an omission claim. */
private fun unrelatedNode(n: JsonObject,action: ActionEvidence?): Boolean {
    val t=type(n)
    val target=n["target"]
    val filter=n["filter"] as? JsonObject
    val base=(filter?.get("baseFilter") as? JsonObject) ?: filter
    if(type(base?.get("controllerPredicate")) in setOf("ControlledByOpponent","ControlledByTargetOpponent")) return true
    if((base?.get("cardPredicates") as? JsonArray).orEmpty().any { type(it)=="IsToken" }) return true
    if(t in setOf("DealDamage","LoseLife") && type(target) in setOf("Controller","PlayerRef")) return true
    if(t in setOf("GainLife","Scry","ShuffleLibrary","EmitLibrarySearchedEvent","AttachEquipment","DrawCards","AddMana","AddColorlessMana","CreateToken","CreatePredefinedToken","GrantKeyword")) return true
    if(t=="ModifyStats") {
        val modifier=(n["toughnessModifier"] as? JsonObject)
        if(modifier==null && (n["toughnessBonus"] as? JsonPrimitive)?.intOrNull?.let { it>=0 }==true) return true
        if(type(modifier)=="Fixed" && modifier?.get("amount")?.jsonPrimitive?.intOrNull?.let { it>=0 }==true) return true
        if(type(modifier)=="AggregateBattlefield" && (modifier?.get("aggregation")?.jsonPrimitive?.contentOrNull ?: "COUNT")=="COUNT") return true
    }
    if(t in setOf("MoveToZone","MoveCollection") && !hasDeathContext(action)) {
        val destination=n["destination"]
        val zone=(destination as? JsonPrimitive)?.contentOrNull ?: (destination as? JsonObject)?.get("zone")?.jsonPrimitive?.contentOrNull
        if(zone in setOf("Hand","Library","Battlefield","Command")) return true
        if(n["fromZone"]?.jsonPrimitive?.contentOrNull in setOf("Graveyard","Hand","Library","Exile")) return true
    }
    return false
}
internal fun nativeAt(action: ActionEvidence?,path: String): JsonElement? {
    if(action==null) return null
    val relative=if(action.path in setOf("script","script.spell","script.unsupported")) path.removePrefix("script.") else path.removePrefix(action.path).removePrefix(".")
    var current: JsonElement=action.native
    if(relative.isEmpty()) return current
    for(part in relative.split('.')) {
        val field=part.substringBefore('[')
        if(field.isNotEmpty()) current=(current as? JsonObject)?.get(field) ?: return null
        Regex("\\[(\\d+)\\]").findAll(part).forEach { m -> current=(current as? JsonArray)?.getOrNull(m.groupValues[1].toInt()) ?: return null }
    }
    return current
}
private fun probableSacrifice(node: JsonObject?,action: ActionEvidence?,path: String,printedCreature: Boolean): Boolean {
    if(node==null || action==null || action.path in setOf("script","script.unsupported") || path.contains("grant",true)) return false
    if(action.path.contains("staticAbilities")) return false
    val body=action.native as? JsonObject
    if(body?.get("activateFromZone")?.jsonPrimitive?.contentOrNull?.let { it!="Battlefield" }==true) return false
    if(body?.get("activeZone")?.jsonPrimitive?.contentOrNull?.let { it!="Battlefield" }==true) return false
    val activeZones=body?.get("activeZones") as? JsonArray
    if(activeZones!=null && activeZones.none { it.jsonPrimitive.contentOrNull=="Battlefield" }) return false
    val trigger=body?.get("trigger") as? JsonObject
    val binding=(action.native as? JsonObject)?.get("binding")?.jsonPrimitive?.contentOrNull ?: "SELF"
    if(binding=="SELF" && trigger?.get("from")?.jsonPrimitive?.contentOrNull=="Battlefield" && trigger["to"]?.jsonPrimitive?.contentOrNull!="Battlefield") return false
    if(type(node)=="SacrificeSelf") return printedCreature
    if(type(node) !in setOf("AtomSacrifice","Sacrifice","SacrificeTarget","ForceSacrifice")) return false
    val filter=node["filter"] as? JsonObject
    val creature=(filter?.get("cardPredicates") as? JsonArray).orEmpty().any { type(it)=="IsCreature" }
    // Effect-payment payer and arbitrary player-iteration bindings require a separate proof.
    if(type(node)=="AtomSacrifice" && path.contains(".effect")) return false
    return creature && !unrelatedNode(node,action)
}
fun rootIssues(findings: List<Finding>,report: DiscoveryReport,benchmark: BenchmarkReport?=null,
               authoredCards: List<com.wingedsheep.sdk.model.CardDefinition> = emptyList()): List<RootIssue> {
    val printedCreatures=authoredCards.filter { it.isCreature }.map { it.name }.toSet()
    val sourcesByCard=report.results.groupBy { it.card }.mapValues { (_,rs) -> rs.flatMap { it.sourceReferences }.distinct() }
    val actions=report.actions.associateBy { it.id }
    val results=report.results.groupBy { it.actionId }
    data class Tagged(val ref: DiagnosticRef,val category: DiagnosticCategory,val t: String,val context: String,
        val stage: String,val shape: JsonElement,val reason: String,val benchmarkIds: List<String>,val confirmed: List<String>)
    val tagged=findings.mapIndexed { index,f ->
        val a=actions[f.actionId]; val matching=results[f.actionId].orEmpty().filter { it.abilityPath==f.path }
        val result=matching.firstOrNull()
        val obj=f.evidence as? JsonObject
        val operation=(obj?.get("pathway") as? JsonObject)?.get("nativeOperation") ?: result?.nativeOperation
        val unsupported=(obj?.get("unsupported") as? JsonArray)?.map { it.jsonObject }?.sortedBy { u ->
            if(u["nativeType"]?.jsonPrimitive?.contentOrNull?.substringAfterLast('.') in setOf("GainLifeEffect","CreateTokenEffect","GrantKeywordEffect")) 1 else 0
        }?.firstOrNull()
        val native=if(f.check=="B_INCOMPLETE_ACCEPTED_ACTION" && unsupported!=null)
            nativeAt(actions[unsupported["actionId"]?.jsonPrimitive?.contentOrNull] ?: a,unsupported["path"]?.jsonPrimitive?.contentOrNull ?: "") ?: buildJsonObject { put("type",unsupported["nativeType"] ?: JsonPrimitive("Unknown")) }
            else obj?.get("nativePattern") ?: operation ?: f.evidence
        val t=type(native) ?: unsupported?.get("nativeType")?.jsonPrimitive?.contentOrNull?.substringAfterLast('.') ?: f.check
        val bare=native as? JsonObject
        val bench=benchmark?.cases.orEmpty().filter { row -> row.card==f.card &&
            (row.actual.any { r -> r.actionId==f.actionId && (r.abilityPath==f.path || f.path.startsWith(row.expected.path)) } || f.path.startsWith(row.expected.path) || row.expected.path.startsWith(f.path)) }
        val confirmed=bench.filter { it.status=="CONFIRMED_MISMATCH" }.map { it.id }
        val category=when {
            f.check=="E_ELIGIBILITY_DATA_GAP" -> DiagnosticCategory.ELIGIBILITY_DATA_PROBLEM
            f.check=="F_BENCHMARK_MISMATCH" && f.severity=="CONFIRMED_MISMATCH" -> DiagnosticCategory.CONFIRMED_BENCHMARK_MISMATCH
            bare!=null && report.objective=="own-nontoken-creature-deaths" && unrelatedNode(bare,a) -> DiagnosticCategory.UNSUPPORTED_BUT_LIKELY_UNRELATED
            report.objective=="own-nontoken-creature-deaths" && f.check=="A_POTENTIAL_MISS" && probableSacrifice(bare,a,f.path,f.card in printedCreatures) -> DiagnosticCategory.PROBABLE_EXTRACTION_OMISSION
            f.check=="D_SUSPICIOUS_EXCLUSION" && result!=null && mechanicalVerdict(result)==Verdict.MATCH && (result.operation=="SACRIFICE" || type((result.nativeOperation as? JsonObject)?.get("target"))=="Self") &&
                result.enablingRelationship!="INCIDENTAL_SYMMETRIC_REMOVAL" && !result.decisions.last().reason.contains("symmetrical",true) -> DiagnosticCategory.POSSIBLE_POLICY_FALSE_NEGATIVE
            f.check=="D_SUSPICIOUS_EXCLUSION" && result!=null && result.operation in setOf("DESTROY","DAMAGE_POTENTIALLY_LETHAL","TOUGHNESS_REDUCTION","SACRIFICE") -> DiagnosticCategory.EXPECTED_POLICY_EXCLUSION
            (report.objective=="reanimate-own-creatures" && t in setOf("MoveToZone","MoveCollection") || report.objective=="create-creature-tokens" && (t.startsWith("CreateToken") || t=="CreatePredefinedToken")) && f.check=="A_POTENTIAL_MISS" -> DiagnosticCategory.MISSING_MECHANICAL_FACT
            f.check=="G_UNRESOLVED_OBJECTIVE_CONSTRAINT" -> {
                val fact=obj?.get("fact") as? JsonObject
                if(fact?.get("kind")?.jsonPrimitive?.contentOrNull=="MOVE_OBJECT" && fact["sourcePlayer"]?.jsonPrimitive?.contentOrNull=="UNKNOWN" || fact?.get("destinationController")?.jsonPrimitive?.contentOrNull=="UNKNOWN") DiagnosticCategory.UNRESOLVED_OWNERSHIP_OR_CONTROL
                else DiagnosticCategory.MISSING_OBJECTIVE_INTERPRETATION
            }
            f.check=="B_INCOMPLETE_ACCEPTED_ACTION" -> {
                val issues=(obj?.get("unsupported") as? JsonArray).orEmpty()
                val unrelated=issues.isNotEmpty() && issues.all { x ->
                    val u=x.jsonObject["nativeType"]?.jsonPrimitive?.contentOrNull?.substringAfterLast('.')
                    u in setOf("GainLifeEffect","LoseLifeEffect","GrantKeywordEffect","AttachEquipmentEffect")
                }
                if(unrelated) DiagnosticCategory.UNSUPPORTED_BUT_LIKELY_UNRELATED else DiagnosticCategory.INCOMPLETE_INTERPRETATION
            }
            else -> DiagnosticCategory.UNVERIFIED_WARNING
        }
        val stage=when(category) {
            DiagnosticCategory.ELIGIBILITY_DATA_PROBLEM -> if(obj?.get("format")?.jsonPrimitive?.contentOrNull=="UNKNOWN") "FORMAT_LEGALITY" else "COLOR_IDENTITY"
            DiagnosticCategory.POSSIBLE_POLICY_FALSE_NEGATIVE,DiagnosticCategory.EXPECTED_POLICY_EXCLUSION -> "POLICY"
            DiagnosticCategory.PROBABLE_EXTRACTION_OMISSION,DiagnosticCategory.MISSING_MECHANICAL_FACT -> "EXTRACTION"
            DiagnosticCategory.MISSING_OBJECTIVE_INTERPRETATION -> "OBJECTIVE_COMPATIBILITY"
            DiagnosticCategory.UNRESOLVED_OWNERSHIP_OR_CONTROL -> "OWNERSHIP_OR_CONTROL"
            DiagnosticCategory.CONFIRMED_BENCHMARK_MISMATCH -> when(f.reason.substringBefore(':')) {
                "POLICY_DISAGREEMENT","CONTRIBUTION_CLASSIFICATION" -> "POLICY"
                "EXPECTED_PATHWAY_MISSED" -> "EXTRACTION"
                "UNEXPECTED_MECHANICAL_PATHWAY","MECHANICAL_COMPATIBILITY" -> "COMPATIBILITY"
                else -> f.reason.substringBefore(':')
            }
            else -> "INTERPRETATION"
        }
        // Group missing metadata by missing field, never by the card's name/identity values.
        val shape=if(category==DiagnosticCategory.ELIGIBILITY_DATA_PROBLEM) buildJsonObject { put("missing",stage) }
            else if(category==DiagnosticCategory.CONFIRMED_BENCHMARK_MISMATCH) buildJsonObject { put("type",t); put("stage",stage); put("pattern",canonicalShape(native)) }
            else canonicalShape(native)
        val context=a?.path?.replace(Regex("\\[\\d+\\]"),"[]") ?: f.path.substringBefore('[').substringBeforeLast('.',f.path)
        Tagged(DiagnosticRef(index,f.card,f.face,f.path,f.actionId,sourcesByCard[f.card] ?: listOf("native:${f.face}.${f.path}")),category,t,context,stage,shape,
            when(category) {
                DiagnosticCategory.MISSING_MECHANICAL_FACT -> "Movement-like native node was not visited into a local movement fact; suspected gap, not confirmed satisfiable capability"
                DiagnosticCategory.MISSING_OBJECTIVE_INTERPRETATION -> "Movement fact is recognized but objective constraints or branch interpretation remain unresolved"
                DiagnosticCategory.UNRESOLVED_OWNERSHIP_OR_CONTROL -> "Native movement is recognized but required player/owner binding remains unresolved"
                DiagnosticCategory.PROBABLE_EXTRACTION_OMISSION -> "Native sacrifice node is present in an executable action but no local pathway was emitted; binding/zone still requires review"
                DiagnosticCategory.POSSIBLE_POLICY_FALSE_NEGATIVE -> "Mechanically compatible sacrifice excluded without a source-established removal qualification"
                DiagnosticCategory.EXPECTED_POLICY_EXCLUSION -> "Known ordinary-friendly-target or incidental-removal policy exclusion; not a confirmed extraction error"
                DiagnosticCategory.UNSUPPORTED_BUT_LIKELY_UNRELATED -> "Known player-only, non-death movement, nonnegative modifier or unrelated outcome; native action remains available"
                DiagnosticCategory.ELIGIBILITY_DATA_PROBLEM -> "Missing $stage data; separate from mechanical coverage"
                DiagnosticCategory.INCOMPLETE_INTERPRETATION -> "Accepted local pathway has unresolved enclosing mechanics; no whole-action verification claimed"
                else -> f.reason
            },bench.map { it.id },confirmed)
    }
    val accepted=report.results.filter { it.verdict==Verdict.MATCH }.map { it.actionId }.toSet()
    val compatibleRejected=report.results.filter { mechanicalVerdict(it)==Verdict.MATCH && policyVerdict(it)==Verdict.NO_MATCH }.map { it.actionId }.toSet()
    fun rank(category: DiagnosticCategory)=when(category) {
        DiagnosticCategory.CONFIRMED_BENCHMARK_MISMATCH -> 0
        DiagnosticCategory.PROBABLE_EXTRACTION_OMISSION -> 1
        DiagnosticCategory.POSSIBLE_POLICY_FALSE_NEGATIVE -> 2
        DiagnosticCategory.INCOMPLETE_INTERPRETATION,DiagnosticCategory.MISSING_MECHANICAL_FACT,DiagnosticCategory.MISSING_OBJECTIVE_INTERPRETATION,DiagnosticCategory.UNRESOLVED_OWNERSHIP_OR_CONTROL -> 3
        DiagnosticCategory.UNVERIFIED_WARNING -> 4
        DiagnosticCategory.ELIGIBILITY_DATA_PROBLEM -> 5
        else -> 6
    }
    return tagged.groupBy { "${it.category}|${it.t}|${it.context}|${it.stage}|${it.shape}" }.map { (key,ts) ->
        val t=ts.first(); val refs=ts.map { it.ref }; val ids=refs.mapNotNull { it.actionId }.toSet();val confirmed=ts.flatMap { it.confirmed }.distinct().sorted()
        val priority=if(confirmed.isNotEmpty()) "BENCHMARK_SUPPORTED" else when(rank(t.category)) { 0,1 -> "PROBABLE_NATIVE_GAP";2,3 -> "REVIEW_RELEVANT";5 -> "DATA_GAP";6 -> "LOW_OBJECTIVE_RELEVANCE";else -> "UNVERIFIED" }
        val feasible=when { t.t in setOf("PayOrSuffer","SacrificeSelf","Sacrifice","AtomSacrifice","ModifyStats") -> "EXISTING_SDK_INTERPRETATION_POSSIBLE";t.category==DiagnosticCategory.ELIGIBILITY_DATA_PROBLEM -> "NEEDS_AUTHORITATIVE_METADATA";else -> "REQUIRES_SOURCE_REVIEW" }
        RootIssue(rootHash(key),t.category,t.t,t.context,t.stage,t.shape,t.reason,refs.size,refs.map { it.card }.distinct().size,ids.size,priority,
            PriorityEvidence(confirmed,ts.flatMap { it.benchmarkIds }.distinct().sorted(),ids.count { it in accepted },ids.count { it in compatibleRejected },
                rank(t.category)<=3,feasible,"Confirmed benchmark cases first, then category, distinct cards, distinct actions, stable fingerprint; no aggregate score"),
            when { t.t in setOf("Sacrifice","SacrificeSelf","AtomSacrifice") -> "Preserve mechanical compatibility independently of mandatory timing and discovery-policy relationships"
                t.t=="PayOrSuffer" -> "Traverse typed payment and failure branches with payer and condition evidence"
                t.t=="ModifyStats" -> "Establish dynamic modifier sign; keep unsupported signs UNKNOWN rather than infer a reduction"
                t.category==DiagnosticCategory.ELIGIBILITY_DATA_PROBLEM -> "Repair verified format/identity metadata without changing mechanical extraction"
                else -> "Review representative native nodes; implement one reusable interpreter only after positive/negative validation" },
            "Relevance is diagnostic, not an MTG proof or confirmed corpus false-negative count; individual native records remain linked",refs.take(5),refs)
    }.sortedWith(compareBy<RootIssue> { if(it.evidence.confirmedBenchmarkCases.isNotEmpty()) 0 else 1 }.thenBy { rank(it.category) }.thenByDescending { it.distinctCards }.thenByDescending { it.distinctActions }.thenBy { it.fingerprint })
}
@Serializable data class EngineeringTask(val family: String,val rootFingerprints: List<String>,val confirmedBenchmarkCases: List<String>,
    val categories: List<DiagnosticCategory>,val rawRecordCount: Int,val distinctCards: Int,val distinctActions: Int,
    val feasibleGenericWork: List<String>,val representatives: List<DiagnosticRef>,val proposedNextStep: String)
fun engineeringTasks(roots: List<RootIssue>): List<EngineeringTask> = roots.groupBy { r -> when {
    r.category==DiagnosticCategory.UNSUPPORTED_BUT_LIKELY_UNRELATED -> "LOW_RELEVANCE:${r.nativeType}"
    r.category==DiagnosticCategory.EXPECTED_POLICY_EXCLUSION -> "EXPECTED_POLICY_EXCLUSION:${r.nativeType}"
    r.stage=="POLICY" && r.nativeType in setOf("Sacrifice","SacrificeSelf","AtomSacrifice") -> "SEPARATE_SACRIFICE_CAPABILITY_FROM_POLICY"
    r.nativeType=="PayOrSuffer" || r.affected.any { it.path.endsWith(".suffer") || it.path.contains(".suffer.") } -> "TRAVERSE_PAY_OR_SUFFER_BRANCHES"
    r.nativeType=="ModifyStats" -> "PROVE_DYNAMIC_MODIFIER_SIGN"
    else -> "${r.stage}:${r.nativeType}"
} }.map { (family,rs) ->
    val refs=rs.flatMap { it.affected }.distinctBy { it.findingIndex }
    EngineeringTask(family,rs.map { it.fingerprint },rs.flatMap { it.evidence.confirmedBenchmarkCases }.distinct().sorted(),
        rs.map { it.category }.distinct(),refs.size,refs.map { it.card }.distinct().size,refs.mapNotNull { it.actionId }.distinct().size,
        rs.map { it.evidence.genericInterpretationFeasibility }.distinct(),refs.take(5),rs.first().proposedFix)
}.sortedWith(compareBy<EngineeringTask> { if(it.confirmedBenchmarkCases.isNotEmpty()) 0 else 1 }
    .thenBy { if(it.family.startsWith("LOW_RELEVANCE") || it.family.startsWith("EXPECTED_POLICY_EXCLUSION")) 1 else 0 }
    .thenByDescending { it.confirmedBenchmarkCases.size }.thenByDescending { it.distinctCards }.thenBy { it.family })
fun rootSummary(r: Examination): String = buildString {
    appendLine("Diagnostic root issues: ${r.commander}; ${r.findings.size} raw records -> ${r.rootIssues.size} groups")
    appendLine("Categories (records): ${r.categorizedCounts}")
    appendLine("Benchmark: ${r.benchmark?.numerators ?: "not supplied"}")
    appendLine("Ranked engineering families (confirmed cases first; benign/existing-policy records separated):")
    r.engineeringTasks.filter { !it.family.startsWith("LOW_RELEVANCE") && !it.family.startsWith("EXPECTED_POLICY_EXCLUSION") }.take(20).forEach { t ->
        appendLine("${t.family} | confirmed benchmark cases=${t.confirmedBenchmarkCases} | ${t.rawRecordCount} records / ${t.distinctCards} cards / ${t.distinctActions} actions")
        appendLine("  Next: ${t.proposedNextStep}; feasibility=${t.feasibleGenericWork}; roots=${t.rootFingerprints.size}")
    }
    appendLine("Representative structural groups:")
    r.rootIssues.filter { it.priorityBand!="LOW_OBJECTIVE_RELEVANCE" }.take(12).forEach { g ->
        appendLine("${g.priorityBand} | ${g.category} | ${g.nativeType} | ${g.recordCount} records / ${g.distinctCards} cards / ${g.distinctActions} actions | ${g.fingerprint}")
        appendLine("  ${g.reason}; benchmark=${g.evidence.confirmedBenchmarkCases}; acceptedActions=${g.evidence.acceptedActions}; compatibleRejected=${g.evidence.mechanicallyCompatibleRejectedActions}")
        appendLine("  Next: ${g.proposedFix}; feasibility=${g.evidence.genericInterpretationFeasibility}")
        appendLine("  ${g.representatives.joinToString { "${it.card}:${it.face}.${it.path}" }}")
    }
    appendLine("All roots and individual records are preserved in JSON. Counts are review records, not real-world recall.")
}
