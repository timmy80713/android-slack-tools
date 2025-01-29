package functions.executor

import functions.env.Env
import functions.model.clickup.ClickUpSpace
import functions.model.clickup.ClickUpTask
import functions.model.contentOrNull
import functions.model.slack.SlackMessagePayloadCreator
import functions.repo.SlackRepoImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class ExecutorFinishMockImpl(
    private val tag: String,
    private val slackRepoImpl: SlackRepoImpl,
    private val slackMessagePayloadCreator: SlackMessagePayloadCreator,
) : Executor {

    override suspend fun execute() {

        val clickSpaces = emptyList<ClickUpSpace>()
        val taskGroups = emptyMap<String, List<ClickUpTask>>()

        val slackUsers = slackRepoImpl.fetchUsers()

        val changelog = slackMessagePayloadCreator.createChangelog(
            tag = tag,
            spaces = clickSpaces,
            taskGroups = taskGroups,
            slackUsers = slackUsers,
        )

        val slackWebhooks = Json.parseToJsonElement(System.getenv(Env.SLACK_WEBHOOKS)).jsonObject
        val androidChangelogWebhook = slackWebhooks["android_changelog"]?.contentOrNull!!
        withContext(Dispatchers.IO) {
            slackRepoImpl.respond(
                webhookUrl = androidChangelogWebhook,
                requestBody = changelog,
            )
        }
    }
}