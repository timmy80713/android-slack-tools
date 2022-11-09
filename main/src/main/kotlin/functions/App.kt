package functions

import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import com.google.cloud.pubsub.v1.Publisher
import com.google.protobuf.ByteString
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import functions.env.Env
import functions.extension.debugMessage
import functions.model.PubSubMessagePayload
import functions.model.RequestResult
import functions.model.doOnFailure
import functions.model.doOnSuccess
import functions.slack.SlackApi
import functions.slack.SlackTeamIds
import functions.slack.SlackVerifier
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import kotlin.system.measureTimeMillis

class App : HttpFunction {

    companion object {
        private val FEATURE_WHITELIST = listOf("build-app")
    }

    private val logger = Logger.getLogger(App::class.java.name)

    private val slackVerifier = SlackVerifier()

    override fun service(request: HttpRequest, response: HttpResponse) {
        logger.info("TimmmmmmY Service execution started.")
        logger.info("TimmmmmmY request uri: ${request.uri}")

        measureTimeMillis {
            handleRequest(request, response)
        }.also {
            logger.info("TimmmmmmY Service execution took ${it}ms.")
        }
    }

    private fun handleRequest(request: HttpRequest, response: HttpResponse) {

        logger.info("TimmmmmmY print request header start.")
        request.headers.forEach { map ->
            logger.info("TimmmmmmY header ${map.key} ==> ${map.value}")
        }
        logger.info("TimmmmmmY print request header finish.")

        val requestBodyString: String
        measureTimeMillis {
            requestBodyString = request.reader.readText()
        }.also {
            logger.info("TimmmmmmY parse request body string took ${it}ms.")
            logger.info("TimmmmmmY requestBodyString: $requestBodyString")
        }

        val requestBody: Map<String, String>
        measureTimeMillis {
            requestBody =
                requestBodyString
                    .split("&")
                    .map { URLDecoder.decode(it, Charsets.UTF_8) }
                    .mapNotNull { keyValuePair -> keyValuePair.split("=").let { if (it.size == 2) it else null } }
                    .associate { Pair(it[0], it[1]) }
        }.also {
            logger.info("TimmmmmmY parse request body map took ${it}ms.")
            logger.info("TimmmmmmY print request body start.")
            requestBody.forEach { map ->
                logger.info("TimmmmmmY requestBody ${map.key} ==> ${map.value}")
            }
            logger.info("TimmmmmmY print request body finish.")
        }

        val isValidSlackWebhook: Boolean
        measureTimeMillis {
            isValidSlackWebhook = slackVerifier.isValidSlackWebhook(request, requestBodyString)
        }.also {
            logger.info("TimmmmmmY Slack request verification took ${it}ms.")
        }
        if (isValidSlackWebhook.not()) {
            SlackApi.acknowledgeEphemeral(response, "No slack webhook.")
            return
        }

        val teamId = requestBody["team_id"]
        if (teamId != SlackTeamIds.DCARD) {
            logger.warning("TimmmmmmY Incoming foreign request from $teamId.")
            response.respondNotFound()
            return
        }
        val userId = requestBody["user_id"] ?: run {
            logger.warning("TimmmmmmY Incoming request without user ID.")
            SlackApi.acknowledgeEphemeral(response = response, text = "User ID is missing.")
            return
        }
        val userName = requestBody["user_name"] ?: run {
            logger.warning("TimmmmmmY Incoming request without user name.")
            SlackApi.acknowledgeEphemeral(response = response, text = "User name is missing.")
            return
        }
        val responseUrl = requestBody["response_url"] ?: run {
            logger.warning("TimmmmmmY Incoming request without response URL.")
            SlackApi.acknowledgeEphemeral(response = response, text = "Response URL is missing.")
            return
        }

        val pathSegments = request.path.removePrefix("/").split("/")
        val featureName = pathSegments.getOrNull(0)
        val text = requestBody["text"]?.takeIf { it.isNotBlank() }?.trim() ?: ""
        if ((featureName != null && FEATURE_WHITELIST.contains(featureName)).not()) {
            logger.warning("TimmmmmmYIncoming illegal request from $teamId/$userId($userName) for $featureName with text $text.")
            response.respondNotFound()
            return
        }
        logger.info("TimmmmmmY Incoming request from $teamId/$userId($userName) for $featureName with text $text.")

        val pubSubMessagePayloadString: String
        measureTimeMillis {
            pubSubMessagePayloadString = Json.encodeToString(
                PubSubMessagePayload.serializer(),
                PubSubMessagePayload(
                    userId = userId,
                    userName = userName,
                    responseUrl = responseUrl,
                    text = text,
                ),
            )
        }.also {
            logger.info("TimmmmmmY Pub/Sub message encoding took ${it}ms.")
        }

        val publishResult: RequestResult<String>
        measureTimeMillis {
            publishResult = publishMessage("slack-tools.$featureName", pubSubMessagePayloadString)
        }.also {
            logger.info("TimmmmmmY Pub/Sub message publishing took ${it}ms.")
        }

        publishResult.doOnSuccess { messageId ->
            SlackApi.acknowledgeEphemeral(
                response = response,
                text = if (text.isEmpty()) {
                    "Received `$featureName` request. (mid=$messageId)"
                } else {
                    "Received `$featureName` request with text `$text`. (mid=$messageId)"
                },
            )
        }.doOnFailure {
            SlackApi.acknowledgeEphemeral(
                response = response,
                text = "Publishing Pub/Sub event failed: ${it.debugMessage}",
            )
        }
    }

    private fun publishMessage(
        topicName: String,
        message: String,
    ): RequestResult<String> {
        logger.info("TimmmmmmY Publishing Pub/Sub message. topic=$topicName, message=$message")

        val data: ByteString
        measureTimeMillis {
            data = ByteString.copyFromUtf8(message)
        }.also {
            logger.info("TimmmmmmY Pub/Sub ByteString.copyFromUtf8 took ${it}ms.")
        }

        val pubsubMessage: PubsubMessage
        measureTimeMillis {
            pubsubMessage = PubsubMessage.newBuilder().setData(data).build()
        }.also {
            logger.info("TimmmmmmY Pub/Sub PubsubMessage took ${it}ms.")
        }

        val builder: Publisher.Builder
        measureTimeMillis {
            builder = Publisher.newBuilder(
                ProjectTopicName.of(Env.get(Env.GOOGLE_CLOUD_PROJECT_ID), topicName),
            )
        }.also {
            logger.info("TimmmmmmY Pub/Sub Publisher.newBuilder() took ${it}ms.")
        }

        val publisher: Publisher
        measureTimeMillis {
            publisher = builder.build()
        }.also {
            logger.info("TimmmmmmY Pub/Sub Publisher.build() took ${it}ms.")
        }

        val result: RequestResult<String>
        measureTimeMillis {
            result = try {
                val messageId = publisher.publish(pubsubMessage).get()
                RequestResult.Success(messageId)
            } catch (e: Exception) {
                RequestResult.Failed(e)
            } finally {
                // When finished with the publisher, shutdown to free up resources.
                publisher.shutdown()
                publisher.awaitTermination(1, TimeUnit.MINUTES)
            }
        }.also {
            logger.info("TimmmmmmY Pub/Sub Publisher.publish().get() took ${it}ms.")
        }
        return result
    }

    private fun HttpResponse.respondNotFound() {
        setStatusCode(404)
        writer.write("Not found.")
    }
}