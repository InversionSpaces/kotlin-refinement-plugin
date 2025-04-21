package com.example.refinement

import com.example.refinement.RefinementDiagnostics.DEDUCED_CORRECTNESS
import com.example.refinement.RefinementDiagnostics.DEDUCED_INCORRECTNESS
import com.example.refinement.RefinementDiagnostics.FAILED_TO_DEDUCE_CORRECTNESS
import com.example.refinement.RefinementDiagnostics.FAILED_TO_GET_UNDERLYING_VALUE
import com.example.refinement.RefinementDiagnostics.ONLY_PRIMARY_CONSTRUCTORS_SUPPORTED
import com.example.refinement.RefinementDiagnostics.ONLY_VALUE_CLASSES_ARE_SUPPORTED
import com.example.refinement.RefinementDiagnostics.UNSUPPORTED_MULTIPLE_REQUIRE_CALLS
import com.example.refinement.RefinementDiagnostics.UNSUPPORTED_PREDICATE
import com.example.refinement.RefinementDiagnostics.UNSUPPORTED_TYPE
import com.example.refinement.analysis.IntervalAnalysisVisitor
import com.example.refinement.analysis.evaluate
import com.example.refinement.analysis.interpretComparison
import com.example.refinement.fir.REQUIRE_CALLABLE_ID
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
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
import org.jetbrains.kotlin.fir.references.toResolvedSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.isInt

class FirRefinementConstructorCallChecker(
    private val messageCollector: MessageCollector
) : FirFunctionCallChecker(MppCheckerKind.Common) {
    private fun analyseClass(
        ctor: FirConstructorSymbol,
        decl: FirRegularClass,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ): RefinementClassInfo {
        val parameter = ctor.valueParameterSymbols.singleOrNull() ?: return UnsupportedRefinement
        val parameterType = parameter.resolvedReturnTypeRef.coneType
        if (!parameterType.isInt) {
            reporter.reportOn(parameter.source, UNSUPPORTED_TYPE, parameterType, context)
            return UnsupportedRefinement
        }

        val property = decl.declarations.filterIsInstance<FirProperty>().singleOrNull {
            (it.initializer as? FirPropertyAccessExpression)?.calleeReference?.let {
                (it as? FirPropertyFromParameterResolvedNamedReference)?.resolvedSymbol == parameter
            } == true
        }?.symbol ?: run {
            reporter.reportOn(decl.source, FAILED_TO_GET_UNDERLYING_VALUE, context)
            return UnsupportedRefinement
        }

        val reqs = decl.declarations.flatMap {
            (it as? FirAnonymousInitializer)?.body?.statements ?: emptyList()
        }.mapNotNull { it as? FirFunctionCall }.filter {
            it.calleeReference.toResolvedNamedFunctionSymbol()?.callableId == REQUIRE_CALLABLE_ID
        }

        if (reqs.isEmpty()) {
            return NoRefinement
        }

        if (reqs.size > 1) {
            reqs.forEach {
                reporter.reportOn(it.source, UNSUPPORTED_MULTIPLE_REQUIRE_CALLS, context)
            }

            return UnsupportedRefinement
        }

        val condition = reqs.single().argument
        val refinement = (condition as? FirComparisonExpression)?.let {
            interpretComparison(it)
        }?.let { (arg, interval) ->
            interval.takeIf { arg == property }?.toRefinement()
        } ?: run {
            reporter.reportOn(condition.source, UNSUPPORTED_PREDICATE, context)
            return UnsupportedRefinement
        }

        return ParameterRefinement(parameter, refinement)
    }

    override fun check(
        expression: FirFunctionCall,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val ctor = expression.calleeReference.toResolvedConstructorSymbol() ?: return
        val klass = ctor.getConstructedClass(context.session) ?: return
        val matcher = context.session.refinementPredicateMatcher
        if (!matcher.isAnnotated(klass)) return
        if (!klass.isInlineOrValueClass()) {
            return reporter.reportOn(klass.source, ONLY_VALUE_CLASSES_ARE_SUPPORTED, context)
        }
        if (!ctor.isPrimary) {
            return reporter.reportOn(expression.source, ONLY_PRIMARY_CONSTRUCTORS_SUPPORTED, context)
        }

        val decl = context.session.firProvider.getFirClassifierByFqName(klass.classId) as? FirRegularClass ?: return
        val info: RefinementClassInfo = decl.refinementClassInfo ?: run {
            analyseClass(ctor, decl, context, reporter).also { decl.refinementClassInfo = it }
        }

        if (info !is ParameterRefinement) return
        
        val failed = {
            reporter.reportOn(expression.source, FAILED_TO_DEDUCE_CORRECTNESS, context)
        }

        val cfg = context.findClosest<FirControlFlowGraphOwner> {
            it.controlFlowGraphReference?.controlFlowGraph != null
        }?.controlFlowGraphReference?.controlFlowGraph ?: return failed()
        val analysis = cfg.traverseToFixedPoint(IntervalAnalysisVisitor(messageCollector))
        val analysisInfo = analysis.mapKeys { (it, _) -> it.fir }[expression] ?: return failed()

        val args = expression.argumentList as? FirResolvedArgumentList ?: return failed()
        val paramExpr = args.mapping.mapNotNull { (expr, param) ->
            expr.takeIf { param.symbol == info.parameter }
        }.singleOrNull() ?: return failed()

        val interval = analysisInfo.evaluate(paramExpr)?.toRefinement() ?: return failed()
        if (interval == info.refinement) {
            reporter.reportOn(expression.source, DEDUCED_CORRECTNESS, context)
        } else {
            reporter.reportOn(expression.source, DEDUCED_INCORRECTNESS, context)
        }
    }
}