package org.yarokovisty.stub.samples

class ExampleHttpClient {

    @Suppress("FunctionOnlyReturningConstant")
    fun getString(): String =
        "Request 200"

    @Suppress("FunctionOnlyReturningConstant")
    fun getInt(): Int =
        42

    fun getData(id: Int, name: String): ExampleData =
        ExampleData(id, name)
}
