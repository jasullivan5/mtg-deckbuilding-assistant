package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.references.Player

/** Batch 2 source: 5DN #142; reviewed Oracle/rulings under reference/alignment-batch2. */
val ParadiseMantle=card("Paradise Mantle") {
    manaCost="{0}";colorIdentity=""
    typeLine="Artifact — Equipment"
    oracleText="Equipped creature has \"{T}: Add one mana of any color.\"\nEquip {1}"
    staticAbility { ability=GrantActivatedAbility(ability=grantedActivatedAbility {
        cost=Costs.Tap;effect=Effects.AddManaOfChoice();manaAbility=true;timing=TimingRule.ManaAbility
    }) }
    equipAbility("{1}")
    metadata {
        rarity=Rarity.UNCOMMON;collectorNumber="142";artist="Greg Hildebrandt"
        imageUri="https://cards.scryfall.io/normal/front/1/2/1252e9e2-2dd5-4bd6-aa56-f0a0ba056a77.jpg?1783944376"
    }
}.let { it.copy(setCode="5DN",oracleId="c1121b83-1ba2-473d-89c9-e3bbd4529072",legalFormats=setOf(DeckFormat.MODERN,DeckFormat.LEGACY,DeckFormat.VINTAGE,DeckFormat.COMMANDER),
    metadata=it.metadata.copy(scryfallId="1252e9e2-2dd5-4bd6-aa56-f0a0ba056a77",releaseDate="2004-06-04")) }
