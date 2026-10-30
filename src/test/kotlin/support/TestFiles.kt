package dev.mtg.assistant

import java.nio.file.Path

/** One Gradle-supplied application root; tests do not guess from the working directory. */
internal object TestFiles {
    val root: Path = Path.of(System.getProperty("assistant.projectRoot", ".")).toAbsolutePath().normalize()
    fun fixture(relative: String): Path = root.resolve("src/test/resources").resolve(relative)
    fun slug(name: String): String = name.lowercase().replace("'", "").replace(Regex("[^a-z0-9]+"), "-").trim('-')
}
