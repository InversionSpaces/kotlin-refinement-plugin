package com.example.refinement.models

enum class IntervalRefinement {
    ZERO, POSITIVE, NEGATIVE
}

enum class IntervalLattice {
    ZERO, POSITIVE, NEGATIVE, UNDEFINED, UNKNOWN;

    companion object {
        fun join(left: IntervalLattice, right: IntervalLattice): IntervalLattice =
            when (left) {
                UNDEFINED -> right
                right -> right
                else -> UNKNOWN
            }
    }
}