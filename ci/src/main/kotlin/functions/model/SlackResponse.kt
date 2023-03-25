package functions.model

import functions.api.SlackApiStation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SlackResponse(
    @SerialName("response_type") val responseType: SlackApiStation.ResponseType = SlackApiStation.ResponseType.Ephemeral,
    @SerialName("text") val text: String,
)