package org.yarokovisty.stub.dsl

import org.yarokovisty.stub.runtime.MockRecorder

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
    val recorded = MockRecorder.stopRecording()
    return StubCall(recorded.delegate, recorded.call.methodName, recorded.matchers)
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
    val recorded = MockRecorder.stopRecording()
    return StubCall(recorded.delegate, recorded.call.methodName, recorded.matchers)
}
