package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.references.Player

/** Source-led batch 1: BBD #21; archived Oracle and rulings in reference/alignment-batch1. */
val LeyWeaver=card("Ley Weaver") {
    manaCost="{3}{G}"
    colorIdentity="G"
    typeLine="Creature — Human Druid"
    oracleText="Partner with Lore Weaver (When this creature enters, target player may put Lore Weaver into their hand from their library, then shuffle.)\n{T}: Untap two target lands."
    power=2;toughness=2
    keywordAbilities(KeywordAbility.partnerWith("Lore Weaver"))
    // Partner's search meaning is explicit; the keyword only supplies the construction meaning.
    triggeredAbility {
        trigger=Triggers.self.enters()
        target(Targets.Player)
        effect=Effects.ForEachPlayer(Player.TargetPlayer,
            Effects.May(Patterns.Library.searchLibrary(filter=GameObjectFilter.Any.named("Lore Weaver"),reveal=true)))
    }
    activatedAbility {
        cost=Costs.Tap
        targets(TargetFilter.Land,count=2)
        effect=Effects.UntapEachTarget()
    }
    metadata {
        rarity=Rarity.UNCOMMON;collectorNumber="21";artist="Livia Prima"
        imageUri="https://cards.scryfall.io/normal/front/2/c/2c7e9d68-d419-4ec5-97e9-2478ecb7007f.jpg?1783934871"
        ruling("2018-06-08","“Partner with [name]” represents two abilities. The first is a triggered ability: “When this permanent enters the battlefield, target player may search their library for a card named [name], reveal it, put it into their hand, then shuffle their library.”")
        ruling("2018-06-08","Note that the target player searches their library (which may be affected by effects such as that of Stranglehold) and that the card they find is revealed, even though these words aren’t included in the ability’s reminder text.")
        ruling("2018-06-08","A nonlegendary creature can’t be your commander, even if it has a “partner with” ability.")
    }
}.let { it.copy(setCode="BBD",oracleId="24f1f0e9-8c9b-4f32-95ec-7af883bbeef4",
    legalFormats=setOf(DeckFormat.LEGACY,DeckFormat.VINTAGE,DeckFormat.COMMANDER),
    metadata=it.metadata.copy(scryfallId="2c7e9d68-d419-4ec5-97e9-2478ecb7007f",releaseDate="2018-06-08")) }
