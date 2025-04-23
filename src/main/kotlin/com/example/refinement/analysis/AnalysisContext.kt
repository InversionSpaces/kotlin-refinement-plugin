package com.example.refinement.analysis

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowVariable
import org.jetbrains.kotlin.fir.resolve.dfa.RealVariable
import org.jetbrains.kotlin.fir.resolve.dfa.VariableStorage
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol

class AnalysisContext(
    val checkerContext: CheckerContext,
    val reporter: DiagnosticReporter,
    val messageCollector: MessageCollector
) {
    val session = checkerContext.session
    val variables = VariableStorage(checkerContext.session)

    fun getVariable(expr: FirExpression): DataFlowVariable? =
        variables.get(expr, createReal = true, unwrapAlias = { it })

    fun createLocalVariable(symbol: FirVariableSymbol<*>): DataFlowVariable {
        val variable = RealVariable.local(symbol)
        variables.remember(variable) // not necessary I guess
        return variable
    }
}