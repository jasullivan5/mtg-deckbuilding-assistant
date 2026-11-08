package dev.mtg.assistant

import dev.mtg.assistant.coverage.*
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow

class GlobalBaselineTest : FunSpec({
    val snapshot=SnapshotManifest(bulkType="oracle_cards",bulkId="fixture",updatedAt="2026-11-08T00:00:00Z",downloadedAt="2026-11-08T00:00:00Z",sourceUri="https://api.scryfall.com/bulk-data/oracle_cards",downloadUri="https://data.scryfall.io/fixture",format="jsonl",compression="none",sha256="fixture",bytes=1)
    fun entry(id:String,legality:String="legal",association:String="MISSING")=GlobalBaselineEntry(id,id,legality,if(legality=="legal") "ELIGIBLE" else "EXCLUDED",association,if(association=="CONFIDENT") id else null,null,emptyList())
    fun baseline(rows:List<GlobalBaselineEntry>)=GlobalBaseline(applicationRevision="fixture",engine="engine-a",snapshot=snapshot,identityAlgorithm=IDENTITY_ALGORITHM,nativeIdentitySha256="native-a",populationPolicy="physical-v1",
        counts=mapOf("eligible_oracle_cards" to rows.count { it.poolState=="ELIGIBLE" },"confident_native_identity_associations" to rows.count { it.poolState=="ELIGIBLE" && it.association=="CONFIDENT" }),exclusions=emptyMap(),records=rows,unresolvedWithoutOracleId=0,
        commanders=CommanderInventory("fixture","engine-a",emptyMap(),emptyList(),emptyList()),objectiveScope=ObjectiveInventory("fixture","engine-a",emptyList(),emptyList()),pilotRequirementsSha256="requirements")
    test("source growth legality changes integration gains uncertain resolutions and regressions stay separate") {
        val before=baseline(listOf(entry("existing"),entry("uncertain",association="AMBIGUOUS"),entry("lost",association="CONFIDENT"),entry("banned","banned"),entry("departing",association="CONFIDENT")))
        val after=baseline(listOf(entry("existing",association="CONFIDENT"),entry("uncertain",association="CONFIDENT"),entry("lost"),entry("banned"),entry("departing","banned"),entry("new"))).copy(engine="engine-b",snapshot=snapshot.copy(sha256="later-source"))
        val diff=compareGlobalBaselines(before,after)
        diff.sameSourceUniverse shouldBe false
        diff.beforeEngine shouldBe "engine-a";diff.afterEngine shouldBe "engine-b"
        diff.newlyAddedLegalCards shouldBe listOf("new")
        diff.existingCardsNewlyLegal shouldBe listOf("banned")
        diff.newlyIntegratedExistingCards shouldBe listOf("existing")
        diff.resolvedUncertainAssociations shouldBe listOf("uncertain")
        diff.lostCoverageWithinStillLegalPopulation shouldBe listOf("lost")
        diff.noLongerLegal shouldBe listOf("departing")
    }
    test("changed definitions suppress comparable gain claims and malformed denominators fail") {
        val before=baseline(listOf(entry("a")))
        val after=baseline(listOf(entry("a",association="CONFIDENT"))).copy(populationPolicy="changed")
        compareGlobalBaselines(before,after).comparableDefinitions shouldBe false
        compareGlobalBaselines(before,after).newlyIntegratedExistingCards shouldBe emptyList()
        shouldThrow<IllegalArgumentException> { compareGlobalBaselines(before,before.copy(records=before.records+before.records)) }
        shouldThrow<IllegalArgumentException> { compareGlobalBaselines(before,before.copy(counts=mapOf("eligible_oracle_cards" to 4,"confident_native_identity_associations" to 0))) }
    }
    test("representative selection and discovery use actual current interfaces without claiming all commanders ran") {
        val catalog=Catalog()
        for(name in listOf("Judith, the Scourge Diva","Krenko, Mob Boss","Ishai, Ojutai Dragonspeaker")) inspectCommander(catalog,name).selectable shouldBe true
        for(name in listOf("Murder","Daretti, Scrap Savant","Judith, the Scourge Diva + Krenko, Mob Boss")) inspectCommander(catalog,name).selectable shouldBe false
        inspectCommander(catalog,"Agent Frank Horrigan").blockers.any { it.code == CommanderBlocker.IDENTITY_UNRESOLVED } shouldBe true
        val unusual=discover(catalog,"Ishai, Ojutai Dragonspeaker",objective="create-creature-tokens")
        unusual.commander shouldBe "Ishai, Ojutai Dragonspeaker"
        unusual.mechanicalInputCards!!.isNotEmpty() shouldBe true
        unusual.results.any { it.verdict==Verdict.MATCH } shouldBe true
    }
})
