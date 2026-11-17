package dev.mtg.assistant

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ControlledPermanentCostScenarioTest : FunSpec({
    val catalog by lazy { Catalog() }
    val academy = "Academy Journeymage"
    val wizard = "Merfolk Trickster"
    val ordinary = "Walking Corpse"

    for ((witness, mana, price) in listOf(
        Triple("none", 4, 5), Triple("none", 6, 5),
        Triple("caster", 3, 4), Triple("caster", 5, 4),
        Triple("opponent", 4, 5), Triple("wrong subtype", 4, 5),
        Triple("left battlefield", 4, 5)
    )) {
        test("Academy casting with witness=$witness and blue mana=$mana requires $price") {
            val d = scenarioDriver(*listOf(academy, wizard, ordinary).map { checkNotNull(catalog.card(it)) }.toTypedArray())
            val caster = d.player1
            when (witness) {
                "caster" -> d.putCreatureOnBattlefield(caster, wizard)
                "opponent" -> d.putCreatureOnBattlefield(d.player2, wizard)
                "wrong subtype" -> d.putCreatureOnBattlefield(caster, ordinary)
                "left battlefield" -> d.moveToGraveyard(d.putCreatureOnBattlefield(caster, wizard))
            }
            val spell = d.putCardInHand(caster, academy)
            d.giveMana(caster, Color.BLUE, mana)
            val cast = d.submit(CastSpell(caster, spell, paymentStrategy = PaymentStrategy.FromPool))
            val accepted = mana >= price
            (cast.error == null) shouldBe accepted
            d.state.getEntity(caster)?.get<ManaPoolComponent>()?.total shouldBe
                if (accepted) mana - price else mana
            if (accepted) d.resolvePendingActions()
            (spell in d.getPermanents(caster)) shouldBe accepted
        }
    }

    for ((witness, price) in listOf("Forest" to 6, "Eroded Canyon" to 5)) {
        test("Cactarantula casting with $witness pays $price") {
            val name = "Cactarantula"
            val d = scenarioDriver(checkNotNull(catalog.card(name)), checkNotNull(catalog.card("Eroded Canyon")))
            val caster = d.player1
            d.putLandOnBattlefield(caster, witness)
            val spell = d.putCardInHand(caster, name)
            d.giveMana(caster, Color.GREEN, 6)
            d.submit(CastSpell(caster, spell, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
            d.state.getEntity(caster)?.get<ManaPoolComponent>()?.total shouldBe 6 - price
            d.resolvePendingActions()
            (spell in d.getPermanents(caster)) shouldBe true
        }
    }
})
