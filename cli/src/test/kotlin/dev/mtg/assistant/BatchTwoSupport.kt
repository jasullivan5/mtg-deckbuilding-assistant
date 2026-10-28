package dev.mtg.assistant

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.combat.*
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import dev.mtg.assistant.extensions.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import com.wingedsheep.engine.support.TestCards

internal fun batchTwoDriver(vararg cards: CardDefinition)=GameTestDriver().also {
    it.registerCards(TestCards.all+cards.toList());it.initMirrorMatch(deck=Deck.of("Forest" to 40));it.passPriorityUntil(Step.PRECOMBAT_MAIN)
}
internal fun GameTestDriver.drainBatchTwo(accept: Boolean=false) {
    repeat(80) {
        when(val choice=pendingDecision) {
            is ChooseColorDecision -> submitDecision(choice.playerId,ColorChosenResponse(choice.id,Color.GREEN)).error shouldBe null
            is YesNoDecision -> submitYesNo(choice.playerId,accept).error shouldBe null
            is SelectManaSourcesDecision -> submitDecision(choice.playerId,ManaSourcesSelectedResponse(choice.id,autoPay=true)).error shouldBe null
            null -> if(stackSize>0) bothPass().error shouldBe null else return
            else -> autoResolveDecision()
        }
    };error("Unsettled native resolution")
}
internal fun GameTestDriver.tokenIds(player: EntityId)=state.getBattlefield(player).filter { state.getEntity(it)!!.has<TokenComponent>() }

internal fun GameTestDriver.clearHand(player: EntityId) { getHand(player).forEach { moveToGraveyard(it) } }
