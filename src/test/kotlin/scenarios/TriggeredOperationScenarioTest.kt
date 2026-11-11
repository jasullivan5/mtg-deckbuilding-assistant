package dev.mtg.assistant

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TriggeredOperationScenarioTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun card(name: String) = checkNotNull(catalog.card(name))

    test("native entry sacrifice causes separate deaths and the death trigger changes both life totals") {
        val marauder = card("Fleshbag Marauder")
        val cutthroat = card("Zulaport Cutthroat")
        val corpse = card("Walking Corpse")
        val d = scenarioDriver(marauder, cutthroat, corpse)
        val me = d.player1
        val opponent = d.player2
        val theirs = d.putCreatureOnBattlefield(opponent, corpse.name)
        d.putCreatureOnBattlefield(me, cutthroat.name)
        d.giveMana(me, Color.BLACK, 3)
        d.submit(CastSpell(me, d.putCardInHand(me, marauder.name), paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        val deaths = d.events.filterIsInstance<ZoneChangeEvent>().filter { it.fromZone == Zone.BATTLEFIELD && it.toZone == Zone.GRAVEYARD }
        deaths.size shouldBe 2
        deaths.any { it.entityId == theirs } shouldBe true
        d.getLifeTotal(me) shouldBe 21
        d.getLifeTotal(opponent) shouldBe 19
    }

    test("native self death makes its controller and opponent lose life") {
        val ogre = card("Deathcurse Ogre")
        val murder = card("Murder")
        val d = scenarioDriver(ogre, murder)
        val me = d.player1
        val target = d.putCreatureOnBattlefield(me, ogre.name)
        d.giveMana(me, Color.BLACK, 3)
        d.submit(CastSpell(me, d.putCardInHand(me, murder.name), targets = listOf(ChosenTarget.Permanent(target)),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        d.getLifeTotal(me) shouldBe 17
        d.getLifeTotal(d.player2) shouldBe 17
    }
})
