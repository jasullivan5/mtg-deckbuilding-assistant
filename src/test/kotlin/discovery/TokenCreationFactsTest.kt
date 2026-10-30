package dev.mtg.assistant

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.targets.*
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Explicit synthetic native fixtures, not evidence of authored-card coverage. */
class TokenCreationFactsTest : FunSpec({
    fun creature(n: Int=1,receiver: EffectTarget?=null)=CreateTokenEffect(DynamicAmount.Fixed(n),1,1,emptySet(),setOf("Goblin"),controller=receiver)
    fun fixture(e: Effect)=card("Synthetic token action") { typeLine="Sorcery";spell { effect=e } }
    fun extract(e: Effect)=PathwayExtractor().extract(listOf(fixture(e)))
    fun rows(e: Effect)=tokenCreationFacts(extract(e).facts).execute()
    test("creature template is a correlated fact with original action and native source") {
        val x=extract(creature(2));val r=tokenCreationFacts(x.facts).execute().single()
        r.verdict shouldBe Verdict.MATCH;r.fact.tokenCreation!!.quantity shouldBe DynamicAmount.Fixed(2)
        r.fact.actionId shouldBe x.actions.single { it.path=="script.spell" }.id
        r.fact.native.toString().contains("CreateToken") shouldBe true
        r.fact.circumstances!!.actionKind shouldBe "SPELL"
    }
    test("predefined Treasure is a noncreature fourth-objective composition") {
        val x=extract(CreatePredefinedTokenEffect("Treasure",2))
        tokenCreationFacts(x.facts).execute().single().verdict shouldBe Verdict.NO_MATCH
        tokenCreationFacts(x.facts,creature=false).execute().single().verdict shouldBe Verdict.MATCH
    }
    test("unknown native predefined template remains visible and unknown") {
        val x=extract(CreatePredefinedTokenEffect("Synthetic unregistered template"))
        tokenCreationFacts(x.facts).execute().single().verdict shouldBe Verdict.UNKNOWN
        x.unsupported.isNotEmpty() shouldBe true
    }
    test("opposing receiver does not qualify") { rows(creature(receiver=EffectTarget.PlayerRef(Player.AnOpponent))).single().verdict shouldBe Verdict.NO_MATCH }
    test("zero quantity does not qualify") { rows(creature(0)).single().verdict shouldBe Verdict.NO_MATCH }
    test("negative quantity does not qualify") { rows(creature(-2)).single().verdict shouldBe Verdict.NO_MATCH }
    test("X preserves positive-quantity and resource prerequisite") {
        val r=rows(creature().copy(count=DynamicAmount.XValue)).single()
        r.verdict shouldBe Verdict.MATCH;r.fact.prerequisites.any { it.contains("strictly positive") } shouldBe true
    }
    test("zero multiplier cannot create positive quantity") { rows(creature().copy(count=DynamicAmount.Multiply(DynamicAmount.XValue,0))).single().verdict shouldBe Verdict.NO_MATCH }
    test("unbound copy target does not become certain even with a creature override") {
        rows(CreateTokenCopyOfTargetEffect(EffectTarget.ContextTarget(9),overrideCardTypes=setOf(CardType.CREATURE))).single().verdict shouldBe Verdict.UNKNOWN
    }
    test("source copy uses native exceptions type replacement precedence") {
        val c=card("Synthetic creature copy") { typeLine="Creature — Beast";power=2;toughness=2;spell { effect=CreateTokenCopyOfSourceEffect(exceptions=CopyExceptions(addedCardTypes=setOf(CardType.CREATURE),overrideCardTypes=setOf(CardType.ARTIFACT))) } }
        val fs=PathwayExtractor().extract(listOf(c)).facts
        tokenCreationFacts(fs).execute().single().verdict shouldBe Verdict.NO_MATCH
        tokenCreationFacts(fs,false).execute().single().verdict shouldBe Verdict.MATCH
    }
    test("batch keeps one event and independent template groups") {
        val x=extract(CreateTokenBatchEffect(listOf(creature(2),CreatePredefinedTokenEffect("Treasure",1))))
        val rs=tokenCreationFacts(x.facts).execute()
        rs.map { it.verdict } shouldBe listOf(Verdict.MATCH,Verdict.NO_MATCH)
        rs.map { it.fact.tokenCreation!!.creationEvent }.distinct().size shouldBe 1
    }
    test("batch shared opposing controller excludes all creature groups") {
        rows(CreateTokenBatchEffect(listOf(creature()),EffectTarget.PlayerRef(Player.AnOpponent))).single().verdict shouldBe Verdict.NO_MATCH
    }
    test("correlated composite cannot combine enemy creature with our noncreature token") {
        val rs=rows(CompositeEffect(listOf(creature(receiver=EffectTarget.PlayerRef(Player.AnOpponent)),CreatePredefinedTokenEffect("Treasure"))))
        rs.all { it.verdict==Verdict.NO_MATCH } shouldBe true
    }
    test("multiple token effects remain distinguishable") {
        val rs=rows(CompositeEffect(listOf(creature(1),creature(3))))
        rs.size shouldBe 2;rs.map { it.fact.id }.distinct().size shouldBe 2
        rs.map { it.fact.actionId }.distinct().size shouldBe 1
    }
    test("movement is not creation") {
        rows(MoveToZoneEffect(EffectTarget.Self,Zone.BATTLEFIELD,fromZone=Zone.GRAVEYARD)).isEmpty() shouldBe true
    }
    test("one composite movement and creation supports B and C with identical action") {
        val c=card("Synthetic return and create") { typeLine="Sorcery";spell { val t=target(TargetFilter.CreatureInYourGraveyard);effect=CompositeEffect(listOf(MoveToZoneEffect(t,Zone.BATTLEFIELD,fromZone=Zone.GRAVEYARD),creature())) } }
        val fs=PathwayExtractor().extract(listOf(c)).facts
        val b=reanimationFacts(fs).execute().single();val cc=tokenCreationFacts(fs).execute().single()
        b.verdict shouldBe Verdict.MATCH;cc.verdict shouldBe Verdict.MATCH;b.fact.actionId shouldBe cc.fact.actionId
    }
    test("generic condition preserves branch uncertainty and never merges incompatible outcomes") {
        val gated=GatedEffect(Gate.WhenCondition(com.wingedsheep.sdk.scripting.conditions.CreatureDiedThisTurnCondition),
            creature(receiver=EffectTarget.PlayerRef(Player.AnOpponent)),CreatePredefinedTokenEffect("Treasure"))
        rows(gated).all { it.verdict==Verdict.NO_MATCH } shouldBe true
    }
    test("conditional creation remains unknown with explicit branch prerequisites") {
        val r=rows(GatedEffect(Gate.WhenCondition(com.wingedsheep.sdk.scripting.conditions.CreatureDiedThisTurnCondition),creature())).single()
        r.verdict shouldBe Verdict.UNKNOWN;r.fact.prerequisites.any { it.contains("Gate") } shouldBe true
    }
    test("optional choice retains circumstances and does not imply mandatory generation") {
        val r=rows(GatedEffect(Gate.MayDecide(),creature())).single()
        r.fact.circumstances!!.mandatory shouldBe Verdict.NO_MATCH
    }
    test("copy of unconstrained permanent remains unknown rather than inferring creature by name") {
        val c=card("Synthetic copy") { typeLine="Sorcery";spell { val t=target(TargetFilter.Permanent);effect=CreateTokenCopyOfTargetEffect(t) } }
        tokenCreationFacts(PathwayExtractor().extract(listOf(c)).facts).execute().single().verdict shouldBe Verdict.UNKNOWN
    }
    test("copy override does not erase unresolved or contradictory selection prerequisites") {
        val c=card("Synthetic contradictory copy") { typeLine="Sorcery";spell {
            val t=target(TargetFilter(GameObjectFilter(cardPredicates=listOf(com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature,com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsNoncreature))))
            effect=CreateTokenCopyOfTargetEffect(t,overrideCardTypes=setOf(CardType.CREATURE))
        } }
        tokenCreationFacts(PathwayExtractor().extract(listOf(c)).facts).execute().single().verdict shouldBe Verdict.UNKNOWN
    }
    test("contradictory creature requirements cannot be overwritten into a match") {
        FactQuery(extract(creature()).facts).where(createdObjectIsCreature()).where(createdObjectIsCreature(false)).execute().single().verdict shouldBe Verdict.NO_MATCH
    }
})
