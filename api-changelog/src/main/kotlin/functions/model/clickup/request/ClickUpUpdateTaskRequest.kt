package functions.model.clickup.request

import functions.model.clickup.ClickUpStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClickUpUpdateTaskRequest(
    @SerialName("status") val status: ClickUpStatus,
)