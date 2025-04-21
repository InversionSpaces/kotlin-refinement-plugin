package com.example.refinement.analysis

import com.example.refinement.fir.MINUS_CALLABLE_ID
import com.example.refinement.fir.PLUS_CALLABLE_ID
import com.example.refinement.fir.TIMES_CALLABLE_ID
import com.example.refinement.fir.literalIntValue
import com.example.refinement.fir.propertyAccess
import com.example.refinement.fir.propertyAccessSymbol
import com.example.refinement.models.IntervalLattice
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.fir.analysis.cfa.util.ControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowInfo
import org.jetbrains.kotlin.fir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.references.toResolvedSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.classId

typealias IntervalInfo = ControlFlowInfo<FirVariableSymbol<*>, IntervalLattice>
typealias PathAwareIntervalInfo = PathAwareControlFlowInfo<FirVariableSymbol<*>, IntervalLattice>

fun PathAwareIntervalInfo.evaluate(
    expression: FirExpression,
    messageCollector: MessageCollector? = null // TODO: Remove
): IntervalLattice? {
    val literal = expression.literalIntValue
    val property = expression.propertyAccessSymbol
    return when {
        literal != null -> IntervalLattice.fromLiteral(literal)

        property != null -> retrieve(property)

        expression is FirFunctionCall -> {
            val left = expression.dispatchReceiver ?: return null
            val right = expression.arguments.singleOrNull() ?: return null
            val leftInterval = evaluate(left) ?: return null
            val rightInterval = evaluate(right) ?: return null
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