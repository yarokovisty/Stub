package org.yarokovisty.stub.runtime

class StubDelegate {

    private val answers = mutableMapOf<String, Answer<*>>()
    private val callLog = mutableListOf<MethodCall>()

    @Suppress("UNCHECKED_CAST")
    fun <T> handle(methodName: String, args: List<Any?> = emptyList()): T {
        val call = MethodCall(methodName, args)

        if (MockRecorder.isRecording) {
            MockRecorder.record(this, call)
            return null as T
        }

        callLog.add(call)
        val answer = answers[methodName]
            ?: throw MissingAnswerException(methodName)

        return executeAnswer(answer, call) as T
    }

    fun setAnswer(methodName: String, answer: Answer<*>) {
        answers[methodName] = answer
    }

    fun wasCalled(methodName: String): Boolean =
        callLog.any { it.methodName == methodName }

    fun callCount(methodName: String): Int =
        callLog.count { it.methodName == methodName }

    @Suppress("UNCHECKED_CAST")
    private fun executeAnswer(answer: Answer<*>, call: MethodCall): Any? =
        when (answer) {
            is Answer.Value -> answer.value
            is Answer.Throwing -> throw answer.exception
            is Answer.Lambda<*> -> (answer.block)(call)
        }
}
