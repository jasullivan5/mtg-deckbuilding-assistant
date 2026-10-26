package dev.mtg.assistant.audit

import dev.mtg.assistant.discovery.*
import kotlinx.serialization.Serializable

@Serializable data class CrossObjectiveRow(val nativeType: String,val nodes: Int,val distinctCards: Int,
    val usedByBoth: Int,val onlyFirst: Int,val onlySecond: Int,val neither: Int,
    val partialFirst: Int,val partialSecond: Int,val examples: List<String>,val reuse: String)
@Serializable data class SharedStructuralGap(val nativeType: String,val occurrencesFirst: Int,val occurrencesSecond: Int,val representatives: List<String>)
@Serializable data class CrossObjectiveReport(val firstObjective: String,val secondObjective: String,
    val sameEligiblePool: Boolean,val sharedFactIdentities: Int,val identicalFactEvidence: Boolean,
    val sharedAcceptedActionIds: List<String>,val rows: List<CrossObjectiveRow>,val sharedUnsupported: List<SharedStructuralGap>,val limitations: List<String>)
/** Same native-operation identity across objectives, not two disconnected card tags. */
fun crossObjective(a: DiscoveryReport,b: DiscoveryReport): CrossObjectiveReport {
    require(a.argentumCommit==b.argentumCommit) { "Pin mismatch" }
    fun pool(r: DiscoveryReport)=r.eligibility.filter { it.format==Verdict.MATCH && it.deck==Verdict.MATCH }.map { it.card }.toSet()
    val same=a.commander==b.commander && a.conventionalQuery==b.conventionalQuery && pool(a)==pool(b)
    require(same) { "Cross-objective comparison requires the same commander, eligible pool and conventional query" }
    fun key(r: ResultEvidence)=Triple(r.card,r.face,r.abilityPath)
    val ar=a.results.groupBy(::key);val br=b.results.groupBy(::key)
    val facts=(a.facts+b.facts).distinctBy { it.id }
    val rows=facts.groupBy { (it.native as? kotlinx.serialization.json.JsonObject)?.get("type")?.toString()?.trim('"') ?: it.kind.name }.map { (type,fs) ->
        fun has(rs: Map<Triple<String,String,String>,List<ResultEvidence>>,f: MechanicalFact)=rs[Triple(f.card,f.face,f.path)].orEmpty().any { it.verdict==Verdict.MATCH }
        CrossObjectiveRow(type,fs.size,fs.map { it.card }.distinct().size,fs.count { has(ar,it)&&has(br,it) },fs.count { has(ar,it)&& !has(br,it) },fs.count { !has(ar,it)&&has(br,it) },fs.count { !has(ar,it)&& !has(br,it) },
            fs.count { f -> ar[Triple(f.card,f.face,f.path)].orEmpty().any { it.localSupport==SupportStatus.PARTIALLY_SUPPORTED } },
            fs.count { f -> br[Triple(f.card,f.face,f.path)].orEmpty().any { it.localSupport==SupportStatus.PARTIALLY_SUPPORTED } },fs.take(5).map { "${it.card}|${it.actionId}|${it.path}" },when {
                fs.any { has(ar,it) } && fs.any { has(br,it) } -> "NATIVE_STRUCTURE_USED_BY_BOTH_OBJECTIVES"
                fs.any { has(ar,it) } -> "A_USED_B_NOT_ACCEPTED_CHECK_B_CONSTRAINTS"
                fs.any { has(br,it) } -> "B_USED_A_PROJECTION_OR_POLICY_DIFFERS"
                else -> "FACTS_PRESENT_NEITHER_QUERY_ACCEPTS_NOT_NECESSARILY_UNSUPPORTED"
            })
    }.sortedBy { it.nativeType }
    val af=a.facts.associateBy { it.id };val bf=b.facts.associateBy { it.id }
    fun acceptedIds(r: DiscoveryReport): Set<String> {
        val actions=r.actions.associateBy { it.id }
        return r.results.filter { it.verdict==Verdict.MATCH }.flatMap { listOfNotNull(it.actionId,associatedSpellAction(actions,it.actionId)) }.toSet()
    }
    val au=a.unsupported.groupBy { it.nativeType };val bu=b.unsupported.groupBy { it.nativeType }
    val gaps=(au.keys intersect bu.keys).sorted().map { t -> SharedStructuralGap(t,au.getValue(t).size,bu.getValue(t).size,
        (au.getValue(t)+bu.getValue(t)).distinctBy { Triple(it.card,it.face,it.path) }.take(5).map { "${it.card}|${it.path}|${it.reason}" }) }
    return CrossObjectiveReport(a.objective,b.objective,same,(af.keys intersect bf.keys).size,af==bf,
        (acceptedIds(a) intersect acceptedIds(b)).sorted(),rows,gaps,
        listOf("Rows count extracted operation facts, not all native SDK nodes or complete-card support. Run ordinary coverage for independent native-node enumeration.",
            "Not used by an objective is not necessarily unsupported. Shared action IDs expose associated costs/effects; no claim that both effects always resolve.",
            "Original death projection still has objective-specific removal/dependency traversal. Reanimation adds no second native walker. Both reports export the same generic operation facts."))
}
fun crossObjectiveSummary(r: CrossObjectiveReport)="${r.firstObjective} vs ${r.secondObjective}\nSame pool=${r.sameEligiblePool}; same facts=${r.identicalFactEvidence}; shared fact identities=${r.sharedFactIdentities}; shared accepted actions=${r.sharedAcceptedActionIds.size}\n"+
    r.rows.joinToString("\n") { "${it.nativeType}: facts=${it.nodes}, cards=${it.distinctCards}, both=${it.usedByBoth}, A-only=${it.onlyFirst}, B-only=${it.onlySecond}, neither=${it.neither}, partial=${it.partialFirst}/${it.partialSecond}" }
