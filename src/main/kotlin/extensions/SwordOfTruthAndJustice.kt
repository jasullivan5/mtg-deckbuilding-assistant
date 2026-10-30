package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.references.Player

/** Source-reviewed card: MH1 #229; full source and rulings in src/test/resources/cards. */
val SwordOfTruthAndJustice=card("Sword of Truth and Justice") {
    manaCost="{3}";colorIdentity=""
    typeLine="Artifact — Equipment"
    oracleText="Equipped creature gets +2/+2 and has protection from white and from blue.\nWhenever equipped creature deals combat damage to a player, put a +1/+1 counter on a creature you control, then proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)\nEquip {2}"
    staticAbility { ability=ModifyStats(2,2) }
    staticAbility { ability=GrantProtection(Color.WHITE) }
    staticAbility { ability=GrantProtection(Color.BLUE) }
    triggeredAbility {
        trigger=Triggers.attached.dealsCombatDamage(Recipient.AnyPlayer)
        effect=Effects.Pipeline {
            val creatures=gather(GameObjectFilter.Creature.youControl())
            ifNotEmpty(creatures) {
                val chosen=chooseExactly(1,from=creatures)
                // Use the single-entity placement path, which respects counter prohibitions at this pin.
                run(Effects.ForEachInCollection(chosen,Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE,1,EffectTarget.IterationEntity)))
            }
            run(Effects.Proliferate())
        }
    }
    equipAbility("{2}")
    metadata {
        rarity=Rarity.MYTHIC;collectorNumber="229";artist="Chris Rahn"
        imageUri="https://cards.scryfall.io/normal/front/2/b/2be8d24e-1370-4e85-90f2-66b6d6e9c4a4.jpg?1783933072"
        ruling("2023-02-04","If a permanent ever has both +1/+1 counters and -1/-1 counters on it at the same time, they're removed in pairs as a state-based action so that the permanent has only one of those kinds of counters on it.")
        ruling("2023-02-04","An ability that triggers \"Whenever you proliferate\" triggers even if you chose no permanents or players while doing so.")
        ruling("2023-02-04","Players can respond to a spell or ability whose effect includes proliferating. Once that spell or ability starts to resolve, however, and its controller chooses which permanents and players will get new counters, it's too late for anyone to respond.")
        ruling("2023-02-04","To proliferate, you can choose any permanent that has a counter, including ones controlled by opponents. You can choose any player who has a counter, including opponents. You can't choose cards in any zone other than the battlefield, even if they have counters on them.")
        ruling("2023-02-04","You don't have to choose every permanent or player that has a counter, only the ones you want to add another counter to. Since \"any number\" includes zero, you don't have to choose any permanents at all, and you don't have to choose any players at all.")
        ruling("2023-02-04","If a player or permanent has more than one kind of counter on it, and you choose for it to get additional counters, it must get one of each kind of counter it already has. You can't have it get just one kind of counter it already has and not the others.")
        ruling("2019-06-14","If a permanent or player you choose while proliferating has more than one kind of counter on it, it will get one more of each of those kinds. This means that if you put a +1/+1 counter on a creature you control with a -1/-1 counter on it, then choose that creature while proliferating, you'll put another of each of those counters on it before state-based actions remove them.")
        ruling("2019-06-14","Protection from a color means that the equipped creature can't be blocked by creatures of that color, can't be the target of spells of that color or abilities from sources of that color, can't be enchanted or equipped by Auras or Equipment of that color, and all damage that sources of that color would deal to it is prevented. Nothing other than these events is prevented or illegal.")
    }
}.let {it.copy(setCode="MH1",oracleId="7a8baaf9-e21f-41a0-9f15-80764f6e5e68",legalFormats=setOf(DeckFormat.MODERN,DeckFormat.LEGACY,DeckFormat.VINTAGE,DeckFormat.COMMANDER,DeckFormat.BRAWL),
    metadata=it.metadata.copy(scryfallId="2be8d24e-1370-4e85-90f2-66b6d6e9c4a4",releaseDate="2019-06-14"))}
