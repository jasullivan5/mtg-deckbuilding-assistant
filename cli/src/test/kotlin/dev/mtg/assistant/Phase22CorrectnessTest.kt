package dev.mtg.assistant

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.costs.*
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.*
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.audit.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Path
import java.nio.file.Files

class Phase22CorrectnessTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun paths(c: CardDefinition)=creatureDeathPathways(PathwayExtractor().extract(listOf(c)).pathways)
        .where(canAffectCreature()).where(canAffectController(Controller.YOU)).where(canAffectTokenStatus(TokenStatus.NONTOKEN)).where(satisfiesDeathGenerationPolicy()).execute()
    for(name in listOf("Ball Lightning","Spark Elemental","Accursed Centaur","Demonic Taskmaster","Lord of the Pit")) test("$name mandatory sacrifice is a capability without a payoff requirement") {
        val p=paths(catalog.card(name)!!).first { it.pathway.operation=="SACRIFICE" }
        all(p.decisions.filter { it.basis=="MECHANICAL" }.map { it.verdict }) shouldBe Verdict.MATCH
        p.verdict shouldBe Verdict.MATCH
        p.pathway.origin shouldBe "EFFECT"
        p.pathway.circumstances!!.mandatory shouldBe Verdict.MATCH
        if(name in listOf("Ball Lightning","Spark Elemental")) {
            p.pathway.circumstances!!.deferredTiming shouldBe true
            p.pathway.circumstances!!.delayedTrigger shouldBe false
            p.pathway.circumstances!!.sourceMustRemainOnBattlefield shouldBe true
        }
        if(name=="Demonic Taskmaster") {
            p.pathway.circumstances!!.objectRestrictions.contains("EXCLUDES_SOURCE") shouldBe true
            p.pathway.circumstances!!.sourceMustRemainOnBattlefield shouldBe false
        }
    }
    test("Murder and Fleshbag retain mechanical compatibility despite policy rejection") {
        for(name in listOf("Murder","Fleshbag Marauder")) {
            val p=paths(catalog.card(name)!!).single()
            all(p.decisions.take(3).map { it.verdict }) shouldBe Verdict.MATCH
            p.decisions.last().verdict shouldBe Verdict.NO_MATCH
        }
    }
    test("mandatory exclusion can be configured without changing mechanical truth") {
        val p=paths(catalog.card("Ball Lightning")!!).single().pathway
        satisfiesDeathGenerationPolicy(DeathGenerationPolicy(includeMandatorySacrifice=false)).assess(p,Requirements()).verdict shouldBe Verdict.NO_MATCH
        p.scope.compatible(Requirements(controller=Controller.YOU,token=TokenStatus.NONTOKEN)) shouldBe Verdict.MATCH
    }
    test("PayOrSuffer exposes Pit Raptor failure as an effect with conditional prerequisites") {
        val e=PathwayExtractor().extract(listOf(catalog.card("Pit Raptor")!!))
        val p=paths(catalog.card("Pit Raptor")!!).single()
        p.verdict shouldBe Verdict.MATCH
        p.pathway.path.endsWith("effect.suffer") shouldBe true
        p.pathway.origin shouldBe "EFFECT"
        p.pathway.prerequisites.any { it.contains("payment") && it.contains("not made") } shouldBe true
        e.unsupported.none { it.nativeType.endsWith("PayOrSufferEffect") } shouldBe true
    }
    // Synthetic native SDK definitions below isolate sign, payer and branch behavior.
    test("PayOrSuffer payment is a resolution cost and opponent payer cannot sacrifice our creature") {
        fun fixture(player: EffectTarget)=card("Synthetic payer") { typeLine="Enchantment"; activatedAbility {
            cost=Costs.Free; effect=PayOrSufferEffect(PayCost.Atom(CostAtom.Sacrifice(GameObjectFilter.Creature)),Effects.DrawCards(1),player)
        } }
        val own=paths(fixture(EffectTarget.Controller)).single()
        own.pathway.origin shouldBe "RESOLUTION_COST"; own.verdict shouldBe Verdict.MATCH
        paths(fixture(EffectTarget.PlayerRef(Player.EachOpponent))).single().decisions[1].verdict shouldBe Verdict.NO_MATCH
    }
    test("cardinality bounds remove Muxus buff and preserve unknown entity-power signs") {
        PathwayExtractor().extract(listOf(catalog.card("Muxus, Goblin Grandee")!!)).pathways.none { it.path.startsWith("script.triggeredAbilities[1]") } shouldBe true
        amountBounds(DynamicAmount.Count(Player.You,Zone.BATTLEFIELD,GameObjectFilter.Creature)).negativePotential() shouldBe Verdict.NO_MATCH
        amountBounds(DynamicAmount.Fixed(-2)).negativePotential() shouldBe Verdict.MATCH
        amountBounds(DynamicAmount.Fixed(0)).negativePotential() shouldBe Verdict.NO_MATCH
        amountBounds(DynamicAmount.Multiply(DynamicAmount.Count(Player.You,Zone.BATTLEFIELD),-1)).negativePotential() shouldBe Verdict.UNKNOWN
        amountBounds(DynamicAmount.EntityProperty(EffectTarget.Self,EntityNumericProperty.Power)).negativePotential() shouldBe Verdict.UNKNOWN
    }
    test("unknown sign cannot silently become a mechanically certain reduction") {
        val unknown=DynamicAmount.EntityProperty(EffectTarget.Self,EntityNumericProperty.Power)
        val c=card("Synthetic unknown sign") { typeLine="Creature"; power=2;toughness=2; activatedAbility { cost=Costs.Free;effect=ModifyStatsEffect(DynamicAmount.Fixed(0),unknown,EffectTarget.Self) } }
        val p=paths(c).single()
        p.pathway.operationCompatibility shouldBe Verdict.UNKNOWN
        p.verdict shouldBe Verdict.UNKNOWN
    }
    test("intrinsic self-toughness loss does not need a power bonus or beneficial outcome") {
        val c=card("Synthetic self shrink without bonus") { typeLine="Creature";power=2;toughness=2
            activatedAbility { cost=Costs.Free;effect=Effects.ModifyStats(0,-1,EffectTarget.Self) }
        }
        paths(c).single().verdict shouldBe Verdict.MATCH
        paths(c).single().pathway.circumstances!!.sourceMustRemainOnBattlefield shouldBe true
    }
    test("negative and positive conditional signs remain unresolved until the branch is known") {
        val a=DynamicAmount.Conditional(com.wingedsheep.sdk.scripting.conditions.CreatureDiedThisTurnCondition,DynamicAmount.Fixed(-3),DynamicAmount.Fixed(2))
        amountBounds(a).negativePotential() shouldBe Verdict.UNKNOWN
        amountBounds(DynamicAmount.Add(DynamicAmount.Fixed(3),DynamicAmount.Fixed(-1))).negativePotential() shouldBe Verdict.NO_MATCH
    }
    test("frozen reviewed benchmark matches current Judith catalog with explicit denominators") {
        val file=listOf(Path.of("../benchmark/review-v1.json"),Path.of("benchmark/review-v1.json")).first(Files::exists)
        val report=evaluateBenchmark(discover(catalog,"Judith, the Scourge Diva"),file)
        report.denominators["reviewed_cases"] shouldBe 29
        report.numerators["expected_accepted_recovered"] shouldBe 15
        report.numerators["expected_accepted_missed"] shouldBe 0
        report.numerators["confirmed_mismatch_cases"] shouldBe 0
        report.numerators["incorrect_accepted_reviewed_cases"] shouldBe 0
        report.numerators["unresolved_cases"] shouldBe 1
    }
})
