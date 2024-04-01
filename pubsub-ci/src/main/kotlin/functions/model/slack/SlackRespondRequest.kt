package functions.model.slack

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SlackRespondRequest(
    @SerialName("response_type") val responseType: ResponseType,
    @SerialName("text") val text: String,
) {
    @Serializable
    enum class ResponseType {
        @SerialName("ephemeral")
        Ephemeral,

        @SerialName("in_channel")
        InChannel,
    }
}