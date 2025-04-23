package com.example.refinement

fun <T> Pair<T?, T?>.fold(f: (T, T) -> T): T? = when {
    first == null -> second
    second == null -> first
    else -> f(first!!, second!!)
}