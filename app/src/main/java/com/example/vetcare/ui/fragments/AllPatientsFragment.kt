package com.example.vetcare.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vetcare.utils.FirestoreCollections
import com.example.vetcare.models.Pet
import com.example.vetcare.adapters.PetListAdapter
import com.example.vetcare.R
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class AllPatientsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    private lateinit var adapter: PetListAdapter
    private val pets = mutableListOf<Pet>()

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_all_patients, container, false)

        recyclerView = view.findViewById(R.id.recyclerPets)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        setupRecycler()
        loadAllPatients()

        return view
    }

    private fun setupRecycler() {
        adapter = PetListAdapter(
            pets = pets,
            onClick = { pet -> openPatient(pet) },
            onEdit = { pet -> openPatient(pet) },
            onDelete = {
                Toast.makeText(requireContext(), "Удаление недоступно", Toast.LENGTH_SHORT).show()
            }
        )
        adapter.hideButtons = true

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun loadAllPatients() {
        progressBar.isVisible = true
        recyclerView.isVisible = false
        tvEmpty.isVisible = false

        db.collection(FirestoreCollections.PETS)
            .get()
            .addOnSuccessListener { snapshot ->
                pets.clear()

                for (doc in snapshot.documents) {
                    pets.add(
                        Pet(
                            petId = doc.id,
                            name = doc.getString("name") ?: "Без имени",
                            species = doc.getString("species") ?: "Не указано",
                            breed = doc.getString("breed") ?: "",
                            birthDate = doc.getString("birthDate") ?: "",
                            ownerId = doc.getString("ownerId") ?: "",
                            medicalHistory = doc.getString("medicalHistory") ?: "",
                            photoUrl = doc.getString("photoUrl") ?: "",
                            lastCheckup = doc.getString("lastCheckup") ?: "",
                            createdAt = doc.getLong("createdAt") ?: 0,
                            isActive = doc.getBoolean("isActive") ?: true
                        )
                    )
                }

                pets.sortBy { it.name.lowercase(Locale.getDefault()) }

                adapter.updatePets(pets)

                progressBar.isVisible = false
                recyclerView.isVisible = pets.isNotEmpty()
                tvEmpty.isVisible = pets.isEmpty()
            }
            .addOnFailureListener {
                progressBar.isVisible = false
                tvEmpty.isVisible = true
                tvEmpty.text = "Ошибка загрузки пациентов"
            }
    }

    private fun openPatient(pet: Pet) {
        val fragment = PatientDetailFragment.Companion.newInstance(pet.petId)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}