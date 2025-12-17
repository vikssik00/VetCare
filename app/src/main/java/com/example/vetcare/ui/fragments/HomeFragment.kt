package com.example.vetcare.ui.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.vetcare.models.Appointment
import com.example.vetcare.models.Pet
import com.example.vetcare.R
import com.example.vetcare.utils.UserRoles
import com.example.vetcare.ui.activities.AppointmentActivity
import com.example.vetcare.ui.activities.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvDate: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnQuickAppointment: Button
    private lateinit var tvPetsCount: TextView
    private lateinit var tvAppointmentsCount: TextView
    private lateinit var tvVetsCount: TextView
    private lateinit var tvServicesCount: TextView

    private val petList = mutableListOf<Pet>()
    private val appointmentsList = mutableListOf<Appointment>()

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupClickListeners()

        loadInitialData()

        return view
    }

    private fun initViews(view: View) {
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvDate = view.findViewById(R.id.tvDate)
        progressBar = view.findViewById(R.id.progressBar)
        btnQuickAppointment = view.findViewById(R.id.btnQuickAppointment)
        tvPetsCount = view.findViewById(R.id.tvPetsCount)
        tvAppointmentsCount = view.findViewById(R.id.tvAppointmentsCount)

        setupStatsCardsClickListeners(view)

        val btnChat = view.findViewById<ImageButton>(R.id.btnChat)
        val btnNotifications = view.findViewById<ImageButton>(R.id.btnNotifications)

        btnChat?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ChatBotFragment())
                .addToBackStack(null)
                .commit()
        }


        btnNotifications?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, NotificationsFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupStatsCardsClickListeners(view: View) {
        val cardPets = view.findViewById<View>(R.id.cardPets)
        val cardAppointments = view.findViewById<View>(R.id.cardAppointments)

        cardPets?.setOnClickListener { navigateToPets() }
        cardAppointments?.setOnClickListener { navigateToAppointments() }
    }

    private fun setupClickListeners() {
        btnQuickAppointment.setOnClickListener { openAppointmentForSelection() }
    }

    private fun loadInitialData() {
        progressBar.visibility = View.VISIBLE

        updateCurrentDate()
        loadUserName()

        handler.postDelayed({ loadPetsCount() }, 300)
        handler.postDelayed({ loadAppointmentsCount() }, 600)
    }

    private fun updateCurrentDate() {
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ru"))
        tvDate.text = dateFormat.format(Date())
    }

    private fun loadUserName() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val username = doc.getString("firstName") ?: "Пользователь"
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val greeting = when (hour) {
                    in 5..11 -> "Доброе утро"
                    in 12..17 -> "Добрый день"
                    in 18..22 -> "Добрый вечер"
                    else -> "Доброй ночи"
                }
                tvGreeting.text = "$greeting, $username!"
            }
            .addOnFailureListener {
                tvGreeting.text = "Добро пожаловать!"
            }
    }

    private fun loadPetsCount() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("pets")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { result ->
                petList.clear()
                tvPetsCount.text = result.size().toString()
            }
            .addOnFailureListener { tvPetsCount.text = "0" }
    }

    private fun loadAppointmentsCount() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("appointments")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { result ->
                appointmentsList.clear()
                tvAppointmentsCount.text = result.size().toString()

                progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                tvAppointmentsCount.text = "0"

                progressBar.visibility = View.GONE
            }
    }


    private fun loadVetsCount() {
        db.collection("users")
            .whereEqualTo("role", UserRoles.VETERINARIAN)
            .get()
            .addOnSuccessListener { result -> tvVetsCount.text = result.size().toString() }
            .addOnFailureListener { tvVetsCount.text = "0" }
    }

    private fun loadServicesCount() {
        db.collection("services")
            .get()
            .addOnSuccessListener { result -> tvServicesCount.text = result.size().toString() }
            .addOnFailureListener { tvServicesCount.text = "0" }
    }

    private fun openAppointmentForSelection() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("pets")
            .whereEqualTo("ownerId", userId)
            .limit(10)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    showNoPetsDialog()
                } else if (result.size() == 1) {
                    val document = result.documents[0]
                    val pet = document.toObject(Pet::class.java)?.copy(petId = document.id)
                        ?: return@addOnSuccessListener
                    val intent = Intent(requireContext(), AppointmentActivity::class.java)
                    intent.putExtra("petId", pet.petId)
                    intent.putExtra("petName", pet.name)
                    startActivity(intent)
                } else {
                    val pets = mutableListOf<Pet>()
                    for (document in result.documents) {
                        val pet = document.toObject(Pet::class.java)?.copy(petId = document.id)
                        pet?.let { if (pets.size < 5) pets.add(it) }
                    }
                    showPetSelectionDialog(pets)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка загрузки питомцев", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showPetSelectionDialog(pets: List<Pet>) {
        val petNames = pets.map { it.name }
        AlertDialog.Builder(requireContext())
            .setTitle("Выберите питомца")
            .setItems(petNames.toTypedArray()) { _, which ->
                val selectedPet = pets[which]
                openAppointmentActivity(selectedPet)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun openAppointmentActivity(pet: Pet) {
        val intent = Intent(requireContext(), AppointmentActivity::class.java)
        intent.putExtra("petId", pet.petId)
        intent.putExtra("petName", pet.name)
        startActivity(intent)
    }

    private fun showNoPetsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Нет питомцев")
            .setMessage("Чтобы записаться на прием, сначала добавьте питомца")
            .setPositiveButton("Добавить питомца") { _, _ -> navigateToPets() }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun navigateToPets() {
        (activity as? MainActivity)?.navigateToTab(R.id.nav_pets)
    }

    private fun navigateToAppointments() {
        (activity as? MainActivity)?.navigateToTab(R.id.nav_appointments)
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed({ loadPetsCount(); loadAppointmentsCount() }, 500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }
}