package com.example.refinement

import com.example.refinement.RefinementDiagnostics.ANNOTATED_CLASS
import com.example.refinement.RefinementDiagnostics.CONSTRUCTOR_CALL
import com.example.refinement.RefinementDiagnostics.CONTAINING_DECLARATION
import com.example.refinement.RefinementDiagnostics.DECLARATION_FOUND
import com.example.refinement.RefinementDiagnostics.NO_CONTAINING_DECLARATION
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getConstructedClass
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.references.toResolvedSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.render
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import kotlin.math.exp

class FirRefinementAdditionalCheckersExtension(session: FirSession): FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers: Set<FirFunctionCallChecker>
            get() = setOf(constructorChecker)
    }

    companion object {
        val constructorChecker: FirFunctionCallChecker = object : FirFunctionCallChecker(MppCheckerKind.Common) {
            override fun check(
                expression: FirFunctionCall,
                context: CheckerContext,
                reporter: DiagnosticReporter
            ) {
                val ctor = expression.calleeReference.toResolvedSymbol<FirConstructorSymbol>() ?: return
                reporter.reportOn(expression.source, CONSTRUCTOR_CALL, context)
                val klass = ctor.getConstructedClass(context.session) ?: return
                val matcher = context.session.refinementPredicateMatcher
                if (!matcher.isAnnotated(klass)) return
                val decl = context.session.firProvider.getFirClassifierByFqName(klass.classId) as? FirRegularClass ?: return

                decl.declarations.forEach { subDecl ->
                    if (subDecl !is FirAnonymousInitializer) return@forEach
                    reporter.reportOn(subDecl.source, DECLARATION_FOUND, context)
                }
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
    }
}