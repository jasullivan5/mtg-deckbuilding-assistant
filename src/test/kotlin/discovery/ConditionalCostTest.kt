package dev.mtg.assistant

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.conditions.CreatureDiedThisTurnCondition
import com.wingedsheep.sdk.scripting.conditions.PlayerDrewCardsThisTurn
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ConditionalCostTest : FunSpec({
    test("new aggregate and target-distribution constraints remain unresolved for discount potential") {
        val base = com.wingedsheep.sdk.scripting.targets.TargetObject(
            filter = com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.Creature)
        val condition = ChosenTargetCondition(GameObjectFilter.Creature)
        fun potential(target: com.wingedsheep.sdk.scripting.targets.TargetObject) =
            TargetSelection("synthetic", listOf(target)).potential(condition)
        potential(base) shouldBe Verdict.MATCH
        for (target in listOf(
            base.copy(totalAtMost = com.wingedsheep.sdk.scripting.targets.TotalCap.manaValue(DynamicAmount.Fixed(2))),
            base.copy(totalAtMost = com.wingedsheep.sdk.scripting.targets.TotalCap.power(DynamicAmount.Fixed(2))),
            base.copy(differentNames = true), base.copy(differentControllers = true),
            base.copy(onePerCardType = true), base.copy(onePerColor = true),
            base.copy(chooser = com.wingedsheep.sdk.scripting.targets.TargetChooser.Opponent)
        )) potential(target) shouldBe Verdict.UNKNOWN
    }

    val catalog by lazy { Catalog() }
    fun extract(name: String) = PathwayExtractor(inspectStateDependencies = true)
        .extract(listOf(checkNotNull(catalog.card(name))))

    test("activation discount is bound to the native activated action and removes the colored pip") {
        val extracted = extract("Kami of Jealous Thirst")
        val action = extracted.actions.single { it.conditionalCosts.isNotEmpty() }
        action.path shouldBe "script.activatedAbilities[0]"
        val cost = action.conditionalCosts.single()
        cost.checkpoint shouldBe PaymentCheckpoint.ACTIVATION
        cost.condition shouldBe PlayerDrewCardsThisTurn(atLeast = 3)
        cost.baseMana shouldBe ManaCost.parse("{4}{B}")
        cost.reduction shouldBe ManaCost.parse("{4}{B}")
        cost.isolatedManaWhenTrue?.symbols shouldBe emptyList()
        cost.verdict shouldBe Verdict.MATCH
    }

    test("self-casting reduction links the declaration to the spell and retains black mana") {
        val extracted = extract("Dreaded Bat-Cloud")
        val action = extracted.actions.single { it.conditionalCosts.isNotEmpty() }
        action.path shouldBe "script.spell"
        val cost = action.conditionalCosts.single()
        cost.checkpoint shouldBe PaymentCheckpoint.CASTING
        cost.condition shouldBe CreatureDiedThisTurnCondition
        cost.isolatedManaWhenTrue shouldBe ManaCost.parse("{1}{B}")
        extracted.stateDependencies.isEmpty() shouldBe true
    }

    test("Wizard existence qualifies at casting and unrelated mechanics remain separate") {
        val wizard = extract("Academy Journeymage").actions.flatMap { it.conditionalCosts }.single()
        val filter = GameObjectFilter.Any.withSubtype("Wizard")
        wizard.condition shouldBe Compare(
            DynamicAmount.AggregateBattlefield(Player.You, filter), ComparisonOperator.GTE, DynamicAmount.Fixed(1)
        )
        wizard.checkpoint shouldBe PaymentCheckpoint.CASTING
        wizard.baseMana shouldBe ManaCost.parse("{4}{U}")
        wizard.isolatedManaWhenTrue shouldBe ManaCost.parse("{3}{U}")
        wizard.verdict shouldBe Verdict.MATCH
        for (name in listOf("Tragic Slip", "Innkeeper's Talent", "Walking Corpse")) {
            extract(name).actions.flatMap { it.conditionalCosts }.isEmpty() shouldBe true
        }
    }

    test("supported Desert land and flying creature filters retain their full native restrictions") {
        for ((name, price) in listOf("Cactarantula" to "{3}{G}{G}", "Winged Words" to "{1}{U}")) {
            val cost = extract(name).actions.flatMap { it.conditionalCosts }.single()
            val native = cost.reductionSource as CostReductionSource.FixedIfControlFilter
            val condition = cost.condition as Compare
            (condition.left as DynamicAmount.AggregateBattlefield).filter shouldBe native.filter
            cost.isolatedManaWhenTrue shouldBe ManaCost.parse(price)
        }
        val discounts = extract("Geistlight Snare").actions.single { it.conditionalCosts.isNotEmpty() }.conditionalCosts
        discounts.size shouldBe 2
        discounts.map { it.declarationPath }.distinct().size shouldBe 2
        discounts.map { it.isolatedManaWhenTrue } shouldBe List(2) { ManaCost.parse("{1}{U}") }
    }

    test("unsupported predicates and unresolved actors differ from a contradictory controller") {
        controlledBattlefieldExistenceSupport(Player.You, GameObjectFilter.Creature.tapped()) shouldBe Verdict.UNKNOWN
        controlledBattlefieldExistenceSupport(Player.Each, GameObjectFilter.Creature) shouldBe Verdict.UNKNOWN
        controlledBattlefieldExistenceSupport(Player.You, GameObjectFilter.Creature.opponentControls()) shouldBe Verdict.NO_MATCH
    }

    test("zero reductions have no effect and variable base mana remains unresolved") {
        fun cost(amount: Int, base: String, gate: CostGating = CostGating.None) = checkNotNull(selfCastingConditionalCost(
            "synthetic.static[0]",
            ModifySpellCost(
                target = SpellCostTarget.SelfCast,
                gating = gate,
                modification = CostModification.ReduceGenericBy(
                    CostReductionSource.FixedIfControlFilter(amount, GameObjectFilter.Artifact)
                )
            ),
            ManaCost.parse(base)
        ))
        cost(0, "{2}{U}").verdict shouldBe Verdict.NO_MATCH
        cost(1, "{X}{U}").verdict shouldBe Verdict.UNKNOWN
        cost(1, "{2}{U}", CostGating.NthOfTypePerTurn(2)).verdict shouldBe Verdict.UNKNOWN
    }

    test("a synthetic sibling activation does not inherit another ability's discount") {
        val fixture = card("Synthetic separate payments") {
            typeLine = "Artifact"
            activatedAbility {
                cost = Costs.Mana("{2}{B}")
                effect = Effects.GainLife(1)
                costsLessIf("{B}", Conditions.YouDrewCardsThisTurn(3))
            }
            activatedAbility {
                cost = Costs.Mana("{1}")
                effect = Effects.GainLife(1)
            }
        }
        val actions = PathwayExtractor().extract(listOf(fixture)).actions
        actions.single { it.path == "script.activatedAbilities[0]" }.conditionalCosts.single()
            .isolatedManaWhenTrue shouldBe ManaCost.parse("{2}")
        actions.single { it.path == "script.activatedAbilities[1]" }.conditionalCosts.isEmpty() shouldBe true
    }
})
