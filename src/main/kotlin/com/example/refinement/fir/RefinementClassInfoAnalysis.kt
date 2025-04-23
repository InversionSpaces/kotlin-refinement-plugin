package com.example.refinement.fir

import com.example.refinement.RefinementDiagnostics.FAILED_TO_GET_UNDERLYING_VALUE
import com.example.refinement.RefinementDiagnostics.NO_PRIMARY_CONSTRUCTOR_FOUND
import com.example.refinement.RefinementDiagnostics.NO_SINGLE_VALUE_PARAMETER
import com.example.refinement.RefinementDiagnostics.ONLY_VALUE_CLASSES_ARE_SUPPORTED
import com.example.refinement.RefinementDiagnostics.UNSUPPORTED_MULTIPLE_REQUIRE_CALLS
import com.example.refinement.RefinementDiagnostics.UNSUPPORTED_PREDICATE
import com.example.refinement.RefinementDiagnostics.UNSUPPORTED_TYPE
import com.example.refinement.analysis.interpretComparison
import com.example.refinement.refinementPredicateMatcher
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.isInlineOrValueClass
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.expressions.FirComparisonExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.argument
import org.jetbrains.kotlin.fir.references.impl.FirPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.isInt

internal fun analyse(
    decl: FirRegularClass,
    context: CheckerContext,
    reporter: DiagnosticReporter
): RefinementClassInfo {
    if (!decl.symbol.isInlineOrValueClass()) {
        reporter.reportOn(decl.source, ONLY_VALUE_CLASSES_ARE_SUPPORTED, context)
        return UnsupportedRefinement
    }

    val ctor = decl.primaryConstructorIfAny(context.session) ?: run {
        reporter.reportOn(decl.source, NO_PRIMARY_CONSTRUCTOR_FOUND, context)
        return UnsupportedRefinement
    }

    val parameter = ctor.valueParameterSymbols.singleOrNull() ?: run {
        reporter.reportOn(ctor.source, NO_SINGLE_VALUE_PARAMETER, context)
        return UnsupportedRefinement
    }

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
        interpretComparison(it) { it.propertyAccessSymbol }
    }?.let { (arg, interval) ->
        interval.takeIf { arg == property }
    } ?: run {
        reporter.reportOn(condition.source, UNSUPPORTED_PREDICATE, context)
        return UnsupportedRefinement
    }

    return ParameterRefinement(parameter, refinement)
}

fun FirRegularClassSymbol.getRefinementClassInfo(
    context: CheckerContext,
    reporter: DiagnosticReporter,
): RefinementClassInfo? {
    val matcher = context.session.refinementPredicateMatcher
    if (!matcher.isAnnotated(this)) return null

    return refinementClassInfo ?: run {
        val provider = context.session.firProvider
        // TODO: Can it be not FirRegularClass?
        val decl = provider.getFirClassifierByFqName(classId) as? FirRegularClass ?: return null
        analyse(decl, context, reporter).also { decl.refinementClassInfo = it }
    }
}