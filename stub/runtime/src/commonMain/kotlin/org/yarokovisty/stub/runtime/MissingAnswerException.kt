package org.yarokovisty.stub.runtime

class MissingAnswerException(
    methodName: String,
) : IllegalStateException(
    "No answer configured for method '$methodName'. Use every { } returns ... to configure.",
)
