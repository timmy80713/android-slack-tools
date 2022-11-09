package functions.slack

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SlackResponse(
    @SerialName("response_type") val responseType: SlackApi.ResponseType = SlackApi.ResponseType.Ephemeral,
    @SerialName("text") val text: String,
)