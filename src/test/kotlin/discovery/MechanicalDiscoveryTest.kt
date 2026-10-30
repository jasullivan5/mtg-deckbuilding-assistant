package dev.mtg.assistant

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.CreatureDiedThisTurnCondition
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.assertions.throwables.shouldThrow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class MechanicalDiscoveryTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun real(name: String): CardDefinition = requireNotNull(catalog.card(name)) { "Required real authored fixture unavailable: $name" }
    fun extract(name: String) = PathwayExtractor().extract(listOf(real(name)))
    fun evaluate(card: CardDefinition) = creatureDeathPathways(PathwayExtractor().extract(listOf(card)).pathways)
        .where(canAffectCreature()).where(canAffectController(Controller.YOU))
        .where(canAffectTokenStatus(TokenStatus.NONTOKEN)).where(satisfiesDeathGenerationPolicy()).execute()
    fun direct(name: String) = evaluate(real(name)).filter { it.verdict == Verdict.MATCH }

    test("all named integration fixtures actually exist at pinned revision") {
        listOf("Goblin Bombardment","Village Rites","Bone Splinters","Viscera Seer","Phyrexian Tower",
            "Victimize","Murder","Tragic Slip","Fleshbag Marauder","Blood Artist","Gravecrawler").forEach { real(it).name shouldBe it }
    }
    test("format legality is independent of serving as commander and unknown is fail closed") {
        commanderFormatLegality(real("Murder")) shouldBe Verdict.MATCH
        // Explicit synthetic copies isolate legality states; no authored mechanics are substituted.
        commanderFormatLegality(real("Murder").copy(legalFormats=emptySet())) shouldBe Verdict.UNKNOWN
        commanderFormatLegality(real("Murder").copy(legalFormats=setOf(DeckFormat.MODERN))) shouldBe Verdict.NO_MATCH
    }
    test("real mono multi and colorless commanders resolve their complete identities") {
        fullIdentity(resolveCommander(catalog,"Krenko, Mob Boss")) shouldBe setOf(Color.RED)
        fullIdentity(resolveCommander(catalog,"Judith, Carnage Connoisseur")) shouldBe setOf(Color.BLACK,Color.RED)
        fullIdentity(resolveCommander(catalog,"Kozilek, the Broken Reality")) shouldBe emptySet()
    }
    test("hybrid requires both colors while colorless candidates fit a mono commander") {
        val recruit=real("Boros Recruit")
        fullIdentity(recruit) shouldBe setOf(Color.WHITE,Color.RED)
        selectedCommanderEligibility(recruit,setOf(Color.RED)) shouldBe Verdict.NO_MATCH
        selectedCommanderEligibility(recruit,setOf(Color.RED,Color.WHITE)) shouldBe Verdict.MATCH
        selectedCommanderEligibility(real("Sol Ring"),setOf(Color.RED)) shouldBe Verdict.MATCH
        selectedCommanderEligibility(real("Murder"),setOf(Color.RED)) shouldBe Verdict.NO_MATCH
        selectedCommanderEligibility(real("Murder").copy(colorIdentityOverride=null),setOf(Color.RED)) shouldBe Verdict.UNKNOWN
    }
    test("invalid commanders and face aliases fail clearly") {
        shouldThrow<IllegalArgumentException> { resolveCommander(catalog,"Murder") }.message!! shouldContain "Not a supported commander"
        shouldThrow<IllegalArgumentException> { resolveCommander(catalog,"No Such Commander") }
        val front=catalog.cards.firstOrNull { it.backFace != null }
        if(front != null) shouldThrow<IllegalArgumentException> { resolveCommander(catalog,front.backFace!!.name) }
    }
    for(name in listOf("Goblin Bombardment","Viscera Seer","Phyrexian Tower")) test("$name retains its activation sacrifice and associated action") {
        val paths=direct(name)
        paths.any { it.pathway.origin == "ACTIVATION_COST" && it.pathway.operation == "SACRIFICE" } shouldBe true
        val extracted=extract(name)
        paths.forEach { p -> extracted.actions.any { it.id == p.pathway.actionId } shouldBe true }
        if(name == "Phyrexian Tower") {
            paths.size shouldBe 1
            paths.single().pathway.path shouldContain "activatedAbilities[1]"
        }
    }
    for(name in listOf("Village Rites","Bone Splinters")) test("$name retains its additional casting sacrifice") {
        val path=direct(name).single()
        path.pathway.origin shouldBe "CASTING_COST"
        path.pathway.path shouldContain "additionalCosts[0]"
        val action=extract(name).actions.first { it.id == path.pathway.actionId }
        action.native.toString() shouldContain if(name == "Bone Splinters") "byDestruction" else "DrawCards"
    }
    test("Victimize resolution sacrifice retains success gate and exact target prerequisites") {
        val path=direct("Victimize").single().pathway
        path.origin shouldBe "EFFECT"
        path.path shouldContain "gate.action"
        path.prerequisites.joinToString() shouldContain "PermanentsSacrificed"
        val native=extract("Victimize").actions.first { it.id == path.actionId }.native.toString()
        native shouldContain "targetRequirements"
        (real("Victimize").script.targetRequirements.single() as TargetObject).filter.zone shouldBe Zone.GRAVEYARD
        real("Victimize").script.targetRequirements.single().count shouldBe 2
    }
    test("Murder can mechanically affect our nontoken creatures but is rejected by policy") {
        val p=evaluate(real("Murder")).single()
        p.decisions.take(3).map { it.verdict } shouldBe List(3) { Verdict.MATCH }
        p.decisions.last().verdict shouldBe Verdict.NO_MATCH
        p.decisions.last().basis shouldBe "POLICY"
        p.pathway.operation shouldBe "DESTROY"
    }
    test("Tragic Slip enhances removal through morbid but is not a direct generator") {
        direct("Tragic Slip").size shouldBe 0
        val p=extract("Tragic Slip").pathways.single { it.branch == Contribution.DEATH_ENHANCED_REMOVAL }
        p.dependencyVerdict shouldBe Verdict.MATCH
        p.operation shouldBe "TOUGHNESS_REDUCTION"
        p.dependency!! shouldContain "CreatureDiedThisTurn"
        p.scope.actor shouldBe "ANY" // Generic morbid was not rewritten as requiring our creatures.
        p.path shouldContain "gate.condition"
        p.native.toString() shouldContain "-13"
    }
    test("symmetrical Fleshbag sacrifice is a policy rejection not a controller rejection") {
        val p=evaluate(real("Fleshbag Marauder")).single()
        p.decisions.take(3).map { it.verdict } shouldBe List(3) { Verdict.MATCH }
        p.decisions.last().verdict shouldBe Verdict.NO_MATCH
        p.decisions.last().reason shouldContain "symmetrical"
    }
    for(name in listOf("Blood Artist","Gravecrawler")) test("$name is not a direct generator or death-enhanced creature removal") {
        direct(name).size shouldBe 0
        extract(name).pathways.filter { it.branch == Contribution.DEATH_ENHANCED_REMOVAL && it.dependencyVerdict == Verdict.MATCH }.size shouldBe 0
    }
    test("generic predicates reject opponent and token only filters") {
        ObjectScope(GameObjectFilter.Creature, "OPPONENT").compatible(Requirements(controller=Controller.YOU)) shouldBe Verdict.NO_MATCH
        val tokens=GameObjectFilter(cardPredicates=listOf(CardPredicate.IsCreature,CardPredicate.IsToken))
        ObjectScope(tokens,"YOU").compatible(Requirements(token=TokenStatus.NONTOKEN)) shouldBe Verdict.NO_MATCH
        ObjectScope(GameObjectFilter.Creature,"YOU").compatible(Requirements(token=TokenStatus.NONTOKEN)) shouldBe Verdict.MATCH
    }
    test("query conjunction preserves correlated OR scope instead of matching unrelated alternatives") {
        val ownTokens=GameObjectFilter(listOf(CardPredicate.IsCreature,CardPredicate.IsToken),controllerPredicate=ControllerPredicate.ControlledByYou)
        val opposingCards=GameObjectFilter(listOf(CardPredicate.IsCreature,CardPredicate.IsNontoken),controllerPredicate=ControllerPredicate.ControlledByOpponent)
        val union=GameObjectFilter(anyOf=listOf(ownTokens,opposingCards))
        val seed=extract("Goblin Bombardment").pathways.single { it.origin == "ACTIVATION_COST" }
        val synthetic=seed.copy(scope=ObjectScope(union)) // Isolated scope fixture, not a catalog claim.
        val result=MechanicalQuery(listOf(synthetic)).where(canAffectController(Controller.YOU))
            .where(canAffectTokenStatus(TokenStatus.NONTOKEN)).execute().single()
        result.decisions.first().verdict shouldBe Verdict.MATCH
        result.decisions.last().verdict shouldBe Verdict.NO_MATCH
    }
    test("unsupported scope retains UNKNOWN with independently inspectable decisions") {
        val seed=extract("Goblin Bombardment").pathways.first()
        val result=MechanicalQuery(listOf(seed.copy(scope=ObjectScope(unresolved="Synthetic unresolved selector"))))
            .where(canAffectController(Controller.YOU)).execute().single()
        result.verdict shouldBe Verdict.UNKNOWN
    }
    test("synthetic nested composite and condition keep paths and prerequisites") {
        val base=real("Viscera Seer")
        val effect=CompositeEffect(listOf(Effects.DrawCards(1),Effects.If(CreatureDiedThisTurnCondition,Effects.SacrificeOwn(GameObjectFilter.Creature))))
        val fixture=base.copy(name="Synthetic nested test",script=CardScript(spellEffect=effect))
        // Phase 2.2: non-removal sacrifice remains a capability without requiring a beneficial payoff.
        val paths=evaluate(fixture).filter { it.decisions.take(3).all { d -> d.verdict == Verdict.MATCH } }
        paths.single().verdict shouldBe Verdict.MATCH
        paths.single().pathway.path shouldContain "effects[1].then"
        paths.single().pathway.prerequisites.joinToString() shouldContain "WhenCondition"
    }
    test("synthetic independent abilities cannot donate sacrifice costs to another action") {
        val base=real("Goblin Bombardment")
        val sac=base.script.activatedAbilities.single()
        val effect=real("Murder").script.spellEffect!!
        val fixture=base.copy(name="Synthetic independent actions",script=CardScript(activatedAbilities=listOf(sac,sac.copy(cost=Costs.Free,effect=effect,targetRequirements=real("Murder").script.targetRequirements))))
        val results=evaluate(fixture)
        results.filter { it.verdict == Verdict.MATCH }.single().pathway.path shouldContain "activatedAbilities[0].cost"
        results.first { it.pathway.path.contains("activatedAbilities[1]") }.decisions.last().verdict shouldBe Verdict.NO_MATCH
    }
    test("synthetic unsupported native effect is retained with action evidence") {
        val fixture=real("Murder").copy(name="Synthetic unsupported effect",script=CardScript(spellEffect=Effects.GainLife(2)))
        val extracted=PathwayExtractor().extract(listOf(fixture))
        extracted.unsupported.isNotEmpty() shouldBe true
        extracted.actions.any { it.native.toString().contains("GainLife") } shouldBe true
    }
    test("unchanged removal in both conditional branches is not a positive enhancement") {
        val murder=real("Murder")
        val effect=murder.script.spellEffect!!
        val fixture=murder.copy(name="Synthetic unchanged branches",script=murder.script.copy(spellEffect=Effects.If(CreatureDiedThisTurnCondition,effect,effect)))
        val p=PathwayExtractor().extract(listOf(fixture)).pathways.single { it.branch == Contribution.DEATH_ENHANCED_REMOVAL }
        p.dependencyVerdict shouldBe Verdict.NO_MATCH
    }
    test("alternative face executable scripts are inspected without granting separate card eligibility") {
        val base=real("Murder")
        val back=real("Viscera Seer")
        val fixture=base.copy(name="Synthetic front",backFace=back)
        val paths=evaluate(fixture).filter { it.verdict == Verdict.MATCH }
        paths.any { it.pathway.face == "front.backFace" } shouldBe true
        physicalCards(listOf(fixture,back)).map { it.name } shouldBe listOf("Synthetic front")
    }
    test("real damage is conditional and does not guarantee or qualify ordinary friendly-target death") {
        val p=evaluate(real("Lightning Bolt")).single()
        p.pathway.operation shouldBe "DAMAGE_POTENTIALLY_LETHAL"
        p.pathway.prerequisites.joinToString() shouldContain "sufficient"
        p.decisions.last().verdict shouldBe Verdict.NO_MATCH
    }
    test("full discovery uses eligible input and produces serializable inspectable evidence") {
        val report=discover(catalog,"Judith, Carnage Connoisseur","name:slip or name:bombardment or name:splinters or name:murder")
        val accepted=report.results.filter { it.verdict == Verdict.MATCH }
        accepted.any { it.card == "Goblin Bombardment" && it.costOrEffect == "ACTIVATION_COST" } shouldBe true
        accepted.any { it.card == "Tragic Slip" && it.branch == Contribution.DEATH_ENHANCED_REMOVAL } shouldBe true
        accepted.none { it.card == "Murder" } shouldBe true
        accepted.all { it.commanderLegality == Verdict.MATCH && it.deckEligibility == Verdict.MATCH } shouldBe true
        accepted.first { it.card == "Goblin Bombardment" }.sourceReferences.joinToString() shouldContain "GoblinBombardment.kt"
        val text=Json.encodeToString(report)
        Json.decodeFromString<DiscoveryReport>(text).counts shouldBe report.counts
        report.counts.getValue("authored_physical_cards") shouldBe physicalCards(catalog.cards).size
    }
    test("synthetic token-only and opponent-only operations are rejected after actual extraction") {
        val base=real("Goblin Bombardment")
        val ability=base.script.activatedAbilities.single()
        val tokenFilter=GameObjectFilter(listOf(CardPredicate.IsCreature,CardPredicate.IsToken))
        val tokenFixture=base.copy(name="Synthetic token sacrifice",script=CardScript(activatedAbilities=listOf(ability.copy(cost=Costs.Sacrifice(tokenFilter)))))
        evaluate(tokenFixture).single { it.pathway.origin == "ACTIVATION_COST" }.decisions[2].verdict shouldBe Verdict.NO_MATCH
        val opponentEffect=com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect(GameObjectFilter.Creature)
        val opponentFixture=base.copy(name="Synthetic opponent edict",script=CardScript(spellEffect=opponentEffect))
        evaluate(opponentFixture).single().decisions[1].verdict shouldBe Verdict.NO_MATCH
    }
    test("synthetic death-gated enabling and resolution-payment removal are separate contributions") {
        val base=real("Murder")
        val removal=base.script.spellEffect!!
        val enabled=base.copy(name="Synthetic death enables removal",script=base.script.copy(spellEffect=Effects.If(CreatureDiedThisTurnCondition,removal)))
        PathwayExtractor().extract(listOf(enabled)).pathways.single { it.branch == Contribution.DEATH_ENHANCED_REMOVAL }.dependencyVerdict shouldBe Verdict.MATCH
        val payment=base.copy(name="Synthetic pay sacrifice",script=base.script.copy(spellEffect=GatedEffect(Gate.MayPay(Effects.SacrificeOwn(GameObjectFilter.Creature)),removal)))
        evaluate(payment).filter { it.verdict == Verdict.MATCH }.single().pathway.origin shouldBe "RESOLUTION_COST"
    }
    test("synthetic unrelated conditional draw does not enhance removal outside that branch") {
        val base=real("Murder")
        val fixture=base.copy(name="Synthetic unrelated outcome",script=base.script.copy(spellEffect=CompositeEffect(listOf(
            Effects.If(CreatureDiedThisTurnCondition,Effects.DrawCards(1)),base.script.spellEffect!!))))
        PathwayExtractor().extract(listOf(fixture)).pathways.none { it.branch == Contribution.DEATH_ENHANCED_REMOVAL } shouldBe true
    }

    test("synthetic death-triggered opposing sacrifice is removal, not damage to a player") {
        val base=real("Blood Artist")
        val ability=base.script.triggeredAbilities.single().copy(effect=com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect(GameObjectFilter.Creature))
        val fixture=base.copy(name="Synthetic death-triggered edict",script=CardScript(triggeredAbilities=listOf(ability)))
        val p=PathwayExtractor().extract(listOf(fixture)).pathways.single { it.branch == Contribution.DEATH_ENHANCED_REMOVAL }
        p.operation shouldBe "SACRIFICE"
        p.dependencyVerdict shouldBe Verdict.MATCH
    }
    test("synthetic self-cast death discount applies only when this action has a removal effect") {
        val base=real("Murder")
        val modifier=com.wingedsheep.sdk.scripting.ModifySpellCost(
            com.wingedsheep.sdk.scripting.SpellCostTarget.SelfCast,
            com.wingedsheep.sdk.scripting.CostModification.ReduceGenericBy(com.wingedsheep.sdk.scripting.CostReductionSource.FixedIfCreatureDiedThisTurn(1)))
        val fixture=base.copy(name="Synthetic discounted removal",script=base.script.copy(staticAbilities=listOf(modifier)))
        PathwayExtractor().extract(listOf(fixture)).pathways.single { it.branch == Contribution.DEATH_ENHANCED_REMOVAL }.dependencyVerdict shouldBe Verdict.MATCH
        // A real death-discounted creature is not a removal action merely for having deathtouch.
        PathwayExtractor().extract(listOf(real("Dreaded Bat-Cloud"))).pathways.none { it.branch == Contribution.DEATH_ENHANCED_REMOVAL } shouldBe true
    }

    test("real Cyclopean Mummy exiling its already-dead self is not battlefield removal") {
        val card=real("Cyclopean Mummy")
        val extracted=PathwayExtractor().extract(listOf(card))
        extracted.pathways.filter { it.branch == Contribution.DEATH_ENHANCED_REMOVAL }.none { it.dependencyVerdict == Verdict.MATCH } shouldBe true
    }

    test("synthetic attached death-event binding stays unknown without attachment constraints") {
        val base=real("Blood Artist")
        val ability=base.script.triggeredAbilities.single().copy(binding=com.wingedsheep.sdk.scripting.TriggerBinding.ATTACHED,
            effect=com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect(GameObjectFilter.Creature))
        val fixture=base.copy(name="Synthetic attached death",script=CardScript(triggeredAbilities=listOf(ability)))
        val p=PathwayExtractor().extract(listOf(fixture)).pathways.single { it.branch == Contribution.DEATH_ENHANCED_REMOVAL }
        p.scope.compatible(Requirements(controller=Controller.YOU,token=TokenStatus.NONTOKEN)) shouldBe Verdict.UNKNOWN
    }

})
