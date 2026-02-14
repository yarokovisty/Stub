package org.yarokovisty.stub.runtime

data class MethodCall(
    val methodName: String,
    val args: List<Any?> = emptyList(),
)
