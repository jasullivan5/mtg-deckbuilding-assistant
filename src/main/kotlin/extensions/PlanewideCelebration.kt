package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.references.Player

/** Source-reviewed card: WAR #172; archived Oracle and rulings in src/test/resources/cards. */
val PlanewideCelebration=card("Planewide Celebration") {
    manaCost="{5}{G}{G}"
    colorIdentity="G"
    typeLine="Sorcery"
    oracleText="Choose four. You may choose the same mode more than once.\n• Create a 2/2 Citizen creature token that's all colors.\n• Return target permanent card from your graveyard to your hand.\n• Proliferate.\n• You gain 4 life."
    spell {
        modal(chooseCount=4,allowRepeat=true) {
            mode("Create a 2/2 Citizen creature token that's all colors") {
                effect = Effects.CreateToken(power=2,toughness=2,colors=Color.entries.toSet(),creatureTypes=setOf("Citizen"),
                    imageUri="https://cards.scryfall.io/normal/front/d/e/de7ba875-f77b-404f-8b75-4ba6f81da410.jpg?1783933348")
            }
            mode("Return target permanent card from your graveyard to your hand") {
                val permanent=target(TargetFilter.PermanentInYourGraveyard)
                effect=Effects.ReturnToHandFromGraveyard(permanent)
            }
            mode("Proliferate") { effect=Effects.Proliferate() }
            mode("You gain 4 life") { effect=Effects.GainLife(4) }
        }
    }
    metadata {
        rarity=Rarity.RARE;collectorNumber="172";artist="Wisnu Tan"
        imageUri="https://cards.scryfall.io/normal/front/1/1/11016840-56b6-4759-bde6-3dc0e339e87e.jpg?1783933409"
        ruling("2023-02-04","If a permanent ever has both +1/+1 counters and -1/-1 counters on it at the same time, they're removed in pairs as a state-based action so that the permanent has only one of those kinds of counters on it.")
        ruling("2023-02-04","If a player or permanent has more than one kind of counter on it, and you choose for it to get additional counters, it must get one of each kind of counter it already has. You can't have it get just one kind of counter it already has and not the others.")
        ruling("2023-02-04","An ability that triggers \"Whenever you proliferate\" triggers even if you chose no permanents or players while doing so.")
        ruling("2023-02-04","Players can respond to a spell or ability whose effect includes proliferating. Once that spell or ability starts to resolve, however, and its controller chooses which permanents and players will get new counters, it's too late for anyone to respond.")
        ruling("2023-02-04","To proliferate, you can choose any permanent that has a counter, including ones controlled by opponents. You can choose any player who has a counter, including opponents. You can't choose cards in any zone other than the battlefield, even if they have counters on them.")
        ruling("2023-02-04","You don't have to choose every permanent or player that has a counter, only the ones you want to add another counter to. Since \"any number\" includes zero, you don't have to choose any permanents at all, and you don't have to choose any players at all.")
        ruling("2019-05-03","A permanent card is an artifact, creature, enchantment, land, or planeswalker card.")
        ruling("2019-05-03","If the second mode is chosen at least once, and every target permanent card is an illegal target by the time Planewide Celebration tries to resolve, the spell doesn't resolve. None of the other chosen modes happen. If only some of the targets are illegal targets but at least one is still legal, you still do as much as you can and the illegal targets aren't affected.")
        ruling("2019-05-03","No matter which combination of modes you choose, you always follow the instructions of Planewide Celebration in the order they are written. If a mode is chosen more than once, you perform that mode's instruction that many times sequentially.")
    }
}.let { it.copy(setCode="WAR",oracleId="af23d348-adc9-451e-af08-bf9b1986c591",
    legalFormats=setOf(DeckFormat.PIONEER,DeckFormat.MODERN,DeckFormat.LEGACY,DeckFormat.VINTAGE,DeckFormat.COMMANDER,DeckFormat.BRAWL),
    metadata=it.metadata.copy(scryfallId="11016840-56b6-4759-bde6-3dc0e339e87e",releaseDate="2019-05-03")) }
