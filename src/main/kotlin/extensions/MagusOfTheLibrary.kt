package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.references.Player

/** Source-reviewed card: PLC #134; reviewed Oracle/rulings under src/test/resources/cards. */
val MagusOfTheLibrary=card("Magus of the Library") {
    manaCost="{G}{G}";colorIdentity="G"
    typeLine="Creature — Human Wizard"
    oracleText="{T}: Add {C}.\n{T}: Draw a card. Activate only if you have exactly seven cards in hand."
    power=1;toughness=1
    activatedAbility { cost=Costs.Tap;effect=Effects.AddColorlessMana(1);manaAbility=true;timing=TimingRule.ManaAbility }
    activatedAbility {
        cost=Costs.Tap;effect=Effects.DrawCards(1)
        restrictions=listOf(ActivationRestriction.OnlyIfCondition(Conditions.All(Conditions.CardsInHandAtLeast(7),Conditions.CardsInHandAtMost(7))))
    }
    metadata {
        rarity=Rarity.RARE;collectorNumber="134";artist="Wayne Reynolds"
        imageUri="https://cards.scryfall.io/normal/front/8/9/899db99b-df27-4848-a23d-5c645deae450.jpg?1783943140"
        ruling("2004-10-04","You may tap multiples of these in response to each other because the requirement for 7 cards is checked only at the time the ability is announced and not again when it resolves.")
    }
}.let { it.copy(setCode="PLC",oracleId="69266cb7-50eb-43d2-96d6-fc3c85be49f7",legalFormats=setOf(DeckFormat.MODERN,DeckFormat.LEGACY,DeckFormat.VINTAGE,DeckFormat.COMMANDER,DeckFormat.BRAWL),
    metadata=it.metadata.copy(scryfallId="899db99b-df27-4848-a23d-5c645deae450",releaseDate="2007-02-02")) }
