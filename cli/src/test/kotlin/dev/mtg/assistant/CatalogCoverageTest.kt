package dev.mtg.assistant

import dev.mtg.assistant.coverage.*
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPOutputStream

class CatalogCoverageTest : FunSpec({
    val root=if(Files.exists(Path.of("reference"))) Path.of(".") else Path.of("..")
    val fixture=root.resolve("reference/phase27/fixtures/oracle-cards.json")
    fun manifest(path: Path,format: String="json-array",compression: String="none") = SnapshotManifest(
        bulkType="oracle_cards",bulkId="synthetic-fixture",updatedAt="2026-10-26T00:00:00Z",downloadedAt="2026-10-26T01:00:00Z",
        sourceUri="https://api.scryfall.com/bulk-data/oracle_cards",downloadUri="https://data.scryfall.io/synthetic-fixture.json",
        format=format,compression=compression,sha256=sha256(path),bytes=Files.size(path))
    fun load(text: String=Files.readString(fixture),format: String="json-array",gzip: Boolean=false): OracleSnapshot {
        val dir=Files.createTempDirectory("oracle-fixture")
        try {
            val p=dir.resolve("cards")
            if(gzip) GZIPOutputStream(Files.newOutputStream(p)).use { it.write(text.toByteArray()) } else Files.writeString(p,text)
            Files.writeString(Path.of("$p.manifest.json"),Json.encodeToString(manifest(p,format,if(gzip) "gzip" else "none")))
            return loadOracleSnapshot(p)
        } finally { dir.toFile().deleteRecursively() }
    }
    val snapshot by lazy { load() }
    fun card(name: String)=snapshot.cards.single { it.name==name }
    val br=setOf("B","R")
    val catalog by lazy { Catalog() }
    fun native(c: OracleIdentity=card("Fixture Colorless"),id: String?=c.oracleId,name: String=c.name!!,
        identity: Set<String>?=c.colorIdentity?.toSet(),legality: String="MATCH") =
        NativeIdentity(name,id,setOf(name.lowercase()),identity,legality,"UPSTREAM_ARGENTUM")

    test("fixture retains twelve physical records and an adventure or DFC never becomes two cards") {
        snapshot.rawRecords shouldBe 12;snapshot.cards.size shouldBe 12
        card("Fixture Front // Fixture Back").faces.size shouldBe 2
        card("Fixture Adventure // Fixture Trick").faces.size shouldBe 2
        card("Fixture Colorless").source.scryfallId shouldBe "printing-one"
    }
    test("full identity includes hybrid activated symbols and all faces without parsing Oracle mechanics") {
        externalEligibility(card("Fixture Colorless"),emptySet()).state shouldBe PoolState.ELIGIBLE
        externalEligibility(card("Fixture Rakdos"),br).state shouldBe PoolState.ELIGIBLE
        externalEligibility(card("Fixture Rakdos"),setOf("R")).state shouldBe PoolState.EXCLUDED
        externalEligibility(card("Fixture Off-Color Symbol"),br).state shouldBe PoolState.EXCLUDED
        externalEligibility(card("Fixture Front // Fixture Back"),br).state shouldBe PoolState.EXCLUDED
        externalEligibility(card("Fixture Adventure // Fixture Trick"),br).state shouldBe PoolState.ELIGIBLE
    }
    test("bans unknown metadata and basic land types are distinct") {
        externalEligibility(card("Fixture Banned"),br).reasons shouldBe listOf("NOT_COMMANDER_LEGAL")
        for(name in listOf("Fixture Unknown Identity","Fixture Invalid Oracle ID","Fixture Missing Legality"))
            externalEligibility(card(name),br).state shouldBe PoolState.UNRESOLVED
        externalEligibility(card("Fixture Forest Type"),br).reasons shouldBe listOf("BASIC_LAND_TYPE_RESTRICTION_903_5D")
        externalEligibility(card("Fixture Forest Type"),setOf("G")).state shouldBe PoolState.ELIGIBLE
    }
    test("special roles and quantity exceptions never grant construction permission") {
        externalEligibility(card("Fixture Companion"),br).state shouldBe PoolState.ELIGIBLE
        externalEligibility(card("Fixture Companion"),br).constructionNotes.size shouldBe 1
        externalEligibility(card("Fixture Quantity Exception"),br).constructionNotes.size shouldBe 1
        externalEligibility(card("Fixture Companion").copy(keywords=listOf("Choose a background")),br).constructionNotes.size shouldBe 1
        externalEligibility(card("Fixture Colorless").copy(oracleText="If this is your commander, choose a color before the game begins."),br).constructionNotes.size shouldBe 1
    }
    test("loader supports JSON array and gzipped JSONL with identical projected identities") {
        val lines=Json.parseToJsonElement(Files.readString(fixture)).jsonArray.joinToString("\n")
        load(lines,"jsonl",true).cards shouldBe snapshot.cards
    }
    test("exact duplicate printings collapse while conflicting Oracle records are quarantined") {
        val a=Json.parseToJsonElement(Files.readString(fixture)).jsonArray.first().jsonObject
        val reprint=JsonObject(a+mapOf("id" to JsonPrimitive("another-printing")))
        val duplicate=load(JsonArray(listOf(a,reprint)).toString())
        duplicate.cards.size shouldBe 1;duplicate.duplicateRecords shouldBe 1
        val conflict=load(JsonArray(listOf(a,JsonObject(a+mapOf("color_identity" to JsonArray(listOf(JsonPrimitive("R"))))))).toString())
        conflict.cards.single().issues shouldBe listOf("CONFLICTING_RECORDS_FOR_ORACLE_ID")
        externalEligibility(conflict.cards.single(),br).state shouldBe PoolState.UNRESOLVED
    }
    test("manifest integrity failure and unversioned input fail closed") {
        val dir=Files.createTempDirectory("oracle-corrupt")
        try {
            val p=dir.resolve("cards.json"); Files.writeString(p,Files.readString(fixture))
            shouldThrow<IllegalArgumentException> { loadOracleSnapshot(p) }
            Files.writeString(Path.of("$p.manifest.json"),Json.encodeToString(manifest(p)))
            Files.writeString(p,"[]")
            shouldThrow<IllegalArgumentException> { loadOracleSnapshot(p) }
        } finally { dir.toFile().deleteRecursively() }
    }
    test("external name collisions across distinct Oracle IDs are explicit pool ambiguity") {
        val a=Json.parseToJsonElement(Files.readString(fixture)).jsonArray.first().jsonObject
        val b=JsonObject(a+mapOf("oracle_id" to JsonPrimitive("11111111-1111-1111-1111-111111111111")))
        val s=load(JsonArray(listOf(a,b)).toString())
        s.cards.size shouldBe 2
        s.cards.all { "EXTERNAL_NAME_OR_FACE_COLLISION" in it.issues } shouldBe true
        s.cards.all { externalEligibility(it,br).state==PoolState.UNRESOLVED } shouldBe true
    }
    test("Oracle ID is authoritative across renames while name-only candidates stay unresolved") {
        val c=card("Fixture Colorless")
        NativeIndex(listOf(native(name="Renamed Native"))).match(c).method shouldBe "ORACLE_ID"
        NativeIndex(listOf(native(id=null))).match(c).status shouldBe CatalogStatus.NATIVE_METADATA_UNRESOLVED
        NativeIndex(listOf(native(id="11111111-1111-1111-1111-111111111111"))).match(c).status shouldBe CatalogStatus.AMBIGUOUS_MAPPING
        NativeIndex(emptyList()).match(c).status shouldBe CatalogStatus.NATIVE_DEFINITION_MISSING
    }
    test("native duplicate Oracle IDs and multiple face aliases cannot silently choose a definition") {
        val c=card("Fixture Colorless")
        NativeIndex(listOf(native(),native(name="Collision"))).match(c).status shouldBe CatalogStatus.AMBIGUOUS_MAPPING
        val a=native(id=null).copy(aliases=setOf("fixture front"))
        val b=native(id=null,name="Second Native").copy(aliases=setOf("fixture back"))
        NativeIndex(listOf(a,b)).match(card("Fixture Front // Fixture Back")).status shouldBe CatalogStatus.AMBIGUOUS_MAPPING
    }
    test("full identity and legality gaps are separate from a missing definition") {
        val c=card("Fixture Colorless")
        for(n in listOf(native(identity=null),native(legality="UNKNOWN"),native(identity=setOf("G"))))
            NativeIndex(listOf(n)).match(c).status shouldBe CatalogStatus.NATIVE_METADATA_UNRESOLVED
    }
    test("coverage denominators reconcile and absent definitions are not mechanical nonmatches") {
        val r=catalogCoverage(catalog,snapshot,"Judith, the Scourge Diva",listOf("create-creature-tokens"))
        r.counts.getValue("eligible_oracle_cards") shouldBe 5
        r.counts.getValue("excluded_records") shouldBe 4
        r.counts.getValue("unresolved_pool_records") shouldBe 3
        r.records.all { it.status==CatalogStatus.NATIVE_DEFINITION_MISSING } shouldBe true
        r.records.all { it.objectives.values.single().state=="NOT_EVALUATED_NATIVE_DEFINITION_MISSING" } shouldBe true
        r.counts.getValue("missing_uninvestigated") shouldBe 5
    }
    test("coverage is independent from objective selection and rejects unsupported commander construction") {
        val before=catalogCoverage(catalog,snapshot,"Judith, the Scourge Diva")
        before.records.all { it.objectives.isEmpty() } shouldBe true
        shouldThrow<IllegalArgumentException> { catalogCoverage(catalog,snapshot,"No Native Commander") }
        shouldThrow<IllegalArgumentException> { catalogCoverage(catalog,snapshot,"Judith, the Scourge Diva",listOf("new-objective")) }
    }
    test("translation review is snapshot-bound identity-unique and optional") {
        val c=card("Fixture Colorless")
        val ledger=TranslationLedger(snapshot.manifest.sha256,listOf(TranslationEntry(c.oracleId!!,"INVESTIGATED_UNRESOLVED")))
        val r=catalogCoverage(catalog,snapshot,"Judith, the Scourge Diva",ledger=ledger)
        r.counts.getValue("missing_investigated") shouldBe 1
        r.counts.getValue("missing_uninvestigated") shouldBe 4
        shouldThrow<IllegalArgumentException> { catalogCoverage(catalog,snapshot,"Judith, the Scourge Diva",ledger=ledger.copy(snapshotSha256="wrong")) }
        shouldThrow<IllegalArgumentException> { catalogCoverage(catalog,snapshot,"Judith, the Scourge Diva",ledger=ledger.copy(cards=ledger.cards+ledger.cards)) }
    }
    test("bounded inventory uses saved report without constructing a native catalog") {
        val r=catalogCoverage(catalog,snapshot,"Judith, the Scourge Diva")
        val p=Files.createTempFile("coverage-report",".json")
        try {
            Files.writeString(p,Json.encodeToString(r));val output=mutableListOf<String>()
            runCli(arrayOf("audit","catalog-missing","--report",p.toString(),"--limit","2","--offset","1","--json"),
                { error("catalog must not load") },output::add) shouldBe 0
            val page=Json.decodeFromString<MissingInventory>(output.single())
            page.total shouldBe 5;page.items.size shouldBe 2;page.offset shouldBe 1
            for(limit in listOf("0","201","not-a-number")) runCli(arrayOf("audit","catalog-missing","--report",p.toString(),"--limit",limit),
                {error("catalog must not load")},{},{}) shouldBe 2
            runCli(arrayOf("audit","catalog-missing","--report",p.toString(),"--unknown","x"),{catalog},{},{}) shouldBe 2
        } finally { Files.deleteIfExists(p) }
    }
})
