package functions.env

object Env {

    const val CIRCLECI_PERSONAL_API_TOKEN = "CIRCLECI_PERSONAL_API_TOKEN"

    fun get(name: String): String? = System.getenv(name)
}