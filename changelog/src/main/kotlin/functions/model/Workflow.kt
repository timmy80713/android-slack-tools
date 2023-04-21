package functions.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Workflow {
    @SerialName("regression_start")
    RegressionStart,

    @SerialName("regression_hotfix")
    RegressionHotfix,

    @SerialName("regression_finish")
    RegressionFinish,

    @SerialName("production_hotfix")
    ProductionHotfix,

    Unknown,
}