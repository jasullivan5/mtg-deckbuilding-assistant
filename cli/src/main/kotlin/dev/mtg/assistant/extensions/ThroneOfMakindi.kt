package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.references.Player

/** Batch 2 source: ZNR #265; reviewed Oracle/rulings under reference/alignment-batch2. */
val ThroneOfMakindi=card("Throne of Makindi") {
    manaCost="";colorIdentity=""
    typeLine="Land"
    oracleText="{T}: Add {C}.\n{1}, {T}: Put a charge counter on this land.\n{T}, Remove a charge counter from this land: Add two mana of any one color. Spend this mana only to cast kicked spells."
    activatedAbility { cost=Costs.Tap;effect=Effects.AddColorlessMana(1);manaAbility=true;timing=TimingRule.ManaAbility }
    activatedAbility { cost=Costs.Composite(Costs.Mana("{1}"),Costs.Tap);effect=Effects.AddCounters(CounterType.CHARGE,1,EffectTarget.Self) }
    activatedAbility {
        cost=Costs.Composite(Costs.Tap,Costs.RemoveCounterFromSelf(CounterType.CHARGE))
        effect=Effects.AddManaOfChoice(amount=2,restriction=ManaRestriction.KickedSpellsOnly)
        manaAbility=true;timing=TimingRule.ManaAbility
    }
    metadata {
        rarity=Rarity.RARE;collectorNumber="265";artist="Igor Kieryluk"
        imageUri="https://cards.scryfall.io/normal/front/d/5/d5a0563e-c83b-40df-abf6-51c83bf6792d.jpg?1783929305"
    }
}.let { it.copy(setCode="ZNR",oracleId="7e8198e9-0f3b-420b-ab09-74f13f4fd548",legalFormats=setOf(DeckFormat.PIONEER,DeckFormat.MODERN,DeckFormat.LEGACY,DeckFormat.VINTAGE,DeckFormat.COMMANDER,DeckFormat.BRAWL),
    metadata=it.metadata.copy(scryfallId="d5a0563e-c83b-40df-abf6-51c83bf6792d",releaseDate="2020-09-25")) }
