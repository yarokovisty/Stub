package org.yarokovisty.stub.dsl

import org.yarokovisty.stub.runtime.MockRecorder
import org.yarokovisty.stub.runtime.RecordedCall

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
    checkRecordedCall(MockRecorder.stopRecording())
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
    checkRecordedCall(MockRecorder.stopRecording())
}

private fun checkRecordedCall(recorded: RecordedCall) {
    check(recorded.delegate.wasCalledMatching(recorded.call.methodName, recorded.matchers)) {
        "Expected call to '${recorded.call.methodName}' was not recorded."
    }
}
