package dev.mtg.assistant.explorer

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import dev.mtg.assistant.Catalog
import dev.mtg.assistant.EngineBuild
import dev.mtg.assistant.coverage.*
import dev.mtg.assistant.discovery.reportJson
import dev.mtg.assistant.extensions.definitionDigest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.time.Instant

@Serializable data class CostExecutionObservation(val scenarioId:String,val scopeId:String,val card:String,val pattern:String,
    val mutant:Boolean,val variant:String?,val definitionSha256:String,val expected:String,val result:String,val executed:Boolean,
    val sourceCost:CostTree,val initialState:JsonElement,val actions:JsonElement,val finalState:JsonElement,
    val checks:Map<String,Boolean>,val problem:String?=null)
@Serializable data class CostExecutionReceipt(val applicationRevision:String,val engine:String,val executedAt:String,
    val requirementsSha256:String,val bindingsSha256:String,val harnessSha256:String,val definitions:Map<String,String>,
    val mutationDefinitions:Map<String,String>,val supportDefinitions:Map<String,String>,val observations:List<CostExecutionObservation>)

/** A single parameterized native transaction pattern; budgets are controlled fixtures, not an affordability solver. */
internal object ActivationCostExecution {
    val declarationFixtures=mapOf("mogg-fanatic:0" to "OPPONENT_PLAYER","caustic-caterpillar:0" to "OWN_ARTIFACT")
    private fun terms(tree:CostTree):List<CostTree> = if(tree.kind=="ALL") tree.children.flatMap(::terms) else listOf(tree)
    private fun manaBudget(tree:CostTree):ManaPoolComponent {
        val fixed=terms(tree).filter { it.kind=="MANA" }.flatMap { fixedCostMana(it.mana!!)!!.entries }.groupBy({it.key},{it.value}).mapValues { it.value.sum() }
        return ManaPoolComponent(white=fixed["W"] ?: 0,blue=fixed["U"] ?: 0,black=fixed["B"] ?: 0,red=fixed["R"] ?: 0,green=fixed["G"] ?: 0,colorless=(fixed["C"] ?: 0)+(fixed["GENERIC"] ?: 0))
    }
    private fun totals(pool:ManaPoolComponent)=listOf(pool.white,pool.blue,pool.black,pool.red,pool.green,pool.colorless)

    fun execute(catalog:Catalog,revision:String):CostExecutionReceipt {
        val bindings=ActivationCostFidelity.bindings.associateBy { it.scopeId }
        val mutationCards=ActivationCostFidelity.mutationCards(catalog)
        val observations=mutableListOf<CostExecutionObservation>()
        val supportCards=listOf("Mogg Fanatic","Lifespark Spellbomb").associateWith { catalog.card(it)!! }
        fun run(scope:ActivationCostScope,card:CardDefinition,mode:String,variant:String?=null) {
            val binding=bindings.getValue(scope.id);val ability=card.activatedAbilities[binding.nativeIndex]
            val costTerms=terms(scope.expected);val self=costTerms.any { it.kind=="SACRIFICE_SELF" };val selected=costTerms.any { it.kind=="SACRIFICE_SELECTED" }
            val tap=costTerms.any { it.kind=="TAP_SELF" };val life=costTerms.filter { it.kind=="PAY_LIFE" }.sumOf { it.amount!! }
            val hasMana=costTerms.any { it.kind=="MANA" }
            // Only two manually grounded declaration fixtures are needed. No target/effect acceptance rule is inferred.
            val targetFixture=declarationFixtures[scope.id] ?: "NONE"
            val support=buildList {
                if(self) add(LabSeed(card,Zone.BATTLEFIELD)) // unrelated same-named copy must not substitute for exact source
                if(selected) add(LabSeed(supportCards.getValue("Mogg Fanatic"),Zone.BATTLEFIELD))
                if(targetFixture=="OWN_ARTIFACT" || mode=="ILLEGAL_SELECTION") add(LabSeed(supportCards.getValue("Lifespark Spellbomb"),Zone.BATTLEFIELD))
            }
            var pool=manaBudget(scope.expected)
            if(mode=="NO_MANA") pool=ManaPoolComponent()
            if(mode=="WRONG_COLOR") pool=ManaPoolComponent(colorless=pool.colorless,red=pool.white+pool.blue+pool.black+pool.green+pool.red)
            val game=LabExecution(card,Step.POSTCOMBAT_MAIN,support,pool)
            // Same-named witnesses must not provide automatic mana for an insufficient-resource control.
            // Argentum may otherwise correctly tap the extra Mind Stone to pay the tested ability.
            if(self) game.seedResearchPosition { state->
                state.getBattlefield().filter { it!=game.source && state.getEntity(it)?.get<CardComponent>()?.name==card.name }
                    .fold(state) { seeded,id->seeded.updateEntity(id) { it.with(TappedComponent) } }
            }
            val selectedId=if(selected) game.id(supportCards.getValue("Mogg Fanatic")) else game.source
            if(mode=="TAPPED") game.seedResearchPosition { state->state.updateEntity(game.source) { it.with(TappedComponent) } }
            if(mode=="NO_LIFE") game.seedResearchPosition { state->state.updateEntity(game.player) { it.with(LifeTotalComponent(1)) } }
            if(mode=="BORROWED_SELECTION") game.seedResearchPosition { state->state.updateEntity(selectedId) {
                it.with(it.get<CardComponent>()!!.copy(ownerId=game.opponent)).with(OwnerComponent(game.opponent)).with(ControllerComponent(game.player))
            } }
            val paidId=when { mode=="SELF_SELECTION"->game.source;mode=="ILLEGAL_SELECTION"->game.id(supportCards.getValue("Lifespark Spellbomb"));selected->selectedId;else->game.source }
            val before=game.nativeState();val initial=game.rawState();val originalRef=before.objectRef(paidId)
            val originalOwner=before.getEntity(paidId)?.get<OwnerComponent>()?.playerId
            val beforePool=before.getEntity(game.player)!!.get<ManaPoolComponent>()!!;val beforeLife=before.getEntity(game.player)!!.get<LifeTotalComponent>()!!.life
            val rejected=mode in setOf("NO_MANA","WRONG_COLOR","TAPPED","NO_LIFE","ILLEGAL_SELECTION")
            val unknown=mode=="UNANNOUNCED_SELECTION"
            val expectation=when { unknown->"Selection left unspecified: retain native choice state without certifying completed payment";rejected->"Original source requirement cannot be paid: reject with no payments or state changes";else->"Pay every printed component with the declared source/selected object; leave ordinary resolving effects untested" }
            var problem:String?=null
            try {
                val targets=when(targetFixture) { "OPPONENT_PLAYER"->listOf(ChosenTarget.Player(game.opponent));"OWN_ARTIFACT"->listOf(ChosenTarget.Permanent(game.id(supportCards.getValue("Lifespark Spellbomb"))));else->emptyList() }
                game.perform("Attempt complete payment: $mode",ActivateAbility(game.player,game.source,ability.id,targets,
                    costPayment=if(selected && !unknown) AdditionalCostPayment(sacrificedPermanents=listOf(paidId)) else null),allowIllegal=rejected)
            } catch(e:Exception) { problem=e.message }
            val after=game.nativeState();val final=game.rawState();val outcome=game.actions.lastOrNull()?.outcome
            val movement=game.events.filterIsInstance<ZoneChangeEvent>().singleOrNull { it.entityId==paidId }
            val sacrifices=game.events.filterIsInstance<PermanentsSacrificedEvent>().flatMap { it.permanentIds }
            val paidMana=after.getEntity(game.player)!!.get<ManaPoolComponent>()!!
            val checks=if(rejected) linkedMapOf("native_rejection" to (outcome=="ILLEGAL_ACTION"),"no_state_or_payment_changes" to (initial==final),"no_sacrifice_events" to sacrifices.isEmpty())
            else if(unknown) linkedMapOf("native_transaction_recorded" to game.actions.isNotEmpty())
            else linkedMapOf(
                "native_activation_completed" to (outcome=="DONE"),
                "fixed_mana_actually_spent" to (if(ability.isManaAbility) game.events.filterIsInstance<ManaSpentEvent>().isEmpty() else totals(paidMana).all { it==0 }),
                "life_actually_paid" to (after.getEntity(game.player)!!.get<LifeTotalComponent>()!!.life==beforeLife-life),
                "tap_actually_observed" to (!tap || game.events.filterIsInstance<TappedEvent>().any { it.entityId==game.source }),
                "exact_sacrifice_and_owner_destination" to (!(self || selected) || (paidId in sacrifices && movement!=null && movement.oldObject==originalRef && movement.newObject!=originalRef && movement.fromZone==Zone.BATTLEFIELD && movement.toZone==Zone.GRAVEYARD && movement.requestedDestination==Zone.GRAVEYARD && movement.ownerId==originalOwner && movement.lastKnown?.controllerId==game.player)),
                "unrelated_objects_preserved" to before.getBattlefield().filter { it!=paidId }.all { id->after.objectRef(id)==before.objectRef(id) && id in after.getBattlefield() },
                "ordinary_effects_remain_on_stack" to (ability.isManaAbility || after.stack.size==before.stack.size+1))
            val executed=game.actions.any { it.nativeAction!=JsonNull && it.nativeResult!=JsonNull }
            val result=when { unknown->"UNRESOLVED";!executed->"NOT_STARTED";checks.values.all { it }->"AGREES";outcome in setOf("UNSUPPORTED_DECISION","EXECUTION_FAILED")->"INCOMPLETE";else->"DIFFERS" }
            observations+=CostExecutionObservation("activation-cost:${scope.id}:$mode${variant?.let { ":mutant-$it" }.orEmpty()}",scope.id,scope.card,
                if(selected) "SELECTED_CREATURE_PAYMENT" else if(life>0) "FIXED_LIFE_PAYMENT" else "FIXED_SOURCE_PAYMENT",variant!=null,variant,definitionDigest(card),expectation,result,executed,scope.expected,initial,
                reportJson.encodeToJsonElement(game.actions),final,checks,problem)
        }
        for(scope in ActivationCostFidelity.scopes) {
            val review=ActivationCostFidelity.review(catalog,scope)
            if(review.comparison.state!="SUPPORTED") continue
            val card=catalog.card(bindings.getValue(scope.id).nativeName)!!
            val leaves=terms(scope.expected);run(scope,card,"SUFFICIENT")
            if(leaves.any { it.kind=="MANA" }) run(scope,card,"NO_MANA")
            if(leaves.any { it.kind=="MANA" && fixedCostMana(it.mana!!)!!.keys.any { key->key in listOf("W","U","B","G") } }) run(scope,card,"WRONG_COLOR")
            if(leaves.any { it.kind=="TAP_SELF" }) run(scope,card,"TAPPED")
            if(leaves.any { it.kind=="PAY_LIFE" }) run(scope,card,"NO_LIFE")
            if(leaves.any { it.kind=="SACRIFICE_SELECTED" }) for(mode in listOf("SELF_SELECTION","BORROWED_SELECTION","ILLEGAL_SELECTION","UNANNOUNCED_SELECTION")) run(scope,card,mode)
        }
        for(id in listOf("missing-mana","missing-sacrifice")) {
            val (mutation,card)=mutationCards.single { it.first.id==id };val scope=ActivationCostFidelity.scopes.single { it.id==mutation.scopeId }
            run(scope,card,if(id=="missing-mana") "NO_MANA" else "SUFFICIENT",id)
        }
        return CostExecutionReceipt(revision,EngineBuild.revision,Instant.now().toString(),ActivationCostFidelity.requirementsHash,ActivationCostFidelity.bindingsHash,ActivationCostFidelity.harnessHash,
            ActivationCostFidelity.scopes.associate { it.id to definitionDigest(catalog.card(bindings.getValue(it.id).nativeName)!!) },mutationCards.associate { it.first.id to it.first.definitionSha256 },
            supportCards.mapValues { definitionDigest(it.value) },observations)
    }
}
