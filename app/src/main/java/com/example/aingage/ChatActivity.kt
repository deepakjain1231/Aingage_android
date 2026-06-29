package com.example.aingage

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aingage.adapter.ChatAdapter
import com.example.aingage.model.MessageItem
import com.example.aingage.network.ApiConstants
import com.example.aingage.network.RetrofitClient
import com.example.aingage.network.SignalRManager
import com.example.aingage.utils.AppSession
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CONTACT_NAME = "contact_name"
        const val EXTRA_PARTICIPANT_ID = "participant_id"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvContactName: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: ChatAdapter

    private var contactParticipantId = 0
    private var contactName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        contactName = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: ""
        contactParticipantId = intent.getIntExtra(EXTRA_PARTICIPANT_ID, 0)

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnBack = findViewById(R.id.btnBack)
        tvContactName = findViewById(R.id.tvContactName)

        tvContactName.text = contactName

        adapter = ChatAdapter(emptyList(), AppSession.participantId)
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerView.adapter = adapter

        btnBack.setOnClickListener { finish() }

        btnSend.setOnClickListener { sendMessage() }

        etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }

        // Observe SignalR events → auto-refresh chat (matches iOS messagesWithParticipantAPICalling notification)
        lifecycleScope.launch {
            SignalRManager.chatUpdated.collect {
                loadMessages(showLoader = false)
            }
        }

        loadMessages()
    }

    private fun loadMessages(showLoader: Boolean = true) {
        if (showLoader) progressBar.visibility = View.VISIBLE

        val url = ApiConstants.BASE_URL +
                ApiConstants.MESSAGE_WITH_PARTICIPANT +
                "person1=$contactParticipantId&person2=${AppSession.participantId}"

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getMessages(url, AppSession.authHeader())
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    parseMessages(response.body()!!)
                }
            } catch (_: Exception) {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun parseMessages(array: com.google.gson.JsonArray) {
        val items = array.mapNotNull { el ->
            runCatching {
                val obj = el.asJsonObject
                val fromObj = obj.getAsJsonObject("FromParticipant")
                val toObj = obj.getAsJsonObject("ToParticipant")
                MessageItem(
                    message = obj.optString("Message"),
                    messageType = obj.optString("MessageType"),
                    filename = obj.optString("Filename"),
                    addedOn = obj.optString("AddedOn"),
                    fromParticipantId = fromObj?.optInt("ParticipantId") ?: 0,
                    toParticipantId = toObj?.optInt("ParticipantId") ?: 0
                )
            }.getOrNull()
        }
        adapter.updateData(items)
        if (items.isNotEmpty()) {
            recyclerView.scrollToPosition(items.size - 1)
        }
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return

        etMessage.setText("")

        // Optimistic UI: append message immediately with current local time
        val nowFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss a", Locale.getDefault()).format(Date())
        val optimisticItem = MessageItem(
            message = text,
            messageType = "TEXT",
            filename = "",
            addedOn = nowFormatted,
            fromParticipantId = AppSession.participantId,
            toParticipantId = contactParticipantId
        )
        adapter.appendMessage(optimisticItem)
        recyclerView.scrollToPosition(adapter.itemCount - 1)

        // API call in background — refresh list on success
        val url = ApiConstants.BASE_URL + ApiConstants.POST_MESSAGE
        val body = JsonObject().apply {
            addProperty("ToParticipantId", contactParticipantId.toString())
            addProperty("FromParticipantId", AppSession.participantId.toString())
            addProperty("Message", text)
        }

        lifecycleScope.launch {
            try {
                RetrofitClient.apiService.postMessage(url, AppSession.authHeader(), body)
                delay(300)
                loadMessages(showLoader = false) // replace optimistic with real server data
            } catch (_: Exception) {}
        }
    }

    // Extension helpers used locally
    private fun com.google.gson.JsonObject.optString(key: String) =
        takeIf { has(key) && !get(key).isJsonNull }?.get(key)?.asString ?: ""

    private fun com.google.gson.JsonObject.optInt(key: String) =
        takeIf { has(key) && !get(key).isJsonNull }?.get(key)?.asInt ?: 0
}
