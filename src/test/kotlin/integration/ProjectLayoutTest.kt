package dev.mtg.assistant

import dev.mtg.assistant.extensions.evidenceTextDigest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Files

class ProjectLayoutTest : FunSpec({
    test("current source and fixture names describe behavior rather than development phases") {
        listOf("cli", "argentum", "reference", "benchmark", "samples", "verification").filter {
            Files.exists(TestFiles.root.resolve(it))
        } shouldBe emptyList()
        Files.walk(TestFiles.root.resolve("src")).use { paths ->
            paths.filter { Regex("(?i)^(phase[0-9]+|batch(one|two)).*").matches(it.fileName.toString()) }.toList()
        } shouldBe emptyList()
    }
    test("relocated source evidence and frozen fixtures retain their original contents") {
        val manifest=Json.parseToJsonElement(Files.readString(TestFiles.fixture("fixture-manifest.json"))).jsonObject
        manifest.getValue("files").jsonArray.forEach { row ->
            val item=row.jsonObject
            val text=Files.readString(TestFiles.fixture(item.getValue("path").jsonPrimitive.content))
            evidenceTextDigest(text) shouldBe item.getValue("sha256TextLf").jsonPrimitive.content
        }
    }
})
