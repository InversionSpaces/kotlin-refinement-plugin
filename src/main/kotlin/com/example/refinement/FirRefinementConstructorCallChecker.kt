package com.example.refinement

import com.example.refinement.RefinementDiagnostics.DEBUG_INFO
import com.example.refinement.RefinementDiagnostics.FAILED_TO_DEDUCE_CORRECTNESS
import com.example.refinement.RefinementDiagnostics.FAILED_TO_GET_UNDERLYING_VALUE
import com.example.refinement.RefinementDiagnostics.ONLY_PRIMARY_CONSTRUCTORS_SUPPORTED
import com.example.refinement.RefinementDiagnostics.ONLY_VALUE_CLASSES_ARE_SUPPORTED
import com.example.refinement.RefinementDiagnostics.UNSUPPORTED_MULTIPLE_REQUIRE_CALLS
import com.example.refinement.RefinementDiagnostics.UNSUPPORTED_PREDICATE
import com.example.refinement.RefinementDiagnostics.UNSUPPORTED_TYPE
import com.example.refinement.models.IntervalRefinement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getConstructedClass
import org.jetbrains.kotlin.fir.declarations.isInlineOrValueClass
import org.jetbrains.kotlin.fir.expressions.FirComparisonExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.argument
import org.jetbrains.kotlin.fir.references.impl.FirPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.render
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.isInt
import org.jetbrains.kotlin.fir.types.resolvedType

object FirRefinementConstructorCallChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    private fun analyseExpr(
        property: FirPropertySymbol,
        expr: FirExpression,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ): IntervalRefinement? {
        val unsupported = {
            reporter.reportOn(expr.source, UNSUPPORTED_PREDICATE, context)
            null
        }

        if (expr !is FirComparisonExpression) return unsupported()

        val left = expr.compareToCall.dispatchReceiver ?: return unsupported()
        val right = expr.compareToCall.argument

        return when {
            left.literalIntValue == 0L  && right.propertyAccessSymbol == property -> {
                when (expr.operation) {
                    FirOperation.EQ -> IntervalRefinement.ZERO
                    FirOperation.GT -> IntervalRefinement.NEGATIVE
                    FirOperation.LT -> IntervalRefinement.POSITIVE
                    else -> unsupported()
                }
            }
            right.literalIntValue == 0L && left.propertyAccessSymbol == property -> {
                when (expr.operation) {
                    FirOperation.EQ -> IntervalRefinement.ZERO
                    FirOperation.GT -> IntervalRefinement.POSITIVE
                    FirOperation.LT -> IntervalRefinement.NEGATIVE
                    else -> unsupported()
                }
            }
            else -> unsupported()
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

        val refinement = analyseExpr(property, reqs.single().argument, context, reporter) ?: return UnsupportedRefinement

        return ParameterRefinement(parameter, refinement)
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

        if (info !is ParameterRefinement) return

        val declaration = context.findClosest<FirFunction>() ?: run {
            return reporter.reportOn(expression.source, FAILED_TO_DEDUCE_CORRECTNESS, context)
        }

        val cfg = declaration.controlFlowGraphReference?.controlFlowGraph ?: return
        reporter.reportOn(expression.source, DEBUG_INFO, cfg.render(), context)
    }

    val FirExpression.literalIntValue: Long?
        get() = (this as? FirLiteralExpression)?.takeIf { it.resolvedType.isInt }?.value as? Long

    val FirExpression.propertyAccessSymbol: FirPropertySymbol?
        get() = (this as? FirPropertyAccessExpression)?.calleeReference?.toResolvedSymbol<FirPropertySymbol>()
}