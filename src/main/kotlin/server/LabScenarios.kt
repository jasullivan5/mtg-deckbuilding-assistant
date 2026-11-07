package dev.mtg.assistant.explorer

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.mtg.sets.definitions.`5dn`.cards.SparkElemental
import com.wingedsheep.mtg.sets.definitions.m11.cards.Plains230
import com.wingedsheep.mtg.sets.definitions.mrd.cards.LifesparkSpellbomb
import com.wingedsheep.mtg.sets.definitions.roe.cards.EvolvingWilds
import com.wingedsheep.mtg.sets.definitions.tmp.cards.MoggFanatic
import com.wingedsheep.mtg.sets.definitions.tsp.cards.MomentaryBlink
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import kotlinx.serialization.Serializable

@Serializable internal data class LabScenarioInfo(
    val id: String, val card: String, val title: String, val question: String, val mechanic: String,
    val conditions: String, val expected: String, val rules: String, val limits: String,
    val cardSource: String, val supportingSource: String? = null
)

internal class LabScenario(
    val info: LabScenarioInfo,
    val card: CardDefinition,
    val setup: () -> LabExecution,
    val run: (LabExecution) -> Boolean
)

internal object LabScenarios {
    private fun mogg(targeted: Boolean): LabScenario {
        val info = LabScenarioInfo(
            if (targeted) "mogg-pay" else "mogg-no-target", MoggFanatic.name,
            if (targeted) "Pay the sacrifice cost" else "Omit the required target",
            "When does Mogg Fanatic go to the graveyard?",
            "Discovery reads a sacrifice of the exact source as an activation cost, with a creature card as its ordinary arrival.",
            "Player 1 owns and controls Mogg Fanatic. Both graveyards are empty and the opponent is at 20 life.",
            if (targeted) "Paying the cost moves Mogg Fanatic to its owner's graveyard while the ability is still on the stack. The opponent takes 1 damage on resolution."
            else "Submitting the activation with no target is illegal. The cost stays unpaid, and the same Mogg Fanatic remains on the battlefield.",
            "CR 113.7a, 602.2, 601.2c/h, 608.2, 700.4, 701.21a",
            "This position leaves out replacement effects, borrowed or token copies, and targets that become illegal after payment.",
            "https://scryfall.com/card/tmp/190/mogg-fanatic"
        )
        return LabScenario(info, MoggFanatic, { position(MoggFanatic) }) { game ->
            val original = game.checkpoint().sourceRef
            val beganAsCreature = game.sourceTypes()?.isCreature == true
            val targets = if (targeted) listOf(ChosenTarget.Player(game.opponent)) else emptyList()
            game.perform(if (targeted) "Activate targeting the opponent" else "Attempt activation without choosing a target",
                ActivateAbility(game.player, game.source, MoggFanatic.activatedAbilities.single().id, targets), allowIllegal = !targeted)
            val payment = game.checkpoint()
            if (targeted) {
                val paid = beganAsCreature && game.observedSacrifice(original) && payment.opponentLife == 20 &&
                    payment.stackSize == 1 && payment.creatureCardInGraveyard == true && game.damageToOpponent() == 0
                game.passPair("Resolve the damage ability")
                val final = game.checkpoint()
                paid && final.opponentLife == 19 && final.stackSize == 0 && game.damageToOpponent() == 1
            } else {
                beganAsCreature && game.actions.single().outcome == "ILLEGAL_ACTION" && game.sacrifices.isEmpty() &&
                    game.sourceMoves.isEmpty() && payment.sourceRef == original && payment.sourceZone == Zone.BATTLEFIELD &&
                    payment.stackSize == 0 && payment.opponentLife == 20 && payment.creatureCardInGraveyard == false
            }
        }
    }

    private val animated = LabScenario(
        LabScenarioInfo("wilds-animated", EvolvingWilds.name, "Animate and sacrifice the land",
            "Can a creature die and leave a land card in the graveyard?",
            "The native cost sacrifices the land itself. The graveyard query checks the arriving card type. Animation gives the departing permanent a separate creature characteristic.",
            "Player 1 has controlled Evolving Wilds since the turn began, owns Lifespark Spellbomb, and has one green mana available.",
            "Lifespark Spellbomb animates the land. When the animated land is sacrificed, the creature dies. The arriving card is a land.",
            "CR 302.6, 400.7, 602.2, 611.2, 700.4, 701.21a",
            "This run stops after payment, with the basic land search still on the stack. It leaves out effects that redirect the sacrifice.",
            "https://scryfall.com/card/roe/228/evolving-wilds", "https://scryfall.com/card/mrd/197/lifespark-spellbomb"),
        EvolvingWilds, { position(EvolvingWilds, listOf(LabSeed(LifesparkSpellbomb, Zone.BATTLEFIELD)), ManaPoolComponent(green = 1)) }
    ) { game ->
        val original = game.checkpoint().sourceRef
        val before = game.sourceTypes()
        game.perform("Activate Lifespark Spellbomb targeting Evolving Wilds",
            ActivateAbility(game.player, game.id(LifesparkSpellbomb), LifesparkSpellbomb.activatedAbilities.first().id,
                listOf(ChosenTarget.Permanent(game.source)), paymentStrategy = PaymentStrategy.FromPool))
        game.passPair("Resolve the land animation")
        val animatedTypes = game.sourceTypes()
        game.perform("Tap and sacrifice Evolving Wilds to activate its search",
            ActivateAbility(game.player, game.source, EvolvingWilds.activatedAbilities.single().id))
        val final = game.checkpoint()
        val arrivedTypes = game.sourceTypes()
        before?.isLand == true && !before.isCreature &&
            animatedTypes?.isLand == true && animatedTypes.isCreature &&
            game.observedSacrifice(original) && arrivedTypes?.isLand == true && !arrivedTypes.isCreature &&
            final.creatureCardInGraveyard == false && final.stackSize == 1
    }

    private fun spark(blink: Boolean): LabScenario {
        val info = LabScenarioInfo(
            if (blink) "spark-blink" else "spark-ordinary", SparkElemental.name,
            if (blink) "Blink in response to the trigger" else "Let the end-step trigger resolve",
            "Which Spark Elemental can the end-step trigger sacrifice?",
            "Discovery reads a sacrifice of the source on resolution of the end-step trigger. The trigger refers to the original permanent.",
            if (blink) "Player 1 owns and controls Spark Elemental. Momentary Blink is in hand, with two untapped Plains available to pay for it."
            else "Player 1 owns and controls Spark Elemental. Play starts in the postcombat main phase.",
            if (blink) "Blink returns the same card as a new permanent. The old trigger resolves and leaves the returned permanent on the battlefield."
            else "The original permanent stays until the trigger resolves, then is sacrificed and goes to its owner's graveyard.",
            "CR 113.7a, 400.7, 603.3, 608.2, 700.4, 701.21a",
            if (blink) "The observation stops after the original trigger resolves. The returned permanent can trigger at a later end step."
            else "This position leaves out responses, control changes and graveyard replacements.",
            "https://scryfall.com/card/5dn/79/spark-elemental",
            if (blink) "https://scryfall.com/card/tsp/29/momentary-blink" else null
        )
        val support = if (blink) listOf(LabSeed(MomentaryBlink, Zone.HAND),
            LabSeed(Plains230, Zone.BATTLEFIELD), LabSeed(Plains230, Zone.BATTLEFIELD)) else emptyList()
        return LabScenario(info, SparkElemental, { LabExecution(SparkElemental, Step.POSTCOMBAT_MAIN, support, ManaPoolComponent()) }) { game ->
            val original = game.checkpoint().sourceRef
            game.passPair("Reach the end step and put the trigger on the stack")
            val triggered = game.checkpoint()
            val pointsToOriginal = original != null && triggered.triggerSource == original &&
                triggered.stackSize == 1 && triggered.sourceZone == Zone.BATTLEFIELD
            if (blink) {
                game.perform("Cast Momentary Blink targeting Spark Elemental",
                    CastSpell(game.player, game.id(MomentaryBlink), listOf(ChosenTarget.Permanent(game.source))))
                game.passPair("Resolve Momentary Blink")
            }
            val beforeTrigger = game.checkpoint()
            game.passPair("Resolve the original end-step trigger")
            val final = game.checkpoint()
            pointsToOriginal && final.stackSize == 0 && if (blink) {
                val returned = beforeTrigger.sourceRef
                val moves = game.sourceMoves
                returned != null && returned.entityId == original.entityId && returned != original &&
                    beforeTrigger.triggerSource == original && beforeTrigger.stackSize == 1 &&
                    moves.map { it.fromZone to it.toZone } == listOf(Zone.BATTLEFIELD to Zone.EXILE, Zone.EXILE to Zone.BATTLEFIELD) &&
                    moves.first().oldObject == original && moves.last().newObject == returned &&
                    game.sacrifices.isEmpty() && final.sourceRef == returned && final.sourceZone == Zone.BATTLEFIELD &&
                    final.creatureCardInGraveyard == false
            } else {
                game.observedSacrifice(original) && final.creatureCardInGraveyard == true
            }
        }
    }

    private fun position(card: CardDefinition, support: List<LabSeed> = emptyList(), mana: ManaPoolComponent = ManaPoolComponent()) =
        LabExecution(card, Step.PRECOMBAT_MAIN, support, mana)

    private fun LabExecution.observedSacrifice(original: ObjectRef?): Boolean {
        val move = sourceMoves.singleOrNull() ?: return false
        val after = checkpoint()
        return original != null && move.oldObject == original && move.newObject == after.sourceRef &&
            move.newObject != original && move.ownerId == player && move.lastKnown?.controllerId == player &&
            move.requestedDestination == Zone.GRAVEYARD && move.toZone == Zone.GRAVEYARD &&
            move.wasSacrificed && sacrifices.singleOrNull()?.playerId == player && creatureDied() == true &&
            after.sourceZone == Zone.GRAVEYARD && sourceCard.name in after.ownerGraveyard
    }

    val all = listOf(mogg(true), mogg(false), animated, spark(false), spark(true)).associateBy { it.info.id }
}
