package com.example.refinement.analysis

import com.example.refinement.fir.*
import com.example.refinement.models.IntervalLattice
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.cfa.util.ControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.resolvedType

typealias IntervalInfo = ControlFlowInfo<FirVariableSymbol<*>, IntervalLattice>
typealias PathAwareIntervalInfo = PathAwareControlFlowInfo<FirVariableSymbol<*>, IntervalLattice>

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
        literal != null -> IntervalLattice.fromLiteral(literal)

        property != null -> {
            val refinement = refineProperty(property, ctx)
            val symbol = property.calleeReference.toResolvedPropertySymbol()
            val variable = symbol?.let { retrieve(it) }
            when {
                refinement != null && variable != null -> IntervalLattice.join(refinement, variable)
                refinement != null -> refinement
                variable != null -> variable
                else -> null
            }
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

fun PathAwareIntervalInfo.update(
    symbol: FirVariableSymbol<*>,
    interval: IntervalLattice
): PathAwareIntervalInfo {
    val b = builder()
    b.mapValuesTo(b) {
        it.value.put(symbol, interval)
    }
    return b.build()
}

fun PathAwareIntervalInfo.updateAll(
    intervals: Map<out FirVariableSymbol<*>, IntervalLattice>
): PathAwareIntervalInfo {
    val b = builder()
    b.mapValuesTo(b) {
        it.value.putAll(intervals)
    }
    return b.build()
}

fun PathAwareIntervalInfo.retrieve(
    symbol: FirVariableSymbol<*>
): IntervalLattice {
    val intervals = mapNotNull { (_, info) -> (info as IntervalInfo)[symbol] as IntervalLattice? }
    return intervals.fold(IntervalLattice.UNDEFINED, IntervalLattice::join)
}