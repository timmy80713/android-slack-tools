package functions.slack

import com.google.cloud.functions.HttpResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object SlackApi {

    @Serializable
    enum class ResponseType(val value: String) {

        @SerialName("ephemeral")
        Ephemeral("ephemeral"),

        @SerialName("in_channel")
        InChannel("in_channel"),
    }

    fun acknowledgeEphemeral(
        response: HttpResponse,
        text: String,
    ) = acknowledge(
        response = response,
        slackResponse = SlackResponse(ResponseType.Ephemeral, text),
    )

    fun acknowledgeInChannel(
        response: HttpResponse,
        text: String,
    ) = acknowledge(
        response = response,
        slackResponse = SlackResponse(ResponseType.InChannel, text),
    )

    fun acknowledge(
        response: HttpResponse,
        slackResponse: SlackResponse,
    ) {
        response.apply {
            setContentType("application/json")
            writer.write(Json.encodeToString(SlackResponse.serializer(), slackResponse))
        }
    }
}