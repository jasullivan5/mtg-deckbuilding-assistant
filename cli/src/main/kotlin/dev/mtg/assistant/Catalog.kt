package dev.mtg.assistant

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.mtg.sets.legality.LegalityData
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.search.SearchService

/** Integration boundary: native definitions stay available for later mechanical-query functions. */
class Catalog(extensionDefinitions: List<dev.mtg.assistant.extensions.ExtensionDefinition> = dev.mtg.assistant.extensions.Extensions.all,
    private val correctionPlan: () -> dev.mtg.assistant.eligibility.CorrectionPlan? = { dev.mtg.assistant.eligibility.ApprovedEligibility.plan }) {
    val eligibility by lazy { dev.mtg.assistant.eligibility.EligibilityResolver(dev.mtg.assistant.discovery.physicalCards(cards),correctionPlan()) }
    private val origins = mutableMapOf<String, dev.mtg.assistant.extensions.Provenance>()
    fun provenance(name: String) = origins[name] ?: dev.mtg.assistant.extensions.Provenance("UPSTREAM_ARGENTUM", "https://github.com/wingedsheep/argentum-engine/tree/" + dev.mtg.assistant.discovery.ARGENTUM_SHA)
    private val reviews = mutableMapOf<String, dev.mtg.assistant.extensions.DefinitionReview>()
    fun definitionReview(name: String) = reviews[name] ?: dev.mtg.assistant.extensions.DefinitionReview()
    fun translationAssurance(name: String): dev.mtg.assistant.extensions.TranslationAssurance? =
        card(name)?.takeIf { provenance(name).origin=="APPLICATION_EXTENSION" }?.let {
            dev.mtg.assistant.extensions.assessTranslation(it,definitionReview(name).assuranceEvidence)
        }
    val registry = CardRegistry()
    private val printings = PrintingRegistry()
    val validationWarnings: List<String>
    val setCount: Int
    val cards: List<CardDefinition>
    private val searchable: List<DefinitionSearchCard> by lazy { cards.map {
        DefinitionSearchCard(it, printings.printingsOf(it.name),eligibility.resolve(it).takeIf { m -> m.corrections.isNotEmpty() })
    } }

    init {
        val sets = MtgSetCatalog.all
        check(sets.isNotEmpty()) { "Argentum discovered no sets on the runtime classpath" }
        setCount = sets.size
        for (set in sets) {
            registry.register((set.cards + set.basicLands).map { card ->
                val stamped = if (card.setCode == null) card.copy(setCode = set.code) else card
                val dated = if (stamped.metadata.releaseDate == null && set.releaseDate != null)
                    stamped.copy(metadata = stamped.metadata.copy(releaseDate = set.releaseDate)) else stamped
                LegalityData.stamp(dated)
            })
        }
        validationWarnings = dev.mtg.assistant.extensions.Extensions.validate(registry.allCardNames().map(registry::requireCard), extensionDefinitions)
        for (extension in extensionDefinitions) {
            registry.register(listOf(extension.definition))
            fun mark(def: CardDefinition) {
                origins[def.name] = extension.provenance
                reviews[def.name] = extension.review
                def.backFace?.let(::mark); def.flipSide?.let(::mark)
            }
            mark(extension.definition)
        }
        cards = registry.allCardNames().sorted().map(registry::requireCard)
        for (name in registry.allCardNames()) {
            registry.getCardsByName(name).forEach(printings::registerSynthesizedDefault)
        }
        sets.forEach { printings.register(it.printings) }
    }

    /** Preserve upstream's exact, case-sensitive lookup, including its variant and face support. */
    fun card(name: String): CardDefinition? = registry.getCard(name)

    fun search(query: String): List<CardDefinition> {
        val compiled = SearchService.compile(query)
        require(compiled.errors.isEmpty()) { "Invalid search: ${compiled.errors.joinToString()}" }
        return searchable.filter(compiled::matches).map { it.definition }
    }
}
