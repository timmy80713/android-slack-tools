package functions.model

enum class Whitelist(val value: String) {
    DevelopmentQa("development-qa"),
    ReleaseRegressionStart("release-regression-start"),
    ReleaseRegressionHotfix("release-regression-hotfix"),
    ReleaseRegressionFinish("release-regression-finish"),
    ReleaseProductionHotfix("release-production-hotfix"),
    ReleaseProductionFinish("release-production-finish"),
}

val Whitelist.toRegex get() = Regex("^/(${this.value})$")