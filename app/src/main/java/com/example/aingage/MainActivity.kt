package com.example.aingage

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.aingage.ui.CustomerFragment
import com.example.aingage.ui.IMFragment
import com.example.aingage.ui.LeadsFragment
import com.example.aingage.ui.ReviewsFragment
import com.example.aingage.network.SignalRManager
import com.example.aingage.utils.AppSession
import com.example.aingage.utils.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var btnLogout: Button

    private val leadsFragment = LeadsFragment()
    private val customerFragment = CustomerFragment()
    private val imFragment = IMFragment()
    private val reviewsFragment = ReviewsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize session from saved prefs
        AppSession.init(this)

        // Start SignalR for real-time messages (matches iOS signalR() call)
        SignalRManager.connect(AppSession.authToken)

        btnLogout = findViewById(R.id.btnLogout)
        bottomNav = findViewById(R.id.bottomNav)

        btnLogout.setOnClickListener { showLogoutDialog() }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_leads -> leadsFragment
                R.id.nav_customer -> customerFragment
                R.id.nav_im -> imFragment
                R.id.nav_reviews -> reviewsFragment
                else -> leadsFragment
            }
            switchFragment(fragment)
            true
        }

        // Default tab = Leads
        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_leads
        }
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Do you really want to logout?")
            .setPositiveButton("Yes") { _, _ -> performLogout() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun performLogout() {
        SignalRManager.disconnect()
        // Clear login data but keep CompanyCode and user credentials (same as iOS)
        PreferenceManager.clearLoginDetails(this)
        PreferenceManager.saveParticipantId(this, 0)
        AppSession.clear()

        // Navigate to Login (from logout → pre-fill credentials)
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // fromAuth = false → LoginActivity will pre-fill saved credentials
        }
        startActivity(intent)
        finish()
    }
}
