package functions.slack

import functions.model.RequestResult
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object SlackApi {

    @Serializable
    enum class ResponseType(val value: String) {

        @SerialName("ephemeral")
        Ephemeral("ephemeral"),

        @SerialName("in_channel")
        InChannel("in_channel"),
    }

    private val client = HttpClient(CIO) {
        expectSuccess = true
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun respondEphemeral(
        responseUrl: String,
        text: String,
    ) = respond(
        responseUrl = responseUrl,
        slackResponse = SlackResponse(ResponseType.Ephemeral, text),
    )

    suspend fun respondEphemeralError(
        responseUrl: String,
        message: String,
    ) = respond(
        responseUrl = responseUrl,
        slackResponse = SlackResponse(ResponseType.Ephemeral, "Error: $message"),
    )

    suspend fun respondInChannel(
        responseUrl: String,
        text: String,
    ) = respond(
        responseUrl = responseUrl,
        slackResponse = SlackResponse(ResponseType.InChannel, text),
    )

    suspend fun respond(
        responseUrl: String,
        slackResponse: SlackResponse,
    ): RequestResult<Unit> {
        try {
            client.post {
                url(responseUrl)
                contentType(ContentType.Application.Json)
                setBody(slackResponse)
            }
        } catch (e: Exception) {
            return RequestResult.Failed(e)
        }
        return RequestResult.Success(Unit)
    }
}