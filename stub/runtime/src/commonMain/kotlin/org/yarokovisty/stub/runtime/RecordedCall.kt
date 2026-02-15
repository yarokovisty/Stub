package org.yarokovisty.stub.runtime

data class RecordedCall(
    val delegate: StubDelegate,
    val call: MethodCall,
    val matchers: List<Matcher<*>>,
)
