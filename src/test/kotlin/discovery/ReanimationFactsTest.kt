package dev.mtg.assistant

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.*
import com.wingedsheep.sdk.scripting.references.Player
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Synthetic native fixtures isolate structural boundaries; authored integration is separate. */
class ReanimationFactsTest : FunSpec({
    fun fixture(filter: TargetFilter=TargetFilter.CreatureInYourGraveyard,to: Zone=Zone.BATTLEFIELD,controller: EffectTarget?=null,from: Zone?=Zone.GRAVEYARD)=card("Synthetic movement") {
        typeLine="Sorcery";spell { val t=target(filter);effect=MoveToZoneEffect(t,to,controllerOverride=controller,fromZone=from) }
    }
    fun result(c: com.wingedsheep.sdk.model.CardDefinition)=reanimationFacts(PathwayExtractor().extract(listOf(c)).facts).execute().single()
    test("native owner-default return from our graveyard qualifies") { result(fixture()).verdict shouldBe Verdict.MATCH }
    test("any graveyard plus our entry control can choose our card") { result(fixture(TargetFilter.CreatureInGraveyard,controller=EffectTarget.Controller)).verdict shouldBe Verdict.MATCH }
    test("opponent-owned cards cannot satisfy own graveyard even under our control") {
        result(fixture(TargetFilter(GameObjectFilter.Creature.ownedByOpponent(),zone=Zone.GRAVEYARD),controller=EffectTarget.Controller)).verdict shouldBe Verdict.NO_MATCH
    }
    test("entry to an opponent is excluded independently of ownership") {
        result(fixture(controller=EffectTarget.PlayerRef(Player.AnOpponent))).verdict shouldBe Verdict.NO_MATCH
    }
    test("graveyard to hand is a reusable fact and a reanimation nonmatch") {
        val r=result(fixture(to=Zone.HAND));r.fact.sourceZone shouldBe Zone.GRAVEYARD;r.fact.destinationZone shouldBe Zone.HAND;r.verdict shouldBe Verdict.NO_MATCH
        FactQuery(listOf(r.fact)).where(sourceZoneIs(Zone.GRAVEYARD)).where(destinationZoneIs(Zone.HAND)).execute().single().verdict shouldBe Verdict.MATCH
    }
    test("exile to battlefield is not direct graveyard movement") {
        result(fixture(TargetFilter(GameObjectFilter.Creature.ownedByYou(),zone=Zone.EXILE),from=Zone.EXILE)).verdict shouldBe Verdict.NO_MATCH
    }
    test("noncreature and token-only constraints exclude desired card") {
        result(fixture(TargetFilter(GameObjectFilter.Any.copy(cardPredicates=listOf(CardPredicate.IsNoncreature)),zone=Zone.GRAVEYARD))).verdict shouldBe Verdict.NO_MATCH
        result(fixture(TargetFilter(GameObjectFilter.Creature.token(),zone=Zone.GRAVEYARD))).verdict shouldBe Verdict.NO_MATCH
    }
    test("correlated owner and creature branches cannot be independently combined") {
        val f=GameObjectFilter.Any.copy(anyOf=listOf(GameObjectFilter.Any.copy(cardPredicates=listOf(CardPredicate.IsNoncreature)).ownedByYou(),GameObjectFilter.Creature.ownedByOpponent()))
        result(fixture(TargetFilter(f,zone=Zone.GRAVEYARD),controller=EffectTarget.Controller)).verdict shouldBe Verdict.NO_MATCH
    }
    test("negation and contradictory ownership use one owner assignment") {
        val f=GameObjectFilter.Creature.copy(controllerPredicate=ControllerPredicate.And(listOf(ControllerPredicate.OwnedByYou,ControllerPredicate.Not(ControllerPredicate.OwnedByYou))))
        result(fixture(TargetFilter(f,zone=Zone.GRAVEYARD))).verdict shouldBe Verdict.NO_MATCH
    }
    test("mixed zone target union preserves ownership and type per clause") {
        val f=TargetFilter(GameObjectFilter.Creature.ownedByOpponent(),zone=Zone.GRAVEYARD).or(TargetFilter(GameObjectFilter.Creature.ownedByYou(),zone=Zone.EXILE))
        val e=PathwayExtractor().extract(listOf(fixture(f,from=null)))
        val rs=reanimationFacts(e.facts).execute();rs.size shouldBe 2;rs.all { it.verdict==Verdict.NO_MATCH } shouldBe true
    }
    test("unknown property predicate remains unknown rather than guessed feasible") {
        result(fixture(TargetFilter(GameObjectFilter.Creature.ownedByYou().manaValueAtMost(3),zone=Zone.GRAVEYARD))).verdict shouldBe Verdict.UNKNOWN
    }
    test("selection without movement and token creation cannot reanimate") {
        val c=card("Synthetic reveal") { typeLine="Sorcery";spell { effect=Effects.DrawCards(1) } }
        reanimationFacts(PathwayExtractor().extract(listOf(c)).facts).execute().isEmpty() shouldBe true
    }
    test("collection source ownership and current zone survive filter selection and movement") {
        fun c(player: Player,to: Zone)=card("Synthetic pipeline") { typeLine="Sorcery";spell { effect=Effects.Pipeline {
            val found=gather(CardSource.FromZone(Zone.GRAVEYARD,player,GameObjectFilter.Creature))
            move(found,CardDestination.ToZone(to,Player.You))
        } } }
        result(c(Player.You,Zone.BATTLEFIELD)).verdict shouldBe Verdict.MATCH
        result(c(Player.EachOpponent,Zone.BATTLEFIELD)).verdict shouldBe Verdict.NO_MATCH
        result(c(Player.You,Zone.HAND)).verdict shouldBe Verdict.NO_MATCH
    }
    test("intermediate exile is kept as a separate current-zone fact") {
        val c=card("Synthetic tracked exile") { typeLine="Sorcery";spell { effect=Effects.Pipeline {
            val found=gather(CardSource.FromZone(Zone.GRAVEYARD,Player.You,GameObjectFilter.Creature))
            val exiled=moveTracked(found,CardDestination.ToZone(Zone.EXILE))
            move(exiled,CardDestination.ToZone(Zone.BATTLEFIELD),underOwnersControl=true)
        } } }
        val rs=reanimationFacts(PathwayExtractor().extract(listOf(c)).facts).execute()
        rs.map { it.fact.sourceZone } shouldBe listOf(Zone.GRAVEYARD,Zone.EXILE)
        rs.all { it.verdict==Verdict.NO_MATCH } shouldBe true
    }
    test("one action exposes sacrifice and movement without a second effect traversal") {
        val c=card("Synthetic shared action") { typeLine="Sorcery";additionalCost(com.wingedsheep.sdk.scripting.AdditionalCost.Atom(com.wingedsheep.sdk.scripting.costs.CostAtom.Sacrifice(GameObjectFilter.Creature)))
            spell { val t=target(TargetFilter.CreatureInYourGraveyard);effect=Effects.PutOntoBattlefieldFromGraveyard(t) } }
        val e=PathwayExtractor().extract(listOf(c));val death=creatureDeathPathways(e.pathways).where(canAffectCreature()).where(canAffectController(Controller.YOU)).where(canAffectTokenStatus(TokenStatus.NONTOKEN)).where(satisfiesDeathGenerationPolicy()).execute().single()
        val move=reanimationFacts(e.facts).execute().single()
        death.verdict shouldBe Verdict.MATCH;move.verdict shouldBe Verdict.MATCH
        death.pathway.actionId shouldBe move.fact.actionId
        e.facts.single { it.id==death.pathway.factId }.kind shouldBe FactKind.SACRIFICE
        FactQuery(e.facts).where(operationIs(FactKind.SACRIFICE)).where(objectIsCreatureCard()).where(sourceZoneIs(Zone.BATTLEFIELD)).where(sourceControlledBy(Controller.YOU)).execute().single { it.fact.kind==FactKind.SACRIFICE }.verdict shouldBe Verdict.MATCH
        e.facts.size shouldBe 2
    }
    test("prior movement updates target zone rather than reusing original target legality") {
        val c=card("Synthetic sequential target") { typeLine="Sorcery";spell {
            val t=target(TargetFilter.CreatureInYourGraveyard)
            effect=Effects.Move(t,Zone.HAND,fromZone=Zone.GRAVEYARD) then Effects.Move(t,Zone.BATTLEFIELD)
        } }
        val rs=reanimationFacts(PathwayExtractor().extract(listOf(c)).facts).execute()
        rs.map { it.fact.sourceZone } shouldBe listOf(Zone.GRAVEYARD,Zone.HAND)
        rs.none { it.verdict==Verdict.MATCH } shouldBe true
    }
    test("unresolved conditional branch does not become an unconditional reanimation") {
        val c=card("Synthetic conditional return") { typeLine="Sorcery";spell {
            val t=target(TargetFilter.CreatureInYourGraveyard)
            effect=GatedEffect(Gate.WhenCondition(com.wingedsheep.sdk.scripting.conditions.CreatureDiedThisTurnCondition),Effects.PutOntoBattlefieldFromGraveyard(t))
        } }
        val r=result(c);r.verdict shouldBe Verdict.UNKNOWN
        r.fact.prerequisites.any { it.contains("Gate=") } shouldBe true
    }
    test("unbound pipeline target never supplies a known own graveyard creature") {
        val c=card("Synthetic unbound") { typeLine="Sorcery";spell { effect=MoveToZoneEffect(EffectTarget.PipelineTarget("missing",0),Zone.BATTLEFIELD,fromZone=Zone.GRAVEYARD) } }
        result(c).verdict shouldBe Verdict.UNKNOWN
    }
    test("zero collection selection cannot produce a return") {
        val c=card("Synthetic zero selection") { typeLine="Sorcery";spell { effect=CompositeEffect(listOf(
            GatherCardsEffect(CardSource.FromZone(Zone.GRAVEYARD,Player.You,GameObjectFilter.Creature),"pool"),
            SelectFromCollectionEffect(from="pool",selection=SelectionMode.ChooseExactly(com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed(0)),storeSelected="picked"),
            MoveCollectionEffect("picked",CardDestination.ToZone(Zone.BATTLEFIELD))
        )) } }
        result(c).verdict shouldBe Verdict.NO_MATCH
    }
})
