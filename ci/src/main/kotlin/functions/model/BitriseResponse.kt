package functions.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BitriseResponse(
    @SerialName("status") val status: String,
    @SerialName("message") val message: String,
    @SerialName("build_number") val buildNumber: Int,
    @SerialName("build_url") val buildUrl: String,
    @SerialName("triggered_workflow") val triggeredWorkflow: String,
)