package functions.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BitriseTriggerRequest(
    @SerialName("hook_info") val hookInfo: HookInfo,
    @SerialName("build_params") val buildParams: BuildParams,
    @SerialName("triggered_by") val triggeredBy: String,
) {
    @Serializable
    data class HookInfo(
        @SerialName("type") val type: String = "bitrise",
        @SerialName("build_trigger_token") val buildTriggerToken: String,
    )

    @Serializable
    data class BuildParams(
        @SerialName("branch") val branch: String? = null,
        @SerialName("tag") val tag: String? = null,
        @SerialName("commit_hash") val commitHash: String? = null,
        @SerialName("workflow_id") val workflowId: String,
        @SerialName("environments") val environments: List<Environment>? = null,
    ) {
        @Serializable
        data class Environment(
            @SerialName("mapped_to") val mappedTo: String,
            @SerialName("value") val value: String,
        ) {
            class Builder {

                private val map = mutableMapOf<String, String>()

                fun build() = map.map { Environment(mappedTo = it.key, value = it.value) }

                fun buildVariants(buildVariants: List<String>) = apply {
                    map += "QA_BUILD_VARIANTS" to buildVariants.joinToString(",")
                }

                fun slackMessage(slackMessage: String) = apply {
                    map += "QA_SLACK_MESSAGE" to slackMessage
                }

                fun triggeredUser(userId: String) = apply {
                    map += "QA_TRIGGERED_USER" to userId
                }

                fun increaseVersionType(type: String) = apply {
                    map += "INCREASE_VERSION_TYPE" to type
                }
            }
        }
    }
}