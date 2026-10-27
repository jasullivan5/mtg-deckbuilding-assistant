package dev.mtg.assistant.discovery

import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CommanderEligibility
import com.wingedsheep.sdk.tooling.CardValidator
import dev.mtg.assistant.Catalog
import dev.mtg.assistant.extensions.DefinitionReview
import dev.mtg.assistant.extensions.Provenance
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable enum class DefinitionAvailability { UPSTREAM_NATIVE, APPLICATION_EXTENSION, MISSING_FROM_PINNED_CATALOG }
@Serializable enum class CommanderBlocker { INVALID_REQUEST, MISSING_DEFINITION, NOT_PHYSICAL_FRONT, LEGALITY_UNRESOLVED, NOT_COMMANDER_LEGAL, IDENTITY_UNRESOLVED, INELIGIBLE_COMMANDER, UNSUPPORTED_CONSTRUCTION, INVALID_NATIVE_DEFINITION }
@Serializable enum class MechanicalReviewStatus { NOT_INDEPENDENTLY_VERIFIED, ENGINE_SCENARIOS_RECORDED, KNOWN_INCOMPLETE }
@Serializable data class CommanderProblem(val code: CommanderBlocker, val reason: String, val nextAction: String)
@Serializable data class CommanderInspection(
    val requestedName: String, val definitionName: String?, val availability: DefinitionAvailability,
    val provenance: Provenance?, val formatLegality: Verdict?, val authoritativeIdentity: Boolean,
    val colorIdentity: List<String>, val nativeCommanderEligible: Boolean?, val physicalFront: Boolean,
    val blockers: List<CommanderProblem>, val mechanicalReview: MechanicalReviewStatus,
    val nativeValidationErrors: List<String>, val scenarioReferences: List<String>, val knownLimitations: List<String>,
    val constructionScope: String = "Single fixed-identity commander; pairs and chosen-color construction unsupported",
    val mechanicalCaveat: String = "Selection eligibility and structural validation do not certify complete rules correctness; scenarios verify only their recorded cases",
    val selectable: Boolean = blockers.isEmpty()
)

/** Pure inspection also accepts isolated SDK fixtures; no card-name exceptions or Oracle effect parser. */
fun inspectCommanderDefinition(name: String, card: CardDefinition?, provenance: Provenance? = null,
    physicalFront: Boolean = true, review: DefinitionReview = DefinitionReview()): CommanderInspection {
    val problems=mutableListOf<CommanderProblem>()
    fun block(code: CommanderBlocker, reason: String, action: String) { problems += CommanderProblem(code,reason,action) }
    if(name.isBlank()) block(CommanderBlocker.INVALID_REQUEST,"A single exact commander name is required; pairs are unsupported","Supply one physical card's exact front name")
    if(card==null) {
        block(CommanderBlocker.MISSING_DEFINITION,"No exact native definition for '$name' in the pinned catalog; this does not establish whether the Magic card exists","Check exact spelling/case, then verify authoritative sources and author a reviewed native extension if absent")
        return CommanderInspection(name,null,DefinitionAvailability.MISSING_FROM_PINNED_CATALOG,null,null,false,emptyList(),null,false,problems,MechanicalReviewStatus.NOT_INDEPENDENTLY_VERIFIED,emptyList(),emptyList(),emptyList())
    }
    val legal=commanderFormatLegality(card)
    val eligible=CommanderEligibility.isLegalCommander(card)
    if(!physicalFront) block(CommanderBlocker.NOT_PHYSICAL_FRONT,"Select the physical card's front name; back/flip/meld aliases cannot be commanders here","Inspect and select its physical front card")
    if(legal==Verdict.UNKNOWN) block(CommanderBlocker.LEGALITY_UNRESOLVED,"Commander legality metadata is unresolved","Verify current format legality before supplying metadata")
    if(legal==Verdict.NO_MATCH) block(CommanderBlocker.NOT_COMMANDER_LEGAL,"The definition's verified formats do not include Commander","Check current authoritative legality; do not infer legality from card type")
    if(card.colorIdentityOverride==null) block(CommanderBlocker.IDENTITY_UNRESOLVED,"Commander has no authoritative full identity override","Verify full identity including all faces; heuristic colors are insufficient")
    val chosen=card.oracleText.contains("before the game begins",true) && card.oracleText.contains("color",true)
    val specialForm=Supertype.LEGENDARY in card.typeLine.supertypes && card.typeLine.subtypes.any { it.value in setOf("Vehicle","Spacecraft") }
    if(chosen || CommanderEligibility.isBackground(card) || (!eligible && specialForm))
        block(CommanderBlocker.UNSUPPORTED_CONSTRUCTION,"Commander construction requires unsupported chosen colors, pairing, or a special commander form","Use a supported single fixed-identity commander; do not approximate construction rules")
    else if(!eligible) block(CommanderBlocker.INELIGIBLE_COMMANDER,"Not a supported commander: native eligibility rejects this card","Choose a legendary creature or a native explicit commander override")
    val errors=CardValidator.validate(card).map { it.toString() }
    if(errors.isNotEmpty()) block(CommanderBlocker.INVALID_NATIVE_DEFINITION,"Native structural validation failed","Repair and test the application extension or report the upstream limitation")
    val mechanical=when {
        errors.isNotEmpty() || review.knownLimitations.isNotEmpty() -> MechanicalReviewStatus.KNOWN_INCOMPLETE
        review.scenarioReferences.isNotEmpty() -> MechanicalReviewStatus.ENGINE_SCENARIOS_RECORDED
        else -> MechanicalReviewStatus.NOT_INDEPENDENTLY_VERIFIED
    }
    return CommanderInspection(name,card.name,if(provenance?.origin=="APPLICATION_EXTENSION") DefinitionAvailability.APPLICATION_EXTENSION else DefinitionAvailability.UPSTREAM_NATIVE,
        provenance,legal,card.colorIdentityOverride!=null,fullIdentity(card).map { it.name }.sorted(),eligible,physicalFront,problems,mechanical,errors,review.scenarioReferences,review.knownLimitations)
}

fun inspectCommander(catalog: Catalog, name: String): CommanderInspection {
    val card=catalog.card(name)
    return inspectCommanderDefinition(name,card,card?.let { catalog.provenance(it.name) },
        card!=null && physicalCards(catalog.cards).any { it.name==card.name },catalog.definitionReview(name))
}

fun commanderCommand(args: List<String>, catalog: Catalog, out: (String)->Unit) {
    require(args.firstOrNull()=="inspect") { "Usage: commander inspect <exact name> [--json]" }
    val values=args.drop(1)
    val json=values.lastOrNull()=="--json"
    val names=if(json) values.dropLast(1) else values
    require(names.isNotEmpty() && names.none { it.startsWith("--") }) { "Usage: commander inspect <exact name> [--json]" }
    val report=inspectCommander(catalog,names.joinToString(" "))
    if(json) out(reportJson.encodeToString(report)) else out(buildString {
        appendLine("Commander: ${report.requestedName}")
        appendLine("Availability: ${report.availability}; selectable: ${report.selectable}")
        appendLine("Provenance: ${report.provenance ?: "No native definition"}")
        appendLine("Commander legality: ${report.formatLegality}; authoritative identity: ${report.authoritativeIdentity}; colors: ${report.colorIdentity}")
        appendLine("Native commander eligibility: ${report.nativeCommanderEligible}; physical front: ${report.physicalFront}")
        report.blockers.forEach { appendLine("${it.code}: ${it.reason}. Next: ${it.nextAction}") }
        appendLine("Mechanical review: ${report.mechanicalReview}")
        report.nativeValidationErrors.forEach { appendLine("Native validation: $it") }
        report.knownLimitations.forEach { appendLine("Known limitation: $it") }
        report.scenarioReferences.forEach { appendLine("Recorded scenarios: $it") }
        appendLine(report.constructionScope)
        appendLine(report.mechanicalCaveat)
    }.trimEnd())
}
