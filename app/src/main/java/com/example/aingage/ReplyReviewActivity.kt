package com.example.aingage

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import android.view.Gravity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.aingage.network.ApiConstants
import com.example.aingage.network.RetrofitClient
import com.example.aingage.utils.AppSession
import com.example.aingage.utils.PreferenceManager
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

class ReplyReviewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REVIEW_ID = "review_id"
        const val EXTRA_DISPLAY_NAME = "display_name"
        const val EXTRA_COMMENT = "comment"
        const val EXTRA_STAR_RATING = "star_rating"
        const val EXTRA_PROFILE_PHOTO_URL = "profile_photo_url"
        const val EXTRA_PHONE_NO = "phone_no"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var etReply: EditText
    private lateinit var btnSend: ImageButton

    private var reviewId = ""
    private var originalComment = ""
    private var phoneNo = ""
    private val replyTexts = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reply_review)

        reviewId = intent.getStringExtra(EXTRA_REVIEW_ID) ?: ""
        originalComment = intent.getStringExtra(EXTRA_COMMENT) ?: ""
        phoneNo = intent.getStringExtra(EXTRA_PHONE_NO) ?: ""
        val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: ""
        val starRating = intent.getIntExtra(EXTRA_STAR_RATING, 0)
        val profilePhotoUrl = intent.getStringExtra(EXTRA_PROFILE_PHOTO_URL) ?: ""

        // Toolbar back
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Avatar
        val ivAvatar = findViewById<ImageView>(R.id.ivAvatar)
        val tvInitial = findViewById<TextView>(R.id.tvAvatarInitial)
        if (profilePhotoUrl.isNotEmpty()) {
            ivAvatar.visibility = View.VISIBLE
            tvInitial.visibility = View.GONE
            ivAvatar.load(profilePhotoUrl) {
                transformations(CircleCropTransformation())
            }
        } else {
            ivAvatar.visibility = View.GONE
            tvInitial.visibility = View.VISIBLE
            tvInitial.text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        }

        findViewById<TextView>(R.id.tvDisplayName).text = displayName
        findViewById<RatingBar>(R.id.ratingBar).rating = starRating.toFloat()
        val tvPhone = findViewById<TextView>(R.id.tvPhoneNo)
        tvPhone.text = phoneNo
        tvPhone.visibility = if (phoneNo.isEmpty()) View.GONE else View.VISIBLE

        recyclerView = findViewById(R.id.recyclerView)
        etReply = findViewById(R.id.etReply)
        btnSend = findViewById(R.id.btnSend)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ReplyListAdapter()

        btnSend.setOnClickListener { sendReply() }

        if (reviewId.isNotEmpty()) loadReviewDetail()
    }

    private fun loadReviewDetail() {
        val companyName = AppSession.companyName.ifEmpty {
            PreferenceManager.getCompanyName(this) ?: ""
        }
        val url = ApiConstants.BASE_URL2 +
                ApiConstants.REVIEW_TEXT_DETAIL +
                "clientName=${java.net.URLEncoder.encode(companyName, "UTF-8")}&id=$reviewId"

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.reviewApiService.getReviewDetail(url)
                if (response.isSuccessful && response.body() != null) {
                    replyTexts.clear()
                    response.body()!!.forEach { el ->
                        val obj = runCatching { el.asJsonObject }.getOrNull() ?: return@forEach
                        val comment = obj.optString("comment")
                        if (comment.isNotEmpty()) replyTexts.add(comment)
                    }
                    recyclerView.adapter?.notifyDataSetChanged()
                }
            } catch (_: Exception) {}
        }
    }

    private fun sendReply() {
        val text = etReply.text.toString().trim()
        if (text.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("AI Agent")
                .setMessage("Please add message")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        if (phoneNo.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("AI Agent")
                .setMessage("User doesn't have a phone number.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Step 1: get participant ID from phone, then post message
        val url = ApiConstants.BASE_URL +
                String.format(ApiConstants.GET_PARTICIPANT_BY_MOBILE, phoneNo)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getParticipantByMobile(
                    url, AppSession.authHeader()
                )
                if (response.isSuccessful && response.body() != null) {
                    val participantId = response.body()!!.optInt("Id")
                    if (participantId != 0) {
                        postMessage(participantId, text)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private suspend fun postMessage(toParticipantId: Int, message: String) {
        val url = ApiConstants.BASE_URL + ApiConstants.POST_MESSAGE
        val body = JsonObject().apply {
            addProperty("ToParticipantId", toParticipantId.toString())
            addProperty("FromParticipantId", AppSession.participantId.toString())
            addProperty("Message", message)
        }
        try {
            val response = RetrofitClient.apiService.postMessage(url, AppSession.authHeader(), body)
            if (response.isSuccessful) {
                etReply.setText("")
                AlertDialog.Builder(this@ReplyReviewActivity)
                    .setTitle("AI Agent")
                    .setMessage("Message sent successfully")
                    .setPositiveButton("OK", null)
                    .show()
                loadReviewDetail()
            }
        } catch (_: Exception) {}
    }

    private fun JsonObject.optString(key: String) =
        takeIf { has(key) && !get(key).isJsonNull }?.get(key)?.asString ?: ""

    private fun JsonObject.optInt(key: String) =
        takeIf { has(key) && !get(key).isJsonNull }?.get(key)?.asInt ?: 0

    // ── Inline adapter for the reply list ────────────────────────────────────

    inner class ReplyListAdapter : RecyclerView.Adapter<ReplyListAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val container: android.widget.LinearLayout = view.findViewById(R.id.replyContainer)
            val card: CardView = view.findViewById(R.id.replyCard)
            val tvComment: TextView = view.findViewById(R.id.tvComment)
        }

        // Row 0 = original review comment (left), rows 1+ = staff replies (right)
        override fun getItemCount() = if (originalComment.isEmpty()) replyTexts.size
                                      else replyTexts.size + 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context).inflate(R.layout.item_reply, parent, false)
        )

        override fun onBindViewHolder(holder: VH, position: Int) {
            if (position == 0 && originalComment.isNotEmpty()) {
                // Original review comment — left aligned, white bubble
                holder.tvComment.text = originalComment
                holder.container.gravity = Gravity.START
                holder.card.setCardBackgroundColor(Color.WHITE)
                holder.tvComment.setTextColor(Color.BLACK)
                setHorizontalMargins(holder.card, startDp = 0, endDp = 60)
            } else {
                // Staff reply — right aligned, teal bubble
                val index = if (originalComment.isNotEmpty()) position - 1 else position
                holder.tvComment.text = replyTexts[index]
                holder.container.gravity = Gravity.END
                holder.card.setCardBackgroundColor(getColor(R.color.aingage_teal))
                holder.tvComment.setTextColor(Color.WHITE)
                setHorizontalMargins(holder.card, startDp = 60, endDp = 0)
            }
        }

        private fun setHorizontalMargins(view: View, startDp: Int, endDp: Int) {
            val density = view.context.resources.displayMetrics.density
            val params = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
            params.marginStart = (startDp * density).toInt()
            params.marginEnd = (endDp * density).toInt()
            view.layoutParams = params
        }
    }
}
