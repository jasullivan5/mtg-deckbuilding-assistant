package dev.mtg.assistant

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.references.Player
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DeathCollectionFactsTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun extract(effect: Effect)=PathwayExtractor().extract(listOf(card("Synthetic pipeline") { typeLine="Creature";power=2;toughness=2;spell { this.effect=effect } }))
    fun query(e: PathwayExtractor)=creatureDeathPathways(e.pathways).where(canAffectCreature()).where(canAffectController(Controller.YOU)).where(canAffectTokenStatus(TokenStatus.NONTOKEN)).where(satisfiesDeathGenerationPolicy()).execute()
    test("raw nonempty guard traverses nested death while retaining threshold") {
        val e=extract(CompositeEffect(listOf(GatherCardsEffect(CardSource.ControlledPermanents(Player.You,GameObjectFilter.Creature),"c"),ConditionalOnCollectionEffect("c",SacrificeEffect(GameObjectFilter.Creature)))))
        query(e).single().verdict shouldBe Verdict.MATCH
        e.pathways.single().prerequisites.any { it.contains("threshold=1") } shouldBe true
        e.structuralSupport.any { it.status==SupportStatus.SUPPORTED && it.nativeType=="ConditionalOnCollectionEffect" } shouldBe true
    }
    test("known zero-size selection prunes impossible nonempty sacrifice branch") {
        val e=extract(CompositeEffect(listOf(GatherCardsEffect(CardSource.ControlledPermanents(),"a"),SelectFromCollectionEffect("a",SelectionMode.ChooseUpTo(com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed(0)),storeSelected="b"),ConditionalOnCollectionEffect("b",SacrificeEffect(GameObjectFilter.Creature),Effects.DrawCards(1)))))
        e.pathways.isEmpty() shouldBe true
    }
    test("unbound and distinct-type collection guards keep child death UNKNOWN") {
        for(effect in listOf(ConditionalOnCollectionEffect("missing",SacrificeEffect(GameObjectFilter.Creature)),CompositeEffect(listOf(GatherCardsEffect(CardSource.ControlledPermanents(),"c"),ConditionalOnCollectionEffect("c",SacrificeEffect(GameObjectFilter.Creature),countDistinctCardTypes=true))))) {
            val e=extract(effect)
            query(e).single().verdict shouldBe Verdict.UNKNOWN
            e.structuralSupport.any { it.status==SupportStatus.PARTIALLY_SUPPORTED } shouldBe true
        }
    }
    test("real conditional traversal exposes held-out Breach but does not turn graveyard exiling into death") {
        val e=PathwayExtractor().extract(listOf(catalog.card("Through the Breach")!!))
        e.pathways.any { it.path.contains("ifNotEmpty") && it.operation=="SACRIFICE" } shouldBe true
        PathwayExtractor().extract(listOf(catalog.card("Diregraf Scavenger")!!)).pathways.none { it.branch==Contribution.DIRECT_DEATH_GENERATOR } shouldBe true
    }
    test("actual hand-to-battlefield binding transfers to Arms Race and held-out Breach") {
        for(name in listOf("Arms Race","Through the Breach","Cauldron Dance")) {
            val e=PathwayExtractor().extract(listOf(catalog.card(name)!!))
            val p=query(e).single { it.pathway.operation=="SACRIFICE" }
            p.verdict shouldBe Verdict.MATCH
            p.pathway.circumstances!!.delayedTrigger shouldBe true
            p.pathway.prerequisites.any { it.contains("same game object") } shouldBe true
        }
    }
    test("held-out Elemental restriction remains UNKNOWN instead of dropping the subtype") {
        val e=PathwayExtractor().extract(listOf(catalog.card("Incandescent Soulstoke")!!))
        query(e).single { it.pathway.operation=="SACRIFICE" }.verdict shouldBe Verdict.UNKNOWN
        e.pathways.single { it.operation=="SACRIFICE" }.scope.filter.description.contains("Elemental") shouldBe true
    }
    fun pipeline(zone: Zone,destination: Zone,filter: GameObjectFilter,player: Player=Player.You,moveType: MoveType=MoveType.Default): Effect = CompositeEffect(listOf(
        GatherCardsEffect(CardSource.FromZone(zone,player,filter),"source"),
        MoveCollectionEffect("source",CardDestination.ToZone(destination,player),moveType=moveType)))
    test("default collection death requires battlefield origin and graveyard destination") {
        val e=extract(pipeline(Zone.BATTLEFIELD,Zone.GRAVEYARD,GameObjectFilter.Creature))
        query(e).single().decisions.take(3).all { it.verdict==Verdict.MATCH } shouldBe true
        // Generic movement removal remains policy-excluded, like choosing a friendly removal target.
        query(e).single().decisions.last().verdict shouldBe Verdict.NO_MATCH
        query(extract(pipeline(Zone.GRAVEYARD,Zone.GRAVEYARD,GameObjectFilter.Creature))).single().verdict shouldBe Verdict.NO_MATCH
        extract(pipeline(Zone.GRAVEYARD,Zone.HAND,GameObjectFilter.Creature)).pathways.isEmpty() shouldBe true
        extract(pipeline(Zone.BATTLEFIELD,Zone.EXILE,GameObjectFilter.Creature)).pathways.isEmpty() shouldBe true
    }
    test("collection sacrifice preserves opponent token and noncreature exclusions") {
        val opponent=query(extract(pipeline(Zone.BATTLEFIELD,Zone.GRAVEYARD,GameObjectFilter.Creature,Player.EachOpponent,MoveType.Sacrifice))).single()
        opponent.decisions[1].verdict shouldBe Verdict.NO_MATCH
        val token=query(extract(pipeline(Zone.BATTLEFIELD,Zone.GRAVEYARD,GameObjectFilter.Creature.token(),moveType=MoveType.Sacrifice))).single()
        token.decisions[2].verdict shouldBe Verdict.NO_MATCH
        val non=query(extract(pipeline(Zone.BATTLEFIELD,Zone.GRAVEYARD,GameObjectFilter.Any.notCreature(),moveType=MoveType.Sacrifice))).single()
        non.decisions[0].verdict shouldBe Verdict.NO_MATCH
    }
    test("filtered move and unbound indexed references retain partial uncertainty") {
        val effect=CompositeEffect(listOf(GatherCardsEffect(CardSource.FromZone(Zone.HAND,Player.You,GameObjectFilter.Any),"a"),MoveCollectionEffect("a",CardDestination.ToZone(Zone.BATTLEFIELD),filter=GameObjectFilter.Creature),SacrificeTargetEffect(com.wingedsheep.sdk.scripting.targets.EffectTarget.PipelineTarget("a"))))
        val e=extract(effect)
        query(e).single().verdict shouldBe Verdict.UNKNOWN
        val unbound=extract(SacrificeTargetEffect(com.wingedsheep.sdk.scripting.targets.EffectTarget.PipelineTarget("absent")))
        query(unbound).single().verdict shouldBe Verdict.UNKNOWN
    }
    test("empty and out-of-range indexed selections cannot sacrifice a creature") {
        val effect=CompositeEffect(listOf(GatherCardsEffect(CardSource.ControlledPermanents(),"a"),SelectFromCollectionEffect("a",SelectionMode.ChooseUpTo(com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed(0)),storeSelected="b"),SacrificeTargetEffect(com.wingedsheep.sdk.scripting.targets.EffectTarget.PipelineTarget("b"))))
        query(extract(effect)).single().decisions[0].verdict shouldBe Verdict.NO_MATCH
    }
    test("unknown collection guard also gates death-enhanced removal confidence") {
        val c=card("Synthetic guarded dependency") { typeLine="Enchantment"
            activatedAbility { cost=Costs.Free
                val target=target(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.Creature)
                effect=ConditionalOnCollectionEffect("unbound",Effects.If(com.wingedsheep.sdk.scripting.conditions.CreatureDiedThisTurnCondition,Effects.Destroy(target)))
            }
        }
        val e=PathwayExtractor().extract(listOf(c))
        e.pathways.single { it.branch==Contribution.DEATH_ENHANCED_REMOVAL }.dependencyVerdict shouldBe Verdict.UNKNOWN
    }
    test("own-compatible target choice is not downgraded merely because other controllers are possible") {
        val e=extract(SacrificeTargetEffect(com.wingedsheep.sdk.scripting.targets.EffectTarget.FilteredTarget(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.Creature)))
        query(e).single().decisions.take(3).all { it.verdict==Verdict.MATCH } shouldBe true
        val opposite=com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.Creature.copy(baseFilter=GameObjectFilter.Creature.copy(controllerPredicate=com.wingedsheep.sdk.scripting.predicates.ControllerPredicate.ControlledByOpponent))
        query(extract(SacrificeTargetEffect(com.wingedsheep.sdk.scripting.targets.EffectTarget.FilteredTarget(opposite)))).single().decisions[1].verdict shouldBe Verdict.NO_MATCH
    }
    test("arrival control differs from the source collection controller restriction") {
        val oldFilter=GameObjectFilter.Creature.copy(controllerPredicate=com.wingedsheep.sdk.scripting.predicates.ControllerPredicate.ControlledByOpponent)
        val e=extract(CompositeEffect(listOf(GatherCardsEffect(CardSource.FromZone(Zone.GRAVEYARD,Player.EachOpponent,oldFilter),"a"),MoveCollectionEffect("a",CardDestination.ToZone(Zone.BATTLEFIELD,Player.You)),SacrificeTargetEffect(com.wingedsheep.sdk.scripting.targets.EffectTarget.PipelineTarget("a")))))
        query(e).single().verdict shouldBe Verdict.MATCH
        e.pathways.single().scope.details.any { it.contains("ControlledByOpponent") } shouldBe true
    }
})
