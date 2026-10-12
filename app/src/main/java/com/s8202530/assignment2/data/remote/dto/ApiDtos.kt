package com.s8202530.assignment2.data.remote.dto

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/** Body for POST /{campus}/auth */
data class LoginRequest(
    @SerializedName("username") val username: String, // student ID, e.g. 8202530
    @SerializedName("password") val password: String, // first name, capitals matter
)

/** Response from POST /{campus}/auth, e.g. {"keypass": "art"} */
data class LoginResponse(
    @SerializedName("keypass") val keypass: String?,
)

/**
 * Response from GET /dashboard/{keypass}.
 * Each topic has different field names (artistName, title, species...),
 * so every entity is kept as a raw JSON object and mapped later.
 */
data class DashboardResponse(
    @SerializedName("entities") val entities: List<JsonObject>?,
    @SerializedName("entityTotal") val entityTotal: Int?,
)
