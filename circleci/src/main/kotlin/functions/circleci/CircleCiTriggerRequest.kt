package functions.circleci

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CircleCiTriggerRequest(
    @SerialName("branch") val branch: String,
    @SerialName("parameters") val parameters: Parameters,
) {
    @Serializable
    data class Parameters(
        @SerialName("pull_request_trigger") val pullRequestTrigger: Boolean,
        @SerialName("qa_trigger") val qaTrigger: Boolean,
        @SerialName("qa_build_variants") val variants: String,
        @SerialName("qa_message") val message: String,
    )
}