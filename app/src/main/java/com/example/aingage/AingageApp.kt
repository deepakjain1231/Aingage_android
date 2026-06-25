package com.example.aingage

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class AingageApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Force light mode globally — no dark mode support
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
