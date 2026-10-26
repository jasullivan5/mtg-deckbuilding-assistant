package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity

/**
 * KLD #165, verified 2026-10-26 against reference/oviya-*-scryfall.json.
 * Source: https://scryfall.com/card/kld/165/oviya-pashiri-sage-lifecrafter
 * Dynamic token base stats are evaluated once at resolution, before the new token exists;
 * these are not characteristic-defining abilities that track the creature count later.
 */
val OviyaPashiriSageLifecrafter = card("Oviya Pashiri, Sage Lifecrafter") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Artificer"
    power = 1
    toughness = 2
    oracleText = "{2}{G}, {T}: Create a 1/1 colorless Servo artifact creature token.\n" +
        "{4}{G}, {T}: Create an X/X colorless Construct artifact creature token, where X is the number of creatures you control."
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}"), Costs.Tap)
        effect = Effects.CreateToken(1,1,creatureTypes=setOf("Servo"),artifactToken=true)
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{G}"), Costs.Tap)
        effect = Effects.CreateToken(0,0,creatureTypes=setOf("Construct"),artifactToken=true,
            dynamicPower=DynamicAmounts.creaturesYouControl(),dynamicToughness=DynamicAmounts.creaturesYouControl())
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27eb9d40-958b-41b8-b04b-7f45adc0a862.jpg"
        ruling("2016-09-20", "The value of X is determined only as Oviya Pashiri's last ability resolves. The token's power and toughness won't change as the number of creatures you control changes.")
        ruling("2016-09-20", "The Construct token you're creating doesn't count towards X for the ability that creates it.")
    }
}.let { it.copy(setCode="KLD",oracleId="41e03224-bcdd-4127-a19a-fe66196339b9",
    metadata=it.metadata.copy(scryfallId="27eb9d40-958b-41b8-b04b-7f45adc0a862",releaseDate="2016-09-30"),
    legalFormats=setOf(DeckFormat.PIONEER,DeckFormat.MODERN,DeckFormat.LEGACY,DeckFormat.VINTAGE,DeckFormat.COMMANDER,DeckFormat.BRAWL)) }
