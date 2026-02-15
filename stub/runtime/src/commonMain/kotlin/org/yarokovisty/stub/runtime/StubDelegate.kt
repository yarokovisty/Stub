package org.yarokovisty.stub.runtime

class StubDelegate {

    private data class AnswerEntry(
        val methodName: String,
        val matchers: List<Matcher<*>>,
        val answer: Answer<*>,
    )

    private val entries = mutableListOf<AnswerEntry>()
    private val callLog = mutableListOf<MethodCall>()

    @Suppress("UNCHECKED_CAST")
    fun <T> handle(methodName: String, args: List<Any?> = emptyList()): T {
        val call = MethodCall(methodName, args)

        if (MockRecorder.isRecording) {
            MockRecorder.record(this, call)
            return null as T
        }

        callLog.add(call)
        val answer = findAnswer(methodName, args)
            ?: throw MissingAnswerException(methodName)

        return executeAnswer(answer, call) as T
    }

    fun setAnswer(methodName: String, matchers: List<Matcher<*>>, answer: Answer<*>) {
        entries.add(AnswerEntry(methodName, matchers, answer))
    }

    fun wasCalled(methodName: String): Boolean =
        callLog.any { it.methodName == methodName }

    fun callCount(methodName: String): Int =
        callLog.count { it.methodName == methodName }

    private fun findAnswer(methodName: String, args: List<Any?>): Answer<*>? =
        entries.lastOrNull { entry ->
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
