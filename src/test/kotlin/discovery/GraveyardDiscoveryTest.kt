package dev.mtg.assistant

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
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
        rows("Agent of Shauku").single().verdict shouldBe Verdict.UNKNOWN
    }

    test("held-out targeting and opponent-only discard keep their own player and action bindings") {
        val triton = rows("Mire Triton").single()
        triton.verdict shouldBe Verdict.MATCH
        triton.stateEntry!!.route shouldBe GraveyardRoute.MILL
        triton.stateEntry.count shouldBe 2
        val millstone = rows("Millstone").single()
        millstone.verdict shouldBe Verdict.MATCH
        millstone.stateEntry!!.sourcePlayer shouldBe "ANY"
        val disciple = rows("Elderfang Disciple").single()
        disciple.stateEntry!!.route shouldBe GraveyardRoute.DISCARD
        disciple.verdict shouldBe Verdict.NO_MATCH
        facts("Elderfang Disciple").single().sourcePlayer shouldBe "OPPONENT"
    }

    test("discarding the source uses the actual card type and retains the activation requirements") {
        for (name in listOf("Twinshot Sniper", "Harvester of Misery", "Reinforced Ronin", "Trumpeting Carnosaur")) {
            val row = rows(name).single { it.stateEntry?.route == GraveyardRoute.DISCARD }
            val fact = facts(name).single { it.id == row.stateEntry!!.factId }
            row.verdict shouldBe Verdict.MATCH
            row.costOrEffect shouldBe "ACTIVATION_COST"
            row.stateEntry!!.deathComparison.verdict shouldBe Verdict.NO_MATCH
            fact.binding shouldBe "SELF"
            fact.cardEntry shouldBe CardEntry(GraveyardRoute.DISCARD, 1, discardsSource = true)
            fact.sourceZone shouldBe Zone.HAND
            fact.sourcePlayer shouldBe "YOU"
            fact.ownerScope shouldBe "YOU"
            fact.destinationZone shouldBe Zone.GRAVEYARD
            fact.destinationController shouldBe "OWNER"
            fact.objectFilter.cardPredicates shouldBe listOf(CardPredicate.IsCreature)
            val action = report.actions.single { it.id == fact.actionId }
            action.native.toString().contains("CostDiscardSelf") shouldBe true
            action.native.toString().contains("targetRequirements") shouldBe (name != "Reinforced Ronin")
        }
        val land = rows("Sokenzan, Crucible of Defiance").single { it.stateEntry?.route == GraveyardRoute.DISCARD }
        land.verdict shouldBe Verdict.NO_MATCH
        land.decisions.single().reason.contains("noncreature card") shouldBe true
    }

    test("exact source sacrifices distinguish the arrival type from the battlefield event") {
        for ((name, role) in listOf("Mogg Fanatic" to "ACTIVATION_COST", "Spark Elemental" to "EFFECT",
            "Reckless Barbarian" to "ACTIVATION_COST", "Ball Lightning" to "EFFECT")) {
            val row = rows(name).single { it.stateEntry?.sacrificesSource == true }
            val fact = facts(name).single { it.id == row.stateEntry!!.factId }
            row.verdict shouldBe Verdict.MATCH
            row.costOrEffect shouldBe role
            row.stateEntry!!.deathComparison.verdict shouldBe Verdict.MATCH
            fact.binding shouldBe "SELF"
            fact.sourceZone shouldBe Zone.BATTLEFIELD
            fact.sourcePlayer shouldBe "YOU"
            fact.destinationZone shouldBe Zone.GRAVEYARD
            fact.destinationController shouldBe "OWNER"
            fact.ownerScope shouldBe "ANY"
            fact.sacrificeChoice shouldBe null
            fact.cardEntry shouldBe CardEntry(GraveyardRoute.SACRIFICE, 1, sourceCreatureAtArrival = Verdict.MATCH)
        }
        for (name in listOf("Seal of Fire", "Evolving Wilds")) {
            val row = rows(name).single { it.stateEntry?.sacrificesSource == true }
            row.verdict shouldBe Verdict.NO_MATCH
            row.stateEntry!!.deathComparison.verdict shouldBe Verdict.UNKNOWN
        }
        val mogg = facts("Mogg Fanatic").single()
        evaluateStateEntry(mogg.copy(ownerScope = "OPPONENT"), GraveyardRoute.SACRIFICE).verdict shouldBe Verdict.NO_MATCH
        evaluateStateEntry(mogg.copy(movesOriginalCard = false), GraveyardRoute.SACRIFICE).verdict shouldBe Verdict.NO_MATCH
        evaluateStateEntry(mogg.copy(cardEntry = mogg.cardEntry!!.copy(sourceCreatureAtArrival = Verdict.UNKNOWN)),
            GraveyardRoute.SACRIFICE).verdict shouldBe Verdict.UNKNOWN
        rows("Pit Raptor").single { it.operation == "SACRIFICE" }.verdict shouldBe Verdict.UNKNOWN
    }

    test("unsupported source characteristics and inconsistent activation zones never become a match") {
        fun source(zone: Zone) = card("Synthetic discard source") {
            typeLine = "Creature — Beast"
            power = 1
            toughness = 1
            activatedAbility {
                cost = AbilityCost.DiscardSelf
                activateFromZone = zone
                effect = DrawCardsEffect(1)
            }
        }
        val backFace = card("Synthetic back face") { typeLine = "Land" }
        val multiFace = PathwayExtractor(inspectCardEntries = true).extract(listOf(source(Zone.HAND).copy(backFace = backFace)))
        graveyardEntries(multiFace.facts).single().verdict shouldBe Verdict.UNKNOWN
        val wrongZone = PathwayExtractor(inspectCardEntries = true).extract(listOf(source(Zone.BATTLEFIELD)))
        graveyardEntries(wrongZone.facts).isEmpty() shouldBe true
        wrongZone.unsupported.any { it.path == "script.activatedAbilities[0].cost" } shouldBe true
    }

    test("Commander eligibility and the established death policy remain separate") {
        report.eligibility.single { it.card == "Tome Scour" }.deck shouldBe Verdict.NO_MATCH
        ("Tome Scour" in report.mechanicalInputCards.orEmpty()) shouldBe false
        rows("Tome Scour") shouldBe emptyList()
        report.eligibility.single { it.card == "Codex Shredder" }.deck shouldBe Verdict.UNKNOWN
        rows("Codex Shredder") shouldBe emptyList()
        val shredder = PathwayExtractor(inspectCardEntries = true).extract(listOf(catalog.card("Codex Shredder")!!))
        val nativeMill = graveyardEntries(shredder.facts).single { it.fact.operation == "MILL" }
        nativeMill.verdict shouldBe Verdict.MATCH
        nativeMill.fact.sourcePlayer shouldBe "ANY"
        rows("Reassembling Skeleton") shouldBe emptyList()
        rows("Murder") shouldBe emptyList()
        val deaths = discover(catalog, report.commander, conventionalQuery = "name:Murder")
        deaths.results.single { it.card == "Murder" }.policyAcceptance shouldBe Verdict.NO_MATCH
    }
})
