package dev.mtg.assistant

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.DoubleCounterPlacement
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicCountersEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CounterPlacementTest : FunSpec({
    val catalog by lazy { Catalog() }
    val plus = CounterType.PLUS_ONE_PLUS_ONE
    val ours = Requirements(controller = Controller.YOU)
    fun extract(name: String) = PathwayExtractor(inspectCounters = true).extract(listOf(checkNotNull(catalog.card(name))))
    fun placement(fact: CounterPlacement, recipient: Requirements = ours) =
        all(counterPlacementDecisions(fact, plus, recipient).map { it.verdict })

    test("authored multipliers preserve the issuing action, factor and separate restrictions") {
        val q = CounterQuestion(plus, ours, amount = 2, placer = Controller.OPPONENT, sourceZone = Zone.BATTLEFIELD)
        for (name in listOf("Doubling Season", "Corpsejack Menace", "Branching Evolution")) {
            val extraction = extract(name)
            val modifier = extraction.counterModifiers.single()
            extraction.counterPlacements.isEmpty() shouldBe true
            extraction.eventOutcomes.isEmpty() shouldBe true
            extraction.actions.single { it.id == modifier.action.actionId }.native shouldBe modifier.action.native
            modifier.transformation shouldBe CounterTransformation.Multiplicative(2)
            modifier.effectsOnly shouldBe (name == "Doubling Season")
            isolatedCounterModification(modifier, q.copy(origin = PlacementOrigin.EFFECT)) shouldBe 4
            isolatedCounterModification(modifier, q.copy(amount = 0, origin = PlacementOrigin.EFFECT)) shouldBe null
            isolatedCounterModification(modifier, q.copy(recipient = Requirements(controller = Controller.OPPONENT), origin = PlacementOrigin.EFFECT)) shouldBe null
            isolatedCounterModification(modifier, q.copy(sourceZone = Zone.GRAVEYARD, origin = PlacementOrigin.EFFECT)) shouldBe null
            isolatedCounterModification(modifier, q.copy(sourceIsEntering = true, origin = PlacementOrigin.EFFECT)) shouldBe null
            isolatedCounterModification(modifier, q.copy(type = CounterType.CHARGE, origin = PlacementOrigin.EFFECT)) shouldBe
                if (name == "Doubling Season") 4 else null
            for (origin in listOf(PlacementOrigin.COST, PlacementOrigin.TURN_BASED)) {
                isolatedCounterModification(modifier, q.copy(origin = origin)) shouldBe if (modifier.effectsOnly) null else 4
            }
        }
        val season = extract("Doubling Season").counterModifiers.single()
        all(counterModifierDecisions(season, q).map { it.verdict }) shouldBe Verdict.MATCH
        all(counterModifierDecisions(season, q.copy(origin = PlacementOrigin.UNKNOWN)).map { it.verdict }) shouldBe Verdict.UNKNOWN
        isolatedCounterModification(season, q) shouldBe null
        isolatedCounterModification(season, q.copy(amount = null, origin = PlacementOrigin.EFFECT)) shouldBe null
    }

    test("placer-gated multipliers distinguish the actor and an unresolved recipient") {
        fun modifier(pattern: EventPattern) = PathwayExtractor(inspectCounters = true).extract(listOf(card("Synthetic multiplier") {
            typeLine = "Enchantment"
            replacementEffect(DoubleCounterPlacement(placedByYou = true, appliesTo = pattern))
        })).counterModifiers.single()
        val q = CounterQuestion(plus, ours, amount = 2, placer = Controller.YOU)
        val known = modifier(EventPattern.CounterPlacementEvent(plus, Recipient.CreatureYouControl))
        isolatedCounterModification(known, q) shouldBe 4
        isolatedCounterModification(known, q.copy(placer = Controller.OPPONENT)) shouldBe null
        val unresolved = modifier(EventPattern.CounterPlacementEvent(plus, Recipient.Object(GameObjectFilter.Creature.tapped())))
        all(counterModifierDecisions(unresolved, q).map { it.verdict }) shouldBe Verdict.UNKNOWN
        isolatedCounterModification(unresolved, q) shouldBe null
    }

    test("native Hardened Scales modifies a separate positive placement from either placer") {
        val x = extract("Hardened Scales")
        x.counterPlacements.isEmpty() shouldBe true
        x.eventOutcomes.isEmpty() shouldBe true
        val effect = x.counterModifiers.single()
        x.actions.single { it.id == effect.action.actionId }.native shouldBe effect.action.native
        x.structuralSupport.single { it.path == effect.action.path }.status shouldBe SupportStatus.SUPPORTED
        x.unsupported.any { it.path.startsWith("script.replacementEffects") } shouldBe false
        val q = CounterQuestion(plus, ours, amount = 2, placer = Controller.OPPONENT, sourceZone = Zone.BATTLEFIELD)
        isolatedCounterModification(effect, q) shouldBe 3
        isolatedCounterModification(effect, q.copy(amount = 0)) shouldBe null
        isolatedCounterModification(effect, q.copy(type = CounterType.CHARGE)) shouldBe null
        isolatedCounterModification(effect, q.copy(recipient = Requirements(controller = Controller.OPPONENT))) shouldBe null
        isolatedCounterModification(effect, q.copy(recipient = ours, sourceZone = Zone.GRAVEYARD)) shouldBe null
        isolatedCounterModification(effect, q.copy(sourceIsEntering = true)) shouldBe null
    }

    test("native Travel Preparations keeps the chosen creature group and either controller") {
        val x = extract("Travel Preparations")
        val fact = x.counterPlacements.single { it.action.actionId.endsWith("|script.spell") }
        fact.timing shouldBe PlacementTiming.RESOLUTION
        fact.amount shouldBe DynamicAmount.Fixed(1)
        fact.targets.single().count shouldBe 2
        fact.optional shouldBe true
        x.structuralSupport.single { it.path == fact.action.path }.status shouldBe SupportStatus.SUPPORTED
        x.unsupported.any { it.path == fact.action.path } shouldBe false
        placement(fact) shouldBe Verdict.MATCH
        placement(fact, Requirements(controller = Controller.OPPONENT)) shouldBe Verdict.MATCH
        x.counterModifiers.isEmpty() shouldBe true
    }

    test("native entry, outlast and removal remain separate operations") {
        val ballista = extract("Walking Ballista")
        val entry = ballista.counterPlacements.single { it.timing == PlacementTiming.ENTRY }
        entry.amount shouldBe DynamicAmount.XValue
        placement(entry) shouldBe Verdict.MATCH
        val grow = ballista.counterPlacements.single { it.timing == PlacementTiming.RESOLUTION }
        grow.action.actionId.endsWith("activatedAbilities[0]") shouldBe true
        val falconer = extract("Abzan Falconer")
        val outlast = falconer.counterPlacements.single()
        placement(outlast) shouldBe Verdict.MATCH
        val native = falconer.actions.single { it.id == outlast.action.actionId }.native.toString()
        native.contains("CostTap") shouldBe true
        native.contains("SorcerySpeed") shouldBe true
    }

    test("signed transformation and native recipient alternatives generalize") {
        val constrictor = extract("Winding Constrictor")
        val verdicts = constrictor.counterModifiers.map { all(counterModifierDecisions(it, CounterQuestion(plus, ours)).map { d -> d.verdict }) }
        verdicts.toSet() shouldBe setOf(Verdict.MATCH, Verdict.NO_MATCH)
        val synthetic = card("Synthetic signed replacement") {
            typeLine = "Enchantment"
            replacementEffect(ModifyCounterPlacement(modifier = -2, placedByYou = true))
        }
        val modifier = PathwayExtractor(inspectCounters = true).extract(listOf(synthetic)).counterModifiers.single()
        val q = CounterQuestion(plus, ours, amount = 1, placer = Controller.YOU)
        isolatedCounterModification(modifier, q) shouldBe 0
        isolatedCounterModification(modifier, q.copy(placer = Controller.OPPONENT)) shouldBe null
    }

    test("guards and unsupported quantities stay unknown while zero and wrong type fail") {
        fun operation(effect: com.wingedsheep.sdk.scripting.effects.Effect) = PathwayExtractor(inspectCounters = true)
            .extract(listOf(card("Synthetic counter operation") {
                typeLine = "Creature — Human"; power = 2; toughness = 2
                activatedAbility { cost = Costs.Mana("{1}"); this.effect = effect }
            })).counterPlacements.single()
        val one = AddCountersEffect(plus, 1, EffectTarget.Self)
        placement(operation(one.copy(count = 0))) shouldBe Verdict.NO_MATCH
        placement(operation(one.copy(counterType = CounterType.CHARGE))) shouldBe Verdict.NO_MATCH
        placement(operation(AddDynamicCountersEffect(plus, DynamicAmount.XValue, EffectTarget.Self))) shouldBe Verdict.UNKNOWN
        placement(operation(Effects.If(Conditions.CreatureDiedThisTurn, one))) shouldBe Verdict.UNKNOWN
        val optional = operation(Effects.May(one))
        optional.optional shouldBe true
        placement(optional) shouldBe Verdict.MATCH
    }
})
