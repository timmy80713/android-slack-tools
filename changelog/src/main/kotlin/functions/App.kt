package functions

import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import functions.api.ClickUpApiClientImpl
import functions.api.SlackApiClientImpl
import functions.executor.ExecutorProductionHotfix
import functions.executor.ExecutorRegressionFinish
import functions.executor.ExecutorRegressionHotfix
import functions.executor.ExecutorRegressionStart
import functions.model.ChangelogRequestBody
import functions.model.Workflow
import functions.model.slack.SlackMessagePayloadCreator
import functions.repo.ClickUpRepoImpl
import functions.repo.SlackRepoImpl
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.logging.Logger

class App : HttpFunction {

    private val logger = Logger.getLogger(App::class.java.name)

    override fun service(request: HttpRequest, response: HttpResponse) {
        logger.info("The service has started.")

        val requestBodyString = request.reader.readText()
        logger.info("Request body string  => $requestBodyString")

        val changelogRequestBody = try {
            Json.decodeFromString(ChangelogRequestBody.serializer(), requestBodyString)
        } catch (e: Exception) {
            logger.info("Parse request body occur exception $e")
            response.setStatusCode(500)
            response.writer.write("Internal error")
            return
        }

        val executor = when (changelogRequestBody.workflow) {
            Workflow.RegressionStart -> {
                ExecutorRegressionStart(
                    tag = changelogRequestBody.tag,
                    clickUpRepoImpl = ClickUpRepoImpl(ClickUpApiClientImpl()),
                )
            }

            Workflow.RegressionHotfix -> {
                ExecutorRegressionHotfix(
                    tag = changelogRequestBody.tag,
                    clickUpRepoImpl = ClickUpRepoImpl(ClickUpApiClientImpl()),
                )
            }

            Workflow.RegressionFinish -> {
                ExecutorRegressionFinish(
                    tag = changelogRequestBody.tag,
                    clickUpRepoImpl = ClickUpRepoImpl(ClickUpApiClientImpl()),
                    slackRepoImpl = SlackRepoImpl(SlackApiClientImpl()),
                    slackMessagePayloadCreator = SlackMessagePayloadCreator(),
                )
            }

            Workflow.ProductionHotfix -> {
                ExecutorProductionHotfix(
                    tag = changelogRequestBody.tag,
                    clickUpRepoImpl = ClickUpRepoImpl(ClickUpApiClientImpl()),
                    slackRepoImpl = SlackRepoImpl(SlackApiClientImpl()),
                    slackMessagePayloadCreator = SlackMessagePayloadCreator(),
                )
            }

            else -> null
        }
        logger.info("The executor is ${executor?.javaClass?.simpleName}.")

        try {
            runBlocking { executor?.execute() }
        } catch (e: Exception) {
            logger.info("Occur exception $e")
            response.setStatusCode(500)
            response.writer.write("Internal error")
            return
        }

        response.setStatusCode(200)
        response.writer.write("Ok")
    }
}