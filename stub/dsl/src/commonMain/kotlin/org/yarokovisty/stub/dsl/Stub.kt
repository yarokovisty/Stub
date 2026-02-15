package org.yarokovisty.stub.dsl

import org.yarokovisty.stub.runtime.Stubbable
import kotlin.reflect.KClass

inline fun <reified T : Any> stub(): T = createStub(T::class)

@Suppress("UnusedParameter")
fun <T : Any> createStub(kClass: KClass<T>): T {
    error("Stub compiler plugin is not applied. Add 'stubCompiler' plugin to your build.")
}

fun clearStubs(stub: Any) {
    require(stub is Stubbable) { "Object is not a stub created by stub<T>()" }
    stub.stubDelegate.reset()
}

fun clearInvocations(stub: Any) {
    require(stub is Stubbable) { "Object is not a stub created by stub<T>()" }
    stub.stubDelegate.clearInvocations()
}
