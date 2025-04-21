package com.example.refinement

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class FirRefinementExtensionRegistrar(
    private val annotations: List<String>,
    private val messageCollector: MessageCollector,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +RefinementPredicateMatcher.getFactory(annotations)
        +FirRefinementAdditionalCheckersExtension.getFactory(messageCollector)
    }
}