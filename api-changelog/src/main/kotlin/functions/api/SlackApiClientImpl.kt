package functions.api

import functions.env.Env
import functions.http.generateHttpClient
import functions.model.RequestResult
import functions.model.slack.SlackUsersResponse
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonObject

class SlackApiClientImpl {

    private val httpClient = generateHttpClient {
        it.defaultRequest {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
    }

    suspend fun fetchUsers(cursor: String? = null): RequestResult<SlackUsersResponse> {
        val response = try {
            httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "slack.com"
                    path("api", "users.list")
                    header("Authorization", System.getenv(Env.SLACK_API_AUTHORIZATION))
                    if (cursor != null) {
                        parameter("cursor", cursor)
                    }
                }
            }
        } catch (e: Exception) {
            return RequestResult.Failure(e)
        }
        return RequestResult.Success(response.body())
    }

    suspend fun respond(
        webhookUrl: String,
        requestBody: JsonObject,
    ): RequestResult<Unit> {
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