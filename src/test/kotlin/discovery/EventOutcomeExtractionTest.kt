package dev.mtg.assistant

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Synthetic native adversaries exercise relationships, not new production card definitions. */
class EventOutcomeExtractionTest : FunSpec({
    fun fixture(filter: GameObjectFilter=GameObjectFilter.Creature.youControl(), e: Effect=DrawCardsEffect(1),
                entry: Boolean=false, self: Boolean=false, creature: Boolean=true, graveyard: Boolean=false, conditional: Boolean=false)=card("Synthetic event relation") {
        typeLine=if(creature) "Creature — Human" else "Enchantment"
        if(creature){power=2;toughness=2}
        triggeredAbility {
            trigger=if(self) Triggers.self.dies() else if(entry) Triggers.another(filter).enters() else Triggers.another(filter).dies()
            if(graveyard) triggerZone=Zone.GRAVEYARD
            if(conditional) interveningIf=Conditions.YouControl(GameObjectFilter.Creature.powerAtLeast(4))
            effect=e
        }
    }
    fun rows(filter:GameObjectFilter=GameObjectFilter.Creature.youControl(),e:Effect=DrawCardsEffect(1))=
        deathResponses(PathwayExtractor().extract(listOf(fixture(filter,e))).eventOutcomes)
    test("draw keeps its exact trigger action path amount receiver and source zone") {
        val x=PathwayExtractor().extract(listOf(fixture())); val o=x.eventOutcomes.single()
        o.kind shouldBe OutcomeKind.DRAW_CARDS;o.recipient shouldBe "YOU";o.event.binding shouldBe TriggerBinding.OTHER
        o.event.activeZones shouldBe setOf(Zone.BATTLEFIELD);o.optional shouldBe false
        x.actions.single { it.id==o.actionId }.path shouldBe "script.triggeredAbilities[0]"
        o.path shouldBe "script.triggeredAbilities[0].effect"
        deathResponses(x.eventOutcomes).single().verdict shouldBe Verdict.MATCH
    }
    test("a token-inclusive trigger intersects a nontoken objective") { rows().single().verdict shouldBe Verdict.MATCH }
    test("token-only deaths do not intersect nontoken deaths") {
        rows(GameObjectFilter.Creature.youControl().copy(cardPredicates=listOf(CardPredicate.IsCreature,CardPredicate.IsToken))).single().verdict shouldBe Verdict.NO_MATCH
    }
    test("opponent-controlled death is not our death") {
        rows(GameObjectFilter.Creature.copy(controllerPredicate=ControllerPredicate.ControlledByOpponent)).single().verdict shouldBe Verdict.NO_MATCH
    }
    test("owned by an opponent can still be controlled by us") {
        rows(GameObjectFilter.Creature.copy(controllerPredicate=ControllerPredicate.OwnedByOpponent)).single().verdict shouldBe Verdict.MATCH
    }
    test("OR alternatives cannot combine our noncreature with an opponent's creature") {
        val filter=GameObjectFilter.Any.copy(anyOf=listOf(
            GameObjectFilter.Any.copy(cardPredicates=listOf(CardPredicate.IsNoncreature),controllerPredicate=ControllerPredicate.ControlledByYou),
            GameObjectFilter.Creature.copy(controllerPredicate=ControllerPredicate.ControlledByOpponent)))
        rows(filter).single().verdict shouldBe Verdict.NO_MATCH
    }
    test("SELF death keeps identity and does not require another object") {
        val o=PathwayExtractor().extract(listOf(fixture(self=true))).eventOutcomes.single()
        o.event.binding shouldBe TriggerBinding.SELF;deathResponses(listOf(o)).single().verdict shouldBe Verdict.MATCH
    }
    test("noncreature self needing animation remains unknown") {
        deathResponses(PathwayExtractor().extract(listOf(fixture(self=true,creature=false))).eventOutcomes).single().verdict shouldBe Verdict.UNKNOWN
    }
    test("source active-zone and intervening-condition limitations remain unknown") {
        for(c in listOf(fixture(graveyard=true),fixture(conditional=true)))
            deathResponses(PathwayExtractor().extract(listOf(c)).eventOutcomes).single().verdict shouldBe Verdict.UNKNOWN
    }
    test("shared interpretation supports a nondeath entry draw") {
        val o=PathwayExtractor().extract(listOf(fixture(entry=true))).eventOutcomes.single()
        o.confidence shouldBe Verdict.MATCH
        eventIntersection(o.event,ZoneEventRequirement(to=Zone.BATTLEFIELD,controller=Controller.YOU)) shouldBe Verdict.MATCH
        deathResponses(listOf(o)).single().verdict shouldBe Verdict.NO_MATCH
    }
    test("Treasure references the existing noncreature token fact without changing creature-token admission") {
        val x=PathwayExtractor().extract(listOf(fixture(e=CreatePredefinedTokenEffect("Treasure"))))
        val o=x.eventOutcomes.single();o.factId shouldBe x.facts.single().id
        o.token!!.creature shouldBe Verdict.NO_MATCH
        tokenCreationFacts(x.facts).execute().single().verdict shouldBe Verdict.NO_MATCH
        deathResponses(x.eventOutcomes).single().verdict shouldBe Verdict.MATCH
    }
    test("zero quantity and opposing draw receiver do not qualify") {
        rows(e=DrawCardsEffect(0)).single().verdict shouldBe Verdict.NO_MATCH
        rows(e=DrawCardsEffect(1,EffectTarget.PlayerRef(Player.AnOpponent))).single().verdict shouldBe Verdict.NO_MATCH
    }
    test("optional draw remains optional and condition gates keep both separate uncertain branches") {
        val may=PathwayExtractor().extract(listOf(fixture(e=Effects.May(Effects.DrawCards(1))))).eventOutcomes.single()
        may.optional shouldBe true;deathResponses(listOf(may)).single().verdict shouldBe Verdict.MATCH
        val condition=com.wingedsheep.sdk.scripting.conditions.CreatureDiedThisTurnCondition
        val x=PathwayExtractor().extract(listOf(fixture(e=GatedEffect(Gate.WhenCondition(condition),DrawCardsEffect(1),CreatePredefinedTokenEffect("Treasure")))))
        x.eventOutcomes.size shouldBe 2
        x.eventOutcomes.map { it.path }.distinct().size shouldBe 2
        deathResponses(x.eventOutcomes).map { it.verdict } shouldBe listOf(Verdict.UNKNOWN,Verdict.UNKNOWN)
    }
    test("same-card abilities never lend one another an event") {
        val c=card("Synthetic unrelated abilities") {
            typeLine="Creature — Human";power=1;toughness=1
            triggeredAbility { trigger=Triggers.self.enters();effect=CreateTokenEffect(DynamicAmount.Fixed(1),1,1,emptySet(),setOf("Human")) }
            triggeredAbility { trigger=Triggers.self.dies();effect=Effects.DrawCards(1) }
            activatedAbility { cost=Costs.Mana("{1}");effect=Effects.DrawCards(2) }
        }
        val x=PathwayExtractor().extract(listOf(c));x.eventOutcomes.size shouldBe 2
        x.eventOutcomes.map { it.actionId }.distinct().size shouldBe 2
        deathResponses(x.eventOutcomes).filter { it.verdict==Verdict.MATCH }.single().outcome.kind shouldBe OutcomeKind.DRAW_CARDS
    }
    test("batch deaths and mixed event alternatives remain inspectable without invented matches") {
        val original=PathwayExtractor().extract(listOf(fixture())).eventOutcomes.single()
        val batch=original.copy(event=original.event.copy(trigger=EventPattern.CreaturesYouControlDiedEvent()))
        deathResponses(listOf(batch)).single().verdict shouldBe Verdict.UNKNOWN
        val mixed=original.copy(event=original.event.copy(trigger=EventPattern.AnyOf(listOf(original.event.trigger, EventPattern.CreaturesYouControlDiedEvent()))))
        deathResponses(listOf(mixed)).single().verdict shouldBe Verdict.MATCH
    }
    test("accepted death production and death response keep separate ability identities") {
        val c=card("Synthetic overlapping contributions") {
            typeLine="Creature � Human";power=1;toughness=1
            activatedAbility { cost=Costs.Sacrifice(GameObjectFilter.Creature);effect=Effects.DrawCards(1) }
            triggeredAbility { trigger=Triggers.another(GameObjectFilter.Creature.youControl()).dies();effect=Effects.DrawCards(1) }
        }
        val x=PathwayExtractor().extract(listOf(c))
        val response=deathResponses(x.eventOutcomes).single()
        response.verdict shouldBe Verdict.MATCH
        evaluateDeathGeneration(x.pathways).any { it.verdict==Verdict.MATCH && it.pathway.actionId!=response.outcome.actionId } shouldBe true
    }

})
