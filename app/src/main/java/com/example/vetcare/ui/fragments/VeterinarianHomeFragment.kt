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
import com.example.vetcare.utils.AppointmentStatus
import com.example.vetcare.utils.FirestoreCollections
import com.example.vetcare.models.Pet
import com.example.vetcare.adapters.PetListAdapter
import com.example.vetcare.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VeterinarianHomeFragment : Fragment() {

    // UI
    private lateinit var tvGreeting: TextView
    private lateinit var tvTodayDate: TextView
    private lateinit var recyclerPatients: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var btnTodayPatients: MaterialButton

    // Статистика
    private lateinit var tvPatientsCount: TextView
    private lateinit var tvCompletedCount: TextView
    private lateinit var tvWaitingCount: TextView
    private lateinit var tvRevenueCount: TextView

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Adapter + data
    private val allPatients = mutableListOf<Pet>()
    private val todayPatients = mutableListOf<Pet>()
    private lateinit var petAdapter: PetListAdapter

    private var showingToday = true

    private val dateLong = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
    private val dateShort = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_veterinarian_home, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupRecycler()
        setupButtons()
        setTodayDate()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadVeterinarianName()
        loadStatistics()
        loadAllPatients()
        loadTodaysPatients()
    }

    private fun initViews(view: View) {
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvTodayDate = view.findViewById(R.id.tvTodayDate)
        recyclerPatients = view.findViewById(R.id.recyclerPets)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        btnTodayPatients = view.findViewById(R.id.btnViewPatients)

        tvPatientsCount = view.findViewById(R.id.statValuePatients)
        tvCompletedCount = view.findViewById(R.id.statValueCompleted)
        tvWaitingCount = view.findViewById(R.id.statValueWaiting)
        tvRevenueCount = view.findViewById(R.id.statValueRevenue)
    }

    private fun setupRecycler() {
        petAdapter = PetListAdapter(
            pets = mutableListOf(),
            onClick = { openPatient(it) },
            onEdit = { openPatient(it) },
            onDelete = {
                Toast.makeText(requireContext(), "Удаление недоступно", Toast.LENGTH_SHORT).show()
            }
        )
        petAdapter.hideButtons = true
        recyclerPatients.layoutManager = LinearLayoutManager(requireContext())
        recyclerPatients.adapter = petAdapter
    }

    private fun setupButtons() {
        btnTodayPatients.setOnClickListener {
            showingToday = !showingToday
            updatePatientList()
            btnTodayPatients.text = if (showingToday) "Показать всех пациентов" else "Пациенты на сегодня"
        }
    }

    private fun setTodayDate() {
        tvTodayDate.text = dateLong.format(Date())
    }

    private fun loadVeterinarianName() {
        val uid = auth.currentUser?.uid ?: return
        db.collection(FirestoreCollections.USERS)
            .document(uid)
            .get()
            .addOnSuccessListener {
                val name = it.getString("firstName") ?: it.getString("name") ?: "Ветеринар"
                tvGreeting.text = "Добро пожаловать, $name!"
            }
    }

    private fun loadAllPatients() {
        db.collection(FirestoreCollections.PETS)
            .get()
            .addOnSuccessListener { snapshot ->
                allPatients.clear()
                for (doc in snapshot) {
                    allPatients.add(
                        Pet(
                            petId = doc.id,
                            name = doc.getString("name") ?: "Без имени",
                            species = doc.getString("species") ?: "",
                            breed = doc.getString("breed") ?: "",
                            birthDate = doc.getString("birthDate") ?: "",
                            ownerId = doc.getString("ownerId") ?: "",
                            medicalHistory = doc.getString("medicalHistory") ?: "",
                            photoUrl = doc.getString("photoUrl") ?: "",
                            lastCheckup = "",
                            createdAt = doc.getLong("createdAt") ?: 0,
                            isActive = true
                        )
                    )
                }
                allPatients.sortBy { it.name.lowercase() }
                updatePatientList()
            }
            .addOnFailureListener {
                showEmpty("Ошибка загрузки пациентов")
            }
    }

    private fun loadTodaysPatients() {
        val vetId = auth.currentUser?.uid ?: return
        val todayStr = dateShort.format(Date())

        db.collection(FirestoreCollections.APPOINTMENTS)
            .whereEqualTo("vetId", vetId)
            .get()
            .addOnSuccessListener { snapshot ->
                val petIds = snapshot.documents
                    .filter { it.getString("dateTime")?.startsWith(todayStr) == true }
                    .mapNotNull { it.getString("petId") }
                    .distinct()

                todayPatients.clear()
                if (petIds.isNotEmpty()) {
                    db.collection(FirestoreCollections.PETS)
                        .whereIn(FieldPath.documentId(), petIds)
                        .get()
                        .addOnSuccessListener { docs ->
                            for (doc in docs) {
                                todayPatients.add(
                                    Pet(
                                        petId = doc.id,
                                        name = doc.getString("name") ?: "Без имени",
                                        species = doc.getString("species") ?: "",
                                        breed = doc.getString("breed") ?: "",
                                        birthDate = doc.getString("birthDate") ?: "",
                                        ownerId = doc.getString("ownerId") ?: "",
                                        medicalHistory = doc.getString("medicalHistory") ?: "",
                                        photoUrl = doc.getString("photoUrl") ?: "",
                                        lastCheckup = "",
                                        createdAt = doc.getLong("createdAt") ?: 0,
                                        isActive = true
                                    )
                                )
                            }
                            todayPatients.sortBy { it.name.lowercase() }
                            updatePatientList()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Ошибка загрузки пациентов на сегодня", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    updatePatientList()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка загрузки записей", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updatePatientList() {
        if (showingToday) {
            petAdapter.updatePets(todayPatients)
            if (todayPatients.isEmpty()) showEmpty("На сегодня пациентов нет") else showList()
        } else {
            petAdapter.updatePets(allPatients)
            if (allPatients.isEmpty()) showEmpty("В клинике пока нет пациентов") else showList()
        }
    }

    private fun loadStatistics() {
        val vetId = auth.currentUser?.uid ?: return
        val todayStr = dateShort.format(Date())

        db.collection(FirestoreCollections.APPOINTMENTS)
            .whereEqualTo("vetId", vetId)
            .get()
            .addOnSuccessListener { docs ->

                var total = 0
                var completed = 0
                var waiting = 0
                var revenue = 0

                for (d in docs) {
                    val dateTime = d.getString("dateTime") ?: continue
                    if (!dateTime.startsWith(todayStr)) continue

                    total++

                    when (d.getString("status")) {
                        AppointmentStatus.COMPLETED -> {
                            completed++
                            revenue += d.getLong("servicePrice")?.toInt()
                                ?: d.getDouble("servicePrice")?.toInt()
                                        ?: 0
                        }
                        AppointmentStatus.SCHEDULED -> waiting++
                    }
                }

                tvPatientsCount.text = total.toString()
                tvCompletedCount.text = completed.toString()
                tvWaitingCount.text = waiting.toString()
                tvRevenueCount.text = "$revenue ₽"
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка загрузки статистики", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading() {
        progressBar.isVisible = true
        recyclerPatients.isVisible = false
        tvEmpty.isVisible = false
    }

    private fun showEmpty(text: String) {
        progressBar.isVisible = false
        recyclerPatients.isVisible = false
        tvEmpty.isVisible = true
        tvEmpty.text = text
    }

    private fun showList() {
        progressBar.isVisible = false
        tvEmpty.isVisible = false
        recyclerPatients.isVisible = true
    }

    private fun openPatient(pet: Pet) {
        Toast.makeText(requireContext(), "Пациент: ${pet.name}", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        loadTodaysPatients()
        loadStatistics()
    }
}