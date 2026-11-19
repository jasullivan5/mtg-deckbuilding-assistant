package dev.mtg.assistant

import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path

class AuraAttachmentTest : FunSpec({
    val catalog by lazy { Catalog() }

    test("Aura host restrictions preserve wrong types and unresolved properties") {
        attachmentRestrictionSupport(TargetObject(filter = TargetFilter.Creature)) shouldBe Verdict.MATCH
        attachmentRestrictionSupport(TargetObject(filter = TargetFilter.Creature.tapped())) shouldBe Verdict.UNKNOWN
        val requirement = TargetObject(filter = TargetFilter.Creature.youControl())
        attachmentRestrictionSupport(requirement) shouldBe Verdict.MATCH
        ObjectScope(requirement.filter.baseFilter).compatible(Requirements(controller = Controller.OPPONENT)) shouldBe Verdict.NO_MATCH
    }

    test("an Aura static bonus to another scope does not become a host modification") {
        val base = checkNotNull(catalog.card("Holy Strength"))
        val fixture = base.copy(script = base.script.copy(staticAbilities = listOf(
            ModifyStats(1, 1, GroupFilter.AllCreatures)
        )))
        val root = PathwayExtractor().extract(listOf(fixture)).actions.single { it.path == "script" }
        val dependency = checkNotNull(root.attachment).dependencies.single()
        dependency.verdict shouldBe Verdict.UNKNOWN
        dependency.stats shouldBe null
    }

    test("an unresolved damage source remains unresolved even with a supported host-controller step") {
        val base = checkNotNull(catalog.card("Wanderlust"))
        val original = base.script.triggeredAbilities.single()
        val damage = original.effect as DealDamageEffect
        val fixture = base.copy(script = base.script.copy(triggeredAbilities = listOf(
            original.copy(effect = damage.copy(damageSource = EffectTarget.EnchantedCreature))
        )))
        val root = PathwayExtractor().extract(listOf(fixture)).actions.single { it.path == "script" }
        checkNotNull(root.attachment).dependencies.single().verdict shouldBe Verdict.UNKNOWN
    }

    test("inventory all native Aura candidates one definition at a time") {
        val cards = catalog.cards.filter { it.isAura || it.script.auraTarget != null }
        val rows = cards.map { card ->
            val extracted = PathwayExtractor().extract(listOf(card))
            val relations = extracted.actions.mapNotNull { it.attachment }
            buildJsonObject {
                put("card", card.name)
                put("aura", card.isAura)
                put("relations", JsonArray(relations.map { relation ->
                    buildJsonObject {
                        put("action", relation.actionId)
                        put("support", relation.support.name)
                        put("restriction", relation.restriction?.description)
                        put("dependencies", JsonArray(relation.dependencies.map { dependency ->
                            buildJsonObject {
                                put("path", dependency.path)
                                put("kind", dependency.kind.name)
                                put("nativeType", dependency.native.jsonObject["type"]?.jsonPrimitive?.content)
                                put("triggerType", dependency.native.jsonObject["trigger"]?.jsonObject?.get("type")?.jsonPrimitive?.content)
                                put("verdict", dependency.verdict.name)
                                put("reason", dependency.reason)
                            }
                        }))
                    }
                }))
            }
        }
        (cards.size > 0) shouldBe true
        rows.size shouldBe cards.size
        System.getenv("AURA_INVENTORY_OUTPUT")?.let { output ->
            Files.writeString(Path.of(output), reportJson.encodeToString(JsonArray.serializer(), JsonArray(rows)))
        }
    }
})
