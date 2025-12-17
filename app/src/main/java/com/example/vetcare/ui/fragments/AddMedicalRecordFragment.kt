package com.example.vetcare.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.vetcare.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddMedicalRecordFragment : Fragment() {

    private lateinit var etDiagnosis: EditText
    private lateinit var etTreatment: EditText
    private lateinit var etRecommendations: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var petId: String = ""
    private var petName: String = ""

    companion object {
        fun newInstance(petId: String, petName: String): AddMedicalRecordFragment {
            return AddMedicalRecordFragment().apply {
                arguments = Bundle().apply {
                    putString("PET_ID", petId)
                    putString("PET_NAME", petName)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_add_medical_record, container, false)

        petId = arguments?.getString("PET_ID") ?: ""
        petName = arguments?.getString("PET_NAME") ?: ""

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupButtons()

        return view
    }

    private fun initViews(view: View) {
        etDiagnosis = view.findViewById(R.id.etDiagnosis)
        etTreatment = view.findViewById(R.id.etTreatment)
        etRecommendations = view.findViewById(R.id.etRecommendations)
        btnSave = view.findViewById(R.id.btnSave)
        btnCancel = view.findViewById(R.id.btnCancel)
    }

    private fun setupButtons() {
        btnSave.setOnClickListener {
            saveMedicalRecord()
        }

        btnCancel.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun saveMedicalRecord() {
        val diagnosis = etDiagnosis.text.toString().trim()
        val treatment = etTreatment.text.toString().trim()
        val recommendations = etRecommendations.text.toString().trim()
        val vetId = auth.currentUser?.uid ?: return

        if (diagnosis.isEmpty()) {
            etDiagnosis.error = "Введите диагноз"
            return
        }

        val currentDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())

        val recordData = hashMapOf(
            "petId" to petId,
            "petName" to petName,
            "vetId" to vetId,
            "diagnosis" to diagnosis,
            "treatment" to treatment,
            "recommendations" to recommendations,
            "date" to currentDate,
            "createdAt" to System.currentTimeMillis()
        )

        // Сохраняем в коллекции medical_records
        db.collection("medical_records")
            .add(recordData)
            .addOnSuccessListener {
                // Обновляем медицинскую историю питомца
                updatePetMedicalHistory(diagnosis, treatment, recommendations, currentDate)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Ошибка сохранения: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updatePetMedicalHistory(diagnosis: String, treatment: String, recommendations: String, date: String) {
        val newRecord = "Дата: $date\nДиагноз: $diagnosis\nЛечение: $treatment\nРекомендации: $recommendations\n---\n"

        db.collection("pets").document(petId).get()
            .addOnSuccessListener { document ->
                val currentHistory = document.getString("medicalHistory") ?: ""
                val updatedHistory = newRecord + currentHistory

                db.collection("pets").document(petId)
                    .update("medicalHistory", updatedHistory, "lastCheckup", date)
                    .addOnSuccessListener {
                        Toast.makeText(
                            requireContext(),
                            "Запись успешно добавлена!",
                            Toast.LENGTH_SHORT
                        ).show()
                        requireActivity().supportFragmentManager.popBackStack()
                    }
            }
    }
}