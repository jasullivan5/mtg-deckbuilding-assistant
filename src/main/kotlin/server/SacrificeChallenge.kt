package dev.mtg.assistant.explorer

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.tooling.CardExporter
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import dev.mtg.assistant.Catalog
import dev.mtg.assistant.EngineBuild
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.extensions.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.time.Instant

@Serializable data class AdversarialFinding(val id: String, val card: String, val defect: String,
    val result: String, val checks: List<PilotCheck>, val definitionSha256: String?, val nativeDefinition: JsonElement?, val limitation: String)
@Serializable data class SacrificeChallengeReport(val engine: String, val requirements: JsonObject, val requirementsSha256: String,
    val frozenMethodSha256: String, val methodUnchanged: Boolean, val reviews: List<PilotReview>,
    val adversarial: List<AdversarialFinding>, val counts: Map<String,Int>,
    val executionState: String, val execution: PilotReceipt?,val adversarialExecution:List<PilotExecution> = emptyList(),
    val reviewStatus: String = "PENDING_SEPARATE_REVIEW_NOT_INDEPENDENTLY_QUALIFIED",
    val limits: List<String> = listOf("Same agent authored and challenged this method; source-first and held-out do not mean independent certification.",
        "The eight original checks are unchanged. Target presence is not target legality; activation cost role is not correct mana quantity or activation window.",
        "Current-source printing mismatch may contradict the original identity check even when the global audit confidently associates the physical Oracle identity. That is a method binding limit, not automatically a mistranslation.",
        "The nominal owner-graveyard fact does not execute replacements or foreign ownership. Object-generation protection is execution-only.",
        "Augur of Skulls and Qasali Pridemage remain missing definitions, not newly integrated cards. Non-sacrifice search, discard and destruction results are outside the reviewed scope."))

/** Bounded challenge of the unchanged method. Mutants exist only in a private, restored registry. */
internal object SacrificeChallenge {
    private val json=Json { ignoreUnknownKeys=true;encodeDefaults=true }
    val text by lazy { javaClass.getResourceAsStream("/retained-evidence/fidelity/sacrifice-challenge/requirements.json")!!.bufferedReader().use { it.readText() } }
    val requirements by lazy { json.parseToJsonElement(text).jsonObject }
    val hash get()=evidenceTextDigest(text)
    val scopes by lazy { json.decodeFromJsonElement<List<PilotScope>>(requirements.getValue("scopes")) }
    private fun native(card: CardDefinition)=json.parseToJsonElement(CardExporter.exportToJson(card))

    fun report(): SacrificeChallengeReport {
        val catalog=Catalog() // Never overlay the API/discovery catalog with adversarial definitions.
        val reviews=scopes.map { SacrificePilot.review(catalog,it) }
        val findings=mutableListOf<AdversarialFinding>()
        fun challenge(id: String, scope: PilotScope, defect: String, mutant: CardDefinition, limit: String="") {
            val original=requireNotNull(catalog.card(scope.card))
            val review=try { catalog.registry.register(mutant);SacrificePilot.review(catalog,scope) } finally { catalog.registry.register(original) }
            val compared=review.checks.filter { it.requirement != "OBJECT_GENERATION_IN_EXECUTION" }
            val result=when { compared.any { it.state=="CONTRADICTED" }->"DETECTED_CONTRADICTION";compared.any { it.state!="SUPPORTED" }->"NOT_ACCEPTED_UNRESOLVED";else->"MISSED_BY_STATIC_METHOD" }
            findings+=AdversarialFinding(id,scope.card,defect,result,review.checks,definitionDigest(mutant),native(mutant),limit)
        }
        val mogg=SacrificePilot.scopes.single { it.card=="Mogg Fanatic" };val m=catalog.card(mogg.card)!!;val a=m.activatedAbilities.single()
        fun activated(c: CardDefinition, ability: com.wingedsheep.sdk.scripting.ActivatedAbility)=c.copy(script=c.script.copy(activatedAbilities=listOf(ability)))
        challenge("wrong-source",mogg,"Replace exact-source payment with sacrifice of the granting permanent",activated(m,a.copy(cost=AbilityCost.SacrificeGrantingPermanent)),"Lack of a recognized self fact is unresolved, not proof of the correct alternative.")
        val spark=SacrificePilot.scopes.single { it.card=="Spark Elemental" };val s=catalog.card(spark.card)!!
        challenge("effect-not-cost",mogg,"Move sacrifice from activation payment to resolving effect",activated(m,a.copy(cost=AbilityCost.Free,effect=s.triggeredAbilities.single().effect)))
        challenge("missing-target",mogg,"Remove required target declaration",activated(m,a.copy(targetRequirements=emptyList())))
        val wilds=SacrificePilot.scopes.single { it.card=="Evolving Wilds" };val w=catalog.card(wilds.card)!!
        challenge("missing-tap",wilds,"Remove tap payment while preserving source sacrifice",activated(w,w.activatedAbilities.single().copy(cost=AbilityCost.SacrificeSelf)))
        challenge("wrong-arrival-type",mogg,"Replace creature characteristics with noncreature artifact characteristics",m.copy(typeLine=TypeLine.parse("Artifact"),creatureStats=null))
        val trigger=s.triggeredAbilities.single();val event=trigger.trigger as EventPattern.StepEvent
        challenge("wrong-trigger-step",spark,"Move end-step sacrifice trigger to upkeep",s.copy(script=s.script.copy(triggeredAbilities=listOf(trigger.copy(trigger=event.copy(step=Step.UPKEEP))))))
        val caterpillar=scopes.single { it.card=="Caustic Caterpillar" };val c=catalog.card(caterpillar.card)!!;val ca=c.activatedAbilities.single()
        challenge("missing-mana",caterpillar,"Remove required {1}{G} mana payment",activated(c,ca.copy(cost=AbilityCost.SacrificeSelf)),"The frozen method checks role and tap only; it has no full-cost requirement.")
        challenge("wrong-target-filter",caterpillar,"Replace artifact/enchantment target and destruction with any-target damage",activated(c,ca.copy(targetRequirements=a.targetRequirements,effect=a.effect)),"Target presence remains true. The method does not establish target filter or non-sacrifice effect fidelity.")
        findings+=AdversarialFinding("generation-guard",spark.card,"Lose source object generation across a zone round trip","EXECUTION_DEPENDENT",emptyList(),null,null,"The frozen static table explicitly leaves this untested. Historical Spark execution failed with source self-sacrifice still structurally present; current adverse execution is a separate observation.")
        findings+=AdversarialFinding("owner-replacement",mogg.card,"Wrong owner destination or replacement handling","EXECUTION_DEPENDENT",emptyList(),null,null,"Nominal owner-graveyard facts are not runtime replacement or borrowed-source verification. No engine mutation was introduced.")
        val receipt=javaClass.getResourceAsStream("/sacrifice-challenge-execution.json")?.bufferedReader()?.use { json.decodeFromString<PilotReceipt>(it.readText()) }
        val current=receipt!=null && receipt.engine==EngineBuild.revision && receipt.requirementsSha256==hash && receipt.harnessSha256==harnessHash &&
            reviews.filter { it.definitionSha256!=null }.all { receipt.definitions[it.scope.card]==it.definitionSha256 } &&
            receipt.definitions["mutant:missing-mana"]==findings.single { it.id=="missing-mana" }.definitionSha256
        return SacrificeChallengeReport(EngineBuild.revision,requirements,hash,requirements.getValue("methodSha256").jsonPrimitive.content,
            requirements.getValue("methodSha256").jsonPrimitive.content==SacrificePilot.harnessHash,reviews,findings,
            linkedMapOf("source_scopes" to reviews.size,"available_native_scopes" to reviews.count { it.definitionSha256!=null },"missing_native_scopes" to reviews.count { it.definitionSha256==null },
                "held_out_available_scopes" to reviews.count { it.scope.heldOut && it.definitionSha256!=null },"detected_contradictions" to findings.count { it.result=="DETECTED_CONTRADICTION" },
                "not_accepted_unresolved" to findings.count { it.result=="NOT_ACCEPTED_UNRESOLVED" },"missed_adversarial_defects" to findings.count { it.result=="MISSED_BY_STATIC_METHOD" },"execution_dependent_classes" to findings.count { it.result=="EXECUTION_DEPENDENT" }),
            if(receipt==null) "MISSING" else if(current) "RECORDED_CURRENT_BINDINGS" else "STALE",receipt,receipt?.scenarios.orEmpty().filter { it.scenarioId.startsWith("mutant:") })
    }
    val harnessHash: String by lazy { java.util.Properties().apply { SacrificeChallenge::class.java.getResourceAsStream("/challenge-harness.properties")!!.use(::load) }.getProperty("sha256") }

    /** Same activation-payment pattern, with target/mana parameters; no new permanent Lab scenarios. */
    fun execute(catalog: Catalog, revision: String): PilotReceipt {
        val available=scopes.filter { catalog.card(it.card)!=null }
        fun payment(scope:PilotScope,card:CardDefinition,omitResources:Boolean,prefix:String="challenge"):PilotExecution {
                val targeted=scope.targetRequired
                val support=if(targeted) listOf(LabSeed(catalog.card("Lifespark Spellbomb")!!,Zone.BATTLEFIELD)) else emptyList()
                val game=LabExecution(card,Step.POSTCOMBAT_MAIN,support,if(omitResources) ManaPoolComponent() else ManaPoolComponent(green=1,colorless=1))
                val before=game.checkpoint()
                val expectedRejection=omitResources && targeted
                val state=try {
                    game.perform("Activate source sacrifice ${if(omitResources) "without mana" else "with resources"}",ActivateAbility(game.player,game.source,card.activatedAbilities.single().id,
                        if(targeted) listOf(ChosenTarget.Permanent(game.id(support.single().card))) else emptyList()),allowIllegal=expectedRejection)
                    if(if(expectedRejection) game.actions.single().outcome=="ILLEGAL_ACTION" && game.sourceMoves.isEmpty() && game.checkpoint().sourceRef==before.sourceRef
                        else game.sourceMoves.singleOrNull()?.let { it.wasSacrificed && it.oldObject==before.sourceRef && it.newObject!=it.oldObject && it.toZone==Zone.GRAVEYARD && it.ownerId==game.player }==true) "AGREES" else "DIFFERS"
                } catch(e:Exception) { "INCOMPLETE" }
                return PilotExecution("$prefix:${scope.printingId}-${if(omitResources) "no-mana" else "paid"}",scope.card,
                    if(expectedRejection) "Missing required mana rejects before sacrifice" else "Exact source sacrifices as payment; resolving effect remains untested",state,
                    buildJsonObject { put("expectedRejection",expectedRejection);put("initial",reportJson.encodeToJsonElement(before));put("actions",reportJson.encodeToJsonElement(game.actions));put("final",reportJson.encodeToJsonElement(game.checkpoint())) })
        }
        val observations=available.flatMap { scope->(if(scope.targetRequired) listOf(false,true) else listOf(false)).map { omit->payment(scope,catalog.card(scope.card)!!,omit) } }
        val scope=available.single { it.card=="Caustic Caterpillar" };val card=catalog.card(scope.card)!!
        val mutant=card.copy(script=card.script.copy(activatedAbilities=listOf(card.activatedAbilities.single().copy(cost=AbilityCost.SacrificeSelf))))
        val adverse=payment(scope,mutant,true,"mutant:missing-mana")
        return PilotReceipt(revision,EngineBuild.revision,Instant.now().toString(),hash,harnessHash,available.associate { it.card to definitionDigest(catalog.card(it.card)!!) } + ("mutant:missing-mana" to definitionDigest(mutant)),observations+adverse)
    }
}
