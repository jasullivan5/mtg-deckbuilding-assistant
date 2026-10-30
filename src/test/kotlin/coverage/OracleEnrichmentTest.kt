package dev.mtg.assistant

import dev.mtg.assistant.coverage.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.*

class OracleEnrichmentTest : FunSpec({
    val manifest=SnapshotManifest(bulkType="oracle_cards",bulkId="synthetic",updatedAt="2026-10-26T00:00:00Z",downloadedAt="2026-10-26T00:00:00Z",
        sourceUri="https://api.scryfall.com/bulk-data/oracle_cards",downloadUri="https://data.scryfall.io/synthetic.json",format="json-array",compression="none",sha256="0".repeat(64),bytes=1)
    fun external(id: String="11111111-1111-1111-1111-111111111111",name: String="Synthetic Artifact")=OracleIdentity(id,name,"normal",emptyList(),"Artifact","","{1}",emptyList(),mapOf("commander" to "legal"),emptyList(),emptyList(),OracleSource(null,null,null,null,null,null))
    fun snap(vararg cards: OracleIdentity)=OracleSnapshot(manifest,cards.toList(),cards.size,0,0)
    fun native(e: OracleIdentity=external(),id: String?=null)=NativeIdentity(e.name!!,id,e.aliases,e.colorIdentity!!.toSet(),"MATCH","UPSTREAM_ARGENTUM",
        e.layout,if(e.faces.isEmpty()) listOf(NativeFaceEvidence(e.name!!,e.manaCost,e.typeLine)) else e.faces.map { NativeFaceEvidence(it.name!!,it.manaCost,it.typeLine) },e.name)
    fun matched(n: NativeIdentity,e: OracleIdentity=external())=NativeIndex(listOf(n),snap(e)).match(e)
    test("unique physical name plus independent cast cost and type evidence enriches a missing ID without mutation") {
        val n=native();val r=matched(n)
        r.association!!.confidence shouldBe IdentityConfidence.CONFIDENT
        r.association!!.oracleId shouldBe external().oracleId
        n.oracleId shouldBe null
        r.metadata!!.nativeOracleId shouldBe null
    }
    test("name alone missing costs and types is a review candidate rather than confidence") {
        matched(native().copy(faces=emptyList())).association!!.confidence shouldBe IdentityConfidence.REVIEW_REQUIRED
        matched(native().copy(faces=listOf(NativeFaceEvidence("Synthetic Artifact","{2}","Artifact")))).association!!.confidence shouldBe IdentityConfidence.REVIEW_REQUIRED
    }
    test("exact name shared by distinct physical IDs stays ambiguous even when one cost matches") {
        val a=external();val b=a.copy(oracleId="22222222-2222-2222-2222-222222222222",manaCost="{2}")
        val r=NativeIndex(listOf(native(a)),snap(a,b)).match(a)
        r.association!!.confidence shouldBe IdentityConfidence.AMBIGUOUS
    }
    test("an Adventure face cannot stand in for its whole physical card") {
        val e=external().copy(name="Synthetic Giant // Synthetic Trick",layout="adventure",faces=listOf(OracleFace("Synthetic Giant","Creature","","{4}{G}",null,null),OracleFace("Synthetic Trick","Sorcery","","{G}",null,null)))
        val n=native().copy(name="Synthetic Trick",physicalName="Synthetic Trick",aliases=setOf("synthetic trick"),faces=listOf(NativeFaceEvidence("Synthetic Trick","{G}","Sorcery")))
        matched(n,e).association!!.confidence shouldBe IdentityConfidence.REVIEW_REQUIRED
    }
    test("whole Adventure and reused ordinary spell have different confident physical identities") {
        val spell=external(name="Synthetic Trick").copy(typeLine="Sorcery",manaCost="{G}")
        val adventure=spell.copy(oracleId="22222222-2222-2222-2222-222222222222",name="Synthetic Giant // Synthetic Trick",layout="adventure",faces=listOf(OracleFace("Synthetic Giant","Creature","","{4}{G}",null,null),OracleFace("Synthetic Trick","Sorcery","","{G}",null,null)))
        val i=NativeIndex(listOf(native(spell),native(adventure)),snap(spell,adventure))
        i.match(spell).association!!.oracleId shouldBe spell.oracleId
        i.match(adventure).association!!.oracleId shouldBe adventure.oracleId
        NativeIndex(listOf(native(adventure)),snap(spell,adventure)).match(spell).status shouldBe CatalogStatus.NATIVE_DEFINITION_MISSING
    }
    test("duplicate native claims cannot silently prefer an ID-bearing definition") {
        val e=external();val a=native(e,e.oracleId);val b=native(e).copy(name="Renamed Native",physicalName=e.name)
        val r=NativeIndex(listOf(a,b),snap(e)).match(e)
        r.status shouldBe CatalogStatus.AMBIGUOUS_MAPPING
        r.association!!.confidence shouldBe IdentityConfidence.CONFLICT
    }
    test("existing valid ID survives historical naming changes without replacement") {
        val e=external();val n=native(e,e.oracleId).copy(name="Historical Name",physicalName="Historical Name",aliases=setOf("historical name"))
        matched(n,e).association!!.method shouldBe "ORACLE_ID"
        matched(n,e).association!!.oracleId shouldBe e.oracleId
    }
    test("existing ID pointing elsewhere conflicts with an independently consistent physical match") {
        val a=external();val b=a.copy(oracleId="22222222-2222-2222-2222-222222222222",name="Different Card")
        val n=native(a,b.oracleId);val r=NativeIndex(listOf(n),snap(a,b)).match(a)
        r.association!!.confidence shouldBe IdentityConfidence.CONFLICT
        n.oracleId shouldBe b.oracleId
        NativeIndex(listOf(n),snap(a,b)).match(b).association!!.confidence shouldBe IdentityConfidence.CONFLICT
    }
    test("malformed and snapshot-absent native IDs remain unresolved instead of being overwritten") {
        matched(native().copy(declaredOracleId="not-a-uuid")).association!!.confidence shouldBe IdentityConfidence.CONFLICT
        matched(native(id="33333333-3333-3333-3333-333333333333")).association!!.confidence shouldBe IdentityConfidence.CONFLICT
    }
    test("identity confidence is independent of incorrect or missing native eligibility metadata") {
        val e=external().copy(colorIdentity=listOf("G"))
        val n=native(e).copy(identity=emptySet(),commanderLegality="UNKNOWN")
        val r=matched(n,e)
        r.association!!.confidence shouldBe IdentityConfidence.CONFIDENT
        r.status shouldBe CatalogStatus.NATIVE_METADATA_UNRESOLVED
        r.metadata!!.issues.toSet() shouldBe setOf("NATIVE_EXTERNAL_IDENTITY_DISAGREEMENT","NATIVE_LEGALITY_MISSING")
        r.metadata!!.candidateCorrections.all { it.disposition=="REVIEW_ONLY_NOT_APPLIED" } shouldBe true
        matched(n.copy(identity=null),e).metadata!!.issues.contains("NATIVE_FULL_IDENTITY_MISSING") shouldBe true
        matched(n.copy(identity=setOf("G"),commanderLegality="NO_MATCH"),e).metadata!!.issues shouldBe listOf("NATIVE_EXTERNAL_LEGALITY_DISAGREEMENT")
    }
    test("missing external eligibility metadata does not erase identity evidence or propose unknown corrections") {
        val e=external().copy(colorIdentity=null,legalities=emptyMap(),issues=listOf("MISSING_OR_INVALID_FULL_COLOR_IDENTITY","MISSING_OR_UNKNOWN_COMMANDER_LEGALITY"))
        val n=native().copy(oracleId=e.oracleId)
        val r=matched(n,e)
        r.association!!.confidence shouldBe IdentityConfidence.CONFIDENT
        r.metadata!!.issues.toSet() shouldBe setOf("EXTERNAL_FULL_IDENTITY_UNRESOLVED","EXTERNAL_COMMANDER_LEGALITY_UNRESOLVED")
        r.metadata!!.candidateCorrections shouldBe emptyList()
    }
    test("native printing ID is independently useful but conflicting physical identity is rejected") {
        val a=external().copy(source=OracleSource("printing-a",null,null,null,null,null));val b=a.copy(oracleId="22222222-2222-2222-2222-222222222222",name="Other Card",source=OracleSource("printing-b",null,null,null,null,null))
        matched(native(a).copy(name="Old Name",physicalName="Old Name",scryfallId="printing-a"),a).association!!.method shouldBe "SCRYFALL_PRINTING_ID"
        NativeIndex(listOf(native(a).copy(scryfallId="printing-b")),snap(a,b)).match(a).association!!.confidence shouldBe IdentityConfidence.CONFLICT
    }
    test("tokens with a printed-card name cannot introduce a confident physical association") {
        val a=external();val token=a.copy(oracleId="22222222-2222-2222-2222-222222222222",layout="token")
        NativeIndex(listOf(native(a)),snap(a,token)).match(a).association!!.oracleId shouldBe a.oracleId
    }
    test("layout differences and incomplete face order require review") {
        val e=external().copy(layout="split",name="Left // Right",faces=listOf(OracleFace("Left","Instant","","{G}",null,null),OracleFace("Right","Instant","","{W}",null,null)))
        val n=native(e)
        matched(n.copy(layout="adventure"),e).association!!.confidence shouldBe IdentityConfidence.REVIEW_REQUIRED
        matched(n.copy(faces=n.faces.reversed()),e).association!!.confidence shouldBe IdentityConfidence.REVIEW_REQUIRED
    }
    test("source changes revision changes and native identity changes invalidate old artifacts") {
        val e=external();val n=native(e);val s=snap(e);val context=OracleEnrichment(listOf(n),s).context
        requireEnrichmentContext(context,listOf(n),s)
        shouldThrow<IllegalArgumentException> { requireEnrichmentContext(context,listOf(n.copy(commanderLegality="UNKNOWN")),s) }
        shouldThrow<IllegalArgumentException> { requireEnrichmentContext(context,listOf(n),s.copy(manifest=manifest.copy(sha256="1".repeat(64)))) }
        shouldThrow<IllegalArgumentException> { requireEnrichmentContext(context,listOf(n),s,revision="different") }
        shouldThrow<IllegalArgumentException> { OracleEnrichment(listOf(n),s).match(e.copy(manaCost="{9}")) }
    }
    test("assignment context is invariant to native enumeration order") {
        val a=native();val b=a.copy(name="Other",physicalName="Other")
        nativeIdentityDigest(listOf(a,b)) shouldBe nativeIdentityDigest(listOf(b,a))
    }
})
