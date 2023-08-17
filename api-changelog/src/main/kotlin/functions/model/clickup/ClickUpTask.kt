package functions.model.clickup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClickUpTask(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("url") val url: String,
    @SerialName("creator") val creator: ClickUpUser,
    @SerialName("assignees") val assignees: List<ClickUpUser>,
    @SerialName("space") val space: ClickUpSpace,
)