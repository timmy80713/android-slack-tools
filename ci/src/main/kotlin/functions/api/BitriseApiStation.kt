package functions.api

import functions.model.BitriseResponse
import functions.model.BitriseTriggerRequest
import functions.model.RequestResult
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*

object BitriseApiStation {
    suspend fun triggerBuild(
        body: BitriseTriggerRequest,
    ): RequestResult<BitriseResponse> {
        val response = try {
            ApiStation.client.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "app.bitrise.io"
                    path("app", "254172eb-6397-4471-ae4c-0a75a7eb9a81", "build", "start.json")
                }
                contentType(ContentType.Application.Json)
                setBody(body)
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