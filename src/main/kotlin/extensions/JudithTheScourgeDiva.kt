package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** RNA #185. Native mechanics; source and raw Scryfall evidence are retained in src/test/resources/cards/. */
val JudithTheScourgeDiva = card("Judith, the Scourge Diva") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Human Shaman"
    power = 2
    toughness = 2
    oracleText = "Other creatures you control get +1/+0.\nWhenever a nontoken creature you control dies, Judith deals 1 damage to any target."
    staticAbility { ability = ModifyStats(1, 0, GroupFilter.OtherCreaturesYouControl) }
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().nontoken()).dies()
        val victim = target(AnyTarget())
        effect = Effects.DealDamage(1, victim) // Default source preserves source LKI across simultaneous deaths.
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "185"
        artist = "Wesley Burt"
        flavorText = "At the end of the show, she stands alone on a stage brilliant with blood."
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a742125-730d-4082-bfd8-5feb7733def4.jpg?1783933645"
        ruling("2019-01-25", "Judith's last ability triggers when Judith dies if it's not a token.")
        ruling("2019-01-25", "If Judith dies at the same time as one or more other nontoken creatures you control, Judith's ability triggers for each of them.")
        ruling("2019-01-25", "If your life total is brought to 0 or less at the same time that nontoken creatures you control are dealt lethal damage, you lose the game before Judith's triggered ability goes on the stack.")
    }
}.let { it.copy(metadata=it.metadata.copy(scryfallId="0a742125-730d-4082-bfd8-5feb7733def4", releaseDate="2019-01-25"), setCode = "RNA", oracleId = "c01516e0-eec3-4370-b935-7674982e8850",
    legalFormats = setOf(DeckFormat.COMMANDER, DeckFormat.PIONEER, DeckFormat.MODERN, DeckFormat.LEGACY, DeckFormat.VINTAGE, DeckFormat.BRAWL)) }
