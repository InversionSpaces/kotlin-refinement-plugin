package com.example.refinement.analysis.interval

import com.example.refinement.analysis.AnalysisContext
import com.example.refinement.fir.*
import com.example.refinement.fold
import com.example.refinement.models.IntervalLattice
import org.jetbrains.kotlin.fir.analysis.cfa.util.ControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowInfo
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowVariable
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.resolvedType

typealias IntervalInfo = ControlFlowInfo<DataFlowVariable, IntervalLattice>
typealias PathAwareIntervalInfo = PathAwareControlFlowInfo<DataFlowVariable, IntervalLattice>

internal fun refineProperty(
    property: FirPropertyAccessExpression,
    ctx: AnalysisContext
): IntervalLattice? {
    val klass = property.dispatchReceiver?.resolvedType?.toRegularClassSymbol(ctx.session) ?: return null
    val info = klass.getRefinementClassInfo(ctx.checkerContext, ctx.reporter) ?: return null

    if (info !is ParameterRefinement) return null

    return info.refinement.toLattice()
}

fun PathAwareIntervalInfo.evaluate(
    expression: FirExpression,
    ctx: AnalysisContext
): IntervalLattice? {
    val literal = expression.literalIntValue
    val property = expression.propertyAccess
    return when {
        literal != null -> IntervalLattice.exact(literal)

        property != null -> {
            val refinement = refineProperty(property, ctx)
            val analysis = ctx.getVariable(property)?.let { retrieve(it) }
            (refinement to analysis).fold(IntervalLattice::join)
        }

        expression is FirFunctionCall -> {
            val left = expression.dispatchReceiver ?: return null
            val right = expression.arguments.singleOrNull() ?: return null
            val leftInterval = evaluate(left, ctx) ?: return null
            val rightInterval = evaluate(right, ctx) ?: return null
            val symbol = expression.calleeReference.toResolvedNamedFunctionSymbol() ?: return null
            when (symbol.callableId) {
                PLUS_CALLABLE_ID -> leftInterval + rightInterval
                MINUS_CALLABLE_ID -> leftInterval - rightInterval
                TIMES_CALLABLE_ID -> leftInterval * rightInterval
                else -> null
            }
        }

        else -> null
    }
}

fun PathAwareIntervalInfo.setInterval(
    variable: DataFlowVariable,
    interval: IntervalLattice
): PathAwareIntervalInfo {
    val b = builder()
    b.mapValuesTo(b) {
        it.value.put(variable, interval)
    }
    return b.build()
}

fun PathAwareIntervalInfo.constrainIntervals(
    intervals: Map<out DataFlowVariable, IntervalLattice>
): PathAwareIntervalInfo {
    val b = builder()
    b.mapValuesTo(b) { (_, info) ->
        val bi = info.builder()
        intervals.forEach { (v, i) ->
            bi.merge(v, i, IntervalLattice::meet)
        }
        bi.build()
    }
    return b.build()
}

fun PathAwareIntervalInfo.retrieve(
    variable: DataFlowVariable
): IntervalLattice? = values
    .mapNotNull { (it as IntervalInfo)[variable] as IntervalLattice? }
    .reduceOrNull(IntervalLattice::join)
