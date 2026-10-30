package dev.mtg.assistant

import dev.mtg.assistant.coverage.*
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.extensions.*
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.dsl.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path

class OracleEnrichmentCoverageTest : FunSpec({
    val name="Synthetic Identity Token Artifact"
    val id="55555555-5555-5555-5555-555555555555"
    val dir=Files.createTempDirectory("metadata-objective")
    val file=dir.resolve("cards.json")
    Files.writeString(file,"""[{"object":"card","oracle_id":"$id","name":"$name","layout":"normal","mana_cost":"{0}","type_line":"Artifact","oracle_text":"Synthetic fixture","color_identity":[],"legalities":{"commander":"legal"}}]""")
    val manifest=SnapshotManifest(bulkType="oracle_cards",bulkId="synthetic",updatedAt="2026-10-26T00:00:00Z",downloadedAt="2026-10-26T00:00:00Z",
        sourceUri="https://api.scryfall.com/bulk-data/oracle_cards",downloadUri="https://data.scryfall.io/synthetic.json",format="json-array",compression="none",sha256=sha256(file),bytes=Files.size(file))
    Files.writeString(Path.of("$file.manifest.json"),Json.encodeToString(manifest))
    val snapshot=loadOracleSnapshot(file)
    afterSpec { dir.toFile().deleteRecursively() }
    fun definition(identity: String)=card(name) { manaCost="{0}";colorIdentity=identity;typeLine="Artifact"
        activatedAbility { cost=Costs.Tap;effect=Effects.CreateToken(1,1,creatureTypes=setOf("Test")) }
    }.copy(oracleId=id,legalFormats=setOf(DeckFormat.COMMANDER))
    fun data(identity: String)=Catalog(Extensions.all+ExtensionDefinition(definition(identity),Provenance("APPLICATION_EXTENSION","synthetic-metadata-fixture","2026-10-27")))
    test("confident association attaches existing conditional objective evidence without hiding metadata disagreement") {
        val c=data("R")
        val before=discover(c,"Judith, the Scourge Diva","name:Identity","create-creature-tokens")
        val report=catalogCoverage(c,snapshot,"Judith, the Scourge Diva",listOf("create-creature-tokens"))
        val row=report.records.single()
        row.mapping.association!!.confidence shouldBe IdentityConfidence.CONFIDENT
        row.mapping.metadata!!.issues shouldBe listOf("NATIVE_EXTERNAL_IDENTITY_DISAGREEMENT")
        row.status shouldBe CatalogStatus.NATIVE_METADATA_UNRESOLVED
        row.objectives.values.single().matchedPathways shouldBe 1
        row.objectives.values.single().conditionalMatchedPathways shouldBe 1
        discover(c,"Judith, the Scourge Diva","name:Identity","create-creature-tokens") shouldBe before
    }
    test("an external identity association does not bypass a native color restriction") {
        val c=data("G")
        val row=catalogCoverage(c,snapshot,"Judith, the Scourge Diva",listOf("create-creature-tokens")).records.single()
        row.mapping.association!!.confidence shouldBe IdentityConfidence.CONFIDENT
        row.objectives.values.single().state shouldBe "NOT_EVALUATED_NATIVE_ELIGIBILITY_DISAGREEMENT"
        row.objectives.values.single().matchedPathways shouldBe 0
        selectedCommanderEligibility(c.card(name)!!,setOf(com.wingedsheep.sdk.core.Color.BLACK,com.wingedsheep.sdk.core.Color.RED)) shouldBe Verdict.NO_MATCH
    }
})
