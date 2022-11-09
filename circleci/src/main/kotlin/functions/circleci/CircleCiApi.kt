package functions.circleci

import functions.model.RequestResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object CircleCiApi {

    private val client = HttpClient(CIO) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
    }

    suspend fun triggerBuild(
        token: String,
        requestBody: CircleCiTriggerRequest,
    ): RequestResult<CircleCiTriggerResponse> {
        val response = try {
            client.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "circleci.com"
                    path("api", "v2", "project", "gh", "timmy80713", "android-dcard-sandbox", "pipeline")
                }
                contentType(ContentType.Application.Json)
                header("Circle-Token", token)
                setBody(requestBody)
            }
        } catch (e: Exception) {
            return RequestResult.Failed(e)
        }
        val responseBody: CircleCiTriggerResponse = response.body()
        return if (response.status.value == 201) {
            RequestResult.Success(responseBody)
        } else {
            RequestResult.Failed(
                ResponseException(response, "Status: ${response.status.value}, payload: ${responseBody.message}")
            )
        }
    }
}