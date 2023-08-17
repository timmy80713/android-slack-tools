package functions.model.slack

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SlackUserProfile(
    @SerialName("email") val email: String?,
)