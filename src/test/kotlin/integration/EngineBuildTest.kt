package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.audit.*
import kotlinx.serialization.encodeToString
import java.nio.file.Files
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow

class EngineBuildTest : FunSpec({
    test("packaged engine metadata identifies actual build rather than historical evidence") {
        EngineBuild.revision.matches(Regex("[0-9a-f]{40}")) shouldBe true
        EngineBuild.implementationSha256.matches(Regex("[0-9a-f]{64}")) shouldBe true
        (EngineBuild.mode in setOf("app-stable", "candidate")) shouldBe true
        ARGENTUM_SHA shouldBe EngineBuild.revision
        EngineBuild.evidenceCompatible("f".repeat(40)) shouldBe false
        EngineBuild.evidenceCompatible(EngineBuild.revision) shouldBe true
        EngineBuild.evidenceCompatible("4133241856227d1d36269e92aaba633bbaf93f5c", "f".repeat(40)) shouldBe false
    }
    test("fresh reports and native provenance use built engine revision") {
        val catalog=Catalog()
        val report=discover(catalog,"Judith, the Scourge Diva","name:Academy","create-creature-tokens")
        report.argentumCommit shouldBe EngineBuild.revision
        catalog.provenance("Lightning Bolt").source.endsWith(EngineBuild.revision) shouldBe true
        val output=mutableListOf<String>()
        runCli(arrayOf("stats"),{catalog},output::add) shouldBe 0
        output.last().contains(EngineBuild.revision) shouldBe true
    }
    test("historical benchmarks can be re-evaluated without rewriting their revision") {
        val historical=ReviewedBenchmark("historical-test","a".repeat(40),"synthetic source-led fixture","2026-10-29",emptyList())
        val file=Files.createTempFile("historical-benchmark-", ".json")
        try {
            Files.writeString(file,reportJson.encodeToString(historical))
            val report=discover(Catalog(),"Judith, the Scourge Diva","name:Academy","create-creature-tokens")
            val evaluated=evaluateBenchmark(report,file)
            evaluated.reviewArgentumCommit shouldBe historical.argentumCommit
            evaluated.discoveryArgentumCommit shouldBe EngineBuild.revision
            readBenchmark(file) shouldBe historical
        } finally { Files.deleteIfExists(file) }
    }
})
