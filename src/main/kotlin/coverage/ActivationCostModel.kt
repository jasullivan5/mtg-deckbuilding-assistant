package dev.mtg.assistant.coverage

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom
import kotlinx.serialization.Serializable

/** Payment structure only. Native costs/engine remain authoritative for actual affordability and payment. */
@Serializable data class CostTree(val kind:String,val children:List<CostTree> = emptyList(),val mana:String?=null,
    val amount:Int?=null,val count:Int?=null,val filter:String?=null,val excludeSelf:Boolean?=null,val distinctNames:Boolean?=null)
@Serializable data class CostComparison(val state:String,val expected:CostTree,val observed:CostTree,
    val expectedCanonical:List<String>?,val observedCanonical:List<String>?,val reasons:List<String>)

/** Exact fixed symbols; GENERIC is deliberately different from C. No hybrid/X/snow/phyrexian solver. */
fun fixedCostMana(text:String):Map<String,Int>? = runCatching {
    val symbols=ManaCost.parse(text).symbols
    require(text.filterNot(Char::isWhitespace)==symbols.joinToString("")) { "Unparsed or noncanonical mana text" }
    if(symbols.any { it !is ManaSymbol.Generic && it !is ManaSymbol.Colored && it !is ManaSymbol.Colorless }) return null
    val result=sortedMapOf<String,Int>()
    for(s in symbols) {
        val (key,amount)=when(s) {
            is ManaSymbol.Generic -> "GENERIC" to s.amount
            is ManaSymbol.Colored -> s.color.symbol.toString() to 1
            ManaSymbol.Colorless -> "C" to 1
            else -> return null
        }
        require(amount>=0)
        if(amount>0) result[key]=Math.addExact(result[key] ?: 0,amount)
    }
    result.toMap()
}.getOrNull()

fun projectActivationCost(cost:AbilityCost):CostTree = when(cost) {
    AbilityCost.Free -> CostTree("ALL")
    AbilityCost.Tap -> CostTree("TAP_SELF")
    AbilityCost.Untap -> CostTree("UNTAP_SELF")
    AbilityCost.SacrificeSelf -> CostTree("SACRIFICE_SELF")
    AbilityCost.SacrificeGrantingPermanent -> CostTree("SACRIFICE_GRANTER")
    is AbilityCost.Composite -> CostTree("ALL",children=cost.costs.map(::projectActivationCost))
    is AbilityCost.Atom -> when(val atom=cost.atom) {
        is CostAtom.Mana -> CostTree("MANA",mana=atom.cost.toString())
        is CostAtom.PayLife -> atom.fixedAmount?.let { CostTree("PAY_LIFE",amount=it) } ?: CostTree("UNRESOLVED_DYNAMIC_LIFE")
        is CostAtom.Sacrifice -> if(atom.filter==GameObjectFilter.Creature && !atom.distinctNames && atom.count>0)
            CostTree("SACRIFICE_SELECTED",count=atom.count,filter="CREATURE",excludeSelf=atom.excludeSelf,distinctNames=atom.distinctNames)
            else CostTree("UNRESOLVED_SACRIFICE_SELECTION")
        else -> CostTree("UNRESOLVED_ATOM_${atom::class.simpleName}")
    }
    AbilityCost.Forage -> CostTree("UNRESOLVED_ALTERNATIVE_FORAGE")
    else -> CostTree("UNRESOLVED_NATIVE_${cost::class.simpleName}")
}

/** Only associative ALL and addition of fixed mana are normalized. Multiplicity of other payments remains. */
fun canonicalActivationCost(tree:CostTree):List<String>? {
    val leaves=mutableListOf<String>();val mana=sortedMapOf<String,Int>()
    fun visit(n:CostTree):Boolean = when(n.kind) {
        "ALL" -> n.copy(children=emptyList())==CostTree("ALL") && n.children.all(::visit)
        "MANA" -> {
            val fixed=n.mana?.let(::fixedCostMana)
            if(fixed==null || n.copy(mana=null)!=CostTree("MANA")) false else {
                for((key,value) in fixed) mana[key]=Math.addExact(mana[key] ?: 0,value)
                true
            }
        }
        "TAP_SELF","UNTAP_SELF","SACRIFICE_SELF","SACRIFICE_GRANTER" -> if(n==CostTree(n.kind)) { leaves+=n.kind;true } else false
        "PAY_LIFE" -> if(n.amount!=null && n.amount>=0 && n.copy(amount=null)==CostTree("PAY_LIFE")) { leaves+="PAY_LIFE:${n.amount}";true } else false
        "SACRIFICE_SELECTED" -> if(n.count!=null && n.count>0 && n.filter=="CREATURE" && n.excludeSelf!=null && n.distinctNames==false && n.children.isEmpty() && n.mana==null && n.amount==null) {
            leaves+="SACRIFICE_SELECTED:count=${n.count}:CREATURE:excludeSelf=${n.excludeSelf}:distinctNames=false:controller=PAYER:owner=ANY:from=BATTLEFIELD:to=OWNER_GRAVEYARD_NOMINAL";true
        } else false
        else -> false
    }
    return runCatching {
        if(!visit(tree)) return null
        if(mana.isNotEmpty()) leaves+="MANA:"+mana.entries.joinToString { "${it.key}=${it.value}" }
        leaves.sorted()
    }.getOrNull()
}

fun compareActivationCost(expected:CostTree,observed:CostTree):CostComparison {
    val a=canonicalActivationCost(expected);val b=canonicalActivationCost(observed)
    val state=if(a==null || b==null) "UNRESOLVED" else if(a==b) "SUPPORTED" else "CONTRADICTED"
    return CostComparison(state,expected,observed,a,b,when(state) {
        "SUPPORTED" -> listOf("Complete supported printed base payment agrees; declaration legality, contextual modifiers and resolving behavior remain outside this comparison")
        "CONTRADICTED" -> listOf("Mandatory fixed payment components, quantities or bindings differ; recognized sacrifice alone cannot compensate for missing payments")
        else -> listOf("A source or native component, alternative, condition or parameter is outside the supported cost domain; no partial-cost acceptance")
    })
}

fun costRequirementCount(tree:CostTree):Int = if(tree.kind=="ALL") 1+tree.children.sumOf(::costRequirementCount) else 1
