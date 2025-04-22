package com.example.refinement

import com.example.refinement.RefinementDiagnostics.DEBUG_INFO
import com.example.refinement.RefinementDiagnostics.DEDUCED_CORRECTNESS
import com.example.refinement.RefinementDiagnostics.DEDUCED_INCORRECTNESS
import com.example.refinement.RefinementDiagnostics.FAILED_TO_DEDUCE_CORRECTNESS
import com.example.refinement.RefinementDiagnostics.FAILED_TO_GET_UNDERLYING_VALUE
import com.example.refinement.RefinementDiagnostics.NO_PRIMARY_CONSTRUCTOR_FOUND
import com.example.refinement.RefinementDiagnostics.ONLY_VALUE_CLASSES_ARE_SUPPORTED
import com.example.refinement.RefinementDiagnostics.NO_SINGLE_VALUE_PARAMETER
import com.example.refinement.RefinementDiagnostics.UNSUPPORTED_MULTIPLE_REQUIRE_CALLS
import com.example.refinement.RefinementDiagnostics.UNSUPPORTED_PREDICATE
import com.example.refinement.RefinementDiagnostics.UNSUPPORTED_TYPE
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.warning0
import org.jetbrains.kotlin.diagnostics.warning1
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.types.ConeKotlinType

object RefinementDiagnostics {
    val ONLY_VALUE_CLASSES_ARE_SUPPORTED by error0<PsiElement>()
    val NO_PRIMARY_CONSTRUCTOR_FOUND by error0<PsiElement>()
    val NO_SINGLE_VALUE_PARAMETER by error0<PsiElement>()
    val FAILED_TO_GET_UNDERLYING_VALUE by error0<PsiElement>()
    val UNSUPPORTED_TYPE by error1<PsiElement, ConeKotlinType>()
    val UNSUPPORTED_MULTIPLE_REQUIRE_CALLS by error0<PsiElement>()
    val UNSUPPORTED_PREDICATE by error0<PsiElement>()

    val FAILED_TO_DEDUCE_CORRECTNESS by warning0<PsiElement>()
    val DEDUCED_CORRECTNESS by warning0<PsiElement>()
    val DEDUCED_INCORRECTNESS by warning0<PsiElement>()

    val DEBUG_INFO by warning1<PsiElement, String>()

    init {
        RootDiagnosticRendererFactory.registerFactory(RefinementDiagnosticRender)
    }
}

object RefinementDiagnosticRender : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap
        get() = KtDiagnosticFactoryToRendererMap("Refinement").apply {
            put(ONLY_VALUE_CLASSES_ARE_SUPPORTED, "Only value classes are supported")
            put(NO_PRIMARY_CONSTRUCTOR_FOUND, "No primary constructor found")
            put(NO_SINGLE_VALUE_PARAMETER, "Primary constructor should have one value parameter")
            put(FAILED_TO_GET_UNDERLYING_VALUE, "Failed to get underlying value")
            put(UNSUPPORTED_TYPE, "Unsupported type: {0}", FirDiagnosticRenderers.RENDER_TYPE)
            put(UNSUPPORTED_MULTIPLE_REQUIRE_CALLS, "Multiple `require` calls are not supported")
            put(UNSUPPORTED_PREDICATE, "Unsupported predicate")
            put(FAILED_TO_DEDUCE_CORRECTNESS, "Failed to deduce correctness of constructor call")
            put(DEDUCED_CORRECTNESS, "Constructor call is correct")
            put(DEDUCED_INCORRECTNESS, "Constructor call is incorrect")

            put(DEBUG_INFO, "DEBUG: {0}", CommonRenderers.STRING)
        }
}