package dev.mtg.assistant

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CounterPlacementScenarioTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun card(name: String) = checkNotNull(catalog.card(name))
    val plus = CounterType.PLUS_ONE_PLUS_ONE

    test("Doubling Season doubles Jace Beleren's entry but skips the loyalty cost") {
        val season = card("Doubling Season")
        val jace = card("Jace Beleren")
        val d = scenarioDriver(season, jace)
        val me = d.player1
        d.giveMana(me, Color.GREEN, 5)
        d.submit(CastSpell(me, d.putCardInHand(me, season.name), paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        d.giveMana(me, Color.BLUE, 3)
        val walker = d.putCardInHand(me, jace.name)
        d.submit(CastSpell(me, walker, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        fun loyalty() = d.state.getEntity(walker)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY)
        loyalty() shouldBe 6
        d.submit(ActivateAbility(me, walker, jace.activatedAbilities.first().id)).error shouldBe null
        loyalty() shouldBe 8
    }

    test("Doubling Season modifies Travel Preparations only for the qualifying recipient") {
        val season = card("Doubling Season")
        val travel = card("Travel Preparations")
        val body = card("Walking Corpse")
        val d = scenarioDriver(season, travel, body)
        val me = d.player1
        val mine = d.putCreatureOnBattlefield(me, body.name)
        val theirs = d.putCreatureOnBattlefield(d.player2, body.name)
        d.giveMana(me, Color.GREEN, 7)
        d.submit(CastSpell(me, d.putCardInHand(me, season.name), paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        d.submit(CastSpell(me, d.putCardInHand(me, travel.name), listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(theirs)),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        val additions = d.events.filterIsInstance<CountersAddedEvent>().filter { it.counterType == plus }
        for ((recipient, expected) in listOf(mine to 2, theirs to 1)) {
            additions.filter { it.entityId == recipient }.map { it.amount } shouldBe listOf(expected)
            d.state.getEntity(recipient)?.get<CountersComponent>()?.getCount(plus) shouldBe expected
        }
    }

    for ((withSeason, x) in listOf(false to 2, true to 2, true to 0))
        test("Walking Ballista entry with Doubling Season=$withSeason and X=$x") {
            val season = card("Doubling Season")
            val ballista = card("Walking Ballista")
            val d = scenarioDriver(season, ballista)
            val me = d.player1
            d.giveMana(me, Color.GREEN, 9)
            if (withSeason) {
                d.submit(CastSpell(me, d.putCardInHand(me, season.name), paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
                d.resolvePendingActions()
            }
            val entrant = d.putCardInHand(me, ballista.name)
            d.submit(CastSpell(me, entrant, xValue = x, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
            d.resolvePendingActions()
            val expected = if (withSeason) x * 2 else x
            d.events.filterIsInstance<CountersAddedEvent>().filter { it.entityId == entrant && it.counterType == plus }
                .map { it.amount } shouldBe if (expected == 0) emptyList() else listOf(expected)
            (d.state.getEntity(entrant)?.get<CountersComponent>()?.getCount(plus) ?: 0) shouldBe expected
        }

    for (scalesActive in listOf(false, true)) test("Travel Preparations places counters on chosen creatures with Scales active=$scalesActive") {
        val scales = card("Hardened Scales")
        val travel = card("Travel Preparations")
        val body = card("Walking Corpse")
        val d = scenarioDriver(scales, travel, body)
        val me = d.player1
        val mine = d.putCreatureOnBattlefield(me, body.name)
        val theirs = d.putCreatureOnBattlefield(d.player2, body.name)
        d.giveMana(me, Color.GREEN, 3)
        if (scalesActive) {
            d.submit(CastSpell(me, d.putCardInHand(me, scales.name), paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
            d.resolvePendingActions()
        }
        d.submit(CastSpell(me, d.putCardInHand(me, travel.name), listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(theirs)),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        val additions = d.events.filterIsInstance<CountersAddedEvent>().filter { it.counterType == plus }
        additions.filter { it.entityId == mine }.map { it.amount } shouldBe listOf(if (scalesActive) 2 else 1)
        additions.filter { it.entityId == theirs }.map { it.amount } shouldBe listOf(1)
    }

    for (x in listOf(0, 1)) test("Walking Ballista entry under Hardened Scales with X=$x") {
        val scales = card("Hardened Scales")
        val ballista = card("Walking Ballista")
        val d = scenarioDriver(scales, ballista)
        val me = d.player1
        d.giveMana(me, Color.GREEN, 5)
        d.submit(CastSpell(me, d.putCardInHand(me, scales.name), paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        val entrant = d.putCardInHand(me, ballista.name)
        d.submit(CastSpell(me, entrant, xValue = x, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        d.events.filterIsInstance<CountersAddedEvent>().filter { it.entityId == entrant && it.counterType == plus }
            .map { it.amount } shouldBe if (x == 0) emptyList() else listOf(2)
        (d.state.getEntity(entrant)?.get<CountersComponent>()?.getCount(plus) ?: 0) shouldBe if (x == 0) 0 else 2
    }
})
