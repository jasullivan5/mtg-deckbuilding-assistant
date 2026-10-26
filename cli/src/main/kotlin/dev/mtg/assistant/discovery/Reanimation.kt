package dev.mtg.assistant.discovery

import dev.mtg.assistant.Catalog
import kotlinx.serialization.json.*

/** Objective B is only a composition of facts/predicates; native traversal lives in PathwayExtractor. */
fun factObjectiveReport(catalog: Catalog,commander: String,identity: List<String>,query: String?,
    eligibility: List<EligibilityEvidence>,counts: Map<String,Int>,extracted: PathwayExtractor,inputCards: List<String>,objective: String = "reanimate-own-creatures"): DiscoveryReport {
    val tokens=objective=="create-creature-tokens"
    val category=if(tokens) Contribution.CREATE_CREATURE_TOKEN else Contribution.REANIMATE_OWN_CREATURE
    val evaluated=if(tokens) tokenCreationFacts(extracted.facts).execute() else reanimationFacts(extracted.facts).execute()
    val byName=eligibility.associateBy { it.card }
    val actions=extracted.actions.associateBy { it.id }
    val results=evaluated.map { e ->
        val f=e.fact; val card=catalog.card(f.card)!!; val eligible=byName.getValue(f.card); val provenance=catalog.provenance(f.card)
        val related=extracted.facts.filter { it.costOrEffect.endsWith("COST") && (it.actionId==f.actionId || associatedSpellAction(actions,it.actionId)==f.actionId) }
        val nativeSource=DefinitionSources.paths[f.card]?.let { "https://github.com/wingedsheep/argentum-engine/blob/$ARGENTUM_SHA/$it" }
        ResultEvidence(f.card,card.oracleId ?: "${card.setCode}:${card.metadata.collectorNumber}:${card.name}",f.face,f.actionId,f.path,
            category,e.verdict,f.operation,f.costOrEffect,
            if(tokens) "created=${f.tokenCreation}; entry-controller=${f.destinationController}; binding=${f.binding}" else "source=${f.sourceZone}; graveyard/zone-player=${f.sourcePlayer}; native ownership/filter=${f.objectFilter}; destination=${f.destinationZone}; entry-controller=${f.destinationController}; selection=${f.selection}; binding=${f.binding}",
            f.prerequisites+listOfNotNull(actions[f.actionId]?.let { "Associated native action=${it.id}; prerequisites=${it.prerequisites}; native costs/targets retained in action evidence" })+
                related.map { "Associated ${it.costOrEffect} route ${it.actionId}: ${it.operation}; ${it.native}; prerequisites=${it.prerequisites}; alternative routes are alternatives, not cumulative costs" },
            null,f.native,e.decisions,listOfNotNull(nativeSource,if(provenance.origin=="APPLICATION_EXTENSION") provenance.source else "native:${f.face}.${f.path}"),
            eligible.format,eligible.deck,interpretation=if(extracted.unsupported.any { it.actionId==f.actionId && !(it.path==f.path && it.reason.startsWith("Effect not interpreted;")) }) "PARTIAL_ACTION_SUPPORTED_PATHWAY_NOT_GAMEPLAY_VERIFIED" else "STRUCTURAL_POTENTIAL_NOT_GAMEPLAY_VERIFIED",
            contributionClassification=if(e.verdict==Verdict.MATCH) category else null,
            controllerCompatibility=f.compatible(FactConstraints(controller=Controller.YOU,owner=if(tokens) null else Controller.YOU)),
            nontokenCompatibility=f.compatible(FactConstraints(creature=true,token=TokenStatus.NONTOKEN)),provenance=provenance,
            enablingRelationship=if(tokens) "NATIVE_CREATION_WITH_RETAINED_BRANCH_PREREQUISITES" else "NATIVE_MOVEMENT_WITH_RETAINED_BRANCH_PREREQUISITES",mechanicalCompatibility=e.verdict,
            mechanicalCapability=if(e.verdict==Verdict.MATCH) if(tokens) "CREATURE_TOKEN_CREATED_UNDER_OUR_CONTROL" else "OWN_GRAVEYARD_CREATURE_CARD_TO_OWN_BATTLEFIELD" else null,
            circumstances=f.circumstances,localSupport=if(e.verdict==Verdict.UNKNOWN) SupportStatus.PARTIALLY_SUPPORTED else SupportStatus.SUPPORTED)
    }
    // A recognized movement is not an unsupported SDK effect simply because A has no death projection.
    val movements=evaluated.groupBy { Triple(it.fact.card,it.fact.face,it.fact.path) }
    val support=extracted.structuralSupport.filterNot { it.reason.startsWith("Effect not interpreted;") && movements.containsKey(Triple(it.card,it.face,it.path)) }.toMutableList()
    movements.forEach { (key,es) -> support += StructuralSupport(key.first,key.second,key.third,
        if(tokens) es.first().fact.native.jsonObject["type"]!!.jsonPrimitive.content else if(es.first().fact.native.jsonObject["type"]?.jsonPrimitive?.content=="MoveCollection") "MoveCollectionEffect" else "MoveToZoneEffect",
        if(es.any { it.verdict==Verdict.UNKNOWN }) SupportStatus.PARTIALLY_SUPPORTED else SupportStatus.SUPPORTED,
        if(tokens) "Generic token facts retained per correlated variant; no whole-action/gameplay verification" else "Generic movement facts retained; objective compatibility assessed per correlated branch; no whole-action/gameplay verification",es.first().fact.actionId) }
    val unsupported=extracted.unsupported.filterNot { u -> u.reason.startsWith("Effect not interpreted;") && movements[Triple(u.card,u.face,u.path)]?.isNotEmpty()==true }
    val accepted=results.filter { it.verdict==Verdict.MATCH }
    return DiscoveryReport(objective=objective,commander=commander,allowedIdentity=identity,conventionalQuery=query,
        counts=counts+linkedMapOf("mechanical_facts" to extracted.facts.size,(if(tokens) "token_creation_facts" else "movement_facts") to evaluated.size,
            (if(tokens) "accepted_token_creation_pathways" else "accepted_reanimation_pathways") to accepted.size,(if(tokens) "accepted_token_creation_cards" else "accepted_reanimation_cards") to accepted.map { it.card }.distinct().size,
            "unknown_query_pathways" to results.count { it.verdict==Verdict.UNKNOWN },(if(tokens) "rejected_token_creation_pathways" else "rejected_movement_pathways") to results.count { it.verdict==Verdict.NO_MATCH },
            "unsupported_structure_records" to unsupported.size,"cards_with_unsupported_structures" to unsupported.map { it.card }.distinct().size)+(if(tokens) linkedMapOf(
            "creature_compatible_creation_pathways" to evaluated.count { all(it.decisions.take(2).map { d -> d.verdict })==Verdict.MATCH },
            "our_controller_compatible_creation_pathways" to evaluated.count { all(it.decisions.take(3).map { d -> d.verdict })==Verdict.MATCH },
            "accepted_fixed_quantity_pathways" to evaluated.count { it.verdict==Verdict.MATCH && it.fact.tokenCreation?.quantity is com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed },
            "accepted_dynamic_quantity_pathways" to evaluated.count { it.verdict==Verdict.MATCH && it.fact.tokenCreation?.quantity !is com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed }) else emptyMap()),
        eligibility=eligibility,results=results,actions=extracted.actions,unsupported=unsupported,
        limitations=discoveryLimitations+(if(tokens) listOf("Creature/predefined/batch/source-copy/target-copy token variants recognized. Specialized amass/populate/chosen-copy and token-granted abilities are not automatically interpreted.","Quantity/controller/type are correlated per native variant. Positive count and copiable characteristics are prerequisites, not guaranteed gameplay outcomes.") else listOf("Reanimation query recognizes direct original-card graveyard-to-battlefield movement only; linked intermediate-zone sequences are retained as separate facts, not composed into reanimation.",
            "Ownership is distinct from battlefield control. Owner-default entry is evaluated using the same chosen owner assignment as source-graveyard compatibility.",
            "Unsupported predicates/bindings/selection restrictions retain UNKNOWN. Card availability, target legality, timing, resources and actual resolution are not guaranteed.")),
        structuralSupport=support,facts=extracted.facts,mechanicalInputCards=inputCards)
}

fun reanimationReport(catalog: Catalog,commander: String,identity: List<String>,query: String?,
    eligibility: List<EligibilityEvidence>,counts: Map<String,Int>,extracted: PathwayExtractor,inputCards: List<String>) =
    factObjectiveReport(catalog,commander,identity,query,eligibility,counts,extracted,inputCards)
