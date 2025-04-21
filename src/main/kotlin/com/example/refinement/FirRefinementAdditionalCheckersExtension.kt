package com.example.refinement

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class FirRefinementAdditionalCheckersExtension(
    session: FirSession,
    messageCollector: MessageCollector,
) : FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers: Set<FirFunctionCallChecker>
            get() = setOf(FirRefinementConstructorCallChecker(messageCollector))
    }

    companion object {
        fun getFactory(messageCollector: MessageCollector): Factory {
            return Factory { session -> FirRefinementAdditionalCheckersExtension(session, messageCollector) }
        }
    }
}