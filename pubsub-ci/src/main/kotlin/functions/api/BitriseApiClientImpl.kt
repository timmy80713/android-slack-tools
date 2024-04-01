package functions.api

import functions.http.generateHttpClient
import functions.model.RequestResult
import functions.model.bitrise.BitriseResponse
import functions.model.bitrise.BitriseTriggerRequest
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*

class BitriseApiClientImpl {

    private val httpClient = generateHttpClient {
        it.defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                host = "app.bitrise.io"
            }
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
    }

    suspend fun triggerBuild(
        requestBody: BitriseTriggerRequest,
    ): RequestResult<BitriseResponse> {
        val response = try {
            httpClient.post {
                url { path("app", "254172eb-6397-4471-ae4c-0a75a7eb9a81", "build", "start.json") }
                setBody(requestBody)
            }
        } catch (e: Exception) {
            return RequestResult.Failure(e)
        }
        val responseBody: BitriseResponse = response.body()
        return if (responseBody.status == "ok") {
            RequestResult.Success(responseBody)
        } else {
            RequestResult.Failure(
                ResponseException(response, "Status: ${responseBody.status}, Message: ${responseBody.message}"),
            )
        }
    }
}