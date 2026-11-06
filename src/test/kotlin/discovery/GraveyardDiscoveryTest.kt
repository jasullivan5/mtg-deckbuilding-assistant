package dev.mtg.assistant

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.objectives.GRAVEYARD_CREATURE_OBJECTIVE
import dev.mtg.assistant.objectives.evaluateStateEntry
import dev.mtg.assistant.objectives.graveyardEntries
import dev.mtg.assistant.explorer.StoredRun
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

class GraveyardDiscoveryTest : FunSpec({
    val catalog by lazy { Catalog() }
    val report by lazy { discover(catalog, "Judith, the Scourge Diva", objective = GRAVEYARD_CREATURE_OBJECTIVE) }
    fun rows(name: String) = report.results.filter { it.card == name }
    fun facts(name: String) = rows(name).map { row -> report.facts.single { it.id == row.stateEntry!!.factId } }

    test("one state query accepts distinct native movements before any death comparison") {
        val examples = listOf(
            Triple("Viscera Seer", GraveyardRoute.SACRIFICE, Zone.BATTLEFIELD),
            Triple("Accursed Centaur", GraveyardRoute.SACRIFICE, Zone.BATTLEFIELD),
            Triple("Stitcher's Supplier", GraveyardRoute.MILL, Zone.LIBRARY),
            Triple("Faithless Looting", GraveyardRoute.DISCARD, Zone.HAND),
            Triple("Thrill of Possibility", GraveyardRoute.DISCARD, Zone.HAND)
        )
        for ((name, route, zone) in examples) {
            val matches = rows(name).filter { it.verdict == Verdict.MATCH }
            matches.isNotEmpty() shouldBe true
            val matchFacts = facts(name).filter { fact -> matches.any { it.stateEntry!!.factId == fact.id } }
            graveyardEntries(matchFacts).map { it.verdict }.distinct() shouldBe listOf(Verdict.MATCH)
            for (row in matches) {
                row.stateEntry!!.route shouldBe route
                row.stateEntry.deathComparison.verdict shouldBe if (route == GraveyardRoute.SACRIFICE) Verdict.MATCH else Verdict.NO_MATCH
                row.sourceReferences.any { "/blob/${report.argentumCommit}/" in it } shouldBe true
            }
            matchFacts.map { it.sourceZone }.distinct() shouldBe listOf(zone)
            matchFacts.map { it.destinationZone }.distinct() shouldBe listOf(Zone.GRAVEYARD)
        }
        rows("Viscera Seer").single().costOrEffect shouldBe "ACTIVATION_COST"
        rows("Accursed Centaur").single().costOrEffect shouldBe "EFFECT"
        rows("Thrill of Possibility").single().costOrEffect shouldBe "CASTING_COST"
        rows("Faithless Looting").single().stateEntry!!.count shouldBe 2
        rows("Blood Bairn").single().stateEntry!!.excludesSource shouldBe true
    }

    test("the Supplier's distinct triggers both mill and neither movement causes a death") {
        val supplier = rows("Stitcher's Supplier")
        supplier.size shouldBe 2
        supplier.map { it.actionId }.distinct().size shouldBe 2
        supplier.map { it.stateEntry!!.count } shouldBe listOf(3, 3)
        val stored = StoredRun(catalog, report, Instant.EPOCH, 1, "test")
        val index = stored.summary.cards.single { it.card == "Stitcher's Supplier" }.index
        val notes = stored.card(index)!!.abilities.map { it.objectiveNotes }
        notes.any { it.any { note -> note.startsWith("When this permanent enters") } } shouldBe true
        notes.any { it.any { note -> note.startsWith("When this permanent dies") } } shouldBe true
        notes.all { it.any { note -> "starts outside the battlefield" in note } } shouldBe true
    }

    test("ownership, physical card status, type, quantity and destination constrain one movement") {
        val seer = facts("Viscera Seer").single()
        val mill = facts("Stitcher's Supplier").first()
        evaluateStateEntry(seer.copy(sourcePlayer = "OPPONENT", ownerScope = "YOU"), GraveyardRoute.SACRIFICE).verdict shouldBe Verdict.MATCH
        evaluateStateEntry(seer.copy(sourcePlayer = "YOU", ownerScope = "OPPONENT"), GraveyardRoute.SACRIFICE).verdict shouldBe Verdict.NO_MATCH
        for (predicate in listOf(CardPredicate.IsToken, CardPredicate.IsNoncreature)) {
            val filter = seer.objectFilter.copy(cardPredicates = listOf(predicate))
            evaluateStateEntry(seer.copy(objectFilter = filter), GraveyardRoute.SACRIFICE).verdict shouldBe Verdict.NO_MATCH
        }
        evaluateStateEntry(mill.copy(movesOriginalCard = false), GraveyardRoute.MILL).verdict shouldBe Verdict.NO_MATCH
        evaluateStateEntry(mill.copy(destinationZone = Zone.EXILE), GraveyardRoute.MILL).verdict shouldBe Verdict.NO_MATCH
        evaluateStateEntry(mill.copy(destinationZone = null), GraveyardRoute.MILL).verdict shouldBe Verdict.UNKNOWN
        evaluateStateEntry(mill.copy(cardEntry = CardEntry(GraveyardRoute.MILL, 0)), GraveyardRoute.MILL).verdict shouldBe Verdict.NO_MATCH
        evaluateStateEntry(mill.copy(cardEntry = CardEntry(GraveyardRoute.MILL, null)), GraveyardRoute.MILL).verdict shouldBe Verdict.UNKNOWN
        rows("Mogg Fanatic").single().verdict shouldBe Verdict.UNKNOWN
        rows("Agent of Shauku").single().verdict shouldBe Verdict.UNKNOWN
    }

    test("held-out targeting and opponent-only discard keep their own player and action bindings") {
        val shredder = rows("Codex Shredder").single { it.stateEntry!!.route == GraveyardRoute.MILL }
        shredder.verdict shouldBe Verdict.MATCH
        shredder.stateEntry!!.sourcePlayer shouldBe "ANY"
        rows("Codex Shredder").count { it.verdict == Verdict.MATCH } shouldBe 1
        val disciple = rows("Elderfang Disciple").single()
        disciple.stateEntry!!.route shouldBe GraveyardRoute.DISCARD
        disciple.verdict shouldBe Verdict.NO_MATCH
        facts("Elderfang Disciple").single().sourcePlayer shouldBe "OPPONENT"
    }

    test("Commander eligibility and the established death policy remain separate") {
        report.eligibility.single { it.card == "Tome Scour" }.deck shouldBe Verdict.NO_MATCH
        "Tome Scour" in report.mechanicalInputCards.orEmpty() shouldBe false
        rows("Tome Scour") shouldBe emptyList()
        rows("Reassembling Skeleton") shouldBe emptyList()
        rows("Murder") shouldBe emptyList()
        val deaths = discover(catalog, report.commander, conventionalQuery = "name:Murder")
        deaths.results.single().policyAcceptance shouldBe Verdict.NO_MATCH
    }
})
