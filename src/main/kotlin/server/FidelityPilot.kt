package dev.mtg.assistant.explorer

import dev.mtg.assistant.Catalog
import dev.mtg.assistant.EngineBuild
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.extensions.definitionDigest
import dev.mtg.assistant.extensions.evidenceTextDigest
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.time.Instant
import java.util.Properties

@Serializable data class PilotScope(val card: String, val sourceFile: String, val sourceSha256: String, val oracleId: String,
    val printingId: String, val role: String, val targetRequired: Boolean, val tapRequired: Boolean, val creatureAtArrival: Boolean, val heldOut: Boolean)
@Serializable data class PilotCheck(val requirement: String, val state: String, val observation: String)
@Serializable data class PilotReview(val scope: PilotScope, val definitionSha256: String?, val fact: MechanicalFact?, val checks: List<PilotCheck>)
@Serializable data class PilotExecution(val scenarioId: String, val card: String, val mechanic: String, val state: String,
    val observations: JsonElement, val problem: String? = null)
@Serializable data class PilotReceipt(val applicationRevision: String, val engine: String, val executedAt: String,
    val requirementsSha256: String, val harnessSha256: String, val definitions: Map<String,String>, val scenarios: List<PilotExecution>)
@Serializable data class FidelityPilot(val engine: String, val requirements: JsonObject, val requirementsSha256: String,
    val reviews: List<PilotReview>, val counts: Map<String,Int>, val potentialReach: Map<String,Int>,
    val executionState: String, val recordedExecution: PilotReceipt?,
    val qualification: String = "AUTHOR_LED_ABILITY_SCOPES_NOT_INDEPENDENTLY_QUALIFIED",
    val limits: List<String> = listOf("One source-bound sacrifice ability per card, not whole-card fidelity. No inherited ledger status is promoted.",
        "Requirements and current sources were retained before these native checks. Existing historical investigations were known; held-out is not a blind independent human evaluation.",
        "Declared targets and timing are bounded structure checks; full satisfiability and every target/effect branch remain outside scope.",
        "Potential structural reach is not source-reviewed coverage. No population-wide fidelity or false-positive percentage is inferred.",
        "Historical Spark Elemental failure remains in docs/SOURCE_SACRIFICE_DISCOVERY.md on engine 03869c08665bf476a8b40dacbfbcd501c54a33bd; current observations do not erase it."))

internal object SacrificePilot {
    private val reader = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path)).bufferedReader(Charsets.UTF_8).use { it.readText() }
    val requirementsText by lazy { resource("/retained-evidence/fidelity/source-sacrifice/requirements.json") }
    val requirements by lazy { reader.parseToJsonElement(requirementsText).jsonObject }
    val scopes by lazy { reader.decodeFromJsonElement<List<PilotScope>>(requirements.getValue("scopes")) }
    val requirementsHash get() = evidenceTextDigest(requirementsText)
    val harnessHash: String by lazy { Properties().apply { javaClass.getResourceAsStream("/fidelity-harness.properties")!!.use(::load) }.getProperty("sha256") }

    fun review(catalog: Catalog, scope: PilotScope): PilotReview {
        val card = catalog.card(scope.card)
        val source = resource("/retained-evidence/" + scope.sourceFile.removePrefix("src/test/resources/"))
        require(evidenceTextDigest(source) == scope.sourceSha256) { "Pilot source hash changed: ${scope.card}" }
        val oracle = reader.parseToJsonElement(source).jsonObject.getValue("card").jsonObject
        require(oracle.getValue("oracle_id").jsonPrimitive.content == scope.oracleId && oracle.getValue("id").jsonPrimitive.content == scope.printingId)
        if (card == null) return PilotReview(scope,null,null,listOf(PilotCheck("NATIVE_DEFINITION","UNRESOLVED","Native definition absent")))
        val e = PathwayExtractor(inspectCardEntries=true).extract(listOf(card))
        val facts = e.facts.filter { it.kind == FactKind.SACRIFICE && it.binding == "SELF" }
        val f = facts.singleOrNull()
        val action = e.actions.singleOrNull { it.id == f?.actionId }?.native as? JsonObject
        fun declaredType(value: JsonElement?, type: String): Boolean = when(value) {
            is JsonObject -> value["type"]?.jsonPrimitive?.contentOrNull == type || value.values.any { declaredType(it,type) }
            is JsonArray -> value.any { declaredType(it,type) }
            else -> false
        }
        val checks = mutableListOf<PilotCheck>()
        fun check(name: String, value: Boolean?, description: String) { checks += PilotCheck(name,if(value == null) "UNRESOLVED" else if(value) "SUPPORTED" else "CONTRADICTED",description) }
        check("PHYSICAL_SOURCE_IDENTITY",printingIdentity(card) == scope.printingId || card.oracleId == scope.oracleId,"Native printing/Oracle binding; source hash ${scope.sourceSha256}")
        check("EXACT_SOURCE",if(facts.size == 1) true else null,"${facts.size} source-bound sacrifice facts; no substitution by another copy")
        check("BATTLEFIELD_TO_OWNER_GRAVEYARD",f?.let { it.sourceZone == Zone.BATTLEFIELD && it.destinationZone == Zone.GRAVEYARD && it.destinationController == "OWNER" },"Nominal movement before replacements; source controller is ${f?.sourcePlayer}")
        check("COST_OR_RESOLUTION_ROLE",f?.let { it.costOrEffect == if(scope.role == "COST") "ACTIVATION_COST" else "EFFECT" },"Expected ${scope.role}; observed ${f?.costOrEffect}")
        check("ARRIVING_CARD_CHARACTERISTICS",f?.cardEntry?.sourceCreatureAtArrival?.let { it == if(scope.creatureAtArrival) Verdict.MATCH else Verdict.NO_MATCH },"Creature at arrival expected ${scope.creatureAtArrival}; departure animation is separate")
        check("TARGET_DECLARATION",action?.let { ((it["targetRequirements"] as? JsonArray).orEmpty().isNotEmpty() || it["targetRequirement"] != null) == scope.targetRequired },"Source-derived target requirement ${scope.targetRequired}; declaration only, not every legal target")
        check("TAP_REQUIREMENT",action?.let { declaredType(it["cost"],"Tap") == scope.tapRequired },"Source-derived tap requirement ${scope.tapRequired}")
        check("TRIGGER_TIMING",action?.let { if(scope.role == "COST") it["trigger"] == null else {
            val trigger=it["trigger"] as? JsonObject
            trigger?.get("type")?.jsonPrimitive?.contentOrNull == "StepEvent" && trigger["step"]?.jsonPrimitive?.contentOrNull?.uppercase() == "END"
        } },"Activation cost versus end-step trigger resolution")
        checks += PilotCheck("OBJECT_GENERATION_IN_EXECUTION","UNTESTED","Static fact presence cannot establish correct behavior after a zone round trip; inspect revision-bound execution observations")
        return PilotReview(scope,definitionDigest(card),f,checks)
    }

    fun report(catalog: Catalog, facts: List<MechanicalFact>): FidelityPilot {
        val reviews = scopes.map { review(catalog,it) }
        val receipt = javaClass.getResourceAsStream("/pilot-execution.json")?.bufferedReader(Charsets.UTF_8)?.use { reader.decodeFromString<PilotReceipt>(it.readText()) }
        val current = receipt != null && receipt.engine == EngineBuild.revision && receipt.requirementsSha256 == requirementsHash && receipt.harnessSha256 == harnessHash &&
            reviews.all { it.definitionSha256 != null && receipt.definitions[it.scope.card] == it.definitionSha256 }
        val counts = linkedMapOf("reviewed_ability_scopes" to reviews.size,"shared_requirement_definitions" to reviews.flatMap { it.checks }.map { it.requirement }.distinct().size,
            "reusable_native_check_applications" to reviews.sumOf { it.checks.count { c -> c.requirement != "OBJECT_GENERATION_IN_EXECUTION" } },
            "held_out_scopes" to reviews.count { it.scope.heldOut },"source_specific_expectation_records" to scopes.size,
            "card_specific_acceptance_rules" to 0)
        for(state in listOf("SUPPORTED","CONTRADICTED","UNRESOLVED","UNTESTED")) counts[state.lowercase()] = reviews.sumOf { it.checks.count { c -> c.state == state } }
        counts["current_execution_scenarios"] = if(current) receipt!!.scenarios.count { it.state != "NOT_STARTED" } else 0
        counts["current_execution_failures"] = if(current) receipt!!.scenarios.count { it.state == "DIFFERS" } else 0
        counts["current_execution_incomplete"] = if(current) receipt!!.scenarios.count { it.state in setOf("INCOMPLETE","NOT_STARTED") } else 0
        val sourceFacts = facts.filter { it.kind == FactKind.SACRIFICE && it.binding == "SELF" && it.cardEntry != null }
        return FidelityPilot(EngineBuild.revision,requirements,requirementsHash,reviews,counts,
            mapOf("structurally_applicable_physical_definitions" to sourceFacts.map { it.card }.distinct().size,"source_bound_fact_occurrences" to sourceFacts.size),
            if(receipt == null) "MISSING" else if(current) "RECORDED_CURRENT_BINDINGS" else "STALE",receipt)
    }

    /** Explicit bounded research execution; never called by a dashboard read or a build. */
    fun execute(catalog: Catalog, revision: String): PilotReceipt {
        val observations = scopes.map { scope ->
            val card = requireNotNull(catalog.card(scope.card))
            var game: LabExecution? = null
            try {
                game = LabExecution(card,Step.POSTCOMBAT_MAIN,emptyList(),ManaPoolComponent())
                val original = game.checkpoint()
                var pendingBeforeResolution: LabCheckpoint? = null
                if(scope.role == "COST") {
                    val ability=card.activatedAbilities.single()
                    game.perform("Activate source-bound ability",ActivateAbility(game.player,game.source,ability.id,
                        if(scope.targetRequired) listOf(ChosenTarget.Player(game.opponent)) else emptyList()))
                } else {
                    game.passPair("Reach end step")
                    pendingBeforeResolution=game.checkpoint()
                    game.passPair("Resolve end-step source sacrifice")
                }
                val after=game.checkpoint(); val move=game.sourceMoves.singleOrNull()
                val agrees = move != null && move.oldObject == original.sourceRef && move.newObject != move.oldObject &&
                    move.ownerId == game.player && move.lastKnown?.controllerId == game.player && move.wasSacrificed &&
                    move.fromZone == Zone.BATTLEFIELD && move.toZone == Zone.GRAVEYARD && move.requestedDestination == Zone.GRAVEYARD &&
                    after.creatureCardInGraveyard == scope.creatureAtArrival &&
                    (scope.role == "COST" || pendingBeforeResolution?.sourceRef == original.sourceRef && pendingBeforeResolution?.sourceZone == Zone.BATTLEFIELD)
                PilotExecution("source-sacrifice-${scope.printingId}",scope.card,"Exact source ${scope.role.lowercase()}; ordinary owned physical source, nominal destination and arriving type",
                    if(agrees) "AGREES" else "DIFFERS",buildJsonObject { put("initial",reportJson.encodeToJsonElement(original));put("beforeResolution",reportJson.encodeToJsonElement(pendingBeforeResolution));put("after",reportJson.encodeToJsonElement(after));put("actions",reportJson.encodeToJsonElement(game.actions));put("move",reportJson.encodeToJsonElement(move)) })
            } catch(e: Exception) { PilotExecution("source-sacrifice-${scope.printingId}",scope.card,"Ordinary source-bound sacrifice",if(game == null) "NOT_STARTED" else "INCOMPLETE",JsonNull,e.message) }
        } + LabScenarios.all.values.map { scenario ->
            val result=executeLab(scenario,revision)
            PilotExecution(scenario.info.id,scenario.info.card,scenario.info.mechanic,
                if(result.comparison == "AGREES") "AGREES" else if(result.comparison == "DIFFERS") "DIFFERS" else "INCOMPLETE",reportJson.encodeToJsonElement(result))
        }
        return PilotReceipt(revision,EngineBuild.revision,Instant.now().toString(),requirementsHash,harnessHash,
            scopes.associate { it.card to definitionDigest(requireNotNull(catalog.card(it.card))) },observations)
    }
}
