package org.yarokovisty.stub.dsl

import org.yarokovisty.stub.runtime.StubDelegate
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
actual fun <T : Any> createStub(kClass: KClass<T>): T {
    require(kClass.java.isInterface) {
        "stub() currently supports only interfaces. '${kClass.simpleName}' is a class."
    }
    val delegate = StubDelegate()
    val handler = InvocationHandler { proxy, method, args ->
        when (method.name) {
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args?.firstOrNull()
            "toString" -> "Stub<${kClass.simpleName}>"
            else -> delegate.handle<Any?>(method.name, args?.toList().orEmpty())
        }
    }
    return Proxy.newProxyInstance(
        kClass.java.classLoader,
        arrayOf(kClass.java),
        handler,
    ) as T
}
