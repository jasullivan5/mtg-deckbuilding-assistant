package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.tooling.CardValidator
import com.wingedsheep.sdk.tooling.LintSeverity
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.Locale

@Serializable data class Provenance(val origin: String, val source: String, val verifiedAt: String? = null)
/** Recorded review scope, never a certificate of complete mechanical correctness. */
data class DefinitionReview(val scenarioReferences: List<String> = emptyList(), val knownLimitations: List<String> = emptyList(), val assuranceEvidence: TranslationEvidence? = null)
data class ExtensionDefinition(val definition: CardDefinition, val provenance: Provenance, val review: DefinitionReview = DefinitionReview())

/** Native definitions only. Registration is fail-closed on names, face aliases, and Oracle identity. */
object Extensions {
    val all: List<ExtensionDefinition> by lazy { listOf(
        ExtensionDefinition(PredatoryUrge,Provenance("APPLICATION_EXTENSION","src/main/kotlin/extensions/PredatoryUrge.kt","2026-10-28"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/PredatoryUrgeScenarioTest.kt"))),
        ExtensionDefinition(SwordOfTruthAndJustice,Provenance("APPLICATION_EXTENSION","src/main/kotlin/extensions/SwordOfTruthAndJustice.kt","2026-10-28"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/SwordOfTruthAndJusticeScenarioTest.kt"))),
        ExtensionDefinition(SpinedTyrranax,Provenance("APPLICATION_EXTENSION","src/main/kotlin/extensions/SpinedTyrranax.kt","2026-10-28"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/SpinedTyrranaxScenarioTest.kt"))),
        ExtensionDefinition(MightOfMurasa,Provenance("APPLICATION_EXTENSION","src/main/kotlin/extensions/MightOfMurasa.kt","2026-10-28"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/MightOfMurasaScenarioTest.kt"))),
        ExtensionDefinition(SavageConception,Provenance("APPLICATION_EXTENSION","src/main/kotlin/extensions/SavageConception.kt","2026-10-28"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/SavageConceptionScenarioTest.kt"))),
        ExtensionDefinition(ParadiseMantle,Provenance("APPLICATION_EXTENSION","src/main/kotlin/extensions/ParadiseMantle.kt","2026-10-28"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/ParadiseMantleScenarioTest.kt"))),
        ExtensionDefinition(LabyrinthOfSkophos,Provenance("APPLICATION_EXTENSION","src/main/kotlin/extensions/LabyrinthOfSkophos.kt","2026-10-28"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/LabyrinthOfSkophosScenarioTest.kt"))),
        ExtensionDefinition(MagusOfTheLibrary,Provenance("APPLICATION_EXTENSION","src/main/kotlin/extensions/MagusOfTheLibrary.kt","2026-10-28"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/MagusOfTheLibraryScenarioTest.kt"))),
        ExtensionDefinition(GaladhrimAmbush,Provenance("APPLICATION_EXTENSION","src/main/kotlin/extensions/GaladhrimAmbush.kt","2026-10-28"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/GaladhrimAmbushScenarioTest.kt"))),
        ExtensionDefinition(HourOfPromise,Provenance("APPLICATION_EXTENSION","src/main/kotlin/extensions/HourOfPromise.kt","2026-10-28"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/HourOfPromiseScenarioTest.kt"))),
        ExtensionDefinition(Momentum,Provenance("APPLICATION_EXTENSION","src/main/kotlin/extensions/Momentum.kt","2026-10-28"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/MomentumScenarioTest.kt"))),
        ExtensionDefinition(ThroneOfMakindi,Provenance("APPLICATION_EXTENSION","src/main/kotlin/extensions/ThroneOfMakindi.kt","2026-10-28"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/ThroneOfMakindiScenarioTest.kt"))),
        ExtensionDefinition(ExperimentTwelve,Provenance("APPLICATION_EXTENSION","src/main/kotlin/extensions/ExperimentTwelve.kt","2026-10-27"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/ExperimentTwelveScenarioTest.kt"))),
        ExtensionDefinition(PlanewideCelebration,Provenance("APPLICATION_EXTENSION","src/main/kotlin/extensions/PlanewideCelebration.kt","2026-10-27"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/PlanewideCelebrationScenarioTest.kt"))),
        ExtensionDefinition(MortalWound,Provenance("APPLICATION_EXTENSION","src/main/kotlin/extensions/MortalWound.kt","2026-10-27"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/MortalWoundScenarioTest.kt"))),
        ExtensionDefinition(Tatterkite,Provenance("APPLICATION_EXTENSION","src/main/kotlin/extensions/Tatterkite.kt","2026-10-27"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/TatterkiteScenarioTest.kt"))),
        ExtensionDefinition(TyriteSanctum,Provenance("APPLICATION_EXTENSION","src/main/kotlin/extensions/TyriteSanctum.kt","2026-10-27"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/TyriteSanctumScenarioTest.kt"))),
        ExtensionDefinition(LeyWeaver,Provenance("APPLICATION_EXTENSION","src/main/kotlin/extensions/LeyWeaver.kt","2026-10-27"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/LeyWeaverScenarioTest.kt"))),
        ExtensionDefinition(KrenkosCommand,
            Provenance("APPLICATION_EXTENSION", "src/main/kotlin/extensions/KrenkosCommand.kt", "2026-10-26"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/TokenCardScenarioTest.kt"))),
        ExtensionDefinition(Bitterblossom,
            Provenance("APPLICATION_EXTENSION", "src/main/kotlin/extensions/Bitterblossom.kt", "2026-10-26"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/TokenCardScenarioTest.kt"))),
        ExtensionDefinition(JudithTheScourgeDiva,
            Provenance("APPLICATION_EXTENSION", "src/main/kotlin/extensions/JudithTheScourgeDiva.kt", "2026-10-25"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/JudithTheScourgeDivaScenarioTest.kt"))),
        ExtensionDefinition(OviyaPashiriSageLifecrafter,
            Provenance("APPLICATION_EXTENSION", "src/main/kotlin/extensions/OviyaPashiriSageLifecrafter.kt", "2026-10-26"),
            DefinitionReview(listOf("src/test/kotlin/scenarios/OviyaPashiriSageLifecrafterScenarioTest.kt")))
    ).map { it.copy(review=it.review.copy(assuranceEvidence=RecordedTranslationEvidence.find(it.definition.oracleId))) } }
    private fun definitions(c: CardDefinition): List<CardDefinition> = listOf(c) +
        (c.backFace?.let(::definitions) ?: emptyList()) + (c.flipSide?.let(::definitions) ?: emptyList())
    private fun namesOf(c: CardDefinition): Set<String> = definitions(c).map { it.name }
        .map { it.lowercase(Locale.ROOT) }.toSet()
    private fun idsOf(c: CardDefinition): Set<String> = definitions(c).mapNotNull { it.oracleId?.lowercase(Locale.ROOT) }.toSet()
    fun validate(upstream: List<CardDefinition>, additions: List<ExtensionDefinition>): List<String> {
        val warnings = mutableListOf<String>()
        val names=upstream.flatMap(::namesOf).toMutableSet()
        val ids=upstream.flatMap(::idsOf).toMutableSet()
        additions.forEach { extension ->
            val card=extension.definition
            require(extension.provenance.origin=="APPLICATION_EXTENSION" && extension.provenance.source.isNotBlank() &&
                extension.provenance.verifiedAt?.let { runCatching { LocalDate.parse(it) }.isSuccess }==true) {
                "Extension requires dated application-owned source provenance: ${card.name}"
            }
            require(!card.oracleId.isNullOrBlank()) { "Extension requires verified Oracle identity: ${card.name}" }
            require(card.colorIdentityOverride != null && card.legalFormats.isNotEmpty()) { "Extension requires verified legality and full color identity" }
            require(extension.review.knownLimitations.isEmpty()) { "Refusing known-incomplete extension: ${card.name}; ${extension.review.knownLimitations}" }
            require(extension.review.scenarioReferences.none { it.isBlank() }) { "Empty scenario reference: ${card.name}" }
            definitions(card).forEach { definition ->
                val findings=CardValidator.validate(definition)
                val errors=findings.filter { it.severity == LintSeverity.ERROR }
                warnings += findings.filter { it.severity == LintSeverity.WARNING }.map { "${definition.name}: ${it.message}" }
                require(errors.isEmpty()) { "Invalid native extension ${definition.name}: $errors" }
            }
            val aliases=namesOf(card)
            require(aliases.none { it in names }) { "Extension identity collision: ${card.name}; no authored definition was overwritten" }
            val oracleIds=idsOf(card)
            require(oracleIds.none { it in ids }) { "Extension Oracle identity collision: ${card.oracleId} (${card.name})" }
            names.addAll(aliases); ids.addAll(oracleIds)
        }
        return warnings
    }
}
