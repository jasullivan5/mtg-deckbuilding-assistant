@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.explorer.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.nio.file.Files
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.io.OutputStream

class ExplorerServerTest : FunSpec({
    val catalog by lazy { Catalog() }
    val client = HttpClient.newHttpClient()
    fun request(server: ExplorerServer, route: String, body: String? = null, origin: String? = null): HttpResponse<String> {
        val b = HttpRequest.newBuilder(URI("http://127.0.0.1:${server.port}/api/v1/$route"))
            .timeout(java.time.Duration.ofSeconds(150))
        if (body != null) b.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body))
        origin?.let { b.header("Origin", it) }
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString())
    }
    fun post(server: ExplorerServer, route: String, name: String, objective: String? = null) = request(server, route,
        buildJsonObject { put("commander",name); objective?.let { put("objective",it) } }.toString())

    test("inspection preserves exact eligibility, blockers and missing definition feedback") {
        ExplorerServer(catalog, 0).use { server ->
            server.start()
            request(server,"health").statusCode() shouldBe 200
            reportJson.decodeFromString<List<Objective>>(request(server,"objectives").body()) shouldBe objectives
            for (name in listOf("Judith, the Scourge Diva", "Murder", "No such commander", "oviya pashiri, sage lifecrafter")) {
                val response = post(server,"commander",name)
                response.statusCode() shouldBe 200
                reportJson.decodeFromString<CommanderInspection>(response.body()) shouldBe inspectCommander(catalog,name)
            }
            val blocked = post(server,"discover","Murder",objectives.first().id)
            blocked.statusCode() shouldBe 422
            reportJson.decodeFromString<ApiError>(blocked.body()).inspection!!.selectable shouldBe false
        }
    }
    test("all three HTTP discoveries agree with CLI full reports and evidence") {
        ExplorerServer(catalog,0,revision="test-revision").use { server ->
            server.start()
            for (objective in objectives) {
                val response = post(server,"discover","Judith, the Scourge Diva",objective.id)
                response.statusCode() shouldBe 200
                val summary = reportJson.decodeFromString<RunSummary>(response.body())
                val exported = client.send(HttpRequest.newBuilder(URI("http://127.0.0.1:${server.port}/api/v1/runs/${summary.runId}/report")).build(),
                    HttpResponse.BodyHandlers.ofInputStream())
                exported.statusCode() shouldBe 200
                val live = exported.body().use { reportJson.decodeFromStream<LiveDiscovery>(it) }
                live.runId shouldBe summary.runId
                summary.counts shouldBe live.report.counts
                summary.cards.map { it.card } shouldBe (live.report.results.map { it.card } + live.report.unsupported.map { it.card }).distinct().sorted()
                val output = Files.createTempFile("explorer-cli-parity-", ".json")
                try {
                    runCli(arrayOf("discover","--commander","Judith, the Scourge Diva","--objective",objective.id,"--json","--output",output.toString()),{catalog},{}) shouldBe 0
                    // Compare every serialized field against the real CLI without building a second giant report tree.
                    val apiHash = MessageDigest.getInstance("SHA-256")
                    DigestOutputStream(OutputStream.nullOutputStream(),apiHash).use {
                        reportJson.encodeToStream(DiscoveryReport.serializer(),live.report,it)
                    }
                    val cliHash = MessageDigest.getInstance("SHA-256")
                    Files.newInputStream(output).use { input ->
                        val buffer=ByteArray(8192)
                        while(true) { val n=input.read(buffer); if(n<0) break; cliHash.update(buffer,0,n) }
                    }
                    apiHash.digest().toList() shouldBe cliHash.digest().toList()
                } finally { Files.deleteIfExists(output) }
                val card=summary.cards.first { it.pathways.getValue("MATCH")>0 }
                val detail=reportJson.decodeFromString<CardEvidence>(request(server,"runs/${summary.runId}/cards/${card.index}").body())
                detail.results shouldBe live.report.results.filter { it.card==card.card }
                detail.actions shouldBe live.report.actions.filter { it.card==card.card }
                detail.facts shouldBe live.report.facts.filter { it.card==card.card }
                detail.unsupported shouldBe live.report.unsupported.filter { it.card==card.card }
                detail.structuralSupport shouldBe live.report.structuralSupport.filter { it.card==card.card }
                detail.eligibility shouldBe live.report.eligibility.filter { it.card==card.card }
                detail.translationAssurance shouldBe live.report.translationAssurance.filter { it.card==card.card }
                request(server,"runs/${summary.runId}/cards/-1").statusCode() shouldBe 404
                request(server,"runs/${summary.runId}/cards/9999999999999").statusCode() shouldBe 404
                live.execution shouldBe "FRESH_STRUCTURAL_DISCOVERY"
                live.applicationRevision shouldBe "test-revision"
            }
            val second = reportJson.decodeFromString<RunSummary>(post(server,"discover","Oviya Pashiri, Sage Lifecrafter","create-creature-tokens").body())
            second.allowedIdentity shouldBe listOf("GREEN")
            second.cards.any { it.card == "Oviya Pashiri, Sage Lifecrafter" && it.pathways.getValue("MATCH")>0 } shouldBe true
            request(server,"runs/expired-run/cards/0").statusCode() shouldBe 410
        }
    }
    test("commander presentation keeps original selection authority and exposes scoped evidence without generic construction notices") {
        ExplorerServer(catalog, 0).use { server ->
            server.start()
            val response = post(server, "commander-view", "Judith, the Scourge Diva", objectives.first().id)
            response.statusCode() shouldBe 200
            val view = reportJson.decodeFromString<CommanderView>(response.body())
            view.inspection shouldBe inspectCommander(catalog, "Judith, the Scourge Diva")
            view.headline shouldBe "Judith, the Scourge Diva can be your commander · Black and red"
            view.authoredCard!!.text.contains("nontoken creature you control dies") shouldBe true
            view.evidenceSummary.single { it.id == "mechanic" }.state shouldBe EvidenceState.NOT_RECORDED
            view.evidenceSummary.single { it.id == "independent" }.state shouldBe EvidenceState.NOT_RECORDED
            post(server, "commander-view", "Judith, the Scourge Diva", "invented").statusCode() shouldBe 400
            val blocked = reportJson.decodeFromString<CommanderView>(post(server, "commander-view", "Murder", objectives.first().id).body())
            blocked.inspection.selectable shouldBe false
            blocked.headline.startsWith("Selection blocked") shouldBe true
        }
    }
    test("image requests use current run indexes, enforce batch limits and retain verified identity") {
        val raw = reportJson.parseToJsonElement(Files.readString(TestFiles.fixture("cards/agent-of-shauku-source.json"))).jsonObject
        var lookups = 0
        val images = CardImages { lookups++; listOf(raw) }
        ExplorerServer(catalog, 0, images = images).use { server ->
            server.start()
            request(server, "runs/expired/images?indices=0").statusCode() shouldBe 410
            lookups shouldBe 0
            val summary = reportJson.decodeFromString<RunSummary>(post(server, "discover", "Judith, the Scourge Diva", objectives.first().id).body())
            val index = summary.cards.single { it.card == "Agent of Shauku" }.index
            for (query in listOf("", "indices=", "indices=-1", "indices=no", "indices=999999", "indices=" + List(41) { index }.joinToString(","))) {
                request(server, "runs/${summary.runId}/images?$query").statusCode() shouldBe 400
            }
            lookups shouldBe 0
            val response = request(server, "runs/${summary.runId}/images?indices=$index")
            response.statusCode() shouldBe 200
            val image = reportJson.decodeFromString<List<CardImage>>(response.body()).single()
            image.card shouldBe "Agent of Shauku"
            image.status shouldBe "VERIFIED"
            image.printingId shouldBe "d8316804-6f8b-423e-a2c3-fa476c095544"
            lookups shouldBe 1
        }
    }
    test("malformed, oversized, invalid objective, route, method and foreign-origin requests are explicit errors") {
        ExplorerServer(catalog,0).use { s ->
            s.start()
            for (body in listOf("{", "{}", "{\"commander\":\" \"}", "{\"commander\":\"Judith\",\"extra\":true}")) request(s,"commander",body).statusCode() shouldBe 400
            post(s,"discover","Judith, the Scourge Diva","invented").statusCode() shouldBe 400
            request(s,"commander","x".repeat(4097)).statusCode() shouldBe 413
            request(s,"commander").statusCode() shouldBe 405
            request(s,"absent").statusCode() shouldBe 404
            request(s,"health",origin="https://foreign.invalid").statusCode() shouldBe 403
            val b = HttpRequest.newBuilder(URI("http://127.0.0.1:${s.port}/api/v1/commander")).POST(HttpRequest.BodyPublishers.ofString("{}"))
            client.send(b.build(),HttpResponse.BodyHandlers.ofString()).statusCode() shouldBe 415
        }
    }
    test("internal failures are reported without fabricating discoveries and worker recovers") {
        ExplorerServer(catalog,0,discovery={_,_->error("deliberate test failure")}).use { s ->
            s.start()
            val response=post(s,"discover","Judith, the Scourge Diva",objectives.first().id)
            response.statusCode() shouldBe 500
            reportJson.decodeFromString<ApiError>(response.body()).code shouldBe "INTERNAL_ERROR"
            post(s,"commander","Judith, the Scourge Diva").statusCode() shouldBe 200
        }
    }
    test("timeout retains exclusive worker reservation until actual computation ends") {
        val entered=CountDownLatch(1); val release=CountDownLatch(1)
        ExplorerServer(catalog,0,timeoutSeconds=1,discovery={_,_->entered.countDown();release.await(10,TimeUnit.SECONDS);error("released test worker")}).use { s ->
            s.start()
            val pending=java.util.concurrent.CompletableFuture.supplyAsync { post(s,"discover","Judith, the Scourge Diva",objectives.first().id) }
            try {
                entered.await(5,TimeUnit.SECONDS) shouldBe true
                post(s,"commander","Judith, the Scourge Diva").statusCode() shouldBe 409
                pending.get(5,TimeUnit.SECONDS).statusCode() shouldBe 504
                post(s,"commander","Judith, the Scourge Diva").statusCode() shouldBe 409
            } finally { release.countDown() }
        }
    }
})
