package dev.mtg.assistant

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.filters.unified.*
import dev.mtg.assistant.extensions.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Two explicit configurations: approved defect observation, or strict unapproved-candidate expectations. */
class CollectionCounterIntegrationTest : FunSpec({
    val candidate=System.getenv("ARGENTUM_CANDIDATE_REVISION")
    test("actual Tatterkite mixed collection preserves other recipient events and continuation") {
        val spread=card("Integration Collection Placement") {manaCost="{0}";typeLine="Instant";spell {
            effect=Effects.Pipeline {val all=gather(GameObjectFilter.Creature);run(Effects.AddCountersToCollection(all,CounterType.CHARGE))} then Effects.GainLife(1)
        }}
        val d=batchTwoDriver(Tatterkite,spread);val me=d.player1
        val kite=d.putCreatureOnBattlefield(me,Tatterkite.name);val other=d.putCreatureOnBattlefield(d.player2,"Grizzly Bears")
        d.state.projectedState.canReceiveCounters(kite) shouldBe false
        val spell=d.putCardInHand(me,spread.name);d.castSpell(me,spell).error shouldBe null;d.drainBatchTwo()
        fun count(id: com.wingedsheep.sdk.model.EntityId)=d.state.getEntity(id)!!.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0
        count(other) shouldBe 1;d.getLifeTotal(me) shouldBe 21
        // Baseline deliberately records the known wrong state; the candidate must satisfy Oracle zero.
        count(kite) shouldBe if(candidate==null) 1 else 0
        val placed=d.events.filterIsInstance<CountersAddedEvent>()
        placed.map {it.entityId}.toSet() shouldBe if(candidate==null) setOf(kite,other) else setOf(other)
        placed.all {it.amount==1 && it.placedBy==me && it.counterType==CounterType.CHARGE} shouldBe true
    }
    test("original collection Sword comparison stays isolated and must respect prohibition on candidate") {
        val original=SwordOfTruthAndJustice.copy(script=SwordOfTruthAndJustice.script.copy(triggeredAbilities=listOf(
            SwordOfTruthAndJustice.triggeredAbilities.single().copy(effect=Effects.Pipeline {
                val creatures=gather(GameObjectFilter.Creature.youControl())
                ifNotEmpty(creatures) {val chosen=chooseExactly(1,from=creatures);run(Effects.AddCountersToCollection(chosen,CounterType.PLUS_ONE_PLUS_ONE))}
                run(Effects.Proliferate())
            })
        )))
        if(candidate!=null) ReliabilityChecks.sword(original,counterBlocked=true)
        else {
            var failure: AssertionError?=null
            try {ReliabilityChecks.sword(original,counterBlocked=true)} catch(e: AssertionError) {failure=e}
            (failure!=null) shouldBe true
        }
        // Production continues to use its already verified ForEachInCollection composition.
        Extensions.all.single {it.definition.name==SwordOfTruthAndJustice.name}.definition shouldBe SwordOfTruthAndJustice
        ReliabilityChecks.sword(SwordOfTruthAndJustice,counterBlocked=true)
    }
})
