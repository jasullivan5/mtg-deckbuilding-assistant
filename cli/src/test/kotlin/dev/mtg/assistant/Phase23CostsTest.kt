package dev.mtg.assistant

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.costs.*
import com.wingedsheep.sdk.scripting.effects.*
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class Phase23CostsTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun extracted(name: String)=PathwayExtractor().extract(listOf(catalog.card(name)!!))
    fun result(p: Pathway)=creatureDeathPathways(listOf(p)).where(canAffectCreature()).where(canAffectController(Controller.YOU)).where(canAffectTokenStatus(TokenStatus.NONTOKEN)).where(satisfiesDeathGenerationPolicy()).execute().single()
    test("Dread Return flashback cost retains route count and associated spell") {
        val e=extracted("Dread Return");val p=e.pathways.single { it.origin=="ALTERNATIVE_CASTING_COST" }
        result(p).verdict shouldBe Verdict.MATCH
        p.native.toString().contains("\"count\":3") shouldBe true
        p.prerequisites.any { it.contains("GRAVEYARD") && it.contains("Flashback") } shouldBe true
        e.actions.single { it.id==p.actionId }.associatedActionId shouldBe "Dread Return|front|script"
    }
    test("held-out optional cost transfers to Final Flourish without conflating the removal effect") {
        for(name in listOf("Vicious Offering","Final Flourish")) {
            val e=extracted(name); val p=e.pathways.single { it.origin=="OPTIONAL_CASTING_COST" }
            result(p).verdict shouldBe Verdict.MATCH
            p.path.startsWith("keywordAbilities[0]") shouldBe true
            e.pathways.filter { it.operation=="TOUGHNESS_REDUCTION" && it.branch==Contribution.DIRECT_DEATH_GENERATOR }.forEach { result(it).decisions.last().verdict shouldBe Verdict.NO_MATCH }
        }
    }
    test("self alternative cost composite preserves zero token and positive sacrifice boundaries") {
        fun fixture(filter: GameObjectFilter,count: Int)=card("Synthetic alternative") {
            typeLine="Sorcery"; spell { effect=Effects.DrawCards(1) }
            selfAlternativeCost=SelfAlternativeCost(ManaCost.parse("{1}"),listOf(AdditionalCost.Composite(listOf(AdditionalCost.Atom(CostAtom.PayLife(1)),AdditionalCost.Atom(CostAtom.Sacrifice(filter,count))))))
        }
        val good=PathwayExtractor().extract(listOf(fixture(GameObjectFilter.Creature,2))).pathways.single()
        good.origin shouldBe "ALTERNATIVE_CASTING_COST";result(good).verdict shouldBe Verdict.MATCH
        result(PathwayExtractor().extract(listOf(fixture(GameObjectFilter.Creature.token(),1))).pathways.single()).verdict shouldBe Verdict.NO_MATCH
        result(PathwayExtractor().extract(listOf(fixture(GameObjectFilter.Creature,0))).pathways.single()).verdict shouldBe Verdict.NO_MATCH
    }
    test("mode-local sacrifice belongs only to Lash's sacrifice branch") {
        val e=extracted("Lash of the Balrog")
        val p=e.pathways.single { it.origin=="CASTING_COST" }
        p.path shouldBe "script.spellEffect.modes[0].additionalCosts[0].atom"
        result(p).verdict shouldBe Verdict.MATCH
        e.pathways.filter { it.operation=="DESTROY" }.forEach { result(it).verdict shouldBe Verdict.NO_MATCH }
        e.pathways.none { it.origin.endsWith("COST") && it.path.contains("modes[1]") } shouldBe true
    }
    test("choice and mana forks transfer to held-out Souls and Morkrut without requiring sacrifice") {
        for(name in listOf("Dusk Mangler","Eaten Alive","Souls of the Lost","Morkrut Behemoth")) {
            val e=extracted(name);val p=e.pathways.single { it.origin=="CASTING_COST" }
            result(p).verdict shouldBe Verdict.MATCH
            p.prerequisites.any { it.contains("branch") || it.contains("choice") } shouldBe true
        }
    }
    test("mode overrides exclude base sacrifice and composite costs preserve role") {
        val c=card("Synthetic mode overrides") { typeLine="Sorcery"
            additionalCost(AdditionalCost.Atom(CostAtom.Sacrifice(GameObjectFilter.Creature)))
            spell { effect=ModalEffect.chooseOne(Mode(Effects.DrawCards(1),additionalCosts=emptyList()),Mode(Effects.DrawCards(2),additionalCosts=emptyList())) }
        }
        PathwayExtractor().extract(listOf(c)).pathways.isEmpty() shouldBe true
    }
    test("unresolved nested cost remains visible rather than pretending full support") {
        val c=card("Synthetic forage choice") { typeLine="Sorcery"
            additionalCost(AdditionalCost.Choice(listOf(AdditionalCost.Forage,AdditionalCost.Atom(CostAtom.Sacrifice(GameObjectFilter.Creature.token())))))
            spell { effect=Effects.DrawCards(1) }
        }
        val e=PathwayExtractor().extract(listOf(c))
        e.unsupported.any { it.path.endsWith("options[0]") } shouldBe true
        result(e.pathways.single()).verdict shouldBe Verdict.NO_MATCH
    }
})
