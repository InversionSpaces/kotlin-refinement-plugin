package com.example.refinement.analysis

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowGraphVisitor
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.emptyNormalPathInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.previousCfgNodes
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNodeWithSubgraphs
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.NormalPath

fun <K : Any, V : Any> ControlFlowGraph.performCDFA(
    visitor: PathAwareControlFlowGraphVisitor<K, V>,
    widening: Widening<V>
): Map<CFGNode<*>, PathAwareControlFlowInfo<K, V>> {
    val wideningVisitor = WideningPathAwareControlFlowGraphVisitor(visitor, widening)
    val nodeMap = traverseToFixedPointImpl(wideningVisitor)
    return traverseToFixedPointImpl(visitor, nodeMap)
}

// NOTE: Copied from cfa.util to change signature
private fun <K : Any, V : Any> ControlFlowGraph.traverseToFixedPointImpl(
    visitor: PathAwareControlFlowGraphVisitor<K, V>,
    state: MutableMap<CFGNode<*>, PathAwareControlFlowInfo<K, V>>? = null,
): MutableMap<CFGNode<*>, PathAwareControlFlowInfo<K, V>> {
    val nodeMap = state ?: LinkedHashMap()
    while (traverseOnceImpl(visitor, nodeMap)) {
        // had changes, continue
    }
    return nodeMap
}

private fun <K : Any, V : Any> ControlFlowGraph.traverseOnceImpl(
    visitor: PathAwareControlFlowGraphVisitor<K, V>,
    nodeMap: MutableMap<CFGNode<*>, PathAwareControlFlowInfo<K, V>>,
): Boolean {
    var changed = false
    for (node in nodes) {
        // TODO, KT-59670: if data for previousNodes hasn't changed, then should be no need to recompute data for this one
        val previousData = node.previousCfgNodes.mapNotNull { source ->
            nodeMap[source]?.let {
                visitor.visitEdge(source, node, node.edgeFrom(source), it)
            }
        }.reduceOrNull { a, b -> visitor.mergeInfo(a, b, node) }
        val newData = node.accept(visitor, previousData ?: emptyNormalPathInfo())
        if (newData != nodeMap.put(node, newData)) {
            changed = true
        }
        if (node is CFGNodeWithSubgraphs<*>) {
            node.subGraphs.forEach {
                changed = changed or (visitor.visitSubGraph(node, it) && it.traverseOnceImpl(visitor, nodeMap))
            }
        }
    }
    return changed
}