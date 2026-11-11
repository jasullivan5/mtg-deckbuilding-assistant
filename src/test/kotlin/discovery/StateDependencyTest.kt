package dev.mtg.assistant

import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.*
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.references.Player
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.explorer.stateDependencyExplanation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class StateDependencyTest : FunSpec({
    val catalog by lazy { Catalog() }
    val report by lazy { discover(catalog,"Kenrith, the Returned King",objective=DEATH_HISTORY_OBJECTIVE) }
    fun extract(name: String) = PathwayExtractor(inspectStateDependencies=true).extract(listOf(catalog.card(name)!!))
    fun verdict(d: StateDependency, a: StateAnchor) = all(stateDependencyDecisions(d,a).map { it.verdict })

    test("Gravelighter exposes a single dependency with draw and sacrifice evidence on opposite branches") {
        val x = extract("Gravelighter")
        val d = x.stateDependencies.single()
        val row = report.results.single { it.card == d.card }
        row.verdict shouldBe Verdict.MATCH
        row.stateDependency shouldBe d
        d.checkpoint shouldBe StateCheckpoint.RESOLUTION
        d.condition shouldBe CreatureDiedThisTurnCondition
        d.whenTrue shouldBe DrawCardsEffect(1)
        (d.whenFalse is ForEachEffect) shouldBe true
        eventIntersection(checkNotNull(d.incomingEvent), ZoneEventRequirement(to = Zone.BATTLEFIELD)) shouldBe Verdict.MATCH
        eventIntersection(checkNotNull(d.incomingEvent), ZoneEventRequirement(from = Zone.BATTLEFIELD, to = Zone.GRAVEYARD)) shouldBe Verdict.NO_MATCH
        val draw = x.eventOutcomes.single { it.id in d.trueLinks.eventOutcomeIds }
        draw.path shouldBe d.outcomePath
        d.falseLinks.eventOutcomeIds.isEmpty() shouldBe true
        val sacrifice = x.facts.single { it.id in d.falseLinks.factIds }
        sacrifice.actionId shouldBe d.actionId
        sacrifice.path shouldBe "${d.otherwisePath}.body"
        sacrifice.kind shouldBe FactKind.SACRIFICE
        sacrifice.sourceZone shouldBe Zone.BATTLEFIELD
        sacrifice.sourcePlayer shouldBe "ANY"
        sacrifice.compatible(FactConstraints(kind = FactKind.SACRIFICE, creature = true,
            token = TokenStatus.NONTOKEN, from = Zone.BATTLEFIELD, sourceController = Controller.YOU)) shouldBe Verdict.MATCH
        stateDependencyExplanation(row, d).outcome.lowercase().contains("each player sacrifices a creature") shouldBe true
        val eventRows = discover(catalog,"Judith, the Scourge Diva", objective="own-nontoken-creature-deaths").results
        eventRows.any { it.card == d.card && it.abilityPath == sacrifice.path && it.verdict == Verdict.MATCH } shouldBe false
    }

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
        val explanation=stateDependencyExplanation(r,d)
        explanation.summary.contains("separate from the ability's trigger") shouldBe true
        explanation.requirements.any { it.startsWith("Incoming trigger:") && it.contains("End Step") } shouldBe true
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
        verdict(d,six) shouldBe Verdict.MATCH
        val copy = x.facts.single { it.id in d.trueLinks.factIds }
        copy.tokenCreation?.copySource shouldBe "Self copiable values"
        x.eventOutcomes.single { it.id in d.trueLinks.eventOutcomeIds }.confidence shouldBe Verdict.UNKNOWN
        verdict(d,six.copy(count=5)) shouldBe Verdict.NO_MATCH
        verdict(d,six.copy(player=Player.EachOpponent)) shouldBe Verdict.NO_MATCH
        verdict(d,six.copy(filter=GameObjectFilter.Creature)) shouldBe Verdict.UNKNOWN
        val synthetic=card("Threshold test fixture") {
            typeLine="Enchantment"; manaCost="{G}"
            triggeredAbility {
                trigger=Triggers.a(GameObjectFilter.Land.youControl()).enters()
                effect=Effects.If(d.condition,LoseLifeEffect(2, EffectTarget.Controller),DrawCardsEffect(1))
            }
        }
        val test=PathwayExtractor(inspectStateDependencies=true).extract(listOf(synthetic)).stateDependencies.single()
        test.incomingEvent?.trigger shouldBe d.incomingEvent?.trigger
        test.incomingEvent?.confidence shouldBe Verdict.MATCH
        verdict(test,six) shouldBe Verdict.MATCH
        verdict(test,six.copy(count=5)) shouldBe Verdict.NO_MATCH
        verdict(test,StateAnchor.DeathHistory()) shouldBe Verdict.NO_MATCH
    }
    test("true history can suppress an operation and changed recipients remain distinct") {
        val synthetic = card("Suppression test fixture") {
            typeLine = "Sorcery"; manaCost = "{B}"
            spell {
                effect = Effects.If(CreatureDiedThisTurnCondition,
                    LoseLifeEffect(0, EffectTarget.Controller), LoseLifeEffect(2, EffectTarget.Controller))
            }
        }
        val d = PathwayExtractor(inspectStateDependencies = true).extract(listOf(synthetic)).stateDependencies.single()
        verdict(d, StateAnchor.DeathHistory()) shouldBe Verdict.MATCH
        d.whenTrue shouldBe LoseLifeEffect(0, EffectTarget.Controller)
        d.whenFalse shouldBe LoseLifeEffect(2, EffectTarget.Controller)
        conditionalEffectChange(DrawCardsEffect(1), DrawCardsEffect(2)) shouldBe Verdict.MATCH
        conditionalEffectChange(LoseLifeEffect(2, EffectTarget.Controller), LoseLifeEffect(2)) shouldBe Verdict.MATCH
        conditionalEffectChange(LoseLifeEffect(0, EffectTarget.Controller), null) shouldBe Verdict.NO_MATCH
    }
    test("equal effects, nested history and unrelated sibling effects do not manufacture a benefit") {
        val synthetic=card("Branch isolation test fixture") {
            typeLine="Sorcery"; manaCost="{B}"
            spell { effect=Effects.If(CreatureDiedThisTurnCondition,DrawCardsEffect(1),DrawCardsEffect(1)) then DrawCardsEffect(3) }
        }
        val d=PathwayExtractor(inspectStateDependencies=true).extract(listOf(synthetic)).stateDependencies.single()
        d.whenTrue shouldBe DrawCardsEffect(1)
        verdict(d,StateAnchor.DeathHistory()) shouldBe Verdict.NO_MATCH
        val nested=d.copy(condition=NotCondition(CreatureDiedThisTurnCondition),contextSupport=Verdict.MATCH,
            whenFalse=null,causalChange=conditionalEffectChange(d.whenTrue,null))
        verdict(nested,StateAnchor.DeathHistory()) shouldBe Verdict.UNKNOWN
        val compound = nested.copy(condition = AllConditions(listOf(CreatureDiedThisTurnCondition, IsYourTurn)))
        verdict(compound,StateAnchor.DeathHistory()) shouldBe Verdict.UNKNOWN
    }
})
