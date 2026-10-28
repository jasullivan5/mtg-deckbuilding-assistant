package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.references.Player

/** Batch 2 source: J25 #26; reviewed Oracle/rulings under reference/alignment-batch2. */
val SpinedTyrranax=card("Spined Tyrranax") {
    manaCost="{4}{G}";colorIdentity="G"
    typeLine="Creature — Dinosaur Beast"
    oracleText="At the beginning of combat on your turn, you may pay {2}{G}. When you do, put a +1/+1 counter on target creature. That creature gains trample until end of turn. (It can deal excess combat damage to the player or planeswalker it's attacking.)"
    power=5;toughness=5
    triggeredAbility {
        trigger=Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect=Effects.ReflexiveTrigger(action=Effects.PayMana("{2}{G}")) {
            val creature=target(TargetFilter.Creature)
            effect=Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE,1,creature) then Effects.GrantKeyword(Keyword.TRAMPLE,creature)
        }
    }
    metadata {
        rarity=Rarity.UNCOMMON;collectorNumber="26";artist="Brent Hollowell"
        imageUri="https://cards.scryfall.io/normal/front/b/b/bb589caa-bba4-4d23-bdfe-7fc37f19bfd6.jpg?1783908862"
        ruling("2024-11-08","You don't choose a target for Spiked Tyrranax's ability at the time it triggers. Rather, a second \"reflexive\" ability triggers when you pay {2}{G} this way. You choose a target for that ability as it goes on the stack. Each player may respond to this triggered ability as normal.")
    }
}.let { it.copy(setCode="J25",oracleId="2d09e689-c163-48c5-af5d-766f110d041e",legalFormats=setOf(DeckFormat.LEGACY,DeckFormat.VINTAGE,DeckFormat.COMMANDER,DeckFormat.BRAWL),
    metadata=it.metadata.copy(scryfallId="bb589caa-bba4-4d23-bdfe-7fc37f19bfd6",releaseDate="2024-11-15")) }
