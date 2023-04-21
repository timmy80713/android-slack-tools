package functions.model.clickup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClickUpTasksResponse(
    @SerialName("tasks") val tasks: List<ClickUpTask>,
    @SerialName("last_page") val lastPage: Boolean,
)