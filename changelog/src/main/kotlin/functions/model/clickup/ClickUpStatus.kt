package functions.model.clickup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ClickUpStatus {
    @SerialName("regression test")
    RegressionTest,

    @SerialName("done/production/closed")
    Close,
}