package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.references.Player

/** Batch 2 source: HOU #120; reviewed Oracle/rulings under reference/alignment-batch2. */
val HourOfPromise=card("Hour of Promise") {
    manaCost="{4}{G}";colorIdentity="G"
    typeLine="Sorcery"
    oracleText="Search your library for up to two land cards, put them onto the battlefield tapped, then shuffle. Then if you control three or more Deserts, create two 2/2 black Zombie creature tokens."
    spell {
        effect=Patterns.Library.searchLibrary(filter=GameObjectFilter.Land,count=2,destination=SearchDestination.BATTLEFIELD,entersTapped=true) then
            Effects.If(Conditions.CompareAmounts(DynamicAmounts.battlefield(Player.You,GameObjectFilter.Land.withSubtype("Desert")).count(),ComparisonOperator.GTE,3),
                Effects.CreateToken(power=2,toughness=2,colors=setOf(Color.BLACK),creatureTypes=setOf("Zombie"),count=2,imageUri="https://cards.scryfall.io/normal/front/5/e/5e16386a-d478-475b-8e9b-edef62cae02b.jpg?1791637181"))
    }
    metadata {
        rarity=Rarity.RARE;collectorNumber="120";artist="Jonas De Ro"
        imageUri="https://cards.scryfall.io/normal/front/c/5/c5ee374f-31dc-4b2c-beba-431d5b1126d0.jpg?1783936018"
        ruling("2017-07-14","When Hour of Promise checks the number of Deserts you control, it includes any Deserts you found earlier with Hour of Promise.")
    }
}.let { it.copy(setCode="HOU",oracleId="51ff3e53-90bb-41bf-80e4-bb3fd51d574a",legalFormats=setOf(DeckFormat.PIONEER,DeckFormat.MODERN,DeckFormat.LEGACY,DeckFormat.VINTAGE,DeckFormat.COMMANDER,DeckFormat.BRAWL),
    metadata=it.metadata.copy(scryfallId="c5ee374f-31dc-4b2c-beba-431d5b1126d0",releaseDate="2017-07-14")) }
