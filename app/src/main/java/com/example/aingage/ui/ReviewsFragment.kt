package com.example.aingage.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aingage.R
import com.example.aingage.ReplyReviewActivity
import com.example.aingage.adapter.ReviewAdapter
import com.example.aingage.model.ReviewItem
import com.example.aingage.network.ApiConstants
import com.example.aingage.network.RetrofitClient
import com.example.aingage.utils.AppSession
import com.example.aingage.utils.PreferenceManager
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

class ReviewsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: ReviewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_reviews, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar)

        adapter = ReviewAdapter(emptyList()) { item ->
            val intent = Intent(requireContext(), ReplyReviewActivity::class.java).apply {
                putExtra(ReplyReviewActivity.EXTRA_REVIEW_ID, item.id)
                putExtra(ReplyReviewActivity.EXTRA_DISPLAY_NAME, item.displayName)
                putExtra(ReplyReviewActivity.EXTRA_COMMENT, item.comment)
                putExtra(ReplyReviewActivity.EXTRA_STAR_RATING, item.starRating)
                putExtra(ReplyReviewActivity.EXTRA_PROFILE_PHOTO_URL, item.profilePhotoUrl)
                putExtra(ReplyReviewActivity.EXTRA_PHONE_NO, item.phoneNo)
            }
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadReviews()
    }

    private fun loadReviews() {
        progressBar.visibility = View.VISIBLE

        val companyName = AppSession.companyName.ifEmpty {
            PreferenceManager.getCompanyName(requireContext()) ?: ""
        }

        val url = ApiConstants.BASE_URL2 +
                ApiConstants.GET_ALL_REVIEWS +
                "clientName=${java.net.URLEncoder.encode(companyName, "UTF-8")}&limit=50&pageno=0"

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.reviewApiService.getAllReviews(url)
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    parseReviews(response.body()!!)
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Log.e("ReviewsFragment", "Exception: ${e.message}", e)
            }
        }
    }

    private fun parseReviews(json: JsonObject) {
        try {
            val dataArray = json.getAsJsonArray("data")
            val reviewsArray = dataArray.get(0).asJsonObject
                .getAsJsonArray("location_reviews")

            val items = reviewsArray.mapNotNull { el ->
                runCatching {
                    val obj = el.asJsonObject
                    val starText = obj.optString("starRating")
                    val starCount = when (starText) {
                        "ONE" -> 1; "TWO" -> 2; "THREE" -> 3; "FOUR" -> 4; "FIVE" -> 5; else -> 0
                    }
                    val dateRaw = obj.optString("createTime")
                    val date = dateRaw.split(" ").firstOrNull() ?: dateRaw

                    ReviewItem(
                        id = obj.optString("id"),
                        displayName = obj.optString("displayName"),
                        comment = obj.optString("comment"),
                        starRating = starCount,
                        createDate = date,
                        profilePhotoUrl = obj.optString("profilePhotoUrl"),
                        phoneNo = obj.optString("phoneNo")
                    )
                }.getOrNull()
            }

            adapter.updateData(items)
        } catch (e: Exception) {
            Log.e("ReviewsFragment", "Parse error: ${e.message}", e)
        }
    }
}
