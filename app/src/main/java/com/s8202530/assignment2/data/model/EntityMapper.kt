package com.s8202530.assignment2.data.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Turns the API's raw JSON objects into [Entity] objects.
 * Pure Kotlin with no Android code, so it is unit tested (EntityMapperTest).
 */
object EntityMapper {

    private const val DESCRIPTION_KEY = "description"

    fun map(json: JsonObject): Entity {
        var description: String? = null
        val fields = mutableListOf<EntityField>()

        for ((key, value) in json.entrySet()) {
            val text = value.asDisplayText()
            if (key.equals(DESCRIPTION_KEY, ignoreCase = true)) {
                description = text.ifBlank { null }
            } else {
                fields += EntityField(key = key, label = labelFor(key), value = text)
            }
        }
        return Entity(fields, description)
    }

    /** "artistName" -> "Artist Name", "date_of_birth" -> "Date Of Birth", "yearReleased" -> "Year Released" */
    fun labelFor(key: String): String =
        key.replace('_', ' ')
            .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercaseChar() } }

    /** Numbers and text as they are; lists joined with commas; null as empty. */
    private fun JsonElement?.asDisplayText(): String = when {
        this == null || isJsonNull -> ""
        isJsonPrimitive -> asJsonPrimitive.asString
        isJsonArray -> asJsonArray.joinToString(", ") { it.asDisplayText() }
        else -> toString()
    }
}
