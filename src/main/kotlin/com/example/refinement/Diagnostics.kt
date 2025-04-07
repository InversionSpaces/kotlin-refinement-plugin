package com.example.refinement

import com.example.refinement.RefinementDiagnostics.CFG_GRAPH
import com.example.refinement.RefinementDiagnostics.CONTAINING_DECLARATION
import com.example.refinement.RefinementDiagnostics.NO_CONTAINING_DECLARATION
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.warning0
import org.jetbrains.kotlin.diagnostics.warning1
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

object RefinementDiagnostics {
    val CONTAINING_DECLARATION by warning1<PsiElement, FirBasedSymbol<*>>()
    val NO_CONTAINING_DECLARATION by warning0<PsiElement>()

    val CFG_GRAPH by warning1<PsiElement, String>()

    init {
        RootDiagnosticRendererFactory.registerFactory(RefinementDiagnosticRender)
    }
}

object RefinementDiagnosticRender : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap
        get() = KtDiagnosticFactoryToRendererMap("Refinement").apply {
            put(CONTAINING_DECLARATION, "Containing declaration: {0}", FirDiagnosticRenderers.DECLARATION_NAME)
            put(NO_CONTAINING_DECLARATION, "No containing declaration found")
            put(CFG_GRAPH, "CFG graph: {0}", CommonRenderers.STRING)
        }
}