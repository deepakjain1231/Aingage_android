package com.example.aingage.ui

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

        adapter = ReviewAdapter(emptyList()) { /* TODO: open reply screen */ }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadReviews()
    }

    private fun loadReviews() {
        progressBar.visibility = View.VISIBLE

        // Get company name - try AppSession first, fallback to SharedPrefs
        val companyName = AppSession.companyName.ifEmpty {
            PreferenceManager.getCompanyName(requireContext()) ?: ""
        }

        // Matches iOS: BASE_URL2 + GET_ALL_REVIEWS + "clientName=X&limit=50&pageno=0"
        val url = ApiConstants.BASE_URL2 +
                ApiConstants.GET_ALL_REVIEWS +
                "clientName=${java.net.URLEncoder.encode(companyName, "UTF-8")}&limit=50&pageno=0"

        Log.d("ReviewsFragment", "Loading reviews from: $url")
        Log.d("ReviewsFragment", "Company name: $companyName")

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.reviewApiService.getAllReviews(url)

                progressBar.visibility = View.GONE
                Log.d("ReviewsFragment", "Response code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    Log.d("ReviewsFragment", "Response body: ${response.body()}")
                    parseReviews(response.body()!!)
                } else {
                    Log.e("ReviewsFragment", "Error: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Log.e("ReviewsFragment", "Exception: ${e.message}", e)
            }
        }
    }

    private fun parseReviews(json: JsonObject) {
        try {
            Log.d("ReviewsFragment", "Parsing: $json")

            // Structure: { "data": [ { "location_reviews": [ {review}, {review}, ... ] } ] }
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
                        displayName = obj.optString("displayName"),
                        comment = obj.optString("comment"),
                        starRating = starCount,
                        createDate = date,
                        profilePhotoUrl = obj.optString("profilePhotoUrl")
                    )
                }.getOrNull()
            }

            Log.d("ReviewsFragment", "Parsed ${items.size} reviews")
            adapter.updateData(items)

        } catch (e: Exception) {
            Log.e("ReviewsFragment", "Parse error: ${e.message}", e)
        }
    }
}
