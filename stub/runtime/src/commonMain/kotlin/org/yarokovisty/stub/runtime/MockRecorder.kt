package org.yarokovisty.stub.runtime

object MockRecorder {

    private var recording = false
    private var lastCall: Pair<StubDelegate, MethodCall>? = null

    val isRecording: Boolean get() = recording

    fun startRecording() {
        recording = true
        lastCall = null
    }

    fun record(delegate: StubDelegate, call: MethodCall) {
        lastCall = Pair(delegate, call)
    }

    fun stopRecording(): Pair<StubDelegate, MethodCall> {
        recording = false
        val captured = lastCall
        lastCall = null
        return checkNotNull(captured) {
            "No stub method was called inside every { } block."
        }
    }
}
