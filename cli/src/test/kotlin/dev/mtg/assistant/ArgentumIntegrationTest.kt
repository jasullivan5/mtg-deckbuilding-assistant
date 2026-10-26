package dev.mtg.assistant

import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.collections.shouldContain
import io.kotest.assertions.throwables.shouldThrow

class ArgentumIntegrationTest : FunSpec({
    val catalog by lazy { Catalog() }

    test("real authored catalog is discovered and registered") {
        (catalog.setCount > 1) shouldBe true
        (catalog.cards.size > 100) shouldBe true
        catalog.card("Lightning Bolt")!!.name shouldBe "Lightning Bolt"
        catalog.card("Definitely Not An Authored Card") shouldBe null
    }
    test("retrieval exposes native script and actual damage effect") {
        val card = catalog.card("Lightning Bolt")!!
        val script: CardScript = card.script
        script.hasBehavior shouldBe true
        (script.spellEffect is DealDamageEffect) shouldBe true
        (script.spellEffect as DealDamageEffect).amount shouldBe DynamicAmount.Fixed(3)
        card.manaCost.toString() shouldBe "{R}"
        card.cmc shouldBe 1
        card.setCode shouldBe "LEA"
        describe(card) shouldContain "DealDamage"
        describe(card) shouldContain "Native CardScript"
    }
    test("existing mtg-search combines conventional type color cost and name filters") {
        catalog.search("t:instant c:r mv=1 name:bolt").map { it.name } shouldContain "Lightning Bolt"
        catalog.search("t:creature name:bolt").map { it.name }.contains("Lightning Bolt") shouldBe false
        catalog.search("!\"Lightning Bolt\"").map { it.name } shouldBe listOf("Lightning Bolt")
    }
    test("search projection preserves creature stats keywords and identity") {
        catalog.search("t:creature name:llanowar pow=1 tou=1 c:g").map { it.name } shouldContain "Llanowar Elves"
        val elf = catalog.card("Llanowar Elves")!!
        elf.script.activatedAbilities.isNotEmpty() shouldBe true
    }
    test("malformed queries fail instead of silently returning misleading matches") {
        shouldThrow<IllegalArgumentException> { catalog.search("t:(") }
    }
    test("CLI emits actual script and search results") {
        val output = mutableListOf<String>()
        runCli(arrayOf("card", "Lightning Bolt"), { catalog }, output::add) shouldBe 0
        output.joinToString("\n") shouldContain "DealDamage"
        output.clear()
        runCli(arrayOf("search", "!\"Lightning Bolt\""), { catalog }, output::add) shouldBe 0
        output.joinToString("\n") shouldContain "Lightning Bolt"
        output.last() shouldBe "Matches: 1"
    }
    test("CLI reports missing names and invalid commands") {
        val errors = mutableListOf<String>()
        runCli(arrayOf("card", "No Such Card"), { catalog }, {}, errors::add) shouldBe 1
        runCli(arrayOf("unknown"), { error("must not load") }, {}, errors::add) shouldBe 2
        runCli(arrayOf("search", "t:("), { catalog }, {}, errors::add) shouldBe 2
        errors.size shouldBe 3
    }
})
