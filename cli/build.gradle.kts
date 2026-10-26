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

tasks.test {
    // Frozen reviews are runtime test inputs; edits must invalidate Gradle test caching.
    inputs.dir(layout.projectDirectory.dir("../benchmark")).withPathSensitivity(PathSensitivity.RELATIVE)
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
