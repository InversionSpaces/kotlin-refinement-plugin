package com.example.refinement.models

enum class IntervalRefinement {
    ZERO, POSITIVE, NEGATIVE
}

enum class IntervalLattice {
    ZERO, POSITIVE, NEGATIVE, UNKNOWN, UNDEFINED;

    fun toRefinement(): IntervalRefinement? = when (this) {
        ZERO -> IntervalRefinement.ZERO
        POSITIVE -> IntervalRefinement.POSITIVE
        NEGATIVE -> IntervalRefinement.NEGATIVE
        else -> null
    }

    operator fun plus(other: IntervalLattice): IntervalLattice = when (this) {
        ZERO -> other
        other -> other
        UNDEFINED -> UNDEFINED
        else -> UNKNOWN
    }

    operator fun minus(other: IntervalLattice): IntervalLattice = this + (-other)

    operator fun times(other: IntervalLattice): IntervalLattice = when (this) {
        ZERO -> ZERO
        POSITIVE -> other
        NEGATIVE -> -other
        UNDEFINED -> UNDEFINED
        else -> UNKNOWN
    }

    operator fun unaryMinus(): IntervalLattice = when (this) {
        POSITIVE -> NEGATIVE
        NEGATIVE -> POSITIVE
        else -> this
    }

    companion object {
        fun join(left: IntervalLattice, right: IntervalLattice): IntervalLattice =
            when (left) {
                UNDEFINED -> right
                right -> right
                else -> UNKNOWN
            }

        fun fromLiteral(literal: Long): IntervalLattice = when {
            literal > 0 -> POSITIVE
            literal < 0 -> NEGATIVE
            else -> ZERO
        }
    }
}