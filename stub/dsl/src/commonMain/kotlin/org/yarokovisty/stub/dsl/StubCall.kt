package org.yarokovisty.stub.dsl

import org.yarokovisty.stub.runtime.Answer
import org.yarokovisty.stub.runtime.Matcher
import org.yarokovisty.stub.runtime.MethodCall
import org.yarokovisty.stub.runtime.StubDelegate

class StubCall<T>(
    private val delegate: StubDelegate,
    private val methodName: String,
    private val matchers: List<Matcher<*>>,
) {

    infix fun returns(value: T) {
        delegate.setAnswer(methodName, matchers, Answer.Value(value))
    }

    infix fun answers(block: (MethodCall) -> T) {
        delegate.setAnswer(methodName, matchers, Answer.Lambda(block))
    }

    infix fun throws(exception: Throwable) {
        delegate.setAnswer(methodName, matchers, Answer.Throwing(exception))
    }
}
