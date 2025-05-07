package com.example.refinement.analysis

interface Widening<D : Any> {
    fun apply(previous: D, current: D): D
}