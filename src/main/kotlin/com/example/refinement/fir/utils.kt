package com.example.refinement.fir

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.toResolvedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.isInt
import org.jetbrains.kotlin.fir.types.resolvedType


val FirExpression.literalIntValue: Long?
    get() = (this as? FirLiteralExpression)?.takeIf { it.resolvedType.isInt }?.value as? Long

val FirExpression.propertyAccessSymbol: FirPropertySymbol?
    get() = (this as? FirPropertyAccessExpression)?.calleeReference?.toResolvedSymbol<FirPropertySymbol>()