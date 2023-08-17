package functions.http

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

fun generateHttpClient(block: (HttpClientConfig<CIOEngineConfig>) -> Unit = {}): HttpClient {
    return HttpClient(CIO) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(httpJson)
        }
        this.apply(block)
    }
}