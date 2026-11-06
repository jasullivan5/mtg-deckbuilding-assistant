package dev.mtg.assistant

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.explorer.StoredRun
import dev.mtg.assistant.objectives.creatureCardArrival
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

class ConditionalArrivalTest : FunSpec({
    val catalog by lazy { Catalog() }
    val report by lazy { discover(catalog, "Judith, the Scourge Diva") }
    val stored by lazy { StoredRun(catalog, report, Instant.EPOCH, 1, "test") }
    fun finding(name: String) = report.conditionalArrivals.single { it.card == name }
    fun fact(name: String) = report.facts.single { it.id == finding(name).factId }

    test("eligible native choices support the same checkpoint criterion across costs and effects") {
        val examples = mapOf("Viscera Seer" to "ACTIVATION_COST", "Accursed Centaur" to "EFFECT",
            "Blood Bairn" to "ACTIVATION_COST", "Carrion Feeder" to "ACTIVATION_COST",
            "Village Rites" to "CASTING_COST")
        for ((name, role) in examples) {
            val finding = finding(name)
            finding.decision.verdict shouldBe Verdict.MATCH
            report.eligibility.single { it.card == name }.let {
                it.format shouldBe Verdict.MATCH
                it.deck shouldBe Verdict.MATCH
            }
            fact(name).let {
                it.costOrEffect shouldBe role
                it.sourceZone shouldBe Zone.BATTLEFIELD
                it.destinationZone shouldBe Zone.GRAVEYARD
                it.destinationController shouldBe "OWNER"
            }
            val card = stored.summary.cards.single { it.card == name }
            card.conditionalGraveyardContribution shouldBe true
            val inspection = stored.card(card.index)!!.graveyardContributions.single()
            inspection.relationship.actionId shouldBe finding.actionId
            inspection.sources.any { "/blob/${report.argentumCommit}/" in it } shouldBe true
        }
        fact("Blood Bairn").sacrificeChoice!!.excludesSource shouldBe true
        fact("Viscera Seer").sacrificeChoice!!.excludesSource shouldBe false
        fact("Accursed Centaur").circumstances!!.mandatory shouldBe Verdict.MATCH
    }

    test("a controlled resource can fail the owner, token, type or destination requirements") {
        val choice = fact("Viscera Seer")
        val borrowed = choice.objectFilter.copy(controllerPredicate = ControllerPredicate.OwnedByOpponent)
        val tokens = choice.objectFilter.copy(cardPredicates = listOf(CardPredicate.IsCreature, CardPredicate.IsToken))
        val noncreature = choice.objectFilter.copy(cardPredicates = listOf(CardPredicate.IsNoncreature))
        for (filter in listOf(borrowed, tokens, noncreature))
            creatureCardArrival(choice.copy(objectFilter = filter)).verdict shouldBe Verdict.NO_MATCH
        creatureCardArrival(choice.copy(destinationZone = Zone.EXILE)).verdict shouldBe Verdict.NO_MATCH
        creatureCardArrival(choice.copy(objectFilter = GameObjectFilter.Land)).verdict shouldBe Verdict.UNKNOWN
        creatureCardArrival(choice.copy(destinationZone = null)).verdict shouldBe Verdict.UNKNOWN
        creatureCardArrival(choice.copy(sourceZone = null)).verdict shouldBe Verdict.UNKNOWN
        creatureCardArrival(null).verdict shouldBe Verdict.UNKNOWN
    }

    test("policy, eligibility, unsupported selection and recursion retain different outcomes") {
        report.results.single { it.card == "Murder" }.policyAcceptance shouldBe Verdict.NO_MATCH
        report.results.single { it.card == "Fleshbag Marauder" && it.operation == "SACRIFICE" }.policyAcceptance shouldBe Verdict.NO_MATCH
        report.conditionalArrivals.none { it.card in setOf("Murder", "Fleshbag Marauder", "Reassembling Skeleton") } shouldBe true
        report.eligibility.single { it.card == "Altar of Bone" }.deck shouldBe Verdict.NO_MATCH
        report.conditionalArrivals.none { it.card == "Altar of Bone" } shouldBe true
        finding("Agent of Shauku").decision.verdict shouldBe Verdict.UNKNOWN
        finding("Mogg Fanatic").decision.verdict shouldBe Verdict.UNKNOWN
    }
})
