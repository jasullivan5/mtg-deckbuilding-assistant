package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.explorer.*
import dev.mtg.assistant.extensions.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*

class EvidenceSummaryTest : FunSpec({
    val card = JudithTheScourgeDiva
    fun record() = TranslationEvidence(card.oracleId!!, definitionDigest(card), ARGENTUM_SHA, "isolated-fixture-author",
        fidelity = FidelityState.SOURCE_REVIEWED, execution = ExecutionState.BOUNDED_EVIDENCE_PASSED,
        source = listOf(EvidenceReference("isolated-source", "a".repeat(64))),
        requirements = listOf(EvidenceReference("isolated-requirements", "b".repeat(64))),
        tests = listOf(EvidenceReference("isolated-execution", "c".repeat(64))))
    fun row(verdict: Verdict = Verdict.MATCH) = ResultEvidence(
        card = "Isolated fixture", identifier = "fixture", face = "front", actionId = "fixture-action",
        abilityPath = "ability.cost", branch = Contribution.DIRECT_DEATH_GENERATOR, verdict = verdict,
        operation = "SACRIFICE", costOrEffect = "ACTIVATION_COST", affectedScope = "creature", prerequisites = emptyList(),
        dependency = null, nativeOperation = JsonObject(emptyMap()), decisions = emptyList(), sourceReferences = emptyList(),
        commanderLegality = Verdict.MATCH, deckEligibility = Verdict.MATCH)
    fun statuses(assurance: TranslationAssurance?, scenarios: List<String> = emptyList(), rows: List<ResultEvidence> = listOf(row())) =
        evidenceIndicators(rows, assurance, scenarios, "Isolated sacrifice cost").associateBy { it.id }

    test("mechanical relevance does not manufacture source review execution or independent approval") {
        val states = statuses(null)
        states.getValue("mechanic").state shouldBe EvidenceState.ESTABLISHED
        for (id in listOf("source", "execution", "independent")) states.getValue(id).state shouldBe EvidenceState.NOT_RECORDED
        states.getValue("execution").explanation.contains("does not mean the engine cannot") shouldBe true
    }
    test("listed scenario files are incomplete evidence rather than a recorded pass") {
        val state = statuses(null, listOf("some-scenario.kt")).getValue("execution")
        state.state shouldBe EvidenceState.INCOMPLETE
        state.limits.any { "does not prove the test ran or passed" in it } shouldBe true
    }
    test("stale definition binding cannot become a positive source or execution label") {
        val old = assessTranslation(card, record().copy(definitionSha256 = "0".repeat(64)))
        old.evidenceCurrent shouldBe false
        val states = statuses(old)
        states.getValue("source").state shouldBe EvidenceState.OUTDATED
        states.getValue("execution").state shouldBe EvidenceState.OUTDATED
        states.getValue("independent").state shouldBe EvidenceState.NOT_RECORDED
    }
    test("a different incompatible engine retains old results without presenting them as fresh") {
        val old = assessTranslation(card, record().copy(argentumCommit = "1".repeat(40)))
        old.evidenceCurrent shouldBe false
        val state = statuses(old).getValue("execution")
        state.state shouldBe EvidenceState.OUTDATED
        state.historical shouldBe true
        state.recordedEngine shouldBe "1".repeat(40)
        val oldFailure = assessTranslation(card, record().copy(argentumCommit = "1".repeat(40), execution = ExecutionState.FAILURE_OBSERVED))
        val failure = statuses(oldFailure).getValue("execution")
        failure.state shouldBe EvidenceState.OUTDATED
        failure.established.any { "earlier record reports an observed failure" in it } shouldBe true
    }
    test("an applicable historical gate output still labels recorded execution as historical") {
        // Isolated mapper input: simulate an accepted implementation-compatible binding on a different build.
        val approvedBinding = assessTranslation(card, record())
        val state = evidenceIndicators(listOf(row()), approvedBinding, emptyList(), "Fixture cost", "2".repeat(40)).single { it.id == "execution" }
        state.state shouldBe EvidenceState.ESTABLISHED
        state.historical shouldBe true
        state.status shouldBe "Historical pass recorded"
        state.limits.any { "not a fresh run" in it } shouldBe true
    }
    test("author source review and bounded passes stay distinct from independent qualification") {
        val states = statuses(assessTranslation(card, record()))
        states.getValue("source").state shouldBe EvidenceState.ESTABLISHED
        states.getValue("execution").state shouldBe EvidenceState.ESTABLISHED
        states.getValue("independent").state shouldBe EvidenceState.NOT_RECORDED
        states.getValue("source").scope.contains("card's implementation") shouldBe true
        states.getValue("source").limits.any { "not an independent" in it } shouldBe true
    }
    test("an observed failure is an actual problem rather than absence of testing") {
        val states = statuses(assessTranslation(card, record().copy(execution = ExecutionState.FAILURE_OBSERVED)))
        states.getValue("execution").state shouldBe EvidenceState.KNOWN_PROBLEM
        states.getValue("independent").state shouldBe EvidenceState.NOT_RECORDED
    }
    test("confirmed and suspected source disagreements receive different states") {
        statuses(assessTranslation(card, record().copy(fidelity = FidelityState.CONFIRMED_DISCREPANCY))).getValue("source").state shouldBe EvidenceState.KNOWN_PROBLEM
        statuses(assessTranslation(card, record().copy(fidelity = FidelityState.SUSPECTED_DISCREPANCY))).getValue("source").state shouldBe EvidenceState.INCOMPLETE
    }
    test("only the existing exact independent approval gate can produce an approval label") {
        val e = record()
        val approval = IndependentApproval("isolated-independent-reviewer", "2026-10-30", "isolated fixture only", bundleDigest(e), true)
        val approved = assessTranslation(card, e.copy(independentApproval = approval))
        statuses(approved).getValue("independent").state shouldBe EvidenceState.ESTABLISHED
        val invalid = assessTranslation(card, e.copy(independentApproval = approval.copy(reviewer = e.author)))
        invalid.qualification shouldBe QualificationState.WITHDRAWN
        statuses(invalid).getValue("independent").state shouldBe EvidenceState.OUTDATED
    }
    test("an unresolved relationship is not the same as a recognized but untested mechanic") {
        statuses(null, rows = listOf(row(Verdict.UNKNOWN))).getValue("mechanic").state shouldBe EvidenceState.INCOMPLETE
        statuses(null, rows = listOf(row())).getValue("mechanic").state shouldBe EvidenceState.ESTABLISHED
        statuses(null, rows = emptyList()).getValue("mechanic").state shouldBe EvidenceState.NOT_RECORDED
    }
    test("partial ability interpretation preserves the scoped positive while concerns remain separate") {
        val partial = row().copy(localSupport = SupportStatus.PARTIALLY_SUPPORTED)
        val state = statuses(null, rows = listOf(partial)).getValue("mechanic")
        state.state shouldBe EvidenceState.ESTABLISHED
        state.scope shouldBe "Isolated sacrifice cost"
        state.limits.any { "not every effect" in it } shouldBe true
        val concern = TranslationConcern("fixture", ConcernKind.ENGINE_EXECUTION, ConcernState.CONFIRMED,
            "Historical scoped failure", "Recorded problem", "fixture reproduction", "Record scoped resolution")
        val assessment = assessTranslation(card, record().copy(concerns = listOf(concern)))
        materialConcerns(assessment).single().state shouldBe EvidenceState.KNOWN_PROBLEM
        materialConcerns(assessment).single().scope.contains("not automatically assigned") shouldBe true
        statuses(assessment).getValue("mechanic").state shouldBe EvidenceState.ESTABLISHED
    }
})
