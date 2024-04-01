package functions.api

import functions.http.generateHttpClient
import functions.model.RequestResult
import functions.model.slack.SlackRespondRequest
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*

class SlackApiClientImpl {

    private val httpClient = generateHttpClient {
        it.defaultRequest {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
    }

    suspend fun respond(webhookUrl: String, requestBody: SlackRespondRequest): RequestResult<Unit> {
        try {
            httpClient.post {
                url(webhookUrl)
                setBody(requestBody)
            }
        } catch (e: Exception) {
            return RequestResult.Failure(e)
        }
        return RequestResult.Success(Unit)
    }
}