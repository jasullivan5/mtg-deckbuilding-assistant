package dev.mtg.assistant

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ClassLevelChangedEvent
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ClassAvailabilityScenarioTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun card(name: String) = checkNotNull(catalog.card(name))

    test("Innkeeper levels through two paid actions and doubles only its controller's placement") {
        val inn = card("Innkeeper's Talent")
        val travel = card("Travel Preparations")
        val hunger = card("Hunger of the Howlpack")
        val body = card("Walking Corpse")
        val d = scenarioDriver(inn, travel, hunger, body)
        val me = d.player1
        val them = d.player2
        val recipient = d.putCreatureOnBattlefield(me, body.name)
        val source = d.putCardInHand(me, inn.name)
        d.giveMana(me, Color.GREEN, 2)
        d.submit(CastSpell(me, source, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        fun level() = d.state.getEntity(source)?.get<ClassLevelComponent>()?.currentLevel
        fun advance(target: Int, mana: Int) {
            d.giveMana(me, Color.GREEN, mana)
            d.submit(ActivateAbility(me, source, AbilityId.classLevelUp(target), paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
            d.resolvePendingActions()
            level() shouldBe target
        }
        fun place() {
            d.giveMana(me, Color.GREEN, 2)
            d.submit(CastSpell(me, d.putCardInHand(me, travel.name), listOf(ChosenTarget.Permanent(recipient)),
                paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
            d.resolvePendingActions()
        }
        level() shouldBe 1
        place()
        advance(2, 1)
        place()
        advance(3, 4)
        place()
        val unaffected = d.putCreatureOnBattlefield(me, body.name)
        d.giveMana(them, Color.GREEN)
        d.passPriority(me)
        d.submit(CastSpell(them, d.putCardInHand(them, hunger.name), listOf(ChosenTarget.Permanent(unaffected)),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        val plus = CounterType.PLUS_ONE_PLUS_ONE
        val additions = d.events.filterIsInstance<CountersAddedEvent>().filter { it.counterType == plus }
        additions.filter { it.entityId == recipient }.map { it.amount } shouldBe listOf(1, 1, 2)
        additions.filter { it.entityId == unaffected }.map { it.amount } shouldBe listOf(1)
        d.state.getEntity(recipient)?.get<CountersComponent>()?.getCount(plus) shouldBe 4
        d.state.getEntity(unaffected)?.get<CountersComponent>()?.getCount(plus) shouldBe 1
        d.events.filterIsInstance<ClassLevelChangedEvent>().filter { it.entityId == source }.map { it.newLevel } shouldBe listOf(2, 3)
    }

    for (level in listOf(1, 3)) test("Stormchaser's Talent cast trigger with constructed level $level") {
        val talent = card("Stormchaser's Talent")
        val unsummon = card("Unsummon")
        val body = card("Walking Corpse")
        val d = scenarioDriver(talent, unsummon, body)
        val me = d.player1
        d.putPermanentOnBattlefield(me, talent.name, classLevel = level)
        val target = d.putCreatureOnBattlefield(d.player2, body.name)
        d.giveMana(me, Color.BLUE)
        d.submit(CastSpell(me, d.putCardInHand(me, unsummon.name), listOf(ChosenTarget.Permanent(target)),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        d.getPermanents(me).count { d.state.getEntity(it)?.get<TokenComponent>() != null } shouldBe if (level == 3) 1 else 0
    }
})
