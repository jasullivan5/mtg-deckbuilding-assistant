package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.references.Player

/** Source-led batch 1: VIS #113; archived Oracle and rulings in reference/alignment-batch1. */
val MortalWound=card("Mortal Wound") {
    manaCost="{G}"
    colorIdentity="G"
    typeLine="Enchantment — Aura"
    oracleText="Enchant creature\nWhen enchanted creature is dealt damage, destroy it."
    auraTarget = TargetObject(filter=TargetFilter.Creature)
    triggeredAbility {
        trigger=Triggers.attached.isDealtDamage()
        effect=Effects.Destroy(EffectTarget.EnchantedCreature)
    }
    metadata {
        rarity=Rarity.COMMON;collectorNumber="113";artist="Kev Walker"
        imageUri="https://cards.scryfall.io/normal/front/8/0/808830ff-496a-41dc-8b64-334ddaca9435.jpg?1783946982"
    }
}.let { it.copy(setCode="VIS",oracleId="f7b70077-5518-45f9-9833-01a4f8afac71",
    legalFormats=setOf(DeckFormat.LEGACY,DeckFormat.PAUPER,DeckFormat.VINTAGE,DeckFormat.COMMANDER),
    metadata=it.metadata.copy(scryfallId="808830ff-496a-41dc-8b64-334ddaca9435",releaseDate="1997-02-03")) }
