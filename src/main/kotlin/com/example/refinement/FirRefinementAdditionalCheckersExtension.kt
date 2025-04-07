package com.example.refinement

import com.example.refinement.RefinementDiagnostics.CONTAINING_DECLARATION
import com.example.refinement.RefinementDiagnostics.NO_CONTAINING_DECLARATION
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.diagnostics.warning0
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.cfa.nearestNonInPlaceGraph
import org.jetbrains.kotlin.fir.analysis.cfa.util.traverseToFixedPoint
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.resolve.dfa.LogicSystem
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.render
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph

class FirRefinementAdditionalCheckersExtension(session: FirSession): FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val callCheckers: Set<FirCallChecker>
            get() = setOf(callChecker)
    }

    companion object {
        val callChecker: FirCallChecker = object : FirCallChecker(MppCheckerKind.Common) {
            override fun check(
                expression: FirCall,
                context: CheckerContext,
                reporter: DiagnosticReporter
            ) {
                val declaration = context.findClosest<FirFunction>()
                if (declaration == null) {
                    return reporter.reportOn(expression.source, NO_CONTAINING_DECLARATION, context)
                }
                reporter.reportOn(expression.source, CONTAINING_DECLARATION, declaration.symbol, context)

                val cfg = declaration.controlFlowGraphReference?.controlFlowGraph ?: return
                reporter.reportOn(expression.source, RefinementDiagnostics.CFG_GRAPH, cfg.render(), context)
            }
        }
    }
}