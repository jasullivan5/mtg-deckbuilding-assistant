package dev.mtg.assistant

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.EntityId
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*

class GrantedAbilityScenarioTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun card(name: String)=checkNotNull(catalog.card(name))
    fun evidence(name: String)=PathwayExtractor().extract(listOf(card(name)))
    fun grant(name: String)=evidence(name).actions.flatMap { it.grantedAbilities }.single()
    fun root(name: String)=evidence(name).actions.single { it.path=="script" }
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
        val id=cast(name,host);resolvePendingActions();return id
    }
    fun GameTestDriver.available(g: GrantedAbility,source: EntityId,host: EntityId)=
        g.observeAvailability(state,checkNotNull(state.objectRef(source)),checkNotNull(state.objectRef(host)),services,root(g.card))
    fun GameTestDriver.activate(g: GrantedAbility,source: EntityId,host: EntityId,player: EntityId,target: EntityId?=null): GrantedActivationObservation {
        priority(player)
        val instance=checkNotNull(available(g,source,host).instance)
        val before=state
        val action=ActivateAbility(player,host,g.declaration.ability.id,
            target?.let { listOf(ChosenTarget.Player(it)) } ?: emptyList(),manaColorChoice=if(g.declaration.ability.isManaAbility) Color.RED else null)
        val result=submit(action);result.error shouldBe null
        val observed=g.observeActivation(before,state,action,result.events,instance,services,root(g.card))
        observed.verdict shouldBe Verdict.MATCH
        println("GRANTED_NATIVE "+reportJson.encodeToString(GrantedActivationObservation.serializer(),observed))
        return observed
    }
    test("Burning Anger uses an opponent host's controller tap and resolution-time power as its damage source") {
        val d=driver("Burning Anger","Giant Growth")
        val host=d.putCreatureOnBattlefield(d.player2,"Hill Giant");d.removeSummoningSickness(host)
        val unrelated=d.putCreatureOnBattlefield(d.player2,"Hill Giant")
        val aura=d.attach("Burning Anger",host);val g=grant("Burning Anger")
        d.available(g,aura,host).instance?.granterController shouldBe d.player1
        d.available(g,aura,host).instance?.recipientController shouldBe d.player2
        d.available(g,aura,unrelated).verdict shouldBe Verdict.NO_MATCH
        (d.submit(ActivateAbility(d.player1,host,g.declaration.ability.id,listOf(ChosenTarget.Player(d.player2)))).error!=null) shouldBe true
        d.activate(g,aura,host,d.player2,d.player1)
        d.isTapped(host) shouldBe true;d.isTapped(aura) shouldBe false
        d.cast("Giant Growth",host);d.resolvePendingActions()
        d.getLifeTotal(d.player1) shouldBe 14
        d.events.filterIsInstance<DamageDealtEvent>().last().let { it.sourceId shouldBe host;it.amount shouldBe 6 }
    }
    test("Burning Anger survives grant removal and resolves after its recipient leaves using native last-known power") {
        val d=driver("Burning Anger","Giant Growth","Murder")
        val host=d.putCreatureOnBattlefield(d.player2,"Hill Giant");d.removeSummoningSickness(host)
        val aura=d.attach("Burning Anger",host);val g=grant("Burning Anger")
        val observed=d.activate(g,aura,host,d.player2,d.player1)
        d.cast("Murder",host);d.cast("Giant Growth",host)
        d.resolvePendingActions()
        d.getGraveyard(d.player2).contains(host) shouldBe true
        d.getGraveyard(d.player1).contains(aura) shouldBe true
        d.getLifeTotal(d.player1) shouldBe 14
        d.events.filterIsInstance<DamageDealtEvent>().last().sourceId shouldBe host
        observed.stackObject?.let { d.state.isCurrentObject(it) } shouldBe false
    }
    test("Presence of Gond creates for the activating host controller and keeps an activated ability after grant loss") {
        val d=driver("Presence of Gond","Boomerang")
        val host=d.putCreatureOnBattlefield(d.player2,"Hill Giant");d.removeSummoningSickness(host)
        val aura=d.attach("Presence of Gond",host);val g=grant("Presence of Gond")
        val observed=d.activate(g,aura,host,d.player2)
        val stack=checkNotNull(d.state.getEntity(checkNotNull(observed.stackObject).entityId)?.get<ActivatedAbilityOnStackComponent>())
        stack.controllerId shouldBe d.player2;stack.sourceId shouldBe host;stack.granterId shouldBe aura
        d.cast("Boomerang",aura);d.bothPass().error shouldBe null
        d.available(g,aura,host).verdict shouldBe Verdict.NO_MATCH
        d.stackSize shouldBe 1
        d.resolvePendingActions()
        d.tokenIds(d.player1).size shouldBe 0;d.tokenIds(d.player2).size shouldBe 1
        (d.submit(ActivateAbility(d.player2,host,g.declaration.ability.id)).error!=null) shouldBe true
    }
    test("tap grant availability is distinct from sickness haste and already-tapped activation legality") {
        val d=driver("Presence of Gond","Expedite")
        val host=d.putCreatureOnBattlefield(d.player1,"Hill Giant")
        val aura=d.attach("Presence of Gond",host);val g=grant("Presence of Gond")
        d.available(g,aura,host).verdict shouldBe Verdict.MATCH
        (d.submit(ActivateAbility(d.player1,host,g.declaration.ability.id)).error!=null) shouldBe true
        d.cast("Expedite",host);d.resolvePendingActions()
        d.activate(g,aura,host,d.player1);d.resolvePendingActions()
        d.tokenIds(d.player1).size shouldBe 1
        (d.submit(ActivateAbility(d.player1,host,g.declaration.ability.id)).error!=null) shouldBe true
        d.available(g,aura,host).verdict shouldBe Verdict.MATCH
    }
    test("host controller changes alter future activators but preserve the already-activated ability controller") {
        val d=driver("Presence of Gond","Control Magic","Vedalken Orrery")
        d.putPermanentOnBattlefield(d.player1,"Vedalken Orrery")
        val host=d.putCreatureOnBattlefield(d.player2,"Hill Giant");d.removeSummoningSickness(host)
        val aura=d.attach("Presence of Gond",host);val g=grant("Presence of Gond")
        d.activate(g,aura,host,d.player2)
        d.cast("Control Magic",host);d.resolvePendingActions()
        d.state.projectedState.getController(host) shouldBe d.player1
        d.tokenIds(d.player2).size shouldBe 1;d.tokenIds(d.player1).size shouldBe 0
        d.available(g,aura,host).instance?.recipientController shouldBe d.player1
    }
    test("native equip moves the Longbow grant and the host is the deathtouch damage source") {
        val d=driver("Viridian Longbow","Typhoid Rats")
        val first=d.putCreatureOnBattlefield(d.player1,"Typhoid Rats");d.removeSummoningSickness(first)
        val second=d.putCreatureOnBattlefield(d.player1,"Hill Giant");d.removeSummoningSickness(second)
        val victim=d.putCreatureOnBattlefield(d.player2,"Hill Giant")
        val bow=d.cast("Viridian Longbow");d.resolvePendingActions()
        fun equip(host: EntityId) {
            d.priority(d.player1);d.giveColorlessMana(d.player1,3)
            d.submit(ActivateAbility(d.player1,bow,card("Viridian Longbow").script.activatedAbilities.single().id,listOf(ChosenTarget.Permanent(host)))).error shouldBe null
            d.resolvePendingActions()
        }
        equip(first);val g=grant("Viridian Longbow")
        d.available(g,bow,first).verdict shouldBe Verdict.MATCH
        d.available(g,bow,second).verdict shouldBe Verdict.NO_MATCH
        d.submit(ActivateAbility(d.player1,first,g.declaration.ability.id,listOf(ChosenTarget.Permanent(victim)))).error shouldBe null
        d.isTapped(first) shouldBe true;d.isTapped(bow) shouldBe false
        d.resolvePendingActions()
        d.getGraveyard(d.player2).contains(victim) shouldBe true
        d.events.filterIsInstance<DamageDealtEvent>().last().sourceId shouldBe first
        equip(second)
        d.available(g,bow,first).verdict shouldBe Verdict.NO_MATCH
        d.available(g,bow,second).verdict shouldBe Verdict.MATCH
        (d.submit(ActivateAbility(d.player1,first,g.declaration.ability.id,listOf(ChosenTarget.Player(d.player2)))).error!=null) shouldBe true
    }
    test("Rite independently grants multiple creatures native mana activations without a response stack") {
        val d=driver("Cryptolith Rite","Boomerang")
        val first=d.putCreatureOnBattlefield(d.player1,"Hill Giant");d.removeSummoningSickness(first)
        val second=d.putCreatureOnBattlefield(d.player1,"Hill Giant");d.removeSummoningSickness(second)
        val opponent=d.putCreatureOnBattlefield(d.player2,"Hill Giant")
        val rite=d.cast("Cryptolith Rite");d.resolvePendingActions();val g=grant("Cryptolith Rite")
        d.available(g,rite,opponent).verdict shouldBe Verdict.NO_MATCH
        val a=d.available(g,rite,first).instance;val b=d.available(g,rite,second).instance
        (a!=b) shouldBe true
        d.activate(g,rite,first,d.player1);d.stackSize shouldBe 0
        d.activate(g,rite,second,d.player1);d.stackSize shouldBe 0
        d.isTapped(first) shouldBe true;d.isTapped(second) shouldBe true;d.isTapped(rite) shouldBe false
        d.events.filterIsInstance<ManaAbilityResolvedEvent>().takeLast(2).map { it.sourceId } shouldBe listOf(first,second)
        d.cast("Boomerang",rite);d.resolvePendingActions()
        d.available(g,rite,first).verdict shouldBe Verdict.NO_MATCH
    }
    test("group-grant controller and recipient changes use current projected control") {
        val d=driver("Cryptolith Rite","Control Magic","Confiscate")
        val own=d.putCreatureOnBattlefield(d.player1,"Hill Giant")
        val foreign=d.putCreatureOnBattlefield(d.player2,"Hill Giant")
        val rite=d.cast("Cryptolith Rite");d.resolvePendingActions();val g=grant("Cryptolith Rite")
        d.available(g,rite,foreign).verdict shouldBe Verdict.NO_MATCH
        d.cast("Control Magic",foreign);d.resolvePendingActions()
        d.available(g,rite,foreign).verdict shouldBe Verdict.MATCH
        d.passPriorityUntil(Step.PRECOMBAT_MAIN,d.player2)
        d.cast("Confiscate",rite,d.player2);d.resolvePendingActions()
        d.state.projectedState.getController(rite) shouldBe d.player2
        d.available(g,rite,own).verdict shouldBe Verdict.NO_MATCH
        d.available(g,rite,foreign).verdict shouldBe Verdict.NO_MATCH
        val another=d.putCreatureOnBattlefield(d.player2,"Hill Giant")
        d.available(g,rite,another).verdict shouldBe Verdict.MATCH
    }
    test("bounced and recast original recipient cannot inherit an old attachment or grant instance") {
        val d=driver("Presence of Gond","Unsummon")
        val host=d.putCreatureOnBattlefield(d.player1,"Hill Giant")
        val aura=d.attach("Presence of Gond",host);val g=grant("Presence of Gond")
        val instance=checkNotNull(d.available(g,aura,host).instance)
        d.cast("Unsummon",host);d.resolvePendingActions()
        d.state.isCurrentObject(instance.recipient) shouldBe false
        d.giveMana(d.player1,Color.RED,4)
        d.submit(CastSpell(d.player1,host,paymentStrategy=PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        d.getPermanents(d.player1).contains(host) shouldBe true
        g.observeAvailability(d.state,instance.granter,instance.recipient,d.services,root(g.card)).verdict shouldBe Verdict.NO_MATCH
        d.available(g,aura,host).verdict shouldBe Verdict.NO_MATCH
    }
    test("a noncreature land gains and loses the group grant through native animation expiry") {
        val d=driver("Cryptolith Rite","Mishra's Factory")
        val land=d.putLandOnBattlefield(d.player1,"Mishra's Factory")
        val rite=d.cast("Cryptolith Rite");d.resolvePendingActions();val g=grant("Cryptolith Rite")
        d.available(g,rite,land).verdict shouldBe Verdict.NO_MATCH
        d.giveColorlessMana(d.player1,1)
        d.submit(ActivateAbility(d.player1,land,card("Mishra's Factory").script.activatedAbilities[1].id)).error shouldBe null
        d.resolvePendingActions()
        d.available(g,rite,land).verdict shouldBe Verdict.MATCH
        d.passPriorityUntil(Step.PRECOMBAT_MAIN,d.player2)
        d.available(g,rite,land).verdict shouldBe Verdict.NO_MATCH
    }
    test("same-name granters retain native selected-supplier provenance and renewed source generations") {
        val d=driver("Cryptolith Rite","Boomerang")
        val host=d.putCreatureOnBattlefield(d.player1,"Hill Giant");d.removeSummoningSickness(host)
        val other=d.putCreatureOnBattlefield(d.player1,"Hill Giant");d.removeSummoningSickness(other)
        val first=d.cast("Cryptolith Rite");d.resolvePendingActions()
        val g=grant("Cryptolith Rite")
        val a=checkNotNull(d.available(g,first,host).instance)
        val second=d.cast("Cryptolith Rite");d.resolvePendingActions()
        d.available(g,first,host).verdict shouldBe Verdict.UNKNOWN // Native lookup deduplicates equal IDs.
        val b=checkNotNull(d.available(g,second,host).instance)
        (a.granter!=b.granter) shouldBe true
        val before=d.state;val action=ActivateAbility(d.player1,host,g.declaration.ability.id,manaColorChoice=Color.RED)
        val result=d.submit(action);result.error shouldBe null
        g.observeActivation(before,d.state,action,result.events,a,d.services).verdict shouldBe Verdict.UNKNOWN
        g.observeActivation(before,d.state,action,result.events,b,d.services).verdict shouldBe Verdict.MATCH
        g.observeActivation(before,d.state,action.copy(sourceId=other),result.events,a,d.services).verdict shouldBe Verdict.UNKNOWN
        g.observeActivation(before,d.state,action,emptyList(),a,d.services).verdict shouldBe Verdict.UNKNOWN
        d.cast("Boomerang",first);d.resolvePendingActions()
        g.observeAvailability(d.state,a.granter,a.recipient,d.services).verdict shouldBe Verdict.NO_MATCH
        d.submit(CastSpell(d.player1,first,paymentStrategy=PaymentStrategy.FromPool)).error shouldBe null
        d.resolvePendingActions()
        d.state.isCurrentObject(a.granter) shouldBe false
        val renewed=checkNotNull(d.available(g,first,host).instance)
        (renewed.granter!=a.granter) shouldBe true
        renewed.recipient shouldBe a.recipient
        g.observeAvailability(d.state,a.granter,a.recipient,d.services).verdict shouldBe Verdict.NO_MATCH
    }
})
