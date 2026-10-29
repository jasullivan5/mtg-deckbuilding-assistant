@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
package dev.mtg.assistant.explorer

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import dev.mtg.assistant.Catalog
import dev.mtg.assistant.EngineBuild
import dev.mtg.assistant.discovery.*
import kotlinx.serialization.json.*
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.Properties
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

private fun applicationRevision(): String = Properties().run {
    requireNotNull(ExplorerServer::class.java.getResourceAsStream("/application-build.properties")) {
        "Missing application provenance; rebuild through dev/dev.ps1"
    }.use(::load)
    getProperty("revision")
}

/** One shared catalog, one discovery worker and one retained run. No browser-supplied commands or files. */
class ExplorerServer(
    private val catalog: Catalog, port: Int = 8787, private val assets: Path? = null,
    private val revision: String = applicationRevision(), private val timeoutSeconds: Long = 120,
    private val discovery: (String, String) -> DiscoveryReport = { name, objective -> discover(catalog, name, objective = objective) }
) : AutoCloseable {
    companion object {
        init {
            // Bound slow request bodies and responses as well as research execution.
            System.getProperties().putIfAbsent("sun.net.httpserver.maxReqTime", "10")
            System.getProperties().putIfAbsent("sun.net.httpserver.maxRspTime", "180")
        }
    }
    private val worker = Executors.newSingleThreadExecutor()
    private val requests = ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS, ArrayBlockingQueue(16))
    private val occupied = AtomicBoolean(false)
    @Volatile private var latest: StoredRun? = null
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", port), 16)
    val port: Int get() = server.address.port

    init {
        server.executor = requests
        server.createContext("/") { exchange ->
            try { handle(exchange) }
            catch (e: Exception) {
                System.err.println("Explorer request failed: ${e.cause ?: e}")
                runCatching { exchange.json(500, ApiError("INTERNAL_ERROR", "The application could not complete this request. See the service terminal.")) }
            } finally { exchange.close() }
        }
    }
    fun start() = server.start()

    private inline fun <reified T> HttpExchange.json(status: Int, value: T) {
        responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        responseHeaders.set("Cache-Control", "no-store")
        responseHeaders.set("X-Content-Type-Options", "nosniff")
        // Stream large exports without constructing another complete report string/tree.
        sendResponseHeaders(status, 0)
        responseBody.use { reportJson.encodeToStream(value, it) }
    }

    private fun handle(x: HttpExchange) {
        if (x.requestHeaders.getFirst("Host") !in setOf("127.0.0.1:$port", "localhost:$port")) {
            x.json(403, ApiError("HOST_REJECTED", "Use the loopback Explorer address.")); return
        }
        val origin = x.requestHeaders.getFirst("Origin")
        if (origin != null && origin !in setOf("http://127.0.0.1:$port", "http://localhost:$port")) {
            x.json(403, ApiError("ORIGIN_REJECTED", "Cross-origin requests are disabled.")); return
        }
        val path = x.requestURI.path
        when (path) {
            "/api/v1/health" -> if (x.requestMethod == "GET") {
                x.json(200, mapOf("status" to "ready", "argentumCommit" to EngineBuild.revision,
                    "engineMode" to EngineBuild.mode, "applicationRevision" to revision)); return
            }
            "/api/v1/objectives" -> if (x.requestMethod == "GET") { x.json(200, objectives); return }
            "/api/v1/commander", "/api/v1/discover" -> { research(x, path.endsWith("discover")); return }
        }
        if (path.startsWith("/api/v1/runs/")) { evidence(x, path); return }
        if (path.startsWith("/api/")) { x.json(404, ApiError("NOT_FOUND", "Unknown API route or method.")); return }
        staticFile(x, path)
    }

    private data class Response(val status: Int, val body: JsonElement)
    private fun research(x: HttpExchange, discoveryRequested: Boolean) {
        if (x.requestMethod != "POST") { x.json(405, ApiError("METHOD_NOT_ALLOWED", "Use POST with JSON.")); return }
        if (!x.requestHeaders.getFirst("Content-Type")?.substringBefore(';')?.trim().equals("application/json", true)) {
            x.json(415, ApiError("CONTENT_TYPE", "Use application/json.")); return
        }
        val bytes = x.requestBody.readNBytes(4097)
        if (bytes.size > 4096) { x.json(413, ApiError("REQUEST_TOO_LARGE", "Request limit is 4096 bytes.")); return }
        val request = try {
            reportJson.decodeFromString<ResearchRequest>(bytes.toString(Charsets.UTF_8)).also {
                require(it.commander.isNotBlank() && it.commander.length <= 200 && it.commander.none(Char::isISOControl))
                require(if (discoveryRequested) objectives.any { o -> o.id == it.objective } else it.objective == null)
            }
        } catch (_: IllegalArgumentException) {
            x.json(400, ApiError("INVALID_REQUEST", "Supply one exact commander name and, for discovery, an existing objective ID.")); return
        }
        if (!occupied.compareAndSet(false, true)) { x.json(409, ApiError("BUSY", "Research is still running. Wait before retrying.")); return }
        if (discoveryRequested) latest = null
        val started = Instant.now()
        val startedNanos = System.nanoTime()
        val future = worker.submit(Callable {
            try {
                val inspected = inspectCommander(catalog, request.commander)
                if (!discoveryRequested) Response(200, reportJson.encodeToJsonElement(inspected))
                else if (!inspected.selectable) Response(422, reportJson.encodeToJsonElement(ApiError(
                    "COMMANDER_BLOCKED", "This commander cannot be selected in this catalog.", inspected)))
                else {
                    val report = discovery(request.commander, request.objective!!)
                    val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos)
                    if (elapsed >= timeoutSeconds * 1000) Response(504, reportJson.encodeToJsonElement(timeoutError()))
                    else {
                        val run = StoredRun(catalog, report, started, elapsed, revision)
                        val body = reportJson.encodeToJsonElement(run.summary)
                        if (System.nanoTime() - startedNanos >= TimeUnit.SECONDS.toNanos(timeoutSeconds))
                            Response(504, reportJson.encodeToJsonElement(timeoutError()))
                        else {
                            // Publish before releasing the gate: newer requests cannot be overwritten by older handlers.
                            latest = run
                            Response(200, body)
                        }
                    }
                }
            } finally { occupied.set(false) }
        })
        try {
            val response = future.get(timeoutSeconds, TimeUnit.SECONDS)
            x.json(response.status, response.body)
        } catch (_: TimeoutException) {
            // Do not release the gate or queue another extraction while uninterruptible work finishes.
            // Late results are never published (deadline checked in the worker).
            x.json(504, timeoutError())
        }
    }
    private fun timeoutError() = ApiError("TIMEOUT",
        "Research exceeded ${timeoutSeconds}s. The worker stays reserved until it finishes; restart the service if it does not recover.")

    private fun evidence(x: HttpExchange, path: String) {
        if (x.requestMethod != "GET") { x.json(405, ApiError("METHOD_NOT_ALLOWED", "Use GET.")); return }
        val parts = path.removePrefix("/api/v1/runs/").split('/')
        val run = latest
        if (run == null || parts.firstOrNull() != run.id) {
            x.json(410, ApiError("RUN_EXPIRED", "This run is no longer retained. Run discovery again; only the latest run is kept.")); return
        }
        if (parts.size == 2 && parts[1] == "report") {
            x.responseHeaders.set("Content-Disposition", "attachment; filename=explorer-${run.id}.json")
            x.json(200, run.export); return
        }
        if (parts.size == 3 && parts[1] == "cards") {
            val card = parts[2].toIntOrNull()?.let(run::card)
            if (card != null) { x.json(200, card); return }
        }
        x.json(404, ApiError("NOT_FOUND", "No such card or evidence route in this run."))
    }

    private fun staticFile(x: HttpExchange, path: String) {
        if (x.requestMethod != "GET") { x.json(405, ApiError("METHOD_NOT_ALLOWED", "Use GET.")); return }
        val root = assets?.toRealPath()
        val file = root?.resolve(if (path == "/") "index.html" else path.removePrefix("/"))?.normalize()
        if (root == null || file == null || !file.startsWith(root) || !Files.isRegularFile(file) || !file.toRealPath().startsWith(root)) {
            x.json(404, ApiError("NOT_FOUND", "Build the Explorer frontend before starting.")); return
        }
        val contentType = when (file.fileName.toString().substringAfterLast('.')) {
            "html" -> "text/html"; "js" -> "text/javascript"; "css" -> "text/css"; else -> "application/octet-stream"
        }
        x.responseHeaders.set("Content-Type", "$contentType; charset=utf-8")
        x.responseHeaders.set("X-Content-Type-Options", "nosniff")
        x.sendResponseHeaders(200, Files.size(file))
        x.responseBody.use { Files.copy(file, it) }
    }
    override fun close() { server.stop(0); worker.shutdownNow(); requests.shutdownNow() }
}

fun serveExplorer(args: List<String>) {
    require(args.size == 2) { "Usage: explorer <frontend-dist-directory> <port>" }
    val assets = Path.of(args[0])
    require(Files.isRegularFile(assets.resolve("index.html"))) { "Frontend build missing; run scripts/explorer.ps1" }
    val port = args[1].toInt()
    require(port in 1024..65535) { "Port must be 1024..65535" }
    println("Loading app-stable catalog…")
    val server = ExplorerServer(Catalog(), port, assets)
    Runtime.getRuntime().addShutdownHook(Thread { server.close() })
    server.start()
    println("Explorer ready: http://127.0.0.1:$port — Ctrl+C to stop")
    CountDownLatch(1).await()
}
