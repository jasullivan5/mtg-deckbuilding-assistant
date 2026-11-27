package dev.mtg.assistant

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.GrantActivatedAbilityEffect
import dev.mtg.assistant.discovery.*
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TemporaryGrantScenarioTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun card(name: String) = checkNotNull(catalog.card(name))
    fun driver(vararg names: String) = scenarioDriver(*names.map(::card).toTypedArray())
    fun grant(name: String) = PathwayExtractor().extract(listOf(card(name))).actions.flatMap { it.temporaryGrants }.single()
    fun GameTestDriver.ref(id: EntityId) = checkNotNull(state.objectRef(id))
    fun GameTestDriver.priority(player: EntityId) {
        if (priorityPlayer != player) passPriority(checkNotNull(priorityPlayer)).error shouldBe null
        priorityPlayer shouldBe player
    }
    fun GameTestDriver.cast(name: String, target: EntityId, player: EntityId = player1, modes: List<Int> = emptyList()): EntityId {
        priority(player)
        Color.entries.forEach { giveMana(player,it,5) }
        val spell = putCardInHand(player,name)
        val targets = listOf(ChosenTarget.Permanent(target))
        submit(CastSpell(player,spell,if (modes.isEmpty()) targets else emptyList(),paymentStrategy=PaymentStrategy.FromPool,
            chosenModes=modes,modeTargetsOrdered=modes.map { targets })).error shouldBe null
        return spell
    }
    fun GameTestDriver.creation(grant: TemporaryGrant, stack: EntityId, expected: Verdict = Verdict.MATCH): TemporaryGrantObservation {
        val before = state
        val resolving = ref(stack)
        val mark = events.size
        bothPass().error shouldBe null
        val slice = events.drop(mark)
        return grant.observeCreation(before,state,slice,resolving,services).also {
            withClue(it.reason) { it.verdict shouldBe expected }
            if (it.verdict == Verdict.MATCH) {
                it.storeMultiplicity shouldBe listOf(0,1)
                grant.observeCreation(before,state,emptyList(),resolving,services).verdict shouldBe Verdict.UNKNOWN
                grant.copy(path = grant.path + ".effects[9]").observeCreation(before,state,slice,resolving,services)
                    .verdict shouldBe Verdict.UNKNOWN
            }
        }
    }
    fun GameTestDriver.expire(grant: TemporaryGrant, instance: TemporaryGrantInstance) {
        passPriorityUntil(Step.END)
        val before = state
        val mark = events.size
        grant.observeAvailability(before,instance).verdict shouldBe Verdict.MATCH
        bothPass().error shouldBe null
        if (state.step == Step.CLEANUP && pendingDecision != null) autoResolveDecision()
        val expiry = grant.observeExpiry(before,state,events.drop(mark),instance)
        withClue(expiry.reason) { expiry.verdict shouldBe Verdict.MATCH }
        grant.observeAvailability(state,instance).verdict shouldBe Verdict.NO_MATCH
    }
    fun GameTestDriver.enterAttack(source: EntityId, player: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS,player)
        declareAttackers(player,listOf(source),getOpponent(player)).error shouldBe null
        passPriorityUntil(Step.DECLARE_BLOCKERS,player)
        declareBlockers(getOpponent(player),emptyMap()).error shouldBe null
    }
    fun GameTestDriver.combatDamage(player: EntityId) {
        passPriorityUntil(Step.COMBAT_DAMAGE,player)
        if (pendingDecision is CombatResolutionDecision) confirmCombatDamage().error shouldBe null
    }

    test("Run Wild grants an opponent's creature regeneration, pays from its controller and expires at cleanup") {
        val plan = grant("Run Wild")
        val game = driver(plan.card,"Hill Giant")
        val host = game.putCreatureOnBattlefield(game.player2,"Hill Giant")
        val wrong = game.putCreatureOnBattlefield(game.player2,"Hill Giant")
        val spell = game.cast(plan.card,host)
        val origin = game.ref(spell)
        val created = checkNotNull(game.creation(plan,spell).instance)
        created.origin shouldBe origin
        created.recipient shouldBe game.ref(host)
        game.state.isCurrentObject(origin) shouldBe false
        plan.observeAvailability(game.state,created.copy(recipient=game.ref(wrong))).verdict shouldBe Verdict.NO_MATCH
        val ability = (plan.effect as GrantActivatedAbilityEffect).ability
        game.priority(game.player2)
        game.giveMana(game.player2,Color.GREEN,1)
        val before = game.state
        val action = ActivateAbility(game.player2,host,ability.id,paymentStrategy=PaymentStrategy.FromPool)
        val used = game.submit(action)
        used.error shouldBe null
        plan.observeActivation(before,game.state,used.events,created,action,game.services).verdict shouldBe Verdict.MATCH
        game.state.getEntity(game.player2)?.get<ManaPoolComponent>()?.green shouldBe 0
        game.resolvePendingActions()
        game.expire(plan,created)
        game.priority(game.player2)
        game.giveMana(game.player2,Color.GREEN,1)
        (game.submit(action).error != null) shouldBe true
    }

    test("Run Wild cannot bind a fizzled target or the renewed receiving object") {
        val plan = grant("Run Wild")
        val game = driver(plan.card,"Hill Giant","Cloudshift")
        val host = game.putCreatureOnBattlefield(game.player1,"Hill Giant")
        val old = game.ref(host)
        val spell = game.cast(plan.card,host)
        game.cast("Cloudshift",host)
        game.bothPass().error shouldBe null
        game.state.isCurrentObject(old) shouldBe false
        game.creation(plan,spell,Verdict.NO_MATCH)
        game.state.grantedActivatedAbilities.isEmpty() shouldBe true
    }

    test("Run Wild activation remains stacked after recipient renewal and duplicate providers remain ambiguous") {
        val plan = grant("Run Wild")
        val game = driver(plan.card,"Hill Giant","Cloudshift")
        val host = game.putCreatureOnBattlefield(game.player1,"Hill Giant")
        val first = game.cast(plan.card,host)
        val origin = checkNotNull(game.creation(plan,first).instance)
        val ability = (plan.effect as GrantActivatedAbilityEffect).ability
        val action = ActivateAbility(game.player1,host,ability.id,paymentStrategy=PaymentStrategy.FromPool)
        val before = game.state
        val result = game.submit(action)
        result.error shouldBe null
        val activated = plan.observeActivation(before,game.state,result.events,origin,action,game.services)
        activated.verdict shouldBe Verdict.MATCH
        game.cast("Cloudshift",host)
        game.bothPass().error shouldBe null
        plan.observeAvailability(game.state,origin).verdict shouldBe Verdict.NO_MATCH
        val stack = checkNotNull(activated.stackObject)
        game.state.isCurrentObject(stack) shouldBe true
        plan.observePersistence(game.state,activated).verdict shouldBe Verdict.MATCH
        val resolveMark = game.events.size
        game.bothPass().error shouldBe null
        game.events.drop(resolveMark).filterIsInstance<AbilityResolvedEvent>().any { it.abilityEntityId == stack.entityId } shouldBe true

        val source = game.cast(plan.card,host)
        val new = checkNotNull(game.creation(plan,source).instance)
        val duplicate = game.cast(plan.card,host)
        game.creation(plan,duplicate)
        game.state.grantedActivatedAbilities.count { it.entityId == host && it.ability.id == ability.id } shouldBe 2
        val ambiguousBefore = game.state
        val ambiguous = game.submit(action)
        ambiguous.error shouldBe null
        plan.observeActivation(ambiguousBefore,game.state,ambiguous.events,new,action,game.services).verdict shouldBe Verdict.UNKNOWN
    }

    test("Glorious Sunrise creates only the chosen land mode and its recipient taps for three mana") {
        val plan = grant("Glorious Sunrise")
        for (selectedMode in listOf(1,3)) {
            val game = driver(plan.card,"Island")
            game.putPermanentOnBattlefield(game.player1,plan.card)
            val land = game.putPermanentOnBattlefield(game.player2,"Island")
            game.passPriorityUntil(Step.BEGIN_COMBAT,game.player1)
            val choice = checkNotNull(game.pendingDecision as? ChooseOptionDecision)
            game.submitDecision(choice.playerId,OptionChosenResponse(choice.id,selectedMode)).error shouldBe null
            (game.pendingDecision as? ChooseTargetsDecision)?.let {
                game.submitTargetSelection(it.playerId,listOf(land)).error shouldBe null
            }
            val observed = game.creation(plan,game.state.stack.last(),
                if (selectedMode == 1) Verdict.MATCH else Verdict.NO_MATCH)
            if (selectedMode == 3) {
                game.state.grantedActivatedAbilities.isEmpty() shouldBe true
                game.getLifeTotal(game.player1) shouldBe 23
                continue
            }
            val instance = checkNotNull(observed.instance)
            instance.recipient shouldBe game.ref(land)
            game.priority(game.player2)
            val ability = (plan.effect as GrantActivatedAbilityEffect).ability
            val action = ActivateAbility(game.player2,land,ability.id)
            val before = game.state
            val used = game.submit(action)
            used.error shouldBe null
            val activation = plan.observeActivation(before,game.state,used.events,instance,action,game.services)
            withClue(activation.reason) { activation.verdict shouldBe Verdict.MATCH }
            activation.stackObject shouldBe null
            game.isTapped(land) shouldBe true
            game.state.getEntity(game.player2)?.get<ManaPoolComponent>()?.green shouldBe 3
            game.expire(plan,instance)
        }
    }

    test("Trash the Town creates a trigger for the opposing host, survives a blink after placement and expires independently") {
        val plan = grant("Trash the Town")
        for (blink in listOf(true,false)) {
            val game = driver(plan.card,"Hill Giant","Cloudshift")
            val attacker = game.putCreatureOnBattlefield(game.player2,"Hill Giant")
            val unrelated = game.putCreatureOnBattlefield(game.player2,"Hill Giant")
            game.removeSummoningSickness(attacker)
            game.passPriorityUntil(Step.PRECOMBAT_MAIN,game.player2)
            val spell = game.cast(plan.card,attacker,modes=listOf(2))
            val instance = checkNotNull(game.creation(plan,spell).instance)
            instance.originController shouldBe game.player1
            game.state.grantedTriggeredAbilities.none { it.entityId == unrelated } shouldBe true
            game.enterAttack(attacker,game.player2)
            val before = game.state
            val mark = game.events.size
            game.combatDamage(game.player2)
            val eventSlice = game.events.drop(mark)
            val trigger = eventSlice.filterIsInstance<AbilityTriggeredEvent>().single()
            val stack = game.ref(checkNotNull(trigger.abilityEntityId))
            val seen = plan.observeTrigger(before,game.state,eventSlice,instance,stack,game.services)
            withClue(seen.reason) { seen.verdict shouldBe Verdict.MATCH }
            seen.recipientController shouldBe game.player2
            if (blink) {
                game.cast("Cloudshift",attacker,game.player2)
                game.bothPass().error shouldBe null
                plan.observeAvailability(game.state,instance).verdict shouldBe Verdict.NO_MATCH
                game.state.isCurrentObject(stack) shouldBe true
                plan.observePersistence(game.state,seen).verdict shouldBe Verdict.MATCH
            }
            val cards = game.getHandSize(game.player2)
            game.resolvePendingActions()
            game.getHandSize(game.player2) shouldBe cards+2
            if (!blink) {
                game.expire(plan,instance)
                game.enterAttack(attacker,game.player2)
                val later = game.events.size
                game.combatDamage(game.player2)
                game.events.drop(later).filterIsInstance<AbilityTriggeredEvent>() shouldBe emptyList()
            }
        }
    }

    test("Commando Raid uses the receiving creature's combat event, target and power") {
        val plan = grant("Commando Raid")
        val game = driver(plan.card,"Hill Giant","Grizzly Bears")
        val attacker = game.putCreatureOnBattlefield(game.player1,"Hill Giant")
        val victim = game.putCreatureOnBattlefield(game.player2,"Grizzly Bears")
        game.removeSummoningSickness(attacker)
        val spell = game.cast(plan.card,attacker)
        val instance = checkNotNull(game.creation(plan,spell).instance)
        game.enterAttack(attacker,game.player1)
        val before = game.state
        val mark = game.events.size
        game.combatDamage(game.player1)
        val pick = checkNotNull(game.pendingDecision as? ChooseTargetsDecision)
        pick.playerId shouldBe game.player1
        game.submitTargetSelection(pick.playerId,listOf(victim)).error shouldBe null
        val trigger = game.events.drop(mark).filterIsInstance<AbilityTriggeredEvent>().single()
        val stack = game.ref(checkNotNull(trigger.abilityEntityId))
        val observed = plan.observeTrigger(before,game.state,game.events.drop(mark),instance,stack,game.services)
        withClue(observed.reason) { observed.verdict shouldBe Verdict.MATCH }
        game.bothPass().error shouldBe null
        val choice = checkNotNull(game.pendingDecision as? YesNoDecision)
        game.submitYesNo(choice.playerId,true).error shouldBe null
        game.events.drop(mark).filterIsInstance<DamageDealtEvent>().any {
            it.sourceId == attacker && it.targetId == victim && it.amount == 3 && !it.isCombatDamage
        } shouldBe true
        game.expire(plan,instance)
    }

    test("Commando Raid does not trigger when the recipient deals noncombat damage") {
        val plan = grant("Commando Raid")
        val game = driver(plan.card,"Prodigal Sorcerer","Grizzly Bears")
        val host = game.putCreatureOnBattlefield(game.player1,"Prodigal Sorcerer")
        game.putCreatureOnBattlefield(game.player2,"Grizzly Bears")
        game.removeSummoningSickness(host)
        val spell = game.cast(plan.card,host)
        val instance = checkNotNull(game.creation(plan,spell).instance)
        val ping = card("Prodigal Sorcerer").script.activatedAbilities.single()
        game.submit(ActivateAbility(game.player1,host,ping.id,listOf(ChosenTarget.Player(game.player2)))).error shouldBe null
        val mark = game.events.size
        game.bothPass().error shouldBe null
        val observed = game.events.drop(mark)
        observed.filterIsInstance<DamageDealtEvent>().single().let {
            it.sourceId shouldBe host
            it.targetId shouldBe game.player2
            it.isCombatDamage shouldBe false
        }
        observed.filterIsInstance<AbilityTriggeredEvent>() shouldBe emptyList()
        plan.observeAvailability(game.state,instance).verdict shouldBe Verdict.MATCH
    }
})
