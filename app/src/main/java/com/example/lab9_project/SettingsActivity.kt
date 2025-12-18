package com.example.lab9_project

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        val switchTheme = findViewById<SwitchMaterial>(R.id.switchTheme)
        switchTheme.isChecked = prefs.getBoolean("dark_theme", false)
        switchTheme.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("dark_theme", checked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            recreate()
        }

        val switchSound = findViewById<SwitchMaterial>(R.id.switchSound)
        switchSound.isChecked = prefs.getBoolean("sound_enabled", true)
        switchSound.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("sound_enabled", checked).apply()
        }

        val switchNotifications = findViewById<SwitchMaterial>(R.id.switchNotifications)
        switchNotifications.isChecked = prefs.getBoolean("notifications_enabled", true)
        switchNotifications.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("notifications_enabled", checked).apply()
        }
    }

    private fun applySavedTheme() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_theme", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
