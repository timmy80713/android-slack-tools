package functions.slack

import com.google.cloud.functions.HttpRequest
import com.slack.api.app_backend.SlackSignature
import functions.App
import functions.env.Env
import java.time.ZonedDateTime
import java.util.logging.Logger

class SlackVerifier {

    companion object {
        private const val HEADER_KEY_SIGNATURE = "x-slack-signature"
        private const val HEADER_KEY_TIMESTAMP = "x-slack-request-timestamp"
    }

    private val logger = Logger.getLogger(App::class.java.name)

    private var verifier: SlackSignature.Verifier? = null

    init {
        verifier = Env.SLACK_SIGNING_SECRET.let {
            Env.get(it)?.let { slackSigningSecret ->
                SlackSignature.Verifier(SlackSignature.Generator(slackSigningSecret))
            }
        }
    }

    fun isValidSlackWebhook(request: HttpRequest, requestBodyString: String): Boolean {
        val signature = request.headers[HEADER_KEY_SIGNATURE]?.firstOrNull() ?: return false
        val timestamp = request.headers[HEADER_KEY_TIMESTAMP]?.firstOrNull() ?: return false
        logger.info("TimmmmmmY signature: $signature")
        logger.info("TimmmmmmY timestamp: $timestamp")
        val isValid = verifier?.isValid(
            timestamp, requestBodyString, signature, ZonedDateTime.now().toInstant().toEpochMilli()
        ) ?: false
        logger.info("TimmmmmmY isValid: $isValid")
        return isValid
    }
}