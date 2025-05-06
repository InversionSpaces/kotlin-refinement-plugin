package com.example.refinement.analysis

import com.example.refinement.models.IntervalLattice
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowGraphVisitor
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowVariable
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.LoopBlockEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.WhenBranchResultEnterNode
import org.jetbrains.kotlin.fir.types.isInt
import org.jetbrains.kotlin.fir.types.resolvedType

class IntervalAnalysisVisitor(
    context: CheckerContext,
    reporter: DiagnosticReporter,
    messageCollector: MessageCollector
) : PathAwareControlFlowGraphVisitor<DataFlowVariable, IntervalLattice>() {

    val ctx = AnalysisContext(context, reporter, messageCollector)

    override fun mergeInfo(
        a: IntervalInfo,
        b: IntervalInfo,
        node: CFGNode<*>
    ): IntervalInfo {
        val builder = b.builder()
        for ((variable, interval) in a) {
            builder.merge(variable as DataFlowVariable, interval as IntervalLattice) { l, r ->
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
        val variable = ctx.createLocalVariable(node.fir.symbol)
        val interval = node.fir.initializer?.let {
            data.evaluate(it, ctx)
        } ?: IntervalLattice.UNKNOWN
        return data.update(variable, interval)
    }

    override fun visitVariableAssignmentNode(
        node: VariableAssignmentNode,
        data: PathAwareIntervalInfo
    ): PathAwareIntervalInfo {
        val data = visitNode(node, data)
        if (!node.fir.lValue.resolvedType.isInt) return data
        val variable = ctx.getVariable(node.fir.lValue) ?: return data
        val interval = data.evaluate(node.fir.rValue, ctx) ?: IntervalLattice.UNKNOWN
        return data.update(variable, interval)
    }

    override fun visitWhenBranchResultEnterNode(
        node: WhenBranchResultEnterNode,
        data: PathAwareIntervalInfo
    ): PathAwareIntervalInfo {
        val data = visitNode(node, data)
        val interpretation = interpretCondition(node.fir.condition, ctx)
        return data.updateAll(interpretation)
    }

    override fun visitLoopBlockEnterNode(
        node: LoopBlockEnterNode,
        data: PathAwareIntervalInfo
    ): PathAwareIntervalInfo {
        val data = visitNode(node, data)
        // TODO: support `for (i in 0 .. 10)` which seems much harder
        val interpretation = interpretCondition(node.fir.condition, ctx)
        return data.updateAll(interpretation)
    }
}
