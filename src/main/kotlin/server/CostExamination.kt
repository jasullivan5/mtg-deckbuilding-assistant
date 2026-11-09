package dev.mtg.assistant.explorer

import dev.mtg.assistant.Catalog
import dev.mtg.assistant.EngineBuild
import dev.mtg.assistant.coverage.*
import dev.mtg.assistant.discovery.reportJson
import dev.mtg.assistant.extensions.definitionDigest
import dev.mtg.assistant.extensions.evidenceTextDigest
import dev.mtg.assistant.extensions.evidenceDigest
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.tooling.CardExporter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.util.Properties
import java.util.zip.GZIPInputStream

@Serializable data class CostExaminationReport(val schemaVersion:Int=1,val applicationRevision:String,val engine:String,
    val reviewStatus:String="ISOLATED_AGENT_EXAMINATION_PENDING_EXTERNAL_QUALIFICATION",val sourceState:String,val nativeState:String,
    val phaseFiles:Map<String,String>,val source:JsonObject,val native:JsonObject,val adjudicationState:String,val adjudication:JsonObject?,val additionalChallenge:CostMutation,
    val executionState:String,val execution:PaymentBoundaryReceipt?,val counts:Map<String,Int>,val potentialTapReach:Map<String,Int>,
    val limits:List<String> = listOf("No-history source-first agent, same provider and coordinator; instruction-only isolation, not independent human approval.",
        "Printed base payment agreement does not establish full activation legality, resolving-effect fidelity or whole-card completeness.",
        "Old execution receipts and baselines remain unchanged. Reexamining an observation does not create new execution evidence.",
        "Potential tap reach enumerates primary-definition cost trees only; additional conditions may prevent the observed behavior. These definitions are not source-reviewed or confirmed affected."))

internal object CostExamination {
    val inputNames=listOf("inputs.json","source-examination.json","native-inputs.json","native-examination.json","execution-requirements.json")
    fun text(name:String)=requireNotNull(javaClass.getResourceAsStream("/retained-evidence/fidelity/cost-examination/$name")).bufferedReader(Charsets.UTF_8).use { it.readText() }
    fun data(name:String)=reportJson.parseToJsonElement(text(name)).jsonObject
    val inputs:Map<String,String> by lazy { inputNames.associateWith { evidenceTextDigest(text(it)) } }
    fun frozenArtifactCurrent(name:String,actualLfHash:String=inputs.getValue(name))=
        data("hash-bindings.json").getValue("files").jsonObject.getValue(name).jsonObject.getValue("utf8LfSha256").jsonPrimitive.content==actualLfHash
    /** Preserve the actual freeze bytes; Git's LF transport must neither invalidate nor rewrite the recorded raw hash. */
    fun frozenHash(name:String):String {
        val binding=data("hash-bindings.json").getValue("files").jsonObject.getValue(name).jsonObject
        val raw=GZIPInputStream(requireNotNull(javaClass.getResourceAsStream("/retained-evidence/fidelity/cost-examination/"+binding.getValue("rawArchive").jsonPrimitive.content))).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val hash=evidenceDigest(raw)
        require(hash==binding.getValue("rawFreezeSha256").jsonPrimitive.content && evidenceTextDigest(raw)==binding.getValue("utf8LfSha256").jsonPrimitive.content) { "Frozen raw/canonical archive no longer agrees: $name" }
        return hash
    }
    val harnessHash:String by lazy { Properties().apply { CostExamination::class.java.getResourceAsStream("/cost-boundary-harness.properties")!!.use(::load) }.getProperty("sha256") }
    private fun optional(name:String)=javaClass.getResourceAsStream(name)?.bufferedReader(Charsets.UTF_8)?.use { reportJson.parseToJsonElement(it.readText()).jsonObject }

    fun bindingsCurrent(record:PaymentBoundaryReceipt,engine:String,harness:String,currentInputs:Map<String,String>,definitions:Map<String,String>) =
        record.engine==engine && record.harnessSha256==harness && record.inputs==currentInputs && record.definitions==definitions &&
            record.scenarios.map { it.id }.distinct().size==record.scenarios.size && record.scenarios.all { scenario->scenario.steps.isNotEmpty() && scenario.steps.all { it.action!=JsonNull && it.nativeResult!=JsonNull } }

    fun additionalChallenge(catalog:Catalog):CostMutation {
        val binding=ActivationCostFidelity.bindings.single { it.scopeId=="viscera-seer:0" }
        val card=catalog.card(binding.nativeName)!!
        val mutant=card.copy(script=card.script.copy(activatedAbilities=card.activatedAbilities.mapIndexed { i,a->
            if(i==binding.nativeIndex) a.copy(cost=AbilityCost.Atom(CostAtom.Sacrifice(GameObjectFilter.Creature,excludeSelf=true))) else a
        }))
        val expected=ActivationCostFidelity.scopes.single { it.id==binding.scopeId }.expected
        val comparison=compareActivationCost(expected,projectActivationCost(mutant.activatedAbilities[binding.nativeIndex].cost))
        return CostMutation("exclude-legal-source",binding.scopeId,card.name,"Incorrectly require another creature, excluding the legal source",
            if(comparison.state=="CONTRADICTED") "DETECTED_CONTRADICTION" else "UNRESOLVED_OR_MISSED",comparison,definitionDigest(mutant),
            reportJson.parseToJsonElement(CardExporter.exportToJson(mutant)),"One examination-driven test-only mutation; no production catalog or qualification change")
    }

    fun report(catalog:Catalog,revision:String):CostExaminationReport {
        val manifest=data("inputs.json");val source=data("source-examination.json");val native=data("native-examination.json");val packet=data("native-inputs.json");val plan=data("execution-requirements.json")
        val sourceCurrent=frozenArtifactCurrent("inputs.json") && frozenArtifactCurrent("source-examination.json") && plan.getValue("sourcePhaseSha256").jsonPrimitive.content==frozenHash("source-examination.json") && source.getValue("manifest").jsonObject.getValue("actualSha256").jsonPrimitive.content==frozenHash("inputs.json") &&
            manifest.getValue("sourceOnlyInputs").jsonArray.all { row->
                val r=row.jsonObject;val path=r.getValue("path").jsonPrimitive.content.removePrefix("src/test/resources/")
                javaClass.getResourceAsStream("/retained-evidence/$path")?.bufferedReader(Charsets.UTF_8)?.use { evidenceTextDigest(it.readText()) }==r.getValue("sha256").jsonPrimitive.content
            }
        val nativeCurrent=sourceCurrent && frozenArtifactCurrent("native-inputs.json") && frozenArtifactCurrent("native-examination.json") && native.getValue("frozenSource").jsonObject.getValue("actualSha256").jsonPrimitive.content==frozenHash("source-examination.json") &&
            native.getValue("nativeInputs").jsonObject.getValue("actualSha256").jsonPrimitive.content==frozenHash("native-inputs.json") &&
            plan.getValue("sourcePhaseSha256").jsonPrimitive.content==frozenHash("source-examination.json") && plan.getValue("nativePhaseSha256").jsonPrimitive.content==frozenHash("native-examination.json") &&
            packet.getValue("engine").jsonPrimitive.content==EngineBuild.revision && packet.getValue("nativeDefinitions").jsonArray.all { row->
                val r=row.jsonObject;catalog.card(r.getValue("name").jsonPrimitive.content)?.let(::definitionDigest)==r.getValue("definitionSha256").jsonPrimitive.content
            }
        val receipt=optional("/cost-boundary-execution.json")?.let { reportJson.decodeFromJsonElement<PaymentBoundaryReceipt>(it) }
        val definitions=receipt?.let { (it.scenarios.map { s->s.card }+listOf("Mogg Fanatic","Lifespark Spellbomb")).distinct().associateWith { name->catalog.card(name)?.let(::definitionDigest).orEmpty() } }.orEmpty()
        val current=receipt!=null && nativeCurrent && bindingsCurrent(receipt,EngineBuild.revision,harnessHash,inputs,definitions)
        val currentAdjudication=optional("/retained-evidence/fidelity/cost-examination/adjudication-current.json")
        val adjudication=currentAdjudication ?: optional("/retained-evidence/fidelity/cost-examination/adjudication.json")
        val adjudicated=adjudication?.get("bindings") as? JsonObject
        val adjudicationCurrent=current && adjudicated!=null && adjudicated["engine"]?.jsonPrimitive?.content==EngineBuild.revision &&
            adjudicated["sourcePhaseSha256"]?.jsonPrimitive?.content==frozenHash("source-examination.json") && adjudicated["nativePhaseSha256"]?.jsonPrimitive?.content==frozenHash("native-examination.json") &&
            adjudicated["executionReceiptSha256"]?.jsonPrimitive?.content==javaClass.getResourceAsStream("/cost-boundary-execution.json")?.bufferedReader(Charsets.UTF_8)?.use { evidenceTextDigest(it.readText()) }
        val phases=inputs+mapOf("hash-bindings.json" to evidenceTextDigest(text("hash-bindings.json")))+
            (if(adjudication!=null) mapOf("adjudication.json" to evidenceTextDigest(text("adjudication.json"))) else emptyMap())+
            (if(currentAdjudication!=null) mapOf("adjudication-current.json" to evidenceTextDigest(text("adjudication-current.json"))) else emptyMap())
        val eligible=retainedGlobalBaseline().records.filter { it.poolState=="ELIGIBLE" && it.association=="CONFIDENT" }.mapNotNull { it.nativeName?.let(catalog::card) }
        val tap=eligible.flatMap { card->card.activatedAbilities.map { card.name to canonicalActivationCost(projectActivationCost(it.cost)) } }.filter { it.second?.contains("TAP_SELF")==true }
        val observations=if(current) receipt!!.scenarios else emptyList()
        return CostExaminationReport(applicationRevision=revision,engine=EngineBuild.revision,sourceState=if(sourceCurrent) "CURRENT_SOURCE_BINDINGS" else "STALE_SOURCE_BINDINGS",
            nativeState=if(nativeCurrent) "CURRENT_NATIVE_BINDINGS" else "STALE_NATIVE_BINDINGS",phaseFiles=phases,source=source,native=native,adjudicationState=if(adjudication==null) "MISSING" else if(adjudicationCurrent) "CURRENT_BINDINGS" else "STALE",adjudication=adjudication,additionalChallenge=additionalChallenge(catalog),
            executionState=if(receipt==null) "MISSING" else if(current) "RECORDED_CURRENT_BINDINGS" else "STALE",execution=receipt,
            counts=mapOf("examined_source_scopes" to source.getValue("abilityCount").jsonPrimitive.int,"examined_native_scopes" to native.getValue("abilityCount").jsonPrimitive.int,
                "independent_approvals" to 0,"current_scenarios" to observations.size,"current_native_actions" to observations.sumOf { it.steps.size },
                "source_expectations_contradicted" to observations.count { it.result=="SOURCE_EXPECTATION_CONTRADICTED" },
                "refusal_diagnostic_limits" to observations.count { it.result=="REFUSAL_SUPPORTED_DIAGNOSTIC_LIMIT" },
                "bounded_expectations_supported" to observations.count { it.result=="BOUNDED_EXPECTATION_SUPPORTED" }),
            potentialTapReach=mapOf("primary_cost_trees_with_source_tap" to tap.size,"distinct_native_physical_cards" to tap.map { it.first }.distinct().size))
    }
}
