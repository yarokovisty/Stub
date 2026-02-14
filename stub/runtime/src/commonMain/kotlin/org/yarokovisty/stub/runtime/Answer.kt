package org.yarokovisty.stub.runtime

sealed interface Answer<out T> {

    data class Value<T>(val value: T) : Answer<T>

    data class Throwing(val exception: Throwable) : Answer<Nothing>

    data class Lambda<T>(val block: (MethodCall) -> T) : Answer<T>
}
