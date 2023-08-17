package functions.model.slack

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SlackUser(
    @SerialName("id") val id: String,
    @SerialName("profile") val profile: SlackUserProfile,
    @SerialName("is_email_confirmed") val isEmailConfirmed: Boolean?,
    @SerialName("is_bot") val isBot: Boolean?,
    @SerialName("is_restricted") val isRestricted: Boolean?,
)