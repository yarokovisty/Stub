package org.yarokovisty.stub.dsl

import org.yarokovisty.stub.runtime.Answer
import org.yarokovisty.stub.runtime.MethodCall
import org.yarokovisty.stub.runtime.StubDelegate

class StubCall<T>(
    private val delegate: StubDelegate,
    private val methodName: String,
) {

    infix fun returns(value: T) {
        delegate.setAnswer(methodName, Answer.Value(value))
    }

    infix fun throws(exception: Throwable) {
        delegate.setAnswer(methodName, Answer.Throwing(exception))
    }

    infix fun answers(block: (MethodCall) -> T) {
        delegate.setAnswer(methodName, Answer.Lambda(block))
    }
}
