package dev.mtg.assistant

import dev.mtg.assistant.discovery.discoveryCommand
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import kotlinx.serialization.json.Json
import kotlin.system.exitProcess

private val scriptJson = Json { prettyPrint = true; encodeDefaults = false }

fun describe(card: CardDefinition): String = buildString {
    appendLine("Name: ${card.name}")
    appendLine("Set: ${card.setCode ?: "unknown"} #${card.metadata.collectorNumber ?: "unknown"}")
    appendLine("Mana cost: ${card.manaCost}; mana value: ${card.cmc}")
    appendLine("Type: ${card.typeLine}")
    appendLine("Colors: ${card.colors}; color identity: ${card.colorIdentity}")
    card.creatureStats?.let { appendLine("Power/toughness: ${it.power.description}/${it.toughness.description}") }
    appendLine("Keywords: ${card.keywords}")
    appendLine("Oracle text: ${card.oracleText}")
    appendLine("Native CardScript: ${card.script.javaClass.name}")
    appendLine("Has behavior: ${card.script.hasBehavior}")
    appendLine("Scripted abilities: ${card.script.allAbilities.size}")
    appendLine("Native CardScript JSON (non-default fields):")
    appendLine(scriptJson.encodeToString(CardScript.serializer(), card.script))
}

private const val HELP = """MTG Deckbuilding Assistant
Usage:
  mtg-assistant explorer <frontend-dist-directory> <port>
  mtg-assistant audit catalog-coverage --commander <name> --oracle-cards <snapshot> [--objective <objective|all>] [--translation-review <review.json>] [--enrichment-output <identities.json>] [--json] [--output <path>]
  mtg-assistant audit catalog-missing --report <coverage.json> [--name <substring>] [--status <STATUS>] [--limit 20] [--offset 0] [--json]
  mtg-assistant audit cross-objective --snapshot <objective-a.json> --other-snapshot <objective-b.json> [--json] [--output <path>]
  mtg-assistant audit benchmark --snapshot <discovery.json> --benchmark <review.json> [--json] [--output <path>]
  mtg-assistant audit benchmark-diff --before <benchmark.json> --after <benchmark.json> [--json] [--output <path>]
  mtg-assistant audit {coverage|inconsistencies} --commander <name> [--snapshot <discovery.json>] [--objective <objective>] [--benchmark <review.json>] [--json] [--output <path>]
  mtg-assistant audit diff --before <discovery.json> --after <discovery.json> [--before-coverage <coverage.json> --after-coverage <coverage.json>] [--json] [--output <path>]
  mtg-assistant commander inspect <exact card name> [--json]
  mtg-assistant stats
  mtg-assistant card <exact card name>
  mtg-assistant search <Argentum query>
  mtg-assistant discover --commander <exact name> [--search <Argentum query>]
      [--objective {own-nontoken-creature-deaths|reanimate-own-creatures|create-creature-tokens}] [--json] [--diagnostics] [--output <path>]
Examples:
  mtg-assistant card "Lightning Bolt"
  mtg-assistant search 't:instant c:r mv=1 name:bolt'
Search results cover the pinned authored Argentum catalog, not every Magic card.
"""

/** Injectable output streams let tests exercise the same command handling used by main. */
fun runCli(args: Array<String>, catalog: () -> Catalog = { Catalog() },
           out: (String) -> Unit = ::println, err: (String) -> Unit = System.err::println): Int {
    if (args.isEmpty() || args[0] in setOf("help", "--help", "-h")) {
        out(HELP)
        return 0
    }
    return try {
        when (args[0]) {
            "explorer" -> dev.mtg.assistant.explorer.serveExplorer(args.drop(1))
            "audit" -> dev.mtg.assistant.audit.auditCommand(args.drop(1), catalog, out)
            "commander" -> dev.mtg.assistant.discovery.commanderCommand(args.drop(1), catalog(), out)
            "discover" -> discoveryCommand(args.drop(1), catalog(), out)
            "stats" -> {
                require(args.size == 1) { "stats takes no arguments" }
                val data = catalog()
                out("Sets: ${data.setCount}; registered names: ${data.cards.size}")
                out("Engine: ${EngineBuild.revision}; mode: ${EngineBuild.mode}; implementation: ${EngineBuild.implementationSha256}")
            }
            "card" -> {
                require(args.size > 1) { "card requires an exact card name" }
                val name = args.drop(1).joinToString(" ")
                val data = catalog()
                val card = data.card(name)
                if (card == null) { err("Card not found: $name (lookup is case-sensitive)"); return 1 }
                out("Engine build: ${EngineBuild.revision}\nProvenance: ${data.provenance(name)}\n" + (data.translationAssurance(name)?.let { "Translation assurance: ${it.qualification}; fidelity=${it.sourceFidelity}; engine=${it.engineExecution}; ${it.reasons}\n" } ?: "") + describe(card))
            }
            "search" -> {
                require(args.size > 1) { "search requires a query; use an explicit empty string to match all" }
                val matches = catalog().search(args.drop(1).joinToString(" "))
                matches.forEach { out("${it.name}\t${it.manaCost}\t${it.typeLine}") }
                out("Matches: ${matches.size}")
            }
            else -> throw IllegalArgumentException("Unknown command: ${args[0]}; use --help")
        }
        0
    } catch (e: IllegalArgumentException) {
        err(e.message ?: "Invalid arguments")
        2
    }
}

fun main(args: Array<String>) { exitProcess(runCli(args)) }
