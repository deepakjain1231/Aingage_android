package com.example.aingage.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject

object PreferenceManager {

    private const val PREF_NAME = "AingagePrefs"

    // Keys (matching iOS UserDefaults keys)
    private const val KEY_COMPANY_CODE = "CompanyCode"
    private const val KEY_USER_ID = "userID"
    private const val KEY_USER_PASS = "userPass"
    private const val KEY_AUTH_TOKEN = "auth_Token"
    private const val KEY_LOGIN_DETAILS = "LoginUserDetails"
    private const val KEY_PARTICIPANT_ID = "ParticipantId"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ─── Company ──────────────────────────────────────────────

    fun saveCompanyData(context: Context, data: JsonObject) {
        prefs(context).edit().putString(KEY_COMPANY_CODE, data.toString()).apply()
    }

    fun getCompanyData(context: Context): JsonObject? {
        val json = prefs(context).getString(KEY_COMPANY_CODE, null) ?: return null
        return runCatching { Gson().fromJson(json, JsonObject::class.java) }.getOrNull()
    }

    fun getCompanyName(context: Context): String? =
        getCompanyData(context)
            ?.takeIf { it.has("Name") && !it.get("Name").isJsonNull }
            ?.get("Name")?.asString

    // ─── Login Credentials ────────────────────────────────────

    fun saveUserCredentials(context: Context, userId: String, password: String) {
        prefs(context).edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_PASS, password)
            .apply()
    }

    fun getSavedUserId(context: Context): String? =
        prefs(context).getString(KEY_USER_ID, null)

    fun getSavedUserPass(context: Context): String? =
        prefs(context).getString(KEY_USER_PASS, null)

    fun clearUserCredentials(context: Context) {
        prefs(context).edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USER_PASS)
            .apply()
    }

    // ─── Auth Token ───────────────────────────────────────────

    fun saveAuthToken(context: Context, token: String) {
        prefs(context).edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getAuthToken(context: Context): String? =
        prefs(context).getString(KEY_AUTH_TOKEN, null)

    // ─── Login Response ───────────────────────────────────────

    fun saveLoginDetails(context: Context, data: JsonObject) {
        prefs(context).edit().putString(KEY_LOGIN_DETAILS, data.toString()).apply()
    }

    fun getLoginDetails(context: Context): JsonObject? {
        val json = prefs(context).getString(KEY_LOGIN_DETAILS, null) ?: return null
        return runCatching { Gson().fromJson(json, JsonObject::class.java) }.getOrNull()
    }

    fun clearLoginDetails(context: Context) {
        prefs(context).edit()
            .remove(KEY_LOGIN_DETAILS)
            .remove(KEY_AUTH_TOKEN)
            .apply()
    }

    fun saveParticipantId(context: Context, id: Int) {
        prefs(context).edit().putInt(KEY_PARTICIPANT_ID, id).apply()
    }

    fun getParticipantId(context: Context): Int =
        prefs(context).getInt(KEY_PARTICIPANT_ID, 0)
}
