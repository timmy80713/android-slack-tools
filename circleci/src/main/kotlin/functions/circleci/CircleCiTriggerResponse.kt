package functions.circleci

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CircleCiTriggerResponse(
    @SerialName("number") val number: Long,
    @SerialName("message") val message: String? = null,
)