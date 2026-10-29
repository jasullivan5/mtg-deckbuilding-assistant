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
                val live = reportJson.decodeFromString<LiveDiscovery>(response.body())
                val cli = mutableListOf<String>()
                runCli(arrayOf("discover","--commander","Judith, the Scourge Diva","--objective",objective.id,"--json"),{catalog},cli::add) shouldBe 0
                live.report shouldBe reportJson.decodeFromString<DiscoveryReport>(cli.single())
                live.execution shouldBe "FRESH_STRUCTURAL_DISCOVERY"
                live.applicationRevision shouldBe "test-revision"
            }
            val second = reportJson.decodeFromString<LiveDiscovery>(post(server,"discover","Oviya Pashiri, Sage Lifecrafter","create-creature-tokens").body())
            second.report.allowedIdentity shouldBe listOf("GREEN")
            second.report.results.any { it.card == "Oviya Pashiri, Sage Lifecrafter" && it.verdict == Verdict.MATCH } shouldBe true
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
