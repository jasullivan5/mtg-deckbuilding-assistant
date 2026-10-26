package dev.mtg.assistant

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.search.SearchCard
import com.wingedsheep.search.SearchView

/** Data projection only; parsing and matching belong entirely to Argentum mtg-search. */
internal class DefinitionSearchCard(
    val definition: CardDefinition,
    printings: List<Printing>,
) : SearchCard {
    override val name = definition.name
    override val manaCost = definition.manaCost.toString()
    override val cmc = definition.cmc
    override val colors = definition.colors.map { it.name }
    override val colorIdentity = definition.colorIdentity.map { it.name }
    override val cardTypes = definition.typeLine.cardTypes.map { it.name }
    override val supertypes = definition.typeLine.supertypes.map { it.name }
    override val subtypes = definition.typeLine.subtypes.map { it.toString() }
    override val basicLand = definition.typeLine.isBasicLand
    override val rarity = definition.metadata.rarity.name
    override val setCode = definition.setCode
    override val collectorNumber = definition.metadata.collectorNumber
    override val oracleText = definition.oracleText
    override val power = definition.creatureStats?.power?.description
    override val toughness = definition.creatureStats?.toughness?.description
    override val loyalty = definition.startingLoyalty?.toString()
    override val keywords = definition.keywords.map { it.name }.sorted()
    override val legalFormats = definition.legalFormats.map { it.name }.sorted()
    override val isDoubleFaced = definition.isDoubleFaced
    override val printingSetCodes = printings.map { it.setCode }.distinct().sorted()
    override val printedNames = printings.mapNotNull { it.printedName }.distinct().sorted()
    override val view = SearchView(this)
}
