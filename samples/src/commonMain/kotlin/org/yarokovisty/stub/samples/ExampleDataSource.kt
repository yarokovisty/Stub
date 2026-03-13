package org.yarokovisty.stub.samples

import io.ktor.client.call.body

class ExampleDataSource(
    private val client: IHttpClient,
) {

    fun getString(): String =
        "string"

    fun getInt(): Int =
        0

    fun getData(id: Int, name: String): ExampleData =
        ExampleData(id, name)

    suspend fun getData(): ExampleData =
        client.get("url").body()
}
