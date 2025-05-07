package com.example.refinement.analysis

import org.jetbrains.kotlin.fir.analysis.cfa.util.ControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowGraphVisitor
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowInfo
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.AnonymousFunctionCaptureNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.AnonymousFunctionExpressionNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.AnonymousObjectEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.AnonymousObjectExpressionExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.BlockEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.BlockExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.BooleanOperatorEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.BooleanOperatorEnterRightOperandNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.BooleanOperatorExitLeftOperandNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.BooleanOperatorExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CallableReferenceNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CatchClauseEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CatchClauseExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CheckNotNullCallNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ClassEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ClassExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CodeFragmentEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CodeFragmentExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ComparisonExpressionNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.DelegateExpressionExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.DelegatedConstructorCallNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ElvisExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ElvisLhsExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ElvisLhsIsNotNullNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ElvisRhsEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EnterDefaultArgumentsNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EnterSafeCallNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EnterValueParameterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EqualityOperatorCallNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ExitDefaultArgumentsNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ExitSafeCallNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ExitValueParameterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FakeExpressionEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FieldInitializerEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FieldInitializerExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FileEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FileExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FinallyBlockEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FinallyBlockExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallArgumentsEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallArgumentsExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.GetClassCallNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.InitBlockEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.InitBlockExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.JumpNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.LiteralExpressionNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.LocalClassExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.LocalFunctionDeclarationNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.LoopBlockEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.LoopBlockExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.LoopConditionEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.LoopConditionExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.LoopEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.LoopExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.MergePostponedLambdaExitsNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.PostponedLambdaExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.PropertyInitializerEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.PropertyInitializerExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.QualifiedAccessNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ReplSnippetEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ReplSnippetExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ResolvedQualifierNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ScriptEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ScriptExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.SmartCastExpressionExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.SplitPostponedLambdasNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.StringConcatenationCallNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.StubNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ThrowExceptionNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.TryExpressionEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.TryExpressionExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.TryMainBlockEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.TryMainBlockExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.TypeOperatorCallNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.WhenBranchConditionEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.WhenBranchConditionExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.WhenBranchResultEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.WhenBranchResultExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.WhenEnterNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.WhenExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.WhenSubjectExpressionExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.WhenSyntheticElseBranchNode
import org.jetbrains.kotlin.kotlinx.collections.immutable.PersistentMap

class WideningPathAwareControlFlowGraphVisitor<K : Any, D : Any>(
    val visitor: PathAwareControlFlowGraphVisitor<K, D>,
    val widening: Widening<D>
) : PathAwareControlFlowGraphVisitor<K, D>() {
    override fun mergeInfo(
        a: ControlFlowInfo<K, D>,
        b: ControlFlowInfo<K, D>,
        node: CFGNode<*>
    ): ControlFlowInfo<K, D> = visitor.mergeInfo(a, b, node)

    private fun widen(
        previous: PathAwareControlFlowInfo<K, D>,
        current: PathAwareControlFlowInfo<K, D>
    ): PathAwareControlFlowInfo<K, D> {
        val b = current.builder()
        b.mapValuesTo(b) { (path, info) ->
            val bi = info.builder()
            previous[path]?.let {
                (it as ControlFlowInfo<K, D>).forEach { (key, prev) ->
                    info[key]?.let {
                        bi.put(key as K, widening.apply(prev as D, it as D))
                    }
                }
            }
            bi.build()
        }

        return b.build()
    }

    override fun visitNode(
        node: CFGNode<*>,
        data: PathAwareControlFlowInfo<K, D>
    ): PathAwareControlFlowInfo<K, D> = when (node) {
        is FakeExpressionEnterNode -> super.visitNode(node, data)
        is AnonymousFunctionCaptureNode -> visitor.visitAnonymousFunctionCaptureNode(node, data)
        is AnonymousObjectExpressionExitNode -> visitor.visitAnonymousObjectExpressionExitNode(node, data)
        is BlockEnterNode -> visitor.visitBlockEnterNode(node, data)
        is BlockExitNode -> visitor.visitBlockExitNode(node, data)
        is BooleanOperatorEnterNode -> visitor.visitBooleanOperatorEnterNode(node, data)
        is BooleanOperatorEnterRightOperandNode -> visitor.visitBooleanOperatorEnterRightOperandNode(node, data)
        is BooleanOperatorExitLeftOperandNode -> visitor.visitBooleanOperatorExitLeftOperandNode(node, data)
        is BooleanOperatorExitNode -> visitor.visitBooleanOperatorExitNode(node, data)
        is AnonymousFunctionExpressionNode -> visitor.visitAnonymousFunctionExpressionNode(node, data)
        is AnonymousObjectEnterNode -> visitor.visitAnonymousObjectEnterNode(node, data)
        is EnterValueParameterNode -> visitor.visitEnterValueParameterNode(node, data)
        is LocalClassExitNode -> visitor.visitLocalClassExitNode(node, data)
        is LocalFunctionDeclarationNode -> visitor.visitLocalFunctionDeclarationNode(node, data)
        is ClassEnterNode -> visitor.visitClassEnterNode(node, data)
        is ClassExitNode -> visitor.visitClassExitNode(node, data)
        is FileEnterNode -> visitor.visitFileEnterNode(node, data)
        is ScriptEnterNode -> visitor.visitScriptEnterNode(node, data)
        is SplitPostponedLambdasNode -> visitor.visitSplitPostponedLambdasNode(node, data)
        is CallableReferenceNode -> visitor.visitCallableReferenceNode(node, data)
        is CatchClauseEnterNode -> visitor.visitCatchClauseEnterNode(node, data)
        is CatchClauseExitNode -> visitor.visitCatchClauseExitNode(node, data)
        is CheckNotNullCallNode -> visitor.visitCheckNotNullCallNode(node, data)
        is CodeFragmentEnterNode -> visitor.visitCodeFragmentEnterNode(node, data)
        is CodeFragmentExitNode -> visitor.visitCodeFragmentExitNode(node, data)
        is ComparisonExpressionNode -> visitor.visitComparisonExpressionNode(node, data)
        is DelegateExpressionExitNode -> visitor.visitDelegateExpressionExitNode(node, data)
        is DelegatedConstructorCallNode -> visitor.visitDelegatedConstructorCallNode(node, data)
        is ElvisExitNode -> visitor.visitElvisExitNode(node, data)
        is ElvisLhsExitNode -> visitor.visitElvisLhsExitNode(node, data)
        is ElvisLhsIsNotNullNode -> visitor.visitElvisLhsIsNotNullNode(node, data)
        is ElvisRhsEnterNode -> visitor.visitElvisRhsEnterNode(node, data)
        is EnterDefaultArgumentsNode -> visitor.visitEnterDefaultArgumentsNode(node, data)
        is EnterSafeCallNode -> visitor.visitEnterSafeCallNode(node, data)
        is EqualityOperatorCallNode -> visitor.visitEqualityOperatorCallNode(node, data)
        is ExitDefaultArgumentsNode -> visitor.visitExitDefaultArgumentsNode(node, data)
        is ExitSafeCallNode -> visitor.visitExitSafeCallNode(node, data)
        is ExitValueParameterNode -> visitor.visitExitValueParameterNode(node, data)
        is FieldInitializerEnterNode -> visitor.visitFieldInitializerEnterNode(node, data)
        is FieldInitializerExitNode -> visitor.visitFieldInitializerExitNode(node, data)
        is FileExitNode -> visitor.visitFileExitNode(node, data)
        is FinallyBlockEnterNode -> visitor.visitFinallyBlockEnterNode(node, data)
        is FinallyBlockExitNode -> visitor.visitFinallyBlockExitNode(node, data)
        is FunctionCallArgumentsEnterNode -> visitor.visitFunctionCallArgumentsEnterNode(node, data)
        is FunctionCallArgumentsExitNode -> visitor.visitFunctionCallArgumentsExitNode(node, data)
        is FunctionCallEnterNode -> visitor.visitFunctionCallEnterNode(node, data)
        is FunctionCallExitNode -> visitor.visitFunctionCallExitNode(node, data)
        is FunctionEnterNode -> visitor.visitFunctionEnterNode(node, data)
        is FunctionExitNode -> visitor.visitFunctionExitNode(node, data)
        is GetClassCallNode -> visitor.visitGetClassCallNode(node, data)
        is InitBlockEnterNode -> visitor.visitInitBlockEnterNode(node, data)
        is InitBlockExitNode -> visitor.visitInitBlockExitNode(node, data)
        is JumpNode -> visitor.visitJumpNode(node, data)
        is LiteralExpressionNode -> visitor.visitLiteralExpressionNode(node, data)
        is LoopBlockEnterNode -> visitor.visitLoopBlockEnterNode(node, data)
        is LoopBlockExitNode -> visitor.visitLoopBlockExitNode(node, data)
        is LoopConditionEnterNode -> visitor.visitLoopConditionEnterNode(node, data)
        is LoopConditionExitNode -> visitor.visitLoopConditionExitNode(node, data)
        is LoopEnterNode -> visitor.visitLoopEnterNode(node, data)
        is LoopExitNode -> visitor.visitLoopExitNode(node, data)
        is MergePostponedLambdaExitsNode -> visitor.visitMergePostponedLambdaExitsNode(node, data)
        is PostponedLambdaExitNode -> visitor.visitPostponedLambdaExitNode(node, data)
        is PropertyInitializerEnterNode -> visitor.visitPropertyInitializerEnterNode(node, data)
        is PropertyInitializerExitNode -> visitor.visitPropertyInitializerExitNode(node, data)
        is QualifiedAccessNode -> visitor.visitQualifiedAccessNode(node, data)
        is ReplSnippetEnterNode -> visitor.visitReplSnippetEnterNode(node, data)
        is ReplSnippetExitNode -> visitor.visitReplSnippetExitNode(node, data)
        is ResolvedQualifierNode -> visitor.visitResolvedQualifierNode(node, data)
        is ScriptExitNode -> visitor.visitScriptExitNode(node, data)
        is SmartCastExpressionExitNode -> visitor.visitSmartCastExpressionExitNode(node, data)
        is StringConcatenationCallNode -> visitor.visitStringConcatenationCallNode(node, data)
        is StubNode -> visitor.visitStubNode(node, data)
        is ThrowExceptionNode -> visitor.visitThrowExceptionNode(node, data)
        is TryExpressionEnterNode -> visitor.visitTryExpressionEnterNode(node, data)
        is TryExpressionExitNode -> visitor.visitTryExpressionExitNode(node, data)
        is TryMainBlockEnterNode -> visitor.visitTryMainBlockEnterNode(node, data)
        is TryMainBlockExitNode -> visitor.visitTryMainBlockExitNode(node, data)
        is TypeOperatorCallNode -> visitor.visitTypeOperatorCallNode(node, data)
        is VariableAssignmentNode -> visitor.visitVariableAssignmentNode(node, data)
        is VariableDeclarationNode -> visitor.visitVariableDeclarationNode(node, data)
        is WhenBranchConditionEnterNode -> visitor.visitWhenBranchConditionEnterNode(node, data)
        is WhenBranchConditionExitNode -> visitor.visitWhenBranchConditionExitNode(node, data)
        is WhenBranchResultEnterNode -> visitor.visitWhenBranchResultEnterNode(node, data)
        is WhenBranchResultExitNode -> visitor.visitWhenBranchResultExitNode(node, data)
        is WhenEnterNode -> visitor.visitWhenEnterNode(node, data)
        is WhenExitNode -> visitor.visitWhenExitNode(node, data)
        is WhenSubjectExpressionExitNode -> visitor.visitWhenSubjectExpressionExitNode(node, data)
        is WhenSyntheticElseBranchNode -> visitor.visitWhenSyntheticElseBranchNode(node, data)
    }.let { widen(data, it) }
}