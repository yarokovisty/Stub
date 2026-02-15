package org.yarokovisty.stub.dsl

import org.yarokovisty.stub.runtime.Matcher
import org.yarokovisty.stub.runtime.MatcherStack

@Suppress("UNCHECKED_CAST")
inline fun <reified T> any(): T {
    MatcherStack.push(Matcher.Any)
    return defaultValue<T>()
}

inline fun <reified T> eq(value: T): T {
    MatcherStack.push(Matcher.Eq(value))
    return value
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> defaultValue(): T =
    when (T::class) {
        Int::class -> 0 as T
        Long::class -> 0L as T
        Short::class -> 0.toShort() as T
        Byte::class -> 0.toByte() as T
        Float::class -> 0f as T
        Double::class -> 0.0 as T
        Boolean::class -> false as T
        Char::class -> '\u0000' as T
        String::class -> "" as T
        else -> null as T
    }
