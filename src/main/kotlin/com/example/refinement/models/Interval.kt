package com.example.refinement.models

import java.math.BigInteger
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed interface InfInt : Comparable<InfInt> {
    data class Finite(val value: BigInteger) : InfInt {
        override fun isNegative(): Boolean = value.signum() < 0
        override fun isPositive(): Boolean = value.signum() > 0
        override fun isZero(): Boolean = value.signum() == 0

        override fun isFinite(): Boolean = true

        override fun toString(): String = value.toString()
    }

    data object PosInf : InfInt {
        override fun isNegative(): Boolean = false
        override fun isPositive(): Boolean = true
        override fun isZero(): Boolean = false

        override fun isFinite(): Boolean = false

        override fun toString(): String = "+inf"
    }

    data object NegInf : InfInt {
        override fun isNegative(): Boolean = true
        override fun isPositive(): Boolean = false
        override fun isZero(): Boolean = false

        override fun isFinite(): Boolean = false

        override fun toString(): String = "-inf"
    }

    fun isNegative(): Boolean
    fun isPositive(): Boolean
    fun isZero(): Boolean

    fun isFinite(): Boolean

    override fun compareTo(other: InfInt): Int = when (this) {
        is PosInf -> if (other is PosInf) 0 else 1
        is NegInf -> if (other is NegInf) 0 else -1
        is Finite -> when (other) {
            is PosInf -> -1
            is NegInf -> 1
            is Finite -> value.compareTo(other.value)
        }
    }

    operator fun plus(other: InfInt): InfInt = when {
        this is PosInf && other !is NegInf -> this
        this is NegInf && other !is PosInf -> this
        this is Finite -> if (other is Finite) finite(value + other.value) else other
        else -> throw IllegalArgumentException("Cannot add $this and $other")
    }

    operator fun unaryMinus(): InfInt = when (this) {
        is PosInf -> negInf()
        is NegInf -> posInf()
        is Finite -> finite(-value)
    }

    operator fun minus(other: InfInt): InfInt = this + -other

    operator fun times(other: InfInt): InfInt = when {
        isZero() || other.isZero() -> finite(0)
        this is Finite && other is Finite -> finite(value * other.value)
        isNegative() xor other.isNegative() -> negInf()
        else -> posInf()
    }

    companion object {
        fun finite(value: Long) = Finite(value.toBigInteger())
        fun finite(value: BigInteger) = Finite(value)
        fun posInf() = PosInf
        fun negInf() = NegInf
    }
}

data class IntervalRefinement(val from: InfInt, val to: InfInt) {
    init {
        require(from <= to) { "`from` must be less than or equal to `to` (got `$from` and `$to`)" }
    }

    fun toLattice(): IntervalLattice = IntervalLattice.Bounded(from, to)

    companion object {
        fun boundedBelow(from: Long, including: Boolean = true): IntervalRefinement =
            from.toBigInteger().let {
                if (including) it else it + BigInteger.ONE
            }.let {
                IntervalRefinement(InfInt.finite(it), InfInt.posInf())
            }


        fun boundedAbove(to: Long, including: Boolean = true): IntervalRefinement =
            to.toBigInteger().let {
                if (including) it else it - BigInteger.ONE
            }.let {
                IntervalRefinement(InfInt.negInf(), InfInt.finite(it))
            }

        fun exact(value: Long): IntervalRefinement =
            InfInt.finite(value).let { IntervalRefinement(it, it) }
    }
}

sealed interface IntervalLattice {
    data object Undefined : IntervalLattice {
        override fun toString(): String = "<undefined>"
    }

    data class Bounded(val from: InfInt, val to: InfInt) : IntervalLattice {
        init {
            require(from <= to) { "`from` must be less than or equal `to` (got `$from` and `$to`)" }
        }

        override fun toString(): String {
            val builder = StringBuilder()
            builder.append(if (from.isFinite()) "[" else "(")
            builder.append(from)
            builder.append(", ")
            builder.append(to)
            builder.append(if (to.isFinite()) "]" else ")")
            return builder.toString()
        }
    }

    fun isUndefined(): Boolean = this is Undefined

    operator fun plus(other: IntervalLattice): IntervalLattice = liftOp(other) {
        Bounded(from + it.from, to + it.to)
    }

    operator fun minus(other: IntervalLattice): IntervalLattice = this + -other

    operator fun times(other: IntervalLattice): IntervalLattice = liftOp(other) {
        val prods = listOf(it.from, it.to).flatMap { listOf(from, to).map { f -> f * it } }
        Bounded(prods.min(), prods.max())
    }

    operator fun unaryMinus(): IntervalLattice = when (this) {
        is Bounded -> Bounded(-to, -from)
        Undefined -> Undefined
    }

    fun isLessOrEqualTo(other: IntervalLattice): Boolean =
        join(this, other) == other

    private fun liftOp(other: IntervalLattice, op: Bounded.(Bounded) -> IntervalLattice): IntervalLattice =
        if (this is Bounded && other is Bounded) this.op(other) else Undefined

    companion object {
        val unbounded: IntervalLattice = Bounded(InfInt.negInf(), InfInt.posInf())

        fun join(left: IntervalLattice, right: IntervalLattice): IntervalLattice = when {
            left is Bounded && right is Bounded -> Bounded(
                minOf(left.from, right.from),
                maxOf(left.to, right.to)
            )

            left is Bounded -> left
            right is Bounded -> right
            else -> Undefined
        }

        fun meet(left: IntervalLattice, right: IntervalLattice): IntervalLattice = when {
            left is Bounded && right is Bounded -> {
                val from = maxOf(left.from, right.from)
                val to = minOf(left.to, right.to)
                if (from < to) Bounded(from, to) else Undefined
            }

            else -> Undefined
        }

        fun exact(literal: Long): IntervalLattice =
            InfInt.finite(literal).let { Bounded(it, it) }
    }
}