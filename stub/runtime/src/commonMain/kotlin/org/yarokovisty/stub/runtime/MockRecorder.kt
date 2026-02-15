package org.yarokovisty.stub.runtime

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
object MockRecorder {

    private data class State(
        val recording: Boolean = false,
        val lastCall: Pair<StubDelegate, MethodCall>? = null,
    )

    private val state = AtomicReference(State())

    val isRecording: Boolean get() = state.load().recording

    fun startRecording() {
        state.store(State(recording = true, lastCall = null))
    }

    fun record(delegate: StubDelegate, call: MethodCall) {
        state.store(state.load().copy(lastCall = Pair(delegate, call)))
    }

    fun stopRecording(): RecordedCall {
        val captured = state.exchange(State())
        val (delegate, call) = checkNotNull(captured.lastCall) {
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
