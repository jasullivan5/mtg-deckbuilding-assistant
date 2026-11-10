package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Path
import java.security.MessageDigest

class SelfReplenishmentDiscoveryTest : FunSpec({
    val catalog by lazy { Catalog() }
    val report by lazy { discover(catalog,"Judith, the Scourge Diva") }
    test("predeclared source and native cases preserve positive negative unknown and missing distinctions") {
        val spec=Json.parseToJsonElement(java.io.File("src/test/resources/regression/self-replenishment-expectations.json").readText()).jsonObject
        for(raw in spec.getValue("cases").jsonArray) {
            val c=raw.jsonObject;val name=c.getValue("card").jsonPrimitive.content;val expected=c.getValue("expected").jsonPrimitive.content
            val native=catalog.card(name)
            if(expected=="MISSING") { (native==null) shouldBe true;continue }
            val x=PathwayExtractor().extract(listOf(native ?: error("Missing native source control $name")))
            val rows=selfReplenishment(x.selfReturns,x.facts)
            when(expected) {
                "MATCH" -> rows.any { it.verdict==Verdict.MATCH } shouldBe true
                "UNKNOWN" -> rows.any { it.verdict==Verdict.UNKNOWN } shouldBe true
                "NO_MATCH" -> { rows.isNotEmpty() shouldBe true;rows.all { it.verdict==Verdict.NO_MATCH } shouldBe true }
                "NONE" -> rows.isEmpty() shouldBe true
            }
            val nativePath=c.getValue("nativeSource").jsonPrimitive.content
            val bytes=java.io.File(System.getenv("ARGENTUM_ENGINE_DIR") ?: "../argentum-engine",nativePath).readText().replace("\r\n","\n").toByteArray()
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } shouldBe c.getValue("nativeSourceLfSha256").jsonPrimitive.content
            c["sourceFile"]?.takeIf { it !is JsonNull }?.jsonPrimitive?.content?.let { p ->
                val source=java.io.File(p).readBytes()
                MessageDigest.getInstance("SHA-256").digest(source).joinToString("") { "%02x".format(it) } shouldBe c.getValue("sourceSha256").jsonPrimitive.content
            }
        }
    }
    test("suppliers need no sacrifice outlet and do not become death producers or draw-token beneficiaries") {
        for(name in listOf("Reassembling Skeleton","Bloodghast","Bladewing's Thrall","Forsaken Miner","Haunted Dead")) {
            val rows=report.results.filter { it.card==name }
            rows.any { it.branch==Contribution.INTRINSIC_CREATURE_INPUT && it.verdict==Verdict.MATCH } shouldBe true
            rows.any { it.branch==Contribution.DIRECT_DEATH_GENERATOR && it.verdict==Verdict.MATCH } shouldBe false
        }
    }
    test("a finality return stays unresolved for the death objective while generic reanimation remains accepted") {
        report.results.single { it.card=="Wolfbat" && it.branch==Contribution.INTRINSIC_CREATURE_INPUT }.verdict shouldBe Verdict.UNKNOWN
        val x=PathwayExtractor().extract(listOf(catalog.card("Wolfbat")!!))
        originalCreatureSelfReturns(x.selfReturns,x.facts).single().verdict shouldBe Verdict.MATCH
        val b=discover(catalog,report.commander,objective="reanimate-own-creatures")
        b.results.any { it.card=="Wolfbat" && it.verdict==Verdict.MATCH } shouldBe true
        b.results.any { it.card=="Reanimate" && it.verdict==Verdict.MATCH } shouldBe true
        report.results.none { it.card=="Reanimate" && it.branch==Contribution.INTRINSIC_CREATURE_INPUT && it.verdict==Verdict.MATCH } shouldBe true
    }
    test("legacy death production responses and history removal remain separate") {
        for((name,branch) in listOf("Viscera Seer" to Contribution.DIRECT_DEATH_GENERATOR,"Grim Haruspex" to Contribution.DEATH_TRIGGERED_OUTCOME,"Tragic Slip" to Contribution.DEATH_ENHANCED_REMOVAL))
            report.results.any { it.card==name && it.branch==branch && it.verdict==Verdict.MATCH } shouldBe true
        report.results.any { it.card=="Tymaret, the Murder King" && it.branch==Contribution.DIRECT_DEATH_GENERATOR && it.verdict==Verdict.MATCH } shouldBe true
        report.results.filter { it.card=="Tymaret, the Murder King" && it.branch==Contribution.INTRINSIC_CREATURE_INPUT }.all { it.verdict==Verdict.NO_MATCH } shouldBe true
    }
})
