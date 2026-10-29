import java.security.MessageDigest

plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
    application
}

dependencies {
    implementation(project(":mtg-sdk"))
    implementation(project(":mtg-sdk-tooling"))
    implementation(project(":mtg-sets"))
    implementation(project(":rules-engine"))
    implementation(project(":mtg-search"))
    implementation(libs.kotlinxSerialization)
    implementation(libs.classgraph)
    testImplementation(testFixtures(project(":rules-engine")))
    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
}

application {
    mainClass.set("dev.mtg.assistant.MainKt")
    applicationName = "mtg-assistant"
    applicationDefaultJvmArgs = listOf("-Xmx1g", "-XX:ActiveProcessorCount=4")
}

// Capture the actual clean engine build, not a separately approved dependency pin.
fun engineGit(vararg args: String): String {
    val process = ProcessBuilder(listOf("git", "-C", rootProject.rootDir.absolutePath) + args)
        .redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader(Charsets.UTF_8).readText().trimEnd()
    check(process.waitFor() == 0) { output }
    return output
}
val engineRevision = engineGit("rev-parse", "HEAD")
fun verifyEngineUnchanged() {
    check(engineGit("rev-parse", "HEAD") == engineRevision && engineGit("status", "--porcelain").isEmpty()) {
        "Engine changed during the build; preserve the work and rerun against one clean revision"
    }
}
val engineImplementation = engineGit("ls-tree", "-r", "HEAD").lineSequence()
    .filter { it.substringAfter('\t') !in setOf("AGENTS.md", "README.md") && !it.substringAfter('\t').startsWith(".github/") }.joinToString("\n", postfix = "\n")
val engineImplementationSha256 = MessageDigest.getInstance("SHA-256")
    .digest(engineImplementation.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
val generatedEngineResources = layout.buildDirectory.dir("generated/engine-resources")
val recordEngineBuild by tasks.registering {
    inputs.property("engineRevision", engineRevision)
    inputs.property("engineImplementationSha256", engineImplementationSha256)
    inputs.property("mode", System.getenv("ARGENTUM_CANDIDATE_REVISION")?.let { "candidate" } ?: "app-stable")
    outputs.dir(generatedEngineResources)
    doLast {
        verifyEngineUnchanged()
        val output = generatedEngineResources.get().file("engine-build.properties").asFile
        output.parentFile.mkdirs()
        output.writeText("revision=$engineRevision\nimplementationSha256=$engineImplementationSha256\nmode=${inputs.properties["mode"]}\n")
    }
}
sourceSets.main { resources.srcDir(generatedEngineResources) }
tasks.processResources { dependsOn(recordEngineBuild) }
tasks.jar { doFirst { verifyEngineUnchanged() } }

// Stock Windows SET assignments break when the checkout path contains '&'.
tasks.startScripts {
    doLast {
        windowsScript.writeText(windowsScript.readText()
            .replace(Regex("(?m)^set ([A-Z_]+=[^\\r\\n]*)"), "set \"$1\"")
            .replace("do set APP_HOME=%%~fi", "do set \"APP_HOME=%%~fi\""))
    }
}

tasks.test {
    doFirst { verifyEngineUnchanged() }
    doLast { verifyEngineUnchanged() }
    // Frozen reviews are runtime test inputs; edits must invalidate Gradle test caching.
    inputs.dir(layout.projectDirectory.dir("../benchmark")).withPathSensitivity(PathSensitivity.RELATIVE)
    // Archived authoritative source snapshots are assertions in extension integration tests.
    inputs.dir(layout.projectDirectory.dir("../reference")).withPathSensitivity(PathSensitivity.RELATIVE)
    // The source-text facade guard must rerun even when a source edit leaves bytecode unchanged.
    inputs.dir(layout.projectDirectory.dir("src/main/kotlin/dev/mtg/assistant/extensions")).withPathSensitivity(PathSensitivity.RELATIVE)
    maxHeapSize = "1g"
    jvmArgs("-XX:ActiveProcessorCount=4")
    testLogging { events("passed", "failed", "skipped") }
}

// Full compilation keeps this small module independent of stale incremental symbols
// observed after native-serialization changes during development.
// Upstream compilation/caching remains untouched.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    incremental = false
}
