package functions.executor

import functions.env.Env
import functions.model.clickup.ClickUpCustomField
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

class ExecutorProductionHotfix(
    private val tag: String,
    private val clickUpRepoImpl: ClickUpRepoImpl,
    private val slackRepoImpl: SlackRepoImpl,
    private val slackMessagePayloadCreator: SlackMessagePayloadCreator,
) : Executor {

    private val logger = Logger.getLogger(ExecutorProductionHotfix::class.java.name)

    override suspend fun execute() {

        val clickUpView = ClickUpView.ProductionHotfix
        val targetStatus = ClickUpStatus.Close

        val taskNameReplaceRegex = "\\s*[\\[【]\\s*[Aa]ndroid\\s*[】\\]]\\s*".toRegex()
        val clickUpTasks = clickUpRepoImpl.fetchTasks(clickUpView.id).filter {
            it.name.contains("TimmmmmmY", ignoreCase = true)
        }.map {
            it.copy(name = it.name.replace(taskNameReplaceRegex, ""))
        }

        logger.info("${clickUpView.name} has ${clickUpTasks.size} tasks.")

        withContext(Dispatchers.IO) {
            clickUpTasks.map {
                async { clickUpRepoImpl.addCustomField(it.id, ClickUpCustomField.AndroidAppVersion.id, tag) }
            }.awaitAll().mapNotNull { it.resultOrNull() }
            clickUpTasks.map {
                async { clickUpRepoImpl.updateTask(it.id, targetStatus) }
            }.awaitAll().mapNotNull { it.resultOrNull() }
        }

        val slackUsers = slackRepoImpl.fetchUsers()

        val changelog = slackMessagePayloadCreator.createChangelog(
            tag = tag,
            clickUpTasks = clickUpTasks,
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