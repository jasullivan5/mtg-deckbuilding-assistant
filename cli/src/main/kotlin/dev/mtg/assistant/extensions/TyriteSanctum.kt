package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.references.Player

/** Source-led batch 1: KHM #272; archived Oracle and rulings in reference/alignment-batch1. */
val TyriteSanctum=card("Tyrite Sanctum") {
    manaCost=""
    colorIdentity=""
    typeLine="Land"
    oracleText="{T}: Add {C}.\n{2}, {T}: Target legendary creature becomes a God in addition to its other types. Put a +1/+1 counter on it.\n{4}, {T}, Sacrifice this land: Put an indestructible counter on target God."
    activatedAbility {
        cost=Costs.Tap;effect=Effects.AddColorlessMana(1)
        manaAbility=true;timing=TimingRule.ManaAbility
    }
    activatedAbility {
        cost=Costs.Composite(Costs.Mana("{2}"),Costs.Tap)
        val creature=target(TargetFilter.Creature.legendary())
        effect=Effects.AddSubtype("God",creature,Duration.Permanent) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE,1,creature)
    }
    activatedAbility {
        cost=Costs.Composite(Costs.Mana("{4}"),Costs.Tap,Costs.SacrificeSelf)
        val god=target(TargetFilter.Permanent.withSubtype("God"))
        effect=Effects.AddCounters(CounterType.INDESTRUCTIBLE,1,god)
    }
    metadata {
        rarity=Rarity.RARE;collectorNumber="272";artist="Volkan Baǵa"
        imageUri="https://cards.scryfall.io/normal/front/3/0/309ff3bf-acd3-4bea-b518-6e25f6cfce61.jpg?1783928170"
        ruling("2021-02-05","You can activate the middle ability targeting a legendary creature that's already a God. You'll put a +1/+1 counter on it.")
        ruling("2021-02-05","An indestructible counter on a permanent gives that permanent indestructible.")
    }
}.let { it.copy(setCode="KHM",oracleId="38c83332-2d21-450c-ae97-34109b565f59",
    legalFormats=setOf(DeckFormat.PIONEER,DeckFormat.MODERN,DeckFormat.LEGACY,DeckFormat.VINTAGE,DeckFormat.COMMANDER,DeckFormat.BRAWL),
    metadata=it.metadata.copy(scryfallId="309ff3bf-acd3-4bea-b518-6e25f6cfce61",releaseDate="2021-02-05")) }
