package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.*
import java.security.MessageDigest
import kotlinx.serialization.json.*

class GrantedTriggerTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun card(name: String)=checkNotNull(catalog.card(name))
    fun extract(name: String)=PathwayExtractor().extract(listOf(card(name)))
    fun grant(name: String)=extract(name).actions.flatMap { it.grantedTriggers }.single()
    test("frozen six-card requirements retain printing identities and exact native source hashes") {
        val root=Path.of(System.getProperty("assistant.projectRoot"));val engine=root.parent.resolve("argentum-engine")
        val cases=Json.parseToJsonElement(Files.readString(root.resolve("src/test/resources/regression/granted-trigger-sources.json"))).jsonObject.getValue("cases").jsonArray
        cases.map { it.jsonObject.getValue("printingId") }.distinct().size shouldBe 6
        for(c in cases.map { it.jsonObject }) {
            val bytes=Files.readString(engine.resolve(c.getValue("nativePath").jsonPrimitive.content)).replace("\r\n","\n").toByteArray()
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } shouldBe c.getValue("nativeLfSha256").jsonPrimitive.content
        }
    }
    test("three creature Auras and the native Equipment back face retain separate source roles") {
        for(name in listOf("Sixth Sense","Combat Research","Custody Battle","Dire Flail")) {
            val g=grant(name)
            g.recipientSupport shouldBe Verdict.MATCH;g.eventSupport shouldBe Verdict.MATCH
            g.kind shouldBe if(name=="Dire Flail") GrantRecipientKind.EQUIPMENT_HOST else GrantRecipientKind.AURA_HOST
            g.sourceBinding shouldBe "RECEIVING_OBJECT";g.body.controller shouldBe "TRIGGER_CONTROLLER"
            g.body.action.associatedActionId shouldBe g.actionId
            reportJson.decodeFromJsonElement(TriggeredAbility.serializer(),g.body.action.native) shouldBe g.declaration.ability
            g.sourceFidelity shouldBe Verdict.UNKNOWN
        }
        grant("Dire Flail").faceName shouldBe "Dire Blunderbuss"
    }
    test("combat draw optionality is a resolution gate while the event and trigger source stay distinct") {
        for((name,optional) in listOf("Sixth Sense" to true,"Combat Research" to false)) {
            val g=grant(name)
            g.declaration.ability.binding shouldBe TriggerBinding.SELF
            val draw=g.eventOutcomes.single { it.kind==OutcomeKind.DRAW_CARDS }
            draw.optional shouldBe optional;draw.recipient shouldBe "YOU"
            g.body.facts.single { it.operation=="DRAW_CARDS" }.actionId shouldBe g.body.action.id
        }
    }
    test("upkeep targets and resolution payment preserve different checkpoints") {
        val g=grant("Custody Battle")
        g.declaration.ability.binding shouldBe TriggerBinding.ANY
        g.body.action.targetSelection?.checkpoint shouldBe TargetCheckpoint.TRIGGER_STACK_ANNOUNCEMENT
        g.body.action.targetSelection?.origin shouldBe TargetRequirementOrigin.GRANTED_TRIGGERED
        g.body.unsupported.isNotEmpty() shouldBe true
        g.body.action.native.toString().contains("PayOrSuffer") shouldBe true
    }
    test("Equipment grant references and reflexive collection stages are retained without composing triggers") {
        val g=grant("Dire Flail")
        g.body.action.native.toString().contains("GrantingPermanent") shouldBe true
        g.body.action.native.toString().contains("Pipeline") shouldBe true
        g.body.unsupported.any { it.reason.contains("Reflexive continuation") } shouldBe true
    }
    test("Stab Wound owns an attachment-relative trigger and never becomes a trigger grant") {
        val x=extract("Stab Wound")
        x.actions.flatMap { it.grantedTriggers }.isEmpty() shouldBe true
        x.actions.any { it.path.startsWith("script.triggeredAbilities") } shouldBe true
        grant("Tandem Lookout").recipientSupport shouldBe Verdict.UNKNOWN
    }
    test("granted bodies are not intrinsic facts actions pathways or triggered outcomes") {
        val x=extract("Combat Research");val g=x.actions.flatMap { it.grantedTriggers }.single()
        x.facts.none { it.actionId==g.body.action.id } shouldBe true
        x.actions.none { it.id==g.body.action.id } shouldBe true
        x.pathways.none { it.actionId==g.body.action.id } shouldBe true
        x.eventOutcomes.none { it.actionId==g.body.action.id } shouldBe true
        x.actions.count { it.path.startsWith("script.staticAbilities[") } shouldBe 3
        extract("Presence of Gond").actions.flatMap { it.grantedAbilities }.size shouldBe 1
    }
    test("granted triggers are identical across optional objective projections") {
        for(name in listOf("Sixth Sense","Custody Battle","Dire Flail","Tandem Lookout")) {
            PathwayExtractor(inspectCardEntries=true,inspectStateDependencies=true,inspectCounters=true).extract(listOf(card(name)))
                .actions.flatMap { it.grantedTriggers }.single() shouldBe grant(name)
        }
    }
    test("old actions default trigger grants to empty and current nested evidence round trips") {
        val action=extract("Sixth Sense").actions.single { it.grantedTriggers.isNotEmpty() }
        reportJson.decodeFromString(ActionEvidence.serializer(),reportJson.encodeToString(ActionEvidence.serializer(),action)) shouldBe action
        val old=JsonObject(reportJson.encodeToJsonElement(ActionEvidence.serializer(),action).jsonObject-"grantedTriggers")
        reportJson.decodeFromJsonElement(ActionEvidence.serializer(),old).grantedTriggers shouldBe emptyList()
    }
    test("unsupported group predicates do not become exact recipient bindings") {
        val base=card("Sixth Sense");val a=base.script.staticAbilities.single() as GrantTriggeredAbility
        val altered=base.copy(name="Isolated broad trigger grant",script=base.script.copy(staticAbilities=listOf(a.copy(filter=GroupFilter(GameObjectFilter.Any)))))
        PathwayExtractor().extract(listOf(altered)).actions.flatMap { it.grantedTriggers }.single().recipientSupport shouldBe Verdict.UNKNOWN
    }
    test("unsupported nested declarations still prevent false equal-ID attribution") {
        val a=card("Sixth Sense").script.staticAbilities.single() as GrantTriggeredAbility
        listOf<StaticAbility>(a,GrantStaticAbility(a)).flatMap { it.triggerDeclarations() }.count { it.ability.id==a.ability.id } shouldBe 2
    }
})
