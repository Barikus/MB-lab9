package com.example.lab9_project;

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private val BASEURL = "http://192.168.56.1/quiz/"

    private lateinit var historyContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        historyContainer = findViewById(R.id.historyContainer)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        val username = intent.getStringExtra("USERNAME") ?: "Guest"
        loadHistory(username)
    }

    private fun loadHistory(username: String) {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        historyContainer.removeAllViews()

        // Запускаем в отдельном потоке, чтобы не блокировать UI
        Thread {
            val putData = PutData(
                BASEURL + "history.php",
                "POST",
                arrayOf("username"),
                arrayOf(username)
            )

            var success = false
            var result = ""

            if (putData.startPut()) {
                if (putData.onComplete()) {
                    result = putData.result.trim()
                    success = true
                }
            }

            // Возвращаемся в UI-поток для обновления экрана
            runOnUiThread {
                progressBar.visibility = View.GONE

                if (!success) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "Не удалось загрузить данные (ошибка сети)"
                    return@runOnUiThread
                }

                try {
                    if (result.startsWith("[")) {
                        val jsonArray = JSONArray(result)

                        if (jsonArray.length() == 0) {
                            tvEmpty.visibility = View.VISIBLE
                            tvEmpty.text = "История прохождений пуста.\nСыграйте игру, чтобы увидеть результат!"
                        } else {
                            tvEmpty.visibility = View.GONE
                            for (i in 0 until jsonArray.length()) {
                                val item = jsonArray.getJSONObject(i)
                                val category = item.optString("category", "Quiz")
                                val score = item.optInt("score", 0)
                                val playedAt = item.optString("playedat", "")

                                addHistoryCard(category, score, playedAt)
                            }
                        }
                    } else {
                        // Если сервер вернул не JSON (ошибку PHP)
                        tvEmpty.visibility = View.VISIBLE
                        tvEmpty.text = "Ошибка сервера:\n$result"
                    }
                } catch (e: Exception) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "Ошибка обработки:\n${e.message}"
                }
            }
        }.start()
    }

    private fun addHistoryCard(category: String, score: Int, dateTime: String) {
        val cardView = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 24) }

            radius = 16f
            cardElevation = 4f
            setContentPadding(32, 24, 32, 24)
            setCardBackgroundColor(Color.parseColor("#424242"))
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Категория
        val categoryText = TextView(this).apply {
            text = category
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE) // ЯВНО БЕЛЫЙ
        }

        // Очки
        val scoreText = TextView(this).apply {
            text = "Результат: $score"
            textSize = 18f
            setPadding(0, 8, 0, 0)
            setTextColor(Color.WHITE)
        }

        // Дата
        val dateText = TextView(this).apply {
            // Если дата пустая, пишем "Нет даты" для проверки
            text = if (dateTime.isNotEmpty()) formatDate(dateTime) else "Нет даты"
            textSize = 14f
            setPadding(0, 8, 0, 0)
            setTextColor(Color.LTGRAY)
        }

        container.addView(categoryText)
        container.addView(scoreText)
        container.addView(dateText)
        cardView.addView(container)

        historyContainer.addView(cardView)
    }

    private fun formatDate(dateTime: String): String {
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val output = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("ru"))
            val d = input.parse(dateTime)
            output.format(d ?: Date())
        } catch (e: Exception) {
            dateTime
        }
    }

    private fun getColorCompat(attr: Int): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}
