package dev.mtg.assistant

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ChosenTargetCostScenarioTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun card(name: String) = checkNotNull(catalog.card(name))
    fun action(name: String) = PathwayExtractor().extract(listOf(card(name))).actions.single { it.path == "script.spell" }

    test("the extracted generic and colored Brush Off modifiers remain separate") {
        val spell = action("Brush Off")
        val costs = spell.conditionalCosts
        costs.map { it.isolatedManaWhenTrue } shouldBe listOf(ManaCost.parse("{1}{U}{U}"), ManaCost.parse("{2}{U}"))
        costs.map { it.chosenTargetCondition }.distinct().size shouldBe 1
        costs.map { it.declarationPath }.distinct().size shouldBe 2
    }

    test("Brush Off uses both native reductions against an instant on the stack") {
        val counter = "Brush Off"
        val instant = "Dire Downdraft"
        val creature = "Walking Corpse"
        val d = scenarioDriver(card(counter), card(instant), card(creature))
        val me = d.player1
        val target = d.putCreatureOnBattlefield(d.player2, creature)
        d.giveMana(me, Color.BLUE, 6)
        d.submit(CastSpell(me, d.putCardInHand(me, instant), listOf(ChosenTarget.Permanent(target)),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        val stackSpell = checkNotNull(d.getTopOfStack())
        val actor = checkNotNull(d.priorityPlayer)
        if (actor != me) d.giveMana(actor, Color.BLUE, 2)
        d.submit(CastSpell(actor, d.putCardInHand(actor, counter), listOf(ChosenTarget.Spell(stackSpell)),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.state.getEntity(actor)?.get<ManaPoolComponent>()?.total shouldBe 0
        d.resolvePendingActions()
        (target in d.getPermanents(d.player2)) shouldBe true
    }

    test("native binding preserves both distinct Titanic Brawl requirements") {
        val name = "Titanic Brawl"
        val creature = "Walking Corpse"
        val d = scenarioDriver(card(name), card(creature))
        val me = d.player1
        val mine = ChosenTarget.Permanent(d.putCreatureOnBattlefield(me, creature))
        val theirs = ChosenTarget.Permanent(d.putCreatureOnBattlefield(d.player2, creature))
        val cast = CastSpell(me, d.putCardInHand(me, name), listOf(mine, theirs), targetGroupCounts = listOf(1, 1))
        val spell = action(name)
        val selection = checkNotNull(spell.targetSelection)
        val filter = ChosenTargetCondition(GameObjectFilter.Creature.youControl())
        val assessment = selection.atAnnouncement(spell, card(name), cast, d.state, d.services.predicateEvaluator, filter)
        assessment.verdict shouldBe Verdict.MATCH
        assessment.groups shouldBe listOf(listOf(mine), listOf(theirs))
        selection.atAnnouncement(spell, card(name), cast.copy(targets = listOf(theirs, mine)), d.state,
            d.services.predicateEvaluator, filter).verdict shouldBe Verdict.UNKNOWN
    }

    for ((tapped, mana, price) in listOf(Triple(true, 4, 3), Triple(false, 4, 4), Triple(false, 3, 4))) {
        test("Downdraft binds the selected creature at casting: tapped=$tapped mana=$mana") {
            val name = "Dire Downdraft"
            val creature = "Walking Corpse"
            val d = scenarioDriver(card(name), card(creature))
            val me = d.player1
            val target = d.putCreatureOnBattlefield(d.player2, creature)
            if (tapped) d.tapPermanent(target)
            d.tapPermanent(d.putCreatureOnBattlefield(d.player2, creature))
            val spell = d.putCardInHand(me, name)
            d.giveMana(me, Color.BLUE, mana)
            val cast = CastSpell(me, spell, listOf(ChosenTarget.Permanent(target)), paymentStrategy = PaymentStrategy.FromPool)
            val action = action(name)
            val selection = checkNotNull(action.targetSelection)
            val filter = ChosenTargetCondition(GameObjectFilter.Creature.tapped())
            val assessment = selection.atAnnouncement(action, card(name), cast, d.state, d.services.predicateEvaluator, filter)
            assessment.groups shouldBe listOf(cast.targets)
            assessment.verdict shouldBe if (tapped) Verdict.MATCH else Verdict.NO_MATCH
            val unsupported = ChosenTargetCondition(GameObjectFilter.Creature.copy(anyOf = listOf(GameObjectFilter.Creature)))
            selection.atAnnouncement(action, card(name), cast, d.state, d.services.predicateEvaluator,
                unsupported).verdict shouldBe Verdict.UNKNOWN
            selection.atAnnouncement(action.copy(id = "other action"), card(name), cast, d.state,
                d.services.predicateEvaluator, filter).verdict shouldBe Verdict.UNKNOWN
            val result = d.submit(cast)
            (result.error == null) shouldBe (mana >= price)
            result.events.any { it is SpellCastEvent } shouldBe (mana >= price)
            d.state.getEntity(me)?.get<ManaPoolComponent>()?.total shouldBe if (mana >= price) mana - price else mana
            if (tapped) {
                d.moveToGraveyard(target)
                d.resolvePendingActions()
                (target in d.state.getGraveyard(d.player2)) shouldBe true
                d.state.getEntity(me)?.get<ManaPoolComponent>()?.total shouldBe mana - price
            }
        }
    }

    for ((zone, cost) in listOf(Zone.GRAVEYARD to 2, Zone.HAND to null)) {
        test("No One Left Behind requires its selected cheap creature card in the graveyard: $zone") {
            val name = "No One Left Behind"
            val creature = "Walking Corpse"
            val d = scenarioDriver(card(name), card(creature))
            val me = d.player1
            val target = if (zone == Zone.GRAVEYARD) d.putCardInGraveyard(me, creature) else d.putCardInHand(me, creature)
            val spell = d.putCardInHand(me, name)
            d.giveMana(me, Color.BLACK, 3)
            val cast = CastSpell(me, spell, listOf(ChosenTarget.Card(target, me, zone)), paymentStrategy = PaymentStrategy.FromPool)
            (d.submit(cast).error == null) shouldBe (cost != null)
            d.state.getEntity(me)?.get<ManaPoolComponent>()?.total shouldBe (3 - (cost ?: 0))
            if (cost != null) d.resolvePendingActions()
            (target in d.getPermanents(me)) shouldBe (cost != null)
        }
    }

    for (ownTargets in 0..2) {
        test("This Town applies one fixed discount with $ownTargets matching selected permanents") {
            val name = "This Town Ain't Big Enough"
            val creature = "Walking Corpse"
            val d = scenarioDriver(card(name), card(creature))
            val me = d.player1
            val own = List(2) { d.putCreatureOnBattlefield(me, creature) }
            val other = List(2) { d.putCreatureOnBattlefield(d.player2, creature) }
            val selected = (own.take(ownTargets) + other.take(2 - ownTargets)).map { ChosenTarget.Permanent(it) }
            d.giveMana(me, Color.BLUE, 5)
            val cast = CastSpell(me, d.putCardInHand(me, name), selected, paymentStrategy = PaymentStrategy.FromPool)
            d.submit(cast).error shouldBe null
            d.state.getEntity(me)?.get<ManaPoolComponent>()?.total shouldBe if (ownTargets > 0) 3 else 0
            d.resolvePendingActions()
            selected.none { it.entityId in d.getPermanents(me) || it.entityId in d.getPermanents(d.player2) } shouldBe true
        }
    }
})
