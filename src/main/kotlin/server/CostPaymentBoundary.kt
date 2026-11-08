package dev.mtg.assistant.explorer

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import dev.mtg.assistant.Catalog
import dev.mtg.assistant.EngineBuild
import dev.mtg.assistant.extensions.definitionDigest
import dev.mtg.assistant.discovery.reportJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.time.Instant

@Serializable data class PaymentBoundaryStep(val label:String,val validationMessage:String?,val outcome:String,val rejection:String?,val message:String?,
    val before:JsonElement,val action:JsonElement,val nativeResult:JsonElement,val after:JsonElement)
@Serializable data class PaymentBoundaryScenario(val id:String,val scopeId:String,val card:String,val definitionSha256:String,
    val sourceExpectation:String,val result:String,val diagnostic:String,val checks:Map<String,Boolean>,val steps:List<PaymentBoundaryStep>,val concern:String?=null)
@Serializable data class PaymentBoundaryReceipt(val schemaVersion:Int=1,val applicationRevision:String,val engine:String,val executedAt:String,
    val inputs:Map<String,String>,val harnessSha256:String,val definitions:Map<String,String>,val scenarios:List<PaymentBoundaryScenario>)

/** Bounded native probes. No application affordability, targeting or payment implementation. */
internal object CostPaymentBoundary {
    fun execute(catalog:Catalog,revision:String):PaymentBoundaryReceipt {
        val scenarios=mutableListOf<PaymentBoundaryScenario>()
        val definitions=linkedMapOf<String,String>()
        val bindings=ActivationCostFidelity.bindings.associateBy { it.scopeId }
        fun run(id:String,scopeId:String,mode:String) {
            val binding=bindings.getValue(scopeId);val source=catalog.card(binding.nativeName)!!
            val creature=catalog.card("Mogg Fanatic")!!;val artifact=catalog.card("Lifespark Spellbomb")!!
            val seeds=if(scopeId=="viscera-seer:0" || scopeId=="caustic-caterpillar:0") listOf(LabSeed(creature,Zone.BATTLEFIELD),LabSeed(artifact,Zone.BATTLEFIELD)) else emptyList()
            val used=(listOf(source)+seeds.map { it.card }).distinctBy { it.name }
            used.forEach { definitions[it.name]=definitionDigest(it) }
            val probe=ActionProcessor(EngineServices(CardRegistry().apply { used.forEach(::register) }),computeUndo=false)
            val pool=if(scopeId=="caustic-caterpillar:0" && mode!="NO_MANA") ManaPoolComponent(colorless=1,green=1) else ManaPoolComponent()
            val game=LabExecution(source,Step.POSTCOMBAT_MAIN,seeds,pool)
            val selected=when(mode) { "SELF"->game.source;"NONCREATURE"->game.id(artifact);else->if(seeds.isNotEmpty()) game.id(creature) else game.source }
            if(mode=="BORROWED") game.seedResearchPosition { state->state.updateEntity(selected) { c->
                c.with(c.get<CardComponent>()!!.copy(ownerId=game.opponent)).with(OwnerComponent(game.opponent)).with(ControllerComponent(game.player))
            } }
            if(mode=="FOREIGN_CONTROL") game.seedResearchPosition { state->state.updateEntity(selected) { it.with(ControllerComponent(game.opponent)) } }
            if(mode.startsWith("TAPPED")) game.seedResearchPosition { state->state.updateEntity(game.source) { it.with(TappedComponent) } }
            val before=game.nativeState();val initial=game.rawState();val originalRef=before.objectRef(selected)
            val owner=before.getEntity(selected)?.get<OwnerComponent>()?.playerId
            val steps=mutableListOf<PaymentBoundaryStep>()
            fun attempt(label:String,action:GameAction) {
                val rawBefore=game.rawState();val validated=probe.validate(game.nativeState(),action);val size=game.actions.size
                try { game.perform(label,action,allowIllegal=true) } catch(_:LabStopped) { /* Raw native outcome, not the driver's stop, is the evidence. */ }
                val observed=game.actions.getOrNull(size)
                val result=observed?.nativeResult ?: JsonNull
                val outcome=(result as? JsonObject)?.get("outcome") as? JsonObject
                val reason=outcome?.get("reason") as? JsonObject
                steps+=PaymentBoundaryStep(label,validated,outcome?.get("type")?.jsonPrimitive?.content ?: "NOT_OBSERVED",
                    reason?.get("type")?.jsonPrimitive?.content,reason?.get("message")?.jsonPrimitive?.content,
                    rawBefore,observed?.nativeAction ?: JsonNull,result,game.rawState())
            }
            val explicit=mode.contains("EXPLICIT")
            val action=ActivateAbility(game.player,game.source,source.activatedAbilities[binding.nativeIndex].id,
                targets=if(scopeId=="caustic-caterpillar:0") listOf(ChosenTarget.Permanent(if(mode=="BAD_TARGET") game.id(creature) else game.id(artifact))) else emptyList(),
                costPayment=if(scopeId=="viscera-seer:0" && mode!="CHOICE") AdditionalCostPayment(sacrificedPermanents=listOf(selected)) else null,
                paymentStrategy=if(explicit) PaymentStrategy.Explicit(emptyList()) else PaymentStrategy.AutoPay)
            attempt("Submit activation",action)
            val choiceChecks=linkedMapOf<String,Boolean>()
            if(mode=="CHOICE") {
                val decision=game.nativeState().pendingDecision as? SelectCardsDecision
                choiceChecks["native_choice_before_payment"]=steps.last().outcome=="Paused" && decision!=null && game.events.none { it is PermanentsSacrificedEvent }
                choiceChecks["only_controlled_creature_options"]=decision?.options?.toSet()==setOf(game.source,game.id(creature))
                if(decision!=null) {
                    attempt("Submit noncandidate decision response",SubmitDecision(game.player,CardsSelectedResponse(decision.id,listOf(game.id(artifact)))))
                    val invalid=steps.last()
                    choiceChecks["invalid_response_rejected_atomically"]=invalid.outcome=="Rejected" && invalid.before==invalid.after &&
                        invalid.nativeResult.jsonObject.getValue("events").jsonArray.isEmpty() && game.nativeState().pendingDecision?.id==decision.id
                    attempt("Submit valid decision response",SubmitDecision(game.player,CardsSelectedResponse(decision.id,listOf(game.id(creature)))))
                }
            }
            val after=game.nativeState();val last=steps.last();val sacrifice=scopeId=="viscera-seer:0" || scopeId=="evolving-wilds:0"
            val rejected=mode in setOf("NONCREATURE","FOREIGN_CONTROL","NO_MANA","BAD_TARGET") || mode.startsWith("TAPPED")
            val expectation=if(rejected) "Source requirements forbid this submitted payment/declaration: native refusal, no payments/events or state changes. Rules do not prescribe the diagnostic enum." else
                "Complete the declared native payment with exact source/selection and owner destination; ordinary resolving effects remain unreviewed."
            val movement=game.events.filterIsInstance<ZoneChangeEvent>().singleOrNull { it.entityId==selected }
            val checks=if(rejected) linkedMapOf("typed_native_refusal" to (last.outcome=="Rejected"),"unchanged_state" to (initial==game.rawState()),
                "no_native_events" to (steps.single().nativeResult.jsonObject.getValue("events").jsonArray.isEmpty())) else linkedMapOf(
                "native_completion" to (last.outcome=="Done" && after.pendingDecision==null),
                "exact_sacrifice_owner_and_generation" to (!sacrifice || (selected in game.events.filterIsInstance<PermanentsSacrificedEvent>().flatMap { it.permanentIds } &&
                    movement!=null && movement.oldObject==originalRef && movement.newObject!=originalRef && movement.fromZone==Zone.BATTLEFIELD && movement.toZone==Zone.GRAVEYARD &&
                    movement.requestedDestination==Zone.GRAVEYARD && movement.ownerId==owner && movement.lastKnown?.controllerId==game.player && owner!=null && selected in after.getGraveyard(owner))),
                "unrelated_objects_preserved" to before.getBattlefield().filter { it!=selected }.all { it in after.getBattlefield() && before.objectRef(it)==after.objectRef(it) },
                "tap_payment_event" to (!(scopeId=="mind-stone:0" || scopeId=="evolving-wilds:0") || game.events.filterIsInstance<TappedEvent>().any { it.entityId==game.source }),
                "life_preserved" to (before.getEntity(game.player)?.get<LifeTotalComponent>()==after.getEntity(game.player)?.get<LifeTotalComponent>()),
                "mana_state_observed" to if(scopeId=="mind-stone:0") (after.getEntity(game.player)?.get<ManaPoolComponent>()?.colorless==1) else (before.getEntity(game.player)?.get<ManaPoolComponent>()==after.getEntity(game.player)?.get<ManaPoolComponent>()),
                "ordinary_resolution_not_executed" to (scopeId=="mind-stone:0" || after.stack.size==before.stack.size+1))
            checks.putAll(choiceChecks)
            val diagnostic=when { last.outcome=="Rejected" && last.rejection=="IllegalAction"->"VALIDATION_REFUSAL";last.outcome=="Rejected"->"EXECUTION_REFUSAL";
                last.outcome=="Paused"->"NATIVE_PENDING_DECISION";last.outcome=="Done"->"NATIVE_COMPLETION";else->"UNOBSERVED" }
            val result=when { checks.values.any { !it }->"SOURCE_EXPECTATION_CONTRADICTED";diagnostic=="EXECUTION_REFUSAL"->"REFUSAL_SUPPORTED_DIAGNOSTIC_LIMIT";else->"BOUNDED_EXPECTATION_SUPPORTED" }
            scenarios+=PaymentBoundaryScenario(id,scopeId,source.name,definitionDigest(source),expectation,result,diagnostic,checks,steps,
                if(result=="SOURCE_EXPECTATION_CONTRADICTED" && mode=="TAPPED_EXPLICIT") "An unpayable source tap was accepted under Explicit; retain as a current native payment concern, not a structural cost mismatch." else
                    if(result=="SOURCE_EXPECTATION_CONTRADICTED") "Source expectation differs; inspect failed checks and actual native actions without assuming a cause." else
                    if(diagnostic=="EXECUTION_REFUSAL") "Explicit selection was refused during payment after generic affordability validation; not a clean IllegalAction observation." else null)
        }
        for(mode in listOf("SELF","OTHER","BORROWED","FOREIGN_CONTROL","NONCREATURE","CHOICE")) run("selected-creature:$mode","viscera-seer:0",mode)
        run("activation-boundary:unpayable-mana","caustic-caterpillar:0","NO_MANA")
        run("activation-boundary:invalid-target","caustic-caterpillar:0","BAD_TARGET")
        for(scope in listOf("mind-stone:0","evolving-wilds:0")) for(mode in listOf("UNTAPPED_EXPLICIT","TAPPED_AUTO","TAPPED_EXPLICIT")) run("tap-boundary:$scope:$mode",scope,mode)
        return PaymentBoundaryReceipt(applicationRevision=revision,engine=EngineBuild.revision,executedAt=Instant.now().toString(),inputs=CostExamination.inputs,
            harnessSha256=CostExamination.harnessHash,definitions=definitions,scenarios=scenarios)
    }
}
