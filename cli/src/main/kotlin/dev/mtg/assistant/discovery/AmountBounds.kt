package dev.mtg.assistant.discovery

import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.Aggregation

/** Conservative numeric bounds; null endpoints are unbounded, not guessed game-state values. */
data class AmountBounds(val lower: Long?,val upper: Long?) {
    fun negativePotential(): Verdict = when {
        lower!=null && lower>=0 -> Verdict.NO_MATCH
        upper!=null && upper<0 -> Verdict.MATCH
        else -> Verdict.UNKNOWN
    }
    fun positivePotential(): Verdict = when {
        upper!=null && upper<=0 -> Verdict.NO_MATCH
        lower!=null && lower>0 -> Verdict.MATCH
        else -> Verdict.UNKNOWN
    }
}
private fun boundedAdd(a: Long?,b: Long?): Long? = if(a==null || b==null) null else runCatching { Math.addExact(a,b) }.getOrNull()
private fun boundedMultiply(a: Long?,b: Int): Long? = if(a==null) null else runCatching { Math.multiplyExact(a,b.toLong()) }.getOrNull()
fun amountBounds(a: DynamicAmount): AmountBounds = when(a) {
    is DynamicAmount.Fixed -> AmountBounds(a.amount.toLong(),a.amount.toLong())
    is DynamicAmount.Count, is DynamicAmount.PlayerCount, is DynamicAmount.CountPlayersWith -> AmountBounds(0,null)
    is DynamicAmount.AggregateBattlefield -> if(a.aggregation==Aggregation.COUNT) AmountBounds(0,null) else AmountBounds(null,null)
    is DynamicAmount.AggregateZone -> if(a.aggregation==Aggregation.COUNT) AmountBounds(0,null) else AmountBounds(null,null)
    DynamicAmount.XValue -> AmountBounds(0,null)
    is DynamicAmount.Multiply -> {
        val b=amountBounds(a.amount)
        if(a.multiplier==0) AmountBounds(0,0)
        else if(a.multiplier>0) AmountBounds(boundedMultiply(b.lower,a.multiplier),boundedMultiply(b.upper,a.multiplier))
        else AmountBounds(boundedMultiply(b.upper,a.multiplier),boundedMultiply(b.lower,a.multiplier))
    }
    is DynamicAmount.Add -> { val l=amountBounds(a.left);val r=amountBounds(a.right);AmountBounds(boundedAdd(l.lower,r.lower),boundedAdd(l.upper,r.upper)) }
    is DynamicAmount.Conditional -> { val t=amountBounds(a.ifTrue);val f=amountBounds(a.ifFalse)
        AmountBounds(if(t.lower==null || f.lower==null) null else minOf(t.lower,f.lower),if(t.upper==null || f.upper==null) null else maxOf(t.upper,f.upper)) }
    else -> AmountBounds(null,null)
}
