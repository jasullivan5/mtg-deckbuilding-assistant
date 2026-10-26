package dev.mtg.assistant.audit

import dev.mtg.assistant.discovery.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

@Serializable data class ReviewCase(val id: String,val card: String,val oracleId: String,val face: String,
    val path: String,val mapping: String,val branch: Contribution,val operation: String?,
    val expectedMechanical: Verdict,val expectedPolicy: Verdict?,val expectedCategory: Contribution?,
    val expectedRole: String?,val reviewStatus: String,val prerequisites: List<String>,
    val requiredNative: Map<String,String>,val reason: String,val sources: List<String>)
@Serializable data class ReviewedBenchmark(val reviewVersion: String,val argentumCommit: String,
    val reviewMethod: String,val frozenAt: String,val cases: List<ReviewCase>)
@Serializable data class BenchmarkIssue(val kind: String,val certainty: String,val reason: String)
@Serializable data class CaseEvaluation(val id: String,val card: String,val expected: ReviewCase,
    val mappingStatus: String,val actual: List<ResultEvidence>,val issues: List<BenchmarkIssue>,val status: String)
@Serializable data class CardEvaluation(val card: String,val expectedAcceptedCategories: List<Contribution>,
    val actualAcceptedCategories: List<Contribution>,val reviewedCaseIds: List<String>,val status: String)
@Serializable data class BenchmarkReport(val reviewVersion: String,val reviewSha256: String,val commander: String,
    val denominators: Map<String,Int>,val numerators: Map<String,Int>,val cases: List<CaseEvaluation>,
    val cards: List<CardEvaluation>,val unreviewedAcceptedPathways: List<ResultEvidence>,
    val limitations: List<String> = listOf("Source-led reviewed sample, not corpus recall or an independently adjudicated human gold standard.",
        "Unreviewed accepted paths are suspected review gaps, never confirmed false positives. Mapping ambiguity and unsupported evidence are explicit.",
        "Mechanical compatibility is conditional existential potential. Counts distinguish policy, source uncertainty, missing extraction and incomplete explanations."))
internal val auditReader=Json { ignoreUnknownKeys=true }
fun readBenchmark(file: Path): ReviewedBenchmark {
    val review=auditReader.decodeFromString<ReviewedBenchmark>(Files.readString(file))
    require(review.argentumCommit==ARGENTUM_SHA) { "Benchmark Argentum pin mismatch" }
    require(review.cases.map { it.id }.distinct().size==review.cases.size) { "Duplicate benchmark case ID" }
    require(review.cases.all { it.mapping in setOf("EXACT","PREFIX","ABSENT") }) { "Unsupported benchmark mapping" }
    return review
}
fun mechanicalVerdict(r: ResultEvidence): Verdict = r.mechanicalCompatibility
    ?: all(r.decisions.filter { it.basis=="MECHANICAL" }.map { it.verdict })
fun policyVerdict(r: ResultEvidence): Verdict? = r.policyAcceptance ?: r.decisions.lastOrNull { it.basis=="POLICY" }?.verdict
fun reviewMatches(c: ReviewCase,r: ResultEvidence): Boolean = c.card==r.card && c.face==r.face && c.branch==r.branch &&
    if(c.mapping=="EXACT") c.path==r.abilityPath else r.abilityPath.startsWith(c.path)
fun evaluateBenchmark(report: DiscoveryReport,review: ReviewedBenchmark,sha256: String = "in-memory-review"): BenchmarkReport {
    val actions=report.actions.associateBy { it.id }; val eligibility=report.eligibility.associateBy { it.card }
    val rows=review.cases.map { c ->
        val matches=report.results.filter { reviewMatches(c,it) }
        val issues=mutableListOf<BenchmarkIssue>()
        fun issue(kind: String,reason: String,certainty: String="CONFIRMED_MISMATCH") { issues+=BenchmarkIssue(kind,certainty,reason) }
        val eligible=eligibility[c.card]
        val mapping=when {
            eligible==null -> "CARD_UNAVAILABLE"
            report.mechanicalInputCards!=null && c.card !in report.mechanicalInputCards && eligible.format==Verdict.MATCH && eligible.deck==Verdict.MATCH -> "OUTSIDE_CONVENTIONAL_SEARCH"
            eligible.format!=Verdict.MATCH || eligible.deck!=Verdict.MATCH -> "INELIGIBLE_OR_UNKNOWN_ELIGIBILITY"
            c.mapping=="ABSENT" && matches.isEmpty() -> "ABSENCE_CONFIRMED"
            c.mapping=="ABSENT" -> "UNEXPECTED_PATHWAY"
            matches.isEmpty() -> "MISSING_PATHWAY"
            matches.size!=1 -> "AMBIGUOUS_MAPPING"
            else -> "UNIQUE_PATHWAY"
        }
        when {
            c.reviewStatus!="CONFIRMED" -> issue("UNRESOLVED_REVIEW",c.reason,"INSUFFICIENT_EVIDENCE")
            mapping in setOf("CARD_UNAVAILABLE","INELIGIBLE_OR_UNKNOWN_ELIGIBILITY","AMBIGUOUS_MAPPING","OUTSIDE_CONVENTIONAL_SEARCH") -> issue("MAPPING_PROBLEM",mapping,"INSUFFICIENT_EVIDENCE")
            c.mapping=="ABSENT" -> matches.forEach { r ->
                if(mechanicalVerdict(r)!=Verdict.NO_MATCH) issue("UNEXPECTED_MECHANICAL_PATHWAY","Reviewed native action has no supported operation for this objective")
                if(r.verdict==Verdict.MATCH) issue("INCORRECT_ACCEPTANCE","No reviewed objective contribution should be accepted")
            }
            matches.isEmpty() -> issue("EXPECTED_PATHWAY_MISSED","Source review expects ${c.operation} at ${c.face}.${c.path}; no path was emitted")
            else -> {
                val r=matches.single()
                if(c.expectedMechanical==Verdict.MATCH && r.controllerCompatibility==Verdict.NO_MATCH)
                    issue("INCORRECT_CONTROLLER_COMPATIBILITY","Source-reviewed desired-event compatibility requires our controller to be possible")
                if(c.branch!=Contribution.CREATE_CREATURE_TOKEN && c.expectedMechanical==Verdict.MATCH && r.nontokenCompatibility==Verdict.NO_MATCH)
                    issue("INCORRECT_TOKEN_COMPATIBILITY","Source-reviewed desired-event compatibility requires a nontoken to be possible")
                if(mechanicalVerdict(r)!=c.expectedMechanical) issue("MECHANICAL_COMPATIBILITY","Expected ${c.expectedMechanical}; actual ${mechanicalVerdict(r)}")
                if(c.expectedPolicy!=null && policyVerdict(r)!=c.expectedPolicy) issue("POLICY_DISAGREEMENT","Expected ${c.expectedPolicy}; actual ${policyVerdict(r)}")
                if(c.expectedRole!=null && r.costOrEffect!=c.expectedRole) issue("COST_EFFECT_RELATIONSHIP","Expected ${c.expectedRole}; actual ${r.costOrEffect}")
                if(c.operation!=null && r.operation!=c.operation) issue("OPERATION_CLASSIFICATION","Expected ${c.operation}; actual ${r.operation}")
                if(c.expectedCategory==null && r.verdict==Verdict.MATCH) issue("INCORRECT_ACCEPTANCE","Reviewed excluded pathway was accepted")
                if(r.contributionClassification!=c.expectedCategory) issue("CONTRIBUTION_CLASSIFICATION","Expected ${c.expectedCategory}; actual ${r.contributionClassification}")
                c.requiredNative.forEach { (key,value) ->
                    if((r.nativeOperation as? JsonObject)?.get(key)?.toString()?.trim('"')!=value) issue("INCOMPLETE_NATIVE_RESTRICTION","Required native $key=$value was not retained")
                }
                if(r.prerequisites.isEmpty() || r.sourceReferences.isEmpty() || actions[r.actionId]==null)
                    issue("INCOMPLETE_EXPLANATION","Native action, conditions, or supporting references missing")
                if(r.verdict==Verdict.MATCH && r.circumstances==null)
                    issue("INCOMPLETE_STRUCTURED_CONSTRAINTS","Native evidence exists but typed circumstance properties are absent","SUSPECTED_MISMATCH")
            }
        }
        CaseEvaluation(c.id,c.card,c,mapping,matches,issues,when {
            issues.any { it.certainty=="CONFIRMED_MISMATCH" } -> "CONFIRMED_MISMATCH"
            issues.any { it.certainty=="INSUFFICIENT_EVIDENCE" } -> "UNRESOLVED"
            issues.isNotEmpty() -> "MATCH_WITH_REVIEW_NOTES"
            else -> "MATCH"
        })
    }
    val confirmed=rows.filter { it.expected.reviewStatus=="CONFIRMED" && it.mappingStatus !in setOf("CARD_UNAVAILABLE","INELIGIBLE_OR_UNKNOWN_ELIGIBILITY","AMBIGUOUS_MAPPING","OUTSIDE_CONVENTIONAL_SEARCH") }
    val positive=confirmed.filter { it.expected.expectedCategory!=null }
    val excluded=confirmed.filter { it.expected.expectedCategory==null }
    val mechanical=confirmed.filter { it.expected.expectedMechanical==Verdict.MATCH }
    val missing=positive.count { row -> row.actual.none { it.verdict==Verdict.MATCH && it.contributionClassification==row.expected.expectedCategory } }
    val unreviewed=report.results.filter { r -> r.verdict==Verdict.MATCH && review.cases.any { it.card==r.card } && review.cases.none { reviewMatches(it,r) } }
    val cards=rows.groupBy { it.card }.map { (card,rs) ->
        val expected=rs.mapNotNull { it.expected.expectedCategory }.distinct().sortedBy { it.name }
        val actual=report.results.filter { it.card==card && it.verdict==Verdict.MATCH }.map { it.branch }.distinct().sortedBy { it.name }
        CardEvaluation(card,expected,actual,rs.map { it.id },when {
            rs.any { it.status=="CONFIRMED_MISMATCH" } -> "CONFIRMED_PATHWAY_MISMATCH"
            unreviewed.any { it.card==card } -> "SUSPECTED_UNREVIEWED_ACCEPTANCE"
            rs.any { it.status=="UNRESOLVED" } -> "UNRESOLVED"
            else -> "REVIEWED_CASES_MATCH"
        })
    }
    return BenchmarkReport(review.reviewVersion,sha256,report.commander,
        linkedMapOf("reviewed_cases" to rows.size,"reviewed_cards" to cards.size,"confirmed_evaluable_cases" to confirmed.size,
            "confirmed_expected_accepted_pathways" to positive.size,"confirmed_expected_excluded_cases" to excluded.size,"confirmed_mechanical_positive_cases" to mechanical.size,
            "expected_unknown_interpretation_boundaries" to confirmed.count { it.expected.expectedMechanical==Verdict.UNKNOWN },
            "confirmed_expected_mechanical_nonmatches" to confirmed.count { it.expected.expectedMechanical==Verdict.NO_MATCH }),
        linkedMapOf("expected_accepted_recovered" to positive.size-missing,"expected_accepted_missed" to missing,
            "excluded_correctly_not_accepted" to excluded.count { x -> x.actual.none { it.verdict==Verdict.MATCH } },
            "incorrect_accepted_reviewed_cases" to excluded.count { x -> x.actual.any { it.verdict==Verdict.MATCH } },
            "confirmed_mechanical_positive_recovered" to mechanical.count { x -> x.actual.any { mechanicalVerdict(it)==Verdict.MATCH } },
            "confirmed_mismatch_cases" to rows.count { it.status=="CONFIRMED_MISMATCH" },"unresolved_cases" to rows.count { it.status=="UNRESOLVED" },
            "suspected_or_incomplete_cases" to rows.count { it.issues.any { i -> i.certainty=="SUSPECTED_MISMATCH" } },"unreviewed_accepted_paths" to unreviewed.size,
            "unknown_boundaries_preserved" to confirmed.count { it.expected.expectedMechanical==Verdict.UNKNOWN && it.actual.isNotEmpty() && it.actual.all { r -> mechanicalVerdict(r)==Verdict.UNKNOWN } }),rows,cards,unreviewed)
}
fun evaluateBenchmark(report: DiscoveryReport,file: Path): BenchmarkReport = evaluateBenchmark(report,readBenchmark(file),
    MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)).joinToString("") { "%02x".format(it) })
fun benchmarkSummary(r: BenchmarkReport): String = buildString {
    appendLine("Reviewed benchmark: ${r.reviewVersion}; ${r.commander}; SHA ${r.reviewSha256}")
    appendLine("Denominators: ${r.denominators}"); appendLine("Numerators: ${r.numerators}")
    r.cases.filter { it.status!="MATCH" }.forEach { appendLine("${it.id} | ${it.mappingStatus} | ${it.status} | ${it.issues}") }
    r.limitations.forEach { appendLine(it) }
}

@Serializable data class BenchmarkCaseChange(val id: String,val card: String,val beforeStatus: String?,val afterStatus: String?,
    val beforeIssues: List<BenchmarkIssue>,val afterIssues: List<BenchmarkIssue>,val before: List<ResultEvidence>,val after: List<ResultEvidence>)
@Serializable data class BenchmarkChangeReport(val sameFrozenReview: Boolean,val beforeNumerators: Map<String,Int>,val afterNumerators: Map<String,Int>,
    val beforeDenominators: Map<String,Int>,val afterDenominators: Map<String,Int>,val changes: List<BenchmarkCaseChange>,
    val limitations: String="Reviewed sample transitions only, not MTG corpus recall; changing the review invalidates controlled comparison")
fun benchmarkDiff(before: BenchmarkReport,after: BenchmarkReport): BenchmarkChangeReport {
    val b=before.cases.associateBy { it.id };val a=after.cases.associateBy { it.id }
    return BenchmarkChangeReport(before.reviewSha256==after.reviewSha256,before.numerators,after.numerators,before.denominators,after.denominators,
        (b.keys+a.keys).sorted().mapNotNull { id -> val old=b[id];val now=a[id]
            if(old==now) null else BenchmarkCaseChange(id,now?.card ?: old!!.card,old?.status,now?.status,old?.issues.orEmpty(),now?.issues.orEmpty(),old?.actual.orEmpty(),now?.actual.orEmpty())
        })
}
