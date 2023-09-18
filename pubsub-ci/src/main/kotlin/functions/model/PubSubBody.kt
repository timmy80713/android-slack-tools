package functions.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PubSubBody(
    @SerialName("message") val message: PubSubMessage,
)