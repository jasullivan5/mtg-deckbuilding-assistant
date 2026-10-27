package dev.mtg.assistant.discovery

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.model.CardDefinition
import dev.mtg.assistant.Catalog

/** A known legal-format entry is required. Unknown is never treated as permission. */
fun commanderFormatLegality(card: CardDefinition): Verdict = when {
    DeckFormat.COMMANDER in card.legalFormats -> Verdict.MATCH
    card.legalFormats.isEmpty() -> Verdict.UNKNOWN
    else -> Verdict.NO_MATCH
}
private val intrinsicLandColors = mapOf(Subtype.PLAINS to Color.WHITE, Subtype.ISLAND to Color.BLUE,
    Subtype.SWAMP to Color.BLACK, Subtype.MOUNTAIN to Color.RED, Subtype.FOREST to Color.GREEN)
fun fullIdentity(card: CardDefinition): Set<Color> = card.colorIdentity +
    card.typeLine.subtypes.mapNotNull { intrinsicLandColors[it] } +
    (card.backFace?.let(::fullIdentity) ?: emptySet()) +
    (card.flipSide?.let(::fullIdentity) ?: emptySet()) + card.cardFaces.flatMap { it.manaCost.colors }
fun selectedCommanderEligibility(card: CardDefinition, allowed: Set<Color>): Verdict =
    if (card.colorIdentityOverride == null) Verdict.UNKNOWN
    else if (allowed.containsAll(fullIdentity(card))) Verdict.MATCH else Verdict.NO_MATCH

/** Physical cards, excluding registry aliases for backs/flips and stand-alone meld results. */
fun physicalCards(cards: List<CardDefinition>): List<CardDefinition> {
    val aliases = cards.flatMap { listOfNotNull(it.backFace?.name, it.flipSide?.name) }.toSet()
    return cards.filter { it.name !in aliases && !it.meldResult }.distinctBy { it.name }
}
fun resolveCommander(catalog: Catalog, name: String): CardDefinition {
    val inspection=inspectCommander(catalog,name)
    require(inspection.selectable) { inspection.blockers.joinToString("; ") { "${it.code}: ${it.reason}. ${it.nextAction}" } }
    return requireNotNull(catalog.card(name))
}
