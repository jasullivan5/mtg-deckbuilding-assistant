package dev.mtg.assistant.discovery

import com.wingedsheep.sdk.model.CardDefinition
import dev.mtg.assistant.Catalog
import io.github.classgraph.ClassGraph
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.encodeToString

/** Source locations are derived from compiled top-level getters, never a card-name table/parser. */
internal object DefinitionSources {
    val paths: Map<String,String> by lazy {
        val result=mutableMapOf<String,String>()
        ClassGraph().acceptPackages("com.wingedsheep.mtg.sets.definitions").enableClassInfo().scan(4).use { scan ->
            for(info in scan.allClasses.sortedBy { it.name }) {
                if(!info.name.endsWith("Kt")) continue
                val location=info.classpathElementURI.toString().replace('\\','/')
                val era=Regex("/mtg-sets/([^/]+)/build/").find(location)?.groupValues?.get(1)
                    ?: Regex("/(\\d{4}(?:-\\d{4})?)\\.jar$").find(location)?.groupValues?.get(1)
                    ?: if(location.endsWith("/core.jar")) "core" else null
                val prefix=if(era == null) "mtg-sets" else "mtg-sets/$era"
                val source=info.sourceFile ?: continue
                val path="$prefix/src/main/kotlin/${info.packageName.replace('.','/')}/$source"
                val clazz=info.loadClass()
                for(method in clazz.declaredMethods) {
                    if(Modifier.isPublic(method.modifiers) && Modifier.isStatic(method.modifiers) && method.parameterCount == 0 && method.returnType == CardDefinition::class.java) {
                        val definition=runCatching { method.invoke(null) as CardDefinition }.getOrNull() ?: continue
                        result.putIfAbsent(definition.name,path)
                    }
                }
            }
        }
        result
    }
}
val discoveryLimitations = listOf(
    "Format legality is the pinned Argentum snapshot. Empty legality is UNKNOWN and excluded, never assumed legal.",
    "Single, fixed-identity commanders supported through native eligibility. Pairs, pre-game chosen colors, newer Vehicle/Spacecraft commander forms, and special construction exceptions are unsupported.",
    "No whole-deck size, singleton, companion, or outside-game validation is performed by a candidate discovery query.",
    "Full identity uses native identity plus alternative faces; conventional color search is not used for eligibility.",
    "MATCH means structural potential given stated prerequisites, not a guaranteed legal action or a gameplay-verified death. Replacement, prevention, indestructibility, resource and timing state can change outcomes.",
    "Unknown filters, conditions, targets, costs, effects and keyword-granted abilities remain in diagnostics; no claim of exhaustive catalog recall.",
    "Death enhancement covers positive condition gates with identifiable removal improvement, death-triggered removal, native conditional magnitudes, activation discounts, and supported self-cast morbid discounts. Other relationships remain UNKNOWN.",
    "Back/flip aliases are excluded from card counts; alternative face scripts remain attached to the physical card. Some native layouts expose overlapping scripts; paths identify the exact authored representations.",
    "No card-name matching or Oracle-text matching is used by mechanical extraction. Native commander designation and the unsupported chosen-color selection guard are the only text-based eligibility checks."
)
fun discover(catalog: Catalog, commanderName: String, conventionalQuery: String? = null, objective: String = "own-nontoken-creature-deaths"): DiscoveryReport {
    require(objective in setOf("own-nontoken-creature-deaths","reanimate-own-creatures","create-creature-tokens")) { "Unsupported objective: $objective" }
    val commander=resolveCommander(catalog,commanderName)
    val allowed=fullIdentity(commander)
    val roots=physicalCards(catalog.cards)
    val eligibility=roots.map { card ->
        val format=commanderFormatLegality(card)
        // Filter B is independent; the actual pipeline below requires Filter A first.
        val deck=selectedCommanderEligibility(card,allowed)
        EligibilityEvidence(card.name,format,deck,fullIdentity(card).map { it.name }.sorted(),
            "format from verified source legalFormats; identity must be a full subset of ${allowed.map { it.name }.sorted()}",catalog.provenance(card.name))
    }
    val byName=eligibility.associateBy { it.card }
    val legal=roots.filter { byName.getValue(it.name).format == Verdict.MATCH }
    val eligible=legal.filter { byName.getValue(it.name).deck == Verdict.MATCH }
    val conventionalNames=conventionalQuery?.let { catalog.search(it).map { it.name }.toSet() }
    val input=eligible.filter { conventionalNames == null || it.name in conventionalNames }
    val extracted=PathwayExtractor().extract(input)
    if(objective in setOf("reanimate-own-creatures","create-creature-tokens")) return factObjectiveReport(catalog,commander.name,allowed.map { it.name }.sorted(),conventionalQuery,eligibility,
        linkedMapOf("authored_physical_cards" to roots.size,"upstream_authored_physical_cards" to roots.count { catalog.provenance(it.name).origin=="UPSTREAM_ARGENTUM" },
            "application_extension_physical_cards" to roots.count { catalog.provenance(it.name).origin=="APPLICATION_EXTENSION" },"registry_names_including_faces" to catalog.cards.size,
            "commander_legal_cards" to legal.size,"format_legality_unknown_cards" to eligibility.count { it.format==Verdict.UNKNOWN },
            "commander_eligible_cards" to eligible.size,"identity_unknown_legal_cards" to legal.count { byName.getValue(it.name).deck==Verdict.UNKNOWN },
            "mechanical_input_cards_after_conventional_search" to input.size),extracted,input.map { it.name },objective)
    val direct=creatureDeathPathways(extracted.pathways).where(canAffectCreature())
        .where(canAffectController(Controller.YOU)).where(canAffectTokenStatus(TokenStatus.NONTOKEN))
        .where(satisfiesDeathGenerationPolicy()).execute()
    // Independent branch: never downstream of direct-generation or its policy.
    val enhanced=MechanicalQuery(extracted.pathways.filter { it.branch == Contribution.DEATH_ENHANCED_REMOVAL })
        .where(canAffectCreature()).where(canAffectController(Controller.YOU))
        .where(canAffectTokenStatus(TokenStatus.NONTOKEN)).where(hasDeathEnhancement()).execute()
    fun through(stage: Int) = direct.count { all(it.decisions.take(stage+1).map { d -> d.verdict }) == Verdict.MATCH }
    val acceptedDirect=direct.filter { it.verdict == Verdict.MATCH }
    val acceptedEnhanced=enhanced.filter { it.verdict == Verdict.MATCH }
    val evaluated=direct+enhanced
    val extractedActions=extracted.actions.associateBy { it.id }
    val results=evaluated.map { evaluatedPath ->
        val p=evaluatedPath.pathway
        val eligibilityEvidence=byName.getValue(p.card.name)
        val provenance=catalog.provenance(p.card.name)
        val source=if(provenance.origin == "UPSTREAM_ARGENTUM") DefinitionSources.paths[p.card.name] else null
        ResultEvidence(p.card.name,p.card.oracleId ?: "${p.card.setCode}:${p.card.metadata.collectorNumber}:${p.card.name}",
            p.face,p.actionId,p.path,p.branch,evaluatedPath.verdict,p.operation,p.origin,p.scope.description,p.prerequisites,p.dependency,p.native,
            evaluatedPath.decisions,listOfNotNull(source?.let { "https://github.com/wingedsheep/argentum-engine/blob/$ARGENTUM_SHA/$it" },
                if(provenance.origin == "APPLICATION_EXTENSION") provenance.source else "native:${p.face}.${p.path}","https://github.com/wingedsheep/argentum-engine/tree/$ARGENTUM_SHA/mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting"),
            eligibilityEvidence.format,eligibilityEvidence.deck,
            interpretation=if(extracted.unsupported.any { it.actionId == p.actionId || it.actionId==associatedSpellAction(extractedActions,p.actionId) }) "PARTIAL_ACTION_SUPPORTED_PATHWAY_NOT_GAMEPLAY_VERIFIED" else "STRUCTURAL_POTENTIAL_NOT_GAMEPLAY_VERIFIED",
            contributionClassification=if(evaluatedPath.verdict == Verdict.MATCH) p.branch else null,
            provenance=provenance,enablingRelationship=p.relationship,
            mechanicalCompatibility=all(evaluatedPath.decisions.filter { it.basis=="MECHANICAL" }.map { it.verdict }),
            policyAcceptance=evaluatedPath.decisions.lastOrNull { it.basis=="POLICY" }?.verdict,
            mechanicalCapability=if(all(evaluatedPath.decisions.filter { it.basis=="MECHANICAL" }.map { it.verdict })==Verdict.MATCH)
                (if(p.branch==Contribution.DIRECT_DEATH_GENERATOR) "CREATURE_DEATH_PATHWAY" else "DEATH_EVENT_REMOVAL_DEPENDENCY") else null,
            circumstances=p.circumstances,
            localSupport=if(all(evaluatedPath.decisions.filter { it.basis=="MECHANICAL" }.map { it.verdict })==Verdict.UNKNOWN || p.scope.unresolved!=null || p.operationCompatibility==Verdict.UNKNOWN || p.dependencyVerdict==Verdict.UNKNOWN && p.branch==Contribution.DEATH_ENHANCED_REMOVAL) SupportStatus.PARTIALLY_SUPPORTED else SupportStatus.SUPPORTED,
            controllerCompatibility=p.scope.compatible(Requirements(controller=Controller.YOU)),
            nontokenCompatibility=p.scope.compatible(Requirements(token=TokenStatus.NONTOKEN)))
    }
    val counts=linkedMapOf(
        "authored_physical_cards" to roots.size,
        "upstream_authored_physical_cards" to roots.count { catalog.provenance(it.name).origin == "UPSTREAM_ARGENTUM" },
        "application_extension_physical_cards" to roots.count { catalog.provenance(it.name).origin == "APPLICATION_EXTENSION" },
        "registry_names_including_faces" to catalog.cards.size,
        "commander_legal_cards" to legal.size,
        "format_legality_unknown_cards" to eligibility.count { it.format == Verdict.UNKNOWN },
        "commander_eligible_cards" to eligible.size,
        "identity_unknown_legal_cards" to legal.count { byName.getValue(it.name).deck == Verdict.UNKNOWN },
        "mechanical_input_cards_after_conventional_search" to input.size,
        "extracted_potential_death_operations" to direct.size,
        "potential_creature_death_pathways" to through(0),
        "our_creature_pathways" to through(1),
        "our_nontoken_creature_pathways" to through(2),
        "mechanically_compatible_direct_pathways" to direct.count { all(it.decisions.filter { d -> d.basis=="MECHANICAL" }.map { d -> d.verdict })==Verdict.MATCH },
        "mechanically_compatible_policy_rejected_pathways" to direct.count { all(it.decisions.filter { d -> d.basis=="MECHANICAL" }.map { d -> d.verdict })==Verdict.MATCH && it.decisions.last().verdict==Verdict.NO_MATCH },
        "accepted_direct_pathways" to acceptedDirect.size,
        "accepted_direct_cards" to acceptedDirect.map { it.pathway.card.name }.distinct().size,
        "accepted_self_toughness_exchange_pathways" to acceptedDirect.count { it.pathway.relationship == "SELECTABLE_SELF_TOUGHNESS_EXCHANGE" },
        "mandatory_unlinked_sacrifice_pathways" to direct.count { it.pathway.operation == "SACRIFICE" && it.pathway.relationship == "MANDATORY_OR_INCIDENTAL_TRIGGER" },
        "policy_unknown_compatible_pathways" to direct.count { it.decisions.last().verdict == Verdict.UNKNOWN && all(it.decisions.take(3).map { d -> d.verdict }) == Verdict.MATCH },
        "accepted_death_enhanced_removal_pathways" to acceptedEnhanced.size,
        "accepted_death_enhanced_removal_cards" to acceptedEnhanced.map { it.pathway.card.name }.distinct().size,
        "policy_rejected_removal_pathways" to direct.count { e -> e.decisions.last().verdict == Verdict.NO_MATCH && all(e.decisions.take(3).map { it.verdict }) == Verdict.MATCH },
        "controller_incompatible_pathways" to direct.count { it.decisions[1].verdict == Verdict.NO_MATCH && it.decisions[0].verdict != Verdict.NO_MATCH },
        "nontoken_incompatible_pathways" to direct.count { it.decisions[2].verdict == Verdict.NO_MATCH && all(it.decisions.take(2).map { d -> d.verdict }) != Verdict.NO_MATCH },
        "unknown_query_pathways" to evaluated.count { it.verdict == Verdict.UNKNOWN },
        "pathways_with_any_unknown_decision" to evaluated.count { e -> e.decisions.any { it.verdict == Verdict.UNKNOWN } },
        "unsupported_structure_records" to extracted.unsupported.size,
        "cards_with_unsupported_structures" to extracted.unsupported.map { it.card }.distinct().size
    )
    return DiscoveryReport(objective="own-nontoken-creature-deaths",commander=commander.name,
        allowedIdentity=allowed.map { it.name }.sorted(),conventionalQuery=conventionalQuery,counts=counts,
        eligibility=eligibility,results=results,actions=extracted.actions,unsupported=extracted.unsupported,limitations=discoveryLimitations,structuralSupport=extracted.structuralSupport,facts=extracted.facts,mechanicalInputCards=input.map { it.name })
}

fun discoveryCommand(args: List<String>, catalog: Catalog, out: (String)->Unit) {
    var commander: String?=null; var query: String?=null; var output: String?=null
    var objective="own-nontoken-creature-deaths"
    var json=false; var diagnostics=false; var i=0
    while(i<args.size) {
        val key=args[i++]
        fun value(): String { require(i<args.size) { "$key requires a value" }; return args[i++] }
        when(key) {
            "--commander" -> { require(commander == null) { "Only one commander is supported" }; commander=value() }
            "--objective" -> objective=value()
            "--search" -> query=value()
            "--output" -> output=value()
            "--json" -> json=true
            "--diagnostics" -> diagnostics=true
            else -> throw IllegalArgumentException("Unknown discover option: $key")
        }
    }
    val report=discover(catalog,commander ?: throw IllegalArgumentException("discover requires --commander <exact name>"),query,objective)
    val text=if(json) reportJson.encodeToString(report) else buildString {
        appendLine("Objective: ${report.objective}")
        appendLine("Commander: ${report.commander}; allowed identity: ${report.allowedIdentity}")
        report.counts.forEach { (k,v)-> appendLine("$k: $v") }
        for(branch in Contribution.entries) {
            appendLine("\n$branch")
            report.results.filter { it.branch == branch && it.verdict == Verdict.MATCH }.forEach { r ->
                appendLine("${r.card} | ${r.face} | ${r.abilityPath} | ${r.operation} | ${r.costOrEffect}")
                appendLine("  Mechanical=${r.mechanicalCompatibility}; policy=${r.policyAcceptance ?: "N/A"}; desirability=${r.strategicDesirability}; circumstances=${r.circumstances}")
                appendLine("  Scope: ${r.affectedScope}; dependency: ${r.dependency ?: "none required"}")
                appendLine("  ${r.decisions.joinToString { "${it.stage}=${it.verdict}: ${it.reason}" }}")
                appendLine("  Action evidence: ${r.actionId}; prerequisites: ${r.prerequisites.joinToString()}")
                appendLine("  Sources: ${r.sourceReferences.joinToString()}")
            }
        }
        if(diagnostics) {
            appendLine("\nREJECTED AND UNKNOWN PATHWAYS")
            report.results.filter { it.verdict != Verdict.MATCH }.forEach { appendLine("${it.card} | ${it.abilityPath} | ${it.verdict} | ${it.decisions}") }
            appendLine("\nUNSUPPORTED STRUCTURES")
            report.unsupported.forEach { appendLine(it) }
        } else appendLine("\nUse --diagnostics for rejected/unknown details; --json exports all decisions, native actions, eligibility and unsupported records.")
        report.limitations.forEach { appendLine("Coverage: $it") }
    }
    if(output != null) {
        val destination=Path.of(output)
        destination.toAbsolutePath().parent?.let(Files::createDirectories)
        Files.writeString(destination,text)
        out("Wrote ${destination.toAbsolutePath()}")
    } else out(text)
}
