package org.yarokovisty.stub.runtime

sealed interface Matcher<in T> {

    fun matches(value: kotlin.Any?): Boolean

    object Any : Matcher<kotlin.Any?> {
        override fun matches(value: kotlin.Any?): Boolean = true
    }

    data class Eq<T>(val expected: T) : Matcher<T> {
        override fun matches(value: kotlin.Any?): Boolean = expected == value
    }
}
