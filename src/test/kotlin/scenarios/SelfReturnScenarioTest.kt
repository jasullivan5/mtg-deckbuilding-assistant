package dev.mtg.assistant

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import dev.mtg.assistant.objectives.nontokenCreatureDeath
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Bounded native positions: first return and a finality barrier, not a sacrifice/return loop. */
class SelfReturnScenarioTest : FunSpec({
    val catalog by lazy { Catalog() }
    test("Skeleton pays its cost and returns the original owned creature tapped without producing a death") {
        val card=catalog.card("Reassembling Skeleton")!!;val d=scenarioDriver(card);val me=d.player1
        val original=d.putCardInGraveyard(me,card.name);d.giveMana(me,Color.BLACK,1)
        val action=ActivateAbility(me,original,card.activatedAbilities.single().id,paymentStrategy=PaymentStrategy.FromPool)
        (d.submit(action).error!=null) shouldBe true
        (original in d.state.getGraveyard(me)) shouldBe true
        d.giveMana(me,Color.BLACK,1);val before=d.events.size
        d.submit(action).error shouldBe null;d.resolvePendingActions()
        val move=d.events.drop(before).filterIsInstance<ZoneChangeEvent>().single { it.entityId==original && it.toZone==Zone.BATTLEFIELD }
        move.fromZone shouldBe Zone.GRAVEYARD;move.ownerId shouldBe me
        requireNotNull(move.newObject).entityId shouldBe original
        (move.oldObject!=move.newObject) shouldBe true
        val returned=requireNotNull(move.newObject).entityId;val entity=d.state.getEntity(returned)!!
        (returned in d.state.getBattlefield(me)) shouldBe true
        entity.has<TappedComponent>() shouldBe true;entity.has<TokenComponent>() shouldBe false
        entity.get<OwnerComponent>()!!.playerId shouldBe me
        nontokenCreatureDeath(move,me).qualifies shouldBe false
    }
    test("Bloodghast observes our land entering while in the graveyard and optionally returns its original card") {
        val card=catalog.card("Bloodghast")!!;val d=scenarioDriver(card);val me=d.player1
        val original=d.putCardInGraveyard(me,card.name);val land=d.putCardInHand(me,"Forest");val before=d.events.size
        d.playLand(me,land).error shouldBe null;d.resolvePendingActions(accept=true)
        val move=d.events.drop(before).filterIsInstance<ZoneChangeEvent>().single { it.entityId==original && it.toZone==Zone.BATTLEFIELD }
        move.fromZone shouldBe Zone.GRAVEYARD;move.ownerId shouldBe me
        requireNotNull(move.newObject).entityId shouldBe original
        (move.oldObject!=move.newObject) shouldBe true
        val returned=requireNotNull(move.newObject).entityId
        (returned in d.state.getBattlefield(me)) shouldBe true
        d.state.getEntity(returned)!!.has<TokenComponent>() shouldBe false
        nontokenCreatureDeath(move,me).qualifies shouldBe false
    }
    test("Wolfbat returns with finality and is exiled instead of a subsequent qualifying death") {
        val wolf=catalog.card("Wolfbat")!!;val murder=catalog.card("Murder")!!
        val draw=card("Return source draw probe") { manaCost="{0}";typeLine="Instant";spell { effect=Effects.DrawCards(2) } }
        val d=scenarioDriver(wolf,murder,draw);val me=d.player1
        val original=d.putCardInGraveyard(me,wolf.name);d.giveMana(me,Color.BLACK,1)
        val before=d.events.size
        d.submit(CastSpell(me,d.putCardInHand(me,draw.name),paymentStrategy=PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions(accept=true)
        val returned=requireNotNull(d.events.drop(before).filterIsInstance<ZoneChangeEvent>().single { it.entityId==original && it.toZone==Zone.BATTLEFIELD }.newObject).entityId
        d.state.getEntity(returned)!!.get<CountersComponent>()!!.getCount(CounterType.FINALITY) shouldBe 1
        d.giveMana(me,Color.BLACK,3);val deathBefore=d.events.size
        d.submit(CastSpell(me,d.putCardInHand(me,murder.name),targets=listOf(ChosenTarget.Permanent(returned)),paymentStrategy=PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        val exit=d.events.drop(deathBefore).filterIsInstance<ZoneChangeEvent>().single { it.entityId==returned && it.fromZone==Zone.BATTLEFIELD }
        exit.toZone shouldBe Zone.EXILE;nontokenCreatureDeath(exit,me).qualifies shouldBe false
    }
})
