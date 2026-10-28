package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.references.Player

/** Batch 2 source: UDS #113; reviewed Oracle/rulings under reference/alignment-batch2. */
val Momentum=card("Momentum") {
    manaCost="{2}{G}";colorIdentity="G"
    typeLine="Enchantment — Aura"
    oracleText="Enchant creature\nAt the beginning of your upkeep, you may put a growth counter on this Aura.\nEnchanted creature gets +1/+1 for each growth counter on this Aura."
    auraTarget=TargetObject(filter=TargetFilter.Creature)
    triggeredAbility { trigger=Triggers.you.beginningOf(Step.UPKEEP);effect=Effects.May(Effects.AddCounters(CounterType.GROWTH,1,EffectTarget.Self)) }
    staticAbility { val growth=DynamicAmounts.countersOnSelf(CounterType.GROWTH)
        ability=GrantDynamicStats(filter=GroupFilter.attachedCreature(),powerBonus=growth,toughnessBonus=growth)
    }
    metadata {
        rarity=Rarity.UNCOMMON;collectorNumber="113";artist="Carl Critchlow"
        imageUri="https://cards.scryfall.io/normal/front/1/0/10bd11a2-7cab-4d3f-b52b-f5bb66fbbec6.jpg?1783946061"
        ruling("2004-10-04","Putting on a counter is optional. If you forget, you can't go back later even if it is something you usually do.")
    }
}.let { it.copy(setCode="UDS",oracleId="80875914-df4c-4b05-8c4d-658ff477c956",legalFormats=setOf(DeckFormat.LEGACY,DeckFormat.VINTAGE,DeckFormat.COMMANDER),
    metadata=it.metadata.copy(scryfallId="10bd11a2-7cab-4d3f-b52b-f5bb66fbbec6",releaseDate="1999-06-07")) }
