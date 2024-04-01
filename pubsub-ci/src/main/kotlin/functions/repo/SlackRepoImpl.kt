package functions.repo

import functions.api.SlackApiClientImpl
import functions.model.RequestResult
import functions.model.slack.SlackRespondRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SlackRepoImpl(private val slackApiClientImpl: SlackApiClientImpl) {

    suspend fun respondInChannel(webhookUrl: String, text: String): RequestResult<Unit> {
        val requestBody = SlackRespondRequest(SlackRespondRequest.ResponseType.InChannel, text)
        return withContext(Dispatchers.IO) {
            slackApiClientImpl.respond(webhookUrl, requestBody)
        }
    }

    suspend fun respondEphemeral(webhookUrl: String, text: String): RequestResult<Unit> {
        val requestBody = SlackRespondRequest(SlackRespondRequest.ResponseType.Ephemeral, text)
        return withContext(Dispatchers.IO) {
            slackApiClientImpl.respond(webhookUrl, requestBody)
        }
    }
}