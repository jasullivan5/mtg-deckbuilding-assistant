package dev.mtg.assistant

import java.util.Properties

/** Packaged with the compiled application, so installed runs never consult a moving checkout. */
object EngineBuild {
    private val build = Properties().apply {
        requireNotNull(EngineBuild::class.java.getResourceAsStream("/engine-build.properties")) {
            "Missing engine build provenance; build through dev/dev.ps1"
        }.use(::load)
    }
    val revision: String = build.getProperty("revision").also { require(it.matches(Regex("[0-9a-f]{40}"))) }
    val implementationSha256: String = build.getProperty("implementationSha256")
    val mode: String = build.getProperty("mode")
    private val historical = Properties().apply {
        requireNotNull(EngineBuild::class.java.getResourceAsStream("/engine-evidence.properties")).use(::load)
    }
    /**
     * The single historical binding is evidence, never dependency selection. Every
     * tracked engine file except root AGENTS.md/README.md and .github/ must have identical Git
     * mode/blob/path. A code/build/data change cannot inherit historical execution.
     */
    fun evidenceCompatible(recordedRevision: String, runtimeRevision: String = revision): Boolean =
        recordedRevision == runtimeRevision || (runtimeRevision == revision &&
            recordedRevision == historical.getProperty("revision") &&
            implementationSha256 == historical.getProperty("implementationSha256"))
}
