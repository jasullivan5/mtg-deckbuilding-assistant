package dev.mtg.assistant

import dev.mtg.assistant.coverage.*
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.*

class OracleEnrichmentCatalogTest : FunSpec({
    val root=if(Files.exists(Path.of("reference"))) Path.of(".") else Path.of("..")
    val source=root.resolve("reference/phase28/development-oracle.json")
    val snapshot by lazy {
        val dir=Files.createTempDirectory("identity-review")
        try {
            val file=dir.resolve("cards.json");Files.copy(source,file)
            val manifest=SnapshotManifest(bulkType="oracle_cards",bulkId="synthetic-reviewed-subset",updatedAt="2026-10-26T21:01:59.922Z",downloadedAt="2026-10-27T00:00:00Z",
                sourceUri="https://api.scryfall.com/bulk-data/oracle_cards",downloadUri="https://data.scryfall.io/review-subset.json",format="json-array",compression="none",sha256=sha256(file),bytes=Files.size(file))
            Files.writeString(Path.of("$file.manifest.json"),Json.encodeToString(manifest));loadOracleSnapshot(file)
        } finally { dir.toFile().deleteRecursively() }
    }
    val catalog by lazy { Catalog() }
    val index by lazy { NativeIndex(physicalCards(catalog.cards).map { nativeIdentity(it,catalog.provenance(it.name).origin) },snapshot) }
    val cases=Json.parseToJsonElement(Files.readString(root.resolve("reference/phase28/development-review.json"))).jsonObject.getValue("cases").jsonArray
    for(case in cases) {
        val row=case.jsonObject;val id=row.getValue("externalOracleId").jsonPrimitive.content;val name=row.getValue("externalName").jsonPrimitive.content
        test("source-led real catalog review: $name $id") {
            val e=snapshot.cards.single { it.oracleId==id };val r=index.match(e)
            val observed=r.association?.confidence?.name ?: "MISSING"
            observed shouldBe row.getValue("expected").jsonPrimitive.content
            if(observed=="CONFIDENT") r.candidates.single().name shouldBe row.getValue("nativeName").jsonPrimitive.content
            row["expectedMetadataIssues"]?.let { r.metadata!!.issues.toSet() shouldBe it.jsonArray.map { e -> e.jsonPrimitive.content }.toSet() }
        }
    }
    test("real color metadata errors do not invalidate physical identity or mutate native eligibility") {
        for(name in listOf("Heartwood Shard","Hooded Kavu","Firescreamer","Sunforger","Toxrill, the Corrosive")) {
            val c=catalog.card(name)!!;val before=nativeIdentity(c,catalog.provenance(name).origin)
            val e=snapshot.cards.single { it.name==name };val r=index.match(e)
            r.association!!.confidence shouldBe IdentityConfidence.CONFIDENT
            r.metadata!!.issues.contains("NATIVE_EXTERNAL_IDENTITY_DISAGREEMENT") shouldBe true
            nativeIdentity(c,catalog.provenance(name).origin) shouldBe before
        }
    }
    test("global mapping does not report a meld result as an independent deck physical definition") {
        physicalCards(catalog.cards).any { it.meldResult } shouldBe false
        index.enrichment!!.assignments.filter { it.confidence==IdentityConfidence.CONFIDENT }.map { it.oracleId }.distinct().size shouldBe index.enrichment!!.assignments.count { it.confidence==IdentityConfidence.CONFIDENT }
    }
})
