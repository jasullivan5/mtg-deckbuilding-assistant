package dev.mtg.assistant

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import dev.mtg.assistant.audit.*
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.extensions.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.assertions.throwables.shouldThrow
import kotlinx.serialization.json.*

class DiscoveryDiagnosticsTest : FunSpec({
    val catalog by lazy { Catalog() }
    test("Judith has verified original printing metadata and authoritative eligibility") {
        val j=catalog.card("Judith, the Scourge Diva")!!
        j.manaCost.toString() shouldBe "{1}{B}{R}"
        j.typeLine.toString() shouldBe "Legendary Creature — Human Shaman"
        j.creatureStats!!.power.description shouldBe "2"
        j.creatureStats!!.toughness.description shouldBe "2"
        j.metadata.collectorNumber shouldBe "185"; j.setCode shouldBe "RNA"
        j.metadata.scryfallId shouldBe "0a742125-730d-4082-bfd8-5feb7733def4"
        j.metadata.rulings.size shouldBe 3
        fullIdentity(resolveCommander(catalog,j.name)) shouldBe setOf(Color.BLACK,Color.RED)
        selectedCommanderEligibility(j,setOf(Color.RED)) shouldBe Verdict.NO_MATCH
        catalog.provenance(j.name).origin shouldBe "APPLICATION_EXTENSION"
        catalog.search("name:\"Judith, the Scourge Diva\"").any { it.name == j.name } shouldBe true
    }
    test("extension collisions fail for name and Oracle ID without overwriting") {
        val ext=Extensions.all.single { it.definition.name==JudithTheScourgeDiva.name }
        shouldThrow<IllegalArgumentException> { Extensions.validate(listOf(ext.definition),listOf(ext)) }
        shouldThrow<IllegalArgumentException> { Extensions.validate(listOf(ext.definition.copy(name="Different Name")),listOf(ext)) }
        catalog.registry.requireCard(ext.definition.name).script shouldBe ext.definition.script
    }
    test("extension requires verified identity and legality") {
        val e=Extensions.all.single { it.definition.name==JudithTheScourgeDiva.name }
        shouldThrow<IllegalArgumentException> { Extensions.validate(emptyList(),listOf(e.copy(definition=e.definition.copy(colorIdentityOverride=null)))) }
        shouldThrow<IllegalArgumentException> { Extensions.validate(emptyList(),listOf(e.copy(definition=e.definition.copy(legalFormats=emptySet())))) }
    }
    test("Judith consumes death events but cannot be accepted as own death generator") {
        val e=PathwayExtractor().extract(listOf(JudithTheScourgeDiva))
        val direct=creatureDeathPathways(e.pathways).where(canAffectCreature()).where(canAffectController(Controller.YOU)).where(canAffectTokenStatus(TokenStatus.NONTOKEN)).where(satisfiesDeathGenerationPolicy()).execute()
        direct.any { it.verdict == Verdict.MATCH } shouldBe false
        e.pathways.any { it.branch == Contribution.DEATH_ENHANCED_REMOVAL && it.dependencyVerdict == Verdict.MATCH } shouldBe true
    }
    fun report(cards: List<CardDefinition>): DiscoveryReport {
        val e=PathwayExtractor().extract(cards)
        val evaluated=creatureDeathPathways(e.pathways).where(canAffectCreature()).where(canAffectController(Controller.YOU)).where(canAffectTokenStatus(TokenStatus.NONTOKEN)).where(satisfiesDeathGenerationPolicy()).execute()
        return DiscoveryReport(objective="test",commander="synthetic",allowedIdentity=emptyList(),conventionalQuery=null,
            counts=mapOf("mechanical_input_cards_after_conventional_search" to cards.size),eligibility=emptyList(),actions=e.actions,unsupported=e.unsupported,limitations=emptyList(),results=evaluated.map {
                val p=it.pathway
                ResultEvidence(p.card.name,p.card.name,p.face,p.actionId,p.path,p.branch,it.verdict,p.operation,p.origin,p.scope.description,p.prerequisites,p.dependency,p.native,it.decisions,emptyList(),Verdict.MATCH,Verdict.MATCH,
                    contributionClassification=if(it.verdict == Verdict.MATCH) p.branch else null)
            })
    }
    // All following cards are synthetic native SDK fixtures for diagnostic logic only.
    fun spell(name: String,effect: Effect)=card(name) { typeLine="Sorcery"; manaCost="{B}"; spell { this.effect=effect } }
    test("fingerprints normalize variable names and UI text but retain source and destination zones") {
        val a=Json.parseToJsonElement("""{"type":"MoveCollection","from":"foo","prompt":"a","destination":"Graveyard"}""")
        val b=Json.parseToJsonElement("""{"type":"MoveCollection","from":"bar","prompt":"b","destination":"Graveyard"}""")
        canonicalShape(a) shouldBe canonicalShape(b)
        (canonicalShape(a) == canonicalShape(Json.parseToJsonElement("""{"type":"MoveCollection","from":"foo","destination":"Exile"}"""))) shouldBe false
    }
    test("independent native traversal sees nested effects even when extraction declines their container") {
        val c=spell("Synthetic nested",ConditionalOnCollectionEffect("unknown",Effects.Destroy(EffectTarget.Self)))
        val r=report(listOf(c)); val nodes=nativeNodes(listOf(c),r)
        nodes.any { it.path.endsWith("ifNotEmpty") && it.nativeType == "MoveToZone" } shouldBe true
        val cov=coverage(listOf(c),r)
        cov.encounteredTypedNodes shouldBe nodes.size
        cov.groups.isNotEmpty() shouldBe true
        examine(listOf(c),r).findings.any { it.check == "A_POTENTIAL_MISS" } shouldBe true
    }
    test("accepted local costs with unresolved outcomes are flagged as incomplete") {
        val c=card("Synthetic incomplete") { typeLine="Enchantment"; activatedAbility { cost=Costs.Sacrifice(GameObjectFilter.Creature); effect=Effects.GainLife(1) } }
        val r=report(listOf(c))
        r.results.single().verdict shouldBe Verdict.MATCH
        examine(listOf(c),r).findings.any { it.check == "B_INCOMPLETE_ACCEPTED_ACTION" } shouldBe true
    }
    test("cross-examiner keeps eligibility gaps separate") {
        val r=report(emptyList()).copy(eligibility=listOf(EligibilityEvidence("missing identity",Verdict.MATCH,Verdict.UNKNOWN,emptyList(),"missing")))
        examine(emptyList(),r).findings.single().check shouldBe "E_ELIGIBILITY_DATA_GAP"
    }
    test("diff is stable for identical snapshots and exposes classification and policy changes") {
        val c=card("Synthetic diff") { typeLine="Enchantment"; activatedAbility { cost=Costs.Sacrifice(GameObjectFilter.Creature); effect=Effects.DrawCards(1) } }
        val r=report(listOf(c)); diff(r,r).changes.size shouldBe 0
        val p=r.results.single();val updated=r.copy(results=listOf(p.copy(verdict=Verdict.NO_MATCH,contributionClassification=null,decisions=p.decisions.dropLast(1)+Decision("death-generation-policy",Verdict.NO_MATCH,"POLICY","test change"))))
        val d=diff(r,updated)
        d.transitions["VERDICT_MATCH_TO_NO_MATCH"] shouldBe 1
        d.transitions["POLICY_DECISION_CHANGED"] shouldBe 1
        d.changes.any { it.kind == "ACCEPTED_CARD_CLASSIFICATIONS_REMOVED" } shouldBe true
    }
    test("equivalent actions with contradictory decisions are flagged but distinct controllers are not conflated") {
        val c=card("Synthetic equivalent") { typeLine="Enchantment"; repeat(2) { activatedAbility { cost=Costs.Sacrifice(GameObjectFilter.Creature); effect=Effects.DrawCards(1) } } }
        val r=report(listOf(c));val p=r.results[1]
        val inconsistent=r.copy(results=listOf(r.results[0],p.copy(decisions=p.decisions.dropLast(1)+Decision("death-generation-policy",Verdict.NO_MATCH,"POLICY","injected contradiction"))))
        examine(listOf(c),inconsistent).findings.any { it.check == "C_CONTRADICTION" } shouldBe true
        examine(listOf(c),r).findings.any { it.check == "C_CONTRADICTION" } shouldBe false
    }
    test("duplicate pathway bases retain every independent outcome and do not invent transitions") {
        val c=card("Synthetic duplicate base") { typeLine="Enchantment"; activatedAbility { cost=Costs.Sacrifice(GameObjectFilter.Creature); effect=Effects.DrawCards(1) } }
        val r=report(listOf(c));val p=r.results.single()
        val second=p.copy(nativeOperation=Json.parseToJsonElement("""{"type":"synthetic distinct native outcome"}"""),verdict=Verdict.NO_MATCH,contributionClassification=null,decisions=p.decisions.dropLast(1)+Decision("death-generation-policy",Verdict.NO_MATCH,"POLICY","distinct outcome"))
        val first=r.copy(results=listOf(p,second));val reordered=r.copy(results=listOf(second,p))
        val same=diff(first,reordered)
        same.changes.size shouldBe 0
        same.transitions.size shouldBe 0
        same.ambiguousPathwayBases.size shouldBe 1
        val changed=diff(first,first.copy(results=listOf(p,second.copy(dependency="updated evidence"))))
        changed.changes.count { it.kind == "PATHWAY_CHANGED" } shouldBe 1
    }
    test("coverage ranks are deterministic and retain meaningful movement differences") {
        val cards=listOf(spell("Synthetic GY",MoveToZoneEffect(EffectTarget.Self,Zone.GRAVEYARD)),spell("Synthetic exile",MoveToZoneEffect(EffectTarget.Self,Zone.EXILE)))
        val r=report(cards); val a=coverage(cards,r)
        a shouldBe coverage(cards,r)
        a.groups.filter { it.nativeType == "MoveToZone" }.size shouldBe 2
    }
})
