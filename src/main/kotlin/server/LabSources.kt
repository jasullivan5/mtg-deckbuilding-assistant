package dev.mtg.assistant.explorer

import com.wingedsheep.sdk.model.CardDefinition
import dev.mtg.assistant.discovery.reportJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable internal data class LabPrinting(
    val card: JsonObject, val snapshotSha256: String, val rulingsSha256: String, val rulings: List<JsonObject>
) {
    val sourceUrl: String get() = card.getValue("scryfall_uri").jsonPrimitive.content.substringBefore('?')
    val name: String get() = card.getValue("name").jsonPrimitive.content
    val printingId: String get() = card.getValue("id").jsonPrimitive.content
    val oracleId: String get() = card.getValue("oracle_id").jsonPrimitive.content
}

@Serializable internal data class LabSources(
    val reviewedOn: String, val rulesUrl: String, val rulesSha256: String, val cards: List<LabPrinting>
) {
    companion object {
        val retained: LabSources by lazy {
            val bytes = requireNotNull(LabSources::class.java.getResourceAsStream("/lab-sources.json"))
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
            reportJson.decodeFromString<LabSources>(bytes)
        }
    }
}

@Serializable internal data class LabListing(
    val scenarios: List<LabScenarioInfo>, val images: Map<String, CardImage>, val sources: LabSources
)

internal fun labListing(): LabListing {
    val sources = LabSources.retained
    val scenarios = LabScenarios.all.values
    val images = scenarios.distinctBy { it.card.name }.mapIndexed { index, scenario ->
        val source = sources.cards.single { it.sourceUrl == scenario.info.cardSource }
        scenario.card.name to verifiedImage(index, scenario.card, source.card, sources.reviewedOn)
    }.toMap()
    return LabListing(scenarios.map { it.info }, images, sources)
}

internal fun labScenariosFor(card: CardDefinition?): List<String> {
    if (card == null) return emptyList()
    return LabScenarios.all.values.filter { scenario ->
        if (card.name != scenario.card.name) return@filter false
        val printing = LabSources.retained.cards.single { it.sourceUrl == scenario.info.cardSource }
        card === scenario.card || card.oracleId == printing.oracleId || printingIdentity(card) == printing.printingId
    }.map { it.info.id }
}
