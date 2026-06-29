package com.example.aingage.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SignalRManager {

    private const val TAG = "SignalRManager"
    private const val SIGNALR_HOST = "texting.iconnectgroup.com"
    private const val SIGNALR_BASE = "https://$SIGNALR_HOST/signalR"
    private const val HUB_NAME = "MessageHub"
    private const val CONNECTION_DATA = """[{"name":"messagehub"}]"""

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _chatUpdated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val chatUpdated: SharedFlow<Unit> = _chatUpdated

    private val _contactsUpdated = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val contactsUpdated: SharedFlow<Unit> = _contactsUpdated

    private var pollJob: Job? = null
    private var previousTimestamp = 0L
    private var currentCipher = ""
    private var lastMessageId = ""

    // Long timeout client for long polling (110s — server holds connection ~100s)
    private val longPollClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Short timeout client for negotiate / start requests
    private val shortClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // ── Public ────────────────────────────────────────────────────────────────

    fun connect(cipher: String) {
        if (cipher.isEmpty()) { Log.e(TAG, "Cipher empty"); return }
        currentCipher = cipher
        previousTimestamp = 0L
        lastMessageId = ""
        Log.d(TAG, "Starting SignalR long polling...")
        pollJob?.cancel()
        pollJob = scope.launch { startLongPolling(cipher) }
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting")
        pollJob?.cancel()
        pollJob = null
        currentCipher = ""
    }

    // ── Long polling flow ─────────────────────────────────────────────────────

    private suspend fun startLongPolling(cipher: String) {
        try {
            // Step 1: Negotiate
            val (connectionToken, _) = negotiate(cipher) ?: run {
                Log.e(TAG, "Negotiate failed — retry in 5s")
                delay(5000)
                if (currentCipher == cipher) startLongPolling(cipher)
                return
            }
            Log.d(TAG, "Negotiate OK")

            // Step 2: Initial connect (activates the connection on server)
            initialConnect(cipher, connectionToken)

            // Step 3: Call /start
            callStart(cipher, connectionToken)

            // Step 4: Poll loop
            pollLoop(cipher, connectionToken)

        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message} — retry in 5s")
            delay(5000)
            if (currentCipher == cipher) startLongPolling(cipher)
        }
    }

    private fun negotiate(cipher: String): Pair<String, String>? {
        val url = buildUrl("$SIGNALR_BASE/negotiate", cipher, null)
        Log.d(TAG, "Negotiate → $url")
        val resp = shortClient.newCall(Request.Builder().url(url).build()).execute()
        val body = resp.body?.string() ?: ""
        Log.d(TAG, "Negotiate ← ${resp.code} $body")
        if (!resp.isSuccessful) return null
        val json = JSONObject(body)
        val token = json.optString("ConnectionToken").takeIf { it.isNotEmpty() } ?: return null
        val id = json.optString("ConnectionId")
        return token to id
    }

    private fun initialConnect(cipher: String, connectionToken: String) {
        val url = buildUrl("$SIGNALR_BASE/connect", cipher, connectionToken,
            "transport" to "longPolling")
        Log.d(TAG, "Connect → $url")
        try {
            val resp = shortClient.newCall(Request.Builder().url(url).build()).execute()
            val body = resp.body?.string() ?: ""
            Log.d(TAG, "Connect ← ${resp.code} $body")
            // Extract initial messageId from connect response
            runCatching {
                val json = JSONObject(body)
                val c = json.optString("C")
                if (c.isNotEmpty()) lastMessageId = c
            }
        } catch (e: Exception) {
            Log.w(TAG, "Initial connect failed (non-fatal): ${e.message}")
        }
    }

    private fun callStart(cipher: String, connectionToken: String) {
        val url = buildUrl("$SIGNALR_BASE/start", cipher, connectionToken,
            "transport" to "longPolling")
        Log.d(TAG, "Start → $url")
        try {
            val resp = shortClient.newCall(Request.Builder().url(url).build()).execute()
            Log.d(TAG, "Start ← ${resp.code} ${resp.body?.string()}")
        } catch (e: Exception) {
            Log.w(TAG, "Start failed (non-fatal): ${e.message}")
        }
    }

    // Continuous long poll loop
    private suspend fun pollLoop(cipher: String, connectionToken: String) {
        Log.d(TAG, "✅ Poll loop started")
        while (currentCoroutineContext().isActive && currentCipher == cipher) {
            try {
                val urlBuilder = "$SIGNALR_BASE/poll".toHttpUrl().newBuilder()
                    .addQueryParameter("transport", "longPolling")
                    .addQueryParameter("clientProtocol", "1.5")
                    .addQueryParameter("connectionToken", connectionToken)
                    .addQueryParameter("connectionData", CONNECTION_DATA)
                    .addQueryParameter("token", cipher)
                if (lastMessageId.isNotEmpty()) {
                    urlBuilder.addQueryParameter("messageId", lastMessageId)
                }
                val url = urlBuilder.build()
                Log.d(TAG, "Poll → $url")

                val resp = longPollClient.newCall(Request.Builder().url(url).build()).execute()
                val body = resp.body?.string() ?: ""
                Log.d(TAG, "Poll ← ${resp.code} $body")

                if (resp.isSuccessful && body.isNotEmpty()) {
                    handleMessage(body)
                } else if (!resp.isSuccessful) {
                    Log.e(TAG, "Poll error ${resp.code} — retry in 3s")
                    delay(3000)
                }
            } catch (e: Exception) {
                if (!currentCoroutineContext().isActive) break
                Log.e(TAG, "Poll exception: ${e.message} — retry in 3s")
                delay(3000)
            }
        }
    }

    // ── URL builder helper ────────────────────────────────────────────────────

    private fun buildUrl(
        base: String,
        cipher: String,
        connectionToken: String?,
        vararg extra: Pair<String, String>
    ): okhttp3.HttpUrl {
        return base.toHttpUrl().newBuilder().apply {
            addQueryParameter("clientProtocol", "1.5")
            addQueryParameter("connectionData", CONNECTION_DATA)
            addQueryParameter("token", cipher)
            connectionToken?.let { addQueryParameter("connectionToken", it) }
            extra.forEach { (k, v) -> addQueryParameter(k, v) }
        }.build()
    }

    // ── Message parsing ───────────────────────────────────────────────────────

    private fun handleMessage(text: String) {
        if (text.trim() == "{}") return
        runCatching {
            val json = JSONObject(text)

            // Update cursor (messageId) for next poll
            val c = json.optString("C")
            if (c.isNotEmpty()) lastMessageId = c

            val messages = json.optJSONArray("M") ?: return

            for (i in 0 until messages.length()) {
                val msg = messages.getJSONObject(i)
                val hub = msg.optString("H")
                val method = msg.optString("M")
                val args: JSONArray = msg.optJSONArray("A") ?: JSONArray()

                Log.d(TAG, "Hub=$hub Method=$method Args=$args")
                if (!hub.equals(HUB_NAME, ignoreCase = true)) continue

                when (method) {
                    "newMessageFromCustomer" -> {
                        val ts = args.optString(0).toLongOrNull() ?: 0L
                        if (ts > previousTimestamp) { previousTimestamp = ts; notifyUpdates() }
                    }
                    "messageReceived" -> {
                        val ts = args.optString(2).toLongOrNull() ?: 0L
                        if (ts > previousTimestamp) { previousTimestamp = ts; notifyUpdates() }
                    }
                }
            }
        }
    }

    private fun notifyUpdates() {
        Log.d(TAG, "🔔 Notifying update")
        scope.launch { _chatUpdated.emit(Unit) }
        scope.launch { _contactsUpdated.emit(Unit) }
    }
}
