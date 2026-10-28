package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.references.Player

/** Batch 2: ZEN #175; full source and rulings in reference/alignment-batch2. */
val PredatoryUrge=card("Predatory Urge") {
    manaCost="{3}{G}";colorIdentity="G"
    typeLine="Enchantment — Aura"
    oracleText="Enchant creature\nEnchanted creature has \"{T}: This creature deals damage equal to its power to target creature. That creature deals damage equal to its power to this creature.\""
    auraTarget=TargetObject(filter=TargetFilter.Creature)
    staticAbility { ability=GrantActivatedAbility(ability=grantedActivatedAbility {
        cost=Costs.Tap
        val prey=target(TargetFilter.Creature)
        effect=Effects.DealDamage(DynamicAmounts.sourcePower(),prey) then
            Effects.DealDamage(DynamicAmounts.powerOf(prey),EffectTarget.Self,damageSource=prey)
    }) }
    metadata {
        rarity=Rarity.RARE;collectorNumber="175";artist="Scott Chou"
        imageUri="https://cards.scryfall.io/normal/front/d/7/d7f8adaa-2852-4e9c-b91d-ce80a21859c9.jpg?1783942133"
        ruling("2009-10-01","The controller of the enchanted creature may activate the ability, not the controller of Predatory Urge.")
        ruling("2009-10-01","If the enchanted creature’s ability is activated, that creature is the one that will deal and be dealt damage when the ability resolves. It doesn’t matter if Predatory Urge leaves the battlefield or somehow becomes attached to another creature by that time.")
        ruling("2009-10-01","If the targeted creature leaves the battlefield (or otherwise becomes an illegal target) before the ability resolves, the ability doesn’t resolve. The enchanted creature isn’t dealt damage.")
        ruling("2009-10-01","On the other hand, if the enchanted creature leaves the battlefield before the ability resolves, the ability continues to resolve. The enchanted creature deals damage to the targeted creature equal to the power the enchanted creature had as it last existed on the battlefield.")
        ruling("2009-10-01","You may have the enchanted creature target itself with its own ability. If you do, it will deal damage to itself equal to its power, then immediately do it again.")
    }
}.let {it.copy(setCode="ZEN",oracleId="f6175cca-0e19-4db8-a175-3369fb650f24",legalFormats=setOf(DeckFormat.MODERN,DeckFormat.LEGACY,DeckFormat.VINTAGE,DeckFormat.COMMANDER),
    metadata=it.metadata.copy(scryfallId="d7f8adaa-2852-4e9c-b91d-ce80a21859c9",releaseDate="2009-10-02"))}
