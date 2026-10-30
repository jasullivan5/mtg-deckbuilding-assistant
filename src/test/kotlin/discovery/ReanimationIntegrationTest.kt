package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.audit.*
import com.wingedsheep.sdk.core.Zone
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** These cases come from the frozen source review, not expected card-name production rules. */
class ReanimationIntegrationTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun e(name: String)=PathwayExtractor().extract(listOf(requireNotNull(catalog.card(name))))
    fun rows(name: String)=reanimationFacts(e(name).facts).execute()
    test("development authored targeted returns preserve own graveyard and owner-default control") {
        for(name in listOf("Reanimate","Zombify","Dread Return")) {
            val r=rows(name).single();r.verdict shouldBe Verdict.MATCH;r.fact.sourceZone shouldBe Zone.GRAVEYARD;r.fact.destinationZone shouldBe Zone.BATTLEFIELD
        }
    }
    test("Victimize action links conditional sacrifice generation and actual gated reanimation") {
        val x=e("Victimize");val r=reanimationFacts(x.facts).execute().single()
        r.verdict shouldBe Verdict.MATCH
        val death=x.pathways.single { it.operation=="SACRIFICE" }
        r.fact.actionId shouldBe death.actionId
        r.fact.path shouldBe "script.spellEffect.then.body"
        r.fact.prerequisites.any { it.contains("Gate=") && it.contains("PermanentsSacrificed") } shouldBe true
        x.facts.single { it.id==death.factId }.kind shouldBe FactKind.SACRIFICE
    }
    test("alternative flashback payment stays associated without asserting it is the normal casting cost") {
        val x=e("Dread Return");val sacrifice=x.facts.single { it.costOrEffect=="ALTERNATIVE_CASTING_COST" };val move=reanimationFacts(x.facts).execute().single().fact
        associatedSpellAction(x.actions.associateBy { it.id },sacrifice.actionId) shouldBe move.actionId
        sacrifice.native.toString().contains("\"count\":3") shouldBe true
    }
    test("held-out hand return is recognized movement but excluded from B") {
        val r=rows("Disentomb").single();r.fact.kind shouldBe FactKind.MOVE_OBJECT;r.fact.destinationZone shouldBe Zone.HAND;r.verdict shouldBe Verdict.NO_MATCH
    }
    test("held-out self activation preserves graveyard source and tapped entry") {
        val r=rows("Reassembling Skeleton").single();r.verdict shouldBe Verdict.MATCH
        r.fact.sourcePlayer shouldBe "YOU";r.fact.native.toString().contains("Tapped") shouldBe true
        r.fact.circumstances!!.actionKind shouldBe "ACTIVATED"
    }
    test("held-out different color uses same facts while remaining ineligible under Judith") {
        rows("Breath of Life").single().verdict shouldBe Verdict.MATCH
        selectedCommanderEligibility(catalog.card("Breath of Life")!!,fullIdentity(resolveCommander(catalog,"Judith, the Scourge Diva"))) shouldBe Verdict.NO_MATCH
    }
    test("held-out indirect Living Death sequence is visible without conflating exile with graveyard") {
        val r=rows("Living Death");r.any { it.fact.sourceZone==Zone.GRAVEYARD && it.fact.destinationZone==Zone.EXILE } shouldBe true
        r.single { it.fact.destinationZone==Zone.BATTLEFIELD }.fact.sourceZone shouldBe Zone.EXILE
        r.none { it.verdict==Verdict.MATCH } shouldBe true
    }
    test("native casting permission is never direct reanimation") {
        val x=e("Gravecrawler");x.facts.single { it.kind==FactKind.CAST_PERMISSION }.native.toString().contains("Graveyard") shouldBe true
        reanimationFacts(x.facts).execute().isEmpty() shouldBe true
    }
    test("attachment and mana-value boundaries remain unresolved") {
        rows("Animate Dead").single().verdict shouldBe Verdict.UNKNOWN
        rows("Unearth").single().verdict shouldBe Verdict.UNKNOWN
    }
    test("normal combined catalog retains Judith as death consumer") {
        val x=e("Judith, the Scourge Diva");creatureDeathPathways(x.pathways).where(canAffectCreature()).where(canAffectController(Controller.YOU)).where(canAffectTokenStatus(TokenStatus.NONTOKEN)).where(satisfiesDeathGenerationPolicy()).execute().none { it.verdict==Verdict.MATCH } shouldBe true
        rows("Judith, the Scourge Diva").isEmpty() shouldBe true
        catalog.provenance("Judith, the Scourge Diva").origin shouldBe "APPLICATION_EXTENSION"
    }
    test("requested unavailable examples are real corpus gaps") {
        listOf("Persist","Necromancy","Exhume").forEach { catalog.card(it) shouldBe null }
    }
})
