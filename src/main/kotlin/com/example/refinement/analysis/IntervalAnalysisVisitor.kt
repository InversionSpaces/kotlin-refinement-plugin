package com.example.refinement.analysis

import com.example.refinement.fir.literalIntValue
import com.example.refinement.fir.propertyAccessSymbol
import com.example.refinement.models.IntervalLattice
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.cfa.util.ControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowGraphVisitor
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.WhenBranchResultEnterNode
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.isInt

class IntervalAnalysisVisitor(
    context: CheckerContext,
    reporter: DiagnosticReporter,
    messageCollector: MessageCollector
) : PathAwareControlFlowGraphVisitor<FirVariableSymbol<*>, IntervalLattice>() {

    val ctx = AnalysisContext(context, reporter, messageCollector)

    override fun mergeInfo(
        a: IntervalInfo,
        b: IntervalInfo,
        node: CFGNode<*>
    ): IntervalInfo {
        val builder = b.builder()
        for ((symbol, interval) in a) {
            builder.merge(symbol as FirVariableSymbol<*>, interval as IntervalLattice) { l, r ->
                IntervalLattice.join(l, r)
            }
        }
        return builder.build()
    }

    override fun visitVariableDeclarationNode(
        node: VariableDeclarationNode,
        data: PathAwareIntervalInfo
    ): PathAwareIntervalInfo {
        val data = visitNode(node, data)
        if (!node.fir.symbol.resolvedReturnType.isInt) return data
        val interval = node.fir.initializer?.let {
            data.evaluate(it, ctx)
        } ?: IntervalLattice.UNKNOWN
        return data.update(node.fir.symbol, interval)
    }

    override fun visitVariableAssignmentNode(
        node: VariableAssignmentNode,
        data: PathAwareIntervalInfo
    ): PathAwareIntervalInfo {
        val data = visitNode(node, data)
        // TODO: Can there be more sophisticated assignments?
        val symbol = node.fir.lValue.propertyAccessSymbol ?: return data
        if (!symbol.resolvedReturnType.isInt) return data
        val interval = data.evaluate(node.fir.rValue, ctx) ?: IntervalLattice.UNKNOWN
        return data.update(symbol, interval)
    }

    override fun visitWhenBranchResultEnterNode(
        node: WhenBranchResultEnterNode,
        data: PathAwareIntervalInfo
    ): PathAwareIntervalInfo {
        val data = visitNode(node, data)
        val interpretation = interpretCondition(node.fir.condition)
        return data.updateAll(interpretation)
    }
}
