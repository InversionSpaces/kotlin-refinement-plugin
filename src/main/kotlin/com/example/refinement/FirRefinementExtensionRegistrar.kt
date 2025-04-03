package com.example.refinement

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class FirRefinementExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +FirRefinementAdditionalCheckersExtension.getFactory()
    }
}