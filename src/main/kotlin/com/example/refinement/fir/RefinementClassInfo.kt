package com.example.refinement.fir

import com.example.refinement.models.IntervalRefinement
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol

sealed interface RefinementClassInfo

object UnsupportedRefinement : RefinementClassInfo

object NoRefinement : RefinementClassInfo

class ParameterRefinement(
    val parameter: FirValueParameterSymbol,
    val refinement: IntervalRefinement
) : RefinementClassInfo

object RefinementDataKey : FirDeclarationDataKey()

var FirRegularClass.refinementClassInfo: RefinementClassInfo?
        by FirDeclarationDataRegistry.data(RefinementDataKey)

val FirRegularClassSymbol.refinementClassInfo: RefinementClassInfo?
        by FirDeclarationDataRegistry.symbolAccessor(RefinementDataKey)