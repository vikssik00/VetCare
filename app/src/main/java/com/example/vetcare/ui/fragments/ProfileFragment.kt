package com.example.vetcare.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.vetcare.R
import com.example.vetcare.ui.activities.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {

    private lateinit var tvProfileName: TextView
    private lateinit var etProfileEmail: EditText
    private lateinit var tvProfileDate: TextView
    private lateinit var btnEditEmail: Button
    private lateinit var btnLogout: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isEditingEmail = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        tvProfileName = view.findViewById(R.id.tvProfileName)
        etProfileEmail = view.findViewById(R.id.etProfileEmail)
        tvProfileDate = view.findViewById(R.id.tvProfileDate)
        btnEditEmail = view.findViewById(R.id.btnEditEmail)
        btnLogout = view.findViewById(R.id.btnLogout)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        loadProfileInfo()

        btnEditEmail.setOnClickListener { toggleEmailEdit() }

        btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(), "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        return view
    }

    private fun loadProfileInfo() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val firstName = doc.getString("firstName") ?: ""
                        val lastName = doc.getString("lastName") ?: ""
                        val middleName = doc.getString("middleName") ?: ""

                        // Объединяем ФИО с пробелами, убираем лишние пробелы
                        val fullName = listOf(lastName, firstName, middleName)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")

                        tvProfileName.text = if (fullName.isNotBlank()) fullName else "Без имени"

                        etProfileEmail.setText(doc.getString("email") ?: "")

                        val createdAt = doc.getLong("createdAt")
                        if (createdAt != null) {
                            val date = Date(createdAt)
                            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                            tvProfileDate.text = "Зарегистрирован: ${sdf.format(date)}"
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Ошибка загрузки профиля", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun toggleEmailEdit() {
        if (!isEditingEmail) {
            // Режим редактирования
            etProfileEmail.isEnabled = true
            etProfileEmail.requestFocus()
            btnEditEmail.text = "Сохранить"
            isEditingEmail = true
        } else {
            // Сохраняем изменения
            val newEmail = etProfileEmail.text.toString().trim()
            if (newEmail.isEmpty()) {
                Toast.makeText(requireContext(), "Введите email", Toast.LENGTH_SHORT).show()
                return
            }

            val userId = auth.currentUser?.uid ?: return
            db.collection("users").document(userId)
                .update("email", newEmail)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Email обновлён", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Ошибка при обновлении", Toast.LENGTH_SHORT).show()
                }

            etProfileEmail.isEnabled = false
            btnEditEmail.text = "Изменить"
            isEditingEmail = false
        }
    }
}