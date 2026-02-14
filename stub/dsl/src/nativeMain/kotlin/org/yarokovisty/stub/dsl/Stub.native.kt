package org.yarokovisty.stub.dsl

import kotlin.reflect.KClass

actual fun <T : Any> createStub(kClass: KClass<T>): T {
    error("stub() is not yet supported on Native. Compiler plugin required for '${kClass.simpleName}'.")
}
