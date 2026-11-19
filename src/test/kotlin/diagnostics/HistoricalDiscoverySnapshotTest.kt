package dev.mtg.assistant

import dev.mtg.assistant.audit.*
import dev.mtg.assistant.discovery.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import java.nio.file.Files

class HistoricalDiscoverySnapshotTest : FunSpec({
    val fixture by lazy {
        reportJson.parseToJsonElement(Files.readString(TestFiles.root.resolve(
            "src/test/resources/regression/historical-native-wire.json"))).jsonObject
    }
    fun snapshot() = fixture.getValue("snapshot").jsonObject
    fun withResult(raw:JsonObject,result:JsonObject)=JsonObject(raw + ("results" to JsonArray(listOf(result))))

    test("a real pre-integration mana trigger stays opaque while recorded decisions and revision remain readable") {
        val raw=snapshot()
        val decoded=discoverySnapshot(raw)
        decoded.original shouldBe raw
        decoded.report.argentumCommit shouldBe fixture.getValue("source").jsonObject.getValue("engine").jsonPrimitive.content
        decoded.report.results.single().verdict shouldBe Verdict.NO_MATCH
        decoded.original.getValue("eventOutcomes").jsonArray.single().jsonObject.getValue("event")
            .jsonObject.getValue("trigger").jsonObject.getValue("type").jsonPrimitive.content shouldBe "LandTappedForMana"
        val path=Files.createTempFile("historical-discovery-", ".json")
        try {
            Files.writeString(path,raw.toString())
            val output=mutableListOf<String>()
            runCli(arrayOf("audit","diff","--before",path.toString(),"--after",path.toString(),"--json"),
                {error("Historical diff must not construct a live catalog")},output::add) shouldBe 0
            val diff=reportJson.decodeFromString<ChangeReport>(output.single())
            diff.comparablePool shouldBe true
            diff.changes shouldBe emptyList()
        } finally { Files.deleteIfExists(path) }
    }

    test("changed opaque SDK evidence is retained verbatim and never reclassified as a current native event") {
        val raw=snapshot()
        val outcome=raw.getValue("eventOutcomes").jsonArray.single().jsonObject
        val result=raw.getValue("results").jsonArray.single().jsonObject
        // Wire-only mutation of a source-derived payload, not an observed game or new admission.
        val changed=JsonObject(outcome + ("event" to JsonObject(outcome.getValue("event").jsonObject +
            ("trigger" to buildJsonObject { put("type","FutureArchivedTrigger");put("uninterpreted",true) }))))
        val before=withResult(raw,JsonObject(result + ("eventOutcome" to outcome)))
        val after=withResult(raw,JsonObject(result + ("eventOutcome" to changed)))
        val diff=diffSnapshots(before,after)
        val change=diff.changes.single { it.kind=="PATHWAY_CHANGED" }
        change.before!!.jsonObject.getValue("eventOutcome") shouldBe outcome
        change.after!!.jsonObject.getValue("eventOutcome") shouldBe changed
        change.attribution shouldContain "RECORDED_OPAQUE_EVIDENCE"
        diff.transitions["RECORDED_OPAQUE_EVIDENCE_CHANGED"] shouldBe 1
        change.after!!.jsonObject.getValue("verdict").jsonPrimitive.content shouldBe "NO_MATCH"
    }

    test("missing historical revision and unknown application decision values still fail closed") {
        shouldThrow<IllegalArgumentException> { discoverySnapshot(JsonObject(snapshot() - "argentumCommit")) }
        val raw=snapshot()
        val result=raw.getValue("results").jsonArray.single().jsonObject
        shouldThrow<SerializationException> {
            discoverySnapshot(withResult(raw,JsonObject(result + ("verdict" to JsonPrimitive("FUTURE_VERDICT")))))
        }
    }
})
