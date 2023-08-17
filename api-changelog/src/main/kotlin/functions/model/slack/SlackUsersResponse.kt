package functions.model.slack

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SlackUsersResponse(
    @SerialName("ok") val ok: Boolean,
    @SerialName("members") val users: List<SlackUser>,
    @SerialName("response_metadata") val metadata: Metadata,
) {
    @Serializable
    data class Metadata(
        @SerialName("next_cursor") val nextCursor: String,
    )
}