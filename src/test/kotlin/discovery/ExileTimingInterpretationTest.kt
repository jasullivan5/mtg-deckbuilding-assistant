package dev.mtg.assistant

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.MoveUntilSourceLeavesEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*

class ExileTimingInterpretationTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun extracted(name: String)=PathwayExtractor().extract(listOf(checkNotNull(catalog.card(name))))
    test("corrected independent triggers reference ordinary exile and the actual separate return fact") {
        val x=extracted("Oblivion Ring")
        val link=x.linkedMovements.single()
        link.kind shouldBe MovementLinkKind.DEFERRED_SOURCE_LINKED
        link.support shouldBe Verdict.MATCH
        link.departure.factIds.size shouldBe 1
        link.departure.native.jsonObject.getValue("type").jsonPrimitive.content shouldBe "MoveToZone"
        link.departure.native.jsonObject.getValue("linkToSource").jsonPrimitive.boolean shouldBe true
        link.continuation?.factIds?.size shouldBe 1
        (link.departure.actionId != link.continuation?.actionId) shouldBe true
        link.durationReturn shouldBe null
        link.sourceFidelity shouldBe Verdict.UNKNOWN
    }
    test("corrected duration has an implicit return checkpoint and no invented return action") {
        val x=extracted("Banisher Priest")
        val link=x.linkedMovements.single()
        link.kind shouldBe MovementLinkKind.DURATION_BOUND
        link.support shouldBe Verdict.MATCH
        link.continuation shouldBe null
        link.durationReturn?.anchor shouldBe EffectTarget.Self
        link.durationReturn?.returnTo shouldBe Zone.BATTLEFIELD
        link.durationReturn?.controller shouldBe "OWNER"
        link.durationReturn?.mechanism shouldBe "ZoneReturnService"
        x.actions.none { it.path=="script.triggeredAbilities[1]" } shouldBe true
        reanimationFacts(x.facts).execute().none { it.verdict==Verdict.MATCH } shouldBe true
        link.sourceFidelity shouldBe Verdict.UNKNOWN
        reportJson.decodeFromString(LinkedMovement.serializer(),reportJson.encodeToString(LinkedMovement.serializer(),link)) shouldBe link
    }
    test("an unknown duration anchor cannot become a self-source relationship") {
        val base=checkNotNull(catalog.card("Banisher Priest"))
        val ability=base.script.triggeredAbilities.single()
        val effect=ability.effect as MoveUntilSourceLeavesEffect
        val fixture=base.copy(script=base.script.copy(triggeredAbilities=listOf(ability.copy(effect=effect.copy(anchor=EffectTarget.ContextTarget(0))))))
        PathwayExtractor().extract(listOf(fixture)).linkedMovements.single().support shouldBe Verdict.UNKNOWN
        val otherZone=base.copy(script=base.script.copy(triggeredAbilities=listOf(ability.copy(effect=effect.copy(destination=Zone.HAND)))))
        val unresolved=PathwayExtractor().extract(listOf(otherZone)).linkedMovements.single()
        unresolved.support shouldBe Verdict.UNKNOWN
        unresolved.departure.to shouldBe Zone.HAND
    }
})
