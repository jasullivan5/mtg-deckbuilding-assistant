package dev.mtg.assistant

import dev.mtg.assistant.discovery.*
import dev.mtg.assistant.audit.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class Phase25IntegrationTest : FunSpec({
    val catalog by lazy { Catalog() }
    fun extract(n: String)=PathwayExtractor().extract(listOf(requireNotNull(catalog.card(n))))
    fun rows(n: String)=tokenCreationFacts(extract(n).facts).execute()
    test("reviewed fixed and triggered creation definitions qualify with native action paths") {
        for(n in listOf("Dragon Fodder","Young Pyromancer","Siege-Gang Commander")) {
            val r=rows(n).single();r.verdict shouldBe Verdict.MATCH
            r.fact.path.startsWith("script.") shouldBe true;(r.fact.circumstances!=null) shouldBe true
            r.fact.prerequisites.isNotEmpty() shouldBe true
        }
    }
    test("Krenko aggregate quantity remains native and conditional") {
        val r=rows("Krenko, Mob Boss").single();r.verdict shouldBe Verdict.MATCH
        r.fact.tokenCreation!!.quantity.toString().contains("AggregateBattlefield") shouldBe true
        r.fact.circumstances!!.actionKind shouldBe "ACTIVATED"
    }
    test("Ophiomancer retains the intervening condition") {
        val r=rows("Ophiomancer").single();r.verdict shouldBe Verdict.MATCH
        r.fact.prerequisites.any { it.contains("Snake") } shouldBe true
    }
    test("Heat Shimmer copy source can be an opponent without changing token receiver") {
        val r=rows("Heat Shimmer").single();r.verdict shouldBe Verdict.MATCH
        r.fact.destinationController shouldBe "YOU";r.fact.tokenCreation!!.copySource.isNullOrEmpty() shouldBe false
    }
    test("Hunted Horror controller exclusion and Treasure type exclusion remain separate") {
        val enemy=rows("Hunted Horror").single();enemy.verdict shouldBe Verdict.NO_MATCH
        enemy.fact.tokenCreation!!.creature shouldBe Verdict.MATCH;enemy.fact.destinationController shouldBe "OPPONENT"
        val treasure=rows("Big Score").single();treasure.verdict shouldBe Verdict.NO_MATCH
        treasure.fact.destinationController shouldBe "YOU";treasure.fact.tokenCreation!!.creature shouldBe Verdict.NO_MATCH
    }
    test("Kuldotha Rebirth native sacrifice and token facts preserve one action") {
        val x=extract("Kuldotha Rebirth");val c=tokenCreationFacts(x.facts).execute().single()
        c.verdict shouldBe Verdict.MATCH
        val sac=x.facts.single { it.kind==FactKind.SACRIFICE }
        sac.actionId shouldBe c.fact.actionId;sac.costOrEffect shouldBe "CASTING_COST"
        creatureDeathPathways(x.pathways).where(canAffectCreature()).where(canAffectController(Controller.YOU)).where(canAffectTokenStatus(TokenStatus.NONTOKEN)).where(satisfiesDeathGenerationPolicy()).execute().any { it.verdict==Verdict.MATCH } shouldBe true
    }
    test("Haunted Dead returns and creates through distinct linked game events not one fabricated action") {
        val fs=extract("Haunted Dead").facts
        val b=reanimationFacts(fs).execute().single { it.verdict==Verdict.MATCH };val c=tokenCreationFacts(fs).execute().single()
        c.verdict shouldBe Verdict.MATCH;(b.fact.actionId==c.fact.actionId) shouldBe false
    }
    test("conditional copy branches retain unknown rather than unconditional creation") {
        val rs=rows("Rite of Replication");rs.size shouldBe 2
        rs.all { it.verdict==Verdict.UNKNOWN } shouldBe true
        rs.all { it.fact.prerequisites.any { p -> p.contains("Gate") } } shouldBe true
    }
    test("specialized Amass remains unsupported instead of a fabricated template") {
        val x=extract("Dreadhorde Invasion")
        tokenCreationFacts(x.facts).execute().isEmpty() shouldBe true
        x.unsupported.any { it.nativeType.contains("Amass") } shouldBe true
    }
    test("held-out fixed quantity and combat trigger transfer to generic interpretation") {
        for((n,q) in listOf("Hordeling Outburst" to 3,"Goblin Rabblemaster" to 1,"Raise the Alarm" to 2)) {
            val r=rows(n).single();r.verdict shouldBe Verdict.MATCH
            r.fact.tokenCreation!!.quantity shouldBe com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed(q)
        }
    }
    test("candidate examples missing from authored catalog remain recorded gaps") {
        listOf("Krenko's Command","Bitterblossom","Tempt with Vengeance").forEach { catalog.card(it) shouldBe null }
    }
    test("Judith remains an extension consumer without token-generation fact") {
        rows("Judith, the Scourge Diva").isEmpty() shouldBe true
        catalog.provenance("Judith, the Scourge Diva").origin shouldBe "APPLICATION_EXTENSION"
    }
})
