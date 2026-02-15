package org.yarokovisty.stub.runtime

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
object MatcherStack {

    private val stack = AtomicReference<List<Matcher<*>>>(emptyList())

    fun push(matcher: Matcher<*>) {
        while (true) {
            val current = stack.load()
            val updated = current + matcher
            if (stack.compareAndSet(current, updated)) return
        }
    }

    fun drain(): List<Matcher<*>> =
        stack.exchange(emptyList())
}
