package org.yarokovisty.stub.dsl

import kotlin.reflect.KClass

inline fun <reified T : Any> stub(): T = createStub(T::class)

@Suppress("UnusedParameter")
fun <T : Any> createStub(kClass: KClass<T>): T {
    error("Stub compiler plugin is not applied. Add 'stubCompiler' plugin to your build.")
}
