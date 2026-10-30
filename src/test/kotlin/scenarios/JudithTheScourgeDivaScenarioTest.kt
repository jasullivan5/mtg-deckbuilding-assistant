package dev.mtg.assistant

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.model.EntityId
import dev.mtg.assistant.extensions.JudithTheScourgeDiva
import io.kotest.matchers.shouldBe

/** Engine scenarios use the actual extension. Slay and Sweep are identified synthetic test tools. */
class JudithTheScourgeDivaScenarioTest : ScenarioTestBase() {
    init {
        cardRegistry.register(listOf(JudithTheScourgeDiva,
            card("Judith Test Slay") { manaCost="{0}"; typeLine="Sorcery"; spell { val c=target(TargetFilter.Creature); effect=Effects.Destroy(c) } },
            card("Judith Test Sweep") { manaCost="{0}"; typeLine="Sorcery"; spell {
                effect=Effects.DestroyAll(GameObjectFilter.Creature)
            } }))
        fun TestGame.finish(target: EntityId): Int {
            var choices=0
            repeat(40) {
                when(val d=state.pendingDecision) {
                    is ChooseTargetsDecision -> { selectTargets(listOf(target)).error shouldBe null; choices++ }
                    is OrderTriggersDecision -> submitDecision(TriggerOrderResponse(d.id,d.triggers.indices.toList())).error shouldBe null
                    null -> if(state.stack.isNotEmpty()) { val results=resolveStack(); results.forEach { it.error shouldBe null } } else return choices
                    else -> error("Unexpected decision $d")
                }
            }
            error("Scenario did not settle")
        }
        test("static anthem applies to our other creatures only") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,JudithTheScourgeDiva.name)
                .withCardOnBattlefield(1,"Aegis Turtle").withCardOnBattlefield(2,"Aegis Turtle").build()
            val own=g.state.getBattlefield(g.player1Id).first { g.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Aegis Turtle" };val other=g.state.getBattlefield(g.player2Id).first()
            val judith=g.findPermanent(JudithTheScourgeDiva.name)!!
            val p=g.state.projectedState
            p.getPower(own) shouldBe 1
            p.getToughness(own) shouldBe 5
            p.getPower(other) shouldBe 0
            p.getPower(judith) shouldBe 2
        }
        for(kind in listOf("own","token","opponent","self","self-token")) test("$kind death produces exactly the proper trigger and player damage") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,JudithTheScourgeDiva.name)
                .withCardOnBattlefield(if(kind == "opponent") 2 else 1,"Aegis Turtle")
                .withCardInHand(1,"Judith Test Slay").build()
            val victim=if(kind.startsWith("self")) g.findPermanent(JudithTheScourgeDiva.name)!! else g.findPermanent("Aegis Turtle")!!
            if(kind in listOf("token","self-token")) g.state=g.state.updateEntity(victim) { it.with(TokenComponent) }
            g.castSpell(1,"Judith Test Slay",targetId=victim).error shouldBe null
            val expected=if(kind in listOf("own","self")) 1 else 0
            g.finish(g.player2Id) shouldBe expected
            g.getLifeTotal(2) shouldBe 20-expected
            if(kind in listOf("token","self-token")) g.state.getEntity(victim) shouldBe null
            else g.isInGraveyard(if(kind == "opponent") 2 else 1,if(kind.startsWith("self")) JudithTheScourgeDiva.name else "Aegis Turtle") shouldBe true
        }
        test("Judith and two own nontokens die simultaneously: three triggers using last known information") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,JudithTheScourgeDiva.name)
                .withCardOnBattlefield(1,"Aegis Turtle").withCardOnBattlefield(1,"Bear Cub")
                .withCardOnBattlefield(2,"Aegis Turtle").withCardInHand(1,"Judith Test Sweep").build()
            g.castSpell(1,"Judith Test Sweep").error shouldBe null
            g.finish(g.player2Id) shouldBe 3
            g.getLifeTotal(2) shouldBe 17
        }
        test("damage may target a creature and resolves as one actual damage") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,JudithTheScourgeDiva.name)
                .withCardOnBattlefield(1,"Aegis Turtle").withCardOnBattlefield(2,"Llanowar Elves")
                .withCardInHand(1,"Judith Test Slay").build()
            val target=g.findPermanent("Llanowar Elves")!!
            g.castSpell(1,"Judith Test Slay",targetId=g.findPermanent("Aegis Turtle")!!).error shouldBe null
            g.finish(target) shouldBe 1
            g.isInGraveyard(2,"Llanowar Elves") shouldBe true
            g.getLifeTotal(2) shouldBe 20
        }
        test("noncreature artifact is not a legal any-target damage selection") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,JudithTheScourgeDiva.name)
                .withCardOnBattlefield(1,"Aegis Turtle").withCardOnBattlefield(2,"Sol Ring")
                .withCardInHand(1,"Judith Test Slay").build()
            g.castSpell(1,"Judith Test Slay",targetId=g.findPermanent("Aegis Turtle")!!).error shouldBe null
            g.resolveStack().forEach { it.error shouldBe null }
            (g.selectTargets(listOf(g.findPermanent("Sol Ring")!!)).error != null) shouldBe true
            g.finish(g.player2Id) shouldBe 1
            g.getLifeTotal(2) shouldBe 19
        }
        test("damage can target Judith herself without being lethal") {
            val g=scenario().withPlayers().withCardOnBattlefield(1,JudithTheScourgeDiva.name)
                .withCardOnBattlefield(1,"Aegis Turtle").withCardInHand(1,"Judith Test Slay").build()
            val j=g.findPermanent(JudithTheScourgeDiva.name)!!
            g.castSpell(1,"Judith Test Slay",targetId=g.findPermanent("Aegis Turtle")!!).error shouldBe null
            g.finish(j) shouldBe 1 // one damage is not lethal to Judith's 2 toughness
            g.isOnBattlefield(JudithTheScourgeDiva.name) shouldBe true
        }
    }
}
