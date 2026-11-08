package dev.mtg.assistant

import dev.mtg.assistant.coverage.*
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.core.ManaCost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ActivationCostModelTest:FunSpec({
    fun mana(text:String)=CostTree("MANA",mana=text)
    fun all(vararg costs:CostTree)=CostTree("ALL",children=costs.toList())
    val self=CostTree("SACRIFICE_SELF")
    test("fixed mana keeps quantity color and generic versus colorless distinct") {
        fixedCostMana("{1}{G}") shouldBe mapOf("GENERIC" to 1,"G" to 1)
        compareActivationCost(mana("{1}{G}"),mana("{G}")).state shouldBe "CONTRADICTED"
        compareActivationCost(mana("{1}{G}"),mana("{1}{R}")).state shouldBe "CONTRADICTED"
        compareActivationCost(mana("{1}"),mana("{C}")).state shouldBe "CONTRADICTED"
        compareActivationCost(mana("{G}{1}"),mana("{1}{G}")).state shouldBe "SUPPORTED"
        for(text in listOf("{X}","{G/P}","{S}","{U/R}","garbage","{G}junk")) fixedCostMana(text) shouldBe null
    }
    test("mandatory conjunction retains multiplicity and rejects alternatives or unknown subcomponents") {
        compareActivationCost(all(mana("{1}{G}"),self),self).state shouldBe "CONTRADICTED"
        compareActivationCost(all(mana("{1}{G}"),self),mana("{1}{G}")).state shouldBe "CONTRADICTED"
        compareActivationCost(all(self),all(self,self)).state shouldBe "CONTRADICTED"
        compareActivationCost(all(mana("{2}{G}"),self),all(mana("{1}"),all(mana("{1}{G}"),self))).state shouldBe "SUPPORTED"
        compareActivationCost(all(mana("{1}{G}"),self),all(mana("{1}{G}"),CostTree("UNKNOWN_CHOICE"))).state shouldBe "UNRESOLVED"
        compareActivationCost(all(mana("{1}{G}"),self),projectActivationCost(AbilityCost.Forage)).state shouldBe "UNRESOLVED"
        compareActivationCost(all(self),CostTree("ANY",children=listOf(self,mana("{1}{G}")))).state shouldBe "UNRESOLVED"
    }
    test("tap untap source identity selection and fixed life remain exact") {
        projectActivationCost(AbilityCost.Tap) shouldBe CostTree("TAP_SELF")
        projectActivationCost(AbilityCost.Untap) shouldBe CostTree("UNTAP_SELF")
        compareActivationCost(CostTree("TAP_SELF"),CostTree("UNTAP_SELF")).state shouldBe "CONTRADICTED"
        compareActivationCost(self,projectActivationCost(AbilityCost.SacrificeGrantingPermanent)).state shouldBe "CONTRADICTED"
        val selection=CostTree("SACRIFICE_SELECTED",count=1,filter="CREATURE",excludeSelf=false,distinctNames=false)
        projectActivationCost(AbilityCost.Atom(CostAtom.Sacrifice(GameObjectFilter.Creature))) shouldBe selection
        compareActivationCost(selection,selection.copy(excludeSelf=true)).state shouldBe "CONTRADICTED"
        compareActivationCost(selection,self).state shouldBe "CONTRADICTED"
        compareActivationCost(CostTree("PAY_LIFE",amount=2),projectActivationCost(AbilityCost.Atom(CostAtom.PayLife(1)))).state shouldBe "CONTRADICTED"
        projectActivationCost(AbilityCost.Atom(CostAtom.Mana(ManaCost.parse("{1}{G}")))).mana shouldBe "{1}{G}"
    }
})
