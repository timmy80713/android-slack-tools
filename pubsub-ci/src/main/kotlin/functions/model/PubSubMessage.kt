package functions.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PubSubMessage(
    @SerialName("data") val data: String,
    @SerialName("messageId") val messageId: String,
    @SerialName("publishTime") val publishTime: String,
)