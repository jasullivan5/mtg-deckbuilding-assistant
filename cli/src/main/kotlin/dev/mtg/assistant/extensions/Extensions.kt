package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.tooling.CardValidator
import kotlinx.serialization.Serializable

@Serializable data class Provenance(val origin: String, val source: String, val verifiedAt: String? = null)
data class ExtensionDefinition(val definition: CardDefinition, val provenance: Provenance)

/** Native definitions only. Registration is fail-closed on names, face aliases, and Oracle identity. */
object Extensions {
    val all: List<ExtensionDefinition> by lazy { listOf(ExtensionDefinition(JudithTheScourgeDiva,
        Provenance("APPLICATION_EXTENSION", "cli/src/main/kotlin/dev/mtg/assistant/extensions/JudithTheScourgeDiva.kt", "2026-10-25"))) }
    fun validate(upstream: List<CardDefinition>, additions: List<ExtensionDefinition>) {
        val names = upstream.map { it.name.lowercase() }.toMutableSet()
        val ids = upstream.mapNotNull { it.oracleId }.toMutableSet()
        fun namesOf(c: CardDefinition): Set<String> = buildSet {
            add(c.name.lowercase()); c.backFace?.let { addAll(namesOf(it)) }; c.flipSide?.let { addAll(namesOf(it)) }
            c.cardFaces.forEach { add(it.name.lowercase()) }
        }
        additions.forEach {
            require(it.definition.colorIdentityOverride != null && it.definition.legalFormats.isNotEmpty()) { "Extension requires verified legality and full color identity" }
            val errors = CardValidator.validate(it.definition)
            require(errors.isEmpty()) { "Invalid native extension ${it.definition.name}: $errors" }
            val aliases=namesOf(it.definition)
            require(aliases.none { name -> name in names }) { "Extension identity collision: ${it.definition.name}; no authored definition was overwritten" }
            it.definition.oracleId?.let { id -> require(ids.add(id)) { "Extension Oracle identity collision: $id (${it.definition.name})" } }
            names.addAll(aliases)
        }
    }
}
