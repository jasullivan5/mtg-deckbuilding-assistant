package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.explorer.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class EventOutcomeExplanationTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun outcome(name:String)=PathwayExtractor().extract(listOf(catalog.card(name)!!)).eventOutcomes.single()
    test("native simple event restrictions read naturally without changing their scope") {
        incomingEvent(outcome("Grim Haruspex")) shouldBe "another nontoken creature you control dies"
        incomingEvent(outcome("Pitiless Plunderer")) shouldBe "another creature you control dies"
    }
    test("token outcomes use user-facing names while exact native references remain in evidence") {
        producedOutcome(outcome("Pitiless Plunderer")) shouldBe "you can create 1 Treasure token(s)"
        producedOutcome(outcome("Doomed Dissenter")) shouldBe "you can create 1 Zombie creature token(s)"
    }
    test("drawing for each player preserves the universal receiver in the explanation") {
        producedOutcome(outcome("Friendly Teddy")) shouldBe "each player can draw 1 card(s)"
    }
})
