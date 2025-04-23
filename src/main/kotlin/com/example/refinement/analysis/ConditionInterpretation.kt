package com.example.refinement.analysis

import com.example.refinement.fir.literalIntValue
import com.example.refinement.models.IntervalLattice
import com.example.refinement.models.IntervalRefinement
import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowVariable

fun <T> interpretComparison(
    comparison: FirComparisonExpression,
    wrap: (FirExpression) -> T?,
): Pair<T, IntervalRefinement>? {
    val left = comparison.compareToCall.dispatchReceiver ?: return null
    val right = comparison.compareToCall.argument

    val leftWrapped = wrap(left)
    val rightVariable = wrap(right)

    val interval = when (comparison.operation) {
        FirOperation.EQ -> IntervalRefinement.ZERO
        FirOperation.GT -> IntervalRefinement.POSITIVE
        FirOperation.LT -> IntervalRefinement.NEGATIVE
        else -> null
    }

    return when {
        right.literalIntValue == 0L && leftWrapped != null -> {
            interval?.let { leftWrapped to it }
        }

        left.literalIntValue == 0L && rightVariable != null -> {
            interval?.let { rightVariable to -it }
        }

        else -> null
    }
}

fun interpretCondition(
    condition: FirExpression,
    ctx: AnalysisContext,
): Map<DataFlowVariable, IntervalLattice> = when (condition) {
    is FirComparisonExpression -> interpretComparison(condition) {
        ctx.getVariable(it)
    }?.let { (variable, refinement) ->
        mapOf(variable to refinement.toLattice())
    } ?: emptyMap()

    is FirBooleanOperatorExpression -> if (condition.kind == LogicOperationKind.AND) {
        val left = interpretCondition(condition.leftOperand, ctx)
        val right = interpretCondition(condition.rightOperand, ctx)
        left.mapValuesTo(right.toMutableMap()) { (symbol, interval) ->
            right[symbol]?.let { IntervalLattice.join(interval, it) } ?: interval
        }
    } else emptyMap()

    else -> emptyMap()
}
