package functions.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Workflow {
    @SerialName("release_regression_start")
    ReleaseRegressionStart,

    @SerialName("release_regression_hotfix")
    ReleaseRegressionHotfix,

    @SerialName("release_regression_finish")
    ReleaseRegressionFinish,

    @SerialName("release_production_finish")
    ReleaseProductionFinish,

    Unknown,
}