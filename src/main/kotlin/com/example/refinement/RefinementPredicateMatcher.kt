package com.example.refinement

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.utils.AbstractSimpleClassPredicateMatchingService
import org.jetbrains.kotlin.name.FqName

class RefinementPredicateMatcher(
    session: FirSession,
    annotations: List<String>
) : AbstractSimpleClassPredicateMatchingService(session) {
    companion object {
        fun getFactory(annotations: List<String>): Factory {
            return Factory { session -> RefinementPredicateMatcher(session, annotations) }
        }
    }

    override val predicate: DeclarationPredicate =
        DeclarationPredicate.create {
            val names = annotations.map { FqName(it) }
            annotated(names)
        }
}

val FirSession.refinementPredicateMatcher: RefinementPredicateMatcher by FirSession.sessionComponentAccessor()