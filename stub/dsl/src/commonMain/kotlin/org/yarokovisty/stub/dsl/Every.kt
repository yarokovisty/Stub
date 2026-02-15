package org.yarokovisty.stub.dsl

import org.yarokovisty.stub.runtime.MockRecorder
import org.yarokovisty.stub.runtime.RecordedCall

@Suppress("TooGenericExceptionCaught")
fun <T> every(block: () -> T): StubCall<T> {
    MockRecorder.startRecording()
    try {
        block()
    } catch (ignored: NullPointerException) {
        // Expected for primitive return types during recording
    } catch (ignored: ClassCastException) {
        // Expected for primitive return types during recording
    }
    return buildStubCall(MockRecorder.stopRecording())
}

@Suppress("TooGenericExceptionCaught")
suspend fun <T> coEvery(block: suspend () -> T): StubCall<T> {
    MockRecorder.startRecording()
    try {
        block()
    } catch (ignored: NullPointerException) {
        // Expected for primitive return types during recording
    } catch (ignored: ClassCastException) {
        // Expected for primitive return types during recording
    }
    return buildStubCall(MockRecorder.stopRecording())
}

private fun <T> buildStubCall(recorded: RecordedCall): StubCall<T> =
    StubCall(recorded.delegate, recorded.call.methodName, recorded.matchers)
