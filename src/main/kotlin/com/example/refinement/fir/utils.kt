package com.example.refinement.fir

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.references.toResolvedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.isInt
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

val FirExpression.literalIntValue: Long?
    get() = (this as? FirLiteralExpression)?.takeIf { it.resolvedType.isInt }?.value as? Long

val FirExpression.propertyAccess: FirPropertyAccessExpression?
    get() = this as? FirPropertyAccessExpression

val FirExpression.propertyAccessSymbol: FirPropertySymbol?
    get() = propertyAccess?.calleeReference?.toResolvedPropertySymbol()

val REQUIRE_CALLABLE_ID = CallableId(
    packageName = FqName("kotlin"),
    callableName = Name.identifier("require")
)

val PLUS_CALLABLE_ID = CallableId(
    packageName = FqName("kotlin"),
    className = FqName("Int"),
    callableName = Name.identifier("plus")
)

val MINUS_CALLABLE_ID = CallableId(
    packageName = FqName("kotlin"),
    className = FqName("Int"),
    callableName = Name.identifier("minus")
)

val TIMES_CALLABLE_ID = CallableId(
    packageName = FqName("kotlin"),
    className = FqName("Int"),
    callableName = Name.identifier("times")
)