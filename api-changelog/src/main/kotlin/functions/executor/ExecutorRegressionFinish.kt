package functions.executor

import functions.env.Env
import functions.model.clickup.CLICK_TEAM_ID
import functions.model.clickup.ClickUpStatus
import functions.model.clickup.ClickUpView
import functions.model.contentOrNull
import functions.model.resultOrNull
import functions.model.slack.SlackMessagePayloadCreator
import functions.repo.ClickUpRepoImpl
import functions.repo.SlackRepoImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.util.logging.Logger

class ExecutorRegressionFinish(
    private val tag: String,
    private val clickUpRepoImpl: ClickUpRepoImpl,
    private val slackRepoImpl: SlackRepoImpl,
    private val slackMessagePayloadCreator: SlackMessagePayloadCreator,
) : Executor {

    private val logger = Logger.getLogger(ExecutorRegressionFinish::class.java.name)

    override suspend fun execute() {

        val clickUpView = ClickUpView.RegressionTest
        val targetStatus = ClickUpStatus.Close

        val clickSpaces = clickUpRepoImpl.fetchSpaces(CLICK_TEAM_ID)

        val taskNameReplaceRegex = "\\s*[\\[【]\\s*[Aa]ndroid\\s*[】\\]]\\s*".toRegex()
        val clickUpTasks = clickUpRepoImpl.fetchTasks(clickUpView.id)
            .filter { task ->
                task.name.contains("TimmmmmmY", ignoreCase = true)
            }
            .map { task ->
                task.copy(name = task.name.replace(taskNameReplaceRegex, ""))
            }
            .map { task ->
                val space = clickSpaces.find { space -> space.id == task.space.id }
                if (space != null) {
                    task.copy(space = space)
                } else {
                    task
                }
            }
            .sortedBy { it.space.name }

        logger.info("${clickUpView.name} has ${clickUpTasks.size} tasks.")

        withContext(Dispatchers.IO) {
            clickUpTasks.map {
                async { clickUpRepoImpl.updateTask(it.id, targetStatus) }
            }.awaitAll().mapNotNull { it.resultOrNull() }
        }

        val taskGroups = clickUpTasks.groupBy { it.space.id }

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