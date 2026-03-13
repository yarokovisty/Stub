package org.yarokovisty.stub.samples

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse

class HttpClientAdapter(
    private val client: HttpClient,
) : IHttpClient {
    override suspend fun get(url: String): HttpResponse =
        client.get(url)
}
