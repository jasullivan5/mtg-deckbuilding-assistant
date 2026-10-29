package dev.mtg.assistant.explorer

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import dev.mtg.assistant.Catalog
import dev.mtg.assistant.discovery.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

@Serializable data class Objective(val id: String, val label: String, val explanation: String)
val objectives = listOf(
    Objective("own-nontoken-creature-deaths", "Our nontoken creatures die", "Find direct death generation and separately classified death-enhanced removal."),
    Objective("reanimate-own-creatures", "Return our creatures", "Find direct movement of original creature cards from our graveyard to our battlefield."),
    Objective("create-creature-tokens", "Create creature tokens", "Find creature-token creation under our control, subject to type, quantity and controller prerequisites.")
)
@Serializable data class ResearchRequest(val commander: String, val objective: String? = null)
@Serializable data class ApiError(val code: String, val message: String, val inspection: CommanderInspection? = null)
@Serializable data class LiveDiscovery(val schemaVersion: Int = 1, val execution: String = "FRESH_STRUCTURAL_DISCOVERY",
    val startedAt: String, val durationMs: Long, val applicationRevision: String,
    val report: DiscoveryReport)

/** One shared catalog, one bounded worker, no browser-supplied shell or filesystem access. */
class ExplorerServer(private val catalog: Catalog, port: Int = 8787, private val assets: Path? = null,
    private val revision: String = "unrecorded", private val timeoutSeconds: Long = 120,
    private val discovery: (String, String) -> DiscoveryReport = { name, objective -> discover(catalog, name, objective = objective) }) : AutoCloseable {
    private val worker = Executors.newSingleThreadExecutor()
    private val requests = Executors.newFixedThreadPool(4)
    private val occupied = AtomicBoolean(false)
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", port), 16)
    val port: Int get() = server.address.port
    init {
        server.executor = requests
        server.createContext("/") { exchange ->
            try { handle(exchange) }
            catch (e: Exception) {
                System.err.println("Explorer request failed: ${e.javaClass.simpleName}: ${e.message}")
                runCatching { exchange.json(500, ApiError("INTERNAL_ERROR", "The application could not complete this request. See the service terminal.")) }
            } finally { exchange.close() }
        }
    }
    fun start() = server.start()
    private inline fun <reified T> HttpExchange.json(status: Int, value: T) {
        val bytes = reportJson.encodeToString(value).toByteArray(Charsets.UTF_8)
        responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        responseHeaders.set("Cache-Control", "no-store")
        sendResponseHeaders(status, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }
    private fun handle(x: HttpExchange) {
        // Reject foreign origins and DNS rebinding; no permissive CORS.
        if (x.requestHeaders.getFirst("Host") !in setOf("127.0.0.1:$port", "localhost:$port")) {
            x.json(403, ApiError("HOST_REJECTED", "Use the loopback Explorer address.")); return
        }
        val origin = x.requestHeaders.getFirst("Origin")
        if (origin != null && origin !in setOf("http://127.0.0.1:$port", "http://localhost:$port")) {
            x.json(403, ApiError("ORIGIN_REJECTED", "Cross-origin requests are disabled.")); return
        }
        val path = x.requestURI.path
        if (path == "/api/v1/health" && x.requestMethod == "GET") { x.json(200, mapOf("status" to "ready", "argentumCommit" to ARGENTUM_SHA, "applicationRevision" to revision)); return }
        if (path == "/api/v1/objectives" && x.requestMethod == "GET") { x.json(200, objectives); return }
        if (path in setOf("/api/v1/commander", "/api/v1/discover")) {
            if (x.requestMethod != "POST") { x.json(405, ApiError("METHOD_NOT_ALLOWED", "Use POST with JSON.")); return }
            if (x.requestHeaders.getFirst("Content-Type")?.substringBefore(';') != "application/json") { x.json(415, ApiError("CONTENT_TYPE", "Use application/json.")); return }
            val bytes = x.requestBody.readNBytes(4097)
            if (bytes.size > 4096) { x.json(413, ApiError("REQUEST_TOO_LARGE", "Request limit is 4096 bytes.")); return }
            val request = try {
                reportJson.decodeFromString<ResearchRequest>(bytes.toString(Charsets.UTF_8)).also {
                    require(it.commander.isNotBlank() && it.commander.length <= 200 && it.commander.none(Char::isISOControl))
                    require(if (path.endsWith("discover")) objectives.any { o -> o.id == it.objective } else it.objective == null)
                }
            } catch (_: IllegalArgumentException) { x.json(400, ApiError("INVALID_REQUEST", "Supply one exact commander name and, for discovery, an existing objective ID.")); return }
            if (!occupied.compareAndSet(false, true)) { x.json(409, ApiError("BUSY", "A research operation is still running. Wait before retrying.")); return }
            val started = Instant.now()
            val future = worker.submit(Callable {
                try {
                    val inspection = inspectCommander(catalog, request.commander)
                    if (path.endsWith("commander")) 200 to reportJson.encodeToJsonElement(inspection)
                    else if (!inspection.selectable) 422 to reportJson.encodeToJsonElement(ApiError("COMMANDER_BLOCKED", "This commander cannot be selected in this catalog.", inspection))
                    else {
                        val report = discovery(request.commander, request.objective!!)
                        200 to reportJson.encodeToJsonElement(LiveDiscovery(startedAt = started.toString(), durationMs = java.time.Duration.between(started, Instant.now()).toMillis(), applicationRevision = revision, report = report))
                    }
                } finally { occupied.set(false) }
            })
            try { val (status, body) = future.get(timeoutSeconds, TimeUnit.SECONDS); x.json(status, body) }
            catch (_: TimeoutException) {
                // Do not cancel/release the gate: extraction may not honor interruption.
                // The response closes; later work is rejected until the worker actually exits.
                x.json(504, ApiError("TIMEOUT", "Research exceeded ${timeoutSeconds}s. The worker remains reserved until it finishes; restart if necessary."))
            }
            return
        }
        if (path.startsWith("/api/")) { x.json(404, ApiError("NOT_FOUND", "Unknown API route.")); return }
        if (x.requestMethod != "GET") { x.json(405, ApiError("METHOD_NOT_ALLOWED", "Use GET.")); return }
        val root = assets?.toAbsolutePath()?.normalize()
        val file = root?.resolve(if (path == "/") "index.html" else path.removePrefix("/"))?.normalize()
        if (root == null || file == null || !file.startsWith(root) || !Files.isRegularFile(file)) { x.json(404, ApiError("NOT_FOUND", "Build the Explorer frontend before starting.")); return }
        val contentType = when (file.fileName.toString().substringAfterLast('.')) { "html" -> "text/html"; "js" -> "text/javascript"; "css" -> "text/css"; else -> "application/octet-stream" }
        x.responseHeaders.set("Content-Type", "$contentType; charset=utf-8")
        x.responseHeaders.set("X-Content-Type-Options", "nosniff")
        val content = Files.readAllBytes(file)
        x.sendResponseHeaders(200, content.size.toLong()); x.responseBody.use { it.write(content) }
    }
    override fun close() { server.stop(0); worker.shutdownNow(); requests.shutdownNow() }
}

fun serveExplorer(args: List<String>) {
    require(args.size == 2) { "Usage: explorer <frontend-dist-directory> <port>" }
    val assets = Path.of(args[0]); require(Files.isRegularFile(assets.resolve("index.html"))) { "Frontend build missing; run scripts/explorer.ps1" }
    val port = args[1].toInt(); require(port in 1024..65535) { "Port must be 1024..65535" }
    println("Loading pinned catalog…")
    val server = ExplorerServer(Catalog(), port, assets, System.getenv("MTG_APPLICATION_REVISION") ?: "unrecorded")
    Runtime.getRuntime().addShutdownHook(Thread { server.close() })
    server.start()
    println("Explorer ready: http://127.0.0.1:$port — Ctrl+C to stop")
    CountDownLatch(1).await()
}

