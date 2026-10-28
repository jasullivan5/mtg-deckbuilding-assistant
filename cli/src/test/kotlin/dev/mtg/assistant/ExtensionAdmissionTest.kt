package dev.mtg.assistant

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.extensions.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow

class ExtensionAdmissionTest : FunSpec({
    fun fixture() = card("Admission Commander") {
        manaCost="{G}"; colorIdentity="G"; typeLine="Legendary Creature — Human"; power=1; toughness=1
    }.copy(oracleId="11111111-1111-4111-8111-111111111111",legalFormats=setOf(DeckFormat.COMMANDER))
    fun extension(c: CardDefinition) = ExtensionDefinition(c,Provenance("APPLICATION_EXTENSION","test-fixture","2026-10-27"))
    test("warning-only registration and inspection retain diagnostics without inventing incompleteness") {
        val c=fixture().copy(script=CardScript(spellEffect=GatherCardsEffect(CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),storeAs="unused")))
        val warnings=Extensions.validate(emptyList(),listOf(extension(c)))
        warnings.isNotEmpty() shouldBe true
        val r=inspectCommanderDefinition(c.name,c)
        r.nativeValidationErrors shouldBe emptyList()
        r.nativeValidationWarnings.isNotEmpty() shouldBe true
        r.selectable shouldBe true
        r.mechanicalReview shouldBe MechanicalReviewStatus.NOT_INDEPENDENTLY_VERIFIED
        val catalog=Catalog(listOf(extension(c)))
        catalog.validationWarnings shouldBe warnings
        inspectCommander(catalog,c.name).nativeValidationWarnings shouldBe r.nativeValidationWarnings
    }
    test("valid definitions and true errors keep distinct behavior") {
        Extensions.validate(emptyList(),listOf(extension(fixture()))) shouldBe emptyList()
        val broken=fixture().copy(equipCost=ManaCost.parse("{1}"))
        shouldThrow<IllegalArgumentException> { Extensions.validate(emptyList(),listOf(extension(broken))) }
        inspectCommanderDefinition(broken.name,broken).blockers.any { it.code==CommanderBlocker.INVALID_NATIVE_DEFINITION } shouldBe true
    }
    test("embedded Lightning Bolt does not overwrite its independent catalog entry or provenance") {
        val upstream=Catalog(emptyList())
        val native=upstream.card("Emeritus of Conflict")!!
        native.cardFaces.any { it.name=="Lightning Bolt" } shouldBe true
        // A new physical identity with the real native prepare structure, not a stripped name-set fixture.
        val added=native.copy(name="Admission Prepare Fixture",oracleId=fixture().oracleId,
            colorIdentityOverride=setOf(Color.RED),legalFormats=setOf(DeckFormat.COMMANDER))
        val catalog=Catalog(listOf(extension(added)))
        catalog.card(added.name)!!.cardFaces shouldBe native.cardFaces
        catalog.card("Lightning Bolt") shouldBe upstream.card("Lightning Bolt")
        catalog.provenance("Lightning Bolt").origin shouldBe "UPSTREAM_ARGENTUM"
        catalog.provenance(added.name).origin shouldBe "APPLICATION_EXTENSION"
    }
    test("actual back and flip aliases cannot overwrite registered names") {
        for(flip in listOf(false,true)) {
            val alias=fixture().copy(name="Alias",oracleId="22222222-2222-4222-8222-222222222222")
            val c=if(flip) fixture().copy(flipSide=alias) else fixture().copy(backFace=alias)
            shouldThrow<IllegalArgumentException> { Extensions.validate(listOf(alias),listOf(extension(c))) }
            val registry=CardRegistry(); registry.register(c)
            registry.requireCard(alias.name) shouldBe alias
            val existing=Catalog(emptyList()).card("Murder")!!
            val collision=if(flip) fixture().copy(flipSide=existing) else fixture().copy(backFace=existing)
            shouldThrow<IllegalArgumentException> { Catalog(listOf(extension(collision))) }
        }
    }
})
