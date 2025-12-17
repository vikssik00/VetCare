package com.example.vetcare.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vetcare.R
import com.example.vetcare.utils.UserRoles
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    companion object {
        private const val ADMIN_EMAIL = "malinkaa2006@yandex.ru"
    }

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etMiddleName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etConfPass: EditText
    private lateinit var etPass: EditText
    private lateinit var btnSignUp: Button
    private lateinit var tvRedirectLogin: TextView
    private lateinit var roleSpinner: Spinner

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register)

        // Привязка элементов интерфейса
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etMiddleName = findViewById(R.id.etMiddleName)
        etEmail = findViewById(R.id.etSEmailAddress)
        etConfPass = findViewById(R.id.etSConfPassword)
        etPass = findViewById(R.id.etSPassword)
        btnSignUp = findViewById(R.id.btnSSigned)
        tvRedirectLogin = findViewById(R.id.login_link)
        roleSpinner = findViewById(R.id.role_spinner)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Настройка спиннера ролей
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.user_roles,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        roleSpinner.adapter = adapter

        btnSignUp.setOnClickListener {
            signUpUser()
        }

        tvRedirectLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun signUpUser() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val middleName = etMiddleName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val pass = etPass.text.toString().trim()
        val confirmPassword = etConfPass.text.toString().trim()
        val selectedRole = roleSpinner.selectedItem.toString()

        // Валидация обязательных полей
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || pass.isBlank() || confirmPassword.isBlank()) {
            Toast.makeText(this, "Заполните все обязательные поля", Toast.LENGTH_SHORT).show()
            return
        }

        if (pass != confirmPassword) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
            return
        }

        if (pass.length < 6) {
            Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверка уникальности email
        checkEmailUnique(email) { isUnique ->
            if (isUnique) {
                createUserAccount(firstName, lastName, middleName, email, pass, selectedRole)
            } else {
                Toast.makeText(this, "Пользователь с таким email уже существует", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkEmailUnique(email: String, callback: (Boolean) -> Unit) {
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                callback(documents.isEmpty)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка проверки email", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }

    private fun createUserAccount(
        firstName: String,
        lastName: String,
        middleName: String,
        email: String,
        pass: String,
        selectedRole: String
    ) {
        // Определение роли
        val role = if (email.equals(ADMIN_EMAIL, ignoreCase = true)) {
            UserRoles.ADMIN
        } else {
            when (selectedRole) {
                "Ветеринар" -> UserRoles.VETERINARIAN
                else -> UserRoles.PET_OWNER
            }
        }

        // Создание пользователя в Firebase Authentication
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    saveUserToFirestore(userId, firstName, lastName, middleName, email, role)
                } else {
                    Toast.makeText(
                        this,
                        "Регистрация не удалась: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun saveUserToFirestore(
        userId: String,
        firstName: String,
        lastName: String,
        middleName: String,
        email: String,
        role: String
    ) {
        val user = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "middleName" to middleName,
            "email" to email,
            "role" to role,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(userId)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Регистрация успешна! Добро пожаловать, $firstName!",
                    Toast.LENGTH_SHORT
                ).show()

                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Ошибка сохранения данных: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

                // Удаляем пользователя из Auth если не удалось сохранить в Firestore
                auth.currentUser?.delete()
            }
    }
}