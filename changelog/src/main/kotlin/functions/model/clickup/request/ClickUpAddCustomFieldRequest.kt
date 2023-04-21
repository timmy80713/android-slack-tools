package functions.model.clickup.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClickUpAddCustomFieldRequest(
    @SerialName("value") val value: String,
)