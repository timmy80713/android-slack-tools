package functions.model

enum class Whitelist(val value: String) {
    Qa("qa"),
    RegressionStart("regression-start"),
    RegressionHotfix("regression-hotfix"),
    ProductionHotfix("production-hotfix"),
}

val Whitelist.toRegex get() = Regex("^/(${this.value})$")