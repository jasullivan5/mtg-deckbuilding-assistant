package dev.mtg.assistant

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.conditions.CreatureDiedThisTurnCondition
import com.wingedsheep.sdk.scripting.conditions.PlayerDrewCardsThisTurn
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ConditionalCostTest : FunSpec({
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

    test("Wizard predicate is retained unresolved and neither resolution effects nor Class levels become discounts") {
        val wizard = extract("Academy Journeymage").actions.flatMap { it.conditionalCosts }.single()
        wizard.verdict shouldBe Verdict.UNKNOWN
        for (name in listOf("Tragic Slip", "Innkeeper's Talent", "Walking Corpse")) {
            extract(name).actions.flatMap { it.conditionalCosts }.isEmpty() shouldBe true
        }
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
