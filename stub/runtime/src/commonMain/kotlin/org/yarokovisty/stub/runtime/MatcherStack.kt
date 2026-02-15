package org.yarokovisty.stub.runtime

object MatcherStack {

    private val stack = mutableListOf<Matcher<*>>()

    fun push(matcher: Matcher<*>) {
        stack.add(matcher)
    }

    fun drain(): List<Matcher<*>> {
        val result = stack.toList()
        stack.clear()
        return result
    }
}
