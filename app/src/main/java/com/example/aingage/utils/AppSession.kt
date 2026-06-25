package com.example.aingage.utils

import android.content.Context

object AppSession {
    var authToken: String = ""
    var participantId: Int = 0
    var companyName: String = ""

    fun init(context: Context) {
        authToken = PreferenceManager.getAuthToken(context) ?: ""
        participantId = PreferenceManager.getParticipantId(context)
        companyName = PreferenceManager.getCompanyName(context) ?: ""
    }

    fun clear() {
        authToken = ""
        participantId = 0
    }

    fun authHeader(): Map<String, String> =
        mapOf("Authorization" to "Basic $authToken")
}
