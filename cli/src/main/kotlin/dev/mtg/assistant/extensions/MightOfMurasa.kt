package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.references.Player

/** Batch 2 source: ZNR #194; reviewed Oracle/rulings under reference/alignment-batch2. */
val MightOfMurasa=card("Might of Murasa") {
    manaCost="{1}{G}";colorIdentity="G"
    typeLine="Instant"
    oracleText="Kicker {2}{G} (You may pay an additional {2}{G} as you cast this spell.)\nTarget creature gets +3/+3 until end of turn. If this spell was kicked, that creature gets +5/+5 until end of turn instead."
    keywordAbility(KeywordAbility.kicker("{2}{G}"))
    spell { val creature=target(TargetFilter.Creature)
        effect=Effects.If(Conditions.WasKicked,Effects.ModifyStats(5,5,creature),Effects.ModifyStats(3,3,creature))
    }
    metadata {
        rarity=Rarity.COMMON;collectorNumber="194";artist="Lie Setiawan"
        imageUri="https://cards.scryfall.io/normal/front/4/2/42914fd9-e56e-4c3e-a445-fdfa45a45178.jpg?1783929336"
        ruling("2024-11-08","If a spell's kicker cost was paid, the spell is \"kicked.\"")
        ruling("2024-11-08","The kicker ability doesn't let you pay a kicker cost more than once.")
        ruling("2024-11-08","If you put a permanent with a kicker ability onto the battlefield without casting it, you can't kick it.")
        ruling("2024-11-08","If you copy a kicked spell on the stack, the copy is also kicked. If the copied spell is a permanent spell, the token the copy of that spell becomes when it enters is also kicked.")
        ruling("2024-11-08","If a card or token enters as a copy of a permanent, the new permanent isn't kicked, even if the original was.")
        ruling("2024-11-08","To determine a spell's total cost, start with the mana cost (or an alternative cost if another card's effect allows you to pay one instead), add any cost increases (such as kicker), then apply any cost reductions. The spell's mana value remains unchanged, no matter what the total cost to cast it was.")
    }
}.let { it.copy(setCode="ZNR",oracleId="902eb71f-62b3-40cb-816b-f6b7e92e91e7",legalFormats=setOf(DeckFormat.PIONEER,DeckFormat.MODERN,DeckFormat.LEGACY,DeckFormat.PAUPER,DeckFormat.VINTAGE,DeckFormat.COMMANDER,DeckFormat.BRAWL),
    metadata=it.metadata.copy(scryfallId="42914fd9-e56e-4c3e-a445-fdfa45a45178",releaseDate="2020-09-25")) }
