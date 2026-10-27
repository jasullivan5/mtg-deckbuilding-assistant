package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** MOR #58. Life loss is an unconditional effect, never an activation/payment prerequisite. */
val Bitterblossom = card("Bitterblossom") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Kindred Enchantment — Faerie"
    oracleText = "At the beginning of your upkeep, you lose 1 life and create a 1/1 black Faerie Rogue creature token with flying."
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.LoseLife(1,EffectTarget.Controller) then Effects.CreateToken(
            power=1,toughness=1,colors=setOf(Color.BLACK),creatureTypes=setOf("Faerie","Rogue"),keywords=setOf(Keyword.FLYING))
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Rebecca Guay"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/8145fed6-6b51-420a-84cf-4ea5e0aa1883.jpg"
        ruling("2024-06-07", "This cards was originally printed with the \"tribal\" card type. That card type has been replaced with \"kindred\". This change does not affect the gameplay function of this card.")
        ruling("2024-06-07", "Kindred is a card type that allows noncreature cards to have creature types. For example, Echoes of Eternity is an Eldrazi (although not a creature) while on the battlefield and an Eldrazi card (although not a creature card) in zones other than the battlefield.")
        ruling("2024-06-07", "While it appears only on cards that already have other card types, kindred is a card type and will be counted by effects that refer to the number of card types among cards in a zone.")
        ruling("2008-04-01", "The effect is mandatory. You'll lose 1 life even if you have only 1 life left.")
        ruling("2008-04-01", "The life loss isn't a payment. You'll get a token even if you had 0 life (and another effect is stopping you from losing the game).")
    }
}.let { it.copy(setCode="MOR",oracleId="fb868840-09fa-49b1-85cb-b08ad065e972",
    metadata=it.metadata.copy(scryfallId="8145fed6-6b51-420a-84cf-4ea5e0aa1883",releaseDate="2008-02-01"),
    legalFormats=setOf(DeckFormat.MODERN,DeckFormat.LEGACY,DeckFormat.VINTAGE,DeckFormat.COMMANDER,DeckFormat.BRAWL)) }
