package dev.mtg.assistant

import com.wingedsheep.sdk.model.CardDefinition
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.security.MessageDigest

/** Read-only research: calls existing queries and exports exact evidence on explicit opt-in.
 * No candidate policy participates in discovery or modifies its results. */
class ObjectiveAdmissionStudyTest : FunSpec({
    val catalog by lazy { Catalog() }
    val judith by lazy { discover(catalog,"Judith, the Scourge Diva") }
    test("current policy keeps mechanical compatibility distinct from primary admission") {
        val murder=judith.results.single { it.card=="Murder" && it.branch==Contribution.DIRECT_DEATH_GENERATOR }
        murder.mechanicalCompatibility shouldBe Verdict.MATCH
        murder.policyAcceptance shouldBe Verdict.NO_MATCH
        val slip=judith.results.single { it.card=="Tragic Slip" && it.branch==Contribution.DEATH_ENHANCED_REMOVAL }
        slip.verdict shouldBe Verdict.MATCH
        judith.results.any { it.card=="Attrition" && it.branch==Contribution.DIRECT_DEATH_GENERATOR && it.verdict==Verdict.MATCH } shouldBe true
        judith.results.any { it.card=="Reassembling Skeleton" && it.branch==Contribution.INTRINSIC_CREATURE_INPUT && it.verdict==Verdict.MATCH } shouldBe true
        judith.results.none { it.card=="Walking Corpse" && it.branch==Contribution.INTRINSIC_CREATURE_INPUT && it.verdict==Verdict.MATCH } shouldBe true
    }
    test("opt-in source-bound admission observations use unchanged extraction and discovery") {
        val directory=System.getenv("OBJECTIVE_ADMISSION_EXPORT") ?: return@test
        val output=File(directory).apply { mkdirs() }
        val criteria=Json.parseToJsonElement(File("docs/evidence/objective-admission-criteria.json").readText()).jsonObject
        val supplement=Json.parseToJsonElement(File("docs/evidence/objective-admission-supplement.json").readText()).jsonObject
        val names=(criteria.getValue("familiarControls").jsonArray.map { it.jsonPrimitive.content }+
            criteria.getValue("heldOutSelection").jsonArray.map { it.jsonObject.getValue("card").jsonPrimitive.content }+
            supplement.getValue("source").jsonObject.getValue("name").jsonPrimitive.content).toSet()
        val cards=names.sorted().map { name ->
            buildJsonObject {
                put("card",name)
                val card=catalog.card(name)
                put("available",card!=null)
                if(card!=null) {
                    put("native",nativeJson.encodeToJsonElement(CardDefinition.serializer(),card))
                    val path=DefinitionSources.paths[name]
                    put("nativeSource",path)
                    if(path!=null) {
                        val source=File(System.getenv("ARGENTUM_ENGINE_DIR") ?: "../argentum-engine",path)
                        if(source.exists()) put("nativeSourceLfSha256",MessageDigest.getInstance("SHA-256").digest(source.readText().replace("\r\n","\n").toByteArray()).joinToString("") { "%02x".format(it) })
                    }
                    val x=PathwayExtractor(inspectCardEntries=true).extract(listOf(card))
                    put("actions",reportJson.encodeToJsonElement(x.actions))
                    put("facts",reportJson.encodeToJsonElement(x.facts))
                    put("eventOutcomes",reportJson.encodeToJsonElement(x.eventOutcomes))
                    put("selfReturns",reportJson.encodeToJsonElement(x.selfReturns))
                    put("unsupported",reportJson.encodeToJsonElement(x.unsupported))
                    put("structuralSupport",reportJson.encodeToJsonElement(x.structuralSupport))
                }
            }
        }
        val selectedReports=listOf("own-nontoken-creature-deaths","reanimate-own-creatures","create-creature-tokens","stock-own-graveyard").map { objective ->
            val r=discover(catalog,"Kenrith, the Returned King",objective=objective)
            buildJsonObject {
                put("objective",objective);put("commander",r.commander)
                put("input",JsonArray(r.mechanicalInputCards.orEmpty().filter { it in names }.map(::JsonPrimitive)))
                put("eligibility",reportJson.encodeToJsonElement(r.eligibility.filter { it.card in names }))
                put("results",reportJson.encodeToJsonElement(r.results.filter { it.card in names }))
            }
        }
        File(output,"native-observations.json").writeText(buildJsonObject {
            put("baselineApplication",criteria.getValue("baselineApplication"));put("argentumCommit",ARGENTUM_SHA)
            put("standaloneExtractionIncludesCardEntries",true)
            put("cards",JsonArray(cards));put("queries",JsonArray(selectedReports))
        }.toString())
        for((slug,r) in listOf("judith" to judith,"krenko" to discover(catalog,"Krenko, Mob Boss"))) {
            File(output,"$slug-current.json").writeText(reportJson.encodeToString(r))
        }
    }
})
