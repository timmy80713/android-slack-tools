package functions.model.clickup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClickUpUser(
    @SerialName("id") val id: Long,
    @SerialName("email") val email: String,
)