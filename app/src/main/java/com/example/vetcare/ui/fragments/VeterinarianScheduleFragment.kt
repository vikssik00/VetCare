package com.example.vetcare.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vetcare.models.Appointment
import com.example.vetcare.utils.AppointmentStatus
import com.example.vetcare.utils.FirestoreCollections
import com.example.vetcare.R
import com.example.vetcare.adapters.VetScheduleAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class VeterinarianScheduleFragment : Fragment() {

    private lateinit var recyclerSchedule: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    private lateinit var scheduleAdapter: VetScheduleAdapter
    private val appointmentList = mutableListOf<Appointment>()

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_vet_schedule, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupRecyclerView()
        loadVetSchedule()

        return view
    }

    private fun initViews(view: View) {
        recyclerSchedule = view.findViewById(R.id.recyclerSchedule)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmpty = view.findViewById(R.id.tvEmpty)
    }

    private fun setupRecyclerView() {
        scheduleAdapter = VetScheduleAdapter { appointment ->
            showAppointmentDetails(appointment)
        }

        recyclerSchedule.layoutManager = LinearLayoutManager(requireContext())
        recyclerSchedule.adapter = scheduleAdapter
    }

    private fun loadVetSchedule() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        recyclerSchedule.visibility = View.GONE

        val vetId = auth.currentUser?.uid ?: return

        db.collection(FirestoreCollections.APPOINTMENTS)
            .whereEqualTo("vetId", vetId)
            .get()
            .addOnSuccessListener { snapshot ->
                appointmentList.clear()

                if (snapshot.isEmpty) {
                    showEmptyState()
                    return@addOnSuccessListener
                }

                for (doc in snapshot.documents) {
                    val appointment = Appointment(
                        id = doc.id,
                        petId = doc.getString("petId") ?: "",
                        petName = doc.getString("petName") ?: "Неизвестно",
                        ownerId = doc.getString("ownerId") ?: "",
                        ownerName = doc.getString("ownerName") ?: "",
                        vetId = doc.getString("vetId") ?: "",
                        vetName = doc.getString("vetName") ?: "",
                        serviceId = doc.getString("serviceId") ?: "",
                        serviceName = doc.getString("serviceName") ?: "Консультация",
                        servicePrice = doc.getDouble("servicePrice") ?: 0.0,
                        appointmentDate = doc.getString("date")
                            ?: doc.getString("appointmentDate")
                            ?: "",
                        appointmentTime = doc.getString("appointmentTime")
                            ?: doc.getString("time")
                            ?: "",
                        duration = (doc.getLong("duration") ?: 30).toInt(),
                        status = doc.getString("status") ?: AppointmentStatus.SCHEDULED,
                        consultationType = doc.getString("consultationType") ?: "offline",
                        notes = doc.getString("notes") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0,
                        updatedAt = doc.getLong("updatedAt") ?: 0
                    )

                    appointmentList.add(appointment)
                }

                groupAppointmentsByDate()

                scheduleAdapter.submitList(appointmentList)
                showScheduleList()
            }
            .addOnFailureListener {
                showEmptyState()
                Toast.makeText(
                    requireContext(),
                    "Ошибка загрузки расписания",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    /**
     * Группировка записей по дате + сортировка по времени
     */
    private fun groupAppointmentsByDate() {
        if (appointmentList.isEmpty()) return

        appointmentList.sortWith(
            compareBy({ it.appointmentDate }, { it.appointmentTime })
        )

        val grouped = mutableListOf<Appointment>()
        var currentDate = ""

        for (appointment in appointmentList) {
            if (appointment.appointmentDate != currentDate) {
                currentDate = appointment.appointmentDate

                grouped.add(
                    Appointment(
                        id = "header_$currentDate",
                        petName = formatDateForDisplay(currentDate),
                        appointmentDate = currentDate,
                        isHeader = true
                    )
                )
            }
            grouped.add(appointment)
        }

        appointmentList.clear()
        appointmentList.addAll(grouped)
    }

    private fun formatDateForDisplay(date: String): String {
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val output = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
            output.format(input.parse(date)!!)
        } catch (e: Exception) {
            date
        }
    }

    private fun showAppointmentDetails(appointment: Appointment) {
        if (appointment.isHeader) return

        val message = """
            🐾 Пациент: ${appointment.petName}
            🕒 Время: ${appointment.appointmentTime} – ${appointment.getEndTime()}
            🏥 Услуга: ${appointment.serviceName}
            👤 Владелец: ${appointment.ownerName}
            📝 Статус: ${getStatusText(appointment.status)}
            💰 Стоимость: ${appointment.servicePrice} ₽
            ${if (appointment.notes.isNotEmpty()) "\n📝 Заметки: ${appointment.notes}" else ""}
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Детали записи")
            .setMessage(message)
            .setPositiveButton("ОК", null)
            .setNeutralButton("Изменить статус") { _, _ ->
                changeAppointmentStatus(appointment)
            }
            .show()
    }

    private fun getStatusText(status: String): String {
        return when (status) {
            AppointmentStatus.COMPLETED -> "✅ Завершено"
            AppointmentStatus.CANCELLED -> "❌ Отменено"
            AppointmentStatus.SCHEDULED -> "⏳ Запланировано"
            else -> status
        }
    }

    private fun changeAppointmentStatus(appointment: Appointment) {
        val statuses = arrayOf("Запланировано", "Завершено", "Отменено")

        val currentIndex = when (appointment.status) {
            AppointmentStatus.SCHEDULED -> 0
            AppointmentStatus.COMPLETED -> 1
            AppointmentStatus.CANCELLED -> 2
            else -> 0
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Изменить статус")
            .setSingleChoiceItems(statuses, currentIndex) { dialog, which ->
                val newStatus = when (which) {
                    0 -> AppointmentStatus.SCHEDULED
                    1 -> AppointmentStatus.COMPLETED
                    2 -> AppointmentStatus.CANCELLED
                    else -> AppointmentStatus.SCHEDULED
                }

                updateAppointmentStatus(appointment.id, newStatus)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateAppointmentStatus(appointmentId: String, newStatus: String) {
        db.collection(FirestoreCollections.APPOINTMENTS)
            .document(appointmentId)
            .update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Статус обновлён", Toast.LENGTH_SHORT).show()
                loadVetSchedule()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка обновления", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEmptyState() {
        progressBar.visibility = View.GONE
        recyclerSchedule.visibility = View.GONE
        tvEmpty.visibility = View.VISIBLE
    }

    private fun showScheduleList() {
        progressBar.visibility = View.GONE
        tvEmpty.visibility = View.GONE
        recyclerSchedule.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        loadVetSchedule()
    }
}