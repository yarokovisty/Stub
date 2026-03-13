package org.yarokovisty.stub.samples

import io.ktor.client.statement.HttpResponse

interface IHttpClient {
    suspend fun get(url: String): HttpResponse
}
