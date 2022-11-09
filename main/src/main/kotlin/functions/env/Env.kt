package functions.env

object Env {

    const val GOOGLE_CLOUD_PROJECT_ID = "GOOGLE_CLOUD_PROJECT_ID"
    const val SLACK_SIGNING_SECRET = "SLACK_SIGNING_SECRET"

    fun get(name: String): String? = System.getenv(name)
}