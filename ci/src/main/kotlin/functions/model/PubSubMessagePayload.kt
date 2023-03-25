package functions.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PubSubMessagePayload(
    @SerialName("teamId") val teamId: String,
    @SerialName("userId") val userId: String,
    @SerialName("userName") val userName: String,
    @SerialName("command") val command: String,
    @SerialName("urlPath") val urlPath: String? = null,
    @SerialName("text") val text: String,
    @SerialName("responseUrl") val responseUrl: String,
)