package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.references.Player

/** Source-reviewed card: SHM #264; archived Oracle and rulings in src/test/resources/cards. */
val Tatterkite=card("Tatterkite") {
    manaCost="{3}"
    colorIdentity=""
    typeLine="Artifact Creature — Scarecrow"
    oracleText="Flying\nThis creature can't have counters put on it."
    power=2;toughness=1
    keywords(Keyword.FLYING)
    staticAbility { ability=CantReceiveCounters(GroupFilter.source()) }
    metadata {
        rarity=Rarity.UNCOMMON;collectorNumber="264";artist="Ron Brown"
        imageUri="https://cards.scryfall.io/normal/front/d/f/dfaf852c-b8a9-44d4-b2cd-3f1e071a8be9.jpg?1783942708"
        ruling("2008-05-01","Tatterkite's ability prevents any kind of counter from being placed on it, not just -1/-1 counters.")
        ruling("2008-05-01","A spell or ability whose effect would put a counter on Tatterkite simply doesn't affect it. The spell or ability isn't countered. If it would put counters on any other creatures, it continues to do so.")
        ruling("2008-05-01","If Tatterkite is dealt damage by a creature with wither, that damage has no effect. It's not prevented; it just doesn't do anything. Abilities that trigger on damage being dealt will still trigger.")
        ruling("2008-05-01","You can't pay a cost that includes putting a counter on Tatterkite. For example, if Tatterkite is the only creature you control, you can't cast Scarscale Ritual because you can't pay the additional cost.")
        ruling("2008-05-01","If Tatterkite would enter with counters on it, it enters but doesn't have those counters. For example, if an effect grants it persist and then it's destroyed, it returns to the battlefield without a -1/-1 counter on it.")
    }
}.let { it.copy(setCode="SHM",oracleId="9505f124-f152-48ce-b0d3-097abdda8e3e",
    legalFormats=setOf(DeckFormat.MODERN,DeckFormat.LEGACY,DeckFormat.VINTAGE,DeckFormat.COMMANDER),
    metadata=it.metadata.copy(scryfallId="dfaf852c-b8a9-44d4-b2cd-3f1e071a8be9",releaseDate="2008-05-02")) }
