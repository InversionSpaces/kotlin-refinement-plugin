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

    val leftVariable = wrap(left)
    val rightVariable = wrap(right)
    val leftLiteral = left.literalIntValue
    val rightLiteral = right.literalIntValue

    return when {
        leftLiteral != null && rightVariable != null -> when (comparison.operation) {
            FirOperation.EQ -> IntervalRefinement.exact(leftLiteral)
            FirOperation.GT_EQ -> IntervalRefinement.boundedBelow(leftLiteral)
            FirOperation.LT_EQ -> IntervalRefinement.boundedAbove(leftLiteral)
            FirOperation.GT -> IntervalRefinement.boundedAbove(leftLiteral, including = false)
            FirOperation.LT -> IntervalRefinement.boundedBelow(leftLiteral, including = false)
            else -> null
        }?.let { rightVariable to it }

        rightLiteral != null && leftVariable != null -> when (comparison.operation) {
            FirOperation.EQ -> IntervalRefinement.exact(rightLiteral)
            FirOperation.GT_EQ -> IntervalRefinement.boundedAbove(rightLiteral)
            FirOperation.LT_EQ -> IntervalRefinement.boundedBelow(rightLiteral)
            FirOperation.GT -> IntervalRefinement.boundedBelow(rightLiteral, including = false)
            FirOperation.LT -> IntervalRefinement.boundedAbove(rightLiteral, including = false)
            else -> null
        }?.let { leftVariable to it }

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
            right[symbol]?.let { IntervalLattice.meet(interval, it) } ?: interval
        }
    } else emptyMap()

    else -> emptyMap()
}
