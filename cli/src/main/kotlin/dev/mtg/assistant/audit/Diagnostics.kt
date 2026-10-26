package dev.mtg.assistant.audit

import com.wingedsheep.sdk.model.CardDefinition
import dev.mtg.assistant.Catalog
import dev.mtg.assistant.discovery.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

private val reader = Json { ignoreUnknownKeys = true }
@Serializable data class NativeNode(val card: String, val face: String, val path: String,
    val context: String, val nativeType: String, val native: JsonObject, val actionId: String?)
@Serializable data class CoverageOccurrence(val card: String, val face: String, val path: String,
    val actionId: String?, val status: String, val reasons: List<String>, val sourceReferences: List<String>)
@Serializable data class CoverageGroup(val fingerprint: String, val nativeType: String, val context: String,
    val shape: JsonElement, val occurrences: Int, val distinctCards: Int, val distinctActions: Int,
    val supportStatus: String, val reasons: List<String>, val relevance: String,
    val insideAcceptedActions: Int, val representatives: List<CoverageOccurrence>, val proposedWork: String)
@Serializable data class CoverageReport(val argentumCommit: String = ARGENTUM_SHA, val commander: String,
    val inputCards: Int, val encounteredTypedNodes: Int, val groups: List<CoverageGroup>,
    val objective: String = "own-nontoken-creature-deaths", val factCounts: Map<String,Int> = emptyMap(),
    val limitations: List<String> = listOf("Independent traversal of serialized authored native definitions, including nested faces, grants and script fields. Does not enumerate every possible SDK subtype or keyword-lowered engine behavior.",
        "Observed nodes without an extracted pathway are not necessarily gaps. Relevance and rank are review aids, not MTG conclusions.",
        "Fingerprints remove incidental text and alpha-rename local IDs; retain native amounts, zones, filters and branch shape. Versioned fingerprint rules may change grouping."))
@Serializable data class Finding(val check: String, val card: String, val face: String, val path: String,
    val actionId: String?, val severity: String, val reason: String, val evidence: JsonElement)
@Serializable data class Examination(val commander: String, val counts: Map<String,Int>, val findings: List<Finding>,
    val rootIssues: List<RootIssue> = emptyList(), val categorizedCounts: Map<String,Int> = emptyMap(), val benchmark: BenchmarkReport? = null, val engineeringTasks: List<EngineeringTask> = emptyList(),
    val objective: String = "own-nontoken-creature-deaths",
    val limitations: List<String> = listOf("Warnings never admit cards into discovery. Potential misses use native structural clues and can include nonbattlefield/player-only operations.",
        "Contradictions use strict action+scope+prerequisite equivalence; this deliberately misses some semantic equivalences.",
        "Action completeness is separate from supported local pathway compatibility; no gameplay or exhaustiveness claim."))
private fun hash(s: String) = MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }.take(20)
private val incidentalKeys = setOf("description","oracleText","flavorText","imageUri","artist","prompt","rulings","metadata")
/** Alpha-normalize local variables, not mechanical enums or literal card-property restrictions. */
fun canonicalShape(value: JsonElement): JsonElement {
    val names = linkedMapOf<String,String>()
    val idKeys=setOf("id","storeAs","storeSelected","storeRemainder","storeMatching","storeNonMatching","from","collection","variableName")
    fun walk(v: JsonElement,key: String = ""): JsonElement = when(v) {
        is JsonObject -> JsonObject(v.filterKeys { it !in incidentalKeys }.toSortedMap().mapValues { (k,x) -> walk(x,k) })
        is JsonArray -> JsonArray(v.map { walk(it,key) })
        is JsonPrimitive -> if(v.isString && key in idKeys && v.content !in setOf("Battlefield","Graveyard","Exile","Hand","Library","Stack","Command")) JsonPrimitive(names.getOrPut(v.content) { "symbol${names.size}" }) else v
        else -> v
    }
    return walk(value)
}
private fun context(path: String) = path.replace(Regex("\\[\\d+\\]"),"[]").substringBefore(".effect").substringBefore(".spellEffect")
/** This walk starts from CardDefinition serialization, not extracted actions/pathways. */
fun nativeNodes(cards: List<CardDefinition>, report: DiscoveryReport): List<NativeNode> = buildList {
    val actionsByCardFace=report.actions.groupBy { it.card to it.face }
    fun face(def: CardDefinition, face: String, rootName: String) {
        val tree=nativeJson.encodeToJsonElement(CardDefinition.serializer(),def).jsonObject
        fun walk(v: JsonElement,path: String) {
            when(v) {
                is JsonObject -> {
                    val type=(v["type"] as? JsonPrimitive)?.content
                    if(type != null) {
                        val action=actionsByCardFace[rootName to face].orEmpty()
                            .filter { it.face == face && (path.startsWith(it.path) || it.path == "script.spell" && path.startsWith("script.spellEffect")) }
                            .maxByOrNull { it.path.length }
                        add(NativeNode(rootName,face,path,context(path),type,v,action?.id))
                    }
                    v.forEach { (k,x) -> if(k !in setOf("backFace","flipSide","cardFaces") && k !in incidentalKeys) walk(x,if(path.isEmpty()) k else "$path.$k") }
                }
                is JsonArray -> v.forEachIndexed { i,x -> walk(x,"$path[$i]") }
                else -> Unit
            }
        }
        walk(tree,"")
        def.backFace?.let { face(it,"$face.backFace",rootName) }; def.flipSide?.let { face(it,"$face.flipSide",rootName) }
        def.cardFaces.forEachIndexed { i,f ->
            val alias=def.copy(name=f.name,script=f.script,backFace=null,flipSide=null,cardFaces=emptyList())
            face(alias,"$face.cardFaces[$i]:${f.name}",rootName)
        }
    }
    cards.forEach { face(it,"front",it.name) }
}
private fun suspect(type: String,objective: String = "own-nontoken-creature-deaths") = if(objective=="create-creature-tokens") type.startsWith("CreateToken") || type in setOf("CreatePredefinedToken","Amass","Populate","CreateAuraToken","CreateRandomCreatureToken") else if(objective=="reanimate-own-creatures") type in setOf("MoveToZone","MoveCollection","GatherCards","SelectFromCollection","ConditionalOnCollection") else type in setOf("Sacrifice","SacrificeSelf","SacrificeTarget","ForceSacrifice","DealDamage","ModifyStats","MoveToZone","MoveCollection","AtomSacrifice","SacrificeSelfCost","ConditionalOnCollection","PayOrSuffer")
private fun relevance(type: String, shape: JsonElement): String = when {
    suspect(type) || type in setOf("SelectFromCollection","FilterCollection","Gate.DoAction","Gate.MayPay","ReflexiveTrigger") -> "POTENTIALLY_RELEVANT"
    type in setOf("GainLife","Scry","ShuffleLibrary","EmitLibrarySearchedEvent","AttachEquipment") -> "LIKELY_UNRELATED"
    else -> "UNRESOLVED"
}
private fun work(type: String) = when(type) {
    "Amass","Populate","CreateTokenCopyOfChosenPermanent","CreateTokenCopyOfEquippedCreature" -> "Resolve native copy/army choice and resulting creature characteristics/controller with action bindings; do not infer creation from a text clue"
    "CreateToken","CreatePredefinedToken","CreateTokenCopyOfSource","CreateTokenCopyOfTarget","CreateTokenBatch" -> "Review correlated native template, merged copy exceptions, receiver, quantity expression and enclosing branch"
    "SelectFromCollection","FilterCollection","MoveCollection" -> "Trace collection provenance, selection bounds, filters and movement zones within the same branch"
    "PayOrSuffer","ConditionalOnCollection" -> "Traverse conditional child effects with branch prerequisites; never infer causal payment from mere sequencing"
    "MoveToZone" -> "Check source/destination, object ownership and entry controller before objective-specific inference"
    "ModifyStats" -> "Check modifier sign/dynamic amount and target scope before inferring potential toughness death"
    else -> "Review native fields and enclosing action; add a reusable interpreter only with positive and negative tests"
}
fun coverage(cards: List<CardDefinition>, report: DiscoveryReport): CoverageReport {
    val nodes=nativeNodes(cards,report)
    val explicit=report.structuralSupport.groupBy { Triple(it.card,it.face,it.path) }
    val resultsByFace=report.results.groupBy { it.card to it.face }
    val issuesByFace=report.unsupported.groupBy { it.card to it.face }
    val paths=report.results.map { Triple(it.card,it.face,it.abilityPath) }.toSet()
    val sources=report.results.associate { it.actionId to it.sourceReferences }
    val provenanceByCard=report.eligibility.associate { it.card to it.provenance }
    val actionMap=report.actions.associateBy { it.id }
    val accepted=report.results.filter { it.verdict == Verdict.MATCH }.flatMap { listOfNotNull(it.actionId,associatedSpellAction(actionMap,it.actionId)) }.toSet()
    data class Entry(val n: NativeNode,val status: String,val reasons: List<String>,val shape: JsonElement)
    val entries=nodes.map { n ->
        val unsupported=issuesByFace[n.card to n.face].orEmpty().filter { it.path == n.path || n.path.startsWith(it.path+".") }
        val matched=Triple(n.card,n.face,n.path) in paths
        val support=explicit[Triple(n.card,n.face,n.path)].orEmpty()
        val local=resultsByFace[n.card to n.face].orEmpty().filter { it.abilityPath==n.path }
        val partialChildren=resultsByFace[n.card to n.face].orEmpty().any { it.abilityPath.startsWith(n.path+".") && it.localSupport==SupportStatus.PARTIALLY_SUPPORTED }
        val state=when { local.any { it.localSupport==SupportStatus.PARTIALLY_SUPPORTED } -> "PARTIALLY_SUPPORTED"
            support.isNotEmpty() && partialChildren -> "PARTIALLY_SUPPORTED"
            support.any { it.status==SupportStatus.UNSUPPORTED } -> "UNSUPPORTED"
            support.any { it.status==SupportStatus.PARTIALLY_SUPPORTED } -> "PARTIALLY_SUPPORTED"
            support.isNotEmpty() && issuesByFace[n.card to n.face].orEmpty().any { it.path.startsWith(n.path+".") } -> "PARTIALLY_SUPPORTED"
            support.isNotEmpty() -> "SUPPORTED"
            unsupported.isNotEmpty() -> "UNSUPPORTED_OR_UNRESOLVED"; matched -> "PATHWAY_EXTRACTED"; else -> "OBSERVED_WITHOUT_LOCAL_PATHWAY" }
        val descendants=issuesByFace[n.card to n.face].orEmpty().filter { it.path.startsWith(n.path+".") }
        val reasons=(unsupported.map { it.reason }+support.map { it.reason }).toMutableList()
        if(partialChildren || local.any { it.localSupport==SupportStatus.PARTIALLY_SUPPORTED }) reasons += "A local or nested pathway retains unresolved mechanical compatibility; recognizing the operation is not complete support"
        if(support.isNotEmpty() && descendants.isNotEmpty()) reasons += "Local interpretation has unresolved descendants: "+descendants.map { it.reason }.distinct().sorted().joinToString("; ")
        Entry(n,state,reasons.distinct().sorted(),canonicalShape(n.native))
    }
    val groups=entries.groupBy { "${it.n.nativeType}|${it.n.context}|${it.status}|${it.reasons}|${it.shape}" }.map { (key,es) ->
        val e=es.first(); val n=e.n
        CoverageGroup(hash(key),n.nativeType,n.context,e.shape,es.size,es.map { it.n.card }.distinct().size,
            es.mapNotNull { it.n.actionId }.distinct().size,e.status,e.reasons,if(report.objective in setOf("reanimate-own-creatures","create-creature-tokens") && suspect(n.nativeType,report.objective)) "POTENTIALLY_RELEVANT" else if(report.objective=="reanimate-own-creatures" && n.nativeType in setOf("DealDamage","ModifyStats","Sacrifice","SacrificeSelf","ForceSacrifice")) "LIKELY_UNRELATED" else relevance(n.nativeType,e.shape),
            es.count { it.n.actionId in accepted },es.take(5).map { x -> CoverageOccurrence(x.n.card,x.n.face,x.n.path,x.n.actionId,x.status,x.reasons,
                sources[x.n.actionId] ?: listOfNotNull("native:${x.n.face}.${x.n.path}", DefinitionSources.paths[x.n.card]?.let { "https://github.com/wingedsheep/argentum-engine/blob/$ARGENTUM_SHA/$it" },provenanceByCard[x.n.card]?.takeIf { it.origin == "APPLICATION_EXTENSION" }?.source)) },work(n.nativeType))
    }.sortedWith(compareBy<CoverageGroup> { if(it.relevance == "POTENTIALLY_RELEVANT" && it.supportStatus !in setOf("PATHWAY_EXTRACTED","SUPPORTED")) 0 else 1 }.thenByDescending { it.distinctCards }.thenBy { it.fingerprint })
    return CoverageReport(commander=report.commander,inputCards=cards.size,encounteredTypedNodes=nodes.size,groups=groups,objective=report.objective,factCounts=report.facts.groupingBy { it.kind.name }.eachCount().toSortedMap())
}
fun examine(cards: List<CardDefinition>, report: DiscoveryReport, benchmark: BenchmarkReport? = null): Examination {
    val findings=mutableListOf<Finding>()
    val resultsByAction=report.results.groupBy { it.actionId }
    val unsupportedByFace=report.unsupported.groupBy { it.card to it.face }
    val paths=report.results.map { Triple(it.card,it.face,it.abilityPath) }.toSet()
    val nodes=nativeNodes(cards,report)
    for(n in nodes.filter { suspect(it.nativeType,report.objective) }) {
        if(Triple(n.card,n.face,n.path) !in paths && report.structuralSupport.none { it.card==n.card && it.face==n.face && it.path==n.path && it.status==SupportStatus.SUPPORTED })
            findings += Finding("A_POTENTIAL_MISS",n.card,n.face,n.path,n.actionId,"REVIEW","Relevant-looking native node has no local pathway; scope/zone/sign or enclosing semantics may make this correct",n.native)
    }
    if(report.objective in setOf("reanimate-own-creatures","create-creature-tokens")) {
        report.results.filter { it.verdict==Verdict.UNKNOWN }.forEach { r ->
            val fact=report.facts.firstOrNull { it.card==r.card && it.face==r.face && it.path==r.abilityPath && it.kind==if(report.objective=="create-creature-tokens") FactKind.CREATE_TOKEN else FactKind.MOVE_OBJECT }
            findings += Finding("G_UNRESOLVED_OBJECTIVE_CONSTRAINT",r.card,r.face,r.abilityPath,r.actionId,"UNKNOWN",
                "Generic fact exists; objective object/controller, quantity, zone/binding/filter or branch compatibility remains unresolved; not a proven extraction omission",
                buildJsonObject { put("pathway",reportJson.encodeToJsonElement(r));put("fact",reportJson.encodeToJsonElement(fact)) })
        }
    }
    val nodesByAction=nodes.groupBy { it.actionId }
    val actions=report.actions.associateBy { it.id }
    for(r in report.results.filter { it.verdict == Verdict.MATCH }) {
        val issues=unsupportedByFace[r.card to r.face].orEmpty().filter { it.actionId == r.actionId || it.actionId==associatedSpellAction(actions,r.actionId) || it.actionId.endsWith("script.unsupported") }
        if(issues.isNotEmpty()) findings += Finding("B_INCOMPLETE_ACCEPTED_ACTION",r.card,r.face,r.abilityPath,r.actionId,"PARTIAL",
            "Supported local pathway; unresolved nodes may affect activation, eligibility, causality or outcome. Face-wide script restrictions included conservatively.",
            buildJsonObject { put("pathway",reportJson.encodeToJsonElement(r)); put("action",reportJson.encodeToJsonElement(actions[r.actionId])); put("unsupported",reportJson.encodeToJsonElement(issues)) })
    }
    // Strict equivalence avoids treating the same effect class as equivalent behavior.
    val equivalent=report.results.groupBy { r ->
        val action=actions[r.actionId]
        canonicalShape(buildJsonObject { put("action",action?.native ?: JsonNull); put("scope",r.affectedScope); put("operation",r.operation); put("operationNode",r.nativeOperation); put("role",r.costOrEffect); put("branch",r.branch.name); put("prerequisites",JsonArray(r.prerequisites.map(::JsonPrimitive))) }).toString()
    }
    for((_,rs) in equivalent) if(rs.map { it.decisions.map { d -> d.stage to d.verdict } }.distinct().size>1)
        rs.forEach { r -> findings += Finding("C_CONTRADICTION",r.card,r.face,r.abilityPath,r.actionId,"REVIEW","Strictly equivalent native actions/scopes/prerequisites received different decision vectors",reportJson.encodeToJsonElement(rs)) }
    for(r in report.results.filter { it.decisions.any { d -> d.basis == "POLICY" && d.verdict != Verdict.MATCH } }) {
        val action=actions[r.actionId] ?: continue
        val associated=resultsByAction[r.actionId].orEmpty().filter { it != r }
        val distinctEffects=nodesByAction[r.actionId].orEmpty().filter { it.path != r.abilityPath && !r.abilityPath.startsWith(it.path+".") && it.nativeType in setOf("DrawCards","AddMana","AddColorlessMana","Gated","ReflexiveTrigger","GainLife","MoveToZone","DealDamage","ModifyStats") }
        if(associated.isNotEmpty() || distinctEffects.isNotEmpty() || r.abilityPath.contains("otherwise"))
            findings += Finding("D_SUSPICIOUS_EXCLUSION",r.card,r.face,r.abilityPath,r.actionId,"REVIEW","Policy rejected a pathway in an action with associated outcomes/alternatives. Co-occurrence alone is not enabling or enhancement.",
                buildJsonObject { put("pathway",reportJson.encodeToJsonElement(r)); put("action",action.native); put("associatedPathways",reportJson.encodeToJsonElement(associated)) })
    }
    report.eligibility.filter { it.format == Verdict.UNKNOWN || it.deck == Verdict.UNKNOWN }.forEach {
        findings += Finding("E_ELIGIBILITY_DATA_GAP",it.card,"front","eligibility",null,"DATA_GAP","Unknown format or identity; separate from mechanical interpretation",reportJson.encodeToJsonElement(it))
    }
    benchmark?.cases?.filter { it.status=="CONFIRMED_MISMATCH" || it.status=="UNRESOLVED" }?.forEach { row ->
        val a=report.actions.filter { it.card==row.card && it.face==row.expected.face && (row.expected.path.startsWith(it.path) || it.path=="script.spell" && row.expected.path.startsWith("script.spellEffect")) }.maxByOrNull { it.path.length }
        row.issues.forEach { issue -> findings+=Finding("F_BENCHMARK_MISMATCH",row.card,row.expected.face,row.expected.path,a?.id,issue.certainty,
            issue.kind+": "+issue.reason,buildJsonObject { put("caseId",row.id);put("nativePattern",row.actual.firstOrNull()?.nativeOperation ?: nativeAt(a,row.expected.path) ?: JsonNull);put("review",reportJson.encodeToJsonElement(row.expected)) }) }
    }
    val sorted=findings.sortedWith(compareBy<Finding> { it.check }.thenBy { it.card }.thenBy { it.path })
    val roots=rootIssues(sorted,report,benchmark,cards)
    return Examination(report.commander,sorted.groupingBy { it.check }.eachCount().toSortedMap(),sorted,roots,
        roots.groupBy { it.category.name }.mapValues { (_,rs) -> rs.sumOf { it.recordCount } }.toSortedMap(),benchmark,engineeringTasks(roots),objective=report.objective)
}
@Serializable data class Change(val key: String, val kind: String, val before: JsonElement?, val after: JsonElement?, val attribution: List<String> = emptyList())
@Serializable data class ChangeReport(val beforeCommander: String, val afterCommander: String,
    val comparablePool: Boolean, val counts: Map<String,Int>, val changes: List<Change>, val transitions: Map<String,Int> = emptyMap(), val ambiguousPathwayBases: List<String> = emptyList(),
    val beforeStageCounts: Map<String,Int> = emptyMap(), val afterStageCounts: Map<String,Int> = emptyMap(),
    val limitations: List<String> = listOf("Attribution labels describe observed field changes, not independently isolated causal experiments. Stable pathway keys: card identity, face, ability path, operation, branch. Duplicate bases use native-operation signatures and occurrence ordinals, never silent map overwrite. Changes to a duplicate variant signature can appear as removal+addition; inspect ambiguousPathwayBases.","No quality score: new/removed matches may be fixes or regressions. Different commanders/pools are explicitly flagged.","Coverage comparison needs --before-coverage and --after-coverage; discovery diff alone cannot reconstruct independent traversal."))
fun diff(before: DiscoveryReport, after: DiscoveryReport, oldCoverage: CoverageReport? = null, newCoverage: CoverageReport? = null): ChangeReport {
    val changes=mutableListOf<Change>()
    fun decisions(r: ResultEvidence,basis: String)=r.decisions.filter { it.basis==basis }.map { Triple(it.stage,it.basis,it.verdict) }
    fun constraintChange(b: ResultEvidence,a: ResultEvidence)=b.prerequisites!=a.prerequisites || b.circumstances!=a.circumstances || b.dependency!=a.dependency
    fun annotationChange(b: ResultEvidence,a: ResultEvidence)=b.localSupport!=a.localSupport && (b.localSupport==null || a.localSupport==null)
    fun structureChange(b: ResultEvidence,a: ResultEvidence)=b.nativeOperation!=a.nativeOperation || b.costOrEffect!=a.costOrEffect ||
        b.controllerCompatibility!=a.controllerCompatibility || b.nontokenCompatibility!=a.nontokenCompatibility ||
        b.localSupport!=a.localSupport && b.localSupport!=null && a.localSupport!=null || b.enablingRelationship!=a.enablingRelationship
    fun attribution(label: String,old: JsonElement?,now: JsonElement?): List<String> {
        if(label=="ELIGIBILITY") return listOf("COMMANDER_ELIGIBILITY")
        if(label in setOf("UNSUPPORTED","COVERAGE")) return listOf("DIAGNOSTIC_INTERPRETATION")
        if(label!="PATHWAY") return listOf("CLASSIFICATION_SUMMARY")
        if(old==null || now==null) {
            val card=(now ?: old)?.jsonObject?.get("card")?.jsonPrimitive?.contentOrNull
            if(before.eligibility.none { it.card==card } || after.eligibility.none { it.card==card }) return listOf("CATALOG_MEMBERSHIP")
            val b=before.eligibility.firstOrNull { it.card==card }; val a=after.eligibility.firstOrNull { it.card==card }
            if(b?.format!=a?.format || b?.deck!=a?.deck) return listOf("COMMANDER_ELIGIBILITY")
            return listOf("MECHANICAL_EXTRACTION_OR_PATH_RESTRUCTURING")
        }
        val b=auditReader.decodeFromJsonElement<ResultEvidence>(old);val a=auditReader.decodeFromJsonElement<ResultEvidence>(now)
        return buildList {
            if(mechanicalVerdict(b)!=mechanicalVerdict(a) || decisions(b,"MECHANICAL")!=decisions(a,"MECHANICAL")) add("COMPATIBILITY_FILTERING")
            if(decisions(b,"POLICY")!=decisions(a,"POLICY")) add("DISCOVERY_POLICY")
            if(structureChange(b,a)) add("STRUCTURAL_EVIDENCE")
            if(annotationChange(b,a)) add("SUPPORT_STATUS_ANNOTATION")
            if(b.commanderLegality!=a.commanderLegality || b.deckEligibility!=a.deckEligibility) add("COMMANDER_ELIGIBILITY")
            if(constraintChange(b,a)) add("CONDITIONS_OR_CONSTRAINTS")
            if(b.contributionClassification!=a.contributionClassification) add("CONTRIBUTION_CLASSIFICATION")
            if(isEmpty()) add("EXPLANATION_ONLY")
        }
    }
    fun compare(label: String, a: Map<String,JsonElement>, b: Map<String,JsonElement>) {
        (a.keys+b.keys).sorted().forEach { key ->
            if(a[key] != b[key]) changes += Change(key, when { key !in a -> "${label}_ADDED"; key !in b -> "${label}_REMOVED"; else -> "${label}_CHANGED" },a[key],b[key],attribution(label,a[key],b[key]))
        }
    }
    fun base(r: ResultEvidence)="${r.identifier}|${r.face}|${r.abilityPath}|${r.operation}|${r.branch}"
    val oldGroups=before.results.groupBy(::base); val newGroups=after.results.groupBy(::base)
    val ambiguous=(oldGroups.keys+newGroups.keys).filter { oldGroups[it].orEmpty().size>1 || newGroups[it].orEmpty().size>1 }.sorted()
    fun paths(r: DiscoveryReport): Map<String,ResultEvidence> = buildMap {
        for((key,rs) in r.results.groupBy(::base)) {
            if(key !in ambiguous) put(key,rs.single())
            else for((signature,variants) in rs.groupBy { hash(it.costOrEffect+canonicalShape(it.nativeOperation)) }) {
                variants.sortedBy { it.prerequisites.toString()+it.dependency+it.affectedScope }.forEachIndexed { i,v -> put("$key|variant:$signature|occurrence:$i",v) }
            }
        }
    }
    val oldPaths=paths(before); val newPaths=paths(after)
    require(oldPaths.size == before.results.size && newPaths.size == after.results.size) { "Pathway comparison identity collision" }
    fun tree(v: ResultEvidence)=JsonObject(reportJson.encodeToJsonElement(v).jsonObject.filterKeys { it != "provenance" })
    compare("PATHWAY",oldPaths.mapValues { tree(it.value) },newPaths.mapValues { tree(it.value) })
    fun cardClasses(r: DiscoveryReport)=r.results.filter { it.verdict == Verdict.MATCH }.groupBy { it.card }.mapValues { (_,v)-> JsonArray(v.map { it.branch.name }.distinct().sorted().map(::JsonPrimitive)) }
    compare("ACCEPTED_CARD_CLASSIFICATIONS",cardClasses(before),cardClasses(after))
    compare("ELIGIBILITY",before.eligibility.associate { it.card to buildJsonObject { put("format",it.format.name);put("deck",it.deck.name);put("identity",reportJson.encodeToJsonElement(it.identity)) } },after.eligibility.associate { it.card to buildJsonObject { put("format",it.format.name);put("deck",it.deck.name);put("identity",reportJson.encodeToJsonElement(it.identity)) } })
    fun issues(r: DiscoveryReport)=r.unsupported.groupBy { "${it.card}|${it.face}|${it.path}|${it.nativeType}" }.mapValues { (_,v) -> JsonArray(v.sortedBy { it.reason }.map { reportJson.encodeToJsonElement(it) }) }
    compare("UNSUPPORTED",issues(before),issues(after))
    if(oldCoverage != null && newCoverage != null) {
        fun groups(r: CoverageReport)=r.groups.groupBy { "${it.nativeType}|${it.context}|${it.shape}" }.mapValues { (_,v) -> JsonArray(v.sortedBy { it.supportStatus }.map { reportJson.encodeToJsonElement(it) }) }
        compare("COVERAGE",groups(oldCoverage),groups(newCoverage))
    }
    val same=before.objective==after.objective && before.allowedIdentity == after.allowedIdentity && before.conventionalQuery == after.conventionalQuery && before.eligibility.filter { it.format == Verdict.MATCH && it.deck == Verdict.MATCH }.map { it.card }.toSet() == after.eligibility.filter { it.format == Verdict.MATCH && it.deck == Verdict.MATCH }.map { it.card }.toSet()
    val transitions=mutableMapOf<String,Int>()
    newPaths.forEach { (key,r) ->
        val o=oldPaths[key] ?: return@forEach
        fun record(k: String) { transitions[k]=(transitions[k] ?: 0)+1 }
        if(o.verdict != r.verdict) record("VERDICT_${o.verdict}_TO_${r.verdict}")
        val op=o.decisions.lastOrNull { it.basis == "POLICY" }; val np=r.decisions.lastOrNull { it.basis == "POLICY" }
        if(op?.verdict != np?.verdict) { record("POLICY_VERDICT_CHANGED"); record("POLICY_DECISION_CHANGED") } // legacy key, now strictly verdict-only
        if(decisions(o,"MECHANICAL")!=decisions(r,"MECHANICAL")) record("MECHANICAL_STAGE_VERDICT_CHANGED")
        if(o.contributionClassification != r.contributionClassification) record("CLASSIFICATION_CHANGED")
        if(structureChange(o,r)) record("STRUCTURAL_EVIDENCE_CHANGED")
        if(annotationChange(o,r)) record("SUPPORT_STATUS_ANNOTATION_ADDED_OR_REMOVED")
        if(constraintChange(o,r)) record("CONDITIONS_OR_CONSTRAINTS_CHANGED")
        if(o!=r && o.verdict==r.verdict && o.contributionClassification==r.contributionClassification && decisions(o,"POLICY")==decisions(r,"POLICY") && decisions(o,"MECHANICAL")==decisions(r,"MECHANICAL") && !structureChange(o,r) && !annotationChange(o,r) && !constraintChange(o,r)) record("EXPLANATION_ONLY_CHANGED")
        if(key.substringBefore("|variant:") in ambiguous && o!=r) record("UNRESOLVED_PATHWAY_IDENTITY_CHANGE")
    }
    return ChangeReport(before.commander,after.commander,same,changes.groupingBy { it.kind }.eachCount().toSortedMap(),changes,transitions.toSortedMap(),ambiguous,before.counts,after.counts)
}
fun coverageSummary(r: CoverageReport) = buildString {
    appendLine("Coverage: ${r.commander}; ${r.inputCards} cards; ${r.encounteredTypedNodes} independently encountered typed nodes; ${r.groups.size} structural groups")
    appendLine("Ranked review candidates (relevance then distinct cards; not quality scores):")
    r.groups.filter { it.supportStatus !in setOf("PATHWAY_EXTRACTED","SUPPORTED") }.take(35).forEach { appendLine("${it.fingerprint} | ${it.nativeType} | ${it.supportStatus} | ${it.relevance} | ${it.distinctCards} cards / ${it.occurrences} nodes / ${it.distinctActions} actions / ${it.insideAcceptedActions} inside accepted actions\n  ${it.reasons}; ${it.proposedWork}\n  ${it.representatives.joinToString { x -> "${x.card}:${x.face}.${x.path}" }}") }
}
fun auditCommand(args: List<String>, catalog: () -> Catalog, out: (String)->Unit) {
    require(args.isNotEmpty()) { "audit requires coverage, inconsistencies, or diff" }
    val options=linkedMapOf<String,String>(); var json=false; var i=1
    while(i<args.size) { val k=args[i++]; if(k == "--json") json=true else { require(k.startsWith("--") && i<args.size) { "Missing option value: $k" }; require(k !in options) { "Duplicate $k" }; options[k]=args[i++] } }
    val allowed=if(args[0] in setOf("diff","benchmark-diff")) setOf("--before","--after","--before-coverage","--after-coverage","--output") else setOf("--commander","--search","--snapshot","--output","--benchmark","--objective","--other-snapshot")
    require(options.keys.all { it in allowed }) { "Unknown audit option: ${options.keys-allowed}" }
    fun required(k: String)=requireNotNull(options[k]) { "$k required" }
    require(("--before-coverage" in options) == ("--after-coverage" in options)) { "Coverage diff requires both --before-coverage and --after-coverage" }
    val result=when(args[0]) {
        "coverage","inconsistencies","benchmark","cross-objective" -> {
            val c=catalog(); val report=options["--snapshot"]?.let { reader.decodeFromString<DiscoveryReport>(Files.readString(Path.of(it))) } ?: discover(c,required("--commander"),options["--search"],options["--objective"] ?: "own-nontoken-creature-deaths")
            options["--commander"]?.let { require(it == report.commander) { "Snapshot commander mismatch" } }
            require(report.argentumCommit == ARGENTUM_SHA) { "Snapshot pin mismatch" }
            val eligible=report.eligibility.filter { it.format == Verdict.MATCH && it.deck == Verdict.MATCH }.map { it.card }.toSet()
            val conventional=report.conventionalQuery?.let { c.search(it).map { it.name }.toSet() }
            val pool=physicalCards(c.cards).filter { it.name in eligible && (conventional == null || it.name in conventional) }
            require(pool.size == report.counts["mechanical_input_cards_after_conventional_search"]) { "Snapshot catalog/pool unavailable; use matching catalog revision" }
            options["--objective"]?.let { require(it==report.objective) { "Snapshot objective mismatch" } }
            if(args[0]=="cross-objective") {
                val other=reader.decodeFromString<DiscoveryReport>(Files.readString(Path.of(required("--other-snapshot"))))
                crossObjective(report,other).let { if(json) reportJson.encodeToString(it) else crossObjectiveSummary(it) }
            }
            else if(args[0] == "benchmark") evaluateBenchmark(report,Path.of(required("--benchmark"))).let { if(json) reportJson.encodeToString(it) else benchmarkSummary(it) }
            else if(args[0] == "coverage") coverage(pool,report).let { if(json) reportJson.encodeToString(it) else coverageSummary(it) }
            else examine(pool,report,options["--benchmark"]?.let { evaluateBenchmark(report,Path.of(it)) }).let { if(json) reportJson.encodeToString(it) else rootSummary(it) }
        }
        "benchmark-diff" -> {
            val before=auditReader.decodeFromString<BenchmarkReport>(Files.readString(Path.of(required("--before"))))
            val after=auditReader.decodeFromString<BenchmarkReport>(Files.readString(Path.of(required("--after"))))
            benchmarkDiff(before,after).let { if(json) reportJson.encodeToString(it) else "Same frozen review: ${it.sameFrozenReview}\nBefore: ${it.beforeNumerators} / ${it.beforeDenominators}\nAfter: ${it.afterNumerators} / ${it.afterDenominators}\n"+it.changes.joinToString("\n") { c -> "${c.id}: ${c.beforeStatus} -> ${c.afterStatus}" } }
        }
        "diff" -> {
            fun load(k: String)=reader.decodeFromString<DiscoveryReport>(Files.readString(Path.of(required(k))))
            fun cv(k: String)=options[k]?.let { reader.decodeFromString<CoverageReport>(Files.readString(Path.of(it))) }
            diff(load("--before"),load("--after"),cv("--before-coverage"),cv("--after-coverage")).let { if(json) reportJson.encodeToString(it) else "Pool comparable: ${it.comparablePool}; ${it.beforeCommander} -> ${it.afterCommander}\n${it.counts}\nTransitions: ${it.transitions}\nStage counts before: ${it.beforeStageCounts}\nStage counts after: ${it.afterStageCounts}\n"+it.changes.take(60).joinToString("\n") { c -> "${c.kind}: ${c.key}" } }
        }
        else -> throw IllegalArgumentException("Unknown audit command: ${args[0]}")
    }
    options["--output"]?.let { val p=Path.of(it).toAbsolutePath(); Files.createDirectories(p.parent); Files.writeString(p,result); out("Wrote $p") } ?: out(result)
}
