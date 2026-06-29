package com.example.aingage.network

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Url

interface ApiService {

    // Company auth - dynamic full URL (no auth)
    @GET
    suspend fun checkClient(@Url url: String): Response<JsonObject>

    // User login
    @POST(ApiConstants.USER_LOGIN)
    suspend fun login(
        @HeaderMap headers: Map<String, String>,
        @Body body: JsonObject
    ): Response<JsonObject>

    // Participant lists (Leads / Customer / IM) - dynamic URL with Basic auth
    @GET
    suspend fun getParticipants(
        @Url url: String,
        @HeaderMap headers: Map<String, String>
    ): Response<JsonArray>

    // Chat messages between two participants
    @GET
    suspend fun getMessages(
        @Url url: String,
        @HeaderMap headers: Map<String, String>
    ): Response<JsonArray>

    // Send a text message
    @POST
    suspend fun postMessage(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body body: JsonObject
    ): Response<JsonObject>

    // Get participant by mobile number (used in Reply Review to get ToParticipantId)
    @GET
    suspend fun getParticipantByMobile(
        @Url url: String,
        @HeaderMap headers: Map<String, String>
    ): Response<JsonObject>
}

// Separate interface for BASE_URL2 (reviews)
interface ReviewApiService {

    @GET
    suspend fun getAllReviews(@Url url: String): Response<JsonObject>

    // Review replies: GET BASE_URL2 + reviewTextDetail?clientName=X&id=Y
    @GET
    suspend fun getReviewDetail(@Url url: String): Response<JsonArray>
}
