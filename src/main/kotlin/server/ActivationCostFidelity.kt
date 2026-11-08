package dev.mtg.assistant.explorer

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.serialization.CardSerialization
import com.wingedsheep.sdk.tooling.CardExporter
import dev.mtg.assistant.Catalog
import dev.mtg.assistant.EngineBuild
import dev.mtg.assistant.coverage.*
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.extensions.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.util.Properties
import java.util.zip.GZIPInputStream

@Serializable data class ActivationCostScope(val id:String,val card:String,val sourceAbility:String,val sourceOrdinal:Int,
    val sourceFile:String,val sourceSha256:String,val oracleId:String,val printingId:String,val expected:CostTree,val heldOut:Boolean,
    val sourceZone:String,val payer:String,val sacrificeController:String,val sacrificeDestination:String,val dependencies:List<String>)
@Serializable data class ActivationCostBinding(val scopeId:String,val nativeName:String,val nativeIndex:Int,val nativeAbilityId:String,val definitionSha256:String)
@Serializable data class ActivationCostReview(val scope:ActivationCostScope,val binding:ActivationCostBinding?,val definitionSha256:String?,
    val actionId:String?,val nativeAbilityId:String?,val nativeCost:JsonElement,val comparison:CostComparison,
    val identityState:String,val reasons:List<String>,val declarationAndResolution:JsonElement,val nativeDefinition:JsonElement)
@Serializable data class CostMutation(val id:String,val scopeId:String,val card:String,val defect:String,val result:String,
    val comparison:CostComparison,val definitionSha256:String,val nativeDefinition:JsonElement,val limits:String)
@Serializable data class ActivationCostReport(val schemaVersion:Int=1,val measurementDefinition:String="printed-base-activation-cost-v1",
    val applicationRevision:String,val engine:String,val requirements:JsonObject,val requirementsSha256:String,val bindingsSha256:String,
    val reviews:List<ActivationCostReview>,val mutations:List<CostMutation>,val counts:Map<String,Int>,val potentialReach:Map<String,Int>,
    val supportedFamilies:Map<String,Int>,val executionState:String,val execution:CostExecutionReceipt?,
    val qualification:String="PENDING_SEPARATE_REVIEW_NOT_WHOLE_ABILITY_OR_CARD_QUALIFICATION",
    val limits:List<String> = listOf("Complete supported printed base payment only. Declaration permissions, zones/timing, targets, contextual modifiers and resolving effects remain separate dependencies.",
        "Only fixed colored/generic/colorless mana, mandatory conjunction, source tap/untap, self sacrifice, simple payer-controlled creature selection and fixed life are structurally supported.",
        "Specialized alternative/conditional/dynamic costs and unknown components never receive partial-cost acceptance. Raw native structure is retained.",
        "Source-bound sacrifice goes nominally to its owner's graveyard before replacements. Structure does not prove actual affordability, replacement outcomes or all source generations.",
        "Technical held-out controls were source-first but public mechanics and prior work were known to the same agent. No independent approval is issued.",
        "The PR40 wrong target/effect-filter miss remains outside payment scope. The original eight-check method and historical execution failures are unchanged."))

internal object ActivationCostFidelity {
    private val json=Json { encodeDefaults=true }
    private fun resource(path:String)=requireNotNull(javaClass.getResourceAsStream(path)).bufferedReader(Charsets.UTF_8).use { it.readText() }
    val requirementsText by lazy { resource("/retained-evidence/fidelity/activation-cost/requirements.json") }
    val requirements by lazy { json.parseToJsonElement(requirementsText).jsonObject }
    val requirementsHash get()=evidenceTextDigest(requirementsText)
    val scopes by lazy { json.decodeFromJsonElement<List<ActivationCostScope>>(requirements.getValue("scopes")) }
    val bindingsText by lazy { resource("/retained-evidence/fidelity/activation-cost/bindings.json") }
    val bindingsHash get()=evidenceTextDigest(bindingsText)
    val bindings by lazy { json.decodeFromJsonElement<List<ActivationCostBinding>>(json.parseToJsonElement(bindingsText).jsonObject.getValue("bindings")) }
    private val reference by lazy { retainedGlobalBaseline() }
    private val referenceIdentities by lazy { reference.records.associateBy { it.oracleId } }
    private val priorReviews by lazy {
        GZIPInputStream(requireNotNull(javaClass.getResourceAsStream("/verification-baseline.json.gz"))).bufferedReader().use { reader ->
            json.parseToJsonElement(reader.readText()).jsonObject.getValue("evidence").jsonArray.filter { row ->
                row.jsonObject.getValue("freshness").jsonPrimitive.content=="CURRENT" && row.jsonObject.getValue("record").jsonObject.getValue("kind").jsonPrimitive.content=="SOURCE_REVIEW"
            }.map { row->row.jsonObject.getValue("record").jsonObject.let { it.getValue("oracleId").jsonPrimitive.content to it.getValue("scopeId").jsonPrimitive.content } }.toSet()
        }
    }
    val harnessHash:String by lazy { Properties().apply { ActivationCostFidelity::class.java.getResourceAsStream("/activation-cost-harness.properties")!!.use(::load) }.getProperty("sha256") }
    fun ability(catalog:Catalog,scope:ActivationCostScope)=catalog.card(bindings.single { it.scopeId==scope.id }.nativeName)!!.activatedAbilities[bindings.single { it.scopeId==scope.id }.nativeIndex]
    private fun native(card:CardDefinition)=json.parseToJsonElement(CardExporter.exportToJson(card))

    fun review(catalog:Catalog,scope:ActivationCostScope):ActivationCostReview {
        val source=resource("/retained-evidence/"+scope.sourceFile.removePrefix("src/test/resources/"))
        require(evidenceTextDigest(source)==scope.sourceSha256) { "Cost source hash changed: ${scope.id}" }
        val oracle=json.parseToJsonElement(source).jsonObject.getValue("card").jsonObject
        require(oracle.getValue("oracle_id").jsonPrimitive.content==scope.oracleId && oracle.getValue("id").jsonPrimitive.content==scope.printingId)
        val binding=bindings.singleOrNull { it.scopeId==scope.id }
        val card=binding?.nativeName?.let(catalog::card)
        val ability=card?.activatedAbilities?.getOrNull(binding!!.nativeIndex)
        val definition=card?.let(::definitionDigest)
        val association=referenceIdentities[scope.oracleId]
        val a=card?.let { PathwayExtractor(inspectCardEntries=true).extract(listOf(it)).actions.singleOrNull { action->action.path=="script.activatedAbilities[${binding!!.nativeIndex}]" } }
        val encoded=ability?.let { CardSerialization.json.encodeToJsonElement(ActivatedAbility.serializer(),it).jsonObject }
        val id=encoded?.get("id")?.jsonPrimitive?.content
        val reasons=buildList {
            if(binding==null || card==null || ability==null) add("Exact native ability binding unavailable")
            if(definition!=binding?.definitionSha256 || id!=binding?.nativeAbilityId) add("Inspected definition or ability identity changed; binding review required")
            if(association?.association!="CONFIDENT" || association.nativeName!=card?.name || association.poolState!="ELIGIBLE") add("No confident legal physical association in the existing reference baseline")
            if(a==null) add("Existing native action record unavailable")
            if(scope.sourceZone!="BATTLEFIELD" || scope.payer!="ABILITY_CONTROLLER" || scope.sacrificeController!="PAYER" || scope.sacrificeDestination!="OWNER_GRAVEYARD_BEFORE_REPLACEMENTS") add("Source payment context outside the supported contract")
        }
        val observed=ability?.cost?.let(::projectActivationCost) ?: CostTree("UNRESOLVED_BINDING")
        val compared=compareActivationCost(scope.expected,observed)
        val result=if(reasons.isEmpty()) compared else compared.copy(state="UNRESOLVED",reasons=compared.reasons+reasons)
        val nativePrinting=card?.let(::printingIdentity)
        return ActivationCostReview(scope,binding,definition,a?.id,id,ability?.cost?.let { CardSerialization.json.encodeToJsonElement(AbilityCost.serializer(),it) } ?: JsonNull,
            result,if(reasons.isNotEmpty()) "UNRESOLVED" else if(nativePrinting==scope.printingId) "CONFIDENT_REFERENCE_ORACLE_AND_PRINTING" else "CONFIDENT_REFERENCE_ORACLE_DIFFERENT_PRINTING",
            reasons+if(scope.card=="Sakura-Tribe Elder") listOf("Earlier exact-printing concern retained in the original method; this cost comparison uses the existing confident physical Oracle association, not a silent correction") else emptyList(),
            encoded?.let { JsonObject(it.filterKeys { key->key!="cost" }) } ?: JsonNull,card?.let(::native) ?: JsonNull)
    }

    fun mutationCards(catalog:Catalog):List<Pair<CostMutation,CardDefinition>> {
        val result=mutableListOf<Pair<CostMutation,CardDefinition>>()
        fun mutate(id:String,scope:ActivationCostScope,defect:String,outside:Boolean=false,transform:(ActivatedAbility)->ActivatedAbility) {
            val binding=bindings.single { it.scopeId==scope.id };val card=catalog.card(binding.nativeName)!!
            val old=card.activatedAbilities[binding.nativeIndex];val replacement=transform(old)
            val mutant=card.copy(script=card.script.copy(activatedAbilities=card.activatedAbilities.mapIndexed { index,a->if(index==binding.nativeIndex) replacement else a }))
            val comparison=compareActivationCost(scope.expected,projectActivationCost(replacement.cost))
            val state=if(outside) "OUTSIDE_PAYMENT_SCOPE" else when(comparison.state) { "CONTRADICTED"->"DETECTED_CONTRADICTION";"UNRESOLVED"->"REJECTED_UNRESOLVED";else->"MISSED_BY_COMPLETE_COST_METHOD" }
            result+=CostMutation(id,scope.id,scope.card,defect,state,comparison,definitionDigest(mutant),native(mutant),if(outside) "The unchanged payment agrees but target/effect fidelity is unreviewed. PR40's miss is retained, not repaired by a cost-only pass." else "Isolated incorrect definition; never registered in the production catalog or approval ledger") to mutant
        }
        val caterpillar=scopes.single { it.card=="Caustic Caterpillar" };val wilds=scopes.single { it.card=="Evolving Wilds" };val mogg=scopes.single { it.card=="Mogg Fanatic" };val greed=scopes.single { it.card=="Greed" }
        fun mana(text:String)=AbilityCost.Atom(CostAtom.Mana(ManaCost.parse(text)))
        mutate("missing-mana",caterpillar,"Remove required {1}{G}, retain sacrifice") { it.copy(cost=AbilityCost.SacrificeSelf) }
        mutate("wrong-mana-amount",caterpillar,"Replace {1}{G} with {G}") { it.copy(cost=AbilityCost.Composite(listOf(mana("{G}"),AbilityCost.SacrificeSelf))) }
        mutate("wrong-mana-color",caterpillar,"Replace {1}{G} with {1}{R}") { it.copy(cost=AbilityCost.Composite(listOf(mana("{1}{R}"),AbilityCost.SacrificeSelf))) }
        mutate("generic-not-colorless",caterpillar,"Replace generic {1} with required {C}") { it.copy(cost=AbilityCost.Composite(listOf(mana("{C}{G}"),AbilityCost.SacrificeSelf))) }
        mutate("missing-sacrifice",caterpillar,"Retain mana but omit mandatory sacrifice") { it.copy(cost=mana("{1}{G}")) }
        mutate("missing-tap",wilds,"Remove tap, retain source sacrifice") { it.copy(cost=AbilityCost.SacrificeSelf) }
        mutate("wrong-sacrifice-source",mogg,"Sacrifice granting permanent rather than source") { it.copy(cost=AbilityCost.SacrificeGrantingPermanent) }
        mutate("wrong-sacrifice-selection",mogg,"Choose a controlled creature instead of exact source") { it.copy(cost=AbilityCost.Atom(CostAtom.Sacrifice(GameObjectFilter.Creature,excludeSelf=true))) }
        val sacrificeEffect=catalog.card("Spark Elemental")!!.triggeredAbilities.single().effect
        mutate("cost-to-effect",mogg,"Move sacrifice from payment to resolution") { it.copy(cost=AbilityCost.Free,effect=sacrificeEffect) }
        mutate("alternative-not-combined",caterpillar,"Replace mandatory conjunction with native Forage choice; generic mana-or-sacrifice constructor is unavailable") { it.copy(cost=AbilityCost.Forage) }
        mutate("wrong-life",greed,"Pay one life instead of two") { it.copy(cost=AbilityCost.Composite(listOf(mana("{B}"),AbilityCost.Atom(CostAtom.PayLife(1))))) }
        val anyTarget=ability(catalog,mogg)
        mutate("wrong-target-effect",caterpillar,"Keep correct cost but change target/effect",outside=true) { it.copy(targetRequirements=anyTarget.targetRequirements,effect=anyTarget.effect) }
        return result
    }
    fun mutations(catalog:Catalog)=mutationCards(catalog).map { it.first }

    fun report(catalog:Catalog,revision:String):ActivationCostReport {
        val baseline=reference
        requireVerificationIdentityBinding(baseline,physicalCards(catalog.cards).map { nativeIdentity(it,catalog.provenance(it.name).origin) })
        val reviews=scopes.map { review(catalog,it) };val mutations=mutations(catalog)
        val legal=baseline.records.filter { it.poolState=="ELIGIBLE" && it.association=="CONFIDENT" }.mapNotNull { it.nativeName?.let(catalog::card) }
        val potential=legal.flatMap { card->card.activatedAbilities.map { card.name to projectActivationCost(it.cost) } }
        val supported=potential.filter { canonicalActivationCost(it.second)!=null }
        val execution=javaClass.getResourceAsStream("/activation-cost-execution.json")?.bufferedReader()?.use { json.decodeFromString<CostExecutionReceipt>(it.readText()) }
        val current=execution!=null && execution.engine==EngineBuild.revision && execution.requirementsSha256==requirementsHash && execution.bindingsSha256==bindingsHash && execution.harnessSha256==harnessHash &&
            reviews.all { execution.definitions[it.scope.id]==it.definitionSha256 } && mutations.all { execution.mutationDefinitions[it.id]==it.definitionSha256 } &&
            execution.supportDefinitions.keys==setOf("Mogg Fanatic","Lifespark Spellbomb") && execution.supportDefinitions.all { (name,hash)->catalog.card(name)?.let(::definitionDigest)==hash }
        val counts=linkedMapOf("source_reviewed_ability_scopes" to reviews.size,"distinct_reviewed_physical_cards" to reviews.map { it.scope.oracleId }.distinct().size,
            "source_requirement_nodes" to scopes.sumOf { costRequirementCount(it.expected) },"supported_complete_cost_scopes" to reviews.count { it.comparison.state=="SUPPORTED" },
            "contradicted_cost_scopes" to reviews.count { it.comparison.state=="CONTRADICTED" },"unresolved_cost_scopes" to reviews.count { it.comparison.state=="UNRESOLVED" },
            "supported_requirement_nodes" to reviews.filter { it.comparison.state=="SUPPORTED" }.sumOf { costRequirementCount(it.scope.expected) },"unreviewed_declaration_resolution_scopes" to reviews.size,
            "held_out_scopes" to reviews.count { it.scope.heldOut },"held_out_supported_scopes" to reviews.count { it.scope.heldOut && it.comparison.state=="SUPPORTED" },
            "previously_reviewed_scopes_strengthened" to reviews.count { it.comparison.state=="SUPPORTED" && it.scope.oracleId to it.actionId in priorReviews },
            "additional_source_reviewed_scopes" to reviews.count { it.comparison.state=="SUPPORTED" && it.scope.oracleId to it.actionId !in priorReviews },
            "additional_source_reviewed_cards" to reviews.filter { it.comparison.state=="SUPPORTED" && it.scope.oracleId !in priorReviews.map { p->p.first } }.map { it.scope.oracleId }.distinct().size,
            "defects_detected" to mutations.count { it.result=="DETECTED_CONTRADICTION" },"defects_unresolved" to mutations.count { it.result=="REJECTED_UNRESOLVED" },
            "defects_missed" to mutations.count { it.result=="MISSED_BY_COMPLETE_COST_METHOD" },"outside_payment_scope" to mutations.count { it.result=="OUTSIDE_PAYMENT_SCOPE" },
            "execution_observations_current" to if(current) execution!!.observations.count { it.executed } else 0,
            "production_execution_observations_current" to if(current) execution!!.observations.count { it.executed && !it.mutant } else 0,
            "execution_patterns_current" to if(current) execution!!.observations.map { it.pattern }.distinct().size else 0,
            "declaration_fixture_scope_records" to ActivationCostExecution.declarationFixtures.size)
        return ActivationCostReport(applicationRevision=revision,engine=EngineBuild.revision,requirements=requirements,requirementsSha256=requirementsHash,bindingsSha256=bindingsHash,
            reviews=reviews,mutations=mutations,counts=counts,potentialReach=mapOf("confident_commander_legal_native_cards" to legal.size,"native_activated_ability_scopes" to potential.size,
                "structurally_supported_ability_scopes" to supported.size,"structurally_supported_physical_cards" to supported.map { it.first }.distinct().size,"unresolved_ability_scopes" to potential.size-supported.size),
            supportedFamilies=supported.groupingBy { canonicalActivationCost(it.second)!!.map { term->term.substringBefore(':') }.sorted().joinToString(" + ").ifEmpty { "FREE" } }.eachCount().toSortedMap(),
            executionState=if(execution==null) "MISSING" else if(current) "RECORDED_CURRENT_BINDINGS" else "STALE",execution=execution)
    }
}
