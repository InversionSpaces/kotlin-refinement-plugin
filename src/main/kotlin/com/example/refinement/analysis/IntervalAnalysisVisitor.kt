package com.example.refinement.analysis

import com.example.refinement.models.IntervalLattice
import org.jetbrains.kotlin.fir.analysis.cfa.util.ControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowGraphVisitor
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol

typealias IntervalInfo = ControlFlowInfo<FirVariableSymbol<*>, IntervalLattice>

class IntervalAnalysisVisitor : PathAwareControlFlowGraphVisitor<FirVariableSymbol<*>, IntervalLattice>() {
    override fun mergeInfo(
        a: IntervalInfo,
        b: IntervalInfo,
        node: CFGNode<*>
    ): IntervalInfo {
        val builder = b.builder()
        for ((symbol, interval) in a) {
            builder.merge(symbol as FirVariableSymbol<*>, interval as IntervalLattice) {
                    l, r -> IntervalLattice.join(l, r)
            }
        }
        return builder.build()
    }
}
