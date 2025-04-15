package com.example.refinement.analysis

import com.example.refinement.fir.literalIntValue
import com.example.refinement.fir.propertyAccessSymbol
import com.example.refinement.models.IntervalLattice
import com.example.refinement.models.IntervalRefinement
import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.fir.expressions.FirBooleanOperatorExpression
import org.jetbrains.kotlin.fir.expressions.FirComparisonExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

fun interpretComparison(
    comparison: FirComparisonExpression,
): Pair<FirPropertySymbol, IntervalLattice>? {
    val left = comparison.compareToCall.dispatchReceiver ?: return null
    val right = comparison.compareToCall.arguments.singleOrNull() ?: return null

    val leftProperty = left.propertyAccessSymbol
    val rightProperty = right.propertyAccessSymbol

    val interval = when (comparison.operation) {
        FirOperation.EQ -> IntervalLattice.ZERO
        FirOperation.GT -> IntervalLattice.POSITIVE
        FirOperation.LT -> IntervalLattice.NEGATIVE
        else -> null
    }

    return when {
        right.literalIntValue == 0L && leftProperty != null -> {
            interval?.let { leftProperty to it }
        }

        left.literalIntValue == 0L && rightProperty != null -> {
            interval?.let { rightProperty to -it }
        }

        else -> null
    }
}

fun interpretCondition(
    condition: FirExpression,
): Map<FirPropertySymbol, IntervalLattice> = when (condition) {
    is FirComparisonExpression -> interpretComparison(condition)?.let { mapOf(it) } ?: emptyMap()
    is FirBooleanOperatorExpression -> if (condition.kind == LogicOperationKind.AND) {
        val left = interpretCondition(condition.leftOperand)
        val right = interpretCondition(condition.rightOperand)
        left.mapValuesTo(right.toMutableMap()) { (symbol, interval) ->
            right[symbol]?.let { IntervalLattice.join(interval, it) } ?: interval
        }
    } else emptyMap()

    else -> emptyMap()
}
