package org.yarokovisty.stub.samples

class ExampleDataSource(
    private val httpClient: ExampleHttpClient,
) {

    fun getString(): String =
        httpClient.getString()

    fun getInt(): Int =
        httpClient.getInt()

    fun getData(id: Int, name: String): ExampleData =
        httpClient.getData(id, name)

    suspend fun getData(): ExampleData =
        httpClient.getData()
}
