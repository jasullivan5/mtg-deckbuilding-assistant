package dev.mtg.assistant

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.*
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*

/** Shared evidence reads the native pile; it does not decide whether to exile or return. */
class LegacyExileScenarioTest : FunSpec({
    val catalog by lazy { Catalog() }
    for(accept in listOf(false,true)) test("Fiend Hunter resolution choice controls establishment of the observed source link: $accept") {
        val card=checkNotNull(catalog.card("Fiend Hunter"))
        val link=PathwayExtractor().extract(listOf(card)).linkedMovements.single()
        val d=exileReturnDriver()
        val target=d.putCreatureOnBattlefield(d.player2,"Hill Giant")
        val original=checkNotNull(d.state.objectRef(target))
        val source=d.castExileControl("Fiend Hunter")
        d.bothPass().error shouldBe null
        val visit=checkNotNull(captureLinkedSource(d.state,checkNotNull(d.state.objectRef(source))))
        val selection=d.pendingDecision as ChooseTargetsDecision
        selection.targetRequirements.single().minTargets shouldBe 1
        d.submitTargetSelection(d.player1,listOf(target)).error shouldBe null
        link.linkedExileNow(d.state,visit) shouldBe emptyList()
        d.bothPass().error shouldBe null
        (d.pendingDecision is YesNoDecision) shouldBe true
        d.submitYesNo(d.player1,accept).error shouldBe null
        val pile=checkNotNull(link.linkedExileNow(d.state,visit))
        pile.map { it.entityId } shouldBe if(accept) listOf(target) else emptyList()
        if(accept) (pile.single()!=original) shouldBe true
        d.castExileControl("Boomerang",source)
        d.bothPass().error shouldBe null
        d.getExile(d.player2).contains(target) shouldBe accept
        d.stackSize shouldBe 1
        d.bothPass().error shouldBe null
        d.getPermanents(d.player2).contains(target) shouldBe true
        link.linkedExileNow(d.state,visit) shouldBe emptyList()
        d.state.isCurrentObject(original) shouldBe !accept
        println("LEGACY_APPLICATION " + buildJsonObject {
            put("engine",EngineBuild.revision);put("acceptAtResolution",accept)
            put("relationship",reportJson.encodeToJsonElement(LinkedMovement.serializer(),link))
            put("sourceVisit",reportJson.encodeToJsonElement(LinkedSourceVisit.serializer(),visit))
            put("original",reportJson.encodeToJsonElement(com.wingedsheep.engine.state.ObjectRef.serializer(),original))
            put("result",reportJson.encodeToJsonElement(com.wingedsheep.engine.state.ObjectRef.serializer(),checkNotNull(d.state.objectRef(target))))
        })
    }
})
