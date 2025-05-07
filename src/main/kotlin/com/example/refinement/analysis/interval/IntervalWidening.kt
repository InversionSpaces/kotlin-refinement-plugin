package com.example.refinement.analysis.interval

import com.example.refinement.analysis.Widening
import com.example.refinement.models.InfInt
import com.example.refinement.models.IntervalLattice
import com.example.refinement.models.IntervalLattice.Bounded

class IntervalWidening : Widening<IntervalLattice> {
    override fun apply(
        previous: IntervalLattice,
        current: IntervalLattice
    ): IntervalLattice = when {
        previous is Bounded && current is Bounded -> Bounded(
            if (previous.from <= current.from) current.from else InfInt.negInf(),
            if (previous.to >= current.to) current.to else InfInt.posInf()
        )

        else -> IntervalLattice.join(previous, current)
    }
}