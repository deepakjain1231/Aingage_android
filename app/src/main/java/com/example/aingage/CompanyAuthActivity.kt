package com.example.aingage

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.aingage.network.ApiConstants
import com.example.aingage.network.RetrofitClient
import com.example.aingage.utils.PreferenceManager
import kotlinx.coroutines.launch

class CompanyAuthActivity : AppCompatActivity() {

    private lateinit var etCompanyName: EditText
    private lateinit var btnAuthenticate: Button
    private lateinit var progressBar: ProgressBar
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_company_auth)

        etCompanyName = findViewById(R.id.etCompanyName)
        btnAuthenticate = findViewById(R.id.btnAuthenticate)
        progressBar = findViewById(R.id.progressBar)

        // Show real app version
        val version = packageManager.getPackageInfo(packageName, 0).versionName
        findViewById<android.widget.TextView>(R.id.tvVersion).text = "V $version"

        btnAuthenticate.setOnClickListener {
            val code = etCompanyName.text.toString().trim()
            if (code.isEmpty()) {
                showAlert("Please Enter Company Code")
            } else {
                getClientsAPI(code)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Pre-fill saved company name (matches iOS viewWillAppear)
        val savedName = PreferenceManager.getCompanyName(this)
        if (!savedName.isNullOrEmpty()) {
            etCompanyName.setText(savedName)
        }
    }

    private fun getClientsAPI(companyCode: String) {
        showLoading(true)

        val url = ApiConstants.BASE_URL + ApiConstants.GET_CLIENTS + companyCode

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.checkClient(url)

                showLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    // Save company data to SharedPreferences (matches iOS UserDefaults)
                    PreferenceManager.saveCompanyData(this@CompanyAuthActivity, data)

                    // Navigate to Login — flag that we came from auth (don't pre-fill credentials)
                    val intent = Intent(this@CompanyAuthActivity, LoginActivity::class.java)
                    intent.putExtra(LoginActivity.EXTRA_FROM_AUTH, true)
                    startActivity(intent)

                } else {
                    showAlert("Company code not valid")
                }

            } catch (e: Exception) {
                showLoading(false)
                showAlert("Company code not valid")
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnAuthenticate.isEnabled = !show
    }

    private fun showAlert(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
