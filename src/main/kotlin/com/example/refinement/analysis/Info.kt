package com.example.refinement.analysis

import com.example.refinement.fir.PLUS_CALLABLE_ID
import com.example.refinement.fir.literalIntValue
import com.example.refinement.fir.propertyAccessSymbol
import com.example.refinement.models.IntervalLattice
import org.jetbrains.kotlin.fir.analysis.cfa.util.ControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowInfo
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.references.toResolvedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol

typealias IntervalInfo = ControlFlowInfo<FirVariableSymbol<*>, IntervalLattice>
typealias PathAwareIntervalInfo = PathAwareControlFlowInfo<FirVariableSymbol<*>, IntervalLattice>

fun PathAwareIntervalInfo.evaluate(
    expression: FirExpression
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

fun PathAwareIntervalInfo.retrieve(
    symbol: FirVariableSymbol<*>
): IntervalLattice {
    val intervals = mapNotNull { (_, info) -> (info as IntervalInfo)[symbol] as IntervalLattice? }
    return intervals.fold(IntervalLattice.UNDEFINED, IntervalLattice::join)
}