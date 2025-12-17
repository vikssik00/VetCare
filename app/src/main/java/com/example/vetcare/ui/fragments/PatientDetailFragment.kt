package com.example.vetcare.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.vetcare.models.Pet
import com.example.vetcare.R
import com.google.firebase.firestore.FirebaseFirestore

class PatientDetailFragment : Fragment() {

    private lateinit var tvPetName: TextView
    private lateinit var tvSpecies: TextView
    private lateinit var tvBreed: TextView
    private lateinit var tvBirthDate: TextView
    private lateinit var tvMedicalHistory: TextView
    private lateinit var tvOwnerInfo: TextView
    private lateinit var tvLastCheckup: TextView

    private lateinit var db: FirebaseFirestore
    private var petId: String = ""

    companion object {
        fun newInstance(petId: String): PatientDetailFragment {
            return PatientDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("PET_ID", petId)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_patient_detail, container, false)

        petId = arguments?.getString("PET_ID") ?: ""
        db = FirebaseFirestore.getInstance()

        initViews(view)
        loadPetDetails()

        return view
    }

    private fun initViews(view: View) {
        tvPetName = view.findViewById(R.id.tvPetName)
        tvSpecies = view.findViewById(R.id.tvSpecies)
        tvBreed = view.findViewById(R.id.tvBreed)
        tvBirthDate = view.findViewById(R.id.tvBirthDate)
        tvMedicalHistory = view.findViewById(R.id.tvMedicalHistory)
        tvOwnerInfo = view.findViewById(R.id.tvOwnerInfo)
        tvLastCheckup = view.findViewById(R.id.tvLastCheckup)
    }

    private fun loadPetDetails() {
        if (petId.isEmpty()) return

        db.collection("pets").document(petId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val pet = document.toObject(Pet::class.java)
                    pet?.let {
                        displayPetInfo(it)
                        loadOwnerInfo(it.ownerId)
                    }
                }
            }
    }

    private fun displayPetInfo(pet: Pet) {
        tvPetName.text = pet.name
        tvSpecies.text = "Вид: ${pet.species}"
        tvBreed.text = "Порода: ${pet.breed}"
        tvBirthDate.text = "Дата рождения: ${pet.birthDate}"
        tvMedicalHistory.text = "Медицинская история:\n${pet.medicalHistory}"
        tvLastCheckup.text = "Последний осмотр: ${pet.lastCheckup}"
    }

    private fun loadOwnerInfo(ownerId: String) {
        if (ownerId.isEmpty()) return

        db.collection("users").document(ownerId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("firstName") ?: ""
                    val phone = document.getString("phone") ?: ""
                    val email = document.getString("email") ?: ""

                    tvOwnerInfo.text = "Владелец: $name\nТелефон: $phone\nEmail: $email"
                }
            }
    }
}