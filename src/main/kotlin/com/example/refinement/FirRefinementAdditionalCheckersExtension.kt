package com.example.refinement

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.diagnostics.warning0
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.arguments

class FirRefinementAdditionalCheckersExtension(session: FirSession): FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val callCheckers: Set<FirCallChecker>
            get() = setOf(callChecker)
    }

    companion object {
        fun getFactory(): Factory = Factory { session -> FirRefinementAdditionalCheckersExtension(session) }

        val callChecker: FirCallChecker = object : FirCallChecker(MppCheckerKind.Common) {
            override fun check(
                expression: FirCall,
                context: CheckerContext,
                reporter: DiagnosticReporter
            ) {
                reporter.reportOn(expression.source, TEST_ERROR, context)
            }
        }

        val TEST_ERROR by warning0<PsiElement>()
    }
}