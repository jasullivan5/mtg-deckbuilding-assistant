package dev.mtg.assistant

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Observed native outcomes, separate from the structural Discover judgment. */
class DeathHistoryScenarioTest : FunSpec({
    val catalog by lazy { Catalog() }
    for(priorDeath in listOf(false,true)) test("Tragic Slip resolution with opposing creature death=$priorDeath") {
        val slip=catalog.card("Tragic Slip")!!
        val murder=catalog.card("Murder")!!
        val body=catalog.card("Walking Corpse")!!
        val d=scenarioDriver(slip,murder,body); val me=d.player1; val opponent=d.player2
        val target=d.putCreatureOnBattlefield(opponent,body.name)
        fun cast(name: String, victim: com.wingedsheep.sdk.model.EntityId) {
            d.submit(CastSpell(me,d.putCardInHand(me,name),targets=listOf(ChosenTarget.Permanent(victim)),
                paymentStrategy=PaymentStrategy.FromPool)).error shouldBe null
            d.resolvePendingActions()
        }
        d.giveMana(me,Color.BLACK,if(priorDeath) 4 else 1)
        if(priorDeath) {
            val first=d.putCreatureOnBattlefield(opponent,body.name)
            cast(murder.name,first)
            d.events.any { it is ZoneChangeEvent && it.entityId==first && it.fromZone==Zone.BATTLEFIELD && it.toZone==Zone.GRAVEYARD } shouldBe true
        }
        cast(slip.name,target)
        (target in d.state.getBattlefield(opponent)) shouldBe !priorDeath
    }
})
