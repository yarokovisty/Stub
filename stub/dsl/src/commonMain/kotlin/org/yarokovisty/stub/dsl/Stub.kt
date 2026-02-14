package org.yarokovisty.stub.dsl

import kotlin.reflect.KClass

inline fun <reified T : Any> stub(): T = createStub(T::class)

expect fun <T : Any> createStub(kClass: KClass<T>): T
