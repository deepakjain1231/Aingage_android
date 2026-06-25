package com.example.aingage

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.aingage.utils.PreferenceManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, 2000L)
    }

    private fun navigateToNextScreen() {
        val intent = when {

            // Case 1: User was logged in → go directly to Home
            isLoggedIn() -> {
                Intent(this, MainActivity::class.java)
            }

            // Case 2: User logged out (has saved credentials) → go to Login with pre-fill
            hasUserCredentials() -> {
                // fromAuth = false → LoginActivity will pre-fill userID & password
                Intent(this, LoginActivity::class.java)
            }

            // Case 3: Company code exists (fresh login path) → go to Auth with pre-fill
            // Case 4: Fresh install → go to Auth (CompanyAuthActivity.onResume pre-fills if code exists)
            else -> {
                Intent(this, CompanyAuthActivity::class.java)
            }
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun isLoggedIn(): Boolean =
        PreferenceManager.getLoginDetails(this) != null &&
                PreferenceManager.getParticipantId(this) != 0

    private fun hasUserCredentials(): Boolean =
        !PreferenceManager.getSavedUserId(this).isNullOrEmpty()
}
