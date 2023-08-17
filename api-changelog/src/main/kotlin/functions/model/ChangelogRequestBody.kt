package functions.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChangelogRequestBody(
    @SerialName("workflow") val workflow: Workflow = Workflow.Unknown,
    @SerialName("tag") val tag: String,
)