package dev.mtg.assistant

import dev.mtg.assistant.coverage.*
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.explorer.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import kotlinx.serialization.json.*

class GlobalCoverageTest : FunSpec({
    fun source(name:String="Fixture",legality:String="legal",layout:String="normal",identity:List<String>?=listOf("G")) = OracleIdentity(
        "11111111-1111-1111-1111-111111111111",name,layout,identity,"Legendary Creature — Human","","{G}",emptyList(),mapOf("commander" to legality),emptyList(),emptyList(),
        OracleSource("22222222-2222-2222-2222-222222222222",null,null,null,null,null),power="1",toughness="1")
    test("whole-format population does not inherit Judith colors or treat unknown legality as excluded") {
        externalEligibility(source(),null).state shouldBe PoolState.ELIGIBLE
        externalEligibility(source(),setOf("B","R")).state shouldBe PoolState.EXCLUDED
        for (legality in listOf("banned","not_legal")) externalEligibility(source(legality=legality),null).state shouldBe PoolState.EXCLUDED
        for (legality in listOf("restricted","unrecognized")) externalEligibility(source(legality=legality),null).state shouldBe PoolState.UNRESOLVED
        externalEligibility(source(identity=null).copy(issues=listOf("MISSING_OR_INVALID_FULL_COLOR_IDENTITY")),null).state shouldBe PoolState.ELIGIBLE
        externalEligibility(source().copy(issues=listOf("CONFLICTING_RECORDS_FOR_ORACLE_ID")),null).state shouldBe PoolState.UNRESOLVED
    }
    test("front identities are not doubled by faces printings aliases or meld results") {
        externalEligibility(source(layout="transform"),null).state shouldBe PoolState.ELIGIBLE
        externalEligibility(source(layout="token"),null).state shouldBe PoolState.EXCLUDED
        val meld=source(layout="meld").copy(relatedParts=listOf("""{"id":"22222222-2222-2222-2222-222222222222","component":"meld_result"}"""))
        externalEligibility(meld,null).reasons shouldBe listOf("MELD_RESULT_NOT_SEPARATE_PHYSICAL_CARD")
        externalEligibility(meld.copy(relatedParts=emptyList()),null).state shouldBe PoolState.UNRESOLVED
        externalEligibility(meld.copy(relatedParts=listOf("""{"id":"22222222-2222-2222-2222-222222222222","component":"meld_part"}""")),null).state shouldBe PoolState.ELIGIBLE
        // Duplicate printing and conflicting-record collapse is exercised by existing OracleSnapshot/CatalogCoverage tests.
    }
    test("source candidate inventory reuses the SDK and keeps alternative forms outside its denominator") {
        sourceCommanderClass(source()).first shouldBe "SDK_IDENTIFIED_SINGLE_CANDIDATE"
        sourceCommanderClass(source().copy(typeLine="Legendary Planeswalker — Fixture",oracleText="Fixture can be your commander.")).first shouldBe "SDK_IDENTIFIED_SINGLE_CANDIDATE"
        sourceCommanderClass(source().copy(typeLine="Legendary Artifact — Vehicle")).first shouldBe "SPECIAL_NONCREATURE_FORM_REVIEW"
        sourceCommanderClass(source().copy(typeLine="Legendary Enchantment — Background")).first shouldBe "BACKGROUND_COMPONENT"
        sourceCommanderClass(source().copy(typeLine="Creature — Human")).first shouldBe "NOT_IDENTIFIED_BY_BOUNDED_PREDICATE"
        sourceCommanderClass(source().copy(typeLine=null)).first shouldBe "SOURCE_UNRESOLVED"
    }
    test("objective reach never sums overlapping card categories or calls no pathway a nonmatch") {
        val base=DiscoveryReport(objective=objectives.first().id,commander="fixture",allowedIdentity=emptyList(),conventionalQuery=null,counts=emptyMap(),eligibility=emptyList(),results=emptyList(),actions=emptyList(),unsupported=emptyList(),limitations=emptyList(),mechanicalInputCards=listOf("No emitted record"))
        val report=base.copy(results=emptyList(),unsupported=listOf(Unsupported("No emitted record","front","script","Fixture","unread","a")),mechanicalInputCards=listOf("No emitted record"))
        val reach=objectiveReach(report,objectives.first())
        reach.counts["eligible_physical_cards_examined"] shouldBe 1
        reach.counts["cards_without_emitted_relevant_pathway"] shouldBe 1
        reach.counts["distinct_cards_with_unsupported_records"] shouldBe 1
        reach.counts["distinct_nonmatch_cards"] shouldBe 0
    }
})
