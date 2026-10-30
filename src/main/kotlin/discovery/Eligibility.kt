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
fun identityRestrictions(card: CardDefinition): Set<Color> =
    card.typeLine.subtypes.mapNotNull { intrinsicLandColors[it] }.toSet() +
    (card.backFace?.let(::fullIdentity) ?: emptySet()) +
    (card.flipSide?.let(::fullIdentity) ?: emptySet()) + card.cardFaces.flatMap { it.manaCost.colors }
fun fullIdentity(card: CardDefinition): Set<Color> = card.colorIdentity + identityRestrictions(card)
fun selectedCommanderEligibility(card: CardDefinition, allowed: Set<Color>,
    metadata: dev.mtg.assistant.eligibility.EffectiveEligibility = dev.mtg.assistant.eligibility.nativeEligibility(card)): Verdict =
    if (!metadata.authoritativeIdentity) Verdict.UNKNOWN
    else if (allowed.containsAll(metadata.colors + identityRestrictions(card))) Verdict.MATCH else Verdict.NO_MATCH

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
