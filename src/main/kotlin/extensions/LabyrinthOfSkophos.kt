package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.references.Player

/** Source-reviewed card: THB #243; reviewed Oracle/rulings under src/test/resources/cards. */
val LabyrinthOfSkophos=card("Labyrinth of Skophos") {
    manaCost="";colorIdentity=""
    typeLine="Land"
    oracleText="{T}: Add {C}.\n{4}, {T}: Remove target attacking or blocking creature from combat."
    activatedAbility { cost=Costs.Tap;effect=Effects.AddColorlessMana(1);manaAbility=true;timing=TimingRule.ManaAbility }
    activatedAbility {
        cost=Costs.Composite(Costs.Mana("{4}"),Costs.Tap)
        val creature=target(TargetFilter(GameObjectFilter.Creature.attackingOrBlocking()))
        effect=Effects.RemoveFromCombat(creature)
    }
    metadata {
        rarity=Rarity.RARE;collectorNumber="243";artist="Jonas De Ro"
        imageUri="https://cards.scryfall.io/normal/front/f/f/ff0dd221-2660-43f6-8e28-8000e3fe72a4.jpg?1783931509"
        ruling("2020-01-24","A creature removed from combat by Labyrinth of Skophos remains tapped if it was tapped.")
        ruling("2020-01-24","If a blocking creature is removed from combat, the creature it blocked remains blocked. Unless that creature has trample, it won't deal combat damage.")
        ruling("2020-01-24","Once creatures attack or block, they remain attacking or blocking creatures through the end of combat step unless removed from combat (by Labyrinth of Skophos or a similar effect).")
    }
}.let { it.copy(setCode="THB",oracleId="9ec5a487-d8ed-459a-8f58-56f6e9a2dfe8",legalFormats=setOf(DeckFormat.PIONEER,DeckFormat.MODERN,DeckFormat.LEGACY,DeckFormat.VINTAGE,DeckFormat.COMMANDER,DeckFormat.BRAWL),
    metadata=it.metadata.copy(scryfallId="ff0dd221-2660-43f6-8e28-8000e3fe72a4",releaseDate="2020-01-24")) }
