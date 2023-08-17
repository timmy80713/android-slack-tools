package functions.model.clickup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClickUpSpacesResponse(
    @SerialName("spaces") val spaces: List<ClickUpSpace>,
)