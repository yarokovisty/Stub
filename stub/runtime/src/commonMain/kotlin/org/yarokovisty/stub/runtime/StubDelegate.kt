package org.yarokovisty.stub.runtime

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@Suppress("TooManyFunctions")
@OptIn(ExperimentalAtomicApi::class)
class StubDelegate {

    private data class AnswerEntry(
        val methodName: String,
        val matchers: List<Matcher<*>>,
        val answer: Answer<*>,
    )

    private val entries = AtomicReference<List<AnswerEntry>>(emptyList())
    private val callLog = AtomicReference<List<MethodCall>>(emptyList())

    @Suppress("UNCHECKED_CAST")
    fun <T> handle(methodName: String, args: List<Any?> = emptyList()): T {
        val call = MethodCall(methodName, args)

        if (MockRecorder.isRecording) {
            MockRecorder.record(this, call)
            return null as T
        }

        appendToCallLog(call)
        val answer = findAnswer(methodName, args)
            ?: throw MissingAnswerException(methodName)

        return executeAnswer(answer, call) as T
    }

    fun setAnswer(methodName: String, matchers: List<Matcher<*>>, answer: Answer<*>) {
        while (true) {
            val current = entries.load()
            val updated = current + AnswerEntry(methodName, matchers, answer)
            if (entries.compareAndSet(current, updated)) return
        }
    }

    fun wasCalled(methodName: String): Boolean =
        callLog.load().any { it.methodName == methodName }

    fun wasCalledMatching(methodName: String, matchers: List<Matcher<*>>): Boolean =
        callLog.load().any { call ->
            call.methodName == methodName && matchesArgs(matchers, call.args)
        }

    fun callCount(methodName: String): Int =
        callLog.load().count { it.methodName == methodName }

    fun reset() {
        entries.store(emptyList())
        callLog.store(emptyList())
    }

    fun clearInvocations() {
        callLog.store(emptyList())
    }

    private fun appendToCallLog(call: MethodCall) {
        while (true) {
            val current = callLog.load()
            val updated = current + call
            if (callLog.compareAndSet(current, updated)) return
        }
    }

    private fun findAnswer(methodName: String, args: List<Any?>): Answer<*>? =
        entries.load().lastOrNull { entry ->
            entry.methodName == methodName && matchesArgs(entry.matchers, args)
        }?.answer

    private fun matchesArgs(matchers: List<Matcher<*>>, args: List<Any?>): Boolean =
        when {
            matchers.isEmpty() && args.isEmpty() -> true
            matchers.size != args.size -> false
            else -> matchers.zip(args).all { (matcher, arg) -> matcher.matches(arg) }
        }

    @Suppress("UNCHECKED_CAST")
    private fun executeAnswer(answer: Answer<*>, call: MethodCall): Any? =
        when (answer) {
            is Answer.Value -> answer.value
            is Answer.Throwing -> throw answer.exception
            is Answer.Lambda<*> -> (answer.block)(call)
        }
}
