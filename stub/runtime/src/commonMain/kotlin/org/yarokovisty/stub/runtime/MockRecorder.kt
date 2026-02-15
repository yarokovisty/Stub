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

    fun stopRecording(): RecordedCall {
        recording = false
        val captured = lastCall
        lastCall = null
        val (delegate, call) = checkNotNull(captured) {
            "No stub method was called inside every { } block."
        }
        val matchers = resolveMatchers(call)
        return RecordedCall(delegate, call, matchers)
    }

    private fun resolveMatchers(call: MethodCall): List<Matcher<*>> {
        val pending = MatcherStack.drain()
        if (pending.isEmpty()) {
            return call.args.map { Matcher.Eq(it) }
        }
        require(pending.size == call.args.size) {
            "Matcher count (${pending.size}) does not match argument count (${call.args.size}). " +
                "When using matchers, all arguments must use matchers."
        }
        return pending
    }
}
