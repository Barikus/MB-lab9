package com.example.lab9_project

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import coil.load
import com.google.android.material.appbar.MaterialToolbar
import org.json.JSONArray
import org.json.JSONObject

class GameActivity : AppCompatActivity() {

    private val BASE_URL = "http://192.168.56.1/quiz/"

    private lateinit var tvQuestion: TextView
    private lateinit var tvCount: TextView
    private lateinit var radioGroup: RadioGroup
    private lateinit var btnNext: Button
    private lateinit var ivImage: ImageView

    private var questionsList = ArrayList<JSONObject>()
    private var currentIndex = 0
    private var score = 0
    private var username = "Guest"
    private var category = "IT"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            sendNotification()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            showExitDialog()
        }

        username = intent.getStringExtra("USERNAME") ?: "Guest"
        category = intent.getStringExtra("CATEGORY") ?: "IT"

        tvQuestion = findViewById(R.id.tvQuestion)
        tvCount = findViewById(R.id.tvQuestionCount)
        radioGroup = findViewById(R.id.radioGroup)
        btnNext = findViewById(R.id.btnNext)
        ivImage = findViewById(R.id.ivQuestionImage)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitDialog()
            }
        })

        loadQuestionsFromServer()

        btnNext.setOnClickListener {
            checkAnswerAndNext()
        }
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle("Выход")
            .setMessage("Прервать игру?")
            .setPositiveButton("Да") { _, _ ->
                finish()
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun loadQuestionsFromServer() {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            val putData = PutData(
                BASE_URL + "get_questions.php",
                "POST",
                arrayOf("category"),
                arrayOf(category)
            )

            if (putData.startPut() && putData.onComplete()) {
                val result = putData.result
                try {
                    if (!result.trim().startsWith("[")) {
                        Toast.makeText(this, "Ошибка сервера: $result", Toast.LENGTH_LONG).show()
                        return@post
                    }

                    val jsonArray = JSONArray(result)
                    for (i in 0 until jsonArray.length()) {
                        questionsList.add(jsonArray.getJSONObject(i))
                    }

                    if (questionsList.isNotEmpty()) {
                        showQuestion()
                    } else {
                        Toast.makeText(this, "Категория '$category' пуста", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Ошибка парсинга: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Нет соединения!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showQuestion() {
        val q = questionsList[currentIndex]
        tvCount.text = "Вопрос ${currentIndex + 1} из ${questionsList.size}"
        tvQuestion.text = q.getString("question")

        val imageUrl = q.optString("image_url", "")
        if (imageUrl.isNotEmpty() && imageUrl != "null") {
            ivImage.visibility = View.VISIBLE
            ivImage.load(imageUrl) {
                placeholder(R.drawable.ic_launcher_foreground)
                error(android.R.drawable.ic_menu_report_image)
            }
        } else {
            ivImage.visibility = View.GONE
        }

        (radioGroup.getChildAt(0) as RadioButton).text = q.getString("option1")
        (radioGroup.getChildAt(1) as RadioButton).text = q.getString("option2")
        (radioGroup.getChildAt(2) as RadioButton).text = q.getString("option3")
        (radioGroup.getChildAt(3) as RadioButton).text = q.getString("option4")

        radioGroup.clearCheck()
    }

    private fun checkAnswerAndNext() {
        val selectedId = radioGroup.checkedRadioButtonId
        if (selectedId == -1) {
            Toast.makeText(this, "Выберите вариант!", Toast.LENGTH_SHORT).show()
            return
        }

        val radioButton = findViewById<RadioButton>(selectedId)
        val answerIndex = radioGroup.indexOfChild(radioButton) + 1
        val correctAnswer = questionsList[currentIndex].getInt("correct_answer")

        if (answerIndex == correctAnswer) {
            score++
        }

        currentIndex++
        if (currentIndex < questionsList.size) {
            showQuestion()
        } else {
            finishGame()
        }
    }

    private fun requestNotificationPermissionAndSend() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)

        if (!notificationsEnabled) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    sendNotification()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            sendNotification()
        }
    }

    private fun sendNotification() {
        val channelId = "quiz_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Результаты викторины",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о завершении игр"
                enableVibration(true)
                enableLights(true)
            }
            manager.createNotificationChannel(channel)
        }

        val resultIntent = Intent(this, ResultActivity::class.java).apply {
            putExtra("SCORE", score)
            putExtra("TOTAL", questionsList.size)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)  // Используем простую иконку
            .setContentTitle("🎉 Викторина завершена!")
            .setContentText("Ваш результат: $score из ${questionsList.size}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Поздравляем! Вы набрали $score из ${questionsList.size} баллов."))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        manager.notify(1001, builder.build())
    }

    private fun finishGame() {
        requestNotificationPermissionAndSend()

        val handler = Handler(Looper.getMainLooper())
        handler.post {
            val putData = PutData(
                BASE_URL + "save_score.php",
                "POST",
                arrayOf("username", "score", "category"),
                arrayOf(username, score.toString(), category)
            )
            putData.startPut()
            if (putData.onComplete()) {
                val intent = Intent(this, ResultActivity::class.java)
                intent.putExtra("SCORE", score)
                intent.putExtra("TOTAL", questionsList.size)
                startActivity(intent)
                finish()
            }
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
