package functions.model.clickup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClickUpSpace(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String?,
    @SerialName("avatar") val avatar: String?,
)