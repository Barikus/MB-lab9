package com.example.lab9_project

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.appbar.MaterialToolbar

class CategoriesActivity : AppCompatActivity() {

    private var username: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        username = intent.getStringExtra("USERNAME")

        findViewById<Button>(R.id.btnIT).setOnClickListener {
            startGame("IT", username)
        }

        findViewById<Button>(R.id.btnAnimals).setOnClickListener {
            startGame("Animals", username)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_categories, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_history -> {
                val intent = Intent(this, HistoryActivity::class.java)
                intent.putExtra("USERNAME", username)
                startActivity(intent)
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startGame(category: String, username: String?) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("CATEGORY", category)
        intent.putExtra("USERNAME", username)
        startActivity(intent)
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
