package com.example.aingage.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.aingage.ChatActivity
import com.example.aingage.R
import com.example.aingage.adapter.ContactAdapter
import com.example.aingage.model.ContactItem
import com.example.aingage.network.ApiConstants
import com.example.aingage.network.RetrofitClient
import com.example.aingage.utils.AppSession
import com.google.gson.JsonArray
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class ContactListFragment : Fragment() {

    abstract val listEndpoint: String
    abstract val searchEndpoint: String

    private lateinit var recyclerView: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var tvEmpty: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: ContactAdapter

    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_contact_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        etSearch = view.findViewById(R.id.etSearch)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        progressBar = view.findViewById(R.id.progressBar)

        adapter = ContactAdapter(emptyList()) { item ->
            onItemClicked(item)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString()?.trim() ?: ""
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
                    if (text.isEmpty()) loadList(showLoader = false)
                    else searchList(text)
                }
            }
        })

        loadList(showLoader = true)
    }

    override fun onResume() {
        super.onResume()
        loadList(showLoader = false)
    }

    private fun loadList(showLoader: Boolean) {
        if (showLoader) showLoading(true)
        val url = ApiConstants.BASE_URL + listEndpoint
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getParticipants(url, AppSession.authHeader())
                showLoading(false)
                if (response.isSuccessful) parseAndShow(response.body())
                else showEmpty(true)
            } catch (e: Exception) {
                showLoading(false)
                showEmpty(true)
            }
        }
    }

    private fun searchList(query: String) {
        val url = ApiConstants.BASE_URL + searchEndpoint + query
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getParticipants(url, AppSession.authHeader())
                if (response.isSuccessful) parseAndShow(response.body())
            } catch (_: Exception) {}
        }
    }

    private fun parseAndShow(array: JsonArray?) {
        val items = array?.mapNotNull { el ->
            val obj = el.asJsonObject
            ContactItem(
                name = obj.optString("Name"),
                mobile = obj.optString("Mobile"),
                unreadCount = obj.optInt("UnreadCount"),
                participantId = obj.optInt("ParticipantId"),
                rawJson = obj
            )
        } ?: emptyList()
        adapter.updateData(items)
        showEmpty(items.isEmpty())
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) recyclerView.visibility = View.GONE
        else recyclerView.visibility = View.VISIBLE
    }

    private fun showEmpty(show: Boolean) {
        tvEmpty.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    open fun onItemClicked(item: ContactItem) {
        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra(ChatActivity.EXTRA_CONTACT_NAME, item.name)
            putExtra(ChatActivity.EXTRA_PARTICIPANT_ID, item.participantId)
        }
        startActivity(intent)
    }
}

// Extension helpers for JsonObject
fun com.google.gson.JsonObject.optString(key: String) =
    takeIf { has(key) && !get(key).isJsonNull }?.get(key)?.asString ?: ""

fun com.google.gson.JsonObject.optInt(key: String) =
    takeIf { has(key) && !get(key).isJsonNull }?.get(key)?.asInt ?: 0
