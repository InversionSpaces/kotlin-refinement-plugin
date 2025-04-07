package com.example.refinement

import com.example.refinement.RefinementDiagnostics.CONTAINING_DECLARATION
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.warning1
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

object RefinementDiagnostics {
    val CONTAINING_DECLARATION by warning1<PsiElement, FirBasedSymbol<*>>()

    init {
        RootDiagnosticRendererFactory.registerFactory(RefinementDiagnosticRender)
    }
}

object RefinementDiagnosticRender : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap
        get() = KtDiagnosticFactoryToRendererMap("Refinement").apply {
            put(CONTAINING_DECLARATION, "Containing declaration: {0}", FirDiagnosticRenderers.DECLARATION_NAME)
        }
}