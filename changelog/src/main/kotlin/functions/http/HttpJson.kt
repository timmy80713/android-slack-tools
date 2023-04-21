package functions.http

import kotlinx.serialization.json.Json

val httpJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}