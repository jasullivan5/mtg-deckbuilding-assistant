package dev.mtg.assistant

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.*
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*

class LegacyExileTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun card(name: String) = checkNotNull(catalog.card(name))
    fun extract(name: String) = PathwayExtractor().extract(listOf(card(name)))

    test("mandatory target and optional resolution retain different native checkpoints") {
        val x=extract("Fiend Hunter")
        val link=x.linkedMovements.single()
        link.kind shouldBe MovementLinkKind.DEFERRED_SOURCE_LINKED
        link.support shouldBe Verdict.MATCH
        link.sourceFidelity shouldBe Verdict.UNKNOWN
        link.departure.path shouldBe "script.triggeredAbilities[0].effect.then"
        link.departure.native.jsonObject["type"]?.jsonPrimitive?.content shouldBe "MoveToZone"
        link.departure.factIds.size shouldBe 1
        val fact=x.facts.single { it.id==link.departure.factIds.single() }
        fact.prerequisites.any { "MayDecide" in it } shouldBe true
        link.prerequisites.any { "may decline exile at resolution" in it } shouldBe true
        val target=card("Fiend Hunter").script.triggeredAbilities.first().allTargetRequirements.single()
        // The originating action retains the complete native target requirement.
        target.optional shouldBe false
        reanimationFacts(x.facts).execute().none { it.verdict==Verdict.MATCH } shouldBe true
        reportJson.decodeFromString(LinkedMovement.serializer(),reportJson.encodeToString(LinkedMovement.serializer(),link)) shouldBe link
    }

    test("generic May wrapper uses existing inner movement facts without a card name rule") {
        val base=card("Oblivion Ring")
        val entry=base.script.triggeredAbilities.first()
        val fixture=base.copy(name="Synthetic optional independent source",script=base.script.copy(
            triggeredAbilities=listOf(entry.copy(effect=Effects.May(entry.effect)))+base.script.triggeredAbilities.drop(1)))
        val x=PathwayExtractor().extract(listOf(fixture))
        val link=x.linkedMovements.single()
        link.support shouldBe Verdict.MATCH
        x.facts.any { it.id==link.departure.factIds.single() && it.path.endsWith(".then") } shouldBe true
        val gated=fixture.script.triggeredAbilities.first().effect as GatedEffect
        val legacy=fixture.copy(script=fixture.script.copy(triggeredAbilities=listOf(entry.copy(
            effect=Effects.May(Effects.ExileUntilLeaves((entry.effect as MoveToZoneEffect).target))))+base.script.triggeredAbilities.drop(1)))
        PathwayExtractor().extract(listOf(legacy)).linkedMovements.isEmpty() shouldBe true
        for(unsupported in listOf(gated.copy(otherwise=Effects.GainLife(1)),
            gated.copy(decisionMaker=com.wingedsheep.sdk.scripting.targets.EffectTarget.Self),
            gated.copy(gate=Gate.MayDecide(inlineOnTrigger=true)),
            gated.copy(gate=Gate.MayDecide(sourceRequiredZone=Zone.BATTLEFIELD)))) {
            val changed=fixture.copy(script=fixture.script.copy(triggeredAbilities=listOf(entry.copy(effect=unsupported))+base.script.triggeredAbilities.drop(1)))
            PathwayExtractor().extract(listOf(changed)).linkedMovements.single().support shouldBe Verdict.UNKNOWN
        }
    }

    test("complex training source and extra duration activation retain explicit uncertainty") {
        val savior=extract("Savior of Ollenbock").linkedMovements.single()
        savior.support shouldBe Verdict.UNKNOWN
        savior.departure.native.jsonObject["type"]?.jsonPrimitive?.content shouldBe "MoveToZone"
        savior.unresolved.any { "not a root self-entry trigger" in it } shouldBe true
        val werefox=extract("Werefox Bodyguard").linkedMovements.single()
        werefox.support shouldBe Verdict.UNKNOWN
        werefox.departure.native.jsonObject["type"]?.jsonPrimitive?.content shouldBe "MoveUntilSourceLeaves"
        werefox.continuation shouldBe null
        werefox.durationReturn?.checkpoint shouldBe "SOURCE_OBJECT_DEPARTURE_BEFORE_NEXT_INSTRUCTION_OR_PRIORITY"
        werefox.unresolved.any { "Additional source declarations" in it } shouldBe true
        extract("Mirror Golem").linkedMovements.isEmpty() shouldBe true
    }
})

