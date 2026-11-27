package dev.mtg.assistant

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import dev.mtg.assistant.discovery.*
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ConditionalGrantScenarioTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun card(name: String) = checkNotNull(catalog.card(name))
    fun driver(vararg names: String) = scenarioDriver(*names.map(::card).toTypedArray())
    fun actions(name: String) = PathwayExtractor().extract(listOf(card(name))).actions
    fun triggers(name: String) = actions(name).flatMap { it.grantedTriggers }
    fun GameTestDriver.ref(id: EntityId) = checkNotNull(state.objectRef(id))
    fun GameTestDriver.priority(player: EntityId) {
        if (priorityPlayer != player) passPriority(checkNotNull(priorityPlayer)).error shouldBe null
        priorityPlayer shouldBe player
    }
    fun GameTestDriver.cast(name: String, target: EntityId? = null, player: EntityId = player1): EntityId {
        priority(player)
        Color.entries.forEach { giveMana(player, it, 10) }
        val id = putCardInHand(player, name)
        submit(CastSpell(player, id, target?.let { listOf(ChosenTarget.Permanent(it)) }.orEmpty(),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        return id
    }
    fun GameTestDriver.attack(source: EntityId, player: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS, player)
        declareAttackers(player, listOf(source), getOpponent(player)).error shouldBe null
    }
    fun GameTestDriver.damage(player: EntityId) {
        passPriorityUntil(Step.DECLARE_BLOCKERS, player)
        declareBlockers(getOpponent(player), emptyMap()).error shouldBe null
        passPriorityUntil(Step.COMBAT_DAMAGE, player)
        if (pendingDecision is CombatResolutionDecision) confirmCombatDamage().error shouldBe null
        (pendingDecision as? OrderTriggersDecision)?.let {
            submitDecision(it.playerId, TriggerOrderResponse(it.id, it.triggers.indices.toList())).error shouldBe null
        }
    }
    fun GameTestDriver.witness(grant: GrantedTrigger, before: GameState, mark: Int,
        instance: GrantedAbilityInstance, root: ActionEvidence? = null): GrantedTriggerObservation {
        val events = events.drop(mark)
        val emitted = events.filterIsInstance<AbilityTriggeredEvent>()
        val stacks = emitted.associateWith { event ->
            event.abilityEntityId?.let { state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>() }
        }
        val expectedProvider = instance.granter.entityId.takeUnless { grant.kind == GrantRecipientKind.SELF }
        val event = checkNotNull(stacks.entries.singleOrNull { (_, stack) ->
            stack?.resolvingTriggeredAbility == grant.declaration.ability && stack.granterId == expectedProvider
        }?.key) {
            "Expected ${grant.declaration.ability.id} provider=$expectedProvider, saw " +
                stacks.map { (e, s) -> listOf(e.sourceId, e.abilityEntityId, s?.resolvingTriggeredAbility?.id, s?.granterId, s?.description) }
        }
        val observation = grant.observeStack(before, state, events, instance,
            ref(checkNotNull(event.abilityEntityId)), services, root)
        withClue(observation.reason) { observation.verdict shouldBe Verdict.MATCH }
        return observation
    }

    test("Helm's two independent grants follow all four host color combinations and the host controller") {
        val name = "Helm of the Ghastlord"
        val roots = actions(name)
        val root = roots.single { it.path == "script" }
        val grants = roots.flatMap { it.grantedTriggers }.sortedBy { it.staticIndex }
        val cases = listOf("Hill Giant" to listOf(false, false), "Tandem Lookout" to listOf(true, false),
            "Vampire Nighthawk" to listOf(false, true), "Darkblade Agent" to listOf(true, true))
        for ((hostName, qualifying) in cases) {
            val game = driver(name, hostName, "Boomerang", "Blind Seer")
            val host = game.putCreatureOnBattlefield(game.player2, hostName)
            val seer = game.putCreatureOnBattlefield(game.player1, "Blind Seer")
            game.removeSummoningSickness(host)
            val aura = game.cast(name, host)
            game.resolvePendingActions()
            val originalAura = game.ref(aura)
            val originalHost = game.ref(host)
            val other = game.putCreatureOnBattlefield(game.player2, "Hill Giant")
            grants.first().observeAvailability(game.state, originalAura, game.ref(other), game.services, root).verdict shouldBe Verdict.NO_MATCH
            val witnessed = grants.mapIndexedNotNull { index, grant ->
                val availability = grant.observeAvailability(game.state, originalAura, originalHost, game.services, root)
                availability.condition?.verdict shouldBe if (qualifying[index]) Verdict.MATCH else Verdict.NO_MATCH
                if (qualifying[index]) grant to checkNotNull(availability.instance) else null
            }
            val before = game.state
            val mark = game.events.size
            game.attack(host, game.player2)
            game.damage(game.player2)
            game.events.drop(mark).filterIsInstance<AbilityTriggeredEvent>().size shouldBe qualifying.count { it }
            val seen = witnessed.map { (grant, instance) ->
                grant to game.witness(grant, before, mark, instance, root).also { it.controller shouldBe game.player2 }
            }
            if (qualifying.all { it }) {
                game.priority(game.player1)
                game.giveMana(game.player1, Color.BLUE, 2)
                game.submit(ActivateAbility(game.player1, seer, card("Blind Seer").script.activatedAbilities.single().id,
                    listOf(ChosenTarget.Permanent(host)), paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
                game.bothPass().error shouldBe null
                val choice = checkNotNull(game.pendingDecision as? ChooseColorDecision)
                game.submitDecision(game.player1, ColorChosenResponse(choice.id, Color.BLUE)).error shouldBe null
                grants.map { it.observeAvailability(game.state, originalAura, originalHost, game.services, root).verdict } shouldBe
                    listOf(Verdict.MATCH, Verdict.NO_MATCH)
                for ((grant, observation) in seen) grant.observePersistence(game.state, observation).verdict shouldBe Verdict.MATCH
                game.cast("Boomerang", aura)
                game.bothPass().error shouldBe null
                for ((grant, observation) in seen) {
                    grant.observeAvailability(game.state, originalAura, originalHost, game.services, root).verdict shouldBe Verdict.NO_MATCH
                    grant.observePersistence(game.state, observation).verdict shouldBe Verdict.MATCH
                }
                val draw = game.getHandSize(game.player2)
                val discard = game.getHandSize(game.player1)
                game.resolvePendingActions()
                game.getHandSize(game.player2) shouldBe draw + 1
                game.getHandSize(game.player1) shouldBe discard - 1
            }
        }
    }

    test("Villainous Ogre activates from itself and gets a regeneration shield after its Demon leaves") {
        val name = "Villainous Ogre"
        val game = driver(name, "Kuro, Pitlord", "Unsummon", "Rend Flesh")
        val ogre = game.putCreatureOnBattlefield(game.player1, name)
        val grant = actions(name).flatMap { it.grantedAbilities }.single()
        val source = game.ref(ogre)
        game.putCreatureOnBattlefield(game.player2, "Kuro, Pitlord")
        fun available() = grant.observeAvailability(game.state, source, source, game.services)
        available().condition?.verdict shouldBe Verdict.NO_MATCH
        val demon = game.putCreatureOnBattlefield(game.player1, "Kuro, Pitlord")
        val instance = checkNotNull(available().instance)
        grant.copy(path = "script.staticAbilities[0].ability").observeAvailability(game.state, source, source, game.services).verdict shouldBe Verdict.UNKNOWN
        grant.copy(staticIndex = 0).observeAvailability(game.state, source, source, game.services).verdict shouldBe Verdict.UNKNOWN
        game.giveMana(game.player1, Color.BLACK, 1)
        val before = game.state
        val activation = ActivateAbility(game.player1, ogre, instance.abilityId, paymentStrategy = PaymentStrategy.FromPool)
        val result = game.submit(activation)
        result.error shouldBe null
        game.state.getEntity(game.player1)?.get<ManaPoolComponent>()?.black shouldBe 0
        grant.observeActivation(before, game.state, activation, result.events, instance, game.services).verdict shouldBe Verdict.MATCH
        game.cast("Unsummon", demon)
        game.bothPass().error shouldBe null
        available().condition?.verdict shouldBe Verdict.NO_MATCH
        game.bothPass().error shouldBe null
        (game.submit(activation).error != null) shouldBe true
        game.cast("Rend Flesh", ogre)
        game.bothPass().error shouldBe null
        game.state.isCurrentObject(source) shouldBe true
        game.isTapped(ogre) shouldBe true
    }

    test("Relic Vial reads the native pre-death grant when sacrificing its final Cleric") {
        val name = "Relic Vial"
        val game = driver(name, "Kitesail Cleric")
        val relic = game.putPermanentOnBattlefield(game.player1, name)
        val source = game.ref(relic)
        val grant = triggers(name).single()
        grant.observeAvailability(game.state, source, source, game.services).verdict shouldBe Verdict.NO_MATCH
        val cleric = game.putCreatureOnBattlefield(game.player1, "Kitesail Cleric")
        val instance = checkNotNull(grant.observeAvailability(game.state, source, source, game.services).instance)
        game.giveColorlessMana(game.player1, 2)
        val before = game.state
        val mark = game.events.size
        game.submit(ActivateAbility(game.player1, relic, card(name).script.activatedAbilities.single().id,
            costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(cleric)),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        grant.observeAvailability(game.state, source, source, game.services).condition?.verdict shouldBe Verdict.NO_MATCH
        val observation = game.witness(grant, before, mark, instance)
        grant.observePersistence(game.state, observation).verdict shouldBe Verdict.MATCH
        grant.observeStack(game.state, game.state, game.events.drop(mark), instance,
            checkNotNull(observation.stackObject), game.services).verdict shouldBe Verdict.UNKNOWN
        game.resolvePendingActions()
        game.getLifeTotal(game.player1) shouldBe 21
        game.getLifeTotal(game.player2) shouldBe 19
    }

    test("Relic Vial attributes both triggers in a simultaneous batch that removes its final Cleric") {
        val name = "Relic Vial"
        val game = driver(name, "Kitesail Cleric", "Wrath of God")
        val relic = game.putPermanentOnBattlefield(game.player1, name)
        val cleric = game.putCreatureOnBattlefield(game.player1, "Kitesail Cleric")
        val other = game.putCreatureOnBattlefield(game.player1, "Hill Giant")
        val grant = triggers(name).single()
        val source = game.ref(relic)
        val before = game.state
        val instance = checkNotNull(grant.observeAvailability(before, source, source, game.services).instance)
        game.cast("Wrath of God")
        val mark = game.events.size
        game.bothPass().error shouldBe null
        (game.pendingDecision as? OrderTriggersDecision)?.let {
            game.submitDecision(it.playerId, TriggerOrderResponse(it.id, it.triggers.indices.toList())).error shouldBe null
        }
        val events = game.events.drop(mark)
        val placements = events.filterIsInstance<AbilityTriggeredEvent>()
        placements.size shouldBe 2
        val observedSubjects = placements.map { placement ->
            val ref = game.ref(checkNotNull(placement.abilityEntityId))
            val observation = grant.observeStack(before, game.state, events, instance, ref, game.services)
            withClue(observation.reason) { observation.verdict shouldBe Verdict.MATCH }
            observation.context?.triggeringEntityId
        }
        observedSubjects.toSet() shouldBe setOf(cleric, other)
        grant.observeAvailability(game.state, source, source, game.services).condition?.verdict shouldBe Verdict.NO_MATCH
        game.resolvePendingActions()
        game.getLifeTotal(game.player1) shouldBe 22
        game.getLifeTotal(game.player2) shouldBe 18
    }

    test("Fire Nation Cadets retain an established attack trigger after the enabling Lesson is exiled") {
        val name = "Fire Nation Cadets"
        val lesson = "Environmental Sciences"
        val game = driver(name, lesson, "Tormod's Crypt")
        val cadets = game.putCreatureOnBattlefield(game.player1, name)
        game.removeSummoningSickness(cadets)
        val crypt = game.putPermanentOnBattlefield(game.player2, "Tormod's Crypt")
        val source = game.ref(cadets)
        val grant = triggers(name).single()
        fun available() = grant.observeAvailability(game.state, source, source, game.services)
        game.putCardInGraveyard(game.player2, lesson)
        available().condition?.verdict shouldBe Verdict.NO_MATCH
        game.putCardInGraveyard(game.player1, lesson)
        val instance = checkNotNull(available().instance)
        val before = game.state
        val mark = game.events.size
        game.attack(cadets, game.player1)
        val observation = game.witness(grant, before, mark, instance)
        game.priority(game.player2)
        game.submit(ActivateAbility(game.player2, crypt, card("Tormod's Crypt").script.activatedAbilities.single().id,
            listOf(ChosenTarget.Player(game.player1)))).error shouldBe null
        game.bothPass().error shouldBe null
        available().condition?.verdict shouldBe Verdict.NO_MATCH
        grant.observePersistence(game.state, observation).verdict shouldBe Verdict.MATCH
        game.bothPass().error shouldBe null
        val mana = game.state.getEntity(game.player1)?.get<ManaPoolComponent>()?.restrictedMana.orEmpty()
        mana.filter { it.expiry == ManaExpiry.END_OF_COMBAT }.map { it.color } shouldBe listOf(Color.RED, Color.RED)
    }

    test("Darkblade Agent's native surveil history enables its draw trigger for that turn") {
        val name = "Darkblade Agent"
        for (surveilled in listOf(false, true)) {
            val game = driver(name, "Consider")
            val agent = game.putCreatureOnBattlefield(game.player1, name)
            game.removeSummoningSickness(agent)
            val source = game.ref(agent)
            val grant = triggers(name).single()
            fun available() = grant.observeAvailability(game.state, source, source, game.services)
            available().condition?.verdict shouldBe Verdict.NO_MATCH
            if (surveilled) {
                game.cast("Consider")
                game.resolvePendingActions()
            }
            val instance = available().instance
            (instance != null) shouldBe surveilled
            val before = game.state
            val mark = game.events.size
            game.attack(agent, game.player1)
            game.damage(game.player1)
            if (instance == null) {
                game.events.drop(mark).filterIsInstance<AbilityTriggeredEvent>().size shouldBe 0
            } else {
                game.witness(grant, before, mark, instance)
                val hand = game.getHandSize(game.player1)
                game.resolvePendingActions()
                game.getHandSize(game.player1) shouldBe hand + 1
                game.passPriorityUntil(Step.UPKEEP, game.player2)
                available().condition?.verdict shouldBe Verdict.NO_MATCH
            }
        }
    }

    test("two conditional Auras supply separate triggers and a recast granter cannot claim the earlier trigger") {
        val name = "Helm of the Ghastlord"
        val game = driver(name, "Boomerang", "Tandem Lookout", "Vedalken Orrery", "Cloudshift")
        game.putPermanentOnBattlefield(game.player1, "Vedalken Orrery")
        val host = game.putCreatureOnBattlefield(game.player2, "Tandem Lookout")
        game.removeSummoningSickness(host)
        val first = game.cast(name, host)
        game.resolvePendingActions()
        val second = game.cast(name, host)
        game.resolvePendingActions()
        val root = actions(name).single { it.path == "script" }
        val blue = triggers(name).single { it.staticIndex == 1 }
        fun available(source: EntityId) = blue.observeAvailability(game.state, game.ref(source), game.ref(host), game.services, root)
        val a = checkNotNull(available(first).instance)
        val b = checkNotNull(available(second).instance)
        val before = game.state
        val mark = game.events.size
        game.attack(host, game.player2)
        game.damage(game.player2)
        val firstTrigger = game.witness(blue, before, mark, a, root)
        game.witness(blue, before, mark, b, root)
        game.cast("Boomerang", first)
        game.bothPass().error shouldBe null
        game.priority(game.player1)
        game.submit(CastSpell(game.player1, first, listOf(ChosenTarget.Permanent(host)),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        game.bothPass().error shouldBe null
        val renewed = checkNotNull(available(first).instance)
        (renewed.granter != a.granter) shouldBe true
        blue.observePersistence(game.state, firstTrigger).verdict shouldBe Verdict.MATCH
        blue.observeStack(before, game.state, game.events.drop(mark), renewed,
            checkNotNull(firstTrigger.stackObject), game.services, root).verdict shouldBe Verdict.UNKNOWN
        game.cast("Cloudshift", host, game.player2)
        game.bothPass().error shouldBe null
        (game.ref(host) != a.recipient) shouldBe true
        blue.observeAvailability(game.state, renewed.granter, a.recipient, game.services, root).verdict shouldBe Verdict.NO_MATCH
        blue.observePersistence(game.state, firstTrigger).verdict shouldBe Verdict.MATCH
    }

    test("Oculus Whelp's simultaneous departure carries an enabled native grant despite structural uncertainty") {
        val name = "Oculus Whelp"
        val werewolf = "Smoldering Werewolf"
        val game = driver(name, werewolf, "Wrath of God")
        val wolf = game.putCreatureOnBattlefield(game.player1, werewolf)
        val whelp = game.putCreatureOnBattlefield(game.player1, name)
        val source = game.ref(whelp)
        val grant = triggers(name).single()
        grant.conditional?.support shouldBe Verdict.UNKNOWN
        game.giveMana(game.player1, Color.RED, 6)
        game.submit(ActivateAbility(game.player1, wolf, card(werewolf).script.activatedAbilities.single().id,
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        game.resolvePendingActions()
        grant.observeAvailability(game.state, source, source, game.services).verdict shouldBe Verdict.UNKNOWN
        game.cast("Wrath of God")
        val mark = game.events.size
        val hand = game.getHandSize(game.player1)
        game.bothPass().error shouldBe null
        val events = game.events.drop(mark)
        val death = events.filterIsInstance<ZoneChangeEvent>().single { it.oldObject == source }
        (grant.declaration.ability.id in death.lastKnown?.lookBackGrants?.conditionalSelfGrantIds.orEmpty()) shouldBe true
        events.filterIsInstance<AbilityTriggeredEvent>().single().sourceId shouldBe whelp
        game.resolvePendingActions()
        game.getHandSize(game.player1) shouldBe hand + 1
    }
})
