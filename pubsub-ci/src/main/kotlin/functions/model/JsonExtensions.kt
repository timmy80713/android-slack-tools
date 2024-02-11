package functions.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull as primitiveBooleanOrNull
import kotlinx.serialization.json.contentOrNull as primitiveContentOrNull
import kotlinx.serialization.json.doubleOrNull as primitiveDoubleOrNull
import kotlinx.serialization.json.intOrNull as primitiveIntOrNull
import kotlinx.serialization.json.longOrNull as primitiveLongOrNull

val JsonElement.jsonObjectOrNull: JsonObject?
    get() = this as? JsonObject

val JsonElement.jsonPrimitiveOrNull: JsonPrimitive?
    get() = this as? JsonPrimitive

val JsonElement.jsonArrayOrNull: JsonArray?
    get() = this as? JsonArray

val JsonElement.booleanOrNull: Boolean?
    get() = jsonPrimitiveOrNull?.primitiveBooleanOrNull

val JsonElement.doubleOrNull: Double?
    get() = jsonPrimitiveOrNull?.primitiveDoubleOrNull

val JsonElement.intOrNull: Int?
    get() = jsonPrimitiveOrNull?.primitiveIntOrNull

val JsonElement.longOrNull: Long?
    get() = jsonPrimitiveOrNull?.primitiveLongOrNull

val JsonElement.contentOrNull: String?
    get() = jsonPrimitiveOrNull?.primitiveContentOrNull