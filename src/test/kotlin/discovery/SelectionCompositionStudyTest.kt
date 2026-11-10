package dev.mtg.assistant

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.security.MessageDigest

/** Existing API observations only. Selection/composition remains a separate research script. */
class SelectionCompositionStudyTest : FunSpec({
    test("source-shaped interactive response contract is distinct from the missing catalog card") {
        // Synthetic native data for one source contract, NOT a registered Grave Pact definition.
        val control=card("Interactive death response source contract") {
            manaCost="{B}";typeLine="Enchantment"
            triggeredAbility {
                trigger=Triggers.a(GameObjectFilter.Creature.youControl()).dies()
                effect=Effects.Sacrifice(GameObjectFilter.Creature,1,EffectTarget.PlayerRef(Player.EachOpponent))
            }
        }
        val x=PathwayExtractor().extract(listOf(control))
        val rows=MechanicalQuery(x.pathways.filter { it.branch==Contribution.DEATH_ENHANCED_REMOVAL })
            .where(canAffectCreature()).where(canAffectController(Controller.YOU))
            .where(canAffectTokenStatus(TokenStatus.NONTOKEN)).where(hasDeathEnhancement()).execute()
        rows.any { it.verdict==Verdict.MATCH } shouldBe true
        val output=System.getenv("SELECTION_STUDY_EXPORT")?.let(::File)
        output?.apply { mkdirs() }?.resolve("synthetic-response-contract.json")?.writeText(buildJsonObject {
            put("scope","Synthetic source-shaped SDK/extractor contract; not a catalog definition, discovery admission or gameplay execution")
            put("engine",ARGENTUM_SHA);put("actions",reportJson.encodeToJsonElement(x.actions))
            put("checks",reportJson.encodeToJsonElement(rows.flatMap { it.decisions }))
        }.toString())
    }
    test("frozen operational controls preserve source identities and declared execution history") {
        val spec=Json.parseToJsonElement(File("src/test/resources/regression/selection-composition-protocol.json").readText()).jsonObject
        val sources=Json.parseToJsonElement(File("src/test/resources/regression/selection-composition-sources.json").readText()).jsonObject
        spec.getValue("frozenExpectations").jsonArray.size shouldBe 19
        sources.getValue("newRecords").jsonArray.size shouldBe 7
        val tannuk=sources.getValue("newRecords").jsonArray.single { it.jsonObject["name"]?.jsonPrimitive?.content=="Tannuk, Memorial Ensign" }.jsonObject
        tannuk.getValue("oracle_text").jsonPrimitive.content.contains("second time this ability has resolved this turn") shouldBe true
        (sources.getValue("newRecords").jsonArray.map { it.jsonObject.getValue("oracle_id") }.distinct().size) shouldBe 7
    }
    test("opt-in operational observation retains native facts and unchanged production decisions") {
        val directory=System.getenv("SELECTION_STUDY_EXPORT") ?: return@test
        val output=File(directory).apply { mkdirs() }
        val spec=Json.parseToJsonElement(File("src/test/resources/regression/selection-composition-protocol.json").readText()).jsonObject
        val names=spec.getValue("frozenExpectations").jsonArray.map { it.jsonObject.getValue("card").jsonPrimitive.content }.toSet()
        val catalog=Catalog()
        val report=discover(catalog,"Kenrith, the Returned King")
        val observations=names.sorted().map { name -> buildJsonObject {
            put("card",name)
            val card=catalog.card(name);put("available",card!=null)
            if(card!=null) {
                put("native",nativeJson.encodeToJsonElement(CardDefinition.serializer(),card))
                val path=DefinitionSources.paths[name];put("source",path)
                if(path!=null) {
                    val file=File(System.getenv("ARGENTUM_ENGINE_DIR") ?: "../argentum-engine",path)
                    if(file.exists()) put("sourceLfSha256",MessageDigest.getInstance("SHA-256").digest(file.readText().replace("\r\n","\n").toByteArray()).joinToString("") { "%02x".format(it) })
                }
                val x=PathwayExtractor().extract(listOf(card))
                put("facts",reportJson.encodeToJsonElement(x.facts));put("actions",reportJson.encodeToJsonElement(x.actions))
                put("eventOutcomes",reportJson.encodeToJsonElement(x.eventOutcomes));put("selfReturns",reportJson.encodeToJsonElement(x.selfReturns))
                put("unsupported",reportJson.encodeToJsonElement(x.unsupported))
            }
            put("eligibility",reportJson.encodeToJsonElement(report.eligibility.filter { it.card==name }))
            put("currentResults",reportJson.encodeToJsonElement(report.results.filter { it.card==name }))
        } }
        File(output,"kenrith-current.json").writeText(reportJson.encodeToString(report))
        File(output,"native-controls.json").writeText(buildJsonObject {
            put("baselineApplication",spec.getValue("applicationBase"));put("engine",ARGENTUM_SHA)
            put("cards",JsonArray(observations))
        }.toString())
    }
})
