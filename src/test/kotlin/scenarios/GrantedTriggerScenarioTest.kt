package dev.mtg.assistant

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.*
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GrantedTriggerScenarioTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun card(name: String)=checkNotNull(catalog.card(name))
    fun extract(name: String)=PathwayExtractor().extract(listOf(card(name)))
    fun grant(name: String)=extract(name).actions.flatMap { it.grantedTriggers }.single()
    fun root(g: GrantedTrigger)=extract(g.card).actions.single { it.path=="script" && it.face==g.face }
    fun driver(vararg names: String)=scenarioDriver(*names.map(::card).toTypedArray())
    fun GameTestDriver.priority(player: EntityId) {
        if(priorityPlayer!=player) passPriority(checkNotNull(priorityPlayer)).error shouldBe null
        priorityPlayer shouldBe player
    }
    fun GameTestDriver.cast(name: String,target: EntityId?=null,player: EntityId=player1): EntityId {
        priority(player);Color.entries.forEach { giveMana(player,it,10) }
        val id=putCardInHand(player,name)
        submit(CastSpell(player,id,target?.let { listOf(ChosenTarget.Permanent(it)) } ?: emptyList(),paymentStrategy=PaymentStrategy.FromPool)).error shouldBe null
        return id
    }
    fun GameTestDriver.attach(name: String,host: EntityId): EntityId {
        val aura=cast(name,host);resolvePendingActions();return aura
    }
    fun GameTestDriver.available(g: GrantedTrigger,source: EntityId,host: EntityId)=
        g.observeAvailability(state,checkNotNull(state.objectRef(source)),checkNotNull(state.objectRef(host)),services,root(g))
    fun GameTestDriver.combat(hosts: List<EntityId>,attacker: EntityId=player2) {
        passPriorityUntil(Step.DECLARE_ATTACKERS,attacker)
        declareAttackers(attacker,hosts,getOpponent(attacker)).error shouldBe null
        passPriorityUntil(Step.DECLARE_BLOCKERS,attacker)
        declareBlockers(getOpponent(attacker),emptyMap()).error shouldBe null
        passPriorityUntil(Step.COMBAT_DAMAGE,attacker)
        if(pendingDecision is CombatResolutionDecision) confirmCombatDamage().error shouldBe null
    }
    fun GameTestDriver.witness(g: GrantedTrigger,before: GameState,mark: Int,instance: GrantedAbilityInstance): GrantedTriggerObservation {
        val slice=events.drop(mark)
        val emitted=slice.filterIsInstance<AbilityTriggeredEvent>().filter { it.sourceId==instance.recipient.entityId }
            .single { state.getEntity(checkNotNull(it.abilityEntityId))?.get<TriggeredAbilityOnStackComponent>()?.granterId==instance.granter.entityId }
        val observation=g.observeStack(before,state,slice,instance,checkNotNull(state.objectRef(checkNotNull(emitted.abilityEntityId))),services,root(g))
        println("GRANTED_TRIGGER_NATIVE "+reportJson.encodeToString(GrantedTriggerObservation.serializer(),observation))
        observation.verdict shouldBe Verdict.MATCH
        return observation
    }
    fun GameTestDriver.drawChoice(accept: Boolean,player: EntityId) {
        bothPass().error shouldBe null
        (pendingDecision is YesNoDecision) shouldBe true
        pendingDecision?.playerId shouldBe player
        submitYesNo(player,accept).error shouldBe null
        resolvePendingActions()
    }
    test("Sixth Sense on an opponent creature binds source damage subject controller and optional draw") {
        for(accept in listOf(true,false)) {
            val d=driver("Sixth Sense")
            val host=d.putCreatureOnBattlefield(d.player2,"Hill Giant");d.removeSummoningSickness(host)
            val aura=d.attach("Sixth Sense",host);val g=grant("Sixth Sense")
            val instance=checkNotNull(d.available(g,aura,host).instance)
            instance.granterController shouldBe d.player1;instance.recipientController shouldBe d.player2
            val before=d.state;val mark=d.events.size
            d.combat(listOf(host));val observed=d.witness(g,before,mark,instance)
            observed.context?.damageSourceId shouldBe host
            observed.context?.triggeringEntityId shouldBe d.player1
            observed.controller shouldBe d.player2
            g.observeStack(before,d.state,emptyList(),instance,checkNotNull(observed.stackObject),d.services,root(g)).verdict shouldBe Verdict.UNKNOWN
            val cause=d.events.drop(mark).filterIsInstance<DamageDealtEvent>().single()
            g.observeStack(before,d.state,d.events.drop(mark)+cause,instance,checkNotNull(observed.stackObject),d.services,root(g)).verdict shouldBe Verdict.UNKNOWN
            val hand=d.getHandSize(d.player2);d.drawChoice(accept,d.player2)
            d.getHandSize(d.player2) shouldBe hand+if(accept) 1 else 0
            d.getLifeTotal(d.player1) shouldBe 17
        }
    }
    test("recipient noncombat damage does not trigger Sixth Sense despite a current grant") {
        val d=driver("Sixth Sense","Burning Anger")
        val host=d.putCreatureOnBattlefield(d.player2,"Hill Giant");d.removeSummoningSickness(host)
        val aura=d.attach("Sixth Sense",host);d.attach("Burning Anger",host)
        val g=grant("Sixth Sense");d.available(g,aura,host).verdict shouldBe Verdict.MATCH
        val ability=(card("Burning Anger").script.staticAbilities.single() as GrantActivatedAbility).ability
        d.priority(d.player2);val mark=d.events.size
        d.submit(ActivateAbility(d.player2,host,ability.id,listOf(ChosenTarget.Player(d.player1)))).error shouldBe null
        d.resolvePendingActions()
        d.events.drop(mark).filterIsInstance<DamageDealtEvent>().single().let { it.sourceId shouldBe host;it.isCombatDamage shouldBe false }
        d.events.drop(mark).filterIsInstance<AbilityTriggeredEvent>().isEmpty() shouldBe true
    }
    test("Combat Research draws for its recipient and ignores an unrelated same-name creature") {
        val d=driver("Combat Research")
        val host=d.putCreatureOnBattlefield(d.player2,"Hill Giant");d.removeSummoningSickness(host)
        val other=d.putCreatureOnBattlefield(d.player2,"Hill Giant");d.removeSummoningSickness(other)
        val aura=d.attach("Combat Research",host);val g=grant("Combat Research")
        d.available(g,aura,other).verdict shouldBe Verdict.NO_MATCH
        var mark=d.events.size;d.combat(listOf(other))
        d.events.drop(mark).filterIsInstance<AbilityTriggeredEvent>().isEmpty() shouldBe true
        val instance=checkNotNull(d.available(g,aura,host).instance);val before=d.state;mark=d.events.size
        d.combat(listOf(host));d.witness(g,before,mark,instance)
        val hand=d.getHandSize(d.player2);d.bothPass().error shouldBe null
        d.getHandSize(d.player2) shouldBe hand+1
    }
    test("removing the grant after detection preserves the stacked trigger but prevents future triggers") {
        val d=driver("Sixth Sense","Boomerang")
        val host=d.putCreatureOnBattlefield(d.player2,"Hill Giant");d.removeSummoningSickness(host)
        val aura=d.attach("Sixth Sense",host);val g=grant("Sixth Sense")
        val instance=checkNotNull(d.available(g,aura,host).instance);val before=d.state;var mark=d.events.size
        d.combat(listOf(host));val observed=d.witness(g,before,mark,instance)
        d.cast("Boomerang",aura);d.bothPass().error shouldBe null
        d.available(g,aura,host).verdict shouldBe Verdict.NO_MATCH
        g.observePersistence(d.state,observed).verdict shouldBe Verdict.MATCH
        val hand=d.getHandSize(d.player2);d.drawChoice(true,d.player2);d.getHandSize(d.player2) shouldBe hand+1
        mark=d.events.size;d.combat(listOf(host))
        d.events.drop(mark).filterIsInstance<AbilityTriggeredEvent>().isEmpty() shouldBe true
    }
    test("Custody Battle follows recipient upkeeps and transfers control after the native choice") {
        val d=driver("Custody Battle")
        val host=d.putCreatureOnBattlefield(d.player2,"Hill Giant")
        val land=d.putLandOnBattlefield(d.player2,"Forest")
        val aura=d.attach("Custody Battle",host);val g=grant("Custody Battle")
        var instance=checkNotNull(d.available(g,aura,host).instance);var before=d.state;var mark=d.events.size
        d.passPriorityUntil(Step.UPKEEP,d.player2);d.witness(g,before,mark,instance)
        d.bothPass().error shouldBe null
        (d.pendingDecision is SelectCardsDecision) shouldBe true;d.pendingDecision?.playerId shouldBe d.player2
        d.submitCardSelection(d.player2,listOf(land)).error shouldBe null
        d.state.projectedState.getController(host) shouldBe d.player2
        instance=checkNotNull(d.available(g,aura,host).instance);before=d.state;mark=d.events.size
        d.passPriorityUntil(Step.UPKEEP,d.player1)
        d.events.drop(mark).filterIsInstance<AbilityTriggeredEvent>().isEmpty() shouldBe true
        before=d.state;mark=d.events.size
        d.passPriorityUntil(Step.UPKEEP,d.player2);d.witness(g,before,mark,instance)
        d.bothPass().error shouldBe null // no remaining land: native suffer branch
        d.state.projectedState.getController(host) shouldBe d.player1
        instance=checkNotNull(d.available(g,aura,host).instance);before=d.state;mark=d.events.size
        d.passPriorityUntil(Step.UPKEEP,d.player1);d.witness(g,before,mark,instance)
    }
    test("recipient renewal cannot replace the captured source of an established trigger") {
        val d=driver("Sixth Sense","Unsummon","Vedalken Orrery")
        d.putPermanentOnBattlefield(d.player2,"Vedalken Orrery")
        val host=d.putCreatureOnBattlefield(d.player2,"Hill Giant");d.removeSummoningSickness(host)
        val aura=d.attach("Sixth Sense",host);val g=grant("Sixth Sense")
        val instance=checkNotNull(d.available(g,aura,host).instance);val before=d.state;val mark=d.events.size
        d.combat(listOf(host));val observed=d.witness(g,before,mark,instance)
        d.cast("Unsummon",host);d.bothPass().error shouldBe null
        d.priority(d.player2);Color.entries.forEach { d.giveMana(d.player2,it,10) }
        d.submit(CastSpell(d.player2,host,paymentStrategy=PaymentStrategy.FromPool)).error shouldBe null
        d.bothPass().error shouldBe null
        d.state.isCurrentObject(instance.recipient) shouldBe false
        g.observePersistence(d.state,observed).verdict shouldBe Verdict.MATCH
        d.available(g,aura,host).verdict shouldBe Verdict.NO_MATCH
        val hand=d.getHandSize(d.player2);d.drawChoice(true,d.player2);d.getHandSize(d.player2) shouldBe hand+1
    }
    test("granter renewal and reattachment cannot supply an earlier trigger declaration instance") {
        val d=driver("Sixth Sense","Boomerang","Vedalken Orrery")
        d.putPermanentOnBattlefield(d.player1,"Vedalken Orrery")
        val host=d.putCreatureOnBattlefield(d.player2,"Hill Giant");d.removeSummoningSickness(host)
        val next=d.putCreatureOnBattlefield(d.player2,"Hill Giant")
        val aura=d.attach("Sixth Sense",host);val g=grant("Sixth Sense")
        val instance=checkNotNull(d.available(g,aura,host).instance);val before=d.state;val mark=d.events.size
        d.combat(listOf(host));val observed=d.witness(g,before,mark,instance)
        d.cast("Boomerang",aura);d.bothPass().error shouldBe null
        d.priority(d.player1)
        d.submit(CastSpell(d.player1,aura,listOf(ChosenTarget.Permanent(next)),paymentStrategy=PaymentStrategy.FromPool)).error shouldBe null
        d.bothPass().error shouldBe null
        val fresh=checkNotNull(d.available(g,aura,next).instance)
        (fresh.granter!=instance.granter) shouldBe true
        d.available(g,aura,host).verdict shouldBe Verdict.NO_MATCH
        g.observePersistence(d.state,observed).verdict shouldBe Verdict.MATCH
        g.observeStack(before,d.state,d.events.drop(mark),fresh,checkNotNull(observed.stackObject),d.services,root(g)).verdict shouldBe Verdict.UNKNOWN
        d.drawChoice(true,d.player2)
    }
    test("control changes affect future grants while an existing trigger keeps its controller") {
        val d=driver("Sixth Sense","Control Magic","Vedalken Orrery")
        d.putPermanentOnBattlefield(d.player1,"Vedalken Orrery")
        val host=d.putCreatureOnBattlefield(d.player2,"Hill Giant");d.removeSummoningSickness(host)
        val aura=d.attach("Sixth Sense",host);val g=grant("Sixth Sense")
        val instance=checkNotNull(d.available(g,aura,host).instance);val before=d.state;val mark=d.events.size
        d.combat(listOf(host));val observed=d.witness(g,before,mark,instance)
        d.cast("Control Magic",host);d.bothPass().error shouldBe null
        d.available(g,aura,host).instance?.recipientController shouldBe d.player1
        g.observePersistence(d.state,observed).verdict shouldBe Verdict.MATCH
        g.observePersistence(d.state,observed).controller shouldBe d.player2
        val hand=d.getHandSize(d.player2);d.drawChoice(true,d.player2);d.getHandSize(d.player2) shouldBe hand+1
    }
    test("two equal-name granters and multiple recipients retain actual native supplying IDs") {
        val d=driver("Sixth Sense")
        val first=d.putCreatureOnBattlefield(d.player2,"Hill Giant");d.removeSummoningSickness(first)
        val second=d.putCreatureOnBattlefield(d.player2,"Hill Giant");d.removeSummoningSickness(second)
        val a=d.attach("Sixth Sense",first);val b=d.attach("Sixth Sense",first);val c=d.attach("Sixth Sense",second)
        val g=grant("Sixth Sense")
        val instances=listOf(a to first,b to first,c to second).map { (source,host)->checkNotNull(d.available(g,source,host).instance) }
        val before=d.state;val mark=d.events.size;d.combat(listOf(first,second))
        d.events.drop(mark).filterIsInstance<AbilityTriggeredEvent>().size shouldBe 3
        for(instance in instances) d.witness(g,before,mark,instance)
        val event=d.events.drop(mark).filterIsInstance<AbilityTriggeredEvent>().first { it.sourceId==second }
        g.observeStack(before,d.state,d.events.drop(mark),instances.first(),checkNotNull(d.state.objectRef(checkNotNull(event.abilityEntityId))),d.services,root(g)).verdict shouldBe Verdict.UNKNOWN
    }
    test("equal-ID declarations on a single granter remain ambiguous even when native triggers exist") {
        val base=card("Sixth Sense");val declaration=base.script.staticAbilities.single() as GrantTriggeredAbility
        val synthetic=base.copy(name="Isolated duplicate trigger declarations",script=base.script.copy(staticAbilities=listOf(declaration,declaration)))
        val d=scenarioDriver(synthetic)
        val host=d.putCreatureOnBattlefield(d.player2,"Hill Giant");d.removeSummoningSickness(host)
        val aura=d.attach(synthetic.name,host)
        val grants=PathwayExtractor().extract(listOf(synthetic)).actions.flatMap { it.grantedTriggers }
        for(g in grants) {
            val root=PathwayExtractor().extract(listOf(synthetic)).actions.single { it.path=="script" }
            g.observeAvailability(d.state,checkNotNull(d.state.objectRef(aura)),checkNotNull(d.state.objectRef(host)),d.services,root).verdict shouldBe Verdict.UNKNOWN
        }
        val mark=d.events.size;d.combat(listOf(host))
        d.events.drop(mark).filterIsInstance<AbilityTriggeredEvent>().size shouldBe 2
    }
    test("intrinsic triggers cannot borrow a matching granted declaration identity") {
        val base=card("Sixth Sense");val declaration=base.script.staticAbilities.single() as GrantTriggeredAbility
        val hostDef=card("Hill Giant").copy(name="Isolated intrinsic trigger host",script=card("Hill Giant").script.copy(triggeredAbilities=listOf(declaration.ability)))
        val d=scenarioDriver(base,hostDef)
        val host=d.putCreatureOnBattlefield(d.player2,hostDef.name);d.removeSummoningSickness(host)
        val aura=d.attach("Sixth Sense",host);val g=grant("Sixth Sense")
        d.available(g,aura,host).verdict shouldBe Verdict.UNKNOWN
        val mark=d.events.size;d.combat(listOf(host))
        d.events.drop(mark).filterIsInstance<AbilityTriggeredEvent>().isNotEmpty() shouldBe true
    }
    test("Dire Blunderbuss native craft equip and attack identify the host and granting Equipment separately") {
        val d=driver("Dire Flail","Sol Ring")
        val equipment=d.putPermanentOnBattlefield(d.player1,"Dire Flail")
        val material=d.putPermanentOnBattlefield(d.player1,"Sol Ring")
        d.giveMana(d.player1,Color.RED,5)
        d.submit(ActivateAbility(d.player1,equipment,card("Dire Flail").script.activatedAbilities.single { !it.isEquipAbility }.id,
            costPayment=AdditionalCostPayment(exiledCards=listOf(material)))).error shouldBe null
        d.resolvePendingActions()
        val host=d.putCreatureOnBattlefield(d.player1,"Hill Giant");d.removeSummoningSickness(host)
        val other=d.putCreatureOnBattlefield(d.player1,"Hill Giant")
        fun equip(target: EntityId) {
            d.priority(d.player1);d.giveColorlessMana(d.player1,1)
            d.submit(ActivateAbility(d.player1,equipment,card("Dire Flail").backFace!!.script.activatedAbilities.single().id,listOf(ChosenTarget.Permanent(target)))).error shouldBe null
            d.resolvePendingActions()
        }
        val g=grant("Dire Flail")
        d.available(g,equipment,host).verdict shouldBe Verdict.NO_MATCH
        equip(other)
        d.available(g,equipment,other).verdict shouldBe Verdict.MATCH
        equip(host);d.available(g,equipment,other).verdict shouldBe Verdict.NO_MATCH
        val instance=checkNotNull(d.available(g,equipment,host).instance)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS,d.player1)
        val before=d.state;val mark=d.events.size
        d.declareAttackers(d.player1,listOf(host),d.player2).error shouldBe null
        val observed=d.witness(g,before,mark,instance)
        observed.nativeGranterId shouldBe equipment;observed.instance.recipient.entityId shouldBe host
        d.resolvePendingActions() // native optional reflexive continuation is not composed by the application
    }
    test("Stab Wound's own Aura trigger does not become a creature-source grant") {
        val d=driver("Stab Wound")
        val host=d.putCreatureOnBattlefield(d.player2,"Hill Giant")
        val aura=d.attach("Stab Wound",host);val mark=d.events.size
        d.passPriorityUntil(Step.UPKEEP,d.player2)
        val event=d.events.drop(mark).filterIsInstance<AbilityTriggeredEvent>().single()
        event.sourceId shouldBe aura;event.controllerId shouldBe d.player1
        val stack=checkNotNull(d.state.getEntity(checkNotNull(event.abilityEntityId))?.get<TriggeredAbilityOnStackComponent>())
        stack.granterId shouldBe null
        d.bothPass().error shouldBe null;d.getLifeTotal(d.player2) shouldBe 18
    }
})

