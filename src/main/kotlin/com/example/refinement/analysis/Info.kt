package com.example.refinement.analysis

import com.example.refinement.fir.literalIntValue
import com.example.refinement.models.IntervalLattice
import org.jetbrains.kotlin.fir.analysis.cfa.util.ControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowInfo
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol

typealias IntervalInfo = ControlFlowInfo<FirVariableSymbol<*>, IntervalLattice>
typealias PathAwareIntervalInfo = PathAwareControlFlowInfo<FirVariableSymbol<*>, IntervalLattice>

fun PathAwareIntervalInfo.evaluate(
    expression: FirExpression
): IntervalLattice? {
    val literal = expression.literalIntValue
    return when {
        literal != null -> IntervalLattice.fromLiteral(literal)
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