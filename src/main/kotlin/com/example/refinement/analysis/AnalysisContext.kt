package com.example.refinement.analysis

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.resolve.dfa.VariableStorage

class AnalysisContext(
    val checkerContext: CheckerContext,
    val reporter: DiagnosticReporter,
    val messageCollector: MessageCollector
) {
    val session = checkerContext.session
    val variables = VariableStorage(checkerContext.session)
}