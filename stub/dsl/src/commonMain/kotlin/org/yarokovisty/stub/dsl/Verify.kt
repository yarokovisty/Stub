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
    val (delegate, call) = MockRecorder.stopRecording()
    check(delegate.wasCalled(call.methodName)) {
        "Expected call to '${call.methodName}' was not recorded."
    }
}
