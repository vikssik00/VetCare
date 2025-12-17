package com.example.vetcare.ui.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vetcare.utils.FirestoreCollections
import com.example.vetcare.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale

class AddEditPetActivity : AppCompatActivity() {

    private lateinit var etPetName: EditText
    private lateinit var etPetSpecies: EditText
    private lateinit var etPetBreed: EditText
    private lateinit var etPetBirthDate: EditText
    private lateinit var etPetMedicalHistory: EditText
    private lateinit var btnSavePet: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var petId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_pet)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        setupClickListeners()

        petId = intent.getStringExtra("pet_id")
        if (petId != null) {
            loadPetData(petId!!)
            btnSavePet.text = "Сохранить изменения"
        } else {
            btnSavePet.text = "Добавить питомца"
        }
    }

    private fun initViews() {
        etPetName = findViewById(R.id.etPetName)
        etPetSpecies = findViewById(R.id.etPetSpecies)
        etPetBreed = findViewById(R.id.etPetBreed)
        etPetBirthDate = findViewById(R.id.etPetBirthDate)
        etPetMedicalHistory = findViewById(R.id.etPetMedicalHistory)
        btnSavePet = findViewById(R.id.btnSavePet)
    }

    private fun setupClickListeners() {
        btnSavePet.setOnClickListener {
            savePet()
        }

        // Добавляем выбор даты при клике на поле даты
        etPetBirthDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.Companion.format(
                    Locale.getDefault(),
                    "%02d.%02d.%04d",
                    dayOfMonth,
                    month + 1,
                    year
                )
                etPetBirthDate.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun loadPetData(petId: String) {
        db.collection(FirestoreCollections.PETS)
            .document(petId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    etPetName.setText(doc.getString("name"))
                    etPetSpecies.setText(doc.getString("species"))
                    etPetBreed.setText(doc.getString("breed"))

                    // Форматируем дату для отображения
                    val birthDate = doc.getString("birthDate") ?: ""
                    etPetBirthDate.setText(birthDate)

                    etPetMedicalHistory.setText(doc.getString("medicalHistory") ?: "")
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
            }
    }

    private fun savePet() {
        val name = etPetName.text.toString().trim()
        val species = etPetSpecies.text.toString().trim()
        val breed = etPetBreed.text.toString().trim()
        val birthDate = etPetBirthDate.text.toString().trim()
        val medicalHistory = etPetMedicalHistory.text.toString().trim()

        // Проверка заполнения полей
        if (name.isEmpty()) {
            etPetName.error = "Введите имя питомца"
            return
        }
        if (species.isEmpty()) {
            etPetSpecies.error = "Введите вид животного"
            return
        }
        if (breed.isEmpty()) {
            etPetBreed.error = "Введите породу"
            return
        }
        if (birthDate.isEmpty()) {
            etPetBirthDate.error = "Выберите дату рождения"
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        // Формируем данные для сохранения
        val petData = hashMapOf(
            "ownerId" to userId,
            "name" to name,
            "species" to species,
            "breed" to breed,
            "birthDate" to birthDate,
            "medicalHistory" to medicalHistory,
            "createdAt" to System.currentTimeMillis()
        )

        // Блокируем кнопку на время сохранения
        btnSavePet.isEnabled = false
        btnSavePet.text = "Сохранение..."

        if (petId == null) {
            // Добавление нового питомца
            db.collection(FirestoreCollections.PETS)
                .add(petData)
                .addOnSuccessListener { documentReference ->
                    Toast.makeText(this, "✅ Питомец добавлен", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    btnSavePet.isEnabled = true
                    btnSavePet.text = "Добавить питомца"
                    Toast.makeText(this, "❌ Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Обновление существующего питомца
            db.collection(FirestoreCollections.PETS)
                .document(petId!!)
                .set(petData)
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ Данные обновлены", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    btnSavePet.isEnabled = true
                    btnSavePet.text = "Сохранить изменения"
                    Toast.makeText(this, "❌ Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onBackPressed() {
        // Можно добавить подтверждение выхода если данные не сохранены
        super.onBackPressed()
    }
}