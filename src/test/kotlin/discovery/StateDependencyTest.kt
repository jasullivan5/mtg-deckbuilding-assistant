package dev.mtg.assistant

import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.*
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.references.Player
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import java.io.File

class StateDependencyTest : FunSpec({
    val catalog by lazy { Catalog() }
    val report by lazy { discover(catalog,"Kenrith, the Returned King",objective=DEATH_HISTORY_OBJECTIVE) }
    fun extract(name: String) = PathwayExtractor(inspectStateDependencies=true).extract(listOf(catalog.card(name)!!))
    fun verdict(d: StateDependency, a: StateAnchor) = all(stateDependencyDecisions(d,a).map { it.verdict })

    test("native morbid checks change exact resolution outcomes without manufacturing a trigger") {
        for(name in listOf("Tragic Slip","Hunger of the Howlpack")) {
            val r=report.results.single { it.card==name }
            r.verdict shouldBe Verdict.MATCH
            val d=checkNotNull(r.stateDependency)
            d.checkpoint shouldBe StateCheckpoint.RESOLUTION
            d.condition shouldBe CreatureDiedThisTurnCondition
            d.incomingEvent shouldBe null
            (d.whenTrue!=d.whenFalse) shouldBe true
        }
    }
    test("intervening-if retains both checkpoints and the own-controller scope, not the unrelated cost ability") {
        val r=report.results.single { it.card=="Denethor, Ruling Steward" }
        r.verdict shouldBe Verdict.MATCH
        val d=checkNotNull(r.stateDependency)
        d.checkpoint shouldBe StateCheckpoint.TRIGGER_AND_RESOLUTION
        d.condition shouldBe ControlledCreatureDiedThisTurnCondition
        verdict(d,StateAnchor.DeathHistory(controller=Controller.OPPONENT)) shouldBe Verdict.NO_MATCH
        verdict(d,StateAnchor.DeathHistory(controller=Controller.YOU,token=TokenStatus.TOKEN)) shouldBe Verdict.MATCH
        (d.actionId.contains("triggeredAbilities[0]")) shouldBe true
    }
    test("event, source resolution history and replacement remain distinct") {
        val grim=extract("Grim Haruspex")
        grim.stateDependencies.isEmpty() shouldBe true
        grim.eventOutcomes.isNotEmpty() shouldBe true
        val tannuk=extract("Tannuk, Memorial Ensign")
        val d=tannuk.stateDependencies.single { it.scope==StateScope.SOURCE_ABILITY_HISTORY }
        d.incomingEvent?.confidence shouldBe Verdict.MATCH
        d.contextSupport shouldBe Verdict.UNKNOWN
        verdict(d,StateAnchor.DeathHistory()) shouldBe Verdict.NO_MATCH
        tannuk.eventOutcomes.single().confidence shouldBe Verdict.UNKNOWN
        val scales=extract("Hardened Scales")
        scales.stateDependencies.isEmpty() shouldBe true
        scales.eventOutcomes.isEmpty() shouldBe true
        report.results.none { it.card in setOf("Grim Haruspex","Tannuk, Memorial Ensign","Hardened Scales") } shouldBe true
    }
    test("same count predicate distinguishes six lands at resolution from the land-entry event") {
        val x=extract("Scute Swarm")
        val d=x.stateDependencies.single { it.scope==StateScope.BATTLEFIELD_COUNT }
        val six=StateAnchor.BattlefieldCount(Player.You,GameObjectFilter.Land,6)
        d.checkpoint shouldBe StateCheckpoint.RESOLUTION
        d.incomingEvent?.confidence shouldBe Verdict.MATCH
        verdict(d,six) shouldBe Verdict.UNKNOWN // Copy and baseline are not comparable magnitudes.
        verdict(d,six.copy(count=5)) shouldBe Verdict.NO_MATCH
        verdict(d,six.copy(player=Player.EachOpponent)) shouldBe Verdict.NO_MATCH
        verdict(d,six.copy(filter=GameObjectFilter.Creature)) shouldBe Verdict.UNKNOWN
        val synthetic=card("Threshold test fixture") {
            typeLine="Enchantment"; manaCost="{G}"
            triggeredAbility {
                trigger=checkNotNull(d.incomingEvent).trigger
                effect=Effects.If(d.condition,DrawCardsEffect(2),DrawCardsEffect(1))
            }
        }
        val test=PathwayExtractor(inspectStateDependencies=true).extract(listOf(synthetic)).stateDependencies.single()
        test.incomingEvent?.trigger shouldBe d.incomingEvent?.trigger
        test.incomingEvent?.confidence shouldBe Verdict.MATCH
        verdict(test,six) shouldBe Verdict.MATCH
        verdict(test,six.copy(count=5)) shouldBe Verdict.NO_MATCH
        verdict(test,StateAnchor.DeathHistory()) shouldBe Verdict.NO_MATCH
    }
    test("equal effects, nested history and unrelated sibling effects do not manufacture a benefit") {
        val synthetic=card("Branch isolation test fixture") {
            typeLine="Sorcery"; manaCost="{B}"
            spell { effect=Effects.If(CreatureDiedThisTurnCondition,DrawCardsEffect(1),DrawCardsEffect(1)) then DrawCardsEffect(3) }
        }
        val d=PathwayExtractor(inspectStateDependencies=true).extract(listOf(synthetic)).stateDependencies.single()
        d.whenTrue shouldBe DrawCardsEffect(1)
        verdict(d,StateAnchor.DeathHistory()) shouldBe Verdict.NO_MATCH
        val nested=d.copy(condition=NotCondition(CreatureDiedThisTurnCondition),whenFalse=null,contextSupport=Verdict.MATCH)
        verdict(nested,StateAnchor.DeathHistory()) shouldBe Verdict.UNKNOWN
        verdict(nested.copy(whenFalse=DrawCardsEffect(2)),StateAnchor.DeathHistory()) shouldBe Verdict.UNKNOWN
    }
    test("export unchanged-pool comparisons when explicitly requested") {
        val dir=System.getenv("STATE_STUDY_EXPORT")?.let(::File) ?: return@test
        dir.mkdirs()
        File(dir,"kenrith-history.json").writeText(reportJson.encodeToString(report))
        for((slug,commander) in listOf("judith" to "Judith, the Scourge Diva","krenko" to "Krenko, Mob Boss")) {
            for(objective in listOf("own-nontoken-creature-deaths","reanimate-own-creatures","create-creature-tokens","stock-own-graveyard",DEATH_HISTORY_OBJECTIVE)) {
                val r=discover(catalog,commander,objective=objective)
                File(dir,"$slug-$objective.json").writeText(reportJson.encodeToString(r))
            }
        }
        File(dir,"kenrith-death.json").writeText(reportJson.encodeToString(discover(catalog,"Kenrith, the Returned King")))
        File(dir,"grave-pact-available.txt").writeText((catalog.card("Grave Pact")!=null).toString())
    }
})
