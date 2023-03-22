package functions

import com.google.cloud.functions.CloudEventsFunction
import functions.model.PubSubBody
import io.cloudevents.CloudEvent
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
        }
    }
}