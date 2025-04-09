package com.example.refinement

import com.example.refinement.RefinementDiagnostics.ONLY_PRIMARY_CONSTRUCTORS_SUPPORTED
import com.example.refinement.RefinementDiagnostics.ONLY_VALUE_CLASSES_ARE_SUPPORTED
import com.example.refinement.RefinementDiagnostics.UNSUPPORTED_MULTIPLE_REQUIRE_CALLS
import com.example.refinement.RefinementDiagnostics.UNSUPPORTED_PREDICATE
import com.example.refinement.RefinementDiagnostics.UNSUPPORTED_TYPE
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getConstructedClass
import org.jetbrains.kotlin.fir.declarations.isInlineOrValueClass
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.argument
import org.jetbrains.kotlin.fir.references.toResolvedSymbol
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.isInt

object FirRefinementConstructorCallChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    private fun analyseExpr(
        parameter: FirValueParameterSymbol,
        expr: FirExpression,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ): RefinementClassInfo = when (expr) {
//        is FirComparisonExpression -> {
//            UnsupportedRefinement
//        }
        else -> {
            reporter.reportOn(expr.source, UNSUPPORTED_PREDICATE, context)
            UnsupportedRefinement
        }
    }

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

        val reqs = decl.declarations.flatMap {
            (it as? FirAnonymousInitializer)?.body?.statements ?: emptyList()
        }.mapNotNull { it as? FirFunctionCall }.filter {
            // TODO: Better match with require?
            it.calleeReference.toResolvedSymbol<FirNamedFunctionSymbol>()?.name?.asString() == "require"
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

        return analyseExpr(parameter, reqs.single().argument, context, reporter)
    }

    override fun check(
        expression: FirFunctionCall,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val ctor = expression.calleeReference.toResolvedSymbol<FirConstructorSymbol>() ?: return
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

        if (info !is IntervalRefinement) return




//                val declaration = context.findClosest<FirFunction>()
//                if (declaration == null) {
//                    return reporter.reportOn(expression.source, NO_CONTAINING_DECLARATION, context)
//                }
//                reporter.reportOn(expression.source, CONTAINING_DECLARATION, declaration.symbol, context)
//
//                val cfg = declaration.controlFlowGraphReference?.controlFlowGraph ?: return
//                reporter.reportOn(expression.source, RefinementDiagnostics.CFG_GRAPH, cfg.render(), context)
    }
}