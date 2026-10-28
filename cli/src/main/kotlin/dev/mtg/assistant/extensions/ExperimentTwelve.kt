package dev.mtg.assistant.extensions

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.references.Player

/** Source-led batch 1: MKC #37; archived Oracle and rulings in reference/alignment-batch1. */
val ExperimentTwelve=card("Experiment Twelve") {
    manaCost="{3}{G}"
    colorIdentity="G"
    typeLine="Creature — Elf Lizard Warrior"
    oracleText="Trample\nWhenever this creature or another creature you control is turned face up, put +1/+1 counters on that creature equal to its power.\nDisguise {4}{G} (You may cast this card face down for {3} as a 2/2 creature with ward {2}. Turn it face up any time for its disguise cost.)"
    power=4;toughness=4
    keywords(Keyword.TRAMPLE)
    disguise = "{4}{G}"
    triggeredAbility {
        trigger = Triggers.you.permanentTurnedFaceUp(GameObjectFilter.Creature)
        effect = Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.powerOf(EffectTarget.TriggeringEntity),EffectTarget.TriggeringEntity)
    }
    metadata {
        rarity=Rarity.RARE;collectorNumber="37";artist="Michele Giorgi"
        imageUri="https://cards.scryfall.io/normal/front/7/d/7d76089e-b21a-4d89-9562-957c18474967.jpg?1783913042"
        ruling("2024-02-02","A disguise ability lets you cast a card face down by paying {3} and announcing that you are using a disguise ability. Any time you have priority, you can turn a face-down permanent with disguise face up by paying its disguise cost.")
        ruling("2024-02-02","The face-down spell has no mana cost and a mana value of 0. When you cast a face-down spell, put it on the stack face down so no other player knows what it is, and pay {3} to cast it. This is an alternative cost.")
        ruling("2024-02-02","The creature spell is a 2/2 creature spell with ward {2} that has no name, mana cost, or creature types. The resulting creature is a 2/2 creature with ward {2} that has no name, mana cost, or creature types. Both the spell and the resulting creature are colorless and have a mana value of 0. Other effects that apply to the spell or creature can still grant it any characteristics it doesn’t have or change the characteristics it does have.")
        ruling("2024-02-02","Any time you have priority, you may turn the face-down creature face up by revealing what its disguise cost is and paying that cost. This is a special action. It doesn’t use the stack and can’t be responded to. Only a face-down permanent can be turned face up this way; a face-down spell cannot.")
        ruling("2024-02-02","At any time, you can look at a face-down spell or permanent you control. You can’t look at face-down permanents or spells you don’t control unless an effect instructs or allows you to do so.")
        ruling("2024-02-02","If a face-down creature loses its abilities, it can’t be turned face up with a disguise ability because it will no longer have a disguise ability (or a disguise cost) once face up.")
        ruling("2024-02-02","Because the permanent is on the battlefield both before and after it’s turned face up, turning a permanent face up doesn’t cause any enters-the-battlefield abilities to trigger.")
        ruling("2024-02-02","Because face-down creatures don’t have a name, they can’t have the same name as any other creature, even another face-down creature.")
        ruling("2024-02-02","A permanent that turns face up or face down changes characteristics but is otherwise the same permanent. Spells and abilities that were targeting that permanent and Auras and Equipment that were attached to that permanent aren’t affected unless the new characteristics of the object change the legality of those targets or attachments.")
        ruling("2024-02-02","Turning a permanent face up or face down doesn’t change whether that permanent is tapped or untapped.")
        ruling("2024-02-02","If a face-down spell leaves the stack and goes to any zone other than the battlefield (if it was countered, for example), you must reveal it. Similarly, if a face-down permanent leaves the battlefield, you must reveal it. You must also reveal all face-down spells and permanents you control if you leave the game or the game ends.")
        ruling("2024-02-02","You must ensure that your face-down spells and permanents can be easily differentiated from each other. You’re not allowed to mix up the cards that represent them on the battlefield to confuse other players. The order in which they entered the battlefield should remain clear, as well as what ability caused them to be face down. (This includes disguise, cloak, and in games involving older cards, morph and manifest, as well as a few other effects that turn cards face down.) Common methods for doing this include using markers or dice, or simply placing them in order on the battlefield.")
    }
}.let { it.copy(setCode="MKC",oracleId="67a10da3-76f2-4f4a-90d1-e77abb5842fe",
    legalFormats=setOf(DeckFormat.LEGACY,DeckFormat.VINTAGE,DeckFormat.COMMANDER),
    metadata=it.metadata.copy(scryfallId="7d76089e-b21a-4d89-9562-957c18474967",releaseDate="2024-02-09")) }
