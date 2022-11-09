package functions

import com.google.cloud.functions.CloudEventsFunction
import functions.circleci.CircleCi
import functions.model.PubSubBody
import functions.model.PubSubMessagePayload
import io.cloudevents.CloudEvent
import kotlinx.serialization.json.Json
import java.util.*
import java.util.logging.Logger

class App : CloudEventsFunction {

    private val logger = Logger.getLogger(App::class.java.name)

    override fun accept(event: CloudEvent?) {
        event?.data?.let {
            val dataString = String(it.toBytes())
            logger.info("TimmmmmmY dataString: $dataString")
            val json = Json {
                ignoreUnknownKeys = true
            }
            val pubSubBody = json.decodeFromString(PubSubBody.serializer(), dataString)
            val encodedData = pubSubBody.message.data
            val decodedData = String(Base64.getDecoder().decode(encodedData), Charsets.UTF_8)
            val messagePayload = Json.decodeFromString(PubSubMessagePayload.serializer(), decodedData)
            CircleCi.handleRequest(messagePayload)
        }
    }
}