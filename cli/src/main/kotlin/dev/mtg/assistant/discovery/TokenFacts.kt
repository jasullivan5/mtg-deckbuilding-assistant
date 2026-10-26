package dev.mtg.assistant.discovery

import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.Aggregation
import kotlinx.serialization.Serializable

/** One correlated creation variant, never an aggregate of different branches/templates. */
@Serializable data class TokenCreation(
    val creature: Verdict,
    val quantity: DynamicAmount,
    val positiveQuantity: Verdict,
    val templateReference: String,
    val copySource: String? = null,
    val creationEvent: String? = null,
    val characteristics: List<String> = emptyList()
)

fun createdObjectIsCreature(required: Boolean = true)=FactPredicate("created-creature-$required") { it.copy(createdCreature=required,impossible=it.impossible || it.createdCreature!=null && it.createdCreature!=required) }
fun quantityCanBePositive()=FactPredicate("positive-quantity-potential") { it.copy(positiveQuantity=true) }
fun tokenCreationFacts(facts: List<MechanicalFact>,creature: Boolean = true)=FactQuery(facts.filter { it.kind==FactKind.CREATE_TOKEN })
    .where(operationIs(FactKind.CREATE_TOKEN)).where(createdObjectIsCreature(creature))
    .where(entersUnder(Controller.YOU)).where(quantityCanBePositive())

/** Conditional existential potential, not the number of tokens in an arbitrary state.
 * This deliberately does not change AmountBounds' stricter death-magnitude interpretation.
 * Only explicit count/X expressions get a satisfiable positive-count prerequisite. */
fun tokenQuantityPotential(a: DynamicAmount): Verdict {
    val bounds=amountBounds(a)
    if(bounds.upper!=null && bounds.upper<=0) return Verdict.NO_MATCH
    if(bounds.lower!=null && bounds.lower>0) return Verdict.MATCH
    return when(a) {
        DynamicAmount.XValue, is DynamicAmount.Count, is DynamicAmount.PlayerCount, is DynamicAmount.CountPlayersWith -> Verdict.MATCH
        is DynamicAmount.AggregateBattlefield -> if(a.aggregation==Aggregation.COUNT) Verdict.MATCH else Verdict.UNKNOWN
        is DynamicAmount.AggregateZone -> if(a.aggregation==Aggregation.COUNT) Verdict.MATCH else Verdict.UNKNOWN
        is DynamicAmount.Multiply -> if(a.multiplier>0) tokenQuantityPotential(a.amount) else Verdict.UNKNOWN
        // Do not merge branches or assume stored/bound quantities can be positive.
        else -> Verdict.UNKNOWN
    }
}
