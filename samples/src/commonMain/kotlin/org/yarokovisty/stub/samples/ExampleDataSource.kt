package org.yarokovisty.stub.samples

class ExampleDataSource(
    private val httpClient: ExampleHttpClient,
) {

    fun get(): String =
        httpClient.get()
}
