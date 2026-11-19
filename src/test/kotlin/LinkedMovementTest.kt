package dev.mtg.assistant

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.targets.*
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

class LinkedMovementTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun card(name: String)=checkNotNull(catalog.card(name))
    fun extract(effect: Effect)=PathwayExtractor().extract(listOf(card("Cloudshift").let {
        it.copy(script=it.script.copy(spellEffect=effect)) }))
    test("source cohort remains hash bound and has distinct Oracle identities") {
        val root=Path.of(System.getProperty("assistant.projectRoot"))
        val frozen=reportJson.parseToJsonElement(Files.readString(root.resolve("src/test/resources/regression/linked-movement-sources.json"))).jsonObject
        frozen["engine"]?.jsonPrimitive?.content shouldBe EngineBuild.revision
        val cases=checkNotNull(frozen["cases"]).jsonObject
        cases.values.map { it.jsonObject.getValue("source").jsonObject.getValue("oracle_id") }.distinct().size shouldBe 4
        cases.values.forEach { value ->
            val c=value.jsonObject
            val source=Files.readString(root.parent.resolve("argentum-engine").resolve(c.getValue("nativePath").jsonPrimitive.content)).replace("\r\n","\n")
            MessageDigest.getInstance("SHA-256").digest(source.toByteArray()).joinToString("") { "%02x".format(it) } shouldBe c.getValue("nativeLfSha256").jsonPrimitive.content
        }
    }
    test("real immediate relationships reference both existing facts without changing their uncertainty") {
        for(name in listOf("Cloudshift","Momentary Blink")) {
            val x=PathwayExtractor().extract(listOf(card(name)))
            val link=x.linkedMovements.single()
            link.support shouldBe Verdict.MATCH
            link.kind shouldBe MovementLinkKind.IMMEDIATE_ORDERED
            link.departure.from shouldBe Zone.BATTLEFIELD
            link.continuation?.controller shouldBe if(name=="Cloudshift") "YOU" else "OWNER"
            val ids=link.departure.factIds+checkNotNull(link.continuation).factIds
            ids.size shouldBe 2
            ids.all { id -> x.facts.any { it.id==id } } shouldBe true
            x.facts.single { it.id==link.continuation.factIds.single() }.unresolved.isNotEmpty() shouldBe true
            reanimationFacts(x.facts).execute().none { it.verdict==Verdict.MATCH } shouldBe true
        }
    }
    test("different native targets and identical printed names never join") {
        val base=card("Cloudshift")
        val original=base.script.spellEffect as CompositeEffect
        val first=original.effects[0] as MoveToZoneEffect
        val second=original.effects[1] as MoveToZoneEffect
        val alternate=EffectTarget.ContextTarget(1)
        extract(CompositeEffect(listOf(first,second.copy(target=alternate)))).linkedMovements.isEmpty() shouldBe true
        val x=PathwayExtractor().extract(listOf(base,base.copy(name="Synthetic separate original")))
        x.linkedMovements.size shouldBe 2
        x.linkedMovements.all { it.departure.actionId==it.continuation?.actionId } shouldBe true
    }
    test("separated instructions and a deferred continuation stay UNKNOWN") {
        val original=card("Cloudshift").script.spellEffect as CompositeEffect
        val first=original.effects[0]
        val second=original.effects[1]
        extract(CompositeEffect(listOf(first,Effects.GainLife(1),second))).linkedMovements.single().support shouldBe Verdict.UNKNOWN
        val delayed=CreateDelayedTriggerEffect(step=Step.END,effect=second)
        extract(CompositeEffect(listOf(first,delayed))).linkedMovements.all { it.support!=Verdict.MATCH } shouldBe true
    }
    test("optional sequences and mutually exclusive branches cannot become mandatory links") {
        val original=card("Cloudshift").script.spellEffect as CompositeEffect
        extract(Effects.May(original)).linkedMovements.single().support shouldBe Verdict.UNKNOWN
        val branch=GatedEffect(gate=Gate.MayDecide(),then=original.effects[0],otherwise=original.effects[1])
        extract(branch).linkedMovements.isEmpty() shouldBe true
    }
    test("no exile and no return remain distinct from a supported relationship") {
        val original=card("Cloudshift").script.spellEffect as CompositeEffect
        extract(original.effects[0]).linkedMovements.isEmpty() shouldBe true
        extract(original.effects[1]).linkedMovements.isEmpty() shouldBe true
    }
    test("multiple exile-producing ability instances cannot claim an ability-specific source pile") {
        val base=card("Oblivion Ring")
        val fixture=base.copy(script=base.script.copy(triggeredAbilities=base.script.triggeredAbilities+base.script.triggeredAbilities.first()))
        val links=PathwayExtractor().extract(listOf(fixture)).linkedMovements
        links.size shouldBe 2
        links.all { it.support==Verdict.UNKNOWN } shouldBe true
    }
    test("a declared exile from an incompatible target zone cannot establish a supported link") {
        val base=card("Cloudshift")
        val target=base.script.targetRequirements.single() as TargetObject
        val sequence=base.script.spellEffect as CompositeEffect
        val first=sequence.effects[0] as MoveToZoneEffect
        val fixture=base.copy(script=base.script.copy(
            targetRequirements=listOf(target.copy(filter=target.filter.copy(zone=Zone.HAND))),
            spellEffect=sequence.copy(effects=listOf(first.copy(fromZone=Zone.BATTLEFIELD),sequence.effects[1]))))
        PathwayExtractor().extract(listOf(fixture)).linkedMovements.single().support shouldBe Verdict.UNKNOWN
    }
    test("old action JSON decodes with no invented links and current links round trip") {
        val x=PathwayExtractor().extract(listOf(card("Cloudshift")))
        val a=x.actions.single { it.path=="script.spell" }
        val json=reportJson.encodeToJsonElement(ActionEvidence.serializer(),a).jsonObject
        reportJson.decodeFromJsonElement(ActionEvidence.serializer(),JsonObject(json.filterKeys { it!="linkedMovements" })).linkedMovements.isEmpty() shouldBe true
        reportJson.decodeFromString(ActionEvidence.serializer(),reportJson.encodeToString(ActionEvidence.serializer(),a)) shouldBe a
    }
})

