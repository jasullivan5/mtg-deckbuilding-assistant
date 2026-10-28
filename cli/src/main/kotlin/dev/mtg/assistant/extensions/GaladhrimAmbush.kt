package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.references.Player

/** Batch 2 source: LTC #38; reviewed Oracle/rulings under reference/alignment-batch2. */
val GaladhrimAmbush=card("Galadhrim Ambush") {
    manaCost="{3}{G}";colorIdentity="G"
    typeLine="Instant"
    oracleText="Create X 1/1 green Elf Warrior creature tokens, where X is the number of attacking creatures.\nPrevent all combat damage that would be dealt this turn by non-Elf creatures."
    spell {
        effect=Effects.CreateToken(power=1,toughness=1,colors=setOf(Color.GREEN),creatureTypes=setOf("Elf","Warrior"),
            count=DynamicAmounts.battlefield(Player.Each,GameObjectFilter.Creature.attacking()).count(),imageUri="https://cards.scryfall.io/normal/front/0/2/023f668a-a8e6-4fcb-98d1-30adf343f050.jpg?1783916056") then
            Effects.PreventCombatDamageFrom(GameObjectFilter.Creature.notSubtype(Subtype("Elf")))
    }
    metadata {
        rarity=Rarity.RARE;collectorNumber="38";artist="John Di Giovanni"
        imageUri="https://cards.scryfall.io/normal/front/e/6/e69e06f0-6142-4f80-9208-362b3e74d51d.jpg?1783916026"
        ruling("2023-06-16","Galadhrim Ambush counts all attacking creatures no matter which player, planeswalker, or battle they're attacking, including attacking creatures you control if Galadhrim Ambush is cast during your combat phase.")
    }
}.let { it.copy(setCode="LTC",oracleId="3b22d21f-e19a-40df-840b-a2b7f11f79c2",legalFormats=setOf(DeckFormat.LEGACY,DeckFormat.VINTAGE,DeckFormat.COMMANDER),
    metadata=it.metadata.copy(scryfallId="e69e06f0-6142-4f80-9208-362b3e74d51d",releaseDate="2023-06-23")) }
