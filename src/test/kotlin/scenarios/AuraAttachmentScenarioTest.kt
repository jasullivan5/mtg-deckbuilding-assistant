package dev.mtg.assistant

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AuraAttachmentScenarioTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun card(name: String) = checkNotNull(catalog.card(name))
    fun root(name: String) = PathwayExtractor().extract(listOf(card(name))).actions.single { it.path == "script" }

    test("Stab Wound shares one host relationship across continuous and triggered declarations") {
        val a = checkNotNull(root("Stab Wound").attachment)
        a.support shouldBe Verdict.MATCH
        val static = a.dependencies.single { it.kind == AttachmentOperationKind.CONTINUOUS_STATS }
        static.stats?.powerBonus shouldBe -2
        static.stats?.toughnessBonus shouldBe -2
        static.verdict shouldBe Verdict.MATCH
        static.actionId shouldBe "Stab Wound|front|script.staticAbilities[0]"
        val trigger = a.dependencies.single { it.kind == AttachmentOperationKind.TRIGGERED_OPERATION }
        trigger.verdict shouldBe Verdict.MATCH
        trigger.timingPlayer shouldBe Player.You
        trigger.timingUsesHostController shouldBe true
        trigger.recipient shouldBe EffectTarget.PlayerRef(Player.TriggeringPlayer)
        trigger.eventOutcomeIds.size shouldBe 1
        root("Burning Anger").attachment?.dependencies?.single()?.verdict shouldBe Verdict.UNKNOWN
        root("Holy Strength").attachment?.dependencies?.single()?.stats?.toughnessBonus shouldBe 2
    }

    for (name in listOf("Stab Wound", "Wanderlust", "Holy Strength")) {
        test("$name casts onto its selected creature and keeps Aura and host controllers distinct") {
            val d = scenarioDriver(card(name), card("Hill Giant"))
            val me = d.player1
            val them = d.player2
            val host = d.putCreatureOnBattlefield(them, "Hill Giant")
            val other = d.putCreatureOnBattlefield(them, "Hill Giant")
            val original = checkNotNull(d.state.objectRef(host))
            val spell = d.putCardInHand(me, name)
            d.giveMana(me, when (name) { "Stab Wound" -> Color.BLACK; "Wanderlust" -> Color.GREEN; else -> Color.WHITE }, 4)
            val castAction = root(name)
            val selected = CastSpell(me, spell, listOf(ChosenTarget.Permanent(host)), paymentStrategy = PaymentStrategy.FromPool)
            checkNotNull(castAction.targetSelection).atAnnouncement(castAction, card(name), selected, d.state,
                d.services.predicateEvaluator, ChosenTargetCondition(com.wingedsheep.sdk.scripting.GameObjectFilter.Creature))
                .verdict shouldBe Verdict.MATCH
            d.submit(CastSpell(me, spell, listOf(ChosenTarget.Permanent(host)),
                paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
            d.resolvePendingActions()
            val aura = d.getPermanents(me).single { d.state.getEntity(it)?.get<CardComponent>()?.name == name }
            val source = checkNotNull(d.state.objectRef(aura))
            val action = root(name)
            val relation = checkNotNull(action.attachment)
            val observed = relation.observe(action, d.state, source, d.cardRegistry, d.services.predicateEvaluator)
            observed.verdict shouldBe Verdict.MATCH
            observed.host shouldBe original
            observed.sourceController shouldBe me
            observed.hostController shouldBe them
            d.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe host
            d.state.projectedState.getPower(host) shouldBe if (name == "Stab Wound") 1 else if (name == "Holy Strength") 4 else 3
            d.state.projectedState.getToughness(other) shouldBe 3
            d.passPriorityUntil(Step.UPKEEP, them)
            d.resolvePendingActions()
            d.getLifeTotal(them) shouldBe when (name) { "Stab Wound" -> 18; "Wanderlust" -> 19; else -> 20 }
            d.getLifeTotal(me) shouldBe 20
            d.moveToGraveyard(aura)
            relation.observe(action, d.state, source, d.cardRegistry, d.services.predicateEvaluator).verdict shouldBe Verdict.NO_MATCH
            d.state.projectedState.getPower(host) shouldBe 3
        }
    }

    test("a noncreature cannot be selected as Stab Wound's casting target") {
        val d = scenarioDriver(card("Stab Wound"))
        val land = d.putLandOnBattlefield(d.player2, "Forest")
        val spell = d.putCardInHand(d.player1, "Stab Wound")
        d.giveMana(d.player1, Color.BLACK, 3)
        (d.submit(CastSpell(d.player1, spell, listOf(ChosenTarget.Permanent(land)),
            paymentStrategy = PaymentStrategy.FromPool)).error != null) shouldBe true
        d.stackSize shouldBe 0
        d.getPermanents(d.player1).isEmpty() shouldBe true
    }

    test("a returned creature is a new host object and inherits no prior attachment") {
        val d = scenarioDriver(card("Stab Wound"), card("Hill Giant"), card("Unsummon"))
        val me = d.player1
        val host = d.putCreatureOnBattlefield(me, "Hill Giant")
        val oldHost = checkNotNull(d.state.objectRef(host))
        d.giveMana(me, Color.BLACK, 3)
        d.submit(CastSpell(me, d.putCardInHand(me, "Stab Wound"), listOf(ChosenTarget.Permanent(host)),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        val aura = d.getPermanents(me).single { d.state.getEntity(it)?.get<CardComponent>()?.name == "Stab Wound" }
        val source = checkNotNull(d.state.objectRef(aura))
        d.giveMana(me, Color.BLUE, 1)
        d.submit(CastSpell(me, d.putCardInHand(me, "Unsummon"), listOf(ChosenTarget.Permanent(host)),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        val returning = d.getHand(me).single { d.state.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant" }
        d.giveMana(me, Color.RED, 4)
        d.submit(CastSpell(me, returning, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        val newHost = d.getPermanents(me).single()
        d.state.isCurrentObject(oldHost) shouldBe false
        d.state.projectedState.getPower(newHost) shouldBe 3
        val action = root("Stab Wound")
        checkNotNull(action.attachment).observe(action, d.state, source, d.cardRegistry,
            d.services.predicateEvaluator, oldHost).verdict shouldBe Verdict.NO_MATCH
    }

    test("an upkeep trigger retains its player after the source Aura leaves") {
        val d = scenarioDriver(card("Stab Wound"), card("Hill Giant"))
        val me = d.player1
        val host = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        d.giveMana(me, Color.BLACK, 3)
        d.submit(CastSpell(me, d.putCardInHand(me, "Stab Wound"), listOf(ChosenTarget.Permanent(host)),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        val aura = d.getPermanents(me).single()
        d.passPriorityUntil(Step.UPKEEP, d.player2)
        (d.stackSize > 0) shouldBe true
        d.moveToGraveyard(aura)
        d.resolvePendingActions()
        d.getLifeTotal(d.player2) shouldBe 18
    }

    test("a legal cast fails to attach when its host leaves before resolution") {
        val d = scenarioDriver(card("Stab Wound"), card("Hill Giant"), card("Unsummon"))
        val me = d.player1
        val host = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        d.giveMana(me, Color.BLACK, 3)
        val auraSpell = d.putCardInHand(me, "Stab Wound")
        d.submit(CastSpell(me, auraSpell, listOf(ChosenTarget.Permanent(host)),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.giveMana(me, Color.BLUE, 1)
        d.submit(CastSpell(me, d.putCardInHand(me, "Unsummon"), listOf(ChosenTarget.Permanent(host)),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        d.getPermanents(me).isEmpty() shouldBe true
        d.state.getGraveyard(me).any { d.state.getEntity(it)?.get<CardComponent>()?.name == "Stab Wound" } shouldBe true
        d.getHand(d.player2).any { d.state.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant" } shouldBe true
    }

    test("an unattached source is different from an unsupported enchant restriction") {
        val d = scenarioDriver(card("Burning Anger"))
        val source = d.putCardInHand(d.player1, "Burning Anger")
        val action = root("Burning Anger")
        val relation = checkNotNull(action.attachment)
        relation.observe(action, d.state, checkNotNull(d.state.objectRef(source)), d.cardRegistry,
            d.services.predicateEvaluator).verdict shouldBe Verdict.NO_MATCH
        attachmentRestrictionSupport(null) shouldBe Verdict.UNKNOWN
    }

    test("host controller changes affect the attachment binding without changing source control") {
        val d = scenarioDriver(card("Stab Wound"), card("Hill Giant"), card("Control Magic"))
        val me = d.player1
        val them = d.player2
        val host = d.putCreatureOnBattlefield(them, "Hill Giant")
        d.giveMana(me, Color.BLACK, 3)
        d.submit(CastSpell(me, d.putCardInHand(me, "Stab Wound"), listOf(ChosenTarget.Permanent(host)),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        val aura = d.getPermanents(me).single()
        val source = checkNotNull(d.state.objectRef(aura))
        d.giveMana(me, Color.BLUE, 4)
        d.submit(CastSpell(me, d.putCardInHand(me, "Control Magic"), listOf(ChosenTarget.Permanent(host)),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        val action = root("Stab Wound")
        val observation = checkNotNull(action.attachment).observe(action, d.state, source, d.cardRegistry, d.services.predicateEvaluator)
        observation.sourceController shouldBe me
        observation.hostController shouldBe me
        d.passPriorityUntil(Step.UPKEEP, them)
        d.resolvePendingActions()
        d.getLifeTotal(them) shouldBe 20
        d.passPriorityUntil(Step.UPKEEP, me)
        d.resolvePendingActions()
        d.getLifeTotal(me) shouldBe 18
    }
})
