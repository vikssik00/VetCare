package com.example.vetcare.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.vetcare.ui.activities.MainActivity
import com.example.vetcare.R
import com.example.vetcare.ui.activities.RegisterActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var tvRedirectSignUp: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var etEmail: EditText
    private lateinit var etPass: EditText
    private lateinit var btnLogin: Button
    private lateinit var cbRememberMe: CheckBox

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        initViews()
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tryAutoLogin()
        setupClickListeners()
        autoFillCredentials()
    }

    private fun initViews() {
        tvRedirectSignUp = findViewById(R.id.tvRedirectSignUp)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        etEmail = findViewById(R.id.etEmailAddress)
        etPass = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        cbRememberMe = findViewById(R.id.cbRememberMe)
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener { login() }

        tvRedirectSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    // Автовход если пользователь уже авторизован
    private fun tryAutoLogin() {
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    // Автозаполнение email
    private fun autoFillCredentials() {
        val prefs = getSharedPreferences("login_prefs", MODE_PRIVATE)
        val savedEmail = prefs.getString("saved_email", "")
        val rememberMe = prefs.getBoolean("remember_me", false)

        etEmail.setText(savedEmail)
        cbRememberMe.isChecked = rememberMe
    }

    private fun saveLoginData(email: String, rememberMe: Boolean) {
        val prefs = getSharedPreferences("login_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("saved_email", email)
            putBoolean("remember_me", rememberMe)
            apply()
        }
    }

    private fun login() {
        val email = etEmail.text.toString().trim()
        val pass = etPass.text.toString().trim()

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Введите корректный email", Toast.LENGTH_SHORT).show()
            return
        }

        btnLogin.isEnabled = false
        btnLogin.text = "Вход..."

        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                btnLogin.isEnabled = true
                btnLogin.text = "Войти"

                if (task.isSuccessful) {
                    saveLoginData(email, cbRememberMe.isChecked)

                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    db.collection("users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { document ->
                            val firstName = document.getString("firstName") ?: "Пользователь"

                            getSharedPreferences("user_prefs", MODE_PRIVATE)
                                .edit()
                                .putString("user_first_name", firstName).apply()

                            // Toast.makeText(this, "Добро пожаловать, $firstName!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                } else {
                    val message = when {
                        task.exception?.message?.contains("password") == true ->
                            "Неверный пароль"
                        task.exception?.message?.contains("no user record") == true ->
                            "Пользователь не найден"
                        else -> "Ошибка входа"
                    }
                    // Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
    }

    // Восстановление пароля только для администратора
    private fun showForgotPasswordDialog() {
        val input = EditText(this)
        input.hint = "Введите email"
        input.setText(etEmail.text.toString())

        AlertDialog.Builder(this)
            .setTitle("Восстановление пароля (для администратора)")
            .setView(input)
            .setPositiveButton("Отправить") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isNotEmpty()) {
                    checkAdminAndSendReset(email)
                } else {
                    Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // Проверяем роль в Firestore
    private fun checkAdminAndSendReset(email: String) {
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val userDoc = result.documents[0]
                val role = userDoc.getString("role") ?: ""
                if (role != "Администратор") {
                    Toast.makeText(this, "Сброс пароля доступен только для администратора", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                sendPasswordResetEmail(email)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка проверки пользователя", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    AlertDialog.Builder(this)
                        .setTitle("Готово")
                        .setMessage("Ссылка для восстановления отправлена на:\n$email")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    Toast.makeText(this, "Ошибка отправки письма", Toast.LENGTH_SHORT).show()
                }
            }
    }
}