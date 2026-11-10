package dev.mtg.assistant

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SelfReplenishmentExtractionTest : FunSpec({
    fun fixture(e:Effect=Effects.Move(EffectTarget.Self,Zone.BATTLEFIELD,fromZone=Zone.GRAVEYARD),
                creature:Boolean=true,zone:Zone=Zone.GRAVEYARD,cost:AbilityCost=Costs.Mana("{1}{B}"),once:Boolean=false)=card("Synthetic original source") {
        typeLine=if(creature) "Creature — Skeleton" else "Enchantment"
        if(creature){power=1;toughness=1}
        activatedAbility { this.cost=cost;effect=e;activateFromZone=zone;if(once) restrictions=listOf(ActivationRestriction.Once) }
    }
    fun evaluate(c:com.wingedsheep.sdk.model.CardDefinition):List<EvaluatedSelfReturn> {
        val x=PathwayExtractor().extract(listOf(c));return selfReplenishment(x.selfReturns,x.facts)
    }
    val move=Effects.Move(EffectTarget.Self,Zone.BATTLEFIELD,fromZone=Zone.GRAVEYARD)
    val finality=Effects.AddCounters(CounterType.FINALITY,1,EffectTarget.Self)
    test("self-bound original creature retains fact action face source zone and payment") {
        val e=evaluate(fixture()).single();e.verdict shouldBe Verdict.MATCH
        e.link.factId shouldBe e.fact.id;e.link.actionId shouldBe e.fact.actionId;e.link.face shouldBe e.fact.face
        e.fact.sourceZone shouldBe Zone.GRAVEYARD;e.fact.destinationZone shouldBe Zone.BATTLEFIELD
        e.fact.movesOriginalCard shouldBe true;e.link.context.activationCost shouldBe "{1}{B}"
        e.link.context.activeZones shouldBe setOf(Zone.GRAVEYARD)
    }
    test("source card must be an original creature and ownership remains correlated") {
        evaluate(fixture(creature=false)).single().verdict shouldBe Verdict.NO_MATCH
        val e=evaluate(fixture()).single()
        selfReplenishment(listOf(e.link),listOf(e.fact.copy(ownerScope="OPPONENT"))).single().verdict shouldBe Verdict.NO_MATCH
        selfReplenishment(listOf(e.link),listOf(e.fact.copy(movesOriginalCard=false))).single().verdict shouldBe Verdict.NO_MATCH
    }
    test("opponent controller and return to hand fail this criterion") {
        evaluate(fixture(e=Effects.Move(EffectTarget.Self,Zone.BATTLEFIELD,fromZone=Zone.GRAVEYARD,controllerOverride=EffectTarget.PlayerRef(Player.AnOpponent)))).single().verdict shouldBe Verdict.NO_MATCH
        evaluate(fixture(e=Effects.Move(EffectTarget.Self,Zone.HAND,fromZone=Zone.GRAVEYARD))).single().verdict shouldBe Verdict.NO_MATCH
    }
    test("ordinary casting from hand and a contradictory source zone cannot supply a graveyard return") {
        evaluate(fixture(zone=Zone.HAND)).single().verdict shouldBe Verdict.NO_MATCH
        evaluate(fixture(e=Effects.Move(EffectTarget.Self,Zone.BATTLEFIELD,fromZone=Zone.HAND),zone=Zone.HAND)).single().verdict shouldBe Verdict.NO_MATCH
    }
    test("exiling the original card to pay its activation cannot be ignored") {
        val e=evaluate(fixture(cost=AbilityCost.ExileSelf)).single()
        e.link.context.sourceCostCompatibility shouldBe Verdict.NO_MATCH;e.verdict shouldBe Verdict.NO_MATCH
    }
    test("a graveyard trigger and its optional output keep separate trigger and source bindings") {
        val c=card("Synthetic graveyard observer") {
            typeLine="Creature — Spirit";power=1;toughness=1
            triggeredAbility { trigger=Triggers.a(GameObjectFilter.Land.youControl()).enters();triggerZone=Zone.GRAVEYARD;effect=Effects.May(move) }
        }
        val e=evaluate(c).single();e.verdict shouldBe Verdict.MATCH;e.link.optional shouldBe true
        e.link.context.kind shouldBe "TRIGGERED";e.link.context.activeZones shouldBe setOf(Zone.GRAVEYARD)
    }
    test("an explicit self-death transition can bind a battlefield-active source to its graveyard card") {
        val c=card("Synthetic dies and returns") {
            typeLine="Creature — Spirit";power=1;toughness=1
            triggeredAbility { trigger=Triggers.self.dies();effect=move }
        }
        val e=evaluate(c).single();e.link.context.selfDeathTransition shouldBe true;e.verdict shouldBe Verdict.MATCH
    }
    test("an unrelated battlefield entry ability cannot make a generic target into its own source") {
        val c=card("Synthetic generic reanimating creature") {
            typeLine="Creature — Spirit";power=1;toughness=1
            triggeredAbility { trigger=Triggers.self.enters();effect=move }
        }
        evaluate(c).single().verdict shouldBe Verdict.NO_MATCH
    }
    test("unresolved variable binding remains unknown") {
        val e=evaluate(fixture()).single()
        selfReplenishment(listOf(e.link.copy(binding=Verdict.UNKNOWN)),listOf(e.fact)).single().verdict shouldBe Verdict.UNKNOWN
    }
    test("ambiguous graveyard-capable active zones retain uncertainty instead of a legacy fallback rejection") {
        val c=card("Synthetic multiple active zones") {
            typeLine="Creature — Spirit";power=1;toughness=1
            triggeredAbility { trigger=Triggers.a(GameObjectFilter.Land.youControl()).enters();triggerZone=Zone.GRAVEYARD;effect=move }
        }
        val native=c.copy(script=c.script.copy(triggeredAbilities=c.script.triggeredAbilities.map { it.copy(activeZones=setOf(Zone.GRAVEYARD,Zone.EXILE)) }))
        evaluate(native).single().verdict shouldBe Verdict.UNKNOWN
    }
    test("finality in the return sequence is a death barrier not an indefinitely reusable input") {
        val e=evaluate(fixture(e=CompositeEffect(listOf(move,finality)))).single()
        e.link.limits.single().kind shouldBe "DEATH_REPLACEMENT";e.verdict shouldBe Verdict.UNKNOWN
    }
    test("finality entry counters are preserved as barriers") {
        val e=evaluate(fixture(e=MoveToZoneEffect(EffectTarget.Self,Zone.BATTLEFIELD,fromZone=Zone.GRAVEYARD,entryCounters=listOf(EntryCounter(CounterType.FINALITY))))).single()
        e.verdict shouldBe Verdict.UNKNOWN;e.link.limits.single().kind shouldBe "DEATH_REPLACEMENT"
    }
    test("finality in an unchosen alternative is not assigned to the return") {
        val e=evaluate(fixture(e=GatedEffect(Gate.MayDecide(),move,finality))).single()
        e.verdict shouldBe Verdict.MATCH;e.link.limits.isEmpty() shouldBe true
    }
    test("another ability cannot lend the return an outcome or limitation") {
        val c=card("Synthetic separate abilities") {
            typeLine="Creature — Spirit";power=1;toughness=1
            activatedAbility { cost=Costs.Mana("{1}");effect=move;activateFromZone=Zone.GRAVEYARD }
            activatedAbility { cost=Costs.Mana("{1}");effect=finality }
        }
        val e=evaluate(c).single();e.verdict shouldBe Verdict.MATCH;e.link.limits.isEmpty() shouldBe true
    }
    test("a one-use restriction is retained without claiming the route can be used indefinitely") {
        val e=evaluate(fixture(once=true)).single();e.verdict shouldBe Verdict.MATCH
        e.link.context.limits.single().kind shouldBe "ACTIVATION_RESTRICTION"
    }
    test("modal and unsupported condition branches stay unknown") {
        val e=GatedEffect(Gate.WhenCondition(com.wingedsheep.sdk.scripting.conditions.CreatureDiedThisTurnCondition),move)
        evaluate(fixture(e=e)).single().verdict shouldBe Verdict.UNKNOWN
    }
    test("a separate sacrifice ability and self return are distinct potential contributions, not cycle evidence") {
        val c=card("Synthetic overlap") {
            typeLine="Creature — Spirit";power=1;toughness=1
            activatedAbility { cost=Costs.Sacrifice(GameObjectFilter.Creature);effect=Effects.DrawCards(1) }
            activatedAbility { cost=Costs.Mana("{1}");effect=move;activateFromZone=Zone.GRAVEYARD }
        }
        val x=PathwayExtractor().extract(listOf(c));val e=selfReplenishment(x.selfReturns,x.facts).single()
        e.verdict shouldBe Verdict.MATCH
        evaluateDeathGeneration(x.pathways).any { it.verdict==Verdict.MATCH && it.pathway.actionId!=e.link.actionId } shouldBe true
    }
    test("source-consuming sacrifice before a self return is unresolved instead of an invented link") {
        evaluate(fixture(zone=Zone.BATTLEFIELD,cost=AbilityCost.SacrificeSelf)).single().verdict shouldBe Verdict.UNKNOWN
    }
    test("an actual unbound native target cannot become an original-source match") {
        evaluate(fixture(e=Effects.Move(EffectTarget.BoundVariable("unresolved"),Zone.BATTLEFIELD,fromZone=Zone.GRAVEYARD))).single().verdict shouldBe Verdict.UNKNOWN
    }
    test("an ambiguous physical layout does not borrow the source face's noncreature negative") {
        evaluate(fixture(creature=false).copy(layout=com.wingedsheep.sdk.model.CardLayout.SPLIT)).single().verdict shouldBe Verdict.UNKNOWN
    }

    test("trigger allowance and actor rebinding remain separate source restrictions") {
        val c=card("Synthetic limited graveyard source") {
            typeLine="Creature — Spirit";power=1;toughness=1
            triggeredAbility { trigger=Triggers.you.drawsNth(2);triggerZone=Zone.GRAVEYARD;oncePerTurn=true;effect=move }
        }
        val limited=evaluate(c).single();limited.verdict shouldBe Verdict.MATCH
        limited.link.context.limits.single().kind shouldBe "TRIGGER_LIMIT"
        val rebound=c.copy(script=c.script.copy(triggeredAbilities=c.script.triggeredAbilities.map { it.copy(controlledByTriggeringEntityController=true) }))
        evaluate(rebound).single().verdict shouldBe Verdict.UNKNOWN
    }

    test("a graveyard card cannot tap itself to pay for its return") {
        evaluate(fixture(cost=AbilityCost.Tap)).single().verdict shouldBe Verdict.NO_MATCH
    }
    test("a counter placed before a zone change is not treated as an entry counter") {
        evaluate(fixture(e=CompositeEffect(listOf(finality,move)))).single().verdict shouldBe Verdict.MATCH
    }

    test("a decision gate's required source zone cannot be loosened for a return") {
        evaluate(fixture(e=GatedEffect(Gate.MayDecide(sourceRequiredZone=Zone.EXILE),move))).single().verdict shouldBe Verdict.NO_MATCH
    }
    test("delegated payment or optionality is retained as unresolved actor interpretation") {
        evaluate(fixture(e=GatedEffect(Gate.MayDecide(),move,decisionMaker=EffectTarget.PlayerRef(Player.AnOpponent)))).single().verdict shouldBe Verdict.UNKNOWN
    }

})
