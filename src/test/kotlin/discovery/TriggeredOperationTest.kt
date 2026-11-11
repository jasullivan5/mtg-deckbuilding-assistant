package dev.mtg.assistant

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TriggeredOperationTest : FunSpec({
    val catalog by lazy { Catalog() }
    val report by lazy { discover(catalog, "Judith, the Scourge Diva") }
    fun extract(name: String) = PathwayExtractor().extract(listOf(checkNotNull(catalog.card(name))))
    fun fixture(effect: Effect, entry: Boolean = false, filter: GameObjectFilter = GameObjectFilter.Creature.youControl()) =
        card("Synthetic event operation") {
            typeLine = "Creature — Human"; power = 2; toughness = 2
            triggeredAbility {
                trigger = if (entry) Triggers.another(filter).enters() else Triggers.another(filter).dies()
                this.effect = effect
            }
            activatedAbility { cost = Costs.Sacrifice(GameObjectFilter.Creature); this.effect = Effects.DrawCards(1) }
        }
    fun rows(effect: Effect, filter: GameObjectFilter = GameObjectFilter.Creature.youControl()) =
        deathResponses(PathwayExtractor().extract(listOf(fixture(effect, filter = filter))).eventOutcomes)
    fun entry(o: EventOutcome) = all(eventOperationDecisions(o, ZoneEventRequirement(to = Zone.BATTLEFIELD)).map { it.verdict })

    test("a synthetic opponent sacrifice has an exact same-action fact distinct from an activation cost") {
        val sacrifice = ForceSacrificeEffect(GameObjectFilter.Creature.nontoken(), 1, EffectTarget.PlayerRef(Player.EachOpponent))
        val x = PathwayExtractor().extract(listOf(fixture(sacrifice)))
        val row = deathResponses(x.eventOutcomes).single()
        row.verdict shouldBe Verdict.MATCH
        val o = row.outcome
        o.playerBinding?.description shouldBe "each opponent"
        val fact = x.facts.single { it.id == o.factId }
        fact.objectFilter shouldBe sacrifice.filter
        fact.sourcePlayer shouldBe "OPPONENT"
        fact.path shouldBe o.path
        fact.actionId shouldBe o.actionId
        fact.operation shouldBe "SACRIFICE"
        x.facts.any { it.kind == FactKind.SACRIFICE && it.actionId != o.actionId && it.costOrEffect.endsWith("COST") } shouldBe true
        o.prerequisites.any { it.contains("later death") } shouldBe true
        rows(sacrifice, GameObjectFilter.Creature.youControl().copy(cardPredicates = listOf(CardPredicate.IsCreature, CardPredicate.IsToken)))
            .single().verdict shouldBe Verdict.NO_MATCH
        rows(sacrifice, GameObjectFilter.Creature.copy(controllerPredicate = ControllerPredicate.ControlledByOpponent))
            .single().verdict shouldBe Verdict.NO_MATCH
    }

    test("one supported player iteration retains the rebound controller and answers an entry query") {
        val effect = Effects.ForEachPlayer(Player.Each, effect = ForceSacrificeEffect(GameObjectFilter.Creature, 1, EffectTarget.Controller))
        val x = PathwayExtractor().extract(listOf(fixture(effect, entry = true)))
        val o = x.eventOutcomes.single()
        o.playerBinding shouldBe EventPlayerBinding(EffectTarget.Controller, Player.Each, "each player")
        x.facts.single { it.id == o.factId }.sourcePlayer shouldBe "ANY"
        entry(o) shouldBe Verdict.MATCH
        deathResponses(listOf(o)).single().verdict shouldBe Verdict.NO_MATCH
    }

    test("controller life loss, optional choices, inert amounts and conditional branches remain distinct") {
        val loss = LoseLifeEffect(2, EffectTarget.Controller)
        rows(loss).single().verdict shouldBe Verdict.MATCH
        val optional = rows(Effects.May(loss)).single()
        optional.outcome.optional shouldBe true
        optional.verdict shouldBe Verdict.MATCH
        rows(LoseLifeEffect(0, EffectTarget.Controller)).single().verdict shouldBe Verdict.NO_MATCH
        rows(LoseLifeEffect(DynamicAmounts.lifeTotal(Player.You), EffectTarget.Controller)).single().verdict shouldBe Verdict.UNKNOWN
        val gated = rows(Effects.If(Conditions.CreatureDiedThisTurn, loss, GainLifeEffect(2)))
        gated.map { it.verdict } shouldBe listOf(Verdict.UNKNOWN, Verdict.UNKNOWN)
        gated.map { it.outcome.path.substringAfterLast('.') } shouldBe listOf("then", "otherwise")
        val entered = PathwayExtractor().extract(listOf(fixture(loss, entry = true))).eventOutcomes.single()
        entry(entered) shouldBe Verdict.MATCH
        deathResponses(listOf(entered)).single().verdict shouldBe Verdict.NO_MATCH
        val unknown = rows(loss).single().outcome
        deathResponses(listOf(unknown.copy(event = unknown.event.copy(trigger = EventPattern.CreaturesYouControlDiedEvent()))))
            .single().verdict shouldBe Verdict.UNKNOWN
    }

    test("the live catalog distinguishes forced sacrifice, draw, damage, loss and gain") {
        val expected = mapOf(
            "Zulaport Cutthroat" to setOf(OutcomeKind.LOSE_LIFE, OutcomeKind.GAIN_LIFE),
            "Deathcurse Ogre" to setOf(OutcomeKind.LOSE_LIFE),
            "Midnight Reaper" to setOf(OutcomeKind.DEAL_PLAYER_DAMAGE, OutcomeKind.DRAW_CARDS),
            "Pyretic Prankster" to setOf(OutcomeKind.FORCED_SACRIFICE)
        )
        for ((name, kinds) in expected) {
            val rows = report.results.filter { it.card == name && it.branch == Contribution.DEATH_TRIGGERED_OUTCOME && it.verdict == Verdict.MATCH }
            rows.map { it.eventOutcome!!.kind }.toSet() shouldBe kinds
            rows.map { it.actionId }.distinct().size shouldBe 1
        }
        val back = extract("Pyretic Prankster").eventOutcomes.single()
        back.face shouldBe "front.backFace"
        back.event.binding shouldBe TriggerBinding.SELF
        extract("Viscera Seer").eventOutcomes.isEmpty() shouldBe true
        catalog.card("Grave Pact") shouldBe null
    }

    test("native entry sacrifice and an unresolved death quantity retain separate evidence") {
        val o = extract("Fleshbag Marauder").eventOutcomes.single()
        o.kind shouldBe OutcomeKind.FORCED_SACRIFICE
        o.playerBinding?.description shouldBe "each player"
        entry(o) shouldBe Verdict.MATCH
        deathResponses(listOf(o)).single().verdict shouldBe Verdict.NO_MATCH
        val xander = extract("Lord Xander, the Collector").eventOutcomes.single()
        xander.playerBinding?.description shouldBe "the targeted opponent"
        deathResponses(listOf(xander)).single().verdict shouldBe Verdict.UNKNOWN
        val serpent = extract("The Serpent Society").eventOutcomes.single()
        serpent.playerBinding?.description shouldBe "each opponent"
        (serpent.event.trigger as EventPattern.ZoneChangeEvent).filter.cardPredicates
            .contains(CardPredicate.HasKeyword(Keyword.DEATHTOUCH)) shouldBe true
        deathResponses(listOf(serpent)).single().verdict shouldBe Verdict.UNKNOWN
    }
})
