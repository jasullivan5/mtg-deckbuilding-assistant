package dev.mtg.assistant

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.predicates.*
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class Phase21MechanicsTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun evaluate(c: CardDefinition)=creatureDeathPathways(PathwayExtractor().extract(listOf(c)).pathways)
        .where(canAffectCreature()).where(canAffectController(Controller.YOU)).where(canAffectTokenStatus(TokenStatus.NONTOKEN)).where(satisfiesDeathGenerationPolicy()).execute()
    test("mandatory sacrifice remains mechanically possible and acceptance is a separate configurable policy") {
        for(name in listOf("Arc Runner","Accursed Centaur","Demonic Taskmaster")) {
            val p=evaluate(catalog.card(name)!!).single()
            p.decisions.take(3).all { it.verdict == Verdict.MATCH } shouldBe true
            p.decisions.last().basis shouldBe "POLICY"
            p.decisions.last().verdict shouldBe Verdict.MATCH
            satisfiesDeathGenerationPolicy(DeathGenerationPolicy(includeMandatorySacrifice=false)).assess(p.pathway,Requirements()).verdict shouldBe Verdict.NO_MATCH
            p.pathway.relationship shouldBe "MANDATORY_OR_INCIDENTAL_TRIGGER"
        }
    }
    test("real self-only toughness exchanges are potential generators with action constraints preserved") {
        for(name in listOf("Carrion Howler","Flowstone Wall")) {
            val p=evaluate(catalog.card(name)!!).single()
            p.verdict shouldBe Verdict.MATCH
            p.pathway.operation shouldBe "TOUGHNESS_REDUCTION"
            p.pathway.relationship shouldBe "SELECTABLE_SELF_TOUGHNESS_EXCHANGE"
            p.pathway.prerequisites.any { it.contains("Toughness must reach zero") } shouldBe true
        }
        evaluate(catalog.card("Murder")!!).single().decisions.last().verdict shouldBe Verdict.NO_MATCH
        evaluate(catalog.card("Blind Creeper")!!).any { it.verdict == Verdict.MATCH } shouldBe true
    }
    test("sacrifice enables Victimize and exploit while ordinary symmetry stays excluded") {
        evaluate(catalog.card("Victimize")!!).single().verdict shouldBe Verdict.MATCH
        evaluate(catalog.card("Fell Stinger")!!).any { it.verdict == Verdict.MATCH && it.pathway.relationship == "REFLEXIVE_ACTION_ENABLES_OUTCOME" } shouldBe true
        evaluate(catalog.card("Fleshbag Marauder")!!).none { it.verdict == Verdict.MATCH } shouldBe true
    }
    test("actual artifact-or-creature sacrifice costs recover the exact payable pathways") {
        for((name,role) in listOf("Deadly Dispute" to "CASTING_COST","Acolyte of Aclazotz" to "ACTIVATION_COST")) {
            val p=evaluate(catalog.card(name)!!).single()
            p.verdict shouldBe Verdict.MATCH
            p.pathway.origin shouldBe role
            p.pathway.operation shouldBe "SACRIFICE"
            p.pathway.scope.filter.cardPredicates.any { it is CardPredicate.Or } shouldBe true
        }
    }
    test("Wicked Guardian selection is resolved locally but damage enabling draw is not death enabling removal") {
        val p=evaluate(catalog.card("Wicked Guardian")!!).single()
        p.decisions.take(3).all { it.verdict == Verdict.MATCH } shouldBe true
        p.pathway.path.endsWith("gate.action.effects[2].body") shouldBe true
        p.pathway.operation shouldBe "DAMAGE_POTENTIALLY_LETHAL"
        p.decisions.last().verdict shouldBe Verdict.NO_MATCH
    }
    // Synthetic native pipeline fixtures isolate provenance; they do not establish authored coverage.
    fun pipeline(player: Player, filter: GameObjectFilter, count: Int = 1, zone: Zone = Zone.BATTLEFIELD): CardDefinition = card("Synthetic selection") {
        typeLine="Sorcery"; manaCost="{B}"
        spell { effect=CompositeEffect(listOf(
            GatherCardsEffect(CardSource.FromZone(zone,player,GameObjectFilter.Creature),"g"),
            SelectFromCollectionEffect("g",SelectionMode.ChooseExactly(DynamicAmount.Fixed(count)),filter=filter,storeSelected="s"),
            MoveCollectionEffect("s",CardDestination.ToZone(Zone.GRAVEYARD),moveType=MoveType.Sacrifice))) }
    }
    test("selection propagates our nontoken scope and movement into the same action") {
        val p=evaluate(pipeline(Player.You,GameObjectFilter.Creature.nontoken())).single()
        p.decisions.take(3).all { it.verdict == Verdict.MATCH } shouldBe true
        p.pathway.scope.unresolved shouldBe null
        // A general non-removal sacrifice does not need a payoff; structural constraints remain.
        p.verdict shouldBe Verdict.MATCH
    }
    test("selection excludes opponent-only token-only zero-count and nonbattlefield deaths") {
        evaluate(pipeline(Player.EachOpponent,GameObjectFilter.Creature)).single().decisions[1].verdict shouldBe Verdict.NO_MATCH
        evaluate(pipeline(Player.You,GameObjectFilter(cardPredicates=listOf(CardPredicate.IsCreature,CardPredicate.IsToken)))).single().decisions[2].verdict shouldBe Verdict.NO_MATCH
        evaluate(pipeline(Player.You,GameObjectFilter.Creature,0)).single().decisions[0].verdict shouldBe Verdict.NO_MATCH
        evaluate(pipeline(Player.You,GameObjectFilter.Creature,zone=Zone.GRAVEYARD)).single().decisions[0].verdict shouldBe Verdict.NO_MATCH
    }
    test("nested logical card predicates preserve supported positive and negative compatibility") {
        val simple=ObjectScope(GameObjectFilter.Creature.nontoken(),"YOU")
        val compound=ObjectScope(GameObjectFilter(cardPredicates=listOf(CardPredicate.And(listOf(CardPredicate.IsCreature,CardPredicate.Or(listOf(CardPredicate.IsNontoken,CardPredicate.IsNoncreature)))))),"YOU")
        val req=Requirements(controller=Controller.YOU,token=TokenStatus.NONTOKEN)
        compound.compatible(req) shouldBe simple.compatible(req)
        val tokenOnly=ObjectScope(GameObjectFilter(cardPredicates=listOf(CardPredicate.And(listOf(CardPredicate.IsCreature,CardPredicate.IsToken)))))
        tokenOnly.compatible(req) shouldBe Verdict.NO_MATCH
    }
    test("unresolved selection restrictions remain visible rather than becoming broad matches") {
        val c=card("Synthetic restricted selection") { typeLine="Sorcery"; manaCost="{B}"; spell {
            effect=CompositeEffect(listOf(GatherCardsEffect(CardSource.ControlledPermanents(Player.You,GameObjectFilter.Creature),"g"),
                SelectFromCollectionEffect("g",SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),storeSelected="s",restrictions=listOf(SelectionRestriction.TotalPowerAtMost(2))),
                MoveCollectionEffect("s",CardDestination.ToZone(Zone.GRAVEYARD),moveType=MoveType.Sacrifice)))
        } }
        val e=PathwayExtractor().extract(listOf(c));e.unsupported.any { it.reason.contains("restrictions/count") } shouldBe true
        evaluate(c).single().decisions[2].verdict shouldBe Verdict.UNKNOWN
    }
    test("query predicate ordering gives the same final conjunction") {
        val paths=PathwayExtractor().extract(listOf(pipeline(Player.You,GameObjectFilter.Creature.nontoken()))).pathways
        val a=creatureDeathPathways(paths).where(canAffectController(Controller.YOU)).where(canAffectTokenStatus(TokenStatus.NONTOKEN)).execute().single()
        val b=creatureDeathPathways(paths).where(canAffectTokenStatus(TokenStatus.NONTOKEN)).where(canAffectController(Controller.YOU)).execute().single()
        a.verdict shouldBe b.verdict
    }
})
