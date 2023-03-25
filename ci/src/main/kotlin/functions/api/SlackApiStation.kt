package functions.api

import functions.model.RequestResult
import functions.model.SlackResponse
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object SlackApiStation {
    @Serializable
    enum class ResponseType(val value: String) {

        @SerialName("ephemeral")
        Ephemeral("ephemeral"),

        @SerialName("in_channel")
        InChannel("in_channel"),
    }

    suspend fun respondEphemeral(
        responseUrl: String,
        text: String,
    ) = respond(
        responseUrl = responseUrl,
        slackResponse = SlackResponse(ResponseType.Ephemeral, text),
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
            ApiStation.client.post {
                url(responseUrl)
                contentType(ContentType.Application.Json)
                setBody(slackResponse)
            }
        } catch (e: Exception) {
            return RequestResult.Failure(e)
        }
        return RequestResult.Success(Unit)
    }
}