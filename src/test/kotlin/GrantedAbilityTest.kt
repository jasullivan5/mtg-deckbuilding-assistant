package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.filters.unified.*
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlinx.serialization.json.*

class GrantedAbilityTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun card(name: String)=checkNotNull(catalog.card(name))
    fun extract(name: String)=PathwayExtractor().extract(listOf(card(name)))
    fun grant(name: String)=extract(name).actions.flatMap { it.grantedAbilities }.single()
    test("five frozen source controls retain exact native hashes and distinct printing identities") {
        val root=Path.of(System.getProperty("assistant.projectRoot"))
        val engine=System.getenv("ARGENTUM_ENGINE_DIR")?.let(Path::of) ?: root.parent.resolve("argentum-engine")
        val sources=Json.parseToJsonElement(Files.readString(root.resolve("src/test/resources/regression/granted-ability-sources.json"))).jsonObject.getValue("cases").jsonArray
        sources.map { it.jsonObject.getValue("printingId") }.distinct().size shouldBe 5
        for(c in sources.map { it.jsonObject }) {
            val text=Files.readString(engine.resolve(c.getValue("nativePath").jsonPrimitive.content)).replace("\r\n","\n")
            MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) } shouldBe c.getValue("nativeLfSha256").jsonPrimitive.content
        }
    }
    test("Aura Equipment and group grants keep granter and receiving source roles separate") {
        for((name,kind) in listOf("Burning Anger" to GrantRecipientKind.AURA_HOST,"Presence of Gond" to GrantRecipientKind.AURA_HOST,
            "Viridian Longbow" to GrantRecipientKind.EQUIPMENT_HOST,"Cryptolith Rite" to GrantRecipientKind.FILTER_GROUP)) {
            val g=grant(name)
            g.kind shouldBe kind
            g.support shouldBe Verdict.MATCH
            g.sourceBinding shouldBe "RECEIVING_OBJECT"
            g.body.source shouldBe "RECEIVING_OBJECT"
            g.body.action.associatedActionId shouldBe g.actionId
            g.body.action.id.endsWith(".ability") shouldBe true
            g.body.action.native.jsonObject.getValue("cost") shouldBe reportJson.encodeToJsonElement(AbilityCost.serializer(),g.declaration.ability.cost)
        }
    }
    test("dynamic damage keeps native quantity and source expressions without using Aura printed power") {
        val g=grant("Burning Anger")
        val damage=g.declaration.ability.effect as DealDamageEffect
        (damage.damageSource==null || damage.damageSource==EffectTarget.Self) shouldBe true
        g.body.facts.any { it.kind==FactKind.DAMAGE && it.native.jsonObject["amount"]!=null } shouldBe true
        g.body.action.targetSelection?.checkpoint shouldBe TargetCheckpoint.ACTIVATION_ANNOUNCEMENT
        g.body.action.targetSelection?.origin shouldBe TargetRequirementOrigin.GRANTED_ACTIVATED
    }
    test("tokens and mana reuse operation facts inside an independently controlled granted body") {
        val gond=grant("Presence of Gond")
        gond.body.facts.single { it.kind==FactKind.CREATE_TOKEN }.destinationController shouldBe "YOU"
        grant("Cryptolith Rite").body.facts.any { it.operation=="MANA_PRODUCTION" } shouldBe true
        grant("Cryptolith Rite").declaration.ability.isManaAbility shouldBe true
    }
    test("grant bodies never enter intrinsic facts or objective pathways") {
        val x=extract("Presence of Gond")
        val g=x.actions.flatMap { it.grantedAbilities }.single()
        x.facts.none { it.actionId==g.body.action.id } shouldBe true
        x.pathways.none { it.actionId==g.body.action.id } shouldBe true
        x.actions.none { it.id==g.body.action.id } shouldBe true
        x.unsupported.any { it.path==g.path } shouldBe true
    }
    test("complex face-down group grants remain unresolved but reuse receiving-context collection provenance") {
        val g=grant("Etrata, Deadly Fugitive")
        g.support shouldBe Verdict.UNKNOWN
        g.body.action.collectionStages.isNotEmpty() shouldBe true
        g.body.action.collectionStages.all { it.actionId==g.body.action.id } shouldBe true
    }
    test("group restrictions and ability identity are not inferred from matching descriptions") {
        val base=card("Cryptolith Rite")
        val native=base.script.staticAbilities.single() as GrantActivatedAbility
        val altered=base.copy(name="Synthetic different grant",script=base.script.copy(staticAbilities=listOf(native.copy(filter=GroupFilter.source()))))
        PathwayExtractor().extract(listOf(altered)).actions.flatMap { it.grantedAbilities }.single().support shouldBe Verdict.UNKNOWN
        val other=PathwayExtractor().extract(listOf(base,altered)).actions.flatMap { it.grantedAbilities }
        other.map { it.id }.distinct().size shouldBe 2
        other.map { it.body.action.id }.distinct().size shouldBe 2
    }
    test("old action reports decode without grant evidence and nested bodies round trip") {
        val action=extract("Presence of Gond").actions.single { it.grantedAbilities.isNotEmpty() }
        reportJson.decodeFromString(ActionEvidence.serializer(),reportJson.encodeToString(ActionEvidence.serializer(),action)) shouldBe action
        val old=JsonObject(reportJson.encodeToJsonElement(ActionEvidence.serializer(),action).jsonObject-"grantedAbilities")
        reportJson.decodeFromJsonElement(ActionEvidence.serializer(),old).grantedAbilities shouldBe emptyList()
    }
    test("grant interpretation is independent of optional objective projections") {
        for(name in listOf("Presence of Gond","Burning Anger","Etrata, Deadly Fugitive")) {
            val projected=PathwayExtractor(inspectCardEntries=true,inspectStateDependencies=true,inspectCounters=true)
                .extract(listOf(card(name))).actions.flatMap { it.grantedAbilities }.single()
            projected shouldBe grant(name)
        }
    }
})

