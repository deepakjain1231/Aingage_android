package com.example.aingage

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.aingage.network.RetrofitClient
import com.example.aingage.utils.AppSession
import com.example.aingage.utils.PreferenceManager
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FROM_AUTH = "from_auth"
    }

    private lateinit var tvVersion: TextView
    private lateinit var etUserId: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnCancel: Button
    private lateinit var progressBar: ProgressBar

    private var fromAuth = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        fromAuth = intent.getBooleanExtra(EXTRA_FROM_AUTH, false)

        tvVersion = findViewById(R.id.tvVersion)
        etUserId = findViewById(R.id.etUserId)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnCancel = findViewById(R.id.btnCancel)
        progressBar = findViewById(R.id.progressBar)

        // Show app version
        tvVersion.text = "V ${packageManager.getPackageInfo(packageName, 0).versionName}"

        btnLogin.setOnClickListener { onLoginClicked() }

        // Cancel → go back to Company Auth screen
        btnCancel.setOnClickListener {
            startActivity(Intent(this, CompanyAuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Pre-fill only when coming from logout, not from Auth screen
        if (!fromAuth) {
            val savedId = PreferenceManager.getSavedUserId(this)
            val savedPass = PreferenceManager.getSavedUserPass(this)
            if (!savedId.isNullOrEmpty()) etUserId.setText(savedId)
            if (!savedPass.isNullOrEmpty()) etPassword.setText(savedPass)
        } else {
            // Coming from Authentication → clear fields
            etUserId.text.clear()
            etPassword.text.clear()
        }
    }

    private fun onLoginClicked() {
        val userId = etUserId.text.toString().trim()
        val password = etPassword.text.toString().trim()

        when {
            userId.isEmpty() -> showAlert("Please enter email")
            password.isEmpty() -> showAlert("Please enter password")
            else -> callLoginApi(userId, password)
        }
    }

    private fun callLoginApi(userId: String, password: String) {
        showLoading(true)

        // Base64 encode password (matches iOS utf8 → base64)
        val base64Password = Base64.encodeToString(
            password.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )

        val clientName = PreferenceManager.getCompanyName(this) ?: ""
        val authToken = PreferenceManager.getAuthToken(this) ?: ""

        val body = JsonObject().apply {
            addProperty("client", clientName)
            addProperty("username", userId)
            addProperty("password", base64Password)
        }

        val headers = mapOf(
            "Content-Type" to "application/json",
            "Authorization" to "Bearer $authToken"
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.login(headers, body)
                showLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    handleLoginSuccess(data, userId, password)
                } else {
                    showAlert("invalid login")
                }
            } catch (e: Exception) {
                showLoading(false)
                showAlert("invalid login")
            }
        }
    }

    private fun handleLoginSuccess(data: JsonObject, userId: String, password: String) {
        // Save full login response
        PreferenceManager.saveLoginDetails(this, data)

        // Save auth token (Cipher field)
        val cipher = data.takeIf { it.has("Cipher") }?.get("Cipher")?.asString ?: ""
        PreferenceManager.saveAuthToken(this, cipher)

        // Save participant ID from User object
        val participantId = data.takeIf { it.has("User") }
            ?.getAsJsonObject("User")
            ?.takeIf { it.has("ParticipantId") }
            ?.get("ParticipantId")?.asInt ?: 0
        PreferenceManager.saveParticipantId(this, participantId)

        // Save credentials for next login pre-fill
        PreferenceManager.saveUserCredentials(this, userId, password)

        // Initialize session so MainActivity has auth token + companyName immediately
        AppSession.init(this)

        // Navigate to Main screen (equivalent to iOS setupTabBar)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
        btnCancel.isEnabled = !show
    }

    private fun showAlert(message: String) {
        AlertDialog.Builder(this)
            .setTitle("AI Agent")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
