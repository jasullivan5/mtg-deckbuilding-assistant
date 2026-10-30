package dev.mtg.assistant

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.tooling.*
import dev.mtg.assistant.extensions.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*

// Assertions take only a definition, never a mutation label or intended defect.
internal object ReliabilityChecks {
    private val kill=card("Reliability Kill") {manaCost="{0}";typeLine="Instant";spell {val t=target(TargetFilter.Creature);effect=Effects.Destroy(t)}}
    private val prey=card("Reliability Prey") {typeLine="Creature";power=3;toughness=7}
    private val lore=card("Lore Weaver") {typeLine="Creature — Human Wizard";power=2;toughness=2}
    fun driver(c: CardDefinition)=scenarioDriver(c,kill,prey,lore)
    fun urge(c: CardDefinition, removeHost: Boolean=false, removeAura: Boolean=false, removeTarget: Boolean=false, opponentHost: Boolean=false) {
        val d=driver(c);val me=d.player1;val owner=if(opponentHost) d.player2 else me
        val host=d.putCreatureOnBattlefield(owner,"Grizzly Bears");d.removeSummoningSickness(host)
        val target=d.putCreatureOnBattlefield(me,prey.name);val aura=d.putCardInHand(me,c.name);d.giveMana(me,Color.GREEN,4)
        d.castSpellWithTargets(me,aura,listOf(ChosenTarget.Permanent(host))).error shouldBe null;d.resolvePendingActions()
        val grant=c.staticAbilities.filterIsInstance<GrantActivatedAbility>().single().ability
        if(opponentHost) d.passPriority(me)
        d.submit(ActivateAbility(owner,host,grant.id,targets=listOf(ChosenTarget.Permanent(target)))).error shouldBe null
        d.state.getEntity(host)!!.has<TappedComponent>() shouldBe true
        if(removeHost || removeAura || removeTarget) {
            // Real response on the stack, with native zone transitions and LKI.
            val responder=if(owner==me) d.player2 else me;d.passPriority(owner)
            val victim=if(removeHost) host else if(removeTarget) target else aura
            val response=if(removeAura) card("Reliability Disenchant") {manaCost="{0}";typeLine="Instant";spell {val t=target(TargetFilter.Enchantment);effect=Effects.Destroy(t)}} else kill
            d.registerCards(listOf(response));val instant=d.putCardInHand(responder,response.name)
            d.castSpellWithTargets(responder,instant,listOf(ChosenTarget.Permanent(victim))).error shouldBe null;d.bothPass().error shouldBe null
        }
        d.resolvePendingActions()
        if(removeTarget) (d.state.getEntity(host)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        else (d.state.getEntity(target)?.get<DamageComponent>()?.amount ?: 0) shouldBe 2
    }
    fun hour(c: CardDefinition, empty: Boolean=false, opponentDeserts: Boolean=false) {
        val d=driver(c);val me=d.player1
        d.putPermanentOnBattlefield(if(opponentDeserts) d.player2 else me,"Desert")
        if(opponentDeserts) repeat(2) {d.putPermanentOnBattlefield(d.player2,"Desert")}
        val found=List(2) {d.putCardOnTopOfLibrary(me,"Desert")}
        val spell=d.putCardInHand(me,c.name);d.giveMana(me,Color.GREEN,5);d.castSpell(me,spell).error shouldBe null
        repeat(80) {
            when(val choice=d.pendingDecision) {
                is SelectCardsDecision -> d.submitCardSelection(choice.playerId,if(empty) emptyList() else found).error shouldBe null
                null -> if(d.stackSize>0) d.bothPass().error shouldBe null else return@repeat
                else -> d.autoResolveDecision()
            }
        }
        d.tokenIds(me).size shouldBe if(!empty && !opponentDeserts) 2 else 0
        if(!empty) found.forEach {d.state.getBattlefield(me).contains(it) shouldBe true;d.state.getEntity(it)!!.has<TappedComponent>() shouldBe true}
        d.events.filterIsInstance<LibraryShuffledEvent>().any {it.playerId==me} shouldBe true
    }
    fun ley(c: CardDefinition, accept: Boolean=false, absent: Boolean=false) {
        val d=driver(c);val me=d.player1;val opp=d.player2
        val named=if(absent) null else d.putCardOnTopOfLibrary(opp,lore.name)
        val eventStart=d.events.size
        val spell=d.putCardInHand(me,c.name);d.giveMana(me,Color.GREEN,4);d.castSpell(me,spell).error shouldBe null
        var may=0
        repeat(80) {
            when(val choice=d.pendingDecision) {
                is ChooseTargetsDecision -> d.submitTargetSelection(choice.playerId,listOf(opp)).error shouldBe null
                is YesNoDecision -> {may++;choice.playerId shouldBe opp;d.submitYesNo(opp,accept).error shouldBe null}
                is SelectCardsDecision -> {choice.playerId shouldBe opp;d.submitCardSelection(opp,if(named==null) emptyList() else listOf(named)).error shouldBe null}
                null -> if(d.stackSize>0) d.bothPass().error shouldBe null else return@repeat
                else -> d.autoResolveDecision()
            }
        }
        may shouldBe 1
        if(named!=null) d.getHand(opp).contains(named) shouldBe accept
        if(accept && named!=null) d.events.filterIsInstance<CardsRevealedEvent>().any {named in it.cardIds && it.revealingPlayerId==opp} shouldBe true
        d.events.drop(eventStart).filterIsInstance<LibraryShuffledEvent>().count {it.playerId==opp} shouldBe if(accept) 1 else 0
    }
    fun kite(c: CardDefinition) {
        val spread=card("Reliability Spread") {manaCost="{0}";typeLine="Instant";spell {effect=Effects.Pipeline { val creatures=gather(GameObjectFilter.Creature);run(Effects.AddCountersToCollection(creatures,CounterType.CHARGE)) }}}
        val d=scenarioDriver(c,spread);val me=d.player1;val kite=d.putCreatureOnBattlefield(me,c.name);val other=d.putCreatureOnBattlefield(me,"Grizzly Bears")
        val spell=d.putCardInHand(me,spread.name);d.castSpell(me,spell).error shouldBe null;d.resolvePendingActions()
        (d.state.getEntity(kite)!!.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0) shouldBe 0
        d.state.getEntity(other)!!.get<CountersComponent>()!!.getCount(CounterType.CHARGE) shouldBe 1
    }
    fun sword(c: CardDefinition, noCreatures: Boolean=false, counterBlocked: Boolean=false) {
        val shroud=card("Reliability Shroud") {typeLine="Creature";power=2;toughness=3;keywords(Keyword.SHROUD)}
        val d=scenarioDriver(c,shroud,Tatterkite);val me=d.player1;val host=d.putCreatureOnBattlefield(me,"Grizzly Bears");d.removeSummoningSickness(host)
        val equipment=d.putPermanentOnBattlefield(me,c.name);d.giveColorlessMana(me,2)
        d.submit(ActivateAbility(me,equipment,c.activatedAbilities.single().id,targets=listOf(ChosenTarget.Permanent(host)))).error shouldBe null;d.resolvePendingActions()
        val chosen=if(noCreatures) equipment else d.putCreatureOnBattlefield(me,if(counterBlocked) Tatterkite.name else shroud.name)
        if(noCreatures || counterBlocked) d.replaceState(d.state.updateEntity(equipment) {it.with(CountersComponent(mapOf(CounterType.CHARGE to 1)))})
        d.passPriorityUntil(Step.DECLARE_ATTACKERS);d.declareAttackers(me,listOf(host),d.player2).error shouldBe null;d.passPriorityUntil(Step.COMBAT_DAMAGE)
        if(noCreatures) d.moveToGraveyard(host) // isolated after-trigger state: no owned creature remains
        var selections=0
        repeat(80) {
            when(val choice=d.pendingDecision) {
                is ChooseTargetsDecision -> throw AssertionError("SJ3 is nontargeting at resolution")
                is SelectCardsDecision -> {selections++;d.submitCardSelection(choice.playerId,listOf(if(counterBlocked && selections>1) equipment else chosen)).error shouldBe null}
                null -> if(d.stackSize>0) d.bothPass().error shouldBe null else return@repeat
                else -> d.autoResolveDecision()
            }
        }
        if(counterBlocked) {
            (d.state.getEntity(chosen)!!.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
            d.state.getEntity(equipment)!!.get<CountersComponent>()!!.getCount(CounterType.CHARGE) shouldBe 2
        } else d.state.getEntity(chosen)!!.get<CountersComponent>()!!.getCount(if(noCreatures) CounterType.CHARGE else CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        (selections>0) shouldBe true
    }
    fun celebration(c: CardDefinition, allIllegal: Boolean, repeatedTarget: Boolean=false) {
        val d=driver(c);val me=d.player1;val first=d.putCardInGraveyard(me,"Forest");val second=d.putCardInGraveyard(me,"Forest")
        val a=ChosenTarget.Card(first,me,Zone.GRAVEYARD);val b=ChosenTarget.Card(if(repeatedTarget) first else second,me,Zone.GRAVEYARD)
        val modes=if(allIllegal) listOf(3,1,3,3) else listOf(3,1,1,3)
        val targets=if(allIllegal) listOf(a) else listOf(a,b)
        val ordered=if(allIllegal) listOf(emptyList(),listOf(a),emptyList(),emptyList()) else listOf(emptyList(),listOf(a),listOf(b),emptyList())
        val spell=d.putCardInHand(me,c.name);d.giveMana(me,Color.GREEN,7)
        d.submit(CastSpell(me,spell,targets=targets,chosenModes=modes,modeTargetsOrdered=ordered)).error shouldBe null
        if(!repeatedTarget) d.moveToGraveyard(first) // move again below to remove it from its targeted zone
        if(!repeatedTarget) d.replaceState(d.state.removeFromZone(ZoneKey(me,Zone.GRAVEYARD),first).addToZone(ZoneKey(me,Zone.EXILE),first))
        d.resolvePendingActions()
        d.getLifeTotal(me) shouldBe if(allIllegal) 20 else 28
        if(!allIllegal) d.getHand(me).contains(if(repeatedTarget) first else second) shouldBe true
        // Life selected first still resolves after the printed return mode.
        if(!allIllegal) {
            val events=d.events
            val returned=events.indexOfFirst {it is ZoneChangeEvent && it.entityId==(if(repeatedTarget) first else second) && it.toZone==Zone.HAND}
            val life=events.indexOfFirst {it is LifeChangedEvent && it.playerId==me}
            (returned>=0 && life>returned) shouldBe true
        }
    }
}

class TranslationReliabilityScenarioTest : FunSpec({
    test("PU1 opponent host controls activation and pays tap") {ReliabilityChecks.urge(PredatoryUrge,opponentHost=true)}
    test("PU2 illegal prey prevents all damage") {ReliabilityChecks.urge(PredatoryUrge,removeTarget=true)}
    test("PU4 Aura removal preserves original host") {ReliabilityChecks.urge(PredatoryUrge,removeAura=true)}
    test("PU4 removed host deals last known damage") {ReliabilityChecks.urge(PredatoryUrge,removeHost=true)}
    test("HP1 zero search is legal and still shuffles") {ReliabilityChecks.hour(HourOfPromise,empty=true)}
    test("HP2 opponent Deserts do not count") {ReliabilityChecks.hour(HourOfPromise,opponentDeserts=true)}
    test("HP3 condition follows fetched Deserts") {ReliabilityChecks.hour(HourOfPromise)}
    test("LW1 opponent can decline without shuffle") {ReliabilityChecks.ley(LeyWeaver)}
    test("LW1 accepted search reveals to all then shuffles") {ReliabilityChecks.ley(LeyWeaver,accept=true)}
    test("LW2 absent named card can fail to find") {ReliabilityChecks.ley(LeyWeaver,accept=true,absent=true)}
    test("TK2 collection placement respects the source counter prohibition") {ReliabilityChecks.kite(Tatterkite)}
    test("SJ2 noncombat damage to player does not trigger Sword") {
        val pinger=card("Reliability Pinger") {typeLine="Creature";power=2;toughness=3;activatedAbility {
            cost=Costs.Tap;val player=target(Targets.Player);effect=Effects.DealDamage(1,player)
        }}
        val d=scenarioDriver(SwordOfTruthAndJustice,pinger);val me=d.player1
        val host=d.putCreatureOnBattlefield(me,pinger.name);d.removeSummoningSickness(host)
        val sword=d.putPermanentOnBattlefield(me,SwordOfTruthAndJustice.name);d.giveColorlessMana(me,2)
        d.submit(ActivateAbility(me,sword,SwordOfTruthAndJustice.activatedAbilities.single().id,targets=listOf(ChosenTarget.Permanent(host)))).error shouldBe null;d.resolvePendingActions()
        d.submit(ActivateAbility(me,host,pinger.activatedAbilities.single().id,targets=listOf(ChosenTarget.Player(d.player2)))).error shouldBe null;d.resolvePendingActions()
        d.getLifeTotal(d.player2) shouldBe 19
        (d.state.getEntity(host)!!.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
    }
    test("SJ3 shroud does not prevent nontargeting choice") {ReliabilityChecks.sword(SwordOfTruthAndJustice)}
    test("SJ3 counter prohibition skips placement but still proliferates") {ReliabilityChecks.sword(SwordOfTruthAndJustice,counterBlocked=true)}
    test("SJ3 no creatures still proliferates") {ReliabilityChecks.sword(SwordOfTruthAndJustice,noCreatures=true)}
    test("PC3 all illegal targets prevent life modes") {ReliabilityChecks.celebration(PlanewideCelebration,allIllegal=true)}
    test("PC1 PC3 partial legal targets and printed order") {ReliabilityChecks.celebration(PlanewideCelebration,allIllegal=false)}
    test("PC2 repeated same target moves once without preventing life") {ReliabilityChecks.celebration(PlanewideCelebration,allIllegal=false,repeatedTarget=true)}
})
