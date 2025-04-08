package com.example.refinement

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class FirRefinementExtensionRegistrar(
    private val annotations: List<String>
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +RefinementPredicateMatcher.getFactory(annotations)
        +::FirRefinementAdditionalCheckersExtension
    }
}