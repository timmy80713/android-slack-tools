package functions.repo

import functions.api.SlackApiClientImpl
import functions.model.RequestResult
import functions.model.slack.SlackUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

class SlackRepoImpl(
    private val slackApiClientImpl: SlackApiClientImpl,
) {
    suspend fun fetchUsers(): List<SlackUser> {
        val slackUsers = try {
            recursiveFetchUsers(emptyList())
        } catch (e: Exception) {
            throw e
        }.filter {
            it.isBot == false &&
                    it.isRestricted == false &&
                    it.isEmailConfirmed == true &&
                    it.profile.email.isNullOrEmpty().not()
        }
        return slackUsers
    }

    private suspend fun recursiveFetchUsers(
        list: List<SlackUser>,
        cursor: String? = null,
    ): List<SlackUser> {
        val paging = withContext(Dispatchers.IO) {
            slackApiClientImpl.fetchUsers(cursor)
        }.let {
            when (it) {
                is RequestResult.Success -> it.result
                is RequestResult.Failure -> throw it.error
            }
        }
        val newUsers = list + paging.users
        val nextCursor = paging.metadata.nextCursor
        if (nextCursor.isEmpty()) return newUsers
        return recursiveFetchUsers(newUsers, nextCursor)
    }

    suspend fun respond(
        webhookUrl: String, requestBody: JsonObject
    ): RequestResult<Unit> {
        return slackApiClientImpl.respond(
            webhookUrl = webhookUrl,
            requestBody = requestBody,
        )
    }
}