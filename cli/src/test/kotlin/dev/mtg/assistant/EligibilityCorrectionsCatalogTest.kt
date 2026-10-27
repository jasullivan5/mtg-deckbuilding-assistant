package dev.mtg.assistant

import com.wingedsheep.sdk.core.Color
import dev.mtg.assistant.coverage.*
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.eligibility.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*

class EligibilityCorrectionsCatalogTest : FunSpec({
    val catalog by lazy { Catalog() }
    val names=listOf("Heartwood Shard","Hooded Kavu","Firescreamer","Sunforger","Toxrill, the Corrosive","Academy Manufactor","Afterthought Sentry")
    val expected=listOf(listOf("G"),listOf("B","R"),listOf("B","R"),listOf("R","W"),listOf("B","U"),emptyList(),emptyList())
    for((i,name) in names.withIndex()) test("reviewed real metadata and source association: $name") {
        val c=catalog.card(name)!!;val r=catalog.eligibility.resolve(c)
        r.identity shouldBe expected[i];r.commanderLegality shouldBe Verdict.MATCH
        r.corrections.single().state shouldBe "APPLIED"
        val record=r.corrections.single().record
        record.oracleId shouldBe ApprovedEligibility.plan.sources.single { it.name==name }.oracleId
        record.definitionSha256 shouldBe eligibilityDefinitionDigest(c)
        c shouldBe catalog.registry.requireCard(name)
    }
    test("observed native Firescreamer identity is black and already excludes Krenko") {
        val c=catalog.card("Firescreamer")!!
        fullIdentity(c) shouldBe setOf(Color.BLACK)
        selectedCommanderEligibility(c,setOf(Color.RED)) shouldBe Verdict.NO_MATCH
        selectedCommanderEligibility(c,setOf(Color.RED),catalog.eligibility.resolve(c)) shouldBe Verdict.NO_MATCH
    }
    test("all three production CLI objectives apply only reviewed corrections to the legal pool") {
        val expectedRemoved=mapOf("Judith, the Scourge Diva" to setOf("Heartwood Shard","Sunforger","Toxrill, the Corrosive"),
            "Krenko, Mob Boss" to setOf("Heartwood Shard","Hooded Kavu","Sunforger"),"Oviya Pashiri, Sage Lifecrafter" to setOf("Sunforger"))
        for((commander,removed) in expectedRemoved) for(objective in coverageObjectives) {
            val output=mutableListOf<String>()
            runCli(arrayOf("discover","--commander",commander,"--objective",objective,"--search","name:Academy","--json"),{catalog},output::add) shouldBe 0
            val r=reportJson.decodeFromString<DiscoveryReport>(output.single())
            val colors=resolveCommander(catalog,commander).let(::fullIdentity)
            val rows=r.eligibility.associateBy { it.card }
            val actualRemoved=names.filter { name ->
                val c=catalog.card(name)!!
                commanderFormatLegality(c)==Verdict.MATCH && selectedCommanderEligibility(c,colors)==Verdict.MATCH &&
                    !(rows.getValue(name).format==Verdict.MATCH && rows.getValue(name).deck==Verdict.MATCH)
            }.toSet()
            actualRemoved shouldBe removed
            setOf("Academy Manufactor","Afterthought Sentry").all { rows.getValue(it).let { e -> e.format==Verdict.MATCH && e.deck==Verdict.MATCH } } shouldBe true
            r.mechanicalInputCards!!.contains("Academy Manufactor") shouldBe true
            r.actions.filter { it.card=="Academy Manufactor" }.all { it.native!=JsonNull } shouldBe true
        }
    }
    test("real commander CLI selection and inspection agree on corrected Toxrill identity") {
        val output=mutableListOf<String>()
        runCli(arrayOf("commander","inspect","Toxrill, the Corrosive","--json"),{catalog},output::add) shouldBe 0
        val inspected=reportJson.decodeFromString<CommanderInspection>(output.single())
        inspected.colorIdentity shouldBe listOf("BLACK","BLUE");inspected.selectable shouldBe true
        inspected.effectiveMetadata!!.originalIdentity shouldBe listOf("B")
        output.clear()
        runCli(arrayOf("discover","--commander","Toxrill, the Corrosive","--objective","create-creature-tokens","--search","name:Academy","--json"),{catalog},output::add) shouldBe 0
        reportJson.decodeFromString<DiscoveryReport>(output.single()).allowedIdentity shouldBe inspected.colorIdentity
    }
    test("conventional identity and Commander format filters share reviewed effective metadata") {
        catalog.search("name:Sunforger id<=r").map { it.name } shouldBe emptyList()
        catalog.search("name:Sunforger id:rw").map { it.name } shouldBe listOf("Sunforger")
        catalog.search("name:Afterthought f:commander").map { it.name } shouldBe listOf("Afterthought Sentry")
        catalog.card("Sunforger")!!.colorIdentityOverride shouldBe emptySet()
        commanderFormatLegality(catalog.card("Afterthought Sentry")!!) shouldBe Verdict.UNKNOWN
    }
    test("coverage retains original disagreement history and effective agreement") {
        val plan=ApprovedEligibility.plan
        val snapshot=OracleSnapshot(plan.snapshot,plan.sources,plan.sources.size,0,0)
        val r=catalogCoverage(catalog,snapshot,"Judith, the Scourge Diva",listOf("create-creature-tokens"))
        r.eligibilityCorrections.size shouldBe 7
        r.records.single { it.external.name=="Firescreamer" }.mapping.let {
            it.metadata!!.nativeIdentity shouldBe setOf("B")
            it.metadata!!.issues shouldBe listOf("NATIVE_EXTERNAL_IDENTITY_DISAGREEMENT")
            it.effectiveMetadata!!.identity shouldBe listOf("B","R")
            it.effectiveAgreement!!.issues shouldBe emptyList()
        }
        r.records.single { it.external.name=="Academy Manufactor" }.objectives.values.single().state.startsWith("NOT_EVALUATED") shouldBe false
    }
})
