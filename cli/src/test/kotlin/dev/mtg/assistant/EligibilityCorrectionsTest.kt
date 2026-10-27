package dev.mtg.assistant

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import dev.mtg.assistant.coverage.*
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.eligibility.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow

class EligibilityCorrectionsTest : FunSpec({
    val id="11111111-1111-1111-1111-111111111111"
    fun native()=card("Reviewed Fixture") { manaCost="{G}";colorIdentity="R";typeLine="Legendary Creature — Human";power=1;toughness=1 }
        .copy(oracleId=id,legalFormats=setOf(DeckFormat.COMMANDER))
    fun source(c: CardDefinition)=OracleIdentity(id,c.name,"normal",listOf("G"),c.typeLine.toString(),"Reviewed fixture","{G}",emptyList(),
        mapOf("commander" to "legal"),emptyList(),emptyList(),OracleSource(null,"https://scryfall.com/card/test/1/reviewed-fixture",null,null,null,null))
    fun plan(c: CardDefinition=native(), field: CorrectionField=CorrectionField.FULL_COLOR_IDENTITY): CorrectionPlan {
        val n=nativeIdentity(c,"UPSTREAM_ARGENTUM");val s=source(c)
        val manifest=SnapshotManifest(bulkType="oracle_cards",bulkId="synthetic-review",updatedAt="2026-10-26T00:00:00Z",downloadedAt="2026-10-27T00:00:00Z",
            sourceUri="https://api.scryfall.com/bulk-data/oracle_cards",downloadUri="https://data.scryfall.io/test.json",format="json-array",compression="none",sha256="a".repeat(64),bytes=1)
        val record=EligibilityCorrection("synthetic-reviewed",ReviewDisposition.APPROVED,id,n,eligibilityDefinitionDigest(c),field,
            if(field==CorrectionField.FULL_COLOR_IDENTITY) n.identity?.sorted() else listOf(n.commanderLegality),
            if(field==CorrectionField.FULL_COLOR_IDENTITY) listOf("G") else listOf("MATCH"),s.source.uri!!,"2026-10-27T00:00:00Z",listOf("Synthetic authoritative test fixture"))
        return CorrectionPlan(context=EnrichmentContext(argentumCommit=ARGENTUM_SHA,snapshotSha256=manifest.sha256,nativeIdentitySha256=nativeIdentityDigest(listOf(n))),
            snapshot=manifest,sources=listOf(s),currentSources=listOf(s),corrections=listOf(record))
    }
    fun resolve(c: CardDefinition=native(), p: CorrectionPlan=plan(c))=EligibilityResolver(listOf(c),p).resolve(c)
    fun invalid(p: CorrectionPlan, c: CardDefinition=native()) { shouldThrow<IllegalArgumentException> { resolve(c,p) } }

    test("approved correction changes effective colors and preserves original native mechanics") {
        val c=native();val r=resolve(c)
        r.originalIdentity shouldBe listOf("R");r.identity shouldBe listOf("G");r.corrections.single().state shouldBe "APPLIED"
        selectedCommanderEligibility(c,setOf(Color.RED),r) shouldBe Verdict.NO_MATCH
        selectedCommanderEligibility(c,setOf(Color.GREEN),r) shouldBe Verdict.MATCH
        c.colorIdentityOverride shouldBe setOf(Color.RED)
    }
    test("independently reviewed missing full identity can admit the otherwise excluded card") {
        val c=native().copy(colorIdentityOverride=null);val r=resolve(c)
        selectedCommanderEligibility(c,setOf(Color.GREEN)) shouldBe Verdict.UNKNOWN
        selectedCommanderEligibility(c,setOf(Color.GREEN),r) shouldBe Verdict.MATCH
        r.originalIdentity shouldBe null
    }
    test("independently reviewed missing format legality remains distinct from color compatibility") {
        val c=native().copy(legalFormats=emptySet());val r=resolve(c,plan(c,CorrectionField.COMMANDER_LEGALITY))
        r.originalCommanderLegality shouldBe Verdict.UNKNOWN;r.commanderLegality shouldBe Verdict.MATCH
        selectedCommanderEligibility(c,setOf(Color.GREEN),r) shouldBe Verdict.NO_MATCH
    }
    test("ambiguous physical identity never authorizes an approved correction") {
        val c=native().copy(oracleId=null);val p=plan(c);val other=p.sources.single().copy(oracleId="22222222-2222-2222-2222-222222222222")
        invalid(p.copy(sources=p.sources+other,currentSources=p.currentSources+other),c)
    }
    test("incorrect Oracle identity cannot authorize a matching card name") {
        val p=plan();invalid(p.copy(corrections=p.corrections.map { it.copy(oracleId="22222222-2222-2222-2222-222222222222") }))
    }
    test("duplicate corrections are rejected before any correction is published") {
        val p=plan();invalid(p.copy(corrections=p.corrections+p.corrections))
    }
    test("conflicting corrections for the same native field are rejected") {
        val p=plan();invalid(p.copy(corrections=p.corrections+p.corrections.single().copy(id="conflict",approved=listOf("B"))))
    }
    test("duplicate native targets and competing native Oracle claims are rejected") {
        val c=native();val p=plan()
        shouldThrow<IllegalArgumentException> { EligibilityResolver(listOf(c,c),p) }
        shouldThrow<IllegalArgumentException> { EligibilityResolver(listOf(c,c.copy(name="Other Native Claim")),p) }
    }
    test("stale expected native value cannot silently broaden eligibility") {
        val p=plan();invalid(p.copy(corrections=p.corrections.map { it.copy(expected=emptyList()) }))
    }
    test("upstream already corrected metadata is retained without double application") {
        val c=native().copy(colorIdentityOverride=setOf(Color.GREEN));val r=resolve(c,plan())
        r.corrections.single().state shouldBe "UPSTREAM_ALREADY_CORRECT";r.identity shouldBe listOf("G")
    }
    test("a different upstream metadata value requires review") {
        invalid(plan(),native().copy(colorIdentityOverride=setOf(Color.BLUE)))
    }
    test("changed native costs or effects require renewed review") {
        invalid(plan(),native().copy(oracleText="Changed native definition"))
        invalid(plan(),native().copy(manaCost=ManaCost.parse("{2}{G}")))
    }
    test("changed source snapshot invalidates approval") {
        val p=plan();invalid(p.copy(context=p.context.copy(snapshotSha256="b".repeat(64))))
    }
    test("changed upstream revision invalidates approval") {
        val p=plan();invalid(p.copy(context=p.context.copy(argentumCommit="b".repeat(40))))
    }
    test("source disagreement or unsupported corrected values fail closed") {
        val p=plan();invalid(p.copy(currentSources=p.currentSources.map { it.copy(colorIdentity=listOf("U")) }))
        invalid(p.copy(corrections=p.corrections.map { it.copy(approved=listOf("X")) }))
        invalid(p.copy(corrections=p.corrections.map { it.copy(approved=listOf("R")) }))
    }
    test("a color correction never makes a Commander-illegal card format legal") {
        val c=native().copy(legalFormats=setOf(DeckFormat.MODERN));val r=resolve(c)
        r.commanderLegality shouldBe Verdict.NO_MATCH;selectedCommanderEligibility(c,setOf(Color.GREEN),r) shouldBe Verdict.MATCH
        inspectCommanderDefinition(c.name,c,metadata=r).selectable shouldBe false
    }
    test("corrected commander selection and inspection use the same effective identity") {
        val c=native().copy(colorIdentityOverride=null);val r=resolve(c)
        inspectCommanderDefinition(c.name,c).selectable shouldBe false
        val inspected=inspectCommanderDefinition(c.name,c,metadata=r)
        inspected.selectable shouldBe true;inspected.colorIdentity shouldBe listOf("GREEN")
        inspected.effectiveMetadata!!.originalIdentity shouldBe null
    }
    test("multiple faces and basic land restrictions cannot be erased by a correction") {
        val back=card("Back") { manaCost="{U}";colorIdentity="U";typeLine="Creature — Human";power=1;toughness=1 }
        val c=native().copy(backFace=back)
        invalid(plan(c),c)
        val land=native().copy(typeLine=TypeLine.parse("Land — Island"))
        invalid(plan(land),land)
    }
    test("native color indicators remain an explicit correction consistency guard") {
        val c=native().copy(colorIndicator=setOf(Color.BLUE));invalid(plan(c),c)
    }
    test("unapproved proposal and absent correction preserve conservative native values") {
        val c=native().copy(colorIdentityOverride=null);val p=plan(c)
        val r=resolve(c,p.copy(corrections=p.corrections.map { it.copy(disposition=ReviewDisposition.PROPOSED) }))
        r.identity shouldBe null;r.corrections shouldBe emptyList()
        EligibilityResolver(listOf(c),null).resolve(c) shouldBe nativeEligibility(c)
    }
    test("a change to another unreviewed eligibility field blocks activation") {
        invalid(plan(),native().copy(legalFormats=emptySet()))
        val c=native().copy(legalFormats=emptySet())
        invalid(plan(c,CorrectionField.COMMANDER_LEGALITY),c.copy(colorIdentityOverride=setOf(Color.BLUE)))
    }
    test("a different observed source version requires review for affected corrections") {
        val c=native();val resolver=EligibilityResolver(listOf(c),plan(c))
        resolver.requireSource("a".repeat(64),listOf(c.name))
        resolver.requireSource("b".repeat(64),listOf("Uncorrected Native Card"))
        shouldThrow<IllegalArgumentException> { resolver.requireSource("b".repeat(64),listOf(c.name)) }
    }
    test("resolved metadata cannot be reused for another definition with the same name") {
        val c=native();val resolver=EligibilityResolver(listOf(c),plan(c))
        shouldThrow<IllegalArgumentException> { resolver.resolve(c.copy(colorIdentityOverride=setOf(Color.BLUE))) }
    }
})
