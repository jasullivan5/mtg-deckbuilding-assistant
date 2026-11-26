package dev.mtg.assistant

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import dev.mtg.assistant.discovery.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

class SharedGrantRecipientTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun card(name: String) = checkNotNull(catalog.card(name))
    fun actions(name: String) = PathwayExtractor().extract(listOf(card(name))).actions

    test("six physical printing controls retain their frozen native source binding") {
        val root = Path.of(System.getProperty("assistant.projectRoot"))
        val engine = root.parent.resolve("argentum-engine")
        val source = root.resolve("src/test/resources/regression/shared-grant-recipient-sources.json")
        val cases = Json.parseToJsonElement(Files.readString(source)).jsonObject.getValue("cases").jsonArray
        cases.map { it.jsonObject.getValue("printingId") }.distinct().size shouldBe 6
        for (row in cases.map { it.jsonObject }) {
            val native = Files.readString(engine.resolve(row.getValue("nativePath").jsonPrimitive.content))
            val digest = MessageDigest.getInstance("SHA-256").digest(native.replace("\r\n", "\n").toByteArray())
            digest.joinToString("") { "%02x".format(it) } shouldBe row.getValue("nativeLfSha256").jsonPrimitive.content
        }
    }

    test("permanent subtype grants retain the native filter without asserting creature type") {
        val gemhide = actions("Gemhide Sliver").flatMap { it.grantedAbilities }.single()
        val manaweft = actions("Manaweft Sliver").flatMap { it.grantedAbilities }.single()
        gemhide.support shouldBe Verdict.MATCH
        manaweft.support shouldBe Verdict.MATCH
        gemhide.declaration.filter.baseFilter shouldBe GameObjectFilter.Permanent.withSubtype("Sliver")
        manaweft.declaration.filter.baseFilter shouldBe GameObjectFilter.Creature.withSubtype("Sliver").youControl()
        grantBodyRecipientFilter(gemhide.declaration.filter, true).cardPredicates.contains(CardPredicate.IsCreature) shouldBe false
        grantBodyRecipientFilter(manaweft.declaration.filter, true).controllerPredicate shouldBe null
        val synapse = actions("Synapse Sliver").flatMap { it.grantedTriggers }.single()
        synapse.eventOutcomes.single().event.sourceIsCreature shouldBe false
    }

    test("both grants reuse pair scope while correlated, state and union filters remain unresolved") {
        val deadeye = actions("Deadeye Navigator").flatMap { it.grantedAbilities }.single()
        val tandem = actions("Tandem Lookout").flatMap { it.grantedTriggers }.single()
        deadeye.kind shouldBe GrantRecipientKind.SOULBOND_PAIR
        tandem.kind shouldBe GrantRecipientKind.SOULBOND_PAIR
        deadeye.support shouldBe Verdict.MATCH
        tandem.recipientSupport shouldBe Verdict.MATCH
        val source = card("Unctus, Grand Metatect")
        val filter = GroupFilter(GameObjectFilter.Creature.withColor(Color.BLUE).youControl(), excludeSelf = true)
        sharedGrantRecipientProblems(source, filter) shouldBe emptyList()
        for (unsupported in listOf(
            filter.copy(excludeTarget = true),
            filter.copy(excludeTriggeringEntity = true),
            filter.copy(chosenSubtypeKey = "choice"),
            filter.copy(baseFilter = GameObjectFilter.Creature.tapped()),
            filter.copy(baseFilter = GameObjectFilter.Creature.copy(anyOf = listOf(GameObjectFilter.Artifact))),
            GroupFilter.soulbondPair().copy(excludeSelf = true)
        )) {
            sharedGrantRecipientProblems(source, unsupported).isNotEmpty() shouldBe true
        }
    }
})
