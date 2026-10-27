package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity

/** M13 #139; Oracle/rulings and the reviewed Assay proposal are archived under reference/phase27. */
val KrenkosCommand = card("Krenko's Command") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Create two 1/1 red Goblin creature tokens."
    spell {
        effect = Effects.CreateToken(power=1,toughness=1,colors=setOf(Color.RED),creatureTypes=setOf("Goblin"),count=2)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84df41e9-e973-4441-b17f-434517134d46.jpg"
    }
}.let { it.copy(setCode="M13",oracleId="9cfe86ae-eebe-44aa-a956-4b3e9e621105",
    metadata=it.metadata.copy(scryfallId="84df41e9-e973-4441-b17f-434517134d46",releaseDate="2012-07-13"),
    legalFormats=setOf(DeckFormat.MODERN,DeckFormat.LEGACY,DeckFormat.PAUPER,DeckFormat.VINTAGE,DeckFormat.COMMANDER,DeckFormat.BRAWL)) }
