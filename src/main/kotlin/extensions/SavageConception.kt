package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.references.Player

/** Source-reviewed card: EVE #75; reviewed Oracle/rulings under src/test/resources/cards. */
val SavageConception=card("Savage Conception") {
    manaCost="{3}{G}{G}";colorIdentity="G"
    typeLine="Sorcery"
    oracleText="Create a 3/3 green Beast creature token.\nRetrace (You may cast this card from your graveyard by discarding a land card in addition to paying its other costs.)"
    spell { effect=Effects.CreateToken(power=3,toughness=3,colors=setOf(Color.GREEN),creatureTypes=setOf("Beast"),imageUri="https://cards.scryfall.io/normal/front/3/f/3fc3a29a-280d-4f2c-9a01-8cfead75f583.jpg?1783942653") }
    staticAbility { ability=MayCastSelfFromZones(listOf(Zone.GRAVEYARD),additionalCost=Costs.additional.DiscardCards(1,GameObjectFilter.Land)) }
    metadata {
        rarity=Rarity.UNCOMMON;collectorNumber="75";artist="William O'Connor"
        imageUri="https://cards.scryfall.io/normal/front/b/8/b88ab58b-918b-478a-8c6b-a213e2bc87ab.jpg?1783942679"
        ruling("2008-08-01","Casting a card by using its retrace ability works just like casting any other spell, with two exceptions: You’re casting it from your graveyard rather than your hand, and you must discard a land card in addition to any other costs.")
        ruling("2008-08-01","A retrace card cast from your graveyard follows the normal timing rules for its card type.")
        ruling("2008-08-01","When a retrace card you cast from your graveyard resolves, fails to resolve, or is countered, it’s put back into your graveyard. You may use the retrace ability to cast it again.")
        ruling("2008-08-01","If the active player casts a spell that has retrace, that player may cast that card again after it resolves, before another player can remove the card from the graveyard. The active player has priority after the spell resolves, so they can immediately cast a new spell. Since casting a card with retrace from the graveyard moves that card onto the stack, no one else would have the chance to affect it while it’s still in the graveyard.")
    }
}.let { it.copy(setCode="EVE",oracleId="494050f4-0a55-415d-9ae9-feb17e61c4e1",legalFormats=setOf(DeckFormat.MODERN,DeckFormat.LEGACY,DeckFormat.VINTAGE,DeckFormat.COMMANDER),
    metadata=it.metadata.copy(scryfallId="b88ab58b-918b-478a-8c6b-a213e2bc87ab",releaseDate="2008-07-25")) }
