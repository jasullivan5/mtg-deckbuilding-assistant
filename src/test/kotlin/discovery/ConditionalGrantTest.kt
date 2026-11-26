package dev.mtg.assistant

import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

class ConditionalGrantTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun card(name: String) = checkNotNull(catalog.card(name))
    fun extract(name: String) = PathwayExtractor().extract(listOf(card(name)))

    test("frozen conditional cohort binds six printings and exact native file hashes") {
        val root = Path.of(System.getProperty("assistant.projectRoot"))
        val rows = Json.parseToJsonElement(Files.readString(root.resolve(
            "src/test/resources/regression/conditional-grant-sources.json"))).jsonObject.getValue("cases").jsonArray
        rows.map { it.jsonObject.getValue("printingId") }.distinct().size shouldBe 6
        for (item in rows.map { it.jsonObject }) {
            val native = root.parent.resolve("argentum-engine").resolve(item.getValue("nativePath").jsonPrimitive.content)
            val bytes = Files.readString(native).replace("\r\n", "\n").toByteArray()
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } shouldBe
                item.getValue("nativeLfSha256").jsonPrimitive.content
        }
    }

    test("Helm exposes independent exact wrappers and nested bodies without adding intrinsic facts") {
        val name = "Helm of the Ghastlord"
        val extracted = extract(name)
        val grants = extracted.actions.flatMap { it.grantedTriggers }
        grants.map { it.staticIndex } shouldBe listOf(1, 3)
        grants.map { it.id }.distinct().size shouldBe 2
        for (grant in grants) {
            val parent = extracted.actions.single { it.id == grant.actionId }
            val native = card(name).script.staticAbilities[grant.staticIndex] as ConditionalStaticAbility
            val condition = checkNotNull(grant.conditional)
            condition.wrapperPath shouldBe parent.path
            condition.innerPath shouldBe grant.path
            condition.condition shouldBe native.condition
            condition.support shouldBe Verdict.MATCH
            condition.checkpoint shouldBe ConditionalGrantCheckpoint.TRIGGER_EVENT
            grant.declaration shouldBe native.ability
            reportJson.decodeFromString(GrantedTrigger.serializer(),
                reportJson.encodeToString(GrantedTrigger.serializer(), grant)) shouldBe grant
            extracted.facts.none { it.actionId == grant.body.action.id } shouldBe true
            extracted.actions.none { it.id == grant.body.action.id } shouldBe true
        }
        (grants[0].conditional?.condition != grants[1].conditional?.condition) shouldBe true
    }

    test("unsupported conditions are retained and ordinary grant reports decode without a wrapper") {
        val whelp = extract("Oculus Whelp").actions.flatMap { it.grantedTriggers }.single()
        whelp.recipientSupport shouldBe Verdict.MATCH
        whelp.conditional?.support shouldBe Verdict.UNKNOWN
        whelp.conditional?.checkpoint shouldBe ConditionalGrantCheckpoint.PRE_DEPARTURE
        val root = extract("Sixth Sense").actions.flatMap { it.grantedTriggers }.single()
        val json = reportJson.encodeToJsonElement(GrantedTrigger.serializer(), root).jsonObject
        ("conditional" in json) shouldBe false
        reportJson.decodeFromJsonElement(GrantedTrigger.serializer(), json).conditional shouldBe null
        val ogre = extract("Villainous Ogre").actions.flatMap { it.grantedAbilities }.single()
        reportJson.decodeFromString(GrantedAbility.serializer(),
            reportJson.encodeToString(GrantedAbility.serializer(), ogre)) shouldBe ogre
    }

    test("same-ID conditional declarations cannot use the native rule as a declaration index") {
        val base = card("Fire Nation Cadets")
        val wrapper = base.script.staticAbilities.single() as ConditionalStaticAbility
        val copy = base.copy(name = "Isolated conditional grant identity",
            script = base.script.copy(staticAbilities = listOf(wrapper, wrapper)))
        val lesson = checkNotNull(catalog.card("Environmental Sciences"))
        val game = scenarioDriver(copy, lesson)
        val source = game.putCreatureOnBattlefield(game.player1, copy.name)
        game.putCardInGraveyard(game.player1, lesson.name)
        val ref = checkNotNull(game.state.objectRef(source))
        val grants = PathwayExtractor().extract(listOf(copy)).actions.flatMap { it.grantedTriggers }
        grants.size shouldBe 2
        for (grant in grants) {
            grant.observeAvailability(game.state, ref, ref, game.services).verdict shouldBe Verdict.UNKNOWN
            grant.declaration shouldBe (wrapper.ability as GrantTriggeredAbility)
        }
    }
})
