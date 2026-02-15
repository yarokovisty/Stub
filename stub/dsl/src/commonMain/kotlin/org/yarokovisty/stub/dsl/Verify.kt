package org.yarokovisty.stub.dsl

import org.yarokovisty.stub.runtime.MockRecorder

@Suppress("TooGenericExceptionCaught")
fun verify(block: () -> Unit) {
    MockRecorder.startRecording()
    try {
        block()
    } catch (ignored: NullPointerException) {
        // Expected for primitive return types during recording
    } catch (ignored: ClassCastException) {
        // Expected for primitive return types during recording
    }
    val recorded = MockRecorder.stopRecording()
    check(recorded.delegate.wasCalled(recorded.call.methodName)) {
        "Expected call to '${recorded.call.methodName}' was not recorded."
    }
}

@Suppress("TooGenericExceptionCaught")
suspend fun coVerify(block: suspend () -> Unit) {
    MockRecorder.startRecording()
    try {
        block()
    } catch (ignored: NullPointerException) {
        // Expected for primitive return types during recording
    } catch (ignored: ClassCastException) {
        // Expected for primitive return types during recording
    }
    val recorded = MockRecorder.stopRecording()
    check(recorded.delegate.wasCalled(recorded.call.methodName)) {
        "Expected call to '${recorded.call.methodName}' was not recorded."
    }
}
