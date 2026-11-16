package dev.mtg.assistant

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.CardsDrawnThisTurnComponent
import com.wingedsheep.engine.state.components.player.CreaturesDiedThisTurnComponent
import com.wingedsheep.sdk.core.Color
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ConditionalCostScenarioTest : FunSpec({
    val catalog by lazy { Catalog() }
    for ((death, mana, pays) in listOf(Triple(false, 2, false), Triple(false, 5, true), Triple(true, 1, false), Triple(true, 2, true))) {
        test("Bat-Cloud payable with death=$death and black mana=$mana: $pays") {
            val card = checkNotNull(catalog.card("Dreaded Bat-Cloud"))
            val d = scenarioDriver(card)
            val me = d.player1
            if (death) d.addComponent(d.player2, CreaturesDiedThisTurnComponent(1))
            d.giveMana(me, Color.BLACK, mana)
            val id = d.putCardInHand(me, card.name)
            val result = d.submit(CastSpell(me, id, paymentStrategy = PaymentStrategy.FromPool))
            (result.error == null) shouldBe pays
            if (pays) d.resolvePendingActions()
            (id in d.getPermanents(me)) shouldBe pays
        }
    }

    for ((draws, mana, pays) in listOf(Triple(2, 0, false), Triple(0, 5, true), Triple(3, 0, true))) {
        test("Kami payable with activator draws=$draws and black mana=$mana: $pays") {
            val card = checkNotNull(catalog.card("Kami of Jealous Thirst"))
            val d = scenarioDriver(card)
            val me = d.player1
            d.addComponent(me, CardsDrawnThisTurnComponent(draws))
            d.addComponent(d.player2, CardsDrawnThisTurnComponent(3))
            if (mana > 0) d.giveMana(me, Color.BLACK, mana)
            val id = d.putCreatureOnBattlefield(me, card.name)
            val result = d.submit(ActivateAbility(me, id, card.activatedAbilities.single().id, paymentStrategy = PaymentStrategy.FromPool))
            (result.error == null) shouldBe pays
            if (pays) d.resolvePendingActions()
            d.getLifeTotal(d.player2) shouldBe if (pays) 18 else 20
        }
    }
})
