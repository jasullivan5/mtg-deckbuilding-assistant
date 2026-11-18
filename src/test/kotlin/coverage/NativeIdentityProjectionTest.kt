package dev.mtg.assistant

import dev.mtg.assistant.coverage.*
import dev.mtg.assistant.discovery.physicalCards
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Files

class NativeIdentityProjectionTest:FunSpec({
    val catalog by lazy { Catalog() }
    val snapshot by lazy {
        val fixture=Json.parseToJsonElement(Files.readString(TestFiles.root.resolve("src/test/resources/identity/native-omen-sources.json"))).jsonObject
        val dir=Files.createTempDirectory("omen-source-review")
        val cards=dir.resolve("cards.json");val sidecar=dir.resolve("cards.json.manifest.json")
        try {
            Files.writeString(cards,fixture.getValue("cards").toString())
            val original=Json.decodeFromJsonElement<SnapshotManifest>(fixture.getValue("sourceManifest"))
            val manifest=original.copy(bulkId=original.bulkId+"-derived-omen-fixture",
                format="json-array",compression="none",sha256=sha256(cards),bytes=Files.size(cards))
            Files.writeString(sidecar,Json.encodeToString(manifest))
            loadOracleSnapshot(cards)
        } finally { Files.deleteIfExists(sidecar);Files.deleteIfExists(cards);Files.deleteIfExists(dir) }
    }
    test("Omen identity uses the source physical layout while native Omen mechanics and ordered faces remain intact") {
        val index=NativeIndex(physicalCards(catalog.cards).map { nativeIdentity(it,catalog.provenance(it.name).origin) },snapshot)
        snapshot.cards.forEach { source ->
            val name=source.faces.first().name!!;val card=catalog.card(name)!!
            card.layout.name shouldBe "OMEN"
            val identity=nativeIdentity(card,catalog.provenance(name).origin)
            identity.layout shouldBe source.layout
            identity.faces.map { it.name } shouldBe source.faces.map { it.name }
            index.match(source).association!!.confidence shouldBe IdentityConfidence.CONFIDENT
            val altered=identity.copy(faces=identity.faces.mapIndexed { i,face->if(i==1)face.copy(manaCost="{99}") else face })
            OracleEnrichment(listOf(altered),snapshot).assignments.single().confidence shouldBe IdentityConfidence.REVIEW_REQUIRED
        }
    }
})
