package org.brightmindenrichment.street_care.util

import com.google.firebase.Timestamp
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.brightmindenrichment.street_care.ui.visit.data.InteractionLog
import java.lang.reflect.Type

object InteractionLogDraftSerializer {

    private val gson = GsonBuilder()
        .registerTypeAdapter(
            Timestamp::class.java,
            object : JsonSerializer<Timestamp>, JsonDeserializer<Timestamp> {
                override fun serialize(
                    src: Timestamp?,
                    typeOfSrc: Type?,
                    context: JsonSerializationContext?
                ): JsonElement {
                    val obj = JsonObject()
                    obj.addProperty("seconds", src?.seconds ?: 0L)
                    obj.addProperty("nanoseconds", src?.nanoseconds ?: 0)
                    return obj
                }

                override fun deserialize(
                    json: JsonElement?,
                    typeOfT: Type?,
                    context: JsonDeserializationContext?
                ): Timestamp? {
                    val obj = json?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
                    val seconds = obj.get("seconds")?.asLong ?: return null
                    val nanoseconds = obj.get("nanoseconds")?.asInt ?: 0
                    return Timestamp(seconds, nanoseconds)
                }
            }
        )
        .create()

    fun serialize(log: InteractionLog): String = gson.toJson(log)

    fun deserialize(json: String): InteractionLog? = try {
        gson.fromJson(json, InteractionLog::class.java)
    } catch (e: Exception) {
        null
    }
}
