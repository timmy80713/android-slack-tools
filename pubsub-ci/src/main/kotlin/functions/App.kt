package functions

import com.google.cloud.functions.CloudEventsFunction
import functions.api.SlackApiStation
import functions.executor.ExecutorBuildApp
import functions.executor.ExecutorDeploy
import functions.model.PubSubBody
import functions.model.PubSubMessagePayload
import functions.model.Whitelist
import functions.model.toRegex
import io.cloudevents.CloudEvent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.*
import java.util.logging.Logger

class App : CloudEventsFunction {

    private val logger = Logger.getLogger(App::class.java.name)

    override fun accept(event: CloudEvent?) {
        logger.info("The service has started.")
        event?.data?.let { cloudEventData ->
            val dataString = String(cloudEventData.toBytes())
            val json = Json {
                ignoreUnknownKeys = true
            }
            val pubSubBody = json.decodeFromString(PubSubBody.serializer(), dataString)
            val encodedData = pubSubBody.message.data
            val decodedData = String(Base64.getDecoder().decode(encodedData), Charsets.UTF_8)
            logger.info("Decoded data: $decodedData")
            val messagePayload = json.decodeFromString(PubSubMessagePayload.serializer(), decodedData)

            val urlPath = messagePayload.urlPath
            val matchResult = urlPath?.run {
                Whitelist.values().map { it.toRegex }.firstNotNullOfOrNull { it.find(this) }
            }
            if (matchResult == null) {
                logger.info("Invalid URL path.")
                runBlocking { SlackApiStation.respondEphemeral(messagePayload.responseUrl, "Not found.") }
                return
            }

            val unformattedWorkflowId = matchResult.groupValues[1]
            val formattedWorkflowId = unformattedWorkflowId.replace("-", "_")
            val executor = when (Whitelist.values().first { it.value == unformattedWorkflowId }) {
                Whitelist.DevelopmentQa -> ExecutorBuildApp(messagePayload, formattedWorkflowId)
                Whitelist.ReleaseRegressionStart,
                Whitelist.ReleaseRegressionHotfix,
                Whitelist.ReleaseRegressionFinish,
                Whitelist.ReleaseProductionHotfix,
                Whitelist.ReleaseProductionFinish -> ExecutorDeploy(messagePayload, formattedWorkflowId)
            }
            logger.info("The executor is ${executor.javaClass.simpleName}.")

            try {
                runBlocking { executor.execute() }
            } catch (e: Exception) {
                logger.info("Occur exception $e")
                return
            }
        }
    }
}