package com.example.lab9_project

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class LoginActivity : AppCompatActivity() {

    private val BASE_URL = "http://192.168.56.1/quiz/"

    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnAction = findViewById<Button>(R.id.btnLogin)
        val tvSwitch = findViewById<TextView>(R.id.tvSwitchToRegister)

        // режим: вход / регистрация
        tvSwitch.setOnClickListener {
            isLoginMode = !isLoginMode
            if (isLoginMode) {
                etEmail.visibility = View.GONE
                btnAction.text = "Войти"
                tvSwitch.text = "Нет аккаунта? Зарегистрироваться"
            } else {
                etEmail.visibility = View.VISIBLE
                btnAction.text = "Зарегистрироваться"
                tvSwitch.text = "Уже есть аккаунт? Войти"
            }
        }

        btnAction.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val email = etEmail.text.toString().trim()

            if (isLoginMode) {
                if (username.isNotEmpty() && password.isNotEmpty()) {
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        val field = arrayOf("username", "password")
                        val data = arrayOf(username, password)
                        val putData = PutData(BASE_URL + "login.php", "POST", field, data)
                        if (putData.startPut() && putData.onComplete()) {
                            val result = putData.result
                            if (result.contains("Login Success")) {
                                Toast.makeText(this, "Вход выполнен!", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, CategoriesActivity::class.java)
                                intent.putExtra("USERNAME", username)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Заполните логин и пароль", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (username.isNotEmpty() && password.isNotEmpty() && email.isNotEmpty()) {
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        val field = arrayOf("username", "password", "email")
                        val data = arrayOf(username, password, email)
                        val putData = PutData(BASE_URL + "signup.php", "POST", field, data)
                        if (putData.startPut() && putData.onComplete()) {
                            val result = putData.result
                            if (result.contains("Sign Up Success")) {
                                Toast.makeText(this, "Регистрация успешна! Теперь войдите.", Toast.LENGTH_LONG).show()
                                isLoginMode = true
                                etEmail.visibility = View.GONE
                                btnAction.text = "Войти"
                                tvSwitch.text = "Нет аккаунта? Зарегистрироваться"
                            } else {
                                Toast.makeText(this, "Ошибка: $result", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Заполните логин, email и пароль", Toast.LENGTH_SHORT).show()
                }
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
