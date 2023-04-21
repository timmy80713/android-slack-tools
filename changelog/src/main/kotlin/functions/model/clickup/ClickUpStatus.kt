package functions.model.clickup

import kotlinx.serialization.SerialName

enum class ClickUpStatus {
    @SerialName("wait for release")
    WaitForRelease,

    @SerialName("regression test")
    RegressionTest,

    @SerialName("done/production/closed")
    Close,
}