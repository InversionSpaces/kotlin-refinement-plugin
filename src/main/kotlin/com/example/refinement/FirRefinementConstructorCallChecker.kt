package com.example.refinement

import com.example.refinement.RefinementDiagnostics.DEDUCED_CORRECTNESS
import com.example.refinement.RefinementDiagnostics.DEDUCED_INCORRECTNESS
import com.example.refinement.RefinementDiagnostics.FAILED_TO_DEDUCE_CORRECTNESS
import com.example.refinement.RefinementDiagnostics.FAILED_TO_GET_UNDERLYING_VALUE
import com.example.refinement.RefinementDiagnostics.NO_PRIMARY_CONSTRUCTOR_FOUND
import com.example.refinement.RefinementDiagnostics.ONLY_VALUE_CLASSES_ARE_SUPPORTED
import com.example.refinement.RefinementDiagnostics.UNSUPPORTED_MULTIPLE_REQUIRE_CALLS
import com.example.refinement.RefinementDiagnostics.UNSUPPORTED_PREDICATE
import com.example.refinement.RefinementDiagnostics.UNSUPPORTED_TYPE
import com.example.refinement.analysis.IntervalAnalysisVisitor
import com.example.refinement.analysis.evaluate
import com.example.refinement.analysis.interpretComparison
import com.example.refinement.fir.NoRefinement
import com.example.refinement.fir.ParameterRefinement
import com.example.refinement.fir.REQUIRE_CALLABLE_ID
import com.example.refinement.fir.RefinementClassInfo
import com.example.refinement.fir.UnsupportedRefinement
import com.example.refinement.fir.getRefinementClassInfo
import com.example.refinement.fir.refinementClassInfo
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.cfa.util.traverseToFixedPoint
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getConstructedClass
import org.jetbrains.kotlin.fir.declarations.isInlineOrValueClass
import org.jetbrains.kotlin.fir.expressions.FirComparisonExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.argument
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.references.impl.FirPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedConstructorSymbol
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.isInt

class FirRefinementConstructorCallChecker(
    private val messageCollector: MessageCollector
) : FirFunctionCallChecker(MppCheckerKind.Common) {
    override fun check(
        expression: FirFunctionCall,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val ctor = expression.calleeReference.toResolvedConstructorSymbol() ?: return
        val klass = ctor.getConstructedClass(context.session) ?: return
        val info = klass.getRefinementClassInfo(context, reporter) ?: return

        if (!ctor.isPrimary) {
            return reporter.reportOn(expression.source, NO_PRIMARY_CONSTRUCTOR_FOUND, context)
        }

        if (info !is ParameterRefinement) return

        val failed = {
            reporter.reportOn(expression.source, FAILED_TO_DEDUCE_CORRECTNESS, context)
        }

        val cfg = context.findClosest<FirControlFlowGraphOwner> {
            it.controlFlowGraphReference?.controlFlowGraph != null
        }?.controlFlowGraphReference?.controlFlowGraph ?: return failed()
        val visitor = IntervalAnalysisVisitor(context, reporter, messageCollector)
        val analysis = cfg.traverseToFixedPoint(visitor)
        val analysisInfo = analysis.mapKeys { (it, _) -> it.fir }[expression] ?: return failed()

        val args = expression.argumentList as? FirResolvedArgumentList ?: return failed()
        val paramExpr = args.mapping.mapNotNull { (expr, param) ->
            expr.takeIf { param.symbol == info.parameter }
        }.singleOrNull() ?: return failed()

        val interval = analysisInfo.evaluate(
            paramExpr, context, reporter, messageCollector
        )?.toRefinement() ?: return failed()
        if (interval == info.refinement) {
            reporter.reportOn(expression.source, DEDUCED_CORRECTNESS, context)
        } else {
            reporter.reportOn(expression.source, DEDUCED_INCORRECTNESS, context)
        }
    }
}