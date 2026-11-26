package dev.mtg.assistant

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.EntityId
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SharedGrantRecipientScenarioTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun card(name: String) = checkNotNull(catalog.card(name))
    fun driver(vararg names: String) = scenarioDriver(*names.map(::card).toTypedArray())
    fun extract(name: String) = PathwayExtractor().extract(listOf(card(name))).actions
    fun activated(name: String) = extract(name).flatMap { it.grantedAbilities }.single()
    fun triggered(name: String) = extract(name).flatMap { it.grantedTriggers }.single()
    fun GameTestDriver.ref(id: EntityId) = checkNotNull(state.objectRef(id))
    fun GameTestDriver.available(grant: GrantedAbility, source: EntityId, recipient: EntityId) =
        grant.observeAvailability(state, ref(source), ref(recipient), services)
    fun GameTestDriver.available(grant: GrantedTrigger, source: EntityId, recipient: EntityId) =
        grant.observeAvailability(state, ref(source), ref(recipient), services)
    fun GameTestDriver.fund(player: EntityId) = Color.entries.forEach { giveMana(player, it, 10) }
    fun GameTestDriver.cast(name: String, target: EntityId? = null): EntityId {
        val id = putCardInHand(player1, name)
        fund(player1)
        submit(CastSpell(player1, id, target?.let { listOf(ChosenTarget.Permanent(it)) }.orEmpty(),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        return id
    }
    fun GameTestDriver.pairByCastingPartner(): EntityId {
        val partner = cast("Hill Giant")
        bothPass().error shouldBe null
        if (pendingDecision == null) bothPass().error shouldBe null
        (pendingDecision is YesNoDecision) shouldBe true
        submitYesNo(player1, true).error shouldBe null
        return partner
    }
    fun GameTestDriver.dealCombatDamage(attackers: List<EntityId>) {
        passPriorityUntil(Step.DECLARE_ATTACKERS, player1)
        declareAttackers(player1, attackers, player2).error shouldBe null
        passPriorityUntil(Step.DECLARE_BLOCKERS, player1)
        declareBlockers(player2, emptyMap()).error shouldBe null
        passPriorityUntil(Step.COMBAT_DAMAGE, player1)
        if (pendingDecision is CombatResolutionDecision) confirmCombatDamage().error shouldBe null
    }

    test("Gemhide crosses controllers but Manaweft qualifies its own controller and requires Sliver subtype") {
        val game = driver("Gemhide Sliver", "Manaweft Sliver", "Muscle Sliver", "Amoeboid Changeling")
        val gemhide = game.putCreatureOnBattlefield(game.player1, "Gemhide Sliver")
        val manaweft = game.putCreatureOnBattlefield(game.player1, "Manaweft Sliver")
        val opposing = game.putCreatureOnBattlefield(game.player2, "Muscle Sliver")
        val nonSliver = game.putCreatureOnBattlefield(game.player1, "Hill Giant")
        val gemhideGrant = activated("Gemhide Sliver")
        val manaweftGrant = activated("Manaweft Sliver")
        game.available(gemhideGrant, gemhide, nonSliver).verdict shouldBe Verdict.NO_MATCH
        game.available(manaweftGrant, manaweft, opposing).verdict shouldBe Verdict.NO_MATCH
        game.available(manaweftGrant, manaweft, gemhide).verdict shouldBe Verdict.MATCH
        val changeling = game.putCreatureOnBattlefield(game.player1, "Amoeboid Changeling")
        game.removeSummoningSickness(changeling)
        val addTypes = card("Amoeboid Changeling").script.activatedAbilities.first()
        game.submit(ActivateAbility(game.player1, changeling, addTypes.id,
            listOf(ChosenTarget.Permanent(nonSliver)))).error shouldBe null
        game.bothPass().error shouldBe null
        game.available(gemhideGrant, gemhide, nonSliver).verdict shouldBe Verdict.MATCH
        game.available(manaweftGrant, manaweft, nonSliver).verdict shouldBe Verdict.MATCH
        val instance = checkNotNull(game.available(gemhideGrant, gemhide, opposing).instance)
        game.removeSummoningSickness(opposing)
        game.passPriority(game.player1).error shouldBe null
        val before = game.state
        val action = ActivateAbility(game.player2, opposing, instance.abilityId, manaColorChoice = Color.BLUE)
        val result = game.submit(action)
        result.error shouldBe null
        gemhideGrant.observeActivation(before, game.state, action, result.events, instance, game.services).verdict shouldBe Verdict.MATCH
        game.isTapped(opposing) shouldBe true
        game.isTapped(gemhide) shouldBe false
    }

    test("Unctus uses projected color, projected control and explicit exclusion") {
        val name = "Unctus, Grand Metatect"
        val game = driver(name, "Tandem Lookout")
        val unctus = game.putCreatureOnBattlefield(game.player1, name)
        val recipient = game.putCreatureOnBattlefield(game.player1, "Hill Giant")
        val opposingBlue = game.putCreatureOnBattlefield(game.player2, "Tandem Lookout")
        val grant = triggered(name)
        grant.recipientSupport shouldBe Verdict.MATCH
        grant.eventSupport shouldBe Verdict.UNKNOWN
        game.available(grant, unctus, unctus).verdict shouldBe Verdict.NO_MATCH
        game.available(grant, unctus, opposingBlue).verdict shouldBe Verdict.NO_MATCH
        game.available(grant, unctus, recipient).verdict shouldBe Verdict.NO_MATCH
        game.fund(game.player1)
        game.submit(ActivateAbility(game.player1, unctus, card(name).script.activatedAbilities.single().id,
            listOf(ChosenTarget.Permanent(recipient)), paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        game.resolvePendingActions()
        game.available(grant, unctus, recipient).verdict shouldBe Verdict.MATCH
        game.passPriorityUntil(Step.UPKEEP, game.player2)
        game.available(grant, unctus, recipient).verdict shouldBe Verdict.NO_MATCH
    }

    test("Deadeye grants blink to both native pair members and the renewed receiver loses its old grant") {
        val game = driver("Deadeye Navigator")
        val navigator = game.putCreatureOnBattlefield(game.player1, "Deadeye Navigator")
        val unrelated = game.putCreatureOnBattlefield(game.player1, "Hill Giant")
        val grant = activated("Deadeye Navigator")
        game.available(grant, navigator, navigator).verdict shouldBe Verdict.NO_MATCH
        val partner = game.pairByCastingPartner()
        game.available(grant, navigator, navigator).verdict shouldBe Verdict.MATCH
        game.available(grant, navigator, unrelated).verdict shouldBe Verdict.NO_MATCH
        val instance = checkNotNull(game.available(grant, navigator, partner).instance)
        game.fund(game.player1)
        val before = game.state
        val action = ActivateAbility(game.player1, partner, instance.abilityId, paymentStrategy = PaymentStrategy.FromPool)
        val result = game.submit(action)
        result.error shouldBe null
        grant.observeActivation(before, game.state, action, result.events, instance, game.services).verdict shouldBe Verdict.MATCH
        game.bothPass().error shouldBe null
        if (game.pendingDecision == null) game.bothPass().error shouldBe null
        game.submitYesNo(game.player1, false).error shouldBe null
        (game.ref(partner) != instance.recipient) shouldBe true
        game.ref(navigator) shouldBe instance.granter
        game.available(grant, navigator, partner).verdict shouldBe Verdict.NO_MATCH
    }

    test("Tandem receives separate source triggers and the stacked draws survive partner departure") {
        val game = driver("Tandem Lookout", "Unsummon")
        val lookout = game.putCreatureOnBattlefield(game.player1, "Tandem Lookout")
        val grant = triggered("Tandem Lookout")
        val partner = game.pairByCastingPartner()
        game.removeSummoningSickness(lookout)
        game.removeSummoningSickness(partner)
        for (member in listOf(lookout, partner)) game.available(grant, lookout, member).verdict shouldBe Verdict.MATCH
        val mark = game.events.size
        game.dealCombatDamage(listOf(lookout, partner))
        val triggers = game.events.drop(mark).filterIsInstance<AbilityTriggeredEvent>()
        triggers.map { it.sourceId }.toSet() shouldBe setOf(lookout, partner)
        triggers.size shouldBe 2
        triggers.all { it.controllerId == game.player1 } shouldBe true
        game.cast("Unsummon", partner)
        game.bothPass().error shouldBe null
        game.available(grant, lookout, lookout).verdict shouldBe Verdict.NO_MATCH
        val hand = game.getHandSize(game.player1)
        game.resolvePendingActions()
        game.getHandSize(game.player1) shouldBe hand + 2
    }

    test("native soulbond ends after projected type loss or a controller change") {
        for (changeControl in listOf(false, true)) {
            val game = driver("Deadeye Navigator", "Control Magic", "Imprisoned in the Moon")
            val navigator = game.putCreatureOnBattlefield(game.player1, "Deadeye Navigator")
            val partner = game.pairByCastingPartner()
            val grant = activated("Deadeye Navigator")
            game.available(grant, navigator, partner).verdict shouldBe Verdict.MATCH
            if (changeControl) {
                game.passPriorityUntil(Step.PRECOMBAT_MAIN, game.player2)
                game.fund(game.player2)
                val control = game.putCardInHand(game.player2, "Control Magic")
                game.submit(CastSpell(game.player2, control, listOf(ChosenTarget.Permanent(partner)),
                    paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
            } else {
                game.cast("Imprisoned in the Moon", partner)
            }
            game.resolvePendingActions()
            if (changeControl) game.state.projectedState.getController(partner) shouldBe game.player2
            else game.state.projectedState.isCreature(partner) shouldBe false
            game.available(grant, navigator, partner).verdict shouldBe Verdict.NO_MATCH
            game.available(grant, navigator, navigator).verdict shouldBe Verdict.NO_MATCH
        }
    }

    test("two Synapse Sliver providers yield independent native triggers without exact supplying IDs") {
        val game = driver("Synapse Sliver", "Muscle Sliver")
        val first = game.putCreatureOnBattlefield(game.player1, "Synapse Sliver")
        val second = game.putCreatureOnBattlefield(game.player2, "Synapse Sliver")
        val receiver = game.putCreatureOnBattlefield(game.player1, "Muscle Sliver")
        val other = game.putCreatureOnBattlefield(game.player1, "Hill Giant")
        game.removeSummoningSickness(first)
        game.removeSummoningSickness(receiver)
        game.removeSummoningSickness(other)
        val grant = triggered("Synapse Sliver")
        val recipients = listOf(first, receiver)
        val instances = recipients.associateWith { recipient ->
            listOf(first, second).map { checkNotNull(game.available(grant, it, recipient).instance) }
        }
        game.available(grant, first, other).verdict shouldBe Verdict.NO_MATCH
        val before = game.state
        val mark = game.events.size
        game.dealCombatDamage(recipients + other)
        val events = game.events.drop(mark)
        val triggers = events.filterIsInstance<AbilityTriggeredEvent>()
        triggers.groupingBy { it.sourceId }.eachCount() shouldBe mapOf(first to 2, receiver to 2)
        for (event in triggers) {
            event.controllerId shouldBe game.player1
            val stack = game.ref(checkNotNull(event.abilityEntityId))
            val rule = checkNotNull(game.state.getEntity(stack.entityId)?.get<TriggeredAbilityOnStackComponent>())
            rule.granterId shouldBe null
            rule.objectReferences.origin shouldBe game.ref(event.sourceId)
            for (instance in instances.getValue(event.sourceId)) {
                grant.observeStack(before, game.state, events, instance, stack, game.services).verdict shouldBe Verdict.UNKNOWN
            }
        }
        val hand = game.getHandSize(game.player1)
        repeat(4) {
            game.bothPass().error shouldBe null
            game.submitYesNo(game.player1, true).error shouldBe null
        }
        game.getHandSize(game.player1) shouldBe hand + 4
    }

    test("a renewed Synapse provider does not restore the old generation or change trigger counts") {
        val game = driver("Synapse Sliver", "Muscle Sliver", "Unsummon")
        val provider = game.putCreatureOnBattlefield(game.player1, "Synapse Sliver")
        val receiver = game.putCreatureOnBattlefield(game.player1, "Muscle Sliver")
        game.removeSummoningSickness(receiver)
        val grant = triggered("Synapse Sliver")
        val old = checkNotNull(game.available(grant, provider, receiver).instance)
        var mark = game.events.size
        game.dealCombatDamage(listOf(receiver))
        game.events.drop(mark).filterIsInstance<AbilityTriggeredEvent>().single().sourceId shouldBe receiver
        game.cast("Unsummon", provider)
        game.bothPass().error shouldBe null
        game.available(grant, provider, receiver).verdict shouldBe Verdict.NO_MATCH
        val hand = game.getHandSize(game.player1)
        game.bothPass().error shouldBe null
        game.submitYesNo(game.player1, true).error shouldBe null
        game.getHandSize(game.player1) shouldBe hand + 1

        game.passPriorityUntil(Step.POSTCOMBAT_MAIN, game.player1)
        game.fund(game.player1)
        game.submit(CastSpell(game.player1, provider, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        game.bothPass().error shouldBe null
        val renewed = checkNotNull(game.available(grant, provider, receiver).instance)
        (old.granter != renewed.granter) shouldBe true
        grant.observeAvailability(game.state, old.granter, old.recipient, game.services).verdict shouldBe Verdict.NO_MATCH
        val before = game.state
        mark = game.events.size
        game.dealCombatDamage(listOf(receiver))
        val events = game.events.drop(mark)
        val trigger = events.filterIsInstance<AbilityTriggeredEvent>().single()
        val stack = game.ref(checkNotNull(trigger.abilityEntityId))
        grant.observeStack(before, game.state, events, old, stack, game.services).verdict shouldBe Verdict.UNKNOWN
        grant.observeStack(before, game.state, events, renewed, stack, game.services).verdict shouldBe Verdict.UNKNOWN
    }
})


